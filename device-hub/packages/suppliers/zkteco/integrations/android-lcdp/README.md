# Zkteco — android-lcdp

**Type:** `native-sdk`  
**Purpose:** Android LCDP/device app SDK boundary  
**Use when:** Android ZKTeco terminals supporting LCDP

## Official documentation

- [ZKTeco SDK download center](https://www.zkteco.com/en/SDK)

## Implementation boundary

This route is represented by `supplier_zkteco.adapters.android_lcdp`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
