# Suprema — svp-android-sdk

**Type:** `native-sdk`  
**Purpose:** Suprema Versatile Platform Android SDK boundary  
**Use when:** Supported Android/SVP device-side applications

## Official documentation

- [BioStar SDK/API starter guide](https://support.supremainc.com/en/support/solutions/articles/24000005839)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.svp_android_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
