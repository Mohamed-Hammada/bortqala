# Anviz — cloudkit

**Type:** `native-sdk`  
**Purpose:** Anviz CloudKit/integration-tool boundary  
**Use when:** Projects provisioned with vendor integration kit

## Official documentation

- [Anviz official integration community](https://community.anviz.com/c/integration/46)

## Implementation boundary

This route is represented by `supplier_anviz.adapters.cloudkit`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
