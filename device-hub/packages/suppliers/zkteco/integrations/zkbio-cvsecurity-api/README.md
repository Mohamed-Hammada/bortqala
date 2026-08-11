# Zkteco — zkbio-cvsecurity-api

**Type:** `platform-http`  
**Purpose:** ZKBio CVSecurity 3rd-party API  
**Use when:** Sites using CVSecurity

## Official documentation

- [ZKBio CVSecurity API](https://www.zkteco.com/en/ZKBio_CVSecurity_API/ZKBioCVSecurity_API)
- [ZKBio CVSecurity](https://www.zkteco.com/en/ZKBio_CVSecurity/ZKBio_CVSecurity)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.zkbio_cvsecurity_api`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
