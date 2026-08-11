# Suprema — biostar-device-sdk

**Type:** `native-sdk`  
**Purpose:** BioStar Device SDK (formerly BioStar 2 Device SDK)  
**Use when:** Direct BioStar 2-generation readers/controllers

## Official documentation

- [BioStar Device SDK documentation](https://kb.supremainc.com/bs2sdk/doku.php?id=en:start)
- [BioStar Device SDK API references](https://kb.supremainc.com/bs2sdk/doku.php?id=en:api_references)
- [BioStar SDK/API starter guide](https://support.supremainc.com/en/support/solutions/articles/24000005839)
- [Suprema Download Center](https://download.supremainc.com/download-center/pages/login.asp)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.biostar_device_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
