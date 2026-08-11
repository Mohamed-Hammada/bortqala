# Virdi — wiegand-rs485

**Type:** `bus`  
**Purpose:** Wiegand/RS-485 integration  
**Use when:** Reader/controller/peripheral integration

## Official documentation

- [API, UCS SDK, UNIS FAQ index](https://support.unionbiometrics.com/TechHub/Faq)
- [Technical courses including RS-485/Wiegand](https://support.unionbiometrics.com/TechHub/TechCourse)

## Implementation boundary

This route is represented by `supplier_virdi.adapters.wiegand_rs485`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
