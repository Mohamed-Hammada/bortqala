# Dahua — netsdk

**Type:** `native-sdk`  
**Purpose:** General NetSDK direct-device integration  
**Use when:** Direct Dahua device/controller integration

## Official documentation

- [Dahua software/SDK download center](https://www.dahuasecurity.com/download-center/softwares)
- [Dahua Partner SDK downloads (NetSDK)](https://previous-depp.dahuasecurity.com/integration/guide/download/sdk)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.netsdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
