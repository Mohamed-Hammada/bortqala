# Honeywell — prowatch-db-integration

**Type:** `platform-native`  
**Purpose:** Controlled Pro-Watch data integration boundary  
**Use when:** Only where explicitly supported by project/vendor

## Official documentation

- [Honeywell Pro-Watch software](https://buildings.honeywell.com/ae/en/products/by-category/access-control/software/pro-watch-software)

## Implementation boundary

This route is represented by `supplier_honeywell.adapters.prowatch_db_integration`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
