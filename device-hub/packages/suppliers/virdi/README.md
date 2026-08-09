# Supplier package: Virdi

Self-contained integration package for **Virdi** attendance, biometric and access-control estates.

## Integration routes

- `alpeta-api` — UBio Alpeta API
- `ucs-sdk` — UCS SDK direct/integration bridge
- `unis-server` — UNIS legacy server integration boundary
- `accessmanager-pro` — AccessManager Pro integration boundary
- `tcp-terminal` — Vendor terminal TCP boundary via licensed UCS/UNIS components
- `usb-scanner-sdk` — FOH/NScan/VScan/Hamster scanner SDK boundary
- `wiegand-rs485` — Wiegand/RS-485 integration

## Package layout

- `src/supplier_virdi/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Virdi.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
