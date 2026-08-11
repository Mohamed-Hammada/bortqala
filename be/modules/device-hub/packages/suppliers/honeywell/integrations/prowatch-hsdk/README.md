# Honeywell — prowatch-hsdk

**Type:** `native-sdk`  
**Purpose:** Honeywell Software Development Kit / PWHSDK  
**Use when:** Deep Pro-Watch integration requiring HSDK license

## Official documentation

- [Honeywell Pro-Watch software](https://buildings.honeywell.com/ae/en/products/by-category/access-control/software/pro-watch-software)
- [Honeywell PWHSDK / HSDK](https://buildings.honeywell.com/ae/en/products/by-category/access-control/software/pwhsdk)
- [Pro-Watch HSDK training](https://buildings.honeywell.com/gb/en/brands/our-brands/security/services/his-integrator-training)

## Implementation boundary

This route is represented by `supplier_honeywell.adapters.prowatch_hsdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
