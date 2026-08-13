# Supplier package: Suprema

Self-contained integration package for **Suprema** attendance, biometric and access-control estates.

## Integration routes

- `g-sdk` — Suprema G-SDK through Device Gateway / Master Gateway
- `biostar-device-sdk` — BioStar Device SDK (formerly BioStar 2 Device SDK)
- `biostar2-local-api` — BioStar 2 New Local REST API / Swagger
- `biostar2-api-v2` — BioStar 2 API v2
- `biostar2-api-v1` — BioStar 2 API v1
- `biostar1-legacy-sdk` — BioStar 1 legacy device SDK boundary
- `svp-android-sdk` — Suprema Versatile Platform Android SDK boundary
- `wiegand-rs485` — Wiegand/RS485 peripheral integration

## Package layout

- `src/supplier_suprema/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Suprema.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
