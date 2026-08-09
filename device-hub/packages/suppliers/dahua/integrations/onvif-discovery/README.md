# Dahua — onvif-discovery

**Type:** `http-rest`  
**Purpose:** ONVIF discovery/status fallback where applicable  
**Use when:** Discovery/video metadata only; not a replacement for access APIs

## Official documentation

- [Dahua DSS integration/API](https://www.dahuasecurity.com/products/software/ecosystem/integration-with-dss)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.onvif_discovery`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
