# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `Pro-Watch 4.*|Pro-Watch 5.*|Pro-Watch 6.*` | platform | `prowatch-web-services` | `prowatch-hsdk` | Web Services API for standard integration; HSDK for licensed deep integration. |
| `PRO3200.*|PRO4200.*|Mercury.*` | panel | `panel-through-prowatch` | `prowatch-hsdk` | Integrate through Pro-Watch rather than assuming a public direct device API. |
| `OmniProx.*|reader.*` | reader | `reader-bus` | — | Reader attaches to access panel; integration is at panel/platform layer. |
| `UNKNOWN` | unknown | `prowatch-web-services` | `prowatch-hsdk` | Identify Pro-Watch version/licensing and panel type. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
