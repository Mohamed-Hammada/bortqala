# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `ASI.*|ASA.*` | terminal | `netsdk` | `dss-openapi` | Standalone access/attendance terminals: direct NetSDK or DSS platform. |
| `ASC.*` | controller | `netsdk` | `dss-openapi` | Access controllers. |
| `DSS.*` | platform | `dss-openapi` | `dss-sdk`, `dss-dip-bridge` | Platform integration. |
| `UNKNOWN` | unknown | `netsdk` | `dss-openapi` | Use NetSDK for direct devices; DSS OpenAPI for managed estates. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
