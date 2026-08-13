# SDK + API version-aware routing

The hub treats **integration route** and **integration version** as separate facts.

## Required inventory

For every deployment record: supplier, model, firmware, platform/server version, selected route, exact SDK/API version, OS, architecture, vendor library checksum, documentation/release-note reference, and conformance-test status.

## Selection states

- `COMPATIBLE` — known constraints are satisfied.
- `NEEDS_SDK_VERSION` — SDK route is possible but no exact installed SDK version was provided.
- `NEEDS_API_VERSION` — an explicitly versioned API requires its API version.
- `NEEDS_SERVER_VERSION` — server/platform version controls availability.
- `NEEDS_VENDOR_MATRIX` — vendor documentation/release matrix is required before compatibility can be claimed.
- `INCOMPATIBLE` — supplied version/OS/architecture violates a known rule.

This deliberately prevents a common failure mode: installing the latest SDK and assuming it supports every old device/firmware.
