# WP-18 — Small Tech-Debt Bundle (filler between big WPs)
**Priority:** 🟢 · **Owner:** Any dev · **Depends on:** — · **Effort:** ~1 day each, independent
**Read first:** `_GLOBAL-RULES.md`

## T-1 GraalVM launcher scripts
`be/start-backend-graal.bat` + `.sh`: probe `GRAALVM_HOME` env then common install paths; set JAVA_HOME and exec existing start command with clear message when absent. README section: how to build native image (`./gradlew nativeCompile`) — do NOT wire CI.

## T-2 Translation CSV ID generator
`be/tools/gen-translations-csv.py`: input YAML `{key: {ar: ..., en: ...}}`, scans target changeset CSV for max existing numeric id, emits next sequential rows per locale convention (`vNNN-001-en/-ar`). Prevents duplicate-PK class bugs (hit in V126/V146). Unit-test id scanning incl. quoted fields.

## T-3 Spring cache expansion
Mirror `TranslationService` pattern: `@Cacheable` on dashboard aggregation methods + access-catalog lookups; caffeine TTL property `hr.cache.ttl-seconds` default 300. Integration test asserts repository called once for two service calls (verify mock). Document eviction on writes (`@CacheEvict` where mutation exists).

## Acceptance Criteria (QA sign-off)
- [x] **T-1 AC** Script starts app with GraalVM when installed, prints actionable message when not; standard scripts untouched.
- [x] **T-2 AC** Generator output loads via Liquibase on H2 (context-load test) with zero PK collisions; ids continue sequence correctly after existing rows.
- [x] **T-3 AC** Second identical dashboard call within TTL hits cache (mock verify count = 1); write path evicts; TTL configurable proven by property test; no behavior change visible to users.
