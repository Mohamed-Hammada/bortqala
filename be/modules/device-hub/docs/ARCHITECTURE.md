# Architecture

```text
ERP / HR / Payroll / Access App
            |
            v
+-------------------------------+
| Unified Gateway :8090         |
| normalized Person/Event/Door  |
+-------------------------------+
   |      |      |      |      |
   v      v      v      v      v
 ZKTeco  Hik   Dahua  Suprema  ...
 direct  ISAPI DSS    BS2/GSDK
 /ADMS   /SDK  /SDK   /SDK
```

## Adapter selection

A device record contains `vendor`, `model`, optional `firmware`, `host`, and an ordered list of candidate integration routes. The router tries the configured route first and can fall back to another route only when explicitly enabled.

## Normalized entities

- `Device`
- `Person`
- `Credential` (card/PIN/mobile)
- `BiometricReference` (metadata only; templates remain vendor-specific unless explicitly handled)
- `AccessEvent`
- `AttendanceEvent`
- `Door`
- `DeviceCommand`

## Native SDK isolation

Native SDKs are isolated behind sidecars because many vendor SDKs are Windows-only, architecture-sensitive, license-controlled, or ship unmanaged libraries. A crash in an SDK process should not crash the main ERP integration gateway.
