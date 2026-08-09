# Suprema — g-sdk

**Type:** `grpc`  
**Purpose:** Suprema G-SDK through Device Gateway / Master Gateway  
**Use when:** BioStar 2 generation devices; NOT BioStar 1

## Official documentation

- [Suprema G-SDK documentation](https://supremainc.github.io/g-sdk/overview/)
- [G-SDK API reference](https://supremainc.github.io/g-sdk/api/)
- [G-SDK support / BioStar 1 exclusion](https://support.supremainc.com/en/support/solutions/articles/24000054528-g-sdk-introduction)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.g_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
