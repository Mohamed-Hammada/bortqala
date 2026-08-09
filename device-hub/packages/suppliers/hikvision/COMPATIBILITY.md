# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `DS-K1T.*|MinMoe.*` | access-terminal | `isapi` | `hcnet-sdk`, `device-gateway`, `isup-sdk` | Prefer ISAPI on reachable fixed-IP terminals; use ISUP/Device Gateway when device registers to platform. |
| `DS-K26.*|DS-K28.*|DS-K27.*` | controller | `isapi` | `hcnet-sdk`, `hikcentral-openapi` | Access controllers; firmware capability decides direct API/SDK. |
| `DS-K110.*|DS-K1F.*` | reader-usb | `usb-sdk` | `wiegand-osdp` | Reader/card issuer path. |
| `HikCentral.*` | platform | `hikcentral-openapi` | — | Platform integration. |
| `ISUP.*` | registered-device | `device-gateway` | `isup-sdk` | No-fixed-IP registration topology. |
| `OTAP.*` | new-platform | `otap-sdk` | — | Use when device/product documentation advertises OTAP. |
| `UNKNOWN` | unknown | `isapi` | `hcnet-sdk`, `device-gateway`, `isup-sdk`, `otap-sdk` | Probe ISAPI safely, then select provisioned SDK/gateway path. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
