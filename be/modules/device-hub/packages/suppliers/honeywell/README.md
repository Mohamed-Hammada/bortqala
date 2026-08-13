# Supplier package: Honeywell

Self-contained integration package for **Honeywell** attendance, biometric and access-control estates.

## Integration routes

- `prowatch-web-services` — Pro-Watch Web Services API
- `prowatch-hsdk` — Honeywell Software Development Kit / PWHSDK
- `prowatch-db-integration` — Controlled Pro-Watch data integration boundary
- `panel-through-prowatch` — Honeywell/Mercury panel operations through Pro-Watch
- `reader-bus` — Wiegand/OSDP reader path through supported panels

## Package layout

- `src/supplier_honeywell/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Honeywell.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
