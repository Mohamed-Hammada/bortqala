# Bortqala Multi‑Vendor Biometric & Access Integrations

This package is an **apply-ready overlay** for `Mohamed-Hammada/bortqala`, branch `fm_bemo_consolidated`.
It integrates the version-aware supplier hub with Bortqala's existing Attendance biometric import/sync pipeline instead of creating a second attendance ledger.

## What is included

- `device-hub/` — all supplier packages and their integration/version profiles:
  - ZKTeco
  - Hikvision
  - Dahua
  - Suprema
  - VIRDI / UnionBiometrics
  - Anviz
  - Honeywell
- Spring Boot Attendance integration under `be/src/main/java/com/bemo/hr/attendance/`.
- Liquibase schema + bilingual translation data.
- Angular 22 UI under `fe/src/app/features/device-integrations/`.
- Docker Compose wiring for the persistent device hub.
- `apply.sh` / `apply.ps1` — idempotent patching of the three existing project files that must be edited.

## Architecture

```text
Angular Device Integrations UI
        │
        ▼
Bortqala /api/v1/device-integrations
        │
        ├─ version resolution ───────────────┐
        │                                    ▼
        │                         device-hub supplier package
        │                         model/firmware/API/SDK rules
        │                                    │
        │                                    ▼
        │                         vendor adapter / SDK bridge
        │                                    │
        └─ existing BiometricDevice ─────────┤
                                             ▼
                              normalized { punches: [...] }
                                             │
                                             ▼
                              BiometricDeviceSyncService
                                             │
                                             ▼
                              existing attendance/payroll flow
```

Bortqala remains the system of record. The hub is the device/protocol adapter layer.

## Apply to the branch

1. Check out `fm_bemo_consolidated`.
2. Extract this ZIP **into the repository root**, preserving paths.
3. Run one of:

```bash
./apply.sh
```

```powershell
.\apply.ps1
```

The script is idempotent. It patches:

- `fe/src/app/app.routes.ts`
- `fe/src/app/features/imports/imports.page.html`
- `be/src/main/resources/db/changelog/releases/next.changelog-master.yaml`

`be/compose.yaml` is supplied as the branch-aligned replacement and adds the `device-hub` service beside PostgreSQL.

## Run

```bash
docker compose -f be/compose.yaml up -d postgres device-hub
cd be
./gradlew bootRun
```

Windows:

```powershell
docker compose -f be/compose.yaml up -d postgres device-hub
cd be
.\gradlew.bat bootRun
```

Open:

```text
/imports/device-integrations
```

The existing `/imports` page also gets a **Device integrations** button.

## Configuration

Bortqala backend:

```text
BEMO_DEVICE_HUB_BASE_URL=http://localhost:8090
DEVICE_HUB_API_KEY=<optional-shared-key>
```

Device hub:

```text
DEVICE_HUB_REGISTRY_PATH=/data/devices.json
DEVICE_HUB_API_KEY=<same-optional-shared-key>
```

If `DEVICE_HUB_API_KEY` is set, Bortqala sends it both for management calls and scheduled normalized-punch fetches.

## Version-aware routing

A device is not pinned only by supplier/model. The resolver considers, where applicable:

- supplier
- model/device family
- device firmware
- platform/server version
- exact SDK version
- API version
- SDK host OS
- CPU architecture
- capability hints

Typical statuses:

- `COMPATIBLE`
- `NEEDS_SDK_VERSION`
- `NEEDS_API_VERSION`
- `NEEDS_SERVER_VERSION`
- `NEEDS_VENDOR_MATRIX`
- `INCOMPATIBLE`

Only a `COMPATIBLE` candidate can be saved as the active route.

## API and SDK support semantics

Each route also exposes `implementationStatus`. This is intentionally separate from version compatibility.

A route can be **version compatible** while still requiring a licensed native SDK, vendor-specific endpoint mapping, or conformance work. The UI shows this instead of calling every catalog entry “fully implemented.”

For HTTP/API routes, the shared adapter can retrieve attendance events when the vendor endpoint for the exact installed API version is configured through `options`. Runtime credentials remain in Bortqala: set `auth` to `basic`, `digest`, `bearer`, or `api-key`; for Bearer/API-key modes the encrypted Bortqala password field is used as the token/key. Example:

```json
{
  "auth": "digest",
  "punch_path": "/ISAPI/AccessControl/AcsEvent?format=json",
  "punch_method": "GET",
  "punch_array_path": "AcsEvent.InfoList",
  "since_param": "startTime",
  "user_fields": ["employeeNoString", "cardNo"],
  "time_fields": ["time", "eventTime"],
  "name_fields": ["name"]
}
```

For APIs that search events with POST, use `punch_method: "POST"`, `punch_body`, and optionally `since_body_field`; `punch_headers` can add non-secret request headers. Do not store tokens in `options`.

Do not copy an endpoint example to a different firmware/API generation without checking the official supplier documentation contained in that supplier package.

For native SDK routes, proprietary DLL/SO/JAR packages are **not redistributed**. Put licensed vendor artifacts in the matching `vendor-libs/` folder or configure the bridge/library path described in each route README.

## Official documentation

Each supplier contains:

```text
device-hub/packages/suppliers/<supplier>/DOCUMENTATION.md
device-hub/packages/suppliers/<supplier>/SDK_API_VERSIONING.md
device-hub/packages/suppliers/<supplier>/profiles/integration_versions.json
device-hub/packages/suppliers/<supplier>/integrations/<route>/README.md
```

The root index is:

```text
device-hub/docs/OFFICIAL_DOCUMENTATION_INDEX.md
```

## Credentials and security

- Vendor passwords are encrypted using Bortqala's existing `DeviceCredentialsCrypto`.
- Passwords are **not persisted** in the device hub JSON registry.
- Credentials are supplied to the hub only for a probe/sync request.
- Optional `DEVICE_HUB_API_KEY` protects hub management and normalized punch endpoints.
- Keep SDK libraries and vendor API secrets out of Git.

## Existing generic biometric devices

The old `/imports` generic JSON endpoint integration remains supported. This feature adds a richer supplier/version layer without breaking existing configured devices.

## Validation included in this package

- Python compilation for gateway + supplier packages.
- Route-selection tests for all seven supplier packages.
- Persistent device registry restart check.
- FastAPI resolve/register smoke test.
- End-to-end HTTP vendor event normalization test into Bortqala-compatible `punches` JSON.
- Idempotent apply-script test.
- Liquibase YAML parse checks.
- JSON profile validation.

A full Gradle + Angular build should still be run after applying the overlay to the complete branch checkout, because the connected GitHub environment does not expose a complete archive checkout to the local build runtime.
