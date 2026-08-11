# Supplier package: Anviz

Self-contained integration package for **Anviz** attendance, biometric and access-control estates.

## Integration routes

- `anviz-standard-sdk` — Anviz Standard SDK direct-device integration
- `crosschex-cloud-api` — CrossChex Cloud developer API boundary
- `crosschex-cloud-webhook` — CrossChex Cloud push/webhook ingestion boundary
- `cloudkit` — Anviz CloudKit/integration-tool boundary
- `crosschex-standard` — CrossChex Standard local integration/export boundary
- `usb-export` — USB/import-export fallback
- `wiegand` — Wiegand reader/controller integration

## Package layout

- `src/supplier_anviz/` — version-aware router and adapter boundaries
- `integrations/` — one folder per integration method, each with official documentation links
- `profiles/device_routes.json` / `.csv` — model/generation routing rules
- `DOCUMENTATION.md` — supplier official documentation index
- `COMPATIBILITY.md` — readable version/device routing matrix
- `vendor-libs/` — location for licensed proprietary SDK binaries (not redistributed)
- `examples/` — sample configuration
- `tests/` — route selector self-test

## Core rule

**Never select an integration only because the device says Anviz.** Select by model + firmware/platform generation + topology + detected capabilities. Unknown devices use safe-probe fallbacks and remain `UNVERIFIED` until tested on hardware.
