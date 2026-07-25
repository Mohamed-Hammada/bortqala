use base64::{Engine, engine::general_purpose::STANDARD as BASE64};
use chrono::{SecondsFormat, Utc};
use ed25519_dalek::{Signer, SigningKey};
use rand_core::OsRng;
use reqwest::blocking::Client;
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::{
    fs,
    path::{Path, PathBuf},
    process::Command,
};

#[derive(Serialize, Deserialize)]
struct InstallationLicense {
    installation_id: String,
    private_key: String,
    activation_id: Option<String>,
    certificate: Option<LicenseCertificate>,
    #[serde(default)]
    license_url: String,
    #[serde(default)]
    server_public_key: String,
}

#[derive(Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LicenseCertificate {
    pub activation_id: String,
    pub license_id: String,
    pub customer_reference: String,
    pub installation_id: String,
    pub device_fingerprint_hash: String,
    pub issued_at: String,
    pub expires_at: Option<String>,
    pub perpetual: bool,
    pub signature: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ActivateRequest {
    license_key: String,
    installation_id: String,
    device_fingerprint_hash: String,
    device_public_key: String,
    timestamp: String,
    signature: String,
}
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct ProofRequest {
    activation_id: String,
    nonce: String,
    timestamp: String,
    signature: String,
}
#[derive(Deserialize)]
struct ApiError {
    message: Option<String>,
}

pub struct LicenseClient {
    path: PathBuf,
    base_url: String,
    server_public_key: String,
    installation: InstallationLicense,
}

impl LicenseClient {
    pub fn load(
        app_data: &Path,
        base_url: String,
        server_public_key: String,
    ) -> Result<Self, String> {
        let path = app_data.join("license-installation.json");
        let mut installation = if path.exists() {
            serde_json::from_slice(&fs::read(&path).map_err(|e| e.to_string())?)
                .map_err(|e| e.to_string())?
        } else {
            let key = SigningKey::generate(&mut OsRng);
            InstallationLicense {
                installation_id: uuid::Uuid::new_v4().to_string(),
                private_key: BASE64.encode(key.to_bytes()),
                activation_id: None,
                certificate: None,
                license_url: base_url.clone(),
                server_public_key: server_public_key.clone(),
            }
        };
        if !base_url.is_empty() {
            installation.license_url = base_url;
        }
        if !server_public_key.is_empty() {
            installation.server_public_key = server_public_key;
        }
        let client = Self {
            path,
            base_url: installation.license_url.trim_end_matches('/').to_string(),
            server_public_key: installation.server_public_key.clone(),
            installation,
        };
        client.save()?;
        Ok(client)
    }
    pub fn load_saved(app_data: &Path) -> Result<Self, String> {
        let path = app_data.join("license-installation.json");
        if !path.exists() {
            return Err("No installed license was found.".into());
        }
        Self::load(app_data, String::new(), String::new())
    }
    pub fn activated(&self) -> bool {
        self.installation.activation_id.is_some()
    }
    pub fn activate(&mut self, license_key: String) -> Result<LicenseCertificate, String> {
        let key = self.key()?;
        let timestamp = now();
        let fingerprint = self.fingerprint(&key);
        let canonical = format!(
            "activate|{}|{}|{}",
            self.installation.installation_id, fingerprint, timestamp
        );
        let request = ActivateRequest {
            license_key,
            installation_id: self.installation.installation_id.clone(),
            device_fingerprint_hash: fingerprint,
            device_public_key: x509_public_key(&key),
            timestamp,
            signature: BASE64.encode(key.sign(canonical.as_bytes()).to_bytes()),
        };
        let certificate: LicenseCertificate = self.post("/public/v1/activations", &request)?;
        self.verify_certificate(&certificate)?;
        self.installation.activation_id = Some(certificate.activation_id.clone());
        self.installation.certificate = Some(certificate.clone());
        self.save()?;
        Ok(certificate)
    }
    pub fn validate(&mut self) -> Result<LicenseCertificate, String> {
        let certificate = self.proof("/public/v1/activations/validate")?;
        self.verify_certificate(&certificate)?;
        self.installation.certificate = Some(certificate.clone());
        self.save()?;
        Ok(certificate)
    }
    pub fn deactivate(&mut self) -> Result<(), String> {
        let _: serde_json::Value = self.proof("/public/v1/activations/deactivate")?;
        self.installation.activation_id = None;
        self.installation.certificate = None;
        self.save()
    }
    fn proof<T: for<'de> Deserialize<'de>>(&self, path: &str) -> Result<T, String> {
        let activation_id = self
            .installation
            .activation_id
            .clone()
            .ok_or("License is not activated.")?;
        let timestamp = now();
        let nonce = uuid::Uuid::new_v4().to_string();
        let canonical = format!("proof|{}|{}|{}", activation_id, nonce, timestamp);
        let key = self.key()?;
        self.post(
            path,
            &ProofRequest {
                activation_id,
                nonce,
                timestamp,
                signature: BASE64.encode(key.sign(canonical.as_bytes()).to_bytes()),
            },
        )
    }
    fn post<T: Serialize, R: for<'de> Deserialize<'de>>(
        &self,
        path: &str,
        body: &T,
    ) -> Result<R, String> {
        let response = Client::new()
            .post(format!("{}{}", self.base_url, path))
            .json(body)
            .send()
            .map_err(|e| format!("License service is unavailable: {e}"))?;
        if !response.status().is_success() {
            let status = response.status();
            let message = response
                .json::<ApiError>()
                .ok()
                .and_then(|e| e.message)
                .unwrap_or_else(|| status.to_string());
            return Err(message);
        }
        response.json().map_err(|e| e.to_string())
    }
    fn verify_certificate(&self, certificate: &LicenseCertificate) -> Result<(), String> {
        if self.server_public_key.is_empty() {
            return Err("License server public key is not configured.".into());
        }
        let bytes = BASE64
            .decode(&self.server_public_key)
            .map_err(|e| e.to_string())?;
        let key_bytes: [u8; 32] = bytes
            .get(bytes.len().saturating_sub(32)..)
            .ok_or("Invalid server public key")?
            .try_into()
            .map_err(|_| "Invalid server public key")?;
        let public =
            ed25519_dalek::VerifyingKey::from_bytes(&key_bytes).map_err(|e| e.to_string())?;
        let signature = ed25519_dalek::Signature::from_slice(
            &BASE64
                .decode(&certificate.signature)
                .map_err(|e| e.to_string())?,
        )
        .map_err(|e| e.to_string())?;
        let canonical = format!(
            "{}|{}|{}|{}|{}|{}|{}|{}",
            certificate.activation_id,
            certificate.license_id,
            certificate.customer_reference,
            certificate.installation_id,
            certificate.device_fingerprint_hash,
            certificate.issued_at,
            certificate.expires_at.as_deref().unwrap_or("null"),
            certificate.perpetual
        );
        public
            .verify_strict(canonical.as_bytes(), &signature)
            .map_err(|_| "License certificate signature is invalid.".into())
    }
    fn fingerprint(&self, key: &SigningKey) -> String {
        let machine = machine_guid();
        let mut hash = Sha256::new();
        hash.update(b"bemo-hr-device-v1|");
        hash.update(machine.as_bytes());
        hash.update(std::env::consts::ARCH.as_bytes());
        hash.update(key.verifying_key().as_bytes());
        format!("{:x}", hash.finalize())
    }
    fn key(&self) -> Result<SigningKey, String> {
        let bytes = BASE64
            .decode(&self.installation.private_key)
            .map_err(|e| e.to_string())?;
        let raw: [u8; 32] = bytes
            .try_into()
            .map_err(|_| "Invalid installation private key")?;
        Ok(SigningKey::from_bytes(&raw))
    }
    fn save(&self) -> Result<(), String> {
        let temporary = self.path.with_extension("tmp");
        fs::write(
            &temporary,
            serde_json::to_vec_pretty(&self.installation).map_err(|e| e.to_string())?,
        )
        .map_err(|e| e.to_string())?;
        fs::rename(temporary, &self.path).map_err(|e| e.to_string())
    }
}
fn now() -> String {
    Utc::now().to_rfc3339_opts(SecondsFormat::Millis, true)
}
fn x509_public_key(key: &SigningKey) -> String {
    let mut der = hex_to_bytes("302a300506032b6570032100");
    der.extend_from_slice(key.verifying_key().as_bytes());
    BASE64.encode(der)
}
fn hex_to_bytes(value: &str) -> Vec<u8> {
    (0..value.len())
        .step_by(2)
        .map(|i| u8::from_str_radix(&value[i..i + 2], 16).unwrap())
        .collect()
}
fn machine_guid() -> String {
    Command::new("reg")
        .args([
            "query",
            r"HKLM\SOFTWARE\Microsoft\Cryptography",
            "/v",
            "MachineGuid",
        ])
        .output()
        .ok()
        .and_then(|o| String::from_utf8(o.stdout).ok())
        .and_then(|s| {
            s.lines()
                .find(|l| l.contains("MachineGuid"))
                .and_then(|l| l.split_whitespace().last())
                .map(str::to_owned)
        })
        .unwrap_or_else(|| {
            std::env::var("COMPUTERNAME").unwrap_or_else(|_| "unknown-device".into())
        })
}
