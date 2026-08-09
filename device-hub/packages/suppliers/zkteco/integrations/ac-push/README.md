# Zkteco — ac-push

**Type:** `push-http`  
**Purpose:** Access Control PUSH  
**Use when:** Access-control PUSH firmware

## Official documentation

- [PUSH SDK](https://www.zkteco.com/en/PUSHSDK)
- [F18 example showing AC PUSH / TA PUSH / Standalone SDK variants](https://zkteco.com/en/FSeries/F18)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.ac_push`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
