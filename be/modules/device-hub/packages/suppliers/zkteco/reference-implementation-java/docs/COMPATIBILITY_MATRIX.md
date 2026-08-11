# Compatibility Matrix

This file is the human-readable starter. Production should generate it from conformance test results.

| Family / route | Examples | Probe | Read users | Read attendance | Realtime | Write users | Biometrics | Access commands | Status |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| Legacy standalone PULL | ZEM-family terminals, selected F/MA/SC/iClock devices | Transport only | Planned | Planned | Planned | Planned | Profile-specific | Profile-specific | Experimental |
| ADMS / TA PUSH | Modern attendance devices with Cloud Server/ADMS | Inbound heartbeat | Parser pending | Raw capture | Raw capture | Command queue pending | Profile-specific | Profile-specific | Experimental |
| AC PUSH | Access-control PUSH models | Inbound heartbeat | Parser pending | N/A/optional | Raw capture | Command queue pending | Profile-specific | Planned | Experimental |
| ZKBio Time API | Licensed ZKBio Time deployment | Generic HTTP | Planned | Planned | API-dependent | Planned | API-dependent | N/A | Skeleton |
| ZKBio CVSecurity API | Licensed CVSecurity API deployment | Generic HTTP | Planned | Planned | API-dependent | Planned | API-dependent | Planned | Skeleton |
| WDMS API | Licensed WDMS middleware | Generic HTTP | Planned | Planned | API-dependent | Planned | API-dependent | Device-dependent | Skeleton |
| Windows Standalone/PULL SDK | Official vendor COM/DLL route | Bridge health only | Pending SDK | Pending SDK | Pending SDK | Pending SDK | SDK-dependent | SDK-dependent | Skeleton |
| ZKFinger/scanner SDKs | USB fingerprint scanners/modules | Not in phase 1 | N/A | N/A | N/A | N/A | Separate capture adapter | N/A | Backlog |

## Support levels

- **Skeleton:** configuration and interface only.
- **Experimental:** basic communication exists but no certified firmware profile.
- **Read-only:** identity/users/events pass fixtures and hardware tests.
- **Certified:** all declared capabilities pass repeatable hardware-in-the-loop tests.
- **Degraded:** known firmware defect or partial command coverage.
- **Unsupported:** confirmed incompatible or blocked by licensing/hardware.
