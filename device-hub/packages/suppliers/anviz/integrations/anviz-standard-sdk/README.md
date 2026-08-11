# Anviz — anviz-standard-sdk

**Type:** `native-sdk`  
**Purpose:** Anviz Standard SDK direct-device integration  
**Use when:** CrossChex Standard-era and direct LAN devices

## Official documentation

- [Anviz software + SDK downloads](https://support.anviz.com/hc/en-us/articles/41638799732377-Anviz-Software)

## Implementation boundary

This route is represented by `supplier_anviz.adapters.anviz_standard_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
