# Supplier package: Hikvision

Self-contained integration package for **Hikvision** attendance, biometric and access-control estates.

## Integration routes

- `isapi` — ISAPI REST/XML direct-device API
- `hcnet-sdk` — Device Network SDK / HCNetSDK private protocol
- `isup-sdk` — ISUP SDK device-to-platform registration
- `device-gateway` — Device Gateway HTTP/HTTPS + RTSP interface over ISUP 5.0
- `otap-sdk` — Open Things Access Protocol SDK
- `hikcentral-openapi` — HikCentral/Open Platform API boundary
- `sadp-discovery` — SADP discovery/activation utility SDK
- `usb-sdk` — USB SDK
- `wiegand-osdp` — Reader/controller bus integration

## Package layout

- `src/supplier_hikvision/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Hikvision.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
