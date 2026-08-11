# Virdi — accessmanager-pro

**Type:** `platform-native`  
**Purpose:** AccessManager Pro integration boundary  
**Use when:** AccessManager-managed deployments

## Official documentation

- [API, UCS SDK, UNIS FAQ index](https://support.unionbiometrics.com/TechHub/Faq)
- [Technical resources / product manuals](https://support.unionbiometrics.com/TechHub/TechResource)

## Implementation boundary

This route is represented by `supplier_virdi.adapters.accessmanager_pro`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
