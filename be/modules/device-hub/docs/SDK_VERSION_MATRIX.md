# SDK version policy matrix

This matrix is generated from each supplier package. `implementation_status` distinguishes a usable generic transport/probe from a complete vendor-specific business wrapper.

| Supplier | Route | Version policy | Version constraint | Status |
|---|---|---|---|---|
| zkteco | `android-lcdp` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| zkteco | `plcommpro-pull` | controller-firmware-sdk-matched | `*` | sdk-bridge-probe |
| zkteco | `standalone-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| zkteco | `zkfinger-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| hikvision | `hcnet-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| hikvision | `isup-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| hikvision | `otap-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| hikvision | `sadp-discovery` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| hikvision | `usb-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| dahua | `dss-sdk` | dss-server-sdk-matched | `*` | sdk-bridge-probe |
| dahua | `netsdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| suprema | `biostar-device-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| suprema | `biostar1-legacy-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| suprema | `g-sdk` | capability-or-vendor-matrix | `>=1.0.0,<2.0.0` | bridge-probe |
| suprema | `svp-android-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| virdi | `ucs-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| virdi | `usb-scanner-sdk` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| anviz | `anviz-standard-sdk` | device-firmware-sdk-matched | `*` | sdk-bridge-probe |
| anviz | `cloudkit` | capability-or-vendor-matrix | `*` | sdk-bridge-probe |
| honeywell | `prowatch-hsdk` | server-sdk-matched | `*` | sdk-bridge-probe |

See the supplier `DOCUMENTATION.md` and `profiles/integration_versions.json` for official documentation links and route-specific notes.
