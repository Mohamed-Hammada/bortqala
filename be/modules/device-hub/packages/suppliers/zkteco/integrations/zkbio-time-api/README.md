# Zkteco — zkbio-time-api

**Type:** `platform-http`  
**Purpose:** ZKBio Time REST/API integration  
**Use when:** Sites using ZKBio Time

## Official documentation

- [ZKBio Time API](https://www.zkteco.com/en/ZKBioTime_API/ZKBioTime_API)
- [ZKTeco download center - other documents](https://www.zkteco.com/en/other_document)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.zkbio_time_api`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
