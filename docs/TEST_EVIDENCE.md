# Test Evidence — Bemo ERP

## 2026-08-23 — market-readiness audit remediation (F-002 payroll payment state, F-003 SoD, F-004 direct-method cash flow)

- Backend non-Docker/H2: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 29s**; JUnit XML: **739 tests / 189 suites / 0 failures / 0 errors / 1 skipped**. New/updated suites: `SalaryPaymentStateTransitionTests` (4), `PayrollServiceTests` (+5 SoD tests = 34 in package), `FinancialStatementsReportServiceTests` (6 cash-flow tests incl. financing-sign regression, internal-transfer netting, exact reconciliation, comparative period). V339/V340 load clean on H2.
- Gates: `be/tools/check-error-codes.py` **575/575 PASS** (incl. new `PAYROLL_SOD_SELF_APPROVAL`, `PAYROLL_SOD_DISBURSEMENT_CONFLICT`); `be/tools/check-test-count.py` floors raised to **700 tests / 185 suites** (observed 739/189).
- Frontend on Node `v24.18.1`: **429 tests / 91 files PASS**; i18n **4,538 keys PASS**; hardcoded scan **109 HTML + 221 TypeScript PASS**; production build **PASS**. FE change limited to the additive `CashFlowReport` model (`reconciled`, `comparative`) in `accounts.page.ts`.
- Known external block: PostgreSQL/Testcontainers suites (`F-001`) cannot execute in this WSL environment (no Docker daemon); all H2-mirror evidence above is Docker-free by design.

## 2026-08-13 completed local remediation verification

- Canonical implementation items through `SEC-002` are implemented or explicitly classified; see `docs/BORTQALA_CURRENT_CODE_REVIEW_REMAINING_WORK_2026-08-13.md`.
- Backend non-Docker/H2: **535 tests / 146 suites / 0 failures / 0 errors / 0 skipped**.
- Backend catalogs: exception codes **468/468 PASS**; translation catalog **7,560 rows PASS**; authorization roles **19/19 PASS**. H2 loads V229 through V241.
- Frontend on Node `v24.18.0`: **284 tests / 50 files PASS**; i18n **2,437 keys PASS**; hardcoded scan **49 HTML + 127 TypeScript PASS**; production build **PASS**.
- CSP bundle scan: no `eval` or `new Function` in generated JavaScript.
- PostgreSQL payroll concurrency remains blocked by unavailable Docker Desktop, so `PAY-001` remains open and release readiness is not claimed.

## Earlier 2026-08-13 review-remediation verification

- Working-tree verification for `FIN-001`…`FIN-004`, `INV-001`, `MFG-001`, `PAY-001`, and `PAY-002`; the canonical item status is in `docs/BORTQALA_CURRENT_CODE_REVIEW_REMAINING_WORK_2026-08-13.md`.
- Backend: `./gradlew test -PskipDockerTests -x processTestAot -x compileAotTestJava -x processAotTestResources -x aotTestClasses` — **528 tests / 146 suites / 0 failures / 0 errors / 0 skipped**. GraalVM/native work remains paused; AOT tasks were excluded only to exercise the JVM suite.
- Backend catalogs: exception codes **463/463 PASS**; translation catalog **7,466 rows PASS**. The H2 production-changelog context includes V229…V235.
- Frontend under Node `v24.18.0`: i18n **2,398 keys PASS**; hardcoded UI scan **49 HTML + 127 TypeScript PASS**; **284 tests / 50 files / 0 failures**; production build **PASS**.
- PostgreSQL payroll concurrency: `PayrollPaymentConcurrencyTests` was added and `compileTestJava` passes. Execution remains **BLOCKED** because Docker Desktop is unavailable (`dockerDesktopLinuxEngine` pipe missing), so `PAY-001` remains open rather than being inferred complete.

## 2026-08-13 final local release-candidate verification

- Candidate code SHA: `6e4bc88a86fec54cfc1c71c031f3145bb6b28f47` (`fm_bemo_consolidated`). Documentation updates follow this code checkpoint.
- Backend: `./gradlew test -PskipDockerTests` — **516 tests / 144 suites / 0 failures / 0 errors / 0 skipped**. The XML result set was generated at 2026-08-13 14:58 Africa/Cairo; the command wrapper timed out after the Java worker completed, so counts were read from all `be/build/test-results/test/TEST-*.xml` files.
- Backend static gates: `python tools/check-error-codes.py` — **454/454 PASS**; `python tools/check-translation-catalog.py` — **7,440 rows PASS**; `python tools/check-authorization-contract.py` — **19/19 roles PASS**.
- Frontend under Node `v24.18.0`: `npm ci` — **PASS** (460 packages audited; 2 moderate and 5 high dependency audit findings reported); `npm run check:i18n` — **2,396 keys PASS**; `npm run check:hardcoded` — **49 HTML + 127 TypeScript files PASS**; `npm run test -- --watch=false` — **284 tests / 50 files / 0 failures**; `npm run build` — **PASS**.
- Docker/PostgreSQL/Compose: **BLOCKED, not run**. `docker info` cannot connect to `npipe:////./pipe/dockerDesktopLinuxEngine` because the Docker daemon is unavailable. This blocks Testcontainers concurrency, PostgreSQL fresh/upgrade migration, `ddl-auto=validate` on PostgreSQL, and production-like Compose smoke.
- CI: **BLOCKED, not run**. The known GitHub Actions account/billing lock remains external; no green exact-SHA CI URL exists.

## 2026-08-13 O2C vertical-slice checkpoint

- Working-tree evidence after parent `41a7f3b`; not final release-SHA evidence.
- `SalesOrderToCashPersistenceTests`: **2/2 PASS** on the H2 application context. It proves persisted real lines, confirmation-time pricing snapshots, locked ATP reservation, valued delivery and COGS journal, linked issued invoice, partial/final receipts, customer ledger, partial return, original-cost stock/COGS reversal, linked credit note, operation replay, tenant isolation, and concurrent no-oversubscription.
- Focused sales/AR/security tests: **PASS** for multi-invoice receipt allocation, idempotent cancellation release, and Sales-role-only delivery/return mutation.
- Sales Angular suite: **6/6 PASS**; i18n **2,374 keys PASS**; hardcoded UI **0 findings across 49 HTML / 127 TS files**; production build **PASS**.
- V209 also repairs upgrade-path gaps exposed by the persisted test: legacy sales-line/customer-credit/invoice columns and malformed historical composite uniqueness on valuation and AR tables.

## 2026-08-13 P2P multi-invoice verification checkpoint

- Parent baseline: `1133e3431d170b50c202f881c31a0021ade75495`; this is working-tree evidence and not final release-SHA evidence.
- Backend focused service, persistence, and authorization suites: **PASS**. The coverage proves ordered multi-invoice allocation, server-derived total/currency, partial balances, real supplier payments, partner-ledger entries, audit actors, SoD, bank validation propagation, same-operation replay, and atomic rollback.
- Frontend procurement component: **15/15 PASS**; i18n and hardcoded-UI gates **PASS**; production build **PASS**.
- Static backend gates: error codes **425/425 PASS**; **7,274** translation rows have unique IDs and key/locale pairs; authorization roles **19/19 PASS**.
- `VendorPaymentProposalConcurrencyTests` is implemented and compiles, but was not executed because the Docker Desktop Linux engine is unavailable. P2P-001 therefore remains `VERIFY`, not `DONE`.

## 2026-08-12 shutdown checkpoint

- Parent baseline: `0c182021944f1b8d411deb72c83eaf45761d81a0`; evidence and implementation are recorded by this checkpoint commit.
- Backend compile and focused payroll/manufacturing/P2P suites: PASS.
- H2 application context and payroll snapshot replay/new-run/tenant isolation: PASS.
- Broad non-Docker run: 478 tests, 472 passed; six operations tests exposed an H2-master omission of existing production migration V192. After restoring parity, the affected suite passed 6/6. Full post-fix rerun remains for the next release checkpoint.
- Frontend procurement component: 13/13 PASS; i18n and hardcoded-UI gates PASS; production build PASS.
- Error codes: 424/424 PASS. Translation catalog IDs and key/locale pairs are unique.
- PostgreSQL/Testcontainers and Compose smoke remain blocked by the unavailable Docker daemon. External CI/billing remains blocked.

Single source of truth for automated test counts and regression baselines. Every
entry records the date, the repository HEAD SHA it was run against, the command,
and the result. The CI pipeline enforces the baselines below: a run that drops
below a recorded baseline or reports failures fails the release gate.

## Baselines (enforced by CI)

| Suite | Baseline | Command | Threshold rule |
|-------|----------|---------|----------------|
| Backend (non-Docker, H2) | **535 tests / 146 suites / 0 failures** | `./gradlew test -PskipDockerTests` | count ≥ 535 AND failures = 0 |
| Backend (full, incl. Testcontainers) | **310 tests** expected when Docker available | `./gradlew test` | failures = 0 |
| Frontend (Angular + Vitest) | **284 tests / 50 files / 0 failures** | `npx ng test --watch=false` | count ≥ 284 AND failures = 0 |

## Evidence log

### 2026-08-12 — release-checklist remediation working tree (baseline `a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8`)

- Status: **VERIFY, not final release evidence**. Changes are uncommitted, so there is no candidate SHA or reviewer sign-off yet.
- Backend compile: isolated workspace copy, `./gradlew.bat compileJava compileTestJava -PskipDockerTests` — **BUILD SUCCESSFUL**.
- Focused backend: `./gradlew.bat test --tests 'com.bemo.hr.payroll.application.*' --tests 'com.bemo.hr.manufacturing.production.application.*' -PskipDockerTests` — **BUILD SUCCESSFUL**. Coverage includes snapshot replay after salary/attendance/policy changes, policy validation/effective selection, frozen-BOM active readiness, and issue-evidence completion costing.
- H2/Liquibase context: `./gradlew.bat test --tests 'com.bemo.hr.shared.security.MeIdentityIntegrationTests' -PskipDockerTests` — **BUILD SUCCESSFUL** after adding V186 to the H2 mirror, quoting its 92 inline decimal types, making its 78 foundation creates idempotent against baseline tables, and loading V201–V204.
- Static backend gates: `python tools/check-error-codes.py` — **420/420 PASS**; `python tools/check-translation-catalog.py` — **7214 rows PASS**; `python tools/check-authorization-contract.py` — **19/19 roles PASS**.
- Not run for this working tree: complete backend suite/count refresh, PostgreSQL/Testcontainers, Liquibase PostgreSQL fresh/upgrade path, frontend suite/build, production-like Compose smoke, independent CI. These remain required by `REL-001`/`REL-002` and the release checklist.

### 2026-08-12 — Staff-audit confirmed gaps

- Frontend: **278 tests / 50 files / 0 failures**. Added the guarded dispatch/assignment/dispute workbench and API-contract tests; route/catalog parity, bilingual DB i18n, hardcoded UI scan, and Angular development build pass.
- Backend focused verification: dispatch/dispute lifecycle tests, access catalog, and H2 Spring/Liquibase context pass on the explicit Java 21 toolchain. V199 contains unique bilingual UI/error rows and an idempotent existing-user menu grant; translation-catalog, error-code, and authorization-contract gates pass.

### 2026-08-10 — Product Epic 12 second vertical pack

- Backend: **391 tests / 78 suites / 0 failures**. V184 removes contractor-specific constants from the pack engine and stores defaults, roles, KPIs, import templates and onboarding steps as versioned pack metadata. It seeds `FOOD_DISTRIBUTION_EG` with dependency-closed modules, FEFO/credit/expiry defaults, four roles, nine KPIs, four templates and nine ordered steps; V185 localizes the pack. Tests prove sector-specific metadata, install evidence and optional-step behavior while existing upgrade customization remains preserved. H2 and error parity **332/332** pass.
- Frontend: **269 tests / 49 files / 0 failures**. The existing Super Admin pack workspace renders the second vertical wholly from the backend contract without sector-specific UI code. i18n/hardcoded gates remain green and production build succeeds with existing SCSS warnings only.

### 2026-08-10 — Product Epic 11 support and customer health

- Backend: **390 tests / 78 suites / 0 failures**. V182 creates tenant tickets, immutable ticket updates, privacy-limited feedback and immutable health snapshots; V183 supplies bilingual UI/errors. Ticket creation and health calculation lock the tenant and are operation-idempotent. Critical impact and deterministic SLA, versioned state transitions, feedback route sanitization and a seven-dimension explained 100-point score are covered. Every health dimension returns status and remediation route. H2 and error parity **332/332** pass.
- Frontend: **268 tests / 49 files / 0 failures**. The global Help entry opens tickets, feedback/rating and an Admin health workspace with direct remediation actions. Tests cover loading, ticket contract/idempotency, safe feedback context and health recalculation. i18n **2240**, hardcoded-UI clean over 46 HTML/122 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 10 subscription and plan control plane

- Backend: **385 tests / 77 suites / 0 failures**. V180 creates data-driven global plans, one versioned tenant subscription and immutable tenant/idempotent change evidence; V181 supplies bilingual UI/errors. Plan changes lock the tenant, validate dependency closure, synchronize entitlement order safely, preserve all ERP data on downgrade, disable features on cancellation, audit mutations and enforce the real user limit in account creation. Tests cover change/replay, cancellation, dependency validation, configuration preservation, limit/inactive enforcement and explicit Super Admin permission. H2 and error parity **326/326** pass.
- Frontend: **264 tests / 48 files / 0 failures**. Super Admin settings expose current lifecycle, usage/limits, date/status changes, data-driven plan definitions, feature membership and immutable history. i18n **2197**, hardcoded-UI clean over 45 HTML/121 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 9 analytics and activation

- Backend: **377 tests / 76 suites / 0 failures**. V178 adds tenant/idempotent raw events, durable daily aggregates and one-shot activation milestones; V179 supplies bilingual UI/errors. A strict scalar property allowlist rejects sensitive/structured payloads. A separate proxy-based safe sink prevents analytics failure from rolling back ERP work. Retention removes raw events while aggregates/milestones survive; tenant summaries remain isolated and platform SQL is explicitly Super Admin-only. Tests cover replay/aggregation/milestones, privacy, aggregate summary, failure isolation and permission contract. H2 and error parity **320/320** pass.
- Frontend: **261 tests / 47 files / 0 failures**. Navigation telemetry strips query/fragment data and tolerates failure; Admin activation UI shows events/days/score/milestones/features, while Super Admin sees platform tenants and controls raw retention. i18n **2165**, hardcoded-UI clean over 44 HTML/120 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 7 supplier and contractor risk scores

- Backend: **372 tests / 75 suites / 0 failures**. V176 adds tenant/versioned descending risk rules and immutable operation-idempotent subject snapshots under a tenant lock; V177 supplies bilingual score/risk/error copy. Supplier and contractor formulas consume existing lifecycle, compliance, bank, profile, worker, settlement and issue evidence; every weight and remediation route is returned. Tests prove complete supplier/contractor scores, transparent components, snapshot replay/audit and threshold validation. H2 and error parity **318/318** pass.
- Frontend: **258 tests / 45 files / 0 failures**. Procurement/workforce/finance roles can inspect weighted scorecards and fix links; only Admin/Super Admin can edit optimistic-versioned thresholds. i18n **2147**, hardcoded-UI clean over 43 HTML/118 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 6 Action Center 2.0

- Backend: **368 tests / 74 suites / 0 failures**. V174 extends tenant notifications with exception, localized impact/reason/recommendation, monetary impact, role targets and safe internal action metadata; V175 supplies bilingual card copy. Ranking is deterministic (severity + unread + decisive role match), old payloads remain source-compatible, external links are rejected, mark-read ownership remains enforced, and sends remain audited. Tests cover role ordering, full guidance contract and link safety. The H2 mirror now correctly includes historical V112/V113 before V174. Error parity **316/316**.
- Frontend: **255 tests / 44 files / 0 failures**. The existing navbar Action Center renders Backend-ranked exception cards, localized priority, impact/reason/recommendation/amount, and direct internal action while preserving fields on mark-read. i18n **2137**, hardcoded-UI clean over 42 HTML/117 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 5 guided onboarding and data quality

- Backend: **365 tests / 74 suites / 0 failures**. V172 stores immutable, operation-idempotent assessment evidence under a pessimistic pack lock; V173 supplies bilingual UI/errors. The evaluator reuses only the selected pack's dependency graph, auto-completes from tenant-scoped contractor/category/worker/import/settlement evidence, requires `IMPORTED`, preserves explicit optional `SKIPPED`, scores quality, and declares `READY` only at complete required setup plus ≥80 quality. Tests cover relevant-step selection, blockers/action routes, import auto-completion, optional skip/unlock, final readiness and replay. H2 and error parity **316/316** pass.
- Frontend: **253 tests / 43 files / 0 failures**. Admin/Super Admin go-live settings expose progress, quality, readiness, localized steps, blockers and direct remediation routes. i18n **2132**, hardcoded-UI clean over 42 HTML/117 TypeScript files, production build green with existing SCSS warnings only.

### 2026-08-10 — Product Epic 4 trial and demo template

- Backend: **361 tests / 73 suites / 0 failures**. V170 adds the tenant commercial lifecycle, versioned demo templates and tenant-owned sample rows; V171 supplies bilingual UI and stable errors. The central write policy keeps expired trials readable while returning `TRIAL_EXPIRED_READ_ONLY` for ERP mutations. Tests cover exact 14-day dates, expired write blocking, tenant-preserving conversion, non-demo reset rejection, operation-idempotent start/conversion/reset, versioned sample replacement and audit evidence. H2 and error parity **314/314** pass.
- Frontend: **250 tests / 42 files / 0 failures**. Super Admin settings expose commercial state, trial dates/access, versioned demo start, paid conversion, safe reset and sample evidence. i18n **2118**, hardcoded-UI clean over 41 HTML/116 TypeScript files, production build green with the four pre-existing SCSS budget warnings only.

### 2026-08-10 — Product Epic 3 first industry pack

- Backend: **352 tests / 71 suites / 0 failures**. V168 creates global pack metadata, tenant version/install evidence and dependency-aware onboarding steps; V169 seeds `CONTRACTOR_WORKFORCE_EG` plus bilingual copy. Installation is operation-idempotent, activates required entitlements through Epic 2, snapshots terminology/dashboard/KPIs/roles/templates, and audits. Tests cover install/replay, blocked prerequisites, optional steps, JSON customization, and v1→v2 replay-safe upgrade preserving customer settings. H2 and error parity **308/308** pass.
- Frontend: **247 tests / 41 files / 0 failures**. Super Admin settings show pack version/modules/roles/KPIs/import templates, go-live steps, custom JSON and non-destructive upgrade. i18n **2101**, hardcoded-UI clean over 40 HTML/115 TypeScript files, production build green.

### 2026-08-09 — Product Epic 2 module entitlement catalog

- Backend: **347 tests / 70 suites / 0 failures**. `EntitlementCatalog` is now the canonical feature/default/dependency/API-prefix source used by both identity and backend interception. V166 adds mandatory change reason to versioned tenant rows; V167 adds bilingual administration/error copy. Tests prove dependency and dependent blocking, effective catalog grouping, and immutable audit. Error parity **302/302**.
- Frontend: **244 tests / 40 files / 0 failures**. Super Admin settings expose effective modules, dependencies, current state, mandatory reason, versioned mutation, and reload. i18n **2079**, hardcoded-UI clean across 39 HTML/114 TypeScript sources, production build green.

### 2026-08-09 — Product Epic 1 AR foundation

- Backend: `./gradlew test -PskipDockerTests` → **343 tests / 69 suites / 0 failures**. V164 adds tenant credit profiles, customer invoices, retry-safe receipts, locked allocations, and collection tasks; V165 adds 43 bilingual UI keys plus stable errors. `SalesReceivablesServiceTests` covers credit-limit blocking, partial allocation and partner-ledger credit, operation replay, deterministic aging buckets, and overdue-task creation. H2 context and error parity **300/300** pass.
- Frontend: `npm test -- --watch=false` → **241 tests / 39 files / 0 failures**. Sales workbench tests cover the five-source load, epoch-date invoice contract, and receipt allocation with generated operation ID. `check:i18n` passes **2067 literal keys**, hardcoded-UI is clean, and the production build is green with existing SCSS budget warnings only.

### 2026-08-09 — P0.5 advanced attendance

- Backend: `./gradlew test -PskipDockerTests` → **338 tests / 68 suites / 0 failures**. V162 creates effective-dated tenant/category/employee policies plus immutable scored exception evidence with optimistic locking and payroll indexes; V163 loads bilingual UI and stable errors. Tests cover policy precedence, cross-midnight assignment, outage/missing-punch scoring, manual override, locked-period rejection, payroll blocking, and H2 migration context. Error-code parity is **284/284**.
- Frontend: `npm test -- --watch=false` → **238 tests / 38 files / 0 failures**. Review tests cover critical filtering, policy/explanation visibility, and a preview contract that leaves daily evidence unchanged until confirmation. `check:i18n` passes **2024 literal keys**, `check:hardcoded` scans 38 HTML + 113 TypeScript files with zero findings, and the production build is green with the existing SCSS budget warnings only.

### 2026-08-09 — P0.4 advanced approvals

- Backend: `./gradlew test -PskipDockerTests` → **332 tests / 67 suites / 0 failures**. V160 creates immutable instance-step snapshots, versioned dated delegations, SLA/escalation fields and locked document uniqueness; its PostgreSQL/H2 upgrade backfill preserves existing definitions and decision links. V161 loads bilingual UI/error copy. `ApprovalWorkflowServiceTests` covers ANY_N threshold behavior, delegation during original-user absence, invalid delegation windows, self-approval blocking, snapshot-only decisions, and escalation. Clean H2 context and error-code parity **263/263** pass.
- Frontend: `npm test -- --watch=false` → **236 tests / 38 files / 0 failures**. Approval service and pending-workspace tests cover delegation contracts/state, overdue/delegated filters, summary counts, required date windows, and signed-in delegator payload. `check:i18n` **1994 literal keys**, `check:hardcoded` 0/38 HTML + 113 TypeScript, development build green.

### 2026-08-09 — P0.3 bank reconciliation and cash position

- Backend: `./gradlew test -PskipDockerTests` → **327 tests / 67 suites / 0 failures**. V158/V159 load cleanly on H2. `BankReconciliationServiceTests` covers exact matching, partial payment plus a balanced fee journal, closed-period rejection, controlled reversal, duplicate-file rejection, balance validation, and currency-separated cash totals. Statement and line aggregates are pessimistically serialized for writes.
- Frontend: `npm test -- --watch=false --no-progress` → **231 tests / 37 files / 0 failures**. `banks.page.spec.ts` covers the four-source load, real multipart CSV import, idempotent auto-match contract, and backend-provided partial suggestion prefill. `check:i18n` **1949 literal keys**, `check:hardcoded` 0/38 HTML + 113 TypeScript, production build green with the four pre-existing SCSS budget warnings.

### 2026-08-09 — P0.2 supplier onboarding and Supplier 360

- Backend: `./gradlew test -PskipDockerTests` → **320 tests / 66 suites / 0 failures**. V156/V157 load cleanly on H2. `SupplierOnboardingServiceTests` covers duplicate tax/IBAN detection, expired and unverified mandatory documents, suspended procurement, and persisted document bytes; `SupplierPaymentValidationTests` proves unverified bank accounts cannot be paid. Error-code parity **271/271**.
- Frontend: `npm test -- --watch=false --no-progress` → **227 tests / 36 files / 0 failures**. New store contract coverage verifies supplier-request routing, duplicate parameters, Supplier 360, and lifecycle refresh. `check:i18n` **1908 literal keys**, `check:hardcoded` 0 findings across 38 HTML + 113 TypeScript sources, production build green with only the four pre-existing SCSS budget warnings.

### 2026-08-09 — P0.1 inventory valuation and Inventory GL

- Backend: `./gradlew test -PskipDockerTests` → **312 tests / 65 suites / 0 failures**. `InventoryValuationServiceTests` covers weighted average, FIFO layer consumption, returns at pre-return average cost, backdated rejection, closed-period rejection, balanced Inventory GL posting, revaluation replay idempotency, and pessimistic item-lock configuration. V154/V155 load cleanly on H2; error-code parity **267/267**.
- Frontend: `npm test -- --watch=false` under Node `v24.18.0` → **223 tests / 35 files / 0 failures**. Operations-store valuation contract coverage is green; translation-management async tests now await component promises. `check:i18n` **1875 keys**, `check:hardcoded` 0 findings across 38 HTML + 113 TypeScript sources, production build green with the four pre-existing SCSS budget warnings.

### 2026-08-09 — working tree (Frankfurter online exchange-rate hint integration)
- Applied the Bortqala `BORTQALA_FRANKFURTER_INTEGRATION_2026-08-09` package (released against `da9374a`; repo HEAD `0f230db` is semantically identical, whitespace-only). Package files overlaid directly: new backend package `com.bemo.hr.finance` additions (`ExchangeRateHintApi`/`Service`/`Scheduler`/`Setting`/`Repository`/`FrankfurterExchangeRateClient` + unit test `ExchangeRateHintServiceTest`), `Currency` gains `reference_*` hint columns and `updateReferenceRate`/`markReferenceUnavailable`, `TreasuryApi`/`Controller` gain the three `/api/v1/finance/exchange-rate-hints/*` endpoints and reference-rate metadata in `GET /currencies`. Frontend `tax-currency.page.{ts,html,scss}` gains the online-reference panel (configured vs reference rate, provider date, last/next sync, difference %, refresh interval config + manual refresh for admins/Finance Managers). All `i18n.t(...)` (no third-arg fallbacks), no hardcoded UI strings.
- **Liquibase renumbering (V148/V149 → V150/V151)**: the package shipped `v148`/`v149` names that collide with the repo's already-registered V148 logout-scope and V149 audit-log translations. Renumbered: **V150** `20260809_v150_frankfurter_exchange_rate_hints.yaml` (`exchange_rate_hint_settings` table + unique app FK + 6 `currencies` reference columns; changeset id `20260809-v150-frankfurter-exchange-rate-hints`) and **V151** `20260809_v151_frankfurter_exchange_rate_translations.{yaml,csv}` (32 keys × 2 locales = 64 rows, ids `v151-001`…`v151-064`). Fixed two package defects: (1) the CSV reused `v149-057-ar`/`v149-058-en` for both `FRANKFURTER_UNAVAILABLE` and `onlineHoursUnit` (would violate the `translations.id` PK) → sequential unique ids; (2) row 27 embedded an unquoted `;` in the en-US `onlineEnabledHint` text → quoted per repo CSV convention. Two keys used by the page TS (`taxCurrency.taxCreated`/`currencyCreated`) were missing from the package CSV → added as rows 061–064. Also fixed the package schema YAML `comment:` that contained an unquoted `:` (Liquibase "mapping values are not allowed here"). Registered V150+V151 in `next.changelog-master.yaml` and `test-h2.changelog-master.yaml`.
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL**; JUnit XML summary: **299 tests, 63 suites, 0 failures, 0 errors, 0 skipped** (+2 from `ExchangeRateHintServiceTest`: rate-direction reciprocal `toBaseRate` and zero-rate rejection). V150/V151 load cleanly on H2 (`MeIdentityIntegrationTests` green); error-codes gate **262/262**. Baseline refreshed to 299/63 in `be/tools/check-test-count.py`.
- Frontend: `npx ng test --watch=false` under node `v24.18.0` → **33 files passed, 211 tests passed, 0 failed** (no new specs shipped; baseline unchanged). `npm run check:i18n` = **1829 keys** (ar-EG + en-US; +31 from V151); `npm run check:hardcoded` = 0 findings across 37 HTML templates and 112 TypeScript sources; `npx ng build` green (pre-existing SCSS budget warnings only: dashboard, report-review, users, app-shell).

### 2026-08-09 — working tree (FINAL-001 + FINAL-002 release-gate fixes)
- **FINAL-002 (Audit Logs empty/error state)**: root cause was frontend-only — on API failure the page rendered the error banner *and* the table's `@empty` block simultaneously (false "لا توجد سجلات تدقيق مسجلة بعد." + bogus pagination). `audit-logs.page.ts` gained `retry()`; `audit-logs.page.html` now renders a `.load-error` block (`audit.loadErrorTitle` + `audit.loadErrorHint` + detail + `common.retry` button) via `@if (error())` and only renders the table+pagination in the `@else` branch (removed the stale duplicate error banner); new `.load-error` SCSS. New **Liquibase V149** `20260809_v149_audit_log_error_state_translations.{yaml,csv}` (2 keys × 2 locales, ids `v149-001/002` per locale), registered in `next` + `test-h2` changelogs.
- **FINAL-001 (attendance decision persistence)**: backend verified correct (`ReportingService.decideDaily` persists in one transaction; `@Version` optimistic locking). Frontend gap closed: `report-review.page.html` confirm button now switches from `common.confirm` to `common.retry` when the prompt shows a persisted failure error (modal already stays open, input preserved, re-enabled on completion; no optimistic counter changes on failure).
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL**; JUnit XML summary: **297 tests, 62 suites, 0 failures, 0 errors, 0 skipped**. V149 loads cleanly on H2 (`MeIdentityIntegrationTests` green); error-codes gate **262/262**.
- Frontend: `npx ng test --watch=false` under node `v24.18.0` → **33 files passed, 211 tests passed, 0 failed** (+3: `audit-logs.page.spec.ts` error-state DOM test asserting `.load-error` replaces `.table-card`/`audit.empty` + `retry()` refires the GET and clears the error; `report-review.page.spec.ts` DOM test asserting the confirm button renders `common.retry` enabled after a 409). Baseline refreshed to 211/33 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = **1798 keys** (ar-EG + en-US; +2 from V149); `npm run check:hardcoded` = 0 findings across 37 HTML templates and 112 TypeScript sources; `npx ng build` green (pre-existing SCSS budget warnings only).

### 2026-08-09 — working tree (logout-scope fix: current-browser vs all-devices)
- Applied the `bortqala_logout_sessions_fix` patch (idempotent `apply_logout_fix.py`, all 15 replacements applicable exactly once): the Sign-out action now opens a scope dialog — "Sign out from this browser" (default, posts `POST /api/v1/auth/logout`, broadcasts a `bemo-erp-logout-event` localStorage event so every tab of the same user signs out) or "Sign out from all devices" (new `POST /api/v1/auth/sessions/revoke-all` → `AuthService.revokeOwnSessions` bumps the JWT `tv` token version, revokes every refresh token for the user, and records a `SESSIONS_REVOKED_SELF` audit row). Cross-tab sync: `AuthService` listens for the storage event and clears only tabs for the same user; `AppShellComponent` gained a redirect effect when `user()` becomes null. New **Liquibase V148** `20260809_v148_logout_scope_translations.{yaml,csv}` (9 keys × 2 locales, ids `v148-001`…`v148-009` per locale: `auth.logoutTitle/Hint/CurrentBrowser/CurrentBrowserHint/AllDevices/AllDevicesHint/AllDevicesWorking/AllDevicesError` + `action.cancel`), registered in `next` + `test-h2` changelogs.
- Backend: `./gradlew test -PskipDockerTests` in `be/` → **BUILD SUCCESSFUL**; JUnit XML summary: **297 tests, 62 suites, 0 failures, 0 errors, 0 skipped**. V148 loads cleanly on H2 (`MeIdentityIntegrationTests`); targeted `AuthSecurityIntegrationTests` + `AuthServiceTenantIsolationTests` green.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **33 files passed, 208 tests passed, 0 failed**. New coverage in `auth.service.spec.ts` (7 tests: `logout()` current-browser default, `logoutCurrentBrowser`, `logoutAllDevices` success + failure-keeps-session, and three storage-event sync cases — same-user clears, different-user ignored, malformed/unrelated ignored). Baseline refreshed to 208/33 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = **1796 keys** (ar-EG + en-US; +9 from V148); `npm run check:hardcoded` = 0 findings across 37 HTML templates and 112 TypeScript sources; `npx ng build --configuration production` green (pre-existing SCSS budget warnings only); error-codes gate `check-error-codes.py` = **262/262**.

### 2026-08-08 — working tree (QA remediation REM-002…REM-008 + REM-005 frontend)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL**; JUnit XML summary: **297 tests, 62 suites, 0 failures, 0 errors, 0 skipped**. New/updated suites: `OperationsDocumentReferencesTests` (5; fixed 2 test bugs — the financial-only movement asserts a ledger entry with no stock row, and created movement/ledger ids are now tracked so `@AfterEach` cleanup deletes in FK-safe order), `EmployeeCodeDedupServiceTests` (6). Liquibase V131 (rewritten mapping-table dedup migration), V142 (Postgres case-insensitive unique index), V143/V144 (translations) all load cleanly on H2.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **33 files passed, 201 tests passed, 0 failed**. New spec: `operations.page.spec.ts` (7 tests: separate document-reference payload + attachment metadata, required-reference blocking, per-operation required sets, `documentTypeLabel`, `primaryReference` precedence, oversized/unsupported attachment rejection). `reports.page.spec.ts` updated for the `create` → `applyPreset` rename (REM-006).
- Related: `npm run check:i18n` = **1707 keys** (ar-EG + en-US; +37 from V141/V143/V144); `npm run check:hardcoded` = 0 findings across 36 HTML templates and 109 TypeScript sources; `npm run build` green (pre-existing SCSS budget warnings only); error-codes gate `check-error-codes.py` = **257/257** (V144 closes the 5 missing `WORKER_CATEGORY_*`/`WORKFORCE_CATEGORY_*` gaps). Baselines refreshed to 297/62 (backend) and 201/33 (frontend) in both `check-test-count` gates.
- REM-005 frontend delivered: separate PO/receipt/delivery-note/invoice/voucher/external-ref/warehouse fields + attachment metadata (max 5 MB, images/PDF/Excel) in the transaction form, per-operation-type required-reference marking/validation, movements table resolves a primary document number with warehouse/attachment shown beneath.
### 2026-08-08 — working tree (REM-002 category scope + REM-008 shortcut select sync)
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **30 files passed, 178 tests passed, 0 failed**. New specs: `categories` scope columns, `WorkerCategoryService`-related FE fixtures. Removed the leftover always-failing diagnostic `fe/src/app/debug-shortcut.spec.ts` (`expect(out).toBe('__FAIL__')`). Fixed 5 pre-existing DOM failures in `shortcut-settings.component.spec.ts` (added after Session 11 in commit `1004b17 "ss"`): root cause was Angular's `SelectControlValueAccessor.writeValue` running *before* the nested `@for` rendered the `<option>` elements, so `select.value` was set with no matching option and never re-written (the model value never changed). Fix: `ngAfterViewChecked` re-syncs each `.shortcut-destination-select` to its draft's `pageCode` (real browsers auto-select the first appended option; jsdom does not), and `changeDestination` now rejects any value not offered by `destinationOptions(index)` (guards against stale/unknown select values — the same desync class as the blocked-duplicate revert).
- Related: `npm run check:i18n` = 1677 keys (ar-EG + en-US; +7 from V135 category-scope translations); `npm run check:hardcoded` = 0 findings. Baseline refreshed to 178/30 in `fe/tools/check-test-count.mjs`.
- Backend: full `./gradlew test -PskipDockerTests` run in progress at time of writing — see next entry for the verified result and refreshed backend baseline.

### 2026-08-08 — working tree (settings QA: notification prefs + shortcuts UI fix)
- Notification prefs no longer persist on toggle: `updateNotificationPrefs` in `settings.page.ts` only updates the `notificationPrefs` signal; `saveNotificationPrefs()` moved into `saveUserPreferences()` (the 💾 Save All System Settings handler, both appearance + reports tabs), and `cancel()` resets prefs to the last-persisted `loadNotificationPrefs()`.
- Shortcut settings theme fix: `shortcut-settings.component.scss` and `settings.page.scss` replaced non-existent tokens (`--surface-card`, `--border-color`, `--text-muted`, `--surface-header`, `--surface-input`, `--primary`) with the app's real tokens (`--surface`, `--line`, `--muted`, `--surface-muted`, `--input-bg`, `--gold`, `--gold-glow`, `--success-soft`, `--warning-soft`, `--danger-soft`, `--warning-text`) — fixes white-cards-in-dark-mode.
- Shortcut destination `<select>` desync fix: Angular skips DOM writes for property bindings when the bound value is unchanged, so a blocked change (duplicate destination) left the select showing the stale picked value. `changeDestination` now takes the `Event` and reverts `select.value` imperatively on the blocked branch.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 159 tests passed, 0 failed** (+4 new DOM/interaction assertions in `shortcut-settings.component.spec.ts`: initial select render, async profile-load render, change sync, blocked-change revert). Baseline refreshed to 159/28 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = 1670 keys (ar-EG + en-US; -1 because `settings.notificationSaved` is no longer invoked after the save-deferral); `npm run check:hardcoded` = 0 findings across 36 HTML templates and 109 TypeScript source files; `npx ng build --configuration production` green (pre-existing SCSS budget warnings only).

### 2026-08-07 — working tree (procurement hardening + TS hardcoded-UI pass)
- Applied the "Bortqala cumulative patch" (operation-specific procurement busy states, three-way-match i18n, TS hardcoded-UI scanner, users/employees localization). Fixed a bug in the delivered V126 CSV: `translations.id` is the primary key, but V126 reused `v126-001/002/003` for both locales (would violate the PK on the H2/PostgreSQL Liquibase load); rewrote to distinct per-locale ids (`v126-001`…`v126-006`, matching the V119/V125 convention).
- The new TS scan then surfaced **119 hardcoded notification/confirm/fallback strings** in 13 files that the patch did not address. Finished the pass: removed 35 third-argument `i18n.t(key, …, 'fallback')` fallbacks (all keys already present in the DB store — verified `imports.*`, `review.*`, `payroll.*`, `manualAttendance.*`, `settings.*`, `operations.invalidNegativeQuantity`), wrapped 21 `notification.success/error(...)` + `window.confirm(...)` literals with `i18n.t(...)` keys, and injected `I18nService` into 4 components that lacked it (settlement-periods, contractor-settlement-detail-modal, workflow-definitions, pending-approvals).
- New **Liquibase V127** `20260807_v127_hardcoded_notification_translations.{yaml,csv}` (21 keys × 2 locales, ids `v127-001`…`v127-042`) registered in `next` + `test-h2` changelogs.
- Backend: `./gradlew test -PskipDockerTests` in `be/` → **BUILD SUCCESSFUL**; JUnit XML summary: **268 tests, 56 suites, 0 failures, 0 errors, 0 skipped**. V126 + V127 both load cleanly on H2.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 155 tests passed, 0 failed** (+5 from the patch: 3 users-page role-summary tests + 2 procurement operation-state tests). Baseline refreshed to 155/28 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = 1668 keys (ar-EG + en-US; +21 from V126/V127); `npm run check:hardcoded` = 0 findings across 36 HTML templates and 109 TypeScript source files (was 119 candidates before this pass); `npm run build` green (pre-existing SCSS budget warnings only).

### 2026-08-07 — HEAD `498227e` (working tree, B-1 budget & encumbrance landed)
- Backend: `./gradlew test -PskipDockerTests` in `be/` → **BUILD SUCCESSFUL in 14m 26s**; JUnit XML summary: **268 tests, 56 suites, 0 failures, 0 errors, 0 skipped** (`be/build/test-results/test/*.xml`). New suites: `com.bemo.hr.budget.*` (BudgetService + encumbrance lifecycle + controller), plus the BUDGETS `AccessCatalog` page. Fixed a broken V125 CSV (stray `;` in `procurement.departmentHint` en-US text split the row into 5 columns and failed the H2 Liquibase migration — all 53 failing context-load tests were one root cause).
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 150 tests passed, 0 failed**. New spec: `budgets.page.spec.ts` (11 tests). Fix: MONTHLY prefill is now driven by a reactive `valueChanges` subscription (`takeUntilDestroyed`) instead of a DOM `(change)` handler so control-level `setValue` also prefills.
- Related: `npm run check:i18n` = 1645 keys (ar-EG + en-US; +42 from V123/V125); `npm run check:hardcoded` = 0 findings across 36 templates; `npm run build` green (pre-existing SCSS budget warnings only; local build validated with `optimization.fonts.inline=false` because this sandbox has no route to fonts.googleapis.com — CI keeps full optimization). Backend baseline scripts fixed: `check-test-count.py` BASE_DIR resolved to the repo root (`be/`), not `be/tools/`.
- Note: local `node` is `v26.5.1` (default), which violates `engines >=22 <25` and breaks `localStorage` in Vitest under jsdom (`--localstorage-file` absent). Tests must run under node 24 (`nvm use 24`, per `.nvmrc`).

### 2026-08-07 — HEAD `498227e` (working tree, A-4 trends surface landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` (native Gradle mirror, source synced from `be/src` on 2026-08-07) → **BUILD SUCCESSFUL in 2m 42s**; JUnit XML summary: **252 tests, 55 suites, 0 failures, 0 errors, 0 skipped** (`/tmp/opencode/be-build/build/test-results/test/*.xml`). New suite: `DashboardServiceTrendsTests` (2 tests) for the multi-period `/api/v1/dashboard/trends` surface.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **27 files passed, 139 tests passed, 0 failed**. New spec: `dashboard.store.spec.ts` (3 tests) covering `loadTrends` + `downloadTrends`.
- Related: `npm run check:i18n` = 1603 keys (ar-EG + en-US; +12 from V121); `npm run check:hardcoded` = 0 findings across 35 templates; `npm run build` green (pre-existing SCSS budget warnings only). Full suite adds 40 Testcontainers tests which cannot run locally (Docker daemon down in this WSL env) — CI provides Docker.

### 2026-08-07 — HEAD `498227e` (working tree, E-1 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` (native Gradle mirror, source synced from `be/src` on 2026-08-07) → **BUILD SUCCESSFUL in 2m 36s**; JUnit XML summary: **250 tests, 54 suites, 0 failures, 0 errors, 0 skipped** (`/tmp/opencode/be-build/build/test-results/test/*.xml`).
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **26 files passed, 136 tests passed, 0 failed**.
- Related: `npm run check:i18n` = 1591 keys (ar-EG + en-US); `npm run check:hardcoded` = 0 findings across 35 templates; `npm run build` green (pre-existing SCSS budget warnings only). Full suite adds 40 Testcontainers tests (run 29/30 family: PunchSourceIdentity 3, WorkforceImportCommit 10, SupplierPayment 10, ReportingBulkDecision 1, LiquibaseUpgradePath + migration/concurrency suites) which cannot run locally (Docker daemon down in this WSL env) — locally they manifest as `NoClassDefFoundError: Could not initialize PostgresIntegrationTest`, and CI provides Docker.

### 2026-08-06 — HEAD `498227e`
- Backend full `./gradlew test`: **290 tests, 259 green, 31 fail** — all 31 are `NoClassDefFoundError: Could not initialize PostgresIntegrationTest` (Docker daemon down; every failing suite extends the Testcontainers base). No non-Docker test fails.
- Frontend: `ng test` **136/136** (26 files), `check:i18n` 1424 keys, `ng build` green.

### 2026-08-06 — HEAD `ee8356e` (release gate, P3-01)
- Backend `./gradlew clean test`: **209 tests, 54 suites, 0 failures / 0 errors / 0 skipped**; PostgreSQL/Testcontainers concurrency 24/24; fresh-database Liquibase 141 changesets with `ddl-auto=validate`; upgrade-path from v1_v67 baseline green.
- Frontend: `npm ci` OK; `check:i18n` 1103 keys; `ng test` 46 tests / 17 files; `ng build` OK.


### 2026-08-24 — WP-04 fixed assets (working tree, V347 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 49s**; JUnit XML summary: **800 tests, 196 suites, 0 failures, 0 errors, 1 skipped**. New suites: `FixedAssetTests` (7) + `AssetDepreciationServiceTests` (8). Gates: `check-error-codes.py` **596/596**, `check-translation-catalog.py` **13,938 rows PASS**, `check-test-count.py` baselines raised to ≥800/≥196.
- Frontend: `npx ng test --watch=false` under node `v24.19.0` → **100 files passed, 475 tests passed, 0 failed**. New spec: `fixed-assets.page.spec.ts` (8 tests). Gates: `check:i18n` **4,646 keys** (+33 from V347); `check:hardcoded` 0 findings across 113 templates + 237 TS files; `ng build` green. FE baseline raised to ≥475/≥100.

### 2026-08-24 — WP-02 settlement-discount completion (working tree, V348 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 4m 4s**; JUnit XML summary: **801 tests, 196 suites, 0 failures, 1 skipped**. New test: `rejectsSettlementDiscountWithoutFinanceRole`; extended `discountedSettlementClosesInvoiceAndBooksDiscountEntry` (originalDue=100.00, subledger verify). Gates: `check-error-codes.py` **597/597** (+1 PROC_SETTLEMENT_DISCOUNT_FORBIDDEN), `check-translation-catalog.py` **13,948 rows PASS** (+10 v348 rows), baseline raised to ≥801/≥196.
- Frontend: `npx ng test --watch=false` under node `v24.19.0` → **100 files passed, 478 tests passed, 0 failed** (+3 procurement settlement specs). Gates: `check:i18n` **4,650 keys** (+4 v348 UI keys); `check:hardcoded` 0 findings; `ng build` green. Baseline raised to ≥478/≥100.

### 2026-08-24 — WP-07 loans deduction-policy switcher (working tree, V350 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 38s**; JUnit XML summary: **816 tests, 196 suites, 0 failures, 0 errors, 0 skipped**. New tests: +9 in `WorkforceEmployeeAdvanceServiceTests` (resolver defaults/category-override/global-fallback, manual rejected when AUTO, blank period, overdue collection + idempotent replay, nothing-due, INVALID mode save, overlapping open version EXISTS) and +2 in `PayrollServiceTests` (`getSheet_skipsAdvanceDeductionAndKeepsZeroWhenPolicyIsManual`, `getSheet_stillAutoDeductsAdvancesWhenPolicyIsAuto`). Gates: `check-error-codes.py` **601/601** (+4 ADVANCE_POLICY_* / ADVANCE_MANUAL_NOT_DUE / ADVANCE_NOTHING_DUE), `check-translation-catalog.py` **14,026 rows PASS** (+46 V350 rows ar+en), baseline raised to ≥816/≥196 (verified via mirror tools copy).
- Frontend: `npx ng test --watch=false` under node `v24.x` → **101 files passed, 487 tests passed, 0 failed** (+5 `advances-policy-settings.component.spec.ts`, +2 employees gating/apply-loop). Gates: `check:i18n` **4,676 keys** (+26 V350 UI keys); `check:hardcoded` 0 findings across 114 templates + 238 TS files; `ng build` green; FE baseline raised to ≥487/≥101.

### 2026-08-25 — WP-10 vertical user-role templates (working tree, V351 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 18s**; JUnit XML summary: **822 tests, 197 suites, 0 failures, 1 skipped**. New suite: `UserRoleTemplateServiceTests` (6: menuOptions gating+vertical tags, template merge/dedupe by code, explicit-vertical param, unknown-vertical rejection, top-3 suggested policy groups by prefix overlap, FEATURE_VERTICALS static checks). Liquibase fixes during verification: V351 seed used `- sql:` instead of the invalid `sqlUpdate` change type; translations CSV header normalized to repo convention (`id;translation_key;locale;text_value`). Gates: `check-error-codes.py` **601/601**, `check-translation-catalog.py` **14,084 rows PASS**, baseline raised to **≥822/≥197**.
- Frontend: `npx ng test --watch=false` under node `v24.x` → **101 files passed, 490 tests passed, 0 failed** (+3 WP-10 cases in `users.page.spec.ts`: AC-1 feature-locked DOM test via teleported modal on `document.body`, template-apply menus+suggested-groups test, AC-3 static fallback test). Gates: `check:i18n` **4,679 keys** (+29 V351 keys); `check:hardcoded` 0 findings across 114 templates + 238 TS files; `ng build` green; FE baseline raised to ≥490/≥101.

### 2026-08-25 — WP-06 generated-report-periods registry (working tree, V352 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 24s**; JUnit XML summary: **824 tests, 197 suites, 0 failures, 1 skipped**. New endpoint `GET /api/v1/reports/generated-periods?year=` (`ReportingService.generatedPeriods` + repo derived query `findByPeriodStartBetweenAndStatusIn`, APPROVED+EXPORTED only; null/out-of-range year → empty list). New tests: +2 in `ReportingServicePeriodTests` (finalized-only ranges incl. EXPORTED and draft exclusion; missing/out-of-range years never error and never hit the repo — strict-stub lesson: no stub needed when the service short-circuits). Liquibase **V352** translations (2 keys × ar/en) registered in both masters. Gates: `check-error-codes.py` **601/601**, `check-translation-catalog.py` **14,088 rows PASS**, baseline raised to **≥824/≥197**.
- Frontend: `npx ng test --watch=false` under node `v24.x` → **101 files passed, 493 tests passed, 0 failed** (+3 `reports.page.spec.ts` cases: generated chip disabled+tooltip+exact-report link href, all-enabled fallback, year-change refetch with stale-token guard). Store fetches the registry independently with its own loading flag. Gates: `check:i18n` **4,681 keys** (+2 V352 keys); `check:hardcoded` 0 findings (114 templates + 238 TS); `ng build` green; FE baseline raised to ≥493/101.

## How to refresh a baseline
1. Update the evidence log entry above with the new date + HEAD SHA.
2. Re-run the exact command listed and paste the observed counts.
3. Only raise a baseline after a verified green run; never lower a baseline to silence a regression (open a blocker instead).

### 2026-08-25 — WP-10/WP-06/WP-08 completion pass (working tree, V351–V353 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 31s**; JUnit XML summary: **825 tests, 197 suites, 0 failures, 1 skipped**. New since last entry: `UserRoleTemplateServiceTests` (6), `ReportingServicePeriodTests` (+2), `DashboardServicePeakClockInTests.bucketsShiftWithConfiguredCompanyZone` (+1). Gates: `check-error-codes.py` **601/601**, `check-translation-catalog.py` **14,094 rows PASS**, `check-test-count.py` baseline raised to ≥825/≥197.
- Frontend: node 24 → **102 files passed, 498 tests passed, 0 failed**. New specs: users role-template cases (+3), reports generated-chip cases (+3), `dashboard.page.spec.ts` (4: legend/colors, category-filter refetch, months w/o filter, export blob), store download test (+1). Gates: `check:i18n` **4,682 keys**; `check:hardcoded` 0 violations across 114 templates + 238 TS files; `ng build` green; FE baseline raised to ≥498/≥101.
- Spec notes: WP-08 AC-4 clamps out-of-range months silently mirroring trends; export ships via repo-convention `/api/v1/exports/clock-in-histogram.xlsx`.

### 2026-08-25 — V354 Export Filename i18n Refactor (working tree)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` → **BUILD SUCCESSFUL in 3m 5s**; JUnit XML summary: **827 tests, 198 suites, 0 failures**. New suite: `DataExportControllerFilenameTests` (2: ar locale resolved from `export.file.*` translation catalog via `TranslationService`, en locale keeps scope slug). `DataExportController` no longer hardcodes Arabic filenames — resolved dynamically from DB. V354 YAML/CSV registered in both masters (22 rows, 11 keys × 2 locales). Error-codes **601/601**, translation catalog **14,116 rows PASS**.
- Frontend: node 24 → **498 tests / 102 files / 0 failures** (no new FE tests needed — existing specs cover export flows). 8 call sites across categories/dashboard/employees/operations/parties/reports stores replaced hardcoded Arabic filename strings with `this.i18n.t('export.file.<slug>')` (same V354 keys). `check:i18n` **4,690 keys**; `check:hardcoded` 0 violations (114 HTML + 238 TS); `ng build` green.
- Changelog lesson: `test-h2.changelog-master.yaml` is ~1123 lines — never use a string-replace script that nests a new `- include:` block inside a prior entry's `file:` property. V354 insert initially broke YAML block mapping for all `@SpringBootTest` context loads (85 cascading failures). Fixed by ensuring V354 is a sibling list item at 6-space indent.
- Baselines raised: `check-test-count.py` MIN_TESTS=827/MIN_SUITES=198.
