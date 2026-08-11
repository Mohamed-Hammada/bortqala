# Zkteco — wdms-api

**Type:** `platform-http`  
**Purpose:** ZKBio WDMS API/middleware  
**Use when:** Standalone PUSH estates behind WDMS

## Official documentation

- [ZKBio WDMS](https://www.zkteco.com/en/ZKBio_WDMS/ZKBio_WDMS)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.wdms_api`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
