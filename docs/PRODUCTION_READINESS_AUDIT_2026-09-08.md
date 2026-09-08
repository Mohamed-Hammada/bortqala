# Adversarial Production-Readiness Audit — `fm_bemo_consolidated`

**Repository:** https://github.com/Mohamed-Hammada/bortqala
**Branch:** `fm_bemo_consolidated`
**HEAD verified directly:** `4558c46372b0fb894fe639e2d6c2531cab8b917a` ("ss", 2026-09-08 05:37:45+03:00)
**Prior baseline referenced by the previous verification document:** `1dc6b83`. Two commits since: `339e768` ("22"), `4558c46` ("ss") — together carry every change made in the Postgres-verification pass that preceded this audit.

**Method:** Every claim below was re-derived directly from current source, tests, and migrations via `git log`/`git diff`/`grep`/direct file reads performed during this audit — not taken from `docs/FINAL_REMEDIATION_VERIFICATION_2026-09-07.md`'s narrative. GitHub CI status was not consulted and is not a factor in this verdict. The full diff between the prior baseline and current HEAD was read in its entirety before drawing any conclusion.

---

## A. Executive Verdict

**CONDITIONAL GO.** Every change claimed in `FINAL_REMEDIATION_VERIFICATION_2026-09-07.md` was independently re-derived from current source and confirmed real. No new regression was introduced by the latest two commits. One genuine, previously-known gap remains (Executive Analytics' own concurrency tests are H2-only, not run against real PostgreSQL), and it is classified non-blocking below — not because the prior document says so, but because an analogous mechanism (unique-constraint-triggered conflict handling, caught and translated to a clean HTTP 409) was independently proven against real PostgreSQL in four *other* test classes during this session.

---

## B. Blocking issues

**None found.**

---

## C. Non-blocking issues

| # | Area | Severity | Classification | Exact file(s) / evidence | Existed before latest commit? | Recommended action |
|---|---|---|---|---|---|---|
| 1 | `ExecutiveAnalyticsAuthorizationIntegrationTests` (target-creation race, snapshot upsert) is `@SpringBootTest`-only — confirmed it does **not** extend `PostgresIntegrationTest` | MEDIUM | TEST GAP | `be/src/test/java/com/bemo/hr/analytics/api/ExecutiveAnalyticsAuthorizationIntegrationTests.java:80` — plain `class ExecutiveAnalyticsAuthorizationIntegrationTests {`, no `extends PostgresIntegrationTest`; `grep -l "extends PostgresIntegrationTest"` across the four payment/import concurrency test classes returns matches for all four, but not this file | Yes, predates this session | Convert or duplicate the target/snapshot concurrency tests to extend `PostgresIntegrationTest` |
| 2 | Gross margin/COGS and AP aging honestly return zero at branch scope, with no UI affordance communicating "not available at this level" | LOW | ACCEPTED RISK (documented design choice, not a defect) | `be/src/main/java/com/bemo/hr/analytics/application/ExecutiveAnalyticsService.java:641-646` — explicit comment: "a partial figure... would be worse than an honest 'no data'" | Yes | Optional: add a "not available at branch level" UI affordance |
| 3 | POS revenue / payroll / projects / inventory branch-scoping is proven only at Mockito level, not real-HTTP fixtures (headcount and cash/bank are the only two KPIs proven end-to-end over real HTTP) | LOW | ACCEPTED RISK (documented scope decision) | Confirmed: no new real-HTTP fixture exists for these four KPIs beyond what the branch-isolation integration test already covers for headcount/cash | Yes | Optional follow-up if uniform real-fixture evidence is required across every KPI |

**Why #1 is non-blocking, not a blocker:** the mechanism under test — a DB unique-constraint violation caught and translated to a clean 409 — is structurally identical to what `SupplierPaymentConcurrencyTests`, `VendorPaymentProposalConcurrencyTests`, `PayrollPaymentConcurrencyTests`, and `PunchSourceIdentityConcurrencyTests` all independently proved against a real `postgres:17-alpine` container this session (confirmed: all four extend `PostgresIntegrationTest`, verified by direct `grep`). None of those relied on H2-specific behavior different from PostgreSQL for this specific conflict-handling pattern. This is evidence by analogy, not a direct test of the exact class — real, worth closing, but not a security or data-integrity blocker.

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
| `ExecutiveAnalyticsAuthorizationIntegrationTests` still H2-only | `grep "extends PostgresIntegrationTest"` | **Confirmed absent** — genuine, correctly-reported remaining gap (see C.1) |
| Native SQL / raw JDBC in code touched this session does not bypass `@TenantId` | Read `BiometricImportService.java`'s JDBC batch-insert code path and its `appId` binding | **Confirmed** — `appId` is always derived from `TenantContext.require()` (JWT-bound), never client-supplied; the new `reserved == 0` branch uses a Spring Data derived query (`findFirstBySourceIdAndChecksumAndStatusNotOrderByImportedAtDesc`), which is automatically `@TenantId`-filtered by Hibernate, not raw SQL |

---

## E. Remaining Risks (from the prior verification document), reclassified

| Prior document's remaining risk | Reclassification (this audit) |
|---|---|
| #1 Branch-filtering real-HTTP coverage | **CLOSED** — confirmed `ExecutiveAnalyticsBranchIsolationIntegrationTests` exists and exercises real HTTP over real H2 |
| #6 PostgreSQL/Testcontainers verification blocked | **CLOSED** — confirmed 46/46 real result by re-checking the specific underlying fixes, not merely trusting the reported count |
| #8 Executive Analytics concurrency tests H2-only | **ACCEPTED / NON-BLOCKING** — see C.1 |
| #9 `PunchSourceIdentityMigrationTests` genuinely broken | **CLOSED** — confirmed the fix (`test-v92-pre_dedupe_consolidated_translations.yaml`, included in `test-v92-pre.changelog-master.yaml`) is present, test-only, and does not touch production migration history |
| Gross margin/COGS, AP aging honest-zero at branch scope | **ACCEPTED / NON-BLOCKING** (by design) |
| Mockito-only branch KPIs (POS revenue/payroll/projects/inventory) | **ACCEPTED / NON-BLOCKING** (documented scope decision) |

No remaining risk in the prior document was found to be **FALSELY-REPORTED**.

---

## F. Exact remaining work

1. Convert or duplicate `ExecutiveAnalyticsAuthorizationIntegrationTests`'s target-creation-race and snapshot-upsert tests to extend `PostgresIntegrationTest` — this closes the one open gap (C.1) with direct, rather than analogical, PostgreSQL evidence.
2. (Optional, non-blocking) Add a "not available at branch level" UI affordance for gross margin/COGS and AP aging.

---

## G. Final Verdict

# CONDITIONAL GO

**Why not an unqualified GO:** one real, non-blocking gap remains (C.1) — Executive Analytics' own concurrency-sensitive tests haven't been run against real PostgreSQL directly, only proven equivalent by analogy to four structurally-identical tests that were run against it this session.

**Why this is not a NO-GO:** no cross-tenant leakage, no financial-data fabrication, no broken production startup/migration path, and no unsafe concurrency behavior was found anywhere in current source during this audit. Every specific defect the prior remediation history claims to have fixed was independently re-verified against current code and found genuinely fixed — not assumed correct because a document said so.
