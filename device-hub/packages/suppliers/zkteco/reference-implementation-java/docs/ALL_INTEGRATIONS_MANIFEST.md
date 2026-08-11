# All integration paths included in this monorepo

| Path | Repository component | What is implemented | External requirement |
|---|---|---|---|
| Classic/Standalone PULL TCP | `adapter-zk-pull` | packet framing/checksum, TCP session, connect/auth, identity options, clock read/write, enable/disable, reboot, door unlock, raw command foundation | device on LAN |
| Classic PULL UDP | `adapter-zk-pull` | same codec/transport, select with `properties.udp=true` | device UDP enabled |
| TA PUSH / ADMS | `adapter-adms-push` + `/iclock/*` | cdata ingress, last-seen, parser, getrequest command queue, devicecmd acknowledgement capture | device Cloud Server/ADMS config |
| AC PUSH | same PUSH ingress architecture | separate protocol/profile classification; capture-first model for firmware-specific records | AC PUSH firmware/docs |
| ZKBio Time API | `adapter-http-platforms` | configurable authenticated HTTP connector | licensed API credentials/version manual |
| ZKBio CVSecurity API | `adapter-http-platforms` | configurable authenticated HTTP connector | licensed API credentials/version manual |
| ZKBio CVAccess | `adapter-http-platforms` | platform connector point | platform/version API access |
| ZKBio WDMS | `adapter-http-platforms` | platform connector point | WDMS deployment/API access |
| ZKBio Time Cloud | `adapter-http-platforms` | cloud connector point | tenant/API access supplied by ZKTeco/partner |
| ZKBio Zlink | `adapter-http-platforms` | cloud connector point | tenant/API access supplied by ZKTeco/partner |
| Standalone SDK / zkemkeeper | `windows-sdk-bridge` | COM dynamic integration, probe, users, attendance | vendor COM runtime installed |
| New PULL / plcommpro | `windows-sdk-bridge` | connect, GetDeviceData, SetDeviceData, ControlDevice | `plcommpro.dll` licensed runtime |
| ZKFinger scanners | `windows-sdk-bridge` + vendor layout | native initialization/device enumeration, deployment hooks | ZKFinger SDK/runtime |
| ZKFinger Linux | `vendor/zkfinger/linux` contract | deployment slot/documented integration boundary | official Linux SDK |
| ZKFinger Android | `vendor/android` contract | deployment slot/documented integration boundary | official Android SDK |
| Visible Light/biometric modules | `vendor/visible-light-sdk` contract | integration boundary/profile | official module SDK |
| Wiegand/RS485 readers | profiles/documentation | represented as controller-attached reader capabilities | supported controller/panel |
| USB import/export | profiles/documentation | offline fallback classification | model-specific export format |

## Meaning of “all versions”

The code includes every major integration *route*, but no honest implementation can guarantee every historic OEM firmware without hardware samples. ZKTeco has firmware variants where the same model can expose different SDK/PUSH behavior. The repo therefore separates **adapter availability** from **model+firmware certification**. Add each real firmware to the conformance matrix after testing.
