# Suprema — wiegand-rs485

**Type:** `bus`  
**Purpose:** Wiegand/RS485 peripheral integration  
**Use when:** Reader/peripheral routes

## Official documentation

- [G-SDK API reference](https://supremainc.github.io/g-sdk/api/)
- [BioStar Device SDK API references](https://kb.supremainc.com/bs2sdk/doku.php?id=en:api_references)

## Implementation boundary

This route is represented by `supplier_suprema.adapters.wiegand_rs485`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
