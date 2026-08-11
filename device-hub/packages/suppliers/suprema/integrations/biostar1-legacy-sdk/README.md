# Suprema — biostar1-legacy-sdk

**Type:** `native-sdk`  
**Purpose:** BioStar 1 legacy device SDK boundary  
**Use when:** Gen-1/BioStar 1 devices excluded by G-SDK

## Official documentation

- [G-SDK support / BioStar 1 exclusion](https://support.supremainc.com/en/support/solutions/articles/24000054528-g-sdk-introduction)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.biostar1_legacy_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
