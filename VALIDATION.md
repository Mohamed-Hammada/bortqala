# Validation report

Target: `Mohamed-Hammada/bortqala` → `fm_bemo_consolidated`

Completed checks:

- [x] Supplier packages present: ZKTeco, Hikvision, Dahua, Suprema, VIRDI, Anviz, Honeywell.
- [x] 55 supplier integration-route directories found.
- [x] 55 route READMEs found.
- [x] Every integration-version catalog row has at least one official documentation URL.
- [x] All JSON profiles parse.
- [x] All YAML files in the overlay parse.
- [x] Python gateway + supplier source compiles with `compileall`.
- [x] Supplier route-selection self-tests run successfully for all seven suppliers.
- [x] Strict SDK-version test: Suprema G-SDK 1.9.1 → `COMPATIBLE`; 2.0.0 → `INCOMPATIBLE` under the catalogued 1.x rule.
- [x] Hikvision DS-K1T341 smoke resolution → ISAPI compatible while HCNetSDK remains version-gated.
- [x] Hub registry persists registered devices to disk and reloads them.
- [x] Optional device-hub API-key middleware tested.
- [x] HTTP GET event normalization tested end-to-end into Bortqala-compatible `punches` JSON.
- [x] HTTP POST event search with `since_body_field` tested end-to-end into normalized `punches` JSON.
- [x] Apply script tested twice; second run is idempotent.
- [x] New TypeScript files passed a syntax check; only expected unresolved-module diagnostics occurred because the complete Angular checkout/node_modules are not present in the artifact workspace.
- [x] New Java sources were syntax-parsed by `javac`; expected missing project/Spring/JPA/Jackson symbols remain when compiling the overlay in isolation.

Not claimed as completed here:

- Full `./gradlew test` against the complete Bortqala checkout.
- Full `npm run build` against the complete Bortqala frontend checkout.
- Hardware conformance against every physical model/firmware combination.
- Proprietary SDK execution without the licensed vendor SDK binaries.

Run the project build after extracting/applying the overlay to a complete branch checkout.
