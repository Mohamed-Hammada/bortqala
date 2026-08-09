# Compatibility and version routing

This is a **family/generation routing matrix**, not a claim that every firmware exposes every API. The runtime must identify device generation/capabilities and then use the first verified route.

| Model / version rule | Generation | Preferred | Fallbacks | Notes |
|---|---|---|---|---|
| `UBio-X.*` | modern | `alpeta-api` | `ucs-sdk` | Modern UBio family; typically Alpeta-managed, UCS SDK for lower-level integration. |
| `AC-5000.*|AC-5100.*|AC-2200.*|AC-2100.*` | modern-legacy | `ucs-sdk` | `alpeta-api`, `unis-server` | Generation/firmware and deployed server decide. |
| `AC-2000.*|AC-1100.*` | legacy | `unis-server` | `ucs-sdk` | Legacy UNIS estates are common; UCS availability must be confirmed. |
| `FOH.*|NScan.*|VScan.*|Hamster.*` | scanner | `usb-scanner-sdk` | — | USB scanner/module integration. |
| `UNKNOWN` | unknown | `alpeta-api` | `ucs-sdk`, `unis-server` | Use deployed platform first, then licensed SDK. |

See `profiles/device_routes.csv` for documentation URLs attached to each row.
