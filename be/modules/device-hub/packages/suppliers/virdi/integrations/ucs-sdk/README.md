# Virdi — ucs-sdk

**Type:** `native-sdk`  
**Purpose:** UCS SDK direct/integration bridge  
**Use when:** Supported VIRDI/UBio terminals and biometric operations

## Official documentation

- [UNIONBIOMETRICS / VIRDI Support Hub](https://support.unionbiometrics.com/)
- [API, UCS SDK, UNIS FAQ index](https://support.unionbiometrics.com/TechHub/Faq)

## Implementation boundary

This route is represented by `supplier_virdi.adapters.ucs_sdk`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
