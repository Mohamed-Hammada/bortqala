# Dahua — dss-sdk

**Type:** `native-sdk`  
**Purpose:** DSS SDK/native integration boundary  
**Use when:** DSS projects requiring SDK rather than OpenAPI

## Official documentation

- [Dahua DSS integration/API](https://www.dahuasecurity.com/products/software/ecosystem/integration-with-dss)
- [Dahua Partner SDK downloads (NetSDK)](https://previous-depp.dahuasecurity.com/integration/guide/download/sdk)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.dss_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
