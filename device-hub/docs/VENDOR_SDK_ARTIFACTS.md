# Vendor SDK artifacts

This repository intentionally excludes manufacturer-owned binaries and documentation packages.

| Vendor | Integration requiring vendor artifact | Suggested location |
|---|---|---|
| ZKTeco | Standalone SDK / zkemkeeper, plcommpro, ZKFinger | `vendor-libs/zkteco/` |
| Hikvision | HCNetSDK / device integration SDK | `vendor-libs/hikvision/` |
| Dahua | NetSDK | `vendor-libs/dahua/` |
| Suprema | BioStar Device SDK (`BS_SDK_V2`) | `vendor-libs/suprema/` |
| Virdi | UCS SDK / partner libraries | `vendor-libs/virdi/` |
| Anviz | Anviz Standard SDK / partner SDK | `vendor-libs/anviz/` |
| Honeywell | Pro-Watch HSDK / partner SDK | `vendor-libs/honeywell/` |

Do not commit vendor DLLs/SOs unless your license explicitly permits redistribution.
