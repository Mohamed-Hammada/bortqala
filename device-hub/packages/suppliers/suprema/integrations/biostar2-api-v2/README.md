# Suprema — biostar2-api-v2

**Type:** `platform-http`  
**Purpose:** BioStar 2 API v2  
**Use when:** BioStar 2 2.4+ legacy API server deployments

## Official documentation

- [BioStar SDK/API starter guide](https://support.supremainc.com/en/support/solutions/articles/24000005839)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.biostar2_api_v2`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
