mod license;

use license::LicenseClient;
use serde::{Deserialize, Serialize};
use std::{
    fs,
    io::{BufRead, BufReader},
    net::TcpListener,
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::{
        Arc, Mutex,
        atomic::{AtomicBool, Ordering},
    },
};
use tauri::Manager;

#[derive(Clone, Serialize, Deserialize)]
struct InstallSecrets {
    database_username: String,
    database_password: String,
    jwt_secret: String,
    bootstrap_admin_password: String,
}

#[derive(Clone, Serialize)]
#[serde(rename_all = "camelCase")]
struct InitialCredentials {
    app_code: String,
    username: String,
    password: String,
}

struct ManagedProcesses {
    backend: Arc<Mutex<Option<Child>>>,
    pg_ctl: Mutex<Option<PathBuf>>,
    postgres_data: Mutex<Option<PathBuf>>,
    resources: PathBuf,
    secrets: InstallSecrets,
    license: Mutex<LicenseClient>,
    navigation_allowed: Arc<AtomicBool>,
    backend_port: Arc<Mutex<Option<u16>>>,
    initial_credentials: InitialCredentials,
}

fn configured(runtime_name: &str, compiled: Option<&'static str>, default: &str) -> String {
    compiled
        .map(str::to_owned)
        .filter(|value| !value.trim().is_empty())
        .or_else(|| {
            std::env::var(runtime_name)
                .ok()
                .filter(|value| !value.trim().is_empty())
        })
        .unwrap_or_else(|| default.to_owned())
}

#[cfg(target_os = "windows")]
fn hidden(command: &mut Command) -> &mut Command {
    use std::os::windows::process::CommandExt;
    command.creation_flags(0x08000000)
}

#[cfg(not(target_os = "windows"))]
fn hidden(command: &mut Command) -> &mut Command {
    command
}

fn free_port() -> Result<u16, String> {
    TcpListener::bind("127.0.0.1:0")
        .map_err(|error| error.to_string())?
        .local_addr()
        .map(|address| address.port())
        .map_err(|error| error.to_string())
}

fn run_checked(command: &mut Command, purpose: &str) -> Result<(), String> {
    let output = hidden(command)
        .output()
        .map_err(|error| format!("{purpose}: {error}"))?;
    if output.status.success() {
        Ok(())
    } else {
        Err(format!(
            "{purpose}: {}",
            String::from_utf8_lossy(&output.stderr)
        ))
    }
}

fn load_or_create_secrets(app: &tauri::AppHandle) -> Result<InstallSecrets, String> {
    let app_data = app
        .path()
        .app_local_data_dir()
        .map_err(|error| error.to_string())?;
    fs::create_dir_all(&app_data).map_err(|error| error.to_string())?;
    let path = app_data.join("install-secrets.json");
    if path.exists() {
        return serde_json::from_slice(&fs::read(path).map_err(|error| error.to_string())?)
            .map_err(|error| format!("Invalid install secrets: {error}"));
    }
    let random = || {
        format!(
            "{}{}",
            uuid::Uuid::new_v4().simple(),
            uuid::Uuid::new_v4().simple()
        )
    };
    let secrets = InstallSecrets {
        database_username: format!("bemo_{}", &uuid::Uuid::new_v4().simple().to_string()[..12]),
        database_password: random(),
        jwt_secret: random(),
        bootstrap_admin_password: random()[..20].to_string(),
    };
    let temporary = app_data.join("install-secrets.json.tmp");
    fs::write(
        &temporary,
        serde_json::to_vec_pretty(&secrets).map_err(|error| error.to_string())?,
    )
    .map_err(|error| error.to_string())?;
    fs::rename(temporary, path).map_err(|error| error.to_string())?;
    Ok(secrets)
}

fn ensure_postgres(
    app: &tauri::AppHandle,
    resources: &Path,
    secrets: &InstallSecrets,
) -> Result<(String, PathBuf, PathBuf), String> {
    let postgres = resources.join("postgres");
    let bin = postgres.join("bin");
    let initdb = bin.join("initdb.exe");
    let pg_ctl = bin.join("pg_ctl.exe");
    let createdb = bin.join("createdb.exe");
    if !initdb.exists() || !pg_ctl.exists() {
        return Err("The packaged PostgreSQL distribution is missing.".into());
    }
    let data = app
        .path()
        .app_local_data_dir()
        .map_err(|error| error.to_string())?
        .join("postgres-data");
    fs::create_dir_all(data.parent().unwrap()).map_err(|error| error.to_string())?;
    if !data.join("PG_VERSION").exists() {
        let password_file = data.parent().unwrap().join("postgres-password.tmp");
        fs::write(&password_file, &secrets.database_password).map_err(|error| error.to_string())?;
        run_checked(
            Command::new(&initdb).args([
                "-D",
                data.to_str().unwrap(),
                "--encoding=UTF8",
                &format!("--username={}", secrets.database_username),
                "--auth=scram-sha-256",
                "--no-locale",
                &format!("--pwfile={}", password_file.to_string_lossy()),
            ]),
            "PostgreSQL initialization failed",
        )?;
        let _ = fs::remove_file(password_file);
    }
    let port = free_port()?;
    run_checked(
        Command::new(&pg_ctl).args([
            "-D",
            data.to_str().unwrap(),
            "-o",
            &format!("-p {port} -h 127.0.0.1"),
            "-w",
            "start",
        ]),
        "PostgreSQL startup failed",
    )?;
    let _ = hidden(
        Command::new(&createdb)
            .args([
                "-h",
                "127.0.0.1",
                "-p",
                &port.to_string(),
                "-U",
                &secrets.database_username,
                "bemo_erp",
            ])
            .env("PGPASSWORD", &secrets.database_password),
    )
    .output();
    Ok((
        format!("jdbc:postgresql://127.0.0.1:{port}/bemo_erp"),
        pg_ctl,
        data,
    ))
}

fn start_backend(
    app: &tauri::AppHandle,
    resources: &Path,
    db_url: &str,
    secrets: &InstallSecrets,
    navigation_allowed: Arc<AtomicBool>,
    backend_port: Arc<Mutex<Option<u16>>>,
) -> Result<Arc<Mutex<Option<Child>>>, String> {
    let native_backend = resources.join("backend").join("bemo-erp.exe");
    let java = resources.join("runtime").join("bin").join("java.exe");
    let jar = resources.join("backend").join("bemo-erp.jar");
    let mut command = if native_backend.exists() {
        Command::new(native_backend)
    } else if java.exists() && jar.exists() {
        let mut java_command = Command::new(java);
        java_command.args(["-jar", jar.to_str().unwrap()]);
        java_command
    } else {
        return Err("The packaged native backend executable is missing.".into());
    };
    command
        .env("SPRING_PROFILES_ACTIVE", "desktop")
        .env("DB_URL", db_url)
        .env("DB_USERNAME", &secrets.database_username)
        .env("DB_PASSWORD", &secrets.database_password)
        .env("HR_JWT_SECRET", &secrets.jwt_secret)
        .env("HR_BOOTSTRAP_ADMIN_USERNAME", "admin")
        .env(
            "HR_BOOTSTRAP_ADMIN_PASSWORD",
            &secrets.bootstrap_admin_password,
        )
        .env("HR_BOOTSTRAP_DEMO_DATA", "true")
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    let mut child = hidden(&mut command)
        .spawn()
        .map_err(|error| error.to_string())?;
    let stdout = child
        .stdout
        .take()
        .ok_or("Could not read backend output.")?;
    let stderr = child
        .stderr
        .take()
        .ok_or("Could not read backend error output.")?;
    let managed = Arc::new(Mutex::new(Some(child)));
    let app_handle = app.clone();
    std::thread::spawn(move || {
        let mut published = false;
        for line in BufReader::new(stdout).lines().map_while(Result::ok) {
            if !published && let Some(value) = line.strip_prefix("BEMO_BACKEND_PORT=") {
                if let Ok(port) = value.parse::<u16>() {
                    if let Ok(mut current) = backend_port.lock() {
                        *current = Some(port);
                    }
                    if navigation_allowed.load(Ordering::SeqCst) {
                        if let Some(window) = app_handle.get_webview_window("main") {
                            let url = format!("http://127.0.0.1:{port}").parse().unwrap();
                            let _ = window.navigate(url);
                            let _ = window.show();
                        }
                    }
                    published = true;
                }
            }
        }
    });
    std::thread::spawn(move || {
        for _ in BufReader::new(stderr).lines().map_while(Result::ok) {
            // Drain the hidden backend stream so a full pipe cannot stall Spring Boot.
        }
    });
    Ok(managed)
}

fn start_owned_services(app: &tauri::AppHandle, state: &ManagedProcesses) -> Result<(), String> {
    let mut state_backend = state
        .backend
        .lock()
        .map_err(|_| "Backend state is unavailable.")?;
    if state_backend.is_some() {
        return Ok(());
    }
    let (db_url, pg_ctl, postgres_data) = ensure_postgres(app, &state.resources, &state.secrets)?;
    let started = start_backend(
        app,
        &state.resources,
        &db_url,
        &state.secrets,
        state.navigation_allowed.clone(),
        state.backend_port.clone(),
    );
    match started {
        Ok(backend) => {
            let mut backend_guard = backend
                .lock()
                .map_err(|_| "Backend state is unavailable.")?;
            *state_backend = backend_guard.take();
            *state
                .pg_ctl
                .lock()
                .map_err(|_| "PostgreSQL state is unavailable.")? = Some(pg_ctl);
            *state
                .postgres_data
                .lock()
                .map_err(|_| "PostgreSQL state is unavailable.")? = Some(postgres_data);
            Ok(())
        }
        Err(error) => {
            let _ = hidden(Command::new(&pg_ctl).args([
                "-D",
                postgres_data.to_str().unwrap(),
                "-m",
                "fast",
                "-w",
                "stop",
            ]))
            .output();
            Err(error)
        }
    }
}

#[tauri::command]
fn license_status(state: tauri::State<'_, ManagedProcesses>) -> bool {
    state.navigation_allowed.load(Ordering::SeqCst)
}

#[tauri::command]
fn initial_credentials(state: tauri::State<'_, ManagedProcesses>) -> InitialCredentials {
    state.initial_credentials.clone()
}

#[tauri::command]
fn activate_license(
    app: tauri::AppHandle,
    state: tauri::State<'_, ManagedProcesses>,
    license_key: String,
) -> Result<String, String> {
    let customer = state
        .license
        .lock()
        .map_err(|_| "License state is unavailable.")?
        .activate(license_key)?
        .customer_reference;
    state.navigation_allowed.store(true, Ordering::SeqCst);
    if let Err(error) = start_owned_services(&app, &state) {
        state.navigation_allowed.store(false, Ordering::SeqCst);
        return Err(error);
    }
    if let Some(port) = *state
        .backend_port
        .lock()
        .map_err(|_| "Backend state is unavailable.")?
    {
        if let Some(window) = app.get_webview_window("main") {
            window
                .navigate(
                    format!("http://127.0.0.1:{port}")
                        .parse()
                        .map_err(|_| "Invalid local URL")?,
                )
                .map_err(|error| error.to_string())?;
        }
    }
    Ok(customer)
}

#[tauri::command]
fn deactivate_license(state: tauri::State<'_, ManagedProcesses>) -> Result<(), String> {
    state
        .license
        .lock()
        .map_err(|_| "License state is unavailable.")?
        .deactivate()?;
    state.navigation_allowed.store(false, Ordering::SeqCst);
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    if std::env::args().any(|argument| argument == "--deactivate-license") {
        let exit_code = match std::env::var("LOCALAPPDATA") {
            Ok(local_app_data) => {
                let app_data = PathBuf::from(local_app_data).join("com.bemo.hr.desktop");
                match LicenseClient::load_saved(&app_data) {
                    Ok(mut license_client) if license_client.activated() => {
                        license_client.deactivate().map(|_| 0).unwrap_or(2)
                    }
                    Ok(_) => 0,
                    Err(_) => 0,
                }
            }
            Err(_) => 2,
        };
        std::process::exit(exit_code);
    }
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            license_status,
            initial_credentials,
            activate_license,
            deactivate_license
        ])
        .setup(|app| {
            let resources = app
                .path()
                .resource_dir()
                .map_err(|error| error.to_string())?;
            let app_data = app
                .path()
                .app_local_data_dir()
                .map_err(|error| error.to_string())?;
            fs::create_dir_all(&app_data).map_err(|error| error.to_string())?;
            let license_url = configured(
                "BEMO_LICENSE_URL",
                option_env!("BEMO_LICENSE_URL"),
                "http://127.0.0.1:8091",
            );
            let license_public_key = configured(
                "BEMO_LICENSE_PUBLIC_KEY",
                option_env!("BEMO_LICENSE_PUBLIC_KEY"),
                "",
            );
            let enforced = configured(
                "BEMO_LICENSE_ENFORCED",
                option_env!("BEMO_LICENSE_ENFORCED"),
                "false",
            )
            .eq_ignore_ascii_case("true");
            let license = LicenseClient::load(&app_data, license_url, license_public_key)?;
            let valid = if enforced {
                license.activated() && license.validate_local().is_ok()
            } else {
                true
            };
            let navigation_allowed = Arc::new(AtomicBool::new(valid));
            let backend_port = Arc::new(Mutex::new(None));
            let secrets = load_or_create_secrets(app.handle())?;
            let backend = Arc::new(Mutex::new(None));
            let initial_credentials = InitialCredentials {
                app_code: "DEMO".into(),
                username: "admin".into(),
                password: secrets.bootstrap_admin_password.clone(),
            };
            app.manage(ManagedProcesses {
                backend,
                pg_ctl: Mutex::new(None),
                postgres_data: Mutex::new(None),
                resources,
                secrets,
                license: Mutex::new(license),
                navigation_allowed,
                backend_port,
                initial_credentials,
            });
            if valid {
                let state = app.state::<ManagedProcesses>();
                start_owned_services(app.handle(), &state)?;
            }
            Ok(())
        })
        .on_window_event(|window, event| {
            if matches!(event, tauri::WindowEvent::Destroyed) {
                let state = window.state::<ManagedProcesses>();
                if let Ok(mut backend) = state.backend.lock() {
                    if let Some(child) = backend.as_mut() {
                        let _ = child.kill();
                    }
                }
                let pg_ctl = state.pg_ctl.lock().ok().and_then(|value| value.clone());
                let postgres_data = state
                    .postgres_data
                    .lock()
                    .ok()
                    .and_then(|value| value.clone());
                if let (Some(pg_ctl), Some(postgres_data)) = (pg_ctl, postgres_data) {
                    let _ = hidden(Command::new(pg_ctl).args([
                        "-D",
                        postgres_data.to_str().unwrap(),
                        "-m",
                        "fast",
                        "-w",
                        "stop",
                    ]))
                    .output();
                }
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running Bemo ERP desktop");
}
