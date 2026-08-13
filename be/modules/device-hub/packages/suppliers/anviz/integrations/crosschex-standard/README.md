# Anviz — crosschex-standard

**Type:** `platform-native`  
**Purpose:** CrossChex Standard local integration/export boundary  
**Use when:** Legacy/on-prem CrossChex deployments

## Official documentation

- [Anviz software + SDK downloads](https://support.anviz.com/hc/en-us/articles/41638799732377-Anviz-Software)

## Implementation boundary

This route is represented by `supplier_anviz.adapters.crosschex_standard`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
