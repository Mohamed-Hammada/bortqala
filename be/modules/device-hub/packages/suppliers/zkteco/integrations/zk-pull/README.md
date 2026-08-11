# Zkteco — zk-pull

**Type:** `direct-protocol`  
**Purpose:** Legacy/standalone TCP/UDP PULL protocol, commonly TCP 4370  
**Use when:** LAN direct devices / classic time-attendance

## Official documentation

- [ZKTeco SDK download center](https://www.zkteco.com/en/SDK)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.zk_pull`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
