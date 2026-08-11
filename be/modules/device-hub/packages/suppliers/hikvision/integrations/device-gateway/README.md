# Hikvision — device-gateway

**Type:** `platform-http`  
**Purpose:** Device Gateway HTTP/HTTPS + RTSP interface over ISUP 5.0  
**Use when:** Gateway-managed MinMoe/access deployments

## Official documentation

- [Hikvision Open Platform - device integration](https://open.hikvision.com/osp)

## Implementation boundary

This route is represented by `supplier_hikvision.adapters.device_gateway`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
