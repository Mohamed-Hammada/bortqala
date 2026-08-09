# SUPREMA version-aware integration policy

This package does **not** select an SDK only from the supplier/model name. Runtime selection considers model, device firmware, server/platform version, exact installed SDK version, API version/capability, OS and CPU architecture.

## Files

- `profiles/device_routes.json` — device/generation rules.
- `profiles/integration_versions.json` — SDK/API/protocol version policies and official documentation.
- `src/supplier_suprema/versioning.py` — version constraint evaluator.
- `src/supplier_suprema/router.py` — strict route selection.

## SDK rule

SDK routes require an explicit installed SDK version. If the public vendor documentation does not provide a complete historical device/firmware compatibility table, the route returns `NEEDS_VENDOR_MATRIX`; it is not declared compatible by guesswork. Put vendor release notes/matrices under `vendor-libs/metadata/` and update `integration_versions.json`.

## API rule

APIs are separately versioned. Some use explicit v1/v2 paths, some are server-version coupled, and others (for example capability-style device APIs) are negotiated from the device. The selected API version and server version must be stored with every configured device/platform.
