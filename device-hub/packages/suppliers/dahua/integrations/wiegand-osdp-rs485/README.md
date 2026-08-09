# Dahua — wiegand-osdp-rs485

**Type:** `bus`  
**Purpose:** Reader/controller physical protocol boundary  
**Use when:** Reader-only hardware through controllers

## Official documentation

- [DSS Professional](https://www.dahuasecurity.com/products/software/software-products/dss-professional)

## Implementation boundary

This route is represented by `supplier_dahua.adapters.wiegand_osdp_rs485`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
