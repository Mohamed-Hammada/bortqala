# Bortqala integration contract

The hub is deployed as a sidecar service and exposes a normalized contract to Bortqala.

## Management

- `GET /health`
- `GET /v1/suppliers`
- `GET /v1/suppliers/{vendor}/routes`
- `POST /v1/resolve-route`
- `GET /v1/devices`
- `POST /v1/devices`
- `PUT /v1/devices/{id}`
- `DELETE /v1/devices/{id}`
- `POST /v1/devices/{id}/probe`

## Attendance events

- `GET /v1/devices/{id}/punches?since=<ISO-8601>`

Response:

```json
{
  "punches": [
    {
      "deviceUserId": "E001",
      "employeeName": "Optional Name",
      "punchedAt": "2026-08-09T02:00:00Z",
      "rawLine": "{...vendor event...}"
    }
  ],
  "vendor": "hikvision",
  "route": "isapi",
  "deviceId": "..."
}
```

`Bortqala BiometricDeviceSyncService` consumes this existing shape, preserving its deduplication, employee mapping, import evidence, auditing and downstream attendance calculations.

If the selected supplier route is catalogued but the event reader is not implemented/configured, the endpoint returns HTTP `501` rather than fabricating support.
