# ZKTeco Universal Gateway

A protocol-adapter gateway for integrating ZKTeco attendance, access-control, biometric, and platform APIs behind one normalized API.

> **Important:** no single ZKTeco protocol covers every device and firmware. This repository treats “universal support” as a continuously tested compatibility program built from independent adapters, capability detection, firmware profiles, and device conformance tests.

## Supported integration paths

| Adapter | Intended device/software families | Initial status |
|---|---|---|
| `zk-pull` | Legacy/standalone terminals reachable by TCP/UDP, commonly port 4370 | Transport probe implemented; binary commands pending device fixtures |
| `adms-push` | Devices configured with Cloud Server / ADMS / PUSH over HTTP(S) | Ingress endpoints and raw-event capture implemented; command/parser profiles pending fixtures |
| `zkbio-time-api` | ZKBio Time API deployments | Generic authenticated HTTP adapter skeleton |
| `zkbio-cvsecurity-api` | ZKBio CVSecurity REST API deployments | Generic authenticated HTTP adapter skeleton |
| `wdms-api` | ZKBio WDMS middleware deployments | Generic authenticated HTTP adapter skeleton |
| `windows-sdk-bridge` | Models requiring official Windows COM/DLL Standalone/PULL SDKs | Separate .NET bridge skeleton; vendor binaries are not included |

## Why adapters instead of one SDK?

ZKTeco publishes several integration routes: device-initiated PUSH/ADMS, Windows-oriented standalone/PULL SDKs, ZKBio Time API, ZKBio CVSecurity API, WDMS middleware, and separate biometric scanner/module SDKs. Device pages also vary by model and firmware: the same product family can advertise Standalone SDK, new PULL SDK, TA PUSH, AC PUSH, ADMS, or platform software integration.

The gateway therefore normalizes devices into capabilities rather than assuming that every model supports the same commands.

## Architecture

```text
ERP / HR / Access application
            |
      REST / Webhooks
            |
+------------------------------+
| ZKTeco Universal Gateway     |
| - device registry            |
| - protocol detection         |
| - normalized users/events    |
| - retry/idempotency/audit     |
+------------------------------+
   |       |       |       |
 PULL    ADMS   ZKBio APIs  Windows SDK bridge
   |       |       |       |
 standalone and platform-managed ZKTeco devices
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/COMPATIBILITY_MATRIX.md](docs/COMPATIBILITY_MATRIX.md), and [docs/OFFICIAL_INTEGRATION_PATHS.md](docs/OFFICIAL_INTEGRATION_PATHS.md).

## Stack

- Java 21
- Spring Boot 4.1
- Maven multi-module build
- Optional .NET 8 Windows SDK bridge
- PostgreSQL/Redis placeholders in Docker Compose

## Run locally

```bash
mvn clean verify
mvn -pl gateway-app spring-boot:run
```

The gateway starts on port `8080` by default.

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/compatibility/protocols
```

Register a direct PULL candidate:

```bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H 'Content-Type: application/json' \
  -d '{
    "label": "Main office clock",
    "preferredProtocol": "ZK_PULL",
    "host": "192.168.1.201",
    "port": 4370,
    "properties": {}
  }'
```

Then probe it:

```bash
curl -X POST http://localhost:8080/api/v1/devices/{device-id}/probe
```

## ADMS/PUSH test endpoints

The experimental ingress exposes the commonly used device callback paths:

- `GET|POST /iclock/cdata?SN=<serial>`
- `GET /iclock/getrequest?SN=<serial>`
- `POST /iclock/devicecmd?SN=<serial>`

Raw payloads are retained only in memory in this starter. Production persistence, authentication, payload limits, and per-firmware parsing must be completed before deployment.

## Definition of “supported”

A model/firmware combination is not marked supported until it passes the conformance suite for the capabilities claimed by that profile. See [docs/DEVICE_ONBOARDING.md](docs/DEVICE_ONBOARDING.md).

## Vendor SDK policy

Official DLLs, COM components, scanner SDKs, firmware, manuals requiring login, and licensed API packages must not be committed. Place deployment-only artifacts under an ignored directory and confirm redistribution rights. See [docs/VENDOR_ARTIFACTS.md](docs/VENDOR_ARTIFACTS.md).

## Roadmap

1. Collect real device model, platform, firmware, communication mode, and sample payloads.
2. Complete read-only attendance/user synchronization for the first device family.
3. Add idempotent write operations and command queues.
4. Add firmware-specific parsers and a hardware-in-the-loop compatibility lab.
5. Publish a generated compatibility matrix from conformance results.

## License

Apache-2.0 for this repository's original source code. ZKTeco vendor SDKs and documentation remain subject to their own licenses.
