# Zkteco — time-cloud-zlink

**Type:** `cloud-http`  
**Purpose:** ZKBio Time Cloud / Zlink boundary  
**Use when:** Cloud-connected deployments

## Official documentation

- [ZKTeco download center - other documents](https://www.zkteco.com/en/other_document)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.time_cloud_zlink`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
