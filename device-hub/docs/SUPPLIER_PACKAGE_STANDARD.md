# Supplier package standard

Each supplier is independently deployable and owns all of its integration methods.

Required contents:

1. `DOCUMENTATION.md` — official links.
2. `COMPATIBILITY.md` — generation/version routing.
3. `profiles/device_routes.json` and `.csv` — machine-readable rules, including official documentation per row.
4. `integrations/<route>/README.md` — one integration route per folder.
5. `src/supplier_<name>/adapters/` — code boundary per route.
6. `vendor-libs/` — licensed SDK location, never assumed redistributable.
7. `tests/` — at least route selection smoke test.

The root gateway may normalize events and commands, but supplier packages remain the source of truth for device/version decisions.
