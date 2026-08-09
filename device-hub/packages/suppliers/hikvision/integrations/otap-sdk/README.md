# Hikvision — otap-sdk

**Type:** `native-sdk`  
**Purpose:** Open Things Access Protocol SDK  
**Use when:** Newer device-to-platform / no-fixed-IP integration

## Official documentation

- [Hikvision Open Platform - device integration](https://open.hikvision.com/osp)
- [OTAP SDK download/example](https://open.hikvision.com/download/5cda567cf47ae80dd41a54b3?id=2bb0d993a2804277aa2974550732003f&type=10)

## Implementation boundary

This route is represented by `supplier_hikvision.adapters.otap_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
