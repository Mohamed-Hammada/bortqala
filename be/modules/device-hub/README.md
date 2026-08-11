# Universal Biometric & Access Hub — Supplier-Packaged Edition

This edition fixes the architectural problem in the earlier ZIP: **each supplier is now a self-contained package with all known integration families relevant to attendance, biometric terminals, access controllers and readers.**

## Supplier packages

- `packages/suppliers/zkteco/`
- `packages/suppliers/hikvision/`
- `packages/suppliers/dahua/`
- `packages/suppliers/suprema/`
- `packages/suppliers/virdi/`
- `packages/suppliers/anviz/`
- `packages/suppliers/honeywell/`

Each package contains adapters, integration-specific README files, a model/firmware/platform routing matrix, tests, vendor-library placeholders, examples, and **official documentation links for every route**.

## Important compatibility principle

There is no technically honest way to guarantee “every device version” from a brand using one protocol. Firmware editions, product generations, licenses and topology change the integration surface. The repo therefore routes by:

`Supplier -> Model family -> Firmware/platform generation -> Capabilities/topology -> Preferred integration -> Fallback integration`

Unknown firmware is not silently guessed. The safe routes are probed and the device remains `UNVERIFIED` until a hardware conformance test records what worked.

## Documentation

Start with `docs/OFFICIAL_DOCUMENTATION_INDEX.md`. Each package also contains `DOCUMENTATION.md`, and each `integrations/*/README.md` repeats the supplier links that apply specifically to that route.

## Proprietary SDKs

The source code contains bridges/boundaries for proprietary SDKs but does not redistribute supplier DLL/SO/JAR packages when licensing is unclear or restricted. Put licensed artifacts in the package's `vendor-libs/` folder or configure an external SDK path.

## Existing ZKTeco work

The earlier full Java ZKTeco reference implementation is retained under `packages/suppliers/zkteco/reference-implementation-java/`.

## Root gateway behavior

The root gateway no longer owns hard-coded vendor drivers. It reads each supplier package's route matrix and pins a supplier-owned route when a device is registered. This keeps version-specific integration logic inside the supplier package.

## Version-aware SDK/API routing (v0.4)

SDK and API versions are first-class compatibility inputs. SDK routes are **not auto-selected without an exact installed SDK version**. When the vendor does not publish a complete historical compatibility matrix, the resolver returns `NEEDS_VENDOR_MATRIX` rather than claiming support. APIs are tracked separately as explicit-version, server-coupled, or capability-negotiated integrations. See `docs/SDK_API_VERSIONING.md` and each supplier's `SDK_API_VERSIONING.md`.

**Implementation-status note:** many proprietary API/SDK routes in this repository are currently generic clients/probes or bridge boundaries until the licensed vendor SDK/API package is supplied. The version catalog states this explicitly; presence of a route does not by itself mean every business operation has been implemented.

## Audit matrices

- `docs/API_SUPPORT_MATRIX.md` — all API/cloud/platform API routes and their version policy.
- `docs/SDK_VERSION_MATRIX.md` — all native/vendor SDK routes, required version pinning and status.

