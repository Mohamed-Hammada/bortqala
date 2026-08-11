# Anviz — usb-export

**Type:** `file`  
**Purpose:** USB/import-export fallback  
**Use when:** Offline terminals / restricted network environments

## Official documentation

- [Anviz manuals](https://support.anviz.com/hc/en-us/articles/41638698285081-Manual)

## Implementation boundary

This route is represented by `supplier_anviz.adapters.usb_export`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
