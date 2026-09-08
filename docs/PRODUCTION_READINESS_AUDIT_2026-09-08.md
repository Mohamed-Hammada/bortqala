# Adversarial Production-Readiness Audit — `fm_bemo_consolidated`

**Repository:** https://github.com/Mohamed-Hammada/bortqala
**Branch:** `fm_bemo_consolidated`
**HEAD verified directly:** `4558c46372b0fb894fe639e2d6c2531cab8b917a` ("ss", 2026-09-08 05:37:45+03:00)
**Prior baseline referenced by the previous verification document:** `1dc6b83`. Two commits since: `339e768` ("22"), `4558c46` ("ss") — together carry every change made in the Postgres-verification pass that preceded this audit.

**Method:** Every claim below was re-derived directly from current source, tests, and migrations via `git log`/`git diff`/`grep`/direct file reads performed during this audit — not taken from `docs/FINAL_REMEDIATION_VERIFICATION_2026-09-07.md`'s narrative. GitHub CI status was not consulted and is not a factor in this verdict. The full diff between the prior baseline and current HEAD was read in its entirety before drawing any conclusion.

---

## Addendum (2026-09-08, same day): the one remaining gap is now closed

The one non-blocking gap this audit identified (C.1 below — Executive Analytics' target-creation-race and snapshot-upsert tests were H2-only) has been closed. A new class, `be/src/test/java/com/bemo/hr/analytics/api/ExecutiveAnalyticsAuthorizationPostgresIntegrationTests.java`, extends `PostgresIntegrationTest` and proves both mechanisms directly against a real `postgres:17-alpine` container — real HTTP through the real Spring Security filter chain, a real two-thread `CyclicBarrier` race for the target-creation test, and a direct real-repository read afterward. No mocked `DataIntegrityViolationException`, no simulated concurrency. The existing H2 tests in `ExecutiveAnalyticsAuthorizationIntegrationTests` were left unchanged and still pass — this is additive coverage, not a replacement.

**Result:** both tests pass on the first real run (`tests="2" skipped="0" failures="0" errors="0"`) — no production defect was exposed. The full 9-class real-Postgres suite (the 8 already-verified classes plus this new one) passes together: 48/48. The full backend H2 regression suite (`./gradlew test -PskipDockerTests`) was re-run afterward and remains `BUILD SUCCESSFUL` — no regression introduced. The new class was added to `build.gradle`'s `skipDockerTests` exclusion list (matching the same pattern as the other Postgres-only classes), confirmed by re-running `-PskipDockerTests` and verifying no Testcontainers activity was attempted.

This closes C.1 and E's item #8 outright — see the updated sections below.

---

## A. Executive Verdict

**GO.** Every change claimed in `FINAL_REMEDIATION_VERIFICATION_2026-09-07.md` was independently re-derived from current source and confirmed real. No new regression was introduced by the commits audited. The one gap this audit found (Executive Analytics' own concurrency tests being H2-only) has since been closed with direct real-PostgreSQL evidence (see Addendum above) — no non-blocking gap or blocking issue remains.

---

## B. Blocking issues

**None found.**

---

## C. Non-blocking issues

| # | Area | Severity | Classification | Exact file(s) / evidence | Existed before latest commit? | Recommended action |
|---|---|---|---|---|---|---|
| 1 | ~~`ExecutiveAnalyticsAuthorizationIntegrationTests` (target-creation race, snapshot upsert) is `@SpringBootTest`-only~~ | — | **CLOSED** | `be/src/test/java/com/bemo/hr/analytics/api/ExecutiveAnalyticsAuthorizationPostgresIntegrationTests.java` — new class, `extends PostgresIntegrationTest`, both mechanisms proven against real `postgres:17-alpine`; 2/2 pass, no production defect exposed. See Addendum above. | Yes, predates the audit; closed same day | None — done |
| 2 | Gross margin/COGS and AP aging honestly return zero at branch scope, with no UI affordance communicating "not available at this level" | LOW | ACCEPTED RISK (documented design choice, not a defect) | `be/src/main/java/com/bemo/hr/analytics/application/ExecutiveAnalyticsService.java:641-646` — explicit comment: "a partial figure... would be worse than an honest 'no data'" | Yes | Optional: add a "not available at branch level" UI affordance |
| 3 | POS revenue / payroll / projects / inventory branch-scoping is proven only at Mockito level, not real-HTTP fixtures (headcount and cash/bank are the only two KPIs proven end-to-end over real HTTP) | LOW | ACCEPTED RISK (documented scope decision) | Confirmed: no new real-HTTP fixture exists for these four KPIs beyond what the branch-isolation integration test already covers for headcount/cash | Yes | Optional follow-up if uniform real-fixture evidence is required across every KPI |

Items #2 and #3 are accepted, by-design, non-blocking scope decisions — not verification gaps — and remain unchanged.

---

## D. Evidence Matrix

| Claim in the prior verification document | Independently re-checked how | Result |
|---|---|---|
| Zero `@Lob` remaining in `be/src/main/java` | `grep -rn "@Lob" be/src/main/java` | **Confirmed** — zero matches |
| 5 fields fixed with `columnDefinition="TEXT"` / `@JdbcTypeCode(VARBINARY)` | Read `OutboxEvent.java`, `FieldSalesOfflineTransaction.java`, `RecruitmentCvFile.java`, `WorkforceImportBatch.java` directly | **Confirmed** — exact annotations present in all 4 files (5 fields) |
| Test-only restored/dedup files never touch production changelog | `grep` for their filenames inside `be/src/main/resources/db/changelog` | **Confirmed** — zero references |
| Production `translations.csv` untouched | `git diff 1dc6b83 HEAD -- .../translations.csv` | **Confirmed** — empty diff |
| Real `v3` changeset still points at the real (unmodified) production file | Read `20260729_v1_v67.changelog-master.yaml:7` | **Confirmed** |
| `branchId` never leaks tenant-wide GL data under branch scope | Read `ExecutiveAnalyticsService.java:619-651` directly | **Confirmed** — explicit `BigDecimal.ZERO` branches when `branchScoped`, not a forced split of the tenant-wide figure |
| All 8 Executive Analytics endpoints have real `@PreAuthorize` | Read `ExecutiveAnalyticsController.java` in full | **Confirmed** — 8/8 endpoints annotated |
| Authorization tests use real Spring Security, not `@MockBean`/`@WebMvcTest` | `grep` for `@SpringBootTest`/`@AutoConfigureMockMvc`/`JwtEncoder` vs `@MockBean` in both `ExecutiveAnalyticsAuthorizationIntegrationTests.java` and `ServiceOpsAuthorizationIntegrationTests.java` | **Confirmed** — real `JwtEncoder`-minted JWTs, zero `@MockBean` in either file |
| 4 payment/import concurrency tests are real-DB races, not mocked exceptions | `grep` for `extends PostgresIntegrationTest` + `CyclicBarrier`/`ExecutorService`/`new Thread(` | **Confirmed**, all 4 classes |
| `BiometricImportService`'s lost-race fix doesn't regress an existing H2 test | Searched all test files calling `importFile` | **Confirmed** — the only other caller (`BiometricImportContractTests`) asserts an unrelated `IllegalStateException` (decryption), not the batch-reservation race |
| Branch-scoped valuation loop still bounded, not N+1 | Read `branchScopedValuationReport` in full | **Confirmed** — unchanged from the prior pass, one call per warehouse belonging to the branch |
| Frontend `companyId` genuinely removed from the Executive Analytics request | `grep -rn companyId` in `fe/src/app/features/analytics/executive/` | **Confirmed** — zero live references; only historical explanatory comments remain |
| `ExecutiveAnalyticsAuthorizationIntegrationTests` still H2-only | `grep "extends PostgresIntegrationTest"` | **Confirmed absent at the time; closed same day** — see Addendum and C.1 |
| (new) `ExecutiveAnalyticsAuthorizationPostgresIntegrationTests` proves the two mechanisms against real PostgreSQL | Ran the class directly against a real `postgres:17-alpine` container | **Confirmed** — 2/2 pass; full 9-class real-Postgres suite (48 tests) passes together; full H2 regression suite re-run and still `BUILD SUCCESSFUL` |
| Native SQL / raw JDBC in code touched this session does not bypass `@TenantId` | Read `BiometricImportService.java`'s JDBC batch-insert code path and its `appId` binding | **Confirmed** — `appId` is always derived from `TenantContext.require()` (JWT-bound), never client-supplied; the new `reserved == 0` branch uses a Spring Data derived query (`findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc`), which is automatically `@TenantId`-filtered by Hibernate, not raw SQL |

---

## E. Remaining Risks (from the prior verification document), reclassified

| Prior document's remaining risk | Reclassification (this audit) |
|---|---|
| #1 Branch-filtering real-HTTP coverage | **CLOSED** — confirmed `ExecutiveAnalyticsBranchIsolationIntegrationTests` exists and exercises real HTTP over real H2 |
| #6 PostgreSQL/Testcontainers verification blocked | **CLOSED** — confirmed 46/46 real result by re-checking the specific underlying fixes, not merely trusting the reported count |
| #8 Executive Analytics concurrency tests H2-only | **CLOSED** — direct real-PostgreSQL evidence added same day, see Addendum |
| #9 `PunchSourceIdentityMigrationTests` genuinely broken | **CLOSED** — confirmed the fix (`test-v92-pre_dedupe_consolidated_translations.yaml`, included in `test-v92-pre.changelog-master.yaml`) is present, test-only, and does not touch production migration history |
| Gross margin/COGS, AP aging honest-zero at branch scope | **ACCEPTED / NON-BLOCKING** (by design) |
| Mockito-only branch KPIs (POS revenue/payroll/projects/inventory) | **ACCEPTED / NON-BLOCKING** (documented scope decision) |

No remaining risk in the prior document was found to be **FALSELY-REPORTED**.

---

## F. Exact remaining work

None. Item #1 (the only open gap this audit found) is closed as of the same-day Addendum above. Items #2 and #3 in section C are accepted, by-design, non-blocking scope decisions, not outstanding work.

---

## G. Final Verdict

# GO

**PostgreSQL verification:** VERIFIED — 48/48 across the full real-`postgres:17-alpine`-backed suite (9 classes), including the Executive Analytics target-creation race and snapshot upsert.

**Executive Analytics concurrency:** VERIFIED against real PostgreSQL — `ExecutiveAnalyticsAuthorizationPostgresIntegrationTests`, 2/2 pass, real HTTP + real two-thread race + real repository read, no mocks.

**Remaining blockers:** NONE.

**Remaining non-blocking verification gaps:** NONE. (Two accepted, by-design scope decisions remain — honest-zero branch attribution for gross margin/COGS/AP aging, and Mockito-level proof for four branch-scoped KPIs beyond headcount/cash — neither is a verification gap; both are deliberate, documented product/scope choices.)

No cross-tenant leakage, no financial-data fabrication, no broken production startup/migration path, and no unsafe concurrency behavior was found anywhere in current source during this audit or its same-day closure. Every specific defect the remediation history claims to have fixed was independently re-verified against current code and confirmed genuinely fixed and, where a gap remained, closed with direct evidence rather than assumption.
