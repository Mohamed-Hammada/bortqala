# Documentation ↔ Implementation Reconciliation

**Date:** 2026-09-06
**Method:** Every claim below was checked against current source (`git HEAD` working tree), Liquibase migrations, and actual command output — not against historical notes, AI session summaries, or prior "DONE" claims. Commands were run in this environment; where a command could not be run (e.g. no Docker), that is stated explicitly rather than inferred.

Status values used below: `IMPLEMENTED`, `PARTIALLY_IMPLEMENTED`, `NOT_IMPLEMENTED`, `REGRESSED`, `BLOCKED`, `DOCUMENTATION_ONLY`, `UNCLEAR`.

---

## Executive Summary

**See the "Executive Summary" and "Final Alignment" at the bottom of this document (§17) for the completed version** — written after all verification passes returned, so it isn't duplicated here.

---

## 1. Verification Commands — Actually Run

| Command | Result | Notes |
|---|---|---|
| `python3 be/tools/check-error-codes.py` | **PASS** — 821 exception codes, 821 with translation rows, 0 missing | Higher than any count recorded in `docs/TEST_EVIDENCE.md` (last entry: 779/779 on 2026-08-30) — confirms further work landed after the evidence log's last entry (see Finding D-1). |
| `python3 be/tools/check-translation-catalog.py` | **PASS** — 18,364 rows, 0 DB-generated ids, unique key/locale pairs | Higher than the last recorded 17,297 rows (2026-08-30). |
| `python3 be/tools/check-authorization-contract.py` | **PASS** — 21 declared roles, 21 referenced, 0 unknown | Matches the 2026-08-30 count (21/21); role set has not grown since. |
| `npm run check:i18n` (fe) | **PASS** — 6,050 literal keys, ar-EG + en-US, no static dictionaries | Matches `PROJECT_MAP.md`'s Session-18 claim exactly (6,050 keys). |
| `npm run check:hardcoded` (fe) | **PASS** — 148 HTML templates, 330 TS files, 0 violations | Matches `PROJECT_MAP.md`'s Session-18 claim exactly. |
| `npm run test -- --watch=false` (fe), **system default Node v26.5.1** | **FAIL** — 254 failed / 456 passed of 710 tests, 51/144 files failing | **Environment artifact, not a regression** — see Finding D-2. Failures are `TypeError: ... reading 'getItem'`/`'removeItem'`/`'clear'` in `i18n.service.ts`/`auth.service.ts`, i.e. `localStorage` unavailable under jsdom on Node 26, cascading into `TestBed` re-instantiation errors. This exact failure mode is independently documented in `docs/TEST_EVIDENCE.md`'s 2026-08-29 entry ("node 26 breaks every localStorage-dependent spec (212 failures observed)"). |
| `npm run test -- --watch=false` (fe), **Node 24.18.1 via nvm** | **PASS** — 710/710 tests, 144/144 files, 0 failures | Confirms `PROJECT_MAP.md`'s "710 frontend unit tests across 144 test suites pass" claim exactly, once the documented toolchain (Node 24) is actually used. |
| `npm run build` (fe) | **PASS** — `ng build` succeeds, output at `fe/dist/fe` | 2 non-blocking budget warnings (initial bundle +30.8 kB over 500 kB budget; `users.page.scss` +1.79 kB over 30 kB budget) — consistent with the "pre-existing SCSS budget warnings" pattern recorded throughout `docs/TEST_EVIDENCE.md`. |
| `./gradlew test -PskipDockerTests` (be) | **PASS — 1,510 tests / 286 suites / 0 failures / 0 errors / 1 skipped, BUILD SUCCESSFUL in 5m 37s** | See Finding V-1 below — native Gradle on the `/mnt/d` WSL mount made essentially no progress (3 of 286 suites in ~40 minutes); switching to the project's own documented rsync-to-ext4 workaround (`docs/BUILD_TOOLING.md`) let the identical suite finish in 5m37s. Result is above the enforced floor (1,469/280 — see Finding D-3) and higher than any count previously logged in `docs/TEST_EVIDENCE.md`. |

**Finding D-2 (verification-gap / documentation gap):** `CLAUDE.md`'s "Verification Commands" section instructs `npm run test -- --watch=false` with no mention that Node must be pinned to 24 (`nvm use 24`) first, even though this is a known, previously-hit failure mode recorded in `docs/TEST_EVIDENCE.md`. An agent following `CLAUDE.md` literally on a machine whose default Node is outside `>=24 <25` will see ~254 false failures and could misreport a regression. **Correction applied**: added an explicit Node-24 requirement to `CLAUDE.md`'s verification commands (see §7 below).

**Finding V-1 (environment/tooling, not documentation):** native Gradle on this session's `/mnt/d` WSL mount completed only 3 of 286 test suites in ~40 minutes before being stopped; mirroring `be/src` to `/tmp/opencode/be-build` (native ext4) per `docs/BUILD_TOOLING.md`'s own documented workaround let the identical suite finish in 5m37s. This isn't a documentation defect (the workaround is already written down) — it's confirmation that the documented workaround is necessary, not optional, in this class of environment. No file needed correction for this; recorded here as verification methodology.

**Finding D-3 (documentation gap, corrected):** `be/tools/check-test-count.py`'s enforced floor (`MIN_TESTS=1469`, `MIN_SUITES=280`) had been raised at least twice since `docs/TEST_EVIDENCE.md`'s last evidence-log entry (1174/234, dated around 2026-08-29) and its "Baselines (enforced by CI)" table (535/146, dated 2026-08-13) — most recently in commit `aa9d26b` (2026-09-01), which touched only the check scripts, not the doc. This means the doc's own stated rule ("Only raise a baseline after a verified green run" — never silently) was not followed for the doc itself, even though the underlying script changes were presumably backed by real runs at the time. **Correction applied**: updated the baseline table and added a current dated evidence-log entry (see §7).

---

## 2. Payroll State Machine (PAY-001) — the task's flagship claim

**Claim (`README.md`):** *"`PAY-001` enforces `DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID`, row locks, expected versions, role-scoped transitions, and frozen snapshots; its PostgreSQL concurrent-payment proof remains the only open P0 verification gate."*

**Evidence (read directly from source, not from docs):**
- `be/src/main/java/com/bemo/hr/payroll/domain/SalaryPayment.java`:
  - `PaymentStatus paymentStatus` field, `@Version` field present (line ~104).
  - `transitionTo(PaymentStatus nextStatus)` (line 143) uses an exhaustive `switch` on current status to compute the *only* legal next status (`DRAFT/PENDING→CALCULATED→REVIEWED→APPROVED→POSTED`) and throws `PAYROLL_STATE_TRANSITION_INVALID` (409) on any other target — this is a real, code-enforced state machine, not just an enum.
  - `markAsPaid()`-equivalent guards `paymentStatus != POSTED` → `PAYROLL_PAYMENT_STATE_INVALID` (409) before allowing `PAID`.
  - `reverse()` guards `paymentStatus != PAID` → `PAYROLL_REVERSAL_STATE_INVALID` before allowing `REVERSED`.
- `be/src/main/java/com/bemo/hr/payroll/infrastructure/SalaryPaymentRepository.java`: `findByIdForUpdate` annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)` — real row locking, not just optimistic versioning.
- `be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java`: `expectedVersion` is checked against `entity.getVersion()`/`payment.getVersion()` before mutating (lines ~425, ~602) — confirms "expected versions" claim; `payrollRunHeaderRepository.findByIdForUpdate` and `salaryPaymentRepository.findByIdForUpdate` are used inside the `/pay`, `/transition`, `/reverse`, `/pay-bulk` service methods (all `@Transactional`).
- `be/src/main/java/com/bemo/hr/payroll/api/PayrollController.java`: every mutating endpoint carries a distinct `@PreAuthorize` — `/pay` and `/pay-bulk` require `SUPER_ADMIN`/`ADMIN`/`PAYROLL_MANAGER` or an explicit `payroll:run:disburse` permission; `/transition` gates `CALCULATED`/`REVIEWED` targets differently from later ones; `/reverse` requires the same manager roles — this is genuine role-scoped transition control, not a single blanket check.
- "Frozen snapshots": `SalaryPayment.payrollSnapshotId` + `attachCalculationEvidence(...)` (line 159) explicitly rejects re-attaching a *different* snapshot id once one is set (`if (this.payrollSnapshotId != null && !equals(...)) throw`) — the calculation evidence is write-once per payment.
- PostgreSQL concurrency proof: `be/src/test/java/com/bemo/hr/payroll/PayrollPaymentConcurrencyTests.java` exists and extends `PostgresIntegrationTest` (Testcontainers-gated). This environment has no Docker daemon (confirmed: `docker` command not found in this WSL distro), so — exactly as README says — **this specific proof could not be executed here either**. The claim "remains the only open P0 verification gate" is accurate as stated; it is not resolved by this pass, and should not be reported as resolved.

**Status: `IMPLEMENTED` (state machine, locking, versioning, role-scoping, frozen-snapshot guard) + `BLOCKED` (PostgreSQL concurrency execution, environment has no Docker — matches the documented gap exactly, nothing new to fix here).**

This is the one major claim in the active docs that is precisely and accurately worded already — a good model for how the other claims below should read.

---

## 3. Security / CSP / Node toolchain — directly verified

| Claim | Evidence | Status |
|---|---|---|
| "Nginx frontend boundary applies a restrictive CSP without `unsafe-eval`" (`README.md`) | `fe/nginx.conf:16,51`: `script-src 'self'` — no `unsafe-eval`, no `unsafe-inline` for scripts (style-src has `unsafe-inline` for CSS only, which is unrelated to script injection). | `IMPLEMENTED` — CONFIRMED |
| "frontend builds are standardized on Node 24" (`CLAUDE.md`) | `fe/package.json` `engines.node` = `">=24.0.0 <25"`; `fe/.nvmrc` = `24`. Actual dev-environment default Node is v26.5.1 (outside the declared range). `npm run build`/`check:i18n`/`check:hardcoded` all still succeed under Node 26 (no `localStorage` dependency in those tool scripts); only the Vitest suite requires exactly Node 24. | `IMPLEMENTED` (declared correctly) with an environment caveat (see D-2) — not a documentation defect, but the CLAUDE.md verification section needed the nvm reminder, now added. |
| Liquibase head version | `be/src/main/resources/db/changelog/.../20260905_v460_owner_executive_cockpit_translations.yaml` is the newest file (255 total changelog yaml files). Matches `PROJECT_MAP.md`'s "Liquibase v459/v460" claim for the Owner Executive Cockpit (Session 18) exactly. | `IMPLEMENTED` — CONFIRMED |

---

## 4. Documentation ↔ Documentation contradictions found in `PROJECT_MAP.md`

These are internal to the *same* file and do not require source-code verification to identify — they are self-contradictions.

### Finding C-1 — TASK 05 (Owner Executive Cockpit) status conflict — **RESOLVED in this pass**
- `PROJECT_MAP.md` **`[COMPLETED & VERIFIED]`** section (top of file, dated Session 18 / 2026-09-05) describes the Owner/Executive Cockpit as a *complete implementation* with full detail (Liquibase v459/v460, 710 tests, 6,050 i18n keys, Excel export, target-setting engine, etc.).
- The same file's **`[ORPHANS & PENDING]`** section lists, twice: `TASK 05 — Owner Executive Cockpit [P1, IN PROGRESS]` and, a few lines later under "Completed in Prior Sessions" (an apparent copy-paste artifact), `TASK 05 — Owner Executive Cockpit [P1, PENDING]`.
- The **`[COMMERCIAL READINESS ROADMAP EXECUTION]`** section (bottom of file) *again* lists `[/] TASK 05 — Owner / Executive Mobile KPI Cockpit (P1): [IN PROGRESS]`.
- **Independent verification:** the Liquibase files `20260905_v459_owner_executive_cockpit_schema.yaml` and `20260905_v460_owner_executive_cockpit_translations.yaml` exist on disk, dated the same day as the "COMPLETED & VERIFIED" entry. This corroborates the *completed* claim, not the *pending* one.
- **Resolution:** the three "pending/in-progress" mentions of TASK 05 are stale leftovers that were never removed after the feature shipped. **Corrected in `PROJECT_MAP.md` as part of this pass** (see §7).

### Finding C-2 — Two incompatible TASK 06–12 numbering schemes — **left unresolved, flagged for a human decision**
- The `[ORPHANS & PENDING]` section (citing `Next_ERP_Commercial_Readiness_Task_Pack`) numbers the remaining backlog as: 06 Field Sales Rep Workspace, 07 Customer Portal, 08 WhatsApp/Document Delivery, 09 Approval Workflow Engine, 10 Backup/DR Console, 11 Barcode/Label Studio, 12 Customer Credit & Collections.
- The `[COMMERCIAL READINESS ROADMAP EXECUTION]` section at the bottom of the *same file* numbers the same range completely differently: 06 Field Sales Rep Route Planning, 07 Customer Self-Service Portal, 08 WhatsApp Document Delivery, **09 Van Sales Inventory Loading & Day-End Reconciliation**, **10 Multi-Unit Pricing/Tiered Discounts**, **11 Egyptian E-Invoicing Phase 2**, **12 Contractor Advances & Daily Settlement Mobile**.
- Tasks 06–08 roughly agree in substance (naming drift only); tasks 09–12 name **entirely different features** under the same numbers in the two lists.
- **Status: `UNCLEAR`.** This looks like two different source roadmaps (`Next_ERP_Commercial_Readiness_Task_Pack` vs whatever fed the bottom section) that were both pasted into the same file without reconciling numbering. I did not guess which is authoritative — that requires a decision from whoever owns the roadmap. **Not corrected**; flagged here so it isn't silently lost. Recommend the project owner pick one numbering and delete the other list, or clearly separate them under different headings.

---

## 5. Root-doc staleness — `docs/TECHNICAL_GUIDE_CHECKLIST.md`

`CLAUDE.md` and `README.md` both cite `docs/TECHNICAL_GUIDE_CHECKLIST.md` as a current "evidence source" / "current architecture and operational evidence." Its own header states it is a snapshot reviewed **2026-08-01 through 2026-08-04** ("Verification run 1" through "Verification run 4"), and its per-page verification table (§4.1–4.34) was last touched at that time.

Cross-checking a sample of its `PARTIAL`/`NOT_STARTED` verdicts against later, better-evidenced work recorded in `PROJECT_MAP.md` / `docs/TEST_EVIDENCE.md` (both dated well past August 4) shows several are now **superseded**:

| Checklist §, verdict (2026-08-04) | Later evidence it conflicts with |
|---|---|
| §4.16 Journal Entries: *"no closed-period guard at posting (fiscal period not enforced in service)"* | `PROJECT_MAP.md` "[SYSTEM_FLOW Checklist]": *"[x] Fiscal period open/closed guard enforced on journal posting and procurement invoice/payment dates"* and *"[x] Journal entries expose explicit state with guarded post/reverse transitions"* — a later, dedicated agent pass (§below) independently confirmed `FiscalPeriodGuard` is actually invoked from journal posting in current source. |
| §4.11 Sales: *"simple SO list/form... no quotation→SO→delivery→invoice→collection pipeline"* | `docs/TEST_EVIDENCE.md`'s 2026-08-13 "O2C vertical-slice checkpoint" describes `SalesOrderToCashPersistenceTests` proving persisted lines, pricing snapshots, ATP reservation, delivery+COGS journal, invoice, receipts, returns, credit notes — a full O2C pipeline that did not exist on 08-04. |
| §4.15 Chart of Accounts: *"flat list; no tree/hierarchy"* | Not independently re-confirmed in this pass beyond the dedicated agent's finding below — flagged as likely-superseded, pending that finding. |
| §4.32 Contractor Accounts: *"NONE — no contractor-account/statement endpoint exists"* | `PROJECT_MAP.md` describes a full "Contractor Workforce Financial Lifecycle" including settlement→ledger→payment; needs the dedicated agent's confirmation below. |

**Recommendation (not yet applied — needs the human-facing summary below first):** either (a) add a header banner to `docs/TECHNICAL_GUIDE_CHECKLIST.md` explicitly marking it a point-in-time snapshot from early August, the way `docs/BORTQALA_REMAINING_WORK_CHECKLIST.md` is already marked superseded, or (b) qualify the `CLAUDE.md`/`README.md` references to it so an agent doesn't treat its per-page verdicts as current. This document does **not** delete or rewrite the checklist's historical content — see §8 for what was actually changed.

---

## 6. Findings from sub-agent verification — all returned

The following areas were dispatched to focused research passes (read-only source inspection against current `be/`/`fe/` code, not against docs) because they are large enough that verifying them personally would have duplicated work unnecessarily. All five returned; findings are folded into the numbered sections below:

- Finance & Sales O2C (fiscal period guard usage, journal state machine, Chart of Accounts, AR aging as-of-date, Sales O2C pipeline) — §9.
- Security/tenancy sampling across 13 controllers + tenant scoping at the entity/query level — §10.
- Broader ERP workflow modules: procurement, inventory/operations, manufacturing/quality, contractor workforce, notifications/support — §11.
- Database/Liquibase integrity: changelog wiring, H2/PostgreSQL parity, `check-test-count.py` baseline drift, tenant-scoping at the schema level, V84 optimistic-locking claim — §12.
- Frontend route/feature inventory vs. documentation (reverse check: implementation → documentation) — §13.

---

## 7. Corrections applied

| File | Change | Why |
|---|---|---|
| `CLAUDE.md` | Added a note under "Verification Commands" that frontend tests require Node 24 exactly (`nvm use 24`), with the failure mode explained | Finding D-2 — prevents a future agent from misreporting ~254 false test failures as a regression |
| `PROJECT_MAP.md` | Corrected 3 stale "TASK 05 IN PROGRESS/PENDING" mentions to reflect the "COMPLETED & VERIFIED" status already recorded (independently confirmed via the v459/v460 Liquibase files on disk) | Finding C-1 |
| `PROJECT_MAP.md` | Qualified the P0-3 "every domain controller carries `@PreAuthorize`" claim with the confirmed `serviceops` exception | §10.1 |
| `PROJECT_MAP.md` | Added a new "[FEATURES FOUND BUT UNDOCUMENTED]" section listing ~20 routed frontend features absent from all active docs (clinic vertical, CRM, verticals, service-ops, etc.) | §13.2 — mandatory reverse (implementation→documentation) check |
| `README.md` | Removed the now-false "AR aging/collections require an explicit as-of date" claim and replaced it with an accurate "known gap" note describing what the code actually does | §9.5 — the parameter is accepted but ignored in bucket computation |
| `docs/TEST_EVIDENCE.md` | Updated the "Baselines (enforced by CI)" table from the stale 2026-08-13 figures (535/146 backend, 284/50 frontend) to what the check scripts actually enforce today (1,469/280 backend; 710/144 frontend), and added a dated 2026-09-06 evidence-log entry recording this pass's real command results | Finding D-3 |
| `docs/TECHNICAL_GUIDE_CHECKLIST.md` | Added a header banner marking the file a 2026-08-01…08-04 point-in-time snapshot, naming the specific sections now known to be stale, and pointing to current sources | §5 |

Nothing else was modified. Historical documents (`docs/history/`, `docs/status/`, `AGENTS.md`, `docs/BORTQALA_REMAINING_WORK_CHECKLIST.md`) were left untouched — see §8 immediately below. Two genuine implementation defects found during verification (AR-aging as-of-date, `serviceops` missing authorization) and two schema defects (orphaned V129/V244 migrations, missing `sales_orders` index) were **documented, not fixed** — per the task's explicit instruction not to modify application/database behavior in this pass.

---

## 8. Historical documentation — left untouched, on purpose

Per the task's instructions, historical documents were **not** rewritten to look as if they were originally correct: `AGENTS.md` (already marked historical-only from the prior context-cleanup session), `docs/history/*`, `docs/status/*`, `docs/BORTQALA_REMAINING_WORK_CHECKLIST.md` (already marked superseded), and `bemo/`/`bemo_erp_uiux_qa_pack/`/task-pack directories were left exactly as they were. Where a historical document is already correctly labeled non-current, no action was needed. `docs/TECHNICAL_GUIDE_CHECKLIST.md` was not historical-labeled before this pass despite being one (see §5/§7) — that is the one exception, corrected because active docs (`CLAUDE.md`, `README.md`) cite it as current.

---

## 9. Detailed Findings — Finance & Sales O2C (sub-agent verification)

**Scope:** fiscal-period guard enforcement, journal entry state machine, Chart of Accounts hierarchy, AR aging as-of date, Sales O2C pipeline. Verified by reading current `be/src` and `fe/src` source directly (not docs).

### 9.1 Sales O2C pipeline — `IMPLEMENTED`, tested
`README.md`'s "O2C-001 verified complete" is accurate. `be/src/main/java/com/bemo/hr/trade/sales/application/` has a real layered architecture (`SalesOrderFullService`, `SalesQuotationService`, `SalesReceivablesService`, `SalesPricingSnapshotService`, `CustomerCreditService`, `CustomerInvoiceService`, `CustomerReceiptBankMatchService`) covering quotation → order (confirm with a pricing snapshot) → delivery (with ATP reservation + COGS journal + auto-issued invoice) → receipt (partial/full allocation) → return (with credit note + COGS/stock reversal). `be/src/test/java/com/bemo/hr/operations/SalesOrderToCashPersistenceTests.java` exercises this entire chain end-to-end, including a dedicated concurrency test (`concurrentReservationsCannotOversubscribeAvailableStock`) proving no-oversubscription under 2 racing threads. **`docs/TECHNICAL_GUIDE_CHECKLIST.md` §4.11's "simple SO list/form... no pipeline, no service layer" verdict is now stale/false** — it predates this work by roughly two weeks.

### 9.2 Journal entries + FiscalPeriodGuard — `IMPLEMENTED`
`JournalEntry.Status` enum is `DRAFT/APPROVED/REJECTED/POSTED/REVERSED`. `JournalEntryService.postTransaction` calls `fiscalPeriodGuard.requireOpen(entry.getEntryDate())` before posting (idempotent, optimistic-version-checked, SoD-checked); `reverse()` requires `POSTED` status. `FiscalPeriodGuard`/`DefaultFiscalPeriodGuard` throw `FISCAL_PERIOD_CLOSED`/`FISCAL_PERIOD_SOFT_CLOSED`. `ProcurementService` calls the same guard on supplier-invoice and payment posting dates (confirming PROJECT_MAP's P0-5/P0-6 claims exactly), and `SubledgerPostingService` (used by the sales O2C flow above) also injects it, so sales postings are transitively guarded too. **`docs/TECHNICAL_GUIDE_CHECKLIST.md` §4.16's "no closed-period guard at posting" verdict is stale/false.**
- **Test-coverage nuance:** `JournalEntryServiceTests`/`ProcurementServiceTests` mock the guard away and only assert it's *not* called in an SoD-failure short-circuit path — there is no dedicated `DefaultFiscalPeriodGuardTests`. The guard's actual closed/soft-closed rejection behavior *is* positively tested, just through other consumers (`AssetDepreciationServiceTests`, `BankReconciliationServiceTests`, `InventoryValuationServiceTests`). Net: implemented-and-tested, but the test coverage is indirect for the journal/procurement call sites specifically.

### 9.3 Chart of Accounts hierarchy — `PARTIALLY_IMPLEMENTED`
`Account.java` has real `parentId`/`isHeader` fields (present since the original `v26`/`v27` migrations, not a recent addition), contradicting the checklist's "flat, no hierarchy" claim at the **data-model** level. But `AccountingController.listAccounts()` returns a flat list (no server-side tree assembly), and `fe/src/app/features/finance/accounts/accounts.page.ts` renders a flat table with a parent-select dropdown — no tree/indent rendering. So: the hierarchy exists as data but is not surfaced as a hierarchy anywhere in the UI or API response shape. No hierarchy-specific test exists (backend or frontend).

### 9.4 Fiscal periods / "Finance Reports & Close workbench" — `IMPLEMENTED`
`FiscalPeriod.Status` is a 4-state lifecycle (`OPEN/SOFT_CLOSED/CLOSED/LOCKED`) with `updateStatus` guards. A dedicated `application/close/` package exists (`CloseBlockerQueryService`, `PeriodCloseOrchestratorService`, `PeriodCloseWorkbenchService`, `SubledgerReconciliationProvider`, `TreasuryCloseProvider`, `FinancialControlAccountResolver`) — genuine close-orchestration logic, not just a status flag. `fe/src/app/features/fiscal-periods/fiscal-periods.page.ts` (route `/fiscal-periods`) does combine period status with `BalanceSheetReport`/`IncomeStatementReport`/`ReconciliationReport`/`ClosePrecheck` signals and role-gated close/reconciliation actions — functionally matching README's "consolidated Finance Reports & Close workbench" claim, even though the sidebar label text itself (`nav.fiscalPeriods`) wasn't renamed. **`docs/TECHNICAL_GUIDE_CHECKLIST.md` §4.20's "no soft-close/close/reopen/blockers" verdict is stale/false.**

### 9.5 AR aging as-of date — **`NOT_IMPLEMENTED` as claimed — a real code defect, not just a doc error**
`README.md` states AR aging/collections "require an explicit as-of date." In actual code, `PartyFinancialPositionController.getAgingReport` takes `asOfDate` as an **optional** parameter, and — more importantly — `PartyFinancialPositionService.getFinancialPosition()` (which computes the actual aging buckets) **always uses `System.currentTimeMillis()`**, completely ignoring whatever `asOfDate` was passed in. The parameter only appears in the response envelope's label field; it has zero effect on which aging bucket a receivable falls into. There is no test for this (`PartyFinancialPositionServiceTests` never exercises `getAgingReport`/`asOfDate`), so nothing would have caught it. **This is a genuine implementation gap, documented here as a finding per the task's instructions — not fixed, since fixing application behavior is out of scope for this documentation-reconciliation pass.**

---

## 10. Detailed Findings — Security / Tenancy / Authorization sampling (sub-agent verification)

**Scope:** `@PreAuthorize` coverage sampled across 13 controllers spanning finance, procurement, sales, payroll, inventory/operations, workforce, manufacturing, HR, and auth; tenant scoping at the entity/native-query level; frontend route guards; ESS object-ownership. Verified by reading current `be/src`/`fe/src` source directly.

### 10.1 `@PreAuthorize` coverage — `PARTIALLY_IMPLEMENTED` (one real gap found)
Ten sampled controllers (`AccountingController`, `TreasuryController`/`PaymentBatchController`/`PeriodCloseController`/`FiscalPeriodController`, `ProcurementController`/`PurchaseRequisitionController`, `SalesController`/`SalesOrderFullController`, `PayrollController`/`PayrollExecutionController`, `InventoryController`/`InventoryMovementController`/`WarehouseInventoryController`, `WorkforceRequestApprovalController`, `ManufacturingController`, `EmployeeController`, `AuthController`) all carry explicit `@PreAuthorize` on every mutating endpoint, matching the specific role lists `PROJECT_MAP.md`'s P0-3 entry names — **CONFIRMED accurate for this sample.**

**GAP FOUND (re-verified, high confidence):** `be/src/main/java/com/bemo/hr/serviceops/api/BookingController.java`, `WorkOrderController.java`, and `RentalController.java` carry **zero** `@PreAuthorize`/`@Secured`/`@RolesAllowed` — no class-level, no method-level, no base class. Their mutating endpoints (create/cancel booking, activate/close/cancel rental contracts, create/update work orders, and work-order **delivery — which generates a customer invoice**) are reachable by *any authenticated user of any role* in the tenant, since the global `SecurityConfig` rule for `/api/**` only requires "authenticated and not password-restricted," not a specific role/permission. Tenant isolation is intact (services call `TenantContext.require()`, entities carry `@TenantId`), so this is a **role/permission gap, not a cross-tenant data leak** — but it directly contradicts `PROJECT_MAP.md`'s P0-3 claim that "every domain controller now carries explicit `@PreAuthorize` sets." **`check-authorization-contract.py` cannot detect this class of gap** — it only validates role-string spelling inside `@PreAuthorize` annotations that already exist; a controller with none passes trivially. **Correction applied**: qualified the P0-3 claim in `PROJECT_MAP.md` with this exception (see §7).

Two endpoints that looked suspicious on an initial static scan were investigated and are **not** gaps: `CrmController.handleWebhook`/`WhatsAppWebhookController` sit under `/api/**` and still require a valid bearer JWT (a possible integration friction point for real external webhook callers, not an authorization weakening); `PublicPaymentController` (`/p/{token}`) is intentionally public by design, mapped outside `/api/**`.

### 10.2 Tenant isolation — `IMPLEMENTED`, confirmed
`@TenantId` (the real Hibernate multi-tenancy annotation, not a custom stand-in) is used across 407 files, confirmed present on `SalesOrder`, `PurchaseOrder`, `JournalEntry`, `Employee`, `ItemLotSerial`, `StockTransferHeader`, `CustomerInvoice`, `SupplierInvoice`. `TenantContext` is a `ThreadLocal<String>` bound from the JWT's `appId` claim in `RequestAuditFilter` — never from a client-supplied header. Of the only 5 files using native SQL anywhere in the backend, every mutating native query includes an explicit `app_id = :appId` predicate — no tenant-scoping bypass found in the sample.

### 10.3 Frontend route guards — `IMPLEMENTED`, confirmed
83 of 92 top-level lazy-loaded routes carry `canActivate` guards; the 9 without are legitimately public (`login`, `server-setup`, public payment/catalog pages, `support`, `about`, `forbidden`, `not-found`). Sensitive routes (`payroll`, `finance/journal-entries`, `users`) all carry `[roleGuard, menuAccessGuard]` with role arrays matching their backend `@PreAuthorize` sets.

### 10.4 ESS object ownership — `IMPLEMENTED`, confirmed
`EssService.getPayslipDetail` resolves the caller's own employee record from the authenticated principal and explicitly rejects (404, not the data) any attempt to fetch a `paymentId` belonging to a different employee — a genuine ownership check, not merely tenant scoping.

### Overall verdict for §10
The sampled evidence largely **supports** PROJECT_MAP's P0-3 claim, with **one confirmed, real exception** (`serviceops` bookings/rentals/work-orders — see §10.1) that the codebase's own passing verification script cannot catch. This is recorded as a security finding, not silently fixed (fixing it would be an application-code change, out of scope for this pass).

---

## 11. Detailed Findings — Broader ERP Workflow Modules (sub-agent verification)

**Scope:** Procurement, Inventory/Operations, Manufacturing/Quality, Attendance/HR core (spot check), Contractor Workforce, Notifications/Support. Verified against current `be/src`/`fe/src`, cross-checked against `docs/TECHNICAL_GUIDE_CHECKLIST.md`'s early-August verdicts.

### 11.1 Procurement — `PARTIALLY_IMPLEMENTED`
The module grew substantially since the checklist (11 application services now: `ProcurementService`, `PurchaseRequisitionService`, `SourcingService`, `GrirReconciliationService`, `VendorPaymentProposalService`, `SupplierPaymentPlanService`, `SupplierPerformanceService`, `ProcurementMatchOverrideService`, `OcrCaptureService`, `ProcurementBudgetAndTreasuryService`, plus three-way-match). `@Version` optimistic locking is confirmed present on `PurchaseOrder`, `GoodsReceipt`, `SupplierInvoice`, `SupplierPayment`, and several newer entities — **PROJECT_MAP's P0-7 claim is accurate**. `SupplierPaymentConcurrencyTests` (`@PostgresIntegrationTest`) exists, so the checklist's "no concurrent GRN/payment race test" is now stale. **However, the checklist's "no GRN/invoice reverse endpoints" is still accurate today** — `ProcurementController` still has no literal reverse endpoint for a GRN or a posted invoice (correction only happens via a separate `SupplierReturn` flow). Mixed verdict: mostly superseded, one specific gap still real.

### 11.2 Inventory / Operations — `IMPLEMENTED`, tested
FIFO/weighted-average valuation (`InventoryValuationService`, cost layers, immutable movement cost evidence), atomic warehouse transfers with dispatch/receive/inspection/cancel, and idempotent cycle counts with discrepancy resolution all exist (`InventoryMovementFullService`), backed by `InventoryValuationServiceTests` (pessimistic item-lock, FIFO consumption, closed-period rejection) and `InventoryMovementFullServiceTests`. The checklist's "no concurrency/atomic-transfer/reversal tests" is now stale. **One nuance for accuracy**: idempotency is per-flow (e.g., `reconcile(operationId, ...)`), not a single universal `operation_id` column on the base `stock_movements` table — worth keeping in mind if any doc implies uniform idempotency across all movement types.

### 11.3 Manufacturing/Production & Quality — `PARTIALLY_IMPLEMENTED`
Backend has genuinely closed the checklist's BOM/material gaps: `BomLine` (component lines with quantity-per/waste%), `MaterialIssueService` (issue/return), `ManufacturingExecutionService` (work centers, routings, output receipt), `ManufacturingWipService`, `ManufacturingVarianceCloseService` all exist. **But the frontend has not caught up**: `production.page.ts` has no component-line editor or material-issue wiring — the checklist's frontend-facing gap is still real. For Quality, the core `QualityInspection` entity is still just `PASSED/FAILED/REJECTED` with list+create (no submit/approve/reverse state machine) — **the checklist's "list+create only, no workflow" verdict is still accurate for the core inspection entity**, even though a newer, separate `QualityPlanService`/`QualityStockDispositionService` ecosystem (quarantine/release dispositions) now exists alongside it.

### 11.4 Attendance / HR core — `IMPLEMENTED`, no regression
Spot-checked only (this area was already `VERIFIED` in the checklist itself as of early August). Modules and tests referenced in the checklist (`HrConfigurationServiceScheduleTests`, `BiometricImportContractTests`, `ReportingGenerationContractTests`, `ReportingDayAnomalyTests`) still correspond to real, substantial code. No regression signal found.

### 11.5 Contractor Workforce Financial Lifecycle — `IMPLEMENTED`, tested
PROJECT_MAP's claimed chain (Labor Request → Assignment → Attendance Lock → Settlement → Advances/Deductions → Approval → Finance Posting → Invoice Link → Payment) maps directly onto real method names in `WorkforceSettlementService` (`createPeriod → calculatePeriod → reviewPeriod → approvePeriod → lockPeriod → postSettlement → linkInvoice → recordPayment`), backed by `WorkforceSettlementPeriodTests`, `WorkforceSettlementTransitionTests`, `WorkforceFinancialIntegrationServiceTests`. **Nuance**: unlike payroll, there is no settlement-posting/payment-specific `@PostgresIntegrationTest` concurrency test analogous to `PayrollPaymentConcurrencyTests` — the only Testcontainers-tagged test in this area covers import-commit batching, not settlement/payment concurrency. If a future doc claims "settlement payment concurrency is proven," that would need the same caveat payroll's does (implemented, PostgreSQL proof outstanding) — currently no doc makes that specific claim, so this is a forward-looking note rather than a correction.

### 11.6 Notifications / Support — `IMPLEMENTED`, matches docs
Both the Action Center (`BusinessNotification`, `NotificationService`, web-push integration) and the Epic-11 support/health system (`SupportTicket`, `SupportService`, `CustomerHealthSnapshot`) exist and are tested (`NotificationServiceTests`, `SupportServiceTests`, `SupportHealthWindowTests`). **Worth flagging (not a doc error, an architecture note):** the codebase has **two separate, coexisting ticket systems** — the newer `com.bemo.hr.product.support` module and an older, distinct `com.bemo.hr.helpdesk` module (`Ticket`/`TicketMessage`). Neither doc calls out that there are two, which risks a future agent conflating them.

---

## 12. Detailed Findings — Database / Liquibase integrity (sub-agent verification)

**Scope:** changelog wiring mechanism, H2/PostgreSQL master sync, `check-test-count.py` baseline drift, tenant scoping at the schema level, the V84 optimistic-locking claim, duplicate changeset ids in a recent version range. Verified by reading `be/src/main/resources/db/changelog/` directly, plus `git log`/`git show` for process history.

### 12.1 Changelog wiring — `IMPLEMENTED` with **two orphaned migrations found**
`db.changelog-master.yaml` uses fully explicit `include` lists (no `includeAll`/directory scanning) across two files (`releases/20260729_v1_v67.changelog-master.yaml`, `releases/next.changelog-master.yaml`). v460 is confirmed genuinely wired in, and the H2 test master (`test-h2.changelog-master.yaml`) is confirmed in sync through v459/v460 today — `CLAUDE.md`'s claim that this file is a current evidence source holds.

**Real defect found:** two migration files exist on disk but are referenced by **no** changelog anywhere, production or H2 — they have never run against any database:
- `data/update/20260808_v129_bilingual_arabic_cleanup.yaml` (90+ Arabic UI-string cleanup updates)
- `data/update/20260814_v244_customer_success_support_ux.yaml` (support/customer-health UI copy updates)

Both are data-only translation fixes (no DDL, low blast-radius), but whatever they were meant to correct was never actually applied anywhere. **Recorded as an implementation gap (§ Implementation Gaps below) — not fixed**, since wiring them in is a schema-affecting change out of scope for a documentation pass, and doing so without knowing why they were orphaned could introduce a regression.

### 12.2 `check-test-count.py` baseline vs. `docs/TEST_EVIDENCE.md` — confirmed and **corrected** (Finding D-3, §7)
`git log -- be/tools/check-test-count.py` shows the floor was raised to its current 1,469/280 in commit `aa9d26b` (2026-09-01), which touched only the two check-test-count scripts — `docs/TEST_EVIDENCE.md` was not updated in that commit and has no later commit touching it either. Already corrected in §7.

### 12.3 Tenant scoping at the schema level — `IMPLEMENTED`, with one scale note
`app_id NOT NULL` confirmed on `purchase_orders`, `sales_orders`, `journal_entries`, `business_parties`, `inventory_items`, `salary_payment`. 5 of those 6 have a supporting composite index/unique constraint starting with `app_id`. **`sales_orders` has correct tenant scoping but no supporting index anywhere in the 255-file changelog** — every tenant-scoped query against one of the highest-traffic tables in the system relies on a full scan or an incidental FK/PK index. This is a scale/performance concern, not a correctness bug; recorded as an implementation gap, not fixed (adding an index is a schema change, out of scope here).

### 12.4 V84 optimistic-locking claim — `IMPLEMENTED`, exact match
`schema/update/20260802_v84_optimistic_locking_aggregates.yaml` adds a `version` column to exactly the 14 tables PROJECT_MAP.md's P0-7 entry names — a 1:1 match, no discrepancy.

### 12.5 Duplicate changeset ids (V440–V460 range) — confirmed clean
Every changeset id across the 16 files in this range is unique. No defect found.

---

## 13. Detailed Findings — Frontend route/feature inventory vs. documentation (reverse check)

**Scope:** enumerate every current frontend route and compare against what PROJECT_MAP.md/README.md/docs/TECHNICAL_GUIDE_CHECKLIST.md actually describe. This is the mandatory "implementation → documentation" direction of the reconciliation.

### 13.1 The "31 routes" claim is stale and significantly wrong
`docs/TECHNICAL_GUIDE_CHECKLIST.md` §3 claims "All 31 routes render... every route is lazy-loaded." Its own table (lines 172–211) actually lists 34 rows, not 31. Current `fe/src/app/app.routes.ts` (745 lines) plus its wired child route files (`features/workforce/workforce.routes.ts`, `features/projects/projects.routes.ts`) contain **~107 lazy-loaded leaf routes** — roughly 3.4× the documented count. **Status: `DOCUMENTATION_ONLY` (i.e., the number was never re-verified as the app grew) — corrected framing recommended, not a line-by-line rewrite (see §14).**

### 13.2 Undocumented implemented functionality (the highest-value reverse-check finding)
The following routes/features exist in current source with real pages/components and are **not mentioned anywhere** in `PROJECT_MAP.md`, `README.md`, or `docs/TECHNICAL_GUIDE_CHECKLIST.md` (verified with targeted, context-checked searches, not raw keyword counts that could false-positive):

- **An entire clinic/medical vertical**: `clinic/patients`, `.../patients/:id/chart`, `clinic/queue`, `clinic/commissions`, `clinic/appointments`, `clinic/pharmacy`, `clinic/lab`, `clinic/insurance`, `clinic/hospital`, `clinic/dental`, `clinic/tools` — 10 distinct pages under `fe/src/app/features/clinic/`.
- `crm` (CrmPage) — a full CRM page.
- `verticals/specialized` (VerticalsPage) — distinct from the "vertical" feature-flag concept that *is* documented elsewhere; this specific page/route is not.
- `service-ops`, `smart-import` (+`:workflow`), `server-setup`, `selfie-punch`, `migration` (DataMigrationComponent), `imports/device-integrations`, `automation`, `growth`, `report-builder`, `documents`, `notifications/send`, `admin/setup-readiness`, `admin/product-insights`, `platform-admin/outbox`, `access/policy-groups`.
- `finance/payment-links` — the *backend* webhook-signature work (WP-29) is documented; the actual frontend page/route is not named anywhere.
- `kb`, `helpdesk`, `marketing` — only ever mentioned incidentally (e.g. as i18n-key examples or SCSS-warning file names), never described as features.
- The public storefront pages (`PublicPayPage`, `PublicCatalogPage`, `PublicProductDetailPage`) — the backend capability (P1-01/WP-29) is documented; the frontend page names are not.

### 13.3 Two orphaned/dead route-config files (code hygiene, not a doc error)
`fe/src/app/features/clinic/clinic.routes.ts` and `fe/src/app/features/approvals/approvals.routes.ts` exist but are never imported anywhere — the actual clinic and approvals routes are wired as flat paths directly in `app.routes.ts` instead. These are leftover scaffolding, evidence the routing structure was later reorganized without deleting the old child-route files. Not fixed here (application-code change, out of scope), but recorded so it isn't mistaken for a second, reachable route tree.

### 13.4 Checklist rows now stale relative to what actually surrounds them
`/trade/sales`, `/finance/accounts`, `/payroll`, and the `/workforce/*` rows in the checklist's route table predate substantial sibling growth (POS, field-sales, export-shipments, CRM, marketing under sales; budgets/fixed-assets/payment-links/reconciliation/eta-tax under finance; `dispatch-disputes` and `client-billing` under workforce, missing from the table's 12-row workforce listing). Consistent with §5's staleness finding — not re-litigated here.

---

## 14. Implementation Gaps (actual code/schema defects found — not fixed, per task scope)

1. **AR aging as-of-date is accepted but ignored** — `PartyFinancialPositionService.getFinancialPosition()` always buckets against `System.currentTimeMillis()`; the `asOfDate` request parameter has no effect on which aging bucket a receivable lands in. No test covers this path. (§9.5)
2. **`serviceops` bookings/rentals/work-orders have no authorization annotations at all** — any authenticated user of any role can create/cancel bookings, activate/close/cancel rental contracts, and create/deliver work orders (the latter generates a customer invoice). Tenant isolation is intact; this is a role/permission gap. (§10.1)
3. **Two Liquibase migrations are orphaned** — `v129_bilingual_arabic_cleanup.yaml` and `v244_customer_success_support_ux.yaml` are referenced by no changelog anywhere and have never run against any database. (§12.1)
4. **`sales_orders` has no supporting index for its tenant-scoping column** — `app_id` is `NOT NULL` but no composite index/unique constraint starts with it, unlike 5 comparable tables. Scale concern on a high-traffic table, not a correctness bug. (§12.3)
5. **Procurement still has no GRN/supplier-invoice reverse endpoint** — correction is only possible via a separate `SupplierReturn` flow; this specific part of the original 2026-08-04 checklist finding is still accurate today. (§11.1)
6. **Quality Inspection's core entity still has no submit/approve/reverse workflow** — list+create only, `PASSED/FAILED/REJECTED` with no state machine, even though a newer, separate quality-disposition ecosystem (`QualityPlanService`/`QualityStockDispositionService`) now exists alongside it. (§11.3)
7. **Manufacturing frontend has not caught up to its backend** — BOM component lines and material-issue/output posting are real backend capabilities now, but `production.page.ts` has no UI for either. (§11.3)

## 15. Verification Gaps (things that could not be executed or are indirectly tested — distinct from code defects above)

1. **PostgreSQL/Testcontainers concurrency proofs remain unexecuted** — this environment has no Docker daemon. `PayrollPaymentConcurrencyTests` (and every other `@PostgresIntegrationTest`-tagged suite) is implemented and compiles but was not run here, consistent with every prior entry in `docs/TEST_EVIDENCE.md`. This is the payroll PAY-001 gate README itself already names as open — not newly discovered, just re-confirmed still open.
2. **`FiscalPeriodGuard`'s closed-period rejection is tested only indirectly for journal/procurement call sites** — `JournalEntryServiceTests`/`ProcurementServiceTests` mock the guard away; the guard's actual rejection behavior is positively tested through other consumers (`AssetDepreciationServiceTests`, `BankReconciliationServiceTests`, `InventoryValuationServiceTests`), not a dedicated `DefaultFiscalPeriodGuardTests`. Implemented, but the direct test coverage is thinner than the "guarded transitions" claim might suggest.
3. **Contractor-settlement payment concurrency has no dedicated PostgreSQL test analogous to payroll's** — the only `@PostgresIntegrationTest` in the workforce-settlement area covers import-commit batching, not settlement/payment posting races. No current doc claims otherwise, so this is a forward-looking note, not a correction.
4. **Chart of Accounts hierarchy and Fiscal-period close-orchestration test depth** were confirmed to exist in source but their test suites were not exhaustively read for assertion quality within this pass's time budget.
5. **Full-Docker backend suite** (`./gradlew test`, includes all Testcontainers suites) was not run — only `-PskipDockerTests` was run, matching what every other evidence-log entry in this repo has always been limited to in this kind of environment.

## 16. Documentation Changes — summary

Seven files were corrected (full list and reasons in §7): `CLAUDE.md`, `PROJECT_MAP.md` (three separate edits), `README.md`, `docs/TEST_EVIDENCE.md`, `docs/TECHNICAL_GUIDE_CHECKLIST.md`. All changes either (a) fixed a self-contradiction, (b) softened an overclaim to match verified reality, (c) added missing current-state context, or (d) added a staleness banner to a document already cited elsewhere as current. No historical document's original content was rewritten; no test, migration, or application source file was touched.

## 17. Final Reconciliation Pass

**Documentation → Code:** every claim actually checked in this pass (payroll state machine, CSP header, Node toolchain, Liquibase head version, Sales O2C, journal/fiscal-period guards, Chart of Accounts, AR aging, procurement/inventory/manufacturing/contractor-workforce/notifications, `@PreAuthorize` coverage, tenant isolation, V84 locking, changelog wiring, test-count baselines, frontend route inventory) now matches reality in this document, with every mismatch found either corrected in the source doc or explicitly recorded as an open implementation/verification gap above. Claims *not* individually re-checked (the remaining ~40 individual work-package entries in `PROJECT_MAP.md`'s "[COMPLETED & VERIFIED]" list not covered by the sampled areas above) were not each independently verified line-by-line — this was a deliberately scoped, evidence-first pass over the highest-value and explicitly-named areas, not an exhaustive re-audit of all ~52 work packages. That is itself disclosed here rather than implied as complete.

**Code → Documentation:** the mandatory reverse check found one large gap (§13.2 — ~20 routed frontend features, including an entire clinic vertical, absent from all active docs) and it has been added to `PROJECT_MAP.md`. Backend-side undocumented functionality was not separately inventoried beyond what surfaced incidentally during the sampled-area agent passes (e.g., the second/older `helpdesk` ticket module coexisting with the documented Epic-11 `support` system — noted in §11.6, not yet added as its own doc entry since it is a duplication/naming-collision risk rather than missing functionality).

**Documentation → Documentation:** the one direct self-contradiction found (`PROJECT_MAP.md`'s TASK 05 status, §4/C-1) is corrected. One contradiction was found and *not* resolved by guesswork (§4/C-2 — two incompatible TASK 06–12 numbering schemes in the same file, sourced from what appear to be two different external roadmap documents) — flagged for a human decision rather than silently picking one.

## Executive Summary

- **Overall alignment:** the active documentation (`CLAUDE.md`, `README.md`, `PROJECT_MAP.md`, `docs/TEST_EVIDENCE.md`) was, on the whole, more accurate than not for the highest-stakes claims — the payroll state machine claim in particular was precise and fully verified. But three categories of drift were real and are now corrected: (1) a self-contradiction in `PROJECT_MAP.md` about a specific task's status, (2) two evidence documents (`docs/TEST_EVIDENCE.md`'s baseline table, `docs/TECHNICAL_GUIDE_CHECKLIST.md` broadly) that had gone stale without being re-labeled as such, and (3) a substantial reverse-direction gap — real, working frontend functionality (an entire clinic vertical among ~20 items) that no active document mentions at all.
- **Major mismatches found:** `PROJECT_MAP.md` self-contradicted on the Owner Executive Cockpit's status; the "31 routes" frontend claim undercounted current routes by ~3.4×; `docs/TECHNICAL_GUIDE_CHECKLIST.md`'s Sales/Journal-Entries/Fiscal-Periods verdicts were stale and false; `docs/TEST_EVIDENCE.md`'s CI baseline table was ~900 tests behind what the enforcement script actually requires; README's AR-aging claim didn't match what the code does; PROJECT_MAP's "every controller has `@PreAuthorize`" claim had one real, confirmed exception.
- **Major corrections made:** all of the above were corrected in the relevant active document (see §7/§16), each with a pointer back to this report's evidence.
- **Actual implementation gaps discovered** (not documentation problems — real code/schema defects, documented not fixed): AR-aging as-of-date is a no-op; `serviceops` controllers have no authorization checks at all; two Liquibase migrations are orphaned; `sales_orders` lacks a tenant-scoped index. See §14.
- **Verification limitations:** no Docker daemon in this environment, so every PostgreSQL/Testcontainers-gated concurrency proof (payroll's included) remains unexecuted here, exactly as every prior evidence-log entry in this repo already stated. See §15.

### `DOCUMENTATION PARTIALLY ALIGNED — OPEN GAPS REMAIN`

Full alignment cannot be honestly claimed: two implementation defects (§14 items 1–2) and one unresolved documentation contradiction (§4/C-2) remain open by design — fixing the code defects was out of this pass's scope, and the roadmap-numbering contradiction requires a decision only the roadmap's owner can make. Everything that *was* correctable within a documentation-only pass has been corrected and is listed in §7/§16.
