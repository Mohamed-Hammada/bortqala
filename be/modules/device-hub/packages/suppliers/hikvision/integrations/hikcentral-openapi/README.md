# Hikvision — hikcentral-openapi

**Type:** `platform-http`  
**Purpose:** HikCentral/Open Platform API boundary  
**Use when:** Sites centrally managed by HikCentral

## Official documentation

- [Hikvision Open Platform - device integration](https://open.hikvision.com/osp)

## Implementation boundary

This route is represented by `supplier_hikvision.adapters.hikcentral_openapi`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
