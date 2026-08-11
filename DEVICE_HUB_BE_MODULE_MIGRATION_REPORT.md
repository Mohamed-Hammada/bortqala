# Device Hub → Backend Module Migration Report

Generated: 2026-08-11T22:50:05+03:00

## Architecture

```text
Angular
  -> /api/v1/device-integrations
  -> Spring Boot DeviceIntegrationService
  -> VendorHubClient
  -> be/modules/device-hub
  -> vendor device/platform
  -> normalized punches
  -> BiometricDeviceSyncService
  -> attendance/payroll
```

## Changed paths

- `be\compose.yaml`
- `docker-compose.yml`
- `docker-compose.prod.yml`
- `.env.development.example`
- `.env.production.example`
- `fe\src\app\features\device-integrations\device-integrations.page.html`
- `fe\src\app\features\device-integrations\device-integrations.page.scss`
- `README_DEVICE_INTEGRATIONS.md`
- `be\modules\README.md`

## Enforced communication rules

- Frontend uses only `/api/v1/device-integrations`.
- Spring Boot uses `BEMO_DEVICE_HUB_BASE_URL`.
- Docker uses internal `http://device-hub:8090`.
- Production requires `DEVICE_HUB_API_KEY`.
- Production does not publish the Device Hub port.
- Existing Bortqala encrypted credentials and biometric sync remain authoritative.

## Warnings

- None.

## Recommended validation

```bash
docker compose -f docker-compose.yml config
docker compose -f be/compose.yaml config
cd be && ./gradlew test
cd ../fe && npm run build
```

Open `/imports/device-integrations` after startup.
