# Hikvision — usb-sdk

**Type:** `native-sdk`  
**Purpose:** USB SDK  
**Use when:** USB readers/card issuers/identity devices

## Official documentation

- [Hikvision Open Platform - device integration](https://open.hikvision.com/osp)
- [Hikvision SDK/download portal](https://open.hikvision.com/download/5cda567cf47ae80dd41a54b3)

## Implementation boundary

This route is represented by `supplier_hikvision.adapters.usb_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
