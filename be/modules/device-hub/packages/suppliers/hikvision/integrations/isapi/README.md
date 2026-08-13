# Hikvision — isapi

**Type:** `http-rest`  
**Purpose:** ISAPI REST/XML direct-device API  
**Use when:** Fixed-IP LAN/WAN devices that advertise ISAPI

## Official documentation

- [Hikvision Open Platform - device integration](https://open.hikvision.com/osp)

## Implementation boundary

This route is represented by `supplier_hikvision.adapters.isapi`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
