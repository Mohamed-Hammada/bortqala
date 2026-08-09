# Supplier package: Dahua

Self-contained integration package for **Dahua** attendance, biometric and access-control estates.

## Integration routes

- `netsdk` — General NetSDK direct-device integration
- `dss-openapi` — DSS Professional OpenAPI
- `dss-sdk` — DSS SDK/native integration boundary
- `dss-dip-bridge` — DSS Integration Platform / bridge boundary
- `onvif-discovery` — ONVIF discovery/status fallback where applicable
- `wiegand-osdp-rs485` — Reader/controller physical protocol boundary

## Package layout

- `src/supplier_dahua/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Dahua.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
