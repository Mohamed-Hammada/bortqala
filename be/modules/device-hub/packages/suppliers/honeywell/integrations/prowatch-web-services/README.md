# Honeywell — prowatch-web-services

**Type:** `platform-http`  
**Purpose:** Pro-Watch Web Services API  
**Use when:** Third-party integration with Pro-Watch

## Official documentation

- [Honeywell Pro-Watch software](https://buildings.honeywell.com/ae/en/products/by-category/access-control/software/pro-watch-software)
- [Pro-Watch 4.3 Web Services API reference product page](https://buildings.honeywell.com/us/en/products/by-category/video-systems/software/pro-watch-4-3-software)
- [Pro-Watch Integrated Security Suite](https://buildings.honeywell.com/us/en/products/by-category/software/security-control-software/access-control-software/pro-watch-integrated-security-suite)

## Implementation boundary

This route is represented by `supplier_honeywell.adapters.prowatch_web_services`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
