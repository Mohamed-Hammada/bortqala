# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `F18` | firmware-dependent | `ac-push` | `adms-ta-push`, `standalone-sdk` | F18 is explicitly sold with AC PUSH, TA PUSH, or Standalone SDK variants; detect firmware before selecting. |
| `F22|F16|TF1700|MA300|MA500` | legacy/access | `ac-push` | `standalone-sdk`, `zk-pull` | Exact firmware decides PUSH vs SDK; probe safe routes. |
| `K20|K30|K40|K50|K60|LX50|UA.*|iClock.*|IN0.*` | classic-attendance | `zk-pull` | `adms-ta-push`, `standalone-sdk` | Classic attendance families; PUSH availability varies by firmware. |
| `MB.*|uFace.*|SpeedFace.*|SenseFace.*|ProFace.*|Horus.*|G4.*|G5.*` | modern | `adms-ta-push` | `ac-push`, `zkbio-time-api`, `zkbio-cvsecurity-api`, `android-lcdp` | Modern terminals commonly use PUSH/platform routes; model firmware must confirm. |
| `C3.*|inBio.*` | controller | `plcommpro-pull` | `zkbio-cvsecurity-api`, `zkbio-cvaccess-api` | Controller family route. |
| `ZK9500|ZK6500|ZK8500|SLK20R` | usb-scanner | `zkfinger-sdk` | — | USB scanner route. |
| `UNKNOWN` | unknown | `zk-pull` | `adms-ta-push`, `ac-push`, `standalone-sdk`, `plcommpro-pull` | Auto-detect using capabilities and safe probes. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
