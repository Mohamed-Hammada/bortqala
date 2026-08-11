# Supplier package: Zkteco

Self-contained integration package for **Zkteco** attendance, biometric and access-control estates.

## Integration routes

- `zk-pull` — Legacy/standalone TCP/UDP PULL protocol, commonly TCP 4370
- `adms-ta-push` — ADMS / TA PUSH device-initiated HTTP
- `ac-push` — Access Control PUSH
- `standalone-sdk` — Standalone SDK / zkemkeeper COM bridge
- `plcommpro-pull` — Pull SDK / plcommpro controller SDK
- `zkbio-time-api` — ZKBio Time REST/API integration
- `zkbio-cvsecurity-api` — ZKBio CVSecurity 3rd-party API
- `zkbio-cvaccess-api` — ZKBio CVAccess platform integration
- `wdms-api` — ZKBio WDMS API/middleware
- `time-cloud-zlink` — ZKBio Time Cloud / Zlink boundary
- `zkfinger-sdk` — ZKFinger scanner SDK bridge
- `android-lcdp` — Android LCDP/device app SDK boundary
- `wiegand-rs485` — Wiegand/RS-485 reader integration via controller

## Package layout

- `src/supplier_zkteco/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Zkteco.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
