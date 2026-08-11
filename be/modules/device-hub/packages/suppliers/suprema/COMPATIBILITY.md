# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `BioStation 3.*|BS3.*|BioStation 2.*|BS2.*|BioStation A2.*|BioLite N2.*|BioEntry W2.*|BioEntry P2.*|BioEntry R2.*|FaceStation 2.*|FaceStation F2.*|FaceLite.*|X-Station 2.*|XPass 2.*|XPass D2.*|CoreStation.*` | biostar2-generation | `g-sdk` | `biostar-device-sdk`, `biostar2-local-api` | BioStar 2 devices: direct SDK/G-SDK or platform API. |
| `BioEntry Plus.*|BioEntry W v2.*|XPass v2.*|XPS2 v2.*|BLN v2.*` | biostar1-generation | `biostar1-legacy-sdk` | — | G-SDK explicitly excludes BioStar 1 devices. |
| `BioStar 2 >=2.7.10` | platform | `biostar2-local-api` | — | New Local API bundled with platform. |
| `BioStar 2 2.4-2.7.9` | platform | `biostar2-api-v2` | — | Legacy API v2. |
| `BioStar 2 <=2.3` | platform | `biostar2-api-v1` | — | Legacy API v1. |
| `UNKNOWN` | unknown | `g-sdk` | `biostar-device-sdk`, `biostar1-legacy-sdk` | Read device generation/capability before choosing. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
