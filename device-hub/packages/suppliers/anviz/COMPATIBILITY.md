# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `^(FaceDeep.*|FacePass.*|W2 Face.*|M7 Palm.*|CX.*|W.*|C2.*|M5.*)$` | current-mixed | `anviz-standard-sdk` | `crosschex-cloud-api`, `cloudkit` | Cloud capabilities differ by model/server; direct SDK remains fallback when supported. |
| `EP300.*|A300.*|OA.*|TC.*` | legacy-attendance | `anviz-standard-sdk` | `crosschex-standard`, `usb-export` | Legacy attendance route. |
| `CrossChex Cloud` | platform | `crosschex-cloud-api` | `crosschex-cloud-webhook` | Cloud integration; endpoint scope depends on account/plan/vendor enablement. |
| `UNKNOWN` | unknown | `anviz-standard-sdk` | `crosschex-cloud-api`, `crosschex-standard` | Detect whether the estate is direct, cloud, or CrossChex-managed. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
