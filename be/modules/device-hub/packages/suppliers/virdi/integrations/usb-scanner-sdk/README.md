# Virdi — usb-scanner-sdk

**Type:** `native-sdk`  
**Purpose:** FOH/NScan/VScan/Hamster scanner SDK boundary  
**Use when:** USB scanners/modules

## Official documentation

- [UNIONBIOMETRICS / VIRDI Support Hub](https://support.unionbiometrics.com/)
- [Technical resources / product manuals](https://support.unionbiometrics.com/TechHub/TechResource)

## Implementation boundary

This route is represented by `supplier_virdi.adapters.usb_scanner_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
