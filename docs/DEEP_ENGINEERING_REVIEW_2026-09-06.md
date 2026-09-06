# Deep Engineering Review — 2026-09-06

**Scope:** Repository-wide engineering review, with primary focus on the latest shipped feature — Owner Executive Cockpit / Profit Pulse / Executive Analytics (`be/src/main/java/com/bemo/hr/analytics/`, `fe/src/app/features/analytics/executive/`, Liquibase v459/v460).
**Method:** Direct line-by-line reading of the core service (`ExecutiveAnalyticsService.java`, 1297 lines) and its controller/API/domain/repository/migration/test files, cross-referenced against the ERP's existing real implementations of the same business concepts (Finance GL, Party AR aging, Inventory valuation, Payroll) in other modules. Four focused sub-passes (frontend, performance/concurrency/security, ERP domain-consistency, plus this document's own synthesis) verified specific claims against source, not documentation. Verification commands were actually executed — see §"Verification Commands Executed" for exact results.
**This is a review-only document. No Java, TypeScript, migration, test, or configuration file was modified while producing it.**

---

## Executive Summary

**Overall assessment: CRITICAL.**

The Owner Executive Cockpit is implemented, passes its own tests, and satisfies every repo-wide static gate (error codes, translation catalog, authorization contract, i18n, hardcoded-string scan). None of that proves it is correct. Read as source code, its core service **manufactures plausible-looking financial numbers — specific EGP amounts, named fake customers, named fake projects, invented product sales — whenever the real data it queried is empty or zero, with no signal anywhere in the API response or the UI that a number is real versus fabricated.** This is exposed through the most senior-role-gated endpoint in the system (`SUPER_ADMIN`/`ADMIN`/`FINANCE_MANAGER`/`PROJECT_MANAGER`/`GENERAL_MANAGER` only) and is directly downloadable as an "official-looking" multi-sheet Excel workbook. This is the review's single Critical finding and the reason the overall assessment is Critical rather than High: it is not a missing feature or an edge-case bug, it is a systemic design pattern across the whole service (and its sibling `ProjectExecutiveDashboardService`), and it defeats the entire purpose of an executive decision-support tool — the failure mode is silent and confident-looking, which is worse than an obvious error or empty state.

Beyond that, the feature duplicates — with different, non-reconciled logic — three pieces of business math the ERP already implements correctly elsewhere (AR aging in `PartyFinancialPositionService`, COGS/inventory valuation in `InventoryValuationService`, and P&L in `FinancialStatementsReportService`, all GL/ledger-sourced), meaning an owner and a finance user looking at "the same" number for the same period can legitimately see two different, unreconciled figures even when both screens are "working as coded."

| Severity | Count | Theme |
|---|---|---|
| Critical | 1 | Fabricated financial data presented as real |
| High | 6 | Synthetic branch/product/stock data, duplicated & divergent business logic, unproven authorization, N+1/full-scan performance |
| Medium | 7 | Dead API parameters, missing idempotency, inconsistent headcount definitions, hardcoded OPEX constant, export unbounded lists |
| Low | 4 | Minor precision/naming inconsistencies, code duplication with sibling dashboard |

---

## Critical Findings

### C-1. `ExecutiveAnalyticsService` fabricates specific, named financial data when real data is empty or zero

**Evidence** (`be/src/main/java/com/bemo/hr/analytics/application/ExecutiveAnalyticsService.java`, method `getOwnerCockpit`, which backs the `/api/v1/analytics/executive/cockpit` endpoint the frontend cockpit page actually calls):

- Line 570-572: if today's real sales total to exactly zero, `todaySales` is silently set to `42_850.00`.
- Line 582-584: same pattern for `todayCollections` → `38_200.00`.
- Line 587-589: `totalRevenue` → `1_450_000.00`.
- Line 592-594: `totalCogs` → `totalRevenue * 0.62`.
- Line 612-614: `totalPayrollDisbursed` → `185_000.00`.
- Line 617-619: `totalOpex` → `250_000.00`.
- Lines 673-683: if computed AR totals to zero, **every AR aging bucket and invoice count is overwritten** with hardcoded values (`220_000.00` current / 14 invoices, `75_000.00` 30-60 / 5 invoices, `30_000.00` 60-90 / 2 invoices, `15_000.00` 90+ / 1 invoice, total `340_000.00`).
- Lines 722-732: identical pattern for AP aging (`140_000`/`45_000`/`18_000`/`7_000`, total `210_000.00`).
- Lines 784-788: if no in-progress/planned manufacturing orders are found, two **named fake production orders** are fabricated ("وحدة خلط وتعبئة أوتوماتيكية" / "ألواح عزل حراري ومقاومة للرطوبة") with invented quantities and a WIP valuation of `68_400.00`.
- Lines 820-825: if no open projects exist, two **named fake construction projects** are fabricated ("أبراج النيل الإدارية" / "مجمع العاصمة السكني") with invented contract values, budgets, and cost variances.
- Lines 891-897: if the tenant has no customer invoices at all, **five named fake customer companies** are fabricated ("شركة الأهرام للمقاولات العامة", "مجموعة النيل للاستثمار العقاري", etc.) with invented invoiced/collected/outstanding amounts.
- Lines 900-918: **Top Products is not a fallback — it is unconditionally fabricated whenever any inventory items exist at all.** `qty = 150 - (i*20)`, `price = 450 + (i*120)` (line 904-905) — pure index arithmetic assigned to the tenant's real item codes/names, never derived from any actual sales line. If no items exist either, five entirely fake products with fake Arabic names and SKUs are used instead (lines 912-918).
- Lines 743-753: "current stock" for every low-stock/dead-stock alert is computed as `reorderPoint * 0.4` (line 746) — **never the item's real on-hand quantity** — so the stock-alert list an owner sees does not reflect real inventory levels even when everything else about the item is real.
- Lines 828-858: the branch performance leaderboard does not query any branch-scoped financial data — it takes the tenant-wide totals and splits them by a fixed formula (main branch = 60%, remaining branches split the other 40% evenly, line 836), regardless of each branch's actual revenue/cost/cash.

**Why this is Critical, not High:** every one of these paths returns `HTTP 200` with a fully-populated, correctly-typed, plausible-looking `OwnerCockpitResponse` — there is no error, no `null`, no empty-state signal, and the API contract (`ExecutiveAnalyticsApi.java`) has no field indicating "this value is synthetic." A brand-new tenant, a tenant in a slow month, or a tenant whose invoices simply haven't been entered into *this specific set of tables* yet (see C-3, C-4 below on why real data can easily be zero even when the business isn't) will see a full, confident, exportable financial dashboard describing a business that does not exist. The `exportExecutiveCockpitExcel` method (line 1005) inherits this completely — an owner can download and circulate a multi-sheet "Executive Cockpit" workbook containing fabricated revenue, AR/AP aging, named fake customers, and named fake projects, formatted identically to a real report.
**This pattern is systemic, not a one-off:** the sibling `be/src/main/java/com/bemo/hr/project/executive/application/ProjectExecutiveDashboardService.java` (lines 176-178) does the same thing for its own bank/cash/uncleared figures (`totalRevenue * 0.40`, `totalActual * 0.05`, `totalCommitted * 0.10`), confirming this is an established pattern in this area of the codebase, not an isolated mistake in one method.
**Recommended fix (for the next task, not this one):** remove every hardcoded fallback; when a real aggregate is genuinely zero/empty, return zero/empty and let the frontend render an explicit "no data for this period" state (the frontend review in this report — see Frontend Review — assesses whether the UI is even prepared to do that today).

---

## High Priority Findings

### H-1. Duplicated, non-reconciled AR aging logic (Executive Cockpit vs. Party Financial Position)
Two independent AR aging implementations exist with different bucket semantics:
- `PartyFinancialPositionService.getFinancialPosition` (`be/src/main/java/com/bemo/hr/party/application/PartyFinancialPositionService.java:49-78`) ages `PartnerLedgerEntry` rows by each customer's actual `paymentTermsDays`; its "current" bucket is strictly not-yet-due.
- `ExecutiveAnalyticsService.getOwnerCockpit` (lines 653-670) ages `CustomerInvoice.outstandingAmount`/`dueDate` directly with a flat 30-day fallback; its "current" bucket absorbs both not-yet-due AND up to 30 days *overdue*.
An invoice 15 days overdue is "current" on the Owner Cockpit and "1-30 days overdue" on Party Financial Position, for the same tenant, same day, same invoice. Neither implementation honors a caller-supplied as-of date (both use "now"). See the ERP Domain Consistency section for full detail.

### H-2. COGS and Inventory Valuation never use the ERP's real costing engine
`InventoryValuationService` implements real FIFO/weighted-average costing with GL reconciliation (`be/src/main/java/com/bemo/hr/operations/InventoryValuationService.java`). `ExecutiveAnalyticsService` never calls it. Instead: COGS for invoices without a populated `cogsAmount` is guessed at `amount * 0.65` (line 551); all POS revenue's COGS is guessed at `posSales * 0.65` unconditionally (line 591); and the standalone "Inventory Valuation" KPI in `getExecutiveOverview` is computed as `reorderQuantity * 150` (a fixed fake unit cost, line 297) or, if that's zero, `itemCount * 5000` (line 301) — never the item's real costed value.

### H-3. Net Profit / P&L is architecturally disconnected from the General Ledger
`FinancialStatementsReportService` computes revenue/expenses/net income from posted `JournalEntry`/`JournalEntryLine` records grouped by `Account.Type`. `ExecutiveAnalyticsService` never imports or queries `JournalEntry`, `JournalEntryLine`, or `Account` anywhere in the file — its "Net Profit" is `grossMarginAmount - totalOpex`, where `totalOpex` includes an unconditional, unexplained flat `+65_000` constant (line 616) on top of raw expense-claim and payroll sums. The two "Net Profit" figures a tenant sees on different screens are not two views of one ledger truth; they are two unrelated calculations that happen to share a name.

### H-4. No test proves authorization or branch-access-denial for this feature
`ExecutiveAnalyticsControllerTests` (`be/src/test/java/com/bemo/hr/analytics/api/ExecutiveAnalyticsControllerTests.java`) are pure Mockito unit tests against a plain `new ExecutiveAnalyticsController(mockService)` — there is no `@WebMvcTest`/Spring Security context anywhere, so **no test in the repository exercises the `@PreAuthorize` annotations on any of the 8 Executive Analytics endpoints.** Separately, `ExecutiveAnalyticsServiceTests` mocks `SecurityAuthorizationEvaluator` but never configures a scenario where `hasBranchAccess` returns `false` — the `BRANCH_ACCESS_DENIED` (403) path in `getOwnerCockpit` (line 520-524) has zero test coverage, and the per-branch `continue`-skip filtering in the leaderboard loop (line 833-835) is equally untested.

### H-5. `getOwnerCockpitAggregatesAllKpis` — the one test that exercises the "real data" path — asserts nothing about values
This test (`ExecutiveAnalyticsServiceTests.java:259-283`) deliberately mocks **every** repository to return an empty list — i.e., it runs exactly the code path that triggers every fabrication in C-1 — and then asserts only `response`, `kpiSummary()`, `arAging()`, `apAging()` are `isNotNull()`. It does not assert a single numeric value. This test would pass identically whether the method returns the real (zero) figures or the fabricated ones, and would also pass if someone quietly changed a fallback constant. It provides no correctness signal at all for the feature's most important behavior.

### H-6. Full-table `findAll()` on ~13 repositories per request, plus an N+1 inside a loop
Every call to `/overview`, `/cockpit`, or `/cockpit/export.xlsx` loads the tenant's **entire, unbounded history** of customer invoices, receipts, supplier invoices, POS transactions, inventory items, employees, expense claims, salary payments, sales quotations, and ETA submissions into memory via `repository.findAll()` (no date filter, no pagination, no projection) and then filters/aggregates in Java streams — including a manual `yyyy-MM` string-format comparison per invoice (line 543) that could have been a single indexed date-range query. Additionally, inside `for (Project p : projects)` (`getOwnerCockpit`, lines 796-818), `costLedgerRepository.sumAmountByProjectIdAndEntryType(p.getId(), ...)` is called once per active project — a textbook N+1. See Performance Review for full detail and dataset-size impact estimate.

---

## Medium Priority Findings

### M-1. `companyId` and `projectId` request parameters are accepted and echoed but never used to filter anything
`getOwnerCockpit(period, companyId, branchId)` and `getExecutiveOverview(period, companyId, branchId, projectId)` both declare `companyId`; `getExecutiveOverview` also declares `projectId`. Neither parameter appears anywhere in the 1297-line service body except being echoed back into the response object (`OwnerCockpitResponse.companyId()`, line 964). Only `branchId` is actually enforced (via `SecurityAuthorizationEvaluator.hasBranchAccess`). A caller can pass any `companyId`/`projectId` string — belonging to another tenant's company/project or a nonexistent one — and receive identical, unscoped, tenant-wide data with no error. This is not a cross-tenant data leak (Hibernate's `@TenantId` filter still scopes the underlying `findAll()` calls to the caller's own tenant), but it is a misleading API contract: the parameter's presence implies scoping that does not exist.

### M-2. No idempotency/uniqueness on `executive_kpi_snapshots`
The table (Liquibase `v319`) has only plain indexes on `app_id`+period/category/key — no unique constraint. `recordSnapshot()` always `new ExecutiveKpiSnapshot(...)` + `save()`, with no find-or-update check. Repeated or concurrent `POST /snapshots` calls with the same `periodKey`+`category`+`kpiKey` silently create unbounded duplicate rows; `listSnapshots` has no dedup on read.

### M-3. Concurrent `saveTargets` for a not-yet-existing period is not race-safe at the HTTP-response layer
`ExecutiveCockpitTarget` does have a real DB unique constraint on `(app_id, period_key)` (Liquibase v459) and an `@Version` column — good. But `saveTargets` (service line 1259-1261) does `findByPeriodKey(...).orElse(null)` then constructs-and-saves a new row with no pessimistic lock and no `try/catch` around the save. Two concurrent first-time `POST /targets` calls for the same new period will both pass the `null` check, and the second `save()` will throw a raw `DataIntegrityViolationException` from the unique constraint — worth confirming (see Performance/Concurrency findings) whether `ApiExceptionHandler` turns this into a clean 409 or lets a raw 500 through.

### M-4. Headcount is computed two different ways in the same feature
`getExecutiveOverview` computes `activeHeadcount` by filtering `Employee::isActive` (line 324). `getOwnerCockpit`'s `OwnerCockpitKpiSummary.activeHeadcount` (line 948) uses `employeeRepository.count()` — the **total** employee count, including inactive employees, despite the field being named `activeHeadcount` in both DTOs. The `/overview` and `/cockpit` endpoints of the same feature can report two different headcount numbers for the same tenant at the same moment, and the cockpit's number over-counts relative to what its field name promises.

### M-5. Unconditional flat OPEX constant with no stated basis
`totalOpex = totalClaimedExpenses.add(totalPayrollDisbursed).add(BigDecimal.valueOf(65_000))` (line 616) — every tenant's operating expenses include a flat, unexplained `65,000` EGP addition on top of whatever real expense claims and payroll exist, with no comment, config flag, or KPI-registry documentation for what this represents.

### M-6. Excel export has no upper bound on several list sections
`topCustomers`/`topProducts` are hard-capped at 5, but `branchLeaderboard`, `lowStockAlerts`, `deadStockAlerts`, `manufacturingWip`, and `projectBudgetControl` are not capped anywhere in the service or the export method — for a large tenant this could produce an unexpectedly large in-memory workbook and response payload on every export request.

### M-7. "Payroll Disbursed" label vs. what it actually measures
The KPI reads `SalaryPayment.netAmount` (post-deduction take-home pay). The Payroll module separately computes employer social-insurance contribution (`PayrollController`/`PayrollService`), which is real employer cost never folded into this figure. An owner reading "Payroll Disbursed" as "total cost of my workforce" would understate it — a scope/labeling gap, not a wrong-formula bug, but relevant to an executive-facing KPI that PROJECT_MAP.md's Session 18 entry calls "Payroll Disbursed vs Pending."

---

## Low Priority Findings

### L-1. Fiscal periods are ignored entirely
No reference to `FiscalPeriod`/`FiscalPeriodGuard` anywhere in `ExecutiveAnalyticsService.java`; "period" is pure `yyyy-MM` calendar-string matching, independent of the tenant's actual configured fiscal calendar or whether that period is open/closed/locked in Finance. Low severity because the KPI grain (`KpiGrain.MONTHLY`/`DAILY`/`REAL_TIME`) doesn't claim fiscal-period alignment, but worth noting given the rest of Finance (`InventoryValuationService`, journal posting) is fiscal-period-aware.

### L-2. Precision/scale inconsistency between target and snapshot tables
`executive_cockpit_targets.target_revenue` is `NUMERIC(15,2)`; `executive_kpi_snapshots.actual_value` is `NUMERIC(19,4)`. Not a bug, but a variance calculation comparing a saved target against a snapshot's actual value crosses two different stored precisions.

### L-3. `getComparativeTrends` is 100% synthetic, unconditionally, for every request
Unlike `getOwnerCockpit`'s "fallback only when real data is empty" pattern, `getComparativeTrends` (lines 443-466) *never* attempts to read real data at all — every point in the trend chart is `1_200_000 * (1 + i*0.03)`-style synthetic growth. This predates TASK-05 (it's part of the original analytics scaffold) but is worth flagging alongside C-1 since the frontend trend chart presumably renders it identically to real data.

### L-4. Two-constructor code smell
`ExecutiveAnalyticsService` has two public constructors — the real 20-dependency one and a 7-dependency legacy one that null-fills the rest (lines 131-143), apparently kept only so older tests/callers compile. This is dead-weight coupling risk: any new required dependency added to the real constructor silently becomes `null` for anything still using the short one, which is exactly the mechanism that makes the null-guarded `!= null` fallbacks throughout the file (and thus C-1) possible in the first place.

---

---

## Frontend Review (`fe/src/app/features/analytics/executive/`)

1. **No provenance signal for fabricated data (Critical, compounds C-1).** `executive-analytics.models.ts` has no `isSynthetic`/`dataQuality`/freshness field anywhere in `OwnerCockpitResponse` or its nested types, and the template renders every KPI card, aging bucket, branch row, and customer/product row identically regardless of whether the backend value is real or a hardcoded fallback. There is no "no real data yet" banner, no muted styling variant, no disclaimer tooltip. This is the frontend half of C-1: even if the backend fix in a follow-up task adds a provenance flag, the frontend contract and template have nothing wired to display it today.
2. **Stale data survives a failed refresh (High).** `loadCockpit()` (`page.ts:121-139`) sets the error signal on failure but never clears `cockpitData()`. If a user changes filters and the new request fails, the previous filter's numbers stay fully rendered under a separate, visually-decoupled error banner (`page.html:20-22` vs. the cockpit grid at line 86+) — easy to miss, showing old numbers as if they were current.
3. **No empty-state UI for cockpit tables (High).** Unlike the Snapshots tab (`page.html:597-599`, which has a real `@empty` block), the cockpit's branch leaderboard, top customers/products, alerts, WIP, and project-control tables have no equivalent — an empty array just renders an empty `<tbody>` with no "no data" message.
4. **Race condition on rapid filter changes (High).** `loadCockpit()`, `onPeriodPresetChange()`, `loadAll()`, and `changeMonths()` all fire independent `firstValueFrom` requests with no cancellation/`switchMap` and no in-flight guard (`page.ts:121-268`). Rapidly clicking period-preset pills (all rendered live and clickable, `page.html:48-59`) can let a slower response for an older filter overwrite a newer one — the UI can silently show the wrong period's numbers with no error.
5. **Percent-field formatting is inconsistent and unguarded (Medium).** `topProducts.marginPercent` uses `| number:'1.1-1'`, but `grossMarginPercent`, `netMarginPercent`, `variancePercent`, both aging summaries' `percentOfTotal`, `expenseBreakdown.percentOfTotal`, and `trendPoints.marginPercent` are all interpolated raw with no pipe and no null/NaN guard. A real `0/0` division on the backend (plausible whenever a bucket total is zero) renders literally as `NaN%`; `undefined` renders as a bare `%`.
6. **Aging/expense bar widths unclamped (Low/Medium).** `[style.width.%]="x.percentOfTotal"` has no clamping; a `NaN` value silently renders a 0-width bar — visually indistinguishable from "genuinely no overdue amount," which is misleading specifically because the two situations ("no data" vs "no risk") should never look the same on an executive dashboard.
7. **`placeholder` attributes bypass the hardcoded-string scanner (Low).** `check:hardcoded` strips all markup/attributes before scanning, so it is structurally blind to hardcoded `placeholder="e.g. 2026-08"` / `placeholder="e.g. 2026-Q3"` (`page.html:64,432,618,673`) — real, English-only user-facing hint text that shows even in an Arabic session and passes the automated gate only because the gate cannot see it.
8. **Test suite doesn't test any of the above (High test gap).** All 5 tests in `executive-analytics.page.spec.ts` assert on signals/outgoing request shape, never on rendered DOM, formatting, or error/race/empty-state behavior — every finding 2–6 above could regress with the suite reporting fully green.
9. **Not a problem:** subscription cleanup (all async work uses `firstValueFrom`, which auto-completes — no manual `.subscribe()` leak risk), i18n/hardcoded-string compliance for the strings the scanner *can* see, and mobile/responsive breakpoints (grid layouts use `auto-fit`/`minmax` and degrade without an explicit breakpoint issue).

---

## Performance Review

### P-1. Full-tenant-history table scans on every dashboard load (Critical)
`getOwnerCockpit`/`getExecutiveOverview` call `.findAll()` on ~13 repositories per request with zero date/period predicate pushed to SQL — the `period` filter is applied by formatting each row's date to `yyyy-MM` and string-comparing it in a Java stream *after* every row for that entity type has already been loaded (e.g. `CustomerInvoice`, line 543; POS transactions convert `Instant`→`LocalDate` per row in Java, line 559). For the reference scale in the task brief (50k invoices / 20k POS rows / 5k inventory items), a single page load issues on the order of 13+ unfiltered `SELECT * ... WHERE app_id = ?` queries with no `LIMIT` — tens of thousands of full entity hydrations — and this repeats on **every** page view, by every viewer, with no caching layer (the existing `ExecutiveKpiSnapshot` infrastructure is never consulted by the live cockpit). Cost grows linearly with tenant data and with concurrent viewers.
**Recommendation:** push period/date filtering into the repository queries (`WHERE app_id = ? AND invoice_date BETWEEN ? AND ?`), and/or precompute and cache period aggregates (the `executive_kpi_snapshots` table already exists for exactly this purpose and is currently unused by this endpoint).

### P-2. N+1 on project cost-ledger sums (High)
`costLedgerRepository.sumAmountByProjectIdAndEntryType(p.getId(), ...)` is called once per active project inside the `for (Project p : projects)` loop (lines 796-818) — confirmed a real N+1 against an indexed single-project aggregate query. For 200 active projects this is 200 serial round trips inside one read-only transaction. Each individual query is index-served (`idx_cost_ledger_app_prj_type`), so this is a latency problem (accumulated round-trip overhead), not a scan problem.
**Recommendation:** replace with one `GROUP BY project_id` query and a lookup map.

### P-3. Excel export doubles the cost and has unbounded sheet sizes (Medium)
`exportExecutiveCockpitExcel` adds no further N+1 beyond calling `getOwnerCockpit` once, but it inherits P-1's full cost on every export click, builds the workbook with POI's non-streaming `XSSFWorkbook` (fully in-memory), and several sheets (`branchLeaderboard`, `lowStockAlerts`/`deadStockAlerts`, `manufacturingWip`, `projectBudgetControl`) have no row cap — unlike `topCustomers`/`topProducts`, which are capped at 5. For a tenant with thousands of low-stock items, this is a plausible latency/memory spike per concurrent export request.

### P-4. Snapshot table has no supporting index for its unfiltered listing, and will grow unbounded (Low, compounds M-2)
`findAllByOrderByCreatedAtDesc()` (backing `GET /snapshots` with no `periodKey`) has no index on `created_at` and no pagination — combined with M-2's unbounded duplicate-row growth, this listing degrades over time with no cleanup path anywhere in the codebase.

---

## Security Findings

No cross-tenant data leak was found in this feature — `@TenantId` (Hibernate multi-tenancy) is present on the underlying entities and the tenant-scoping filter applies automatically to every `findAll()` call, including the native/JPQL aggregate query used for the N+1 in P-2.

1. **`companyId`/`projectId` are accepted but never enforced (High, contract/scoping gap rather than a tenant breach).** Both parameters exist in the API shape of `getExecutiveOverview`/`getOwnerCockpit`, implying company- or project-level scoping *within* a tenant. Neither is used for filtering anywhere in the 1297-line service — `companyId` is only echoed back into the response, `projectId` is not referenced at all past the parameter declaration. If this platform's tenants can contain multiple companies/legal entities, **any user with cockpit access sees all companies' financials merged, with the API silently ignoring the parameter that implied otherwise** — no error, no partial result, just a full unscoped answer. Only `branchId` is genuinely enforced (via `SecurityAuthorizationEvaluator.hasBranchAccess`, throwing 403 `BRANCH_ACCESS_DENIED`, and actually filtering the branch-leaderboard loop).
2. **No test proves the 5-role `@PreAuthorize` gate actually rejects an unauthorized caller (High).** `ExecutiveAnalyticsControllerTests` are plain Mockito unit tests with no Spring Security context — none of the 8 endpoints' `@PreAuthorize` annotations are exercised by any test in the repository (unit or integration). This mirrors a pattern flagged in the previous reconciliation review (a different module, `serviceops`, had *no* `@PreAuthorize` at all and was undetected by `check-authorization-contract.py` because that script only validates role-string spelling inside annotations that already exist — it proves nothing about test coverage of enforcement).
3. **Branch-access-denial path is implemented but untested (Medium).** The `BRANCH_ACCESS_DENIED` 403 path (line 520-524) and the per-branch leaderboard filtering (`continue` at line 833-835) have zero test coverage — a regression here (e.g., someone inverting the `if` condition) would not be caught.
4. **Target-creation race resolves to a clean-ish 409, not a raw 500 (Medium, corrected during review).** Two concurrent first-time `POST /targets` for the same period both pass a null-check and attempt to persist; the DB unique constraint on `(app_id, period_key)` rejects the loser, and `ApiExceptionHandler`'s global `DataIntegrityViolationException` handler does turn this into `409 DATA_CONFLICT` — so this is not an unhandled-exception security concern, but the error message is generic (doesn't name the field or explain the cause) and the losing request's submitted values are silently discarded with no retry-as-update path.
5. **Snapshot recording has no uniqueness/idempotency protection at all (High — see M-2/P-4).** Unlike targets, `recordSnapshot` doesn't even attempt a find-before-insert; combined with the missing DB unique constraint, this is a real data-integrity gap an authenticated, authorized, well-intentioned user (e.g. a retried request from a flaky connection) can trigger with no malicious intent required.

---

## Data Correctness Findings

Tracing **Database → Repository → Service → API → Frontend** for the headline KPIs:

| KPI | Real source available? | What the code actually does | Correctness verdict |
|---|---|---|---|
| Today's Sales / Collections | Yes (`CustomerInvoice`, `PosTransaction`, `CustomerReceipt`) | Computed correctly from real rows filtered by `today.equals(...)` **when any exist** — but silently replaced by hardcoded constants (42,850 / 38,200) when the real sum is exactly zero | **Financially incorrect when real value is legitimately zero** — indistinguishable from a real transaction day |
| Total Revenue | Partially — invoices + POS, but not GL | `totalRevenueInvoices + totalPosSales`, falls back to `1,450,000.00` if zero; never cross-checked against `FinancialStatementsReportService`'s GL-sourced revenue | **Architecturally independent of the ERP's accounting revenue figure** — can disagree with Finance Reports even when "correct" |
| COGS / Gross Margin | Yes for delivery-flow invoices (`CustomerInvoice.cogsAmount`); yes in general via `InventoryValuationService` | Uses `cogsAmount` when populated, else guesses `amount * 0.65`; POS COGS is always guessed at `posSales * 0.65`; never calls the real costing engine | **Systematically approximated, not computed**, for any invoice not created via the specific delivery flow and for 100% of POS revenue |
| Inventory Valuation | Yes, via `InventoryValuationService` (real FIFO/weighted-average with GL reconciliation) | `reorderQuantity * 150` (fixed fake unit price) or `itemCount * 5000` fallback — the real valuation service is never called | **Fabricated, not approximated** — no relationship to actual cost |
| AR / AP Aging | Yes, and already computed elsewhere (`PartyFinancialPositionService`) | Re-implemented inline with different bucket-boundary semantics (see ERP Domain Consistency H-1) and a full hardcoded fallback when totals are zero | **Two different, unreconciled numbers can legitimately exist for the same tenant/day, plus outright fabrication on empty data** |
| Net Profit / Margin | Yes, via `FinancialStatementsReportService` (GL-sourced) | `grossMargin - totalOpex`, where `totalOpex` includes a flat, unexplained `+65,000` constant on top of raw expense/payroll sums; never touches `JournalEntry`/`Account` | **Not the same number as the ERP's actual P&L**, by construction, not just by data gaps |
| Active Headcount | Yes, `Employee.isActive` | `/overview` filters `isActive()`; `/cockpit` uses `employeeRepository.count()` (all employees, active or not) for a field named `activeHeadcount` | **Internally inconsistent between two endpoints of the same feature** |
| Branch Performance | Yes — `Branch`-scoped transactions exist elsewhere in the ERP (per the prior reconciliation review's confirmed Multi-Branch Control Center work) | Tenant-wide totals split by a fixed 60%/40%-even-split formula, never queried per branch | **Fabricated allocation, not real branch-level data**, despite the underlying branch-scoped infrastructure existing |
| Top Products | Yes, real sales exist | `qty = 150 - i*20`, `price = 450 + i*120` — computed from array index, not any sales record, whenever any inventory item exists at all | **Always fabricated, unconditionally** (not just a zero-data fallback) |
| Manufacturing WIP / Project Budget | Yes | Real when in-progress orders/open projects exist (with one more N+1/fallback layer inside — actual cost defaults to `budget * 0.72` when the cost ledger has no entry); fully fabricated named examples when none exist | **Real-with-caveats when data exists; fabricated when it doesn't** |

**Silent-corruption risk statement (explicitly requested by the review brief):** every row in the table above marked "fabricated" or "always approximated" returns HTTP 200 with a well-typed, precisely-formatted BigDecimal — there is no technical failure signal anywhere. This is the definition of a silent data-correctness risk: the number can be *returned successfully* while being *financially wrong*, and nothing in the API, tests, or UI currently distinguishes the two cases.

---

## ERP Domain Consistency

The feature invents its own definitions for revenue, COGS, OPEX, net profit, and AR aging rather than composing the ERP's existing, correct implementations of each:

1. **AR Aging** — `PartyFinancialPositionService.getFinancialPosition` (`be/src/main/java/com/bemo/hr/party/application/PartyFinancialPositionService.java:49-78`) ages real `PartnerLedgerEntry` subledger rows against each customer's actual `paymentTermsDays`, with "current" meaning strictly not-yet-due. `ExecutiveAnalyticsService.getOwnerCockpit` (lines 653-670) ages `CustomerInvoice.outstandingAmount`/`dueDate` directly with a flat 30-day fallback, and its "current" bucket absorbs both not-yet-due *and* up to 30 days overdue. An invoice 15 days overdue is "current" on the Owner Cockpit and "1-30 overdue" on Party Financial Position — same tenant, same day, same invoice, two different answers. Neither honors a caller-supplied as-of date (both silently use "now" — a pre-existing bug on the Party side, confirmed in the prior reconciliation review; the same behavior is separately present here too, not shared code).
2. **COGS / Inventory Valuation** — `InventoryValuationService` (`be/src/main/java/com/bemo/hr/operations/InventoryValuationService.java`) is the ERP's real FIFO/weighted-average costing engine with GL reconciliation (`report()`, computing `variance = subledgerTotal - glBalance`). `ExecutiveAnalyticsService.java` contains zero references to it, to `InventoryValuationPolicy`, or to `InventoryCostLayer` anywhere in the file.
3. **Net Profit / P&L** — `FinancialStatementsReportService.getIncomeStatement` sums real posted `JournalEntry`/`JournalEntryLine` rows by `Account.Type` (REVENUE/EXPENSE) to compute `netIncome`. `ExecutiveAnalyticsService.java` imports no `JournalEntry`, `JournalEntryLine`, or `Account` class at all — its "Net Profit" is a parallel calculation over raw invoice/POS/expense/payroll sums with an unexplained flat `+65,000` OPEX constant baked in.
4. **Payroll cost** — the cockpit's "Payroll Disbursed" reads `SalaryPayment.netAmount` (employee take-home pay), which is a reasonable match for its own label, but is not the employer's total cost of workforce — the Payroll module separately computes employer social-insurance contribution that never feeds into this KPI. An owner reading "Payroll Disbursed" as "total workforce cost" would understate it.
5. **Fiscal periods** — no reference to `FiscalPeriod`/`FiscalPeriodGuard` anywhere in the file; "period" is pure `yyyy-MM` calendar-string matching, independent of the tenant's actual fiscal calendar or whether Finance has closed/locked that period. By contrast, `InventoryValuationService` explicitly gates its own postings through `FiscalPeriodGuard.requireOpen(...)`.

**Pattern, not incident:** this is not one wrong formula — it is a module that never composes the ERP's own financial/costing/aging services, choosing instead to re-derive each concept independently and approximately from raw tables. Every one of the five items above is a case where two different screens can legitimately disagree about "the same" number for reasons that have nothing to do with missing data.

---

## Test Gaps

Explicitly, per the review brief's requested framing — "the code is implemented, but this behavior is currently not proven by tests":

- **The single test that exercises the all-real-data-empty code path (`getOwnerCockpitAggregatesAllKpis`) asserts only `isNotNull()` on the top-level response and two aging summaries — it proves nothing about any of the ~15 fabricated fallback values in C-1.** This is implemented, tests pass, and the behavior (fabrication) is unproven and unguarded by any assertion.
- **No test proves any `@PreAuthorize` role gate actually rejects an unauthorized user** for any of the 8 Executive Analytics endpoints (no `@WebMvcTest`/Spring Security context exists for this controller at all).
- **No test proves `BRANCH_ACCESS_DENIED` (403) actually fires**, nor that the branch-leaderboard loop actually skips a branch the caller lacks access to.
- **No test proves the AR/AP aging bucket boundaries are correct** (e.g. an invoice exactly 30, 31, 60, 61, 90, or 91 days overdue landing in the expected bucket) — the existing tests never populate a `CustomerInvoice`/`SupplierInvoice` with a real due date to exercise this logic at all.
- **No test proves the N+1 cost-ledger loop or the branch-proportional-split math produces correct per-item results** — `getOwnerCockpitAggregatesAllKpis` mocks `projectRepository.findAll()`/`branchRepository.findAll()` to empty lists, so this logic path is never actually exercised with data.
- **No test proves snapshot recording is idempotent or rejects a duplicate** (there is no such protection to test — see M-2).
- **No test proves the target-creation 409-conflict path** works as `ApiExceptionHandler` implies it should (traced by static reading only in this review, not exercised by an actual concurrent-request test).
- **Frontend: no test exercises an HTTP error from `/cockpit`, the stale-data-on-error behavior, any table's empty-array state, `NaN`/`undefined` percent rendering, or the rapid-filter-change race** — all 5 frontend tests assert on signals/request payloads only, never rendered DOM or these failure modes.
- **Frontend: no test exercises `Promise.all`'s partial-failure behavior in `loadAll()`** — if one of four parallel overview-tab requests fails, the other three succeed but are discarded; untested.

---

## Code Quality

- **Two constructors on `ExecutiveAnalyticsService`** (the real 20-dependency one and a legacy 7-dependency one that null-fills the rest, lines 131-143) is the structural root cause that makes every `!= null` fallback-and-fabricate branch in the file possible — any future dependency added to the real constructor silently becomes `null` for anything still using the short one. This should be removed once nothing depends on it (confirm via test/caller audit before removing — not done in this review, since it's a code change).
- **Method size**: `getOwnerCockpit` is ~460 lines doing 15 distinct concerns (sales, POS, collections, revenue/COGS/margin, expenses/OPEX, cash position, AR aging, AP aging, stock alerts, manufacturing WIP, project budget, branch leaderboard, top customers, top products, expense breakdown, targets) in one method — each concern individually is not complex, but the method as a whole is very difficult to review, test in isolation, or safely modify one section of without risk to the others. This is a measurable maintainability cost, not a stylistic complaint: it's part of why the single "empty data" test can't meaningfully cover 15 independent fallback branches with one assertion.
- **Magic numbers throughout**: `0.65` (COGS ratio, three separate places), `150` (fake unit cost), `5000` (fake per-item value), `0.4`/`120` (fake current-stock/valuation), `0.6`/`0.4` (branch split), `0.85`/`0.72` (project EAC/actual ratios), `65_000` (flat OPEX add), `12_500` (flat per-employee payroll in `getExecutiveOverview`) — none are named constants, configuration values, or documented, making it impossible to distinguish "a deliberate simplifying assumption" from "a placeholder someone forgot to replace with real logic" without reading the surrounding code each time.
- **Comments contradicting behavior**: the KPI registry's `formulaEn`/`formulaAr` fields (e.g. `ETA_COMPLIANCE_RATE`: "Accepted ETA Documents / Total Submissions * 100") document a real formula that the actual code never computes (`etaComplianceRate` is a hardcoded `98.4` or `100.0`, `getExecutiveOverview` lines 329-330) — the documented formula and the executed code have diverged.
- **Duplication with sibling feature**: the same "fabricate a plausible ratio when the real aggregate is unavailable" pattern appears independently in `ProjectExecutiveDashboardService` (lines 176-178) — worth a shared fix/policy rather than patching each occurrence separately.

---

## API Contract Review

- `companyId` (both `/overview` and `/cockpit`) and `projectId` (`/overview` only) are declared, documented by presence, and completely unenforced — see Security Findings #1. This should either be implemented or removed from the contract; leaving it as-is actively misleads API consumers (including the frontend, which faithfully sends these parameters expecting them to matter, per `executive-analytics.service.ts:27-38,57-67`).
- `CockpitTargetResponse` has no field distinguishing a real, tenant-saved target (`id` is a UUID) from the hardcoded system default returned when none exists (`id: "default"`, `getTargets` lines 1241-1251) — a client must string-compare against the literal `"default"` to know which case it's in; this is undocumented and fragile.
- Response DTOs (`ExecutiveKpiCard`, `OwnerCockpitKpiSummary`, etc.) have no field for data provenance/freshness — see Frontend Review #1 and Critical Finding C-1; this is as much an API-contract gap as an implementation one, since even a well-behaved frontend has nothing to bind to.
- HTTP methods, pagination, and error-response shape are otherwise consistent with the rest of the codebase's conventions (`ApiError` via `ApiExceptionHandler`, `@RequestParam` optional filters, `POST` for mutations) — no other contract irregularities found.

---

## Database Review

- `executive_cockpit_targets` (v459): correct primary key, `app_id NOT NULL`, a real unique constraint on `(app_id, period_key)`, a supporting index, and `@Version` — this table is well-designed. (One redundant plain index on `app_id` alone exists alongside the unique constraint that already leads with `app_id`; harmless, just unnecessary.)
- `executive_kpi_snapshots` (v319, pre-existing): `app_id NOT NULL`, plain indexes on `(app_id, period_key)` / `(app_id, category)` / `(app_id, kpi_key)` — but **no unique constraint anywhere**, which is the direct database-level cause of the unbounded-duplication finding (M-2). This is a real schema gap: the table structurally cannot prevent the exact kind of duplicate the application layer also doesn't guard against.
- No PostgreSQL/H2 compatibility issue found in either migration (standard `VARCHAR`/`NUMERIC`/`BIGINT` types, no PostgreSQL-specific syntax).
- No rollback/`changeSet` ordering concern found — both migrations are additive-only (new tables), consistent with the project's append-only migration convention confirmed in the prior reconciliation review.

---

## Documentation Accuracy (lightweight check, per review scope)

- `PROJECT_MAP.md`'s Session-18 entry for this feature describes it in fully-realized, confident terms ("Complete implementation of cross-module executive cockpit," listing AR/AP aging, branch leaderboard, top customers/products, target variance engine as delivered capabilities) with no caveat that large parts of the underlying data are synthetic placeholders when real data is sparse. This is not a factual error about what code exists — the endpoints, tables, and UI genuinely exist — but it is a significant **completeness/accuracy gap**: a reader would reasonably conclude these are real, trustworthy financial figures, which C-1 shows is not reliably true. This document does not correct `PROJECT_MAP.md`'s wording (out of scope — see the constraints below), but flags it here as required by the review brief's §13.
- The KPI registry's documented formulas (e.g. `ETA_COMPLIANCE_RATE`) diverge from the executed code, as noted in Code Quality above — this is closer to a code-quality/correctness issue than a "documentation" one, since the mismatch is between two parts of the same source file (a Java string literal vs. the method that ignores it), not between a doc file and code.
- No other documentation mismatch was found specific to this feature within the scope of this review; a full documentation audit was out of scope per the task brief (§13: "do not spend the entire task rewriting documentation").

---

## Recommended Next Tasks

| Priority | Issue | Area | Evidence | Impact | Recommended Fix |
|---|---|---|---|---|---|
| P0 | Remove all hardcoded fabricated fallback values from `ExecutiveAnalyticsService` (and `ProjectExecutiveDashboardService`) | Data correctness | C-1, §Data Correctness table | Executives can make real decisions on fabricated numbers with zero warning; exportable as an "official" report | Return real zero/empty values when real data is empty; add a `dataQuality`/`isEstimated` field to affected response DTOs; frontend renders an explicit "no data yet" state instead |
| P0 | Add a provenance/estimated-data signal to the API contract and frontend template | Data correctness / API contract / Frontend | C-1, Frontend #1, API Contract Review | Even after the backend fix above, any *legitimately* estimated figure (e.g. COGS ratio fallback) still needs a visible "estimated" marker so it isn't confused with a ledger-sourced number | Extend `ExecutiveKpiCard`/`OwnerCockpitKpiSummary` with a per-field or per-section confidence indicator; render it in `executive-analytics.page.html` |
| P1 | Reconcile or explicitly document the divergence between Executive Analytics's AR aging, COGS, and Net Profit and the ERP's real Party/Inventory/Finance implementations | ERP domain consistency | ERP Domain Consistency §1-3 | Two screens can disagree about "the same" number with no explanation | Either call the existing `PartyFinancialPositionService`/`InventoryValuationService`/`FinancialStatementsReportService` from Executive Analytics, or clearly label the cockpit's figures as a distinct, faster/approximate "operational estimate" rather than an accounting figure |
| P1 | Push period/date filtering into SQL and remove the unbounded `findAll()` pattern; consider caching against the existing (currently unused) `executive_kpi_snapshots` table | Performance | P-1 | Every dashboard view does 13+ unfiltered full-tenant-history scans; degrades linearly with data growth and concurrent viewers | Add date-range repository query methods; populate/consult snapshots instead of recomputing from raw tables on every request |
| P1 | Fix the project cost-ledger N+1 | Performance | P-2 | 200 serial round trips for 200 projects on every cockpit/overview load | One `GROUP BY project_id` query + lookup map |
| P1 | Add a unique constraint (or application-level upsert) to `executive_kpi_snapshots` | Database / Data integrity | M-2, P-4, Security #5 | Unbounded duplicate rows from retries/double-submits, with no cleanup path | Liquibase migration adding `UNIQUE(app_id, period_key, category, kpi_key)`; make `recordSnapshot` an upsert |
| P1 | Implement or remove the `companyId`/`projectId` filter parameters | Security / API contract | Security #1, M-1 | API implies company/project-level scoping that doesn't exist; misleading to any caller relying on it | If multi-company-per-tenant is a real requirement, implement the filter; otherwise remove the parameters and update the frontend service |
| P2 | Add authorization tests for all 8 Executive Analytics endpoints and the branch-access-denial path | Testing | H-4, Security #2-3, Test Gaps | No test proves the role gate or branch check actually rejects unauthorized access | Add `@WebMvcTest`/`@SpringBootTest` with `@WithMockUser` covering an allowed and a disallowed role per endpoint, plus a `hasBranchAccess=false` case |
| P2 | Fix the two-different-headcount inconsistency (`/overview` vs `/cockpit`) | Data correctness | M-4 | Same feature reports two different numbers for a field named identically in both places | Make `/cockpit`'s `activeHeadcount` filter `isActive()` like `/overview` does |
| P2 | Frontend: fix stale-data-on-error, add empty states to cockpit tables, guard percent formatting against NaN/undefined, fix rapid-filter-change race | Frontend | Frontend #2-6 | Silent wrong-period display and confusing zero/NaN rendering | Clear `cockpitData` on error or show an inline stale-data banner; add `@empty` blocks; wrap percent bindings in a null/NaN-safe pipe; use `switchMap`-style cancellation for filter changes |
| P3 | Remove the flat, unexplained `+65,000` OPEX constant and other unnamed magic-number ratios, or make them configurable and documented | Code quality | M-5, Code Quality | Untraceable, undocumented business assumption baked into every tenant's OPEX figure | Extract to named, documented constants or tenant configuration; remove if genuinely a leftover placeholder |
| P3 | Remove the legacy 7-dependency constructor once no caller/test depends on it | Code quality | L-4 | Root structural enabler of the null-fallback pattern | Audit callers/tests, delete after confirming no remaining use |
| P3 | Cap the uncapped Excel export sheets (branch leaderboard, stock alerts, WIP, project control) | Performance | P-3, M-6 | Unbounded workbook size/memory for large tenants | Apply the same `.limit(N)` pattern already used for top customers/products, or switch to `SXSSFWorkbook` streaming |

---

## Verification Commands Executed

All commands from the review brief were actually run in this environment (WSL). As established in the prior reconciliation review, native Gradle on the `/mnt/d` WSL mount is unreliable; the project's own documented workaround (`docs/BUILD_TOOLING.md`: rsync `be/src` to `/tmp/opencode/be-build`, a native-ext4 mirror) was used again here and is the source of the backend result below.

| Command | Result |
|---|---|
| `python3 be/tools/check-error-codes.py` | **PASS** — 821/821 codes have translation rows |
| `python3 be/tools/check-translation-catalog.py` | **PASS** — 18,364 rows, unique key/locale pairs |
| `python3 be/tools/check-authorization-contract.py` | **PASS** — 21/21 roles |
| `./gradlew test -PskipDockerTests` (via the ext4 mirror) | **PASS** — `BUILD SUCCESSFUL in 5m 16s`; **1,510 tests / 286 suites / 0 failures / 0 errors / 1 skipped** |
| `npm run check:i18n` (Node 24) | **PASS** — 6,050 keys, ar-EG + en-US |
| `npm run check:hardcoded` (Node 24) | **PASS** — 148 HTML / 330 TS files, 0 violations (see Frontend Review #7 for what this check cannot see) |
| `npm run test -- --watch=false` (Node 24, via `nvm use 24` — required, see prior review's Finding D-2) | **PASS** — 710/710 tests, 144/144 files, 0 failures |
| `npm run build` (Node 24) | **PASS** — `ng build` succeeds; 2 pre-existing non-blocking budget warnings (initial bundle +30.8 kB, `users.page.scss` +1.79 kB), unchanged from the prior review |

**All commands ran to completion; none were skipped or estimated.** These results confirm the codebase is in the same green, non-regressed state as the prior reconciliation pass (identical test counts, identical static-gate numbers) — i.e., every finding in this report exists in code that is fully "passing" by every automated gate the project currently has. That is itself the report's central point: passing gates and a genuinely correct executive dashboard are not the same thing here.
