# Virdi — tcp-terminal

**Type:** `direct-protocol`  
**Purpose:** Vendor terminal TCP boundary via licensed UCS/UNIS components  
**Use when:** Older terminals where platform is not used

## Official documentation

- [API, UCS SDK, UNIS FAQ index](https://support.unionbiometrics.com/TechHub/Faq)

## Implementation boundary

This route is represented by `supplier_virdi.adapters.tcp_terminal`. Public HTTP/TCP probes are implemented where safe. Native/proprietary SDK calls are isolated behind a bridge and require the supplier's licensed binaries/manuals.

## Version rule

Do not enable this route by brand name alone. Select it from `profiles/device_routes.json` after identifying model, firmware/platform generation, topology and enabled capabilities.
