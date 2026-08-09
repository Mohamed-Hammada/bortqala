# Anviz — crosschex-cloud-webhook

**Type:** `push-http`  
**Purpose:** CrossChex Cloud push/webhook ingestion boundary  
**Use when:** Cloud event delivery where enabled

## Official documentation

- [CrossChex Cloud help](https://support.anviz.com/hc/en-us/sections/41599769281049-CrossChex-Cloud)
- [Anviz official integration community](https://community.anviz.com/c/integration/46)

## Implementation boundary

This route is represented by `supplier_anviz.adapters.crosschex_cloud_webhook`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
