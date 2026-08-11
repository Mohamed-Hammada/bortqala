# Suprema — biostar2-local-api

**Type:** `platform-http`  
**Purpose:** BioStar 2 New Local REST API / Swagger  
**Use when:** BioStar 2 >= 2.7.10 platform integration

## Official documentation

- [BioStar SDK/API starter guide](https://support.supremainc.com/en/support/solutions/articles/24000005839)
- [BioStar 2 New Local API / Swagger guide](https://kb.supremainc.com/knowledge/doku.php?id=en:how_to_use_swagger_ui_and_postman_for_biostar_2_new_local_api)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.biostar2_local_api`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
