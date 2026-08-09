# Dahua — dss-openapi

**Type:** `platform-http`  
**Purpose:** DSS Professional OpenAPI  
**Use when:** DSS-managed access/time attendance estates

## Official documentation

- [Dahua DSS integration/API](https://www.dahuasecurity.com/products/software/ecosystem/integration-with-dss)
- [DSS Professional](https://www.dahuasecurity.com/products/software/software-products/dss-professional)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.dss_openapi`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
