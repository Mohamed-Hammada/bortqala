# Backend auxiliary modules

## device-hub

`device-hub/` is Bortqala's backend-owned vendor/protocol adapter runtime.
It is not a second ERP backend and is not an Angular API.

```text
Angular
  -> /api/v1/device-integrations
  -> Spring Boot
  -> VendorHubClient
  -> http://device-hub:8090 (Docker internal network)
  -> vendor device/platform
```

Rules:

1. Angular never calls device-hub directly.
2. Bortqala remains the system of record.
3. Device credentials remain encrypted in Bortqala.
4. Device Hub stores route/protocol metadata, not reusable device secrets.
5. Normalized punches return to the existing `BiometricDeviceSyncService`.
