# Zkteco — zkbio-cvaccess-api

**Type:** `platform-http`  
**Purpose:** ZKBio CVAccess platform integration  
**Use when:** Sites using CVAccess

## Official documentation

- [ZKBio CVAccess](https://www.zkteco.com/en/ZKBio_CVAccess/ZKBio_CVAccess)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.zkbio_cvaccess_api`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
