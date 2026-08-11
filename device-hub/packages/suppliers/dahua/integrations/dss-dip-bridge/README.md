# Dahua — dss-dip-bridge

**Type:** `platform-http`  
**Purpose:** DSS Integration Platform / bridge boundary  
**Use when:** Cross-system / third-party access integration

## Official documentation

- [Dahua DSS integration/API](https://www.dahuasecurity.com/products/software/ecosystem/integration-with-dss)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.dss_dip_bridge`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
