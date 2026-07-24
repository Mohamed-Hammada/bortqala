use std::{
    fs,
    io::{BufRead, BufReader},
    net::TcpListener,
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::{Arc, Mutex},
};
use tauri::{Manager, WebviewUrl};

struct ManagedProcesses {
    backend: Arc<Mutex<Option<Child>>>,
    pg_ctl: PathBuf,
    postgres_data: PathBuf,
}

#[cfg(target_os = "windows")]
fn hidden(command: &mut Command) -> &mut Command {
    use std::os::windows::process::CommandExt;
    command.creation_flags(0x08000000)
}

#[cfg(not(target_os = "windows"))]
fn hidden(command: &mut Command) -> &mut Command { command }

fn free_port() -> Result<u16, String> {
    TcpListener::bind("127.0.0.1:0")
        .map_err(|error| error.to_string())?
        .local_addr().map(|address| address.port()).map_err(|error| error.to_string())
}

fn run_checked(command: &mut Command, purpose: &str) -> Result<(), String> {
    let output = hidden(command).output().map_err(|error| format!("{purpose}: {error}"))?;
    if output.status.success() { Ok(()) } else {
        Err(format!("{purpose}: {}", String::from_utf8_lossy(&output.stderr)))
    }
}

fn ensure_postgres(app: &tauri::AppHandle, resources: &Path) -> Result<(String, PathBuf, PathBuf), String> {
    let postgres = resources.join("postgres");
    let bin = postgres.join("bin");
    let initdb = bin.join("initdb.exe");
    let pg_ctl = bin.join("pg_ctl.exe");
    let createdb = bin.join("createdb.exe");
    if !initdb.exists() || !pg_ctl.exists() {
        return Err("The packaged PostgreSQL distribution is missing.".into());
    }
    let data = app.path().app_local_data_dir().map_err(|error| error.to_string())?.join("postgres-data");
    fs::create_dir_all(data.parent().unwrap()).map_err(|error| error.to_string())?;
    if !data.join("PG_VERSION").exists() {
        run_checked(Command::new(&initdb).args(["-D", data.to_str().unwrap(), "--encoding=UTF8",
            "--username=postgres", "--auth=trust", "--no-locale"]), "PostgreSQL initialization failed")?;
    }
    let port = free_port()?;
    run_checked(Command::new(&pg_ctl).args(["-D", data.to_str().unwrap(), "-o",
        &format!("-p {port} -h 127.0.0.1"), "-w", "start"]), "PostgreSQL startup failed")?;
    let _ = hidden(Command::new(&createdb).args(["-h", "127.0.0.1", "-p", &port.to_string(),
        "-U", "postgres", "hr_platform"])).output();
    Ok((format!("jdbc:postgresql://127.0.0.1:{port}/hr_platform"), pg_ctl, data))
}

fn start_backend(app: &tauri::AppHandle, resources: &Path, db_url: &str) -> Result<Arc<Mutex<Option<Child>>>, String> {
    let java = resources.join("runtime").join("bin").join("java.exe");
    let jar = resources.join("backend").join("hr-platform.jar");
    if !java.exists() || !jar.exists() { return Err("The packaged Java runtime or backend jar is missing.".into()); }
    let secret = format!("{}{}", uuid::Uuid::new_v4().simple(), uuid::Uuid::new_v4().simple());
    let mut command = Command::new(java);
    command.args(["-jar", jar.to_str().unwrap()])
        .env("SPRING_PROFILES_ACTIVE", "desktop")
        .env("DB_URL", db_url).env("DB_USERNAME", "postgres").env("DB_PASSWORD", "")
        .env("HR_JWT_SECRET", secret).stdout(Stdio::piped()).stderr(Stdio::piped());
    let mut child = hidden(&mut command).spawn().map_err(|error| error.to_string())?;
    let stdout = child.stdout.take().ok_or("Could not read backend output.")?;
    let managed = Arc::new(Mutex::new(Some(child)));
    let app_handle = app.clone();
    std::thread::spawn(move || {
        for line in BufReader::new(stdout).lines().map_while(Result::ok) {
            if let Some(value) = line.strip_prefix("BEMO_BACKEND_PORT=") {
                if let Ok(port) = value.parse::<u16>() {
                    if let Some(window) = app_handle.get_webview_window("main") {
                        let url = format!("http://127.0.0.1:{port}").parse().unwrap();
                        let _ = window.navigate(url);
                        let _ = window.show();
                    }
                    break;
                }
            }
        }
    });
    Ok(managed)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let resources = app.path().resource_dir().map_err(|error| error.to_string())?;
            let (db_url, pg_ctl, postgres_data) = ensure_postgres(app.handle(), &resources)?;
            let backend = start_backend(app.handle(), &resources, &db_url)?;
            app.manage(ManagedProcesses { backend, pg_ctl, postgres_data });
            Ok(())
        })
        .on_window_event(|window, event| {
            if matches!(event, tauri::WindowEvent::Destroyed) {
                let state = window.state::<ManagedProcesses>();
                if let Ok(mut backend) = state.backend.lock() {
                    if let Some(child) = backend.as_mut() { let _ = child.kill(); }
                }
                let _ = hidden(Command::new(&state.pg_ctl).args(["-D", state.postgres_data.to_str().unwrap(),
                    "-m", "fast", "-w", "stop"])).output();
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running Bemo HR desktop");
}
