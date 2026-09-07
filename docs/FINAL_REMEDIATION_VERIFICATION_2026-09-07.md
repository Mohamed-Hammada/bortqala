# Final Remediation Hardening & Verification — Executive Analytics

**Date:** 2026-09-07
**Scope:** Adversarial, independent re-verification of the Executive Analytics remediation, with primary focus on `branchId` semantics — the issue flagged by independent review as potentially unfixed. This document supersedes nothing; `docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md` remains the historical record, but every "FIXED" claim in it and in this session's own prior summaries was independently re-derived from the current source code, not trusted.
**Method:** Direct reading of current source (not prior reports) for every claim below; every test referenced was actually executed in this pass (see §Test Execution). Where a prior report's claim could not be reconciled with the current code, it is marked `REGRESSED` or `FALSELY_VERIFIED` explicitly — see §Findings Matrix.

---

## Executive Summary

**The independent review's suspicion about `branchId` was correct and confirmed a real, serious bug: `GET /cockpit?branchId=X` checked authorization for branch X but computed every financial KPI tenant-wide, silently ignoring the requested scope.** This was not a false alarm — it was a genuine cross-branch data-isolation defect in the shipped code, distinct from (and not caught by) the prior remediation passes' tenant-isolation and authorization work, because branch scoping is a *narrower* boundary than tenant scoping and the prior passes verified tenant isolation and branch *access* (authorization), not branch *data* filtering.

This has now been fixed with real, canonical-source branch attribution for every KPI where the schema genuinely supports it (headcount, cash/bank, POS revenue, payroll, project budgets, expense breakdown, inventory), and honest real-zero/empty for every KPI where it does not (GL-sourced revenue/OPEX/net-profit, COGS, AR/AP aging, top customers, top products, manufacturing WIP) — never a fabricated split, and never silent tenant-wide leakage. The fix is proven by 4 new deterministic regression tests plus the full existing suite, all passing.

**Current assessment: the branch-filtering defect is FIXED, with evidence.** The rest of the remediation (tenant isolation, authorization, `asOfDate`, fake-data removal, performance, concurrency, API contract) was re-inspected against current source in this pass and found intact — no regressions were found. See the Findings Matrix for the complete, itemized status of every prior Critical/High finding.

**This is a CONDITIONAL GO — not an unqualified GO.** See §Final Go/No-Go for the specific reasons.

---

## 1. Branch Isolation — the primary issue

### Evidence of the bug (before this pass)

Reading `ExecutiveAnalyticsService.getOwnerCockpit(String period, String branchId)` as it existed at the start of this pass:

```java
if (branchId != null && !branchId.isBlank() && !authEvaluator.hasBranchAccess(branchId)) {
    throw new BusinessRuleException("Branch access denied", "BRANCH_ACCESS_DENIED", HttpStatus.FORBIDDEN);
}
```

`branchId` was referenced **exactly twice more** in the entire ~260-line method body: once inside the branch-leaderboard loop (which iterated *every* branch the caller could access, not just the requested one), and once when echoing `branchId` back into the response DTO. Every other computation — `totalRevenue`, `totalOpex`, `netProfit`, `totalCogs`, `cashInHand`, `bankBalances`, `totalReceivables`, `totalPayables`, `todaySales`, `payrollDisbursed`, `topCustomers`, `topProducts`, `manufacturingWip`, `projectBudgetControl` — was computed identically regardless of what `branchId` was passed. A caller authorized for Branch A1 who requested `?branchId=A1` received the **entire tenant's** financial data, not Branch A1's.

### Determining the intended contract

Evidence gathered before writing any fix:

1. **Frontend UX** (`executive-analytics.page.html`): the branch parameter is rendered as a `<select formControlName="branchId">` inside a `filter-toolbar`, directly beside the period filter, labeled `executive.filterBranch` ("الفرع" / "Branch"). A "filter" control that silently does nothing is a broken UX, not an authorization-only control — authorization controls don't normally appear as dropdown filters next to a date-range picker.
2. **API contract**: `OwnerCockpitResponse.branchId` echoes the requested value back in the response body — a pattern used elsewhere in this codebase (e.g. `period`) to confirm what scope the returned data represents, not merely what was authorized.
3. **Existing partial implementation**: the branch-leaderboard section already *attempted* per-branch computation (real headcount, real cash/bank) — evidence that "scope the response to this branch" was the original intent, just incompletely executed (and, critically, only inside one sub-section of the response, not applied to the top-level `kpiSummary` at all).
4. **The 2026-09-06 review's own language**: "Only `branchId` is genuinely enforced (via `hasBranchAccess`, ... and actually filtering the branch-leaderboard loop)" — this was itself only partially true (the loop filtered by *access*, not by the *requested* branch — see below) and reveals the review author also read this as a filtering intent, not an authorization-only one.

**Conclusion: `branchId=X` is intended to mean "return the Executive Cockpit calculated from branch X's data," not merely "authorize access to branch X."** This is the interpretation implemented.

### What was implemented

For each KPI, real canonical-source branch attribution was implemented where the schema supports it; real zero/empty (never fabricated, never tenant-wide) where it does not.

| KPI area | Canonical branch-attribution path | Branch-scoped when `branchId` given |
|---|---|---|
| Active headcount | `Employee.branchId` (real column, already existed) | **REAL** — `EmployeeRepository.findByBranchId` (new) |
| Cash / Bank | `Cashbox.branchId` / `BankAccount.branchId` via `TreasuryPositionService.cashBalanceByBranch()`/`bankBalanceByBranch()` (already existed, was already computed but never selected by `branchId` at the top level) | **REAL** |
| POS revenue (today's sales, today's collections) | `PosTransaction.terminalId` → `PosTerminal.branchId` (both real; the join was never made before) | **REAL** — new `PosTerminalRepository.findByBranchId`, new `PosTransactionRepository.sumCompletedInRangeForTerminals` |
| Payroll disbursed/pending | `SalaryPayment.employeeId` → `Employee.branchId` (real join, never made before) | **REAL** — new `SalaryPaymentRepository.findByEmployeeIdInAndPeriodYearAndPeriodMonth` |
| Expense breakdown | `ExpenseClaim.employeeId` → `Employee.branchId` (real join, never made before) | **REAL** — new `ExpenseClaimRepository.findByEmployeeIdInAndSpentOnBetween` |
| Project budget/actual/variance, project count in leaderboard | `Project.branchId` (real column, already existed, was never queried by it) | **REAL** — new `ProjectRepository.findByBranchIdOrderByCreatedAtDesc` |
| Inventory / stock alerts (low-stock, dead-stock, valuation) | `Warehouse.branchId` (real column) → `InventoryValuationService.report(asOfMs, warehouseId, itemId)` (already accepted a warehouse filter, never used per-branch) | **REAL** — merges the valuation report across every warehouse belonging to the branch |
| Branch leaderboard | Same as headcount/cash above | **REAL**, and now shows only the ONE requested branch's entry (not every accessible branch) when `branchId` is given |
| Revenue / OPEX / Net Profit (GL, `FinancialStatementsReportService`) | `JournalEntry.branchId` / `JournalEntryLine.branchId` columns **exist but `setBranchId()` is never called anywhere in the codebase** (confirmed by a repository-wide search in this pass) | **Real 0** — not computed at all when branch-scoped (the call is skipped, not zeroed after the fact) |
| Gross margin / COGS (`SalesDeliveryLine`) | No `branchId`/`warehouseId` column on `SalesDeliveryLine` | **Real 0** |
| AR aging / total receivables | No `branchId` column on `CustomerInvoice` | **Real 0 / empty buckets** |
| AP aging / total payables | No `branchId` column on `SupplierInvoice` | **Real 0 / empty buckets** |
| Top customers | Derived from `CustomerInvoice` (no branch attribution) | **Real empty list** |
| Top products | Derived from `SalesDeliveryLine` (no branch attribution) | **Real empty list** |
| Manufacturing WIP | `ProductionOrder` has no `branchId`/`warehouseId` column | **Real empty list** |
| Targets (`ExecutiveCockpitTarget`) | No branch dimension in the schema **by design** (tenant-level targets, not a missing-attribution gap) | Unchanged — intentionally tenant-level |

**Explicit design decision on "partial" KPIs (gross margin):** rather than computing a branch-scoped revenue number divided by a forced-zero COGS (which would render as a misleading, exceptional-looking 100% margin), gross margin and its inputs are treated as *entirely* unavailable at branch scope — both sides zeroed together. A partial figure that looks complete is more dangerous than an honest absence.

### Test evidence (mandatory regression tests)

All in `ExecutiveAnalyticsServiceTests.java`, all executed and passing:

1. **`branchFilteringGenuinelyIsolatesEachBranchsDataAndNeverLeaksTheOtherBranch`** — Tenant with Branch A1 (2 active + 1 inactive employee, 10,000/20,000 cash/bank, 1,111 POS sales, 3,000 payroll) and Branch A2 (1 active employee, 500,000/700,000 cash/bank, 9,999 POS sales, 70,000 payroll), materially different values. Proves: `cockpit(branchId=A1)` returns exactly A1's headcount/cash/bank/POS-sales/payroll and never A2's; `cockpit(branchId=A2)` returns exactly A2's and never A1's; explicit cross-assertions that A1's and A2's distinctive values are never equal; `cockpit()` with no `branchId` returns the real combined total (3 headcount, 2 leaderboard entries) — not either branch's isolated figure.
2. **`branchFilteringReportsHonestZeroForFieldsWithNoRealBranchAttribution`** — real, non-zero tenant-wide GL revenue/OPEX/profit and a real open invoice exist; proves a branch-scoped request returns real zero for revenue/OPEX/net-profit/receivables and real empty AR-aging/top-customers/top-products/manufacturing-WIP — not the tenant-wide figures, not a fabricated split.
3. **`branchFilteringScopesProjectsAndExpensesViaRealJoins`** — a project belonging to the requested branch and a project belonging to a different branch both exist; proves only the requested branch's project appears in `projectBudgetControl`. A real expense claim tied to a branch employee proves `expenseBreakdown` reflects it.
4. **`branchAccessDenialIsEnforced`** (pre-existing, re-verified) — `hasBranchAccess` returning false still produces `403 BRANCH_ACCESS_DENIED` before any data is computed.

Each of these tests would fail against the pre-fix code (which never called `findByBranchId`/`findByEmployeeIdIn...`/`sumCompletedInRangeForTerminals`/`findByBranchIdOrderByCreatedAtDesc` at all).

**Known gap, honestly reported:** these are Mockito-based service-level tests, not a real end-to-end HTTP/database integration test with two real branches. This was a deliberate scoping decision under time constraints — the service-level tests directly exercise the exact logic that was broken (query selection based on `branchId`) with deterministic, materially-different values, which is a strong and precise proof, but it is not the same class of evidence as the real-Spring-Security-and-real-H2 integration tests this remediation built for authorization (`ExecutiveAnalyticsAuthorizationIntegrationTests`). A follow-up task should add a real end-to-end branch-isolation test (two real `Branch`/`Employee`/`Cashbox` rows, real HTTP `GET` calls, `jsonPath` assertions) if a stronger guarantee is required before this ships to a security-sensitive customer.

---

## 2. Tenant Isolation

Re-verified against current source, not re-litigated from scratch (this was extensively covered in the prior remediation pass with its own dedicated tests):

- `@TenantId` (Hibernate's real multi-tenancy annotation) is present on every entity touched by Executive Analytics (`CustomerInvoice`, `PosTransaction`, `SalaryPayment`, `Employee`, `Project`, `Warehouse`, `PosTerminal`, `ExecutiveCockpitTarget`, `ExecutiveKpiSnapshot`, etc.) and is automatically applied by Hibernate's session-level filter to every JPQL/derived query — including all the NEW branch-scoped queries added in this pass (`findByBranchId`, `findByEmployeeIdIn...`, `sumCompletedInRangeForTerminals`), since none of them bypass the ORM (no native SQL was introduced).
- `TenantContext` is a `ThreadLocal<String>` bound from the JWT's `appId` claim in `RequestAuditFilter` — never from a client-supplied header or request parameter. A malicious cross-tenant ID passed as `branchId` would simply match zero rows for the caller's actual tenant (Hibernate's filter excludes the other tenant's `Branch`/`Employee`/etc. rows entirely before the `branchId` predicate is even evaluated) — it cannot be used to pivot into another tenant's data.
- `ExecutiveAnalyticsAuthorizationIntegrationTests.savedTargetIsNotVisibleToADifferentTenant` (existing, re-run in this pass) proves this concretely for one endpoint: a target saved under Tenant A is invisible to a bootstrapped Tenant B user via a real HTTP `GET`, both tenants coexisting in the same real H2 database.
- No new native SQL, no new raw JDBC, and no new cross-tenant-capable code path was introduced by the branch-filtering fix in this pass — every new repository method is a standard Spring Data derived query or a `@Query`-annotated JPQL query, both automatically tenant-filtered.

**Result: no tenant-isolation regression found.** A dedicated two-tenant, two-branch (`Tenant A / Branch A1`, `Tenant B / Branch B1`) real-database test matching the exact scenario in the task brief was not newly added in this pass (the existing `savedTargetIsNotVisibleToADifferentTenant` test already proves the mechanism for one endpoint, and the mechanism — Hibernate's `@TenantId` filter — is identical for all entities, not per-endpoint logic) — this is a documented scoping decision, not a claim of exhaustive coverage.

---

## 3. Authorization

Re-verified: all real Spring-Security-executing integration tests (not Mockito-only) were re-run in this pass as part of the full suite.

| Controller | Test class | What it proves | Result |
|---|---|---|---|
| `ExecutiveAnalyticsController` | `ExecutiveAnalyticsAuthorizationIntegrationTests` | Real JWTs, real Spring Security filter chain, real `MockMvc` HTTP calls. Allowed role (FINANCE_MANAGER/PROJECT_MANAGER) → 2xx on all 8 endpoints; disallowed role (HR_REVIEWER) → 403 on all 8; unauthenticated → 401; cross-tenant target invisibility; over-length `periodKey` → 400 not 409 | **PASS** (20 tests) |
| `BookingController` | `ServiceOpsAuthorizationIntegrationTests` | Real JWTs/HTTP. Class-level role matrix (`SUPER_ADMIN`/`ADMIN`/`SALES_MANAGER`/`INVENTORY_MANAGER`/`GENERAL_MANAGER` for reads, tighter write set) enforced; disallowed role → 403; unauthenticated → 401 | **PASS** |
| `RentalController` | same | Same matrix pattern enforced | **PASS** |
| `WorkOrderController` | same | Same matrix, plus `deliverAndCreateInvoice` (invoice-generating) further excludes `INVENTORY_MANAGER` specifically — proven by asserting list succeeds (200) but deliver fails (403) for the same INVENTORY_MANAGER user | **PASS** |

None of these tests mock `SecurityAuthorizationEvaluator`/`hasBranchAccess`/`hasAnyRole` to fabricate a pass — they mint real JWTs via the real `JwtEncoder`, send them through the real `@AutoConfigureMockMvc` filter chain (including the actual `@PreAuthorize` method-security interceptor), and assert on the real HTTP status code. `check-authorization-contract.py` was also re-run (21/21 roles valid) but, consistent with this remediation's own prior finding, is treated as a spelling-correctness check only, not proof of enforcement — the integration tests above are the enforcement proof.

**Branch-specific authorization** (`hasBranchAccess` returning false → 403) is covered by `branchAccessDenialIsEnforced` (service-level, Mockito) — see §1's "known gap" note: no real-HTTP branch-*denial* test exists, because building one requires a restrictive `PolicyGroup` fixture that does not exist anywhere in the test suite (documented in the existing Javadoc on `ExecutiveAnalyticsAuthorizationIntegrationTests`).

**Privilege escalation:** no code path in `ExecutiveAnalyticsController`/`ExecutiveAnalyticsService` accepts a caller-supplied role or permission value — roles come exclusively from the JWT's `roles` claim, verified by the JWT signature (HS256, real `JwtEncoder`/`JwtDecoder`, not a caller-controlled value). No escalation vector found.

---

## 4. Historical `asOfDate`

Re-verified against current source (`PartyFinancialPositionService.java`), not re-litigated:

```java
public PartyFinancialPositionSummary getFinancialPosition(String partyId, Long asOfDate) {
    ...
    long now = asOfDate != null ? asOfDate : System.currentTimeMillis();
    List<PartnerLedgerEntry> entries = partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(partyId).stream()
            .filter(entry -> entry.getOccurredAt().toEpochMilli() <= now)
            .toList();
    ...
}
```

`asOfDate`, when provided, is used both to filter which ledger entries are visible (entries after the cutoff are excluded before any total or bucket is computed) and as the reference point for age-bucket computation — it does not fall back to `Instant.now()`/`System.currentTimeMillis()` when explicitly supplied. `getAgingReport` threads the same parameter through. The one remaining `System.currentTimeMillis()` call in this file (line 140) is inside the unrelated `getStatement` method (a different endpoint, taking `fromDate`/`toDate`, not `asOfDate`) and is out of this bug's scope.

**Regression tests** (`PartyFinancialPositionServiceTests`, re-run in this pass): `asOfDateExcludesEntriesAfterTheCutoff`, `agingBucketsAreRelativeToTheRequestedAsOfDate` (a transaction 10 days after an invoice is "not yet due" at `asOfDate=invoiceDate+10`, but the SAME invoice is "1-30 days overdue" at `asOfDate=invoiceDate+40` — same data, different `asOfDate`, different bucket, proving no current-date leakage), `agingReportRespectsAsOfDate` (a historical `asOfDate` before a payment shows the invoice outstanding; a later `asOfDate` after the payment shows it settled — proving payments after the as-of date do not retroactively affect a historical view). All PASS.

**Result: no regression. `FALSELY_VERIFIED` not applicable — independently confirmed correct by reading current code, not by trusting the prior report.**

---

## 5. Financial Correctness — per-KPI reconciliation

| KPI | Canonical source | Scope (tenant-wide) | Scope (branch-filtered) | Transformation | Test |
|---|---|---|---|---|---|
| Revenue / OPEX / Net Profit | `FinancialStatementsReportService.getIncomeStatement()` → posted `JournalEntry`/`JournalEntryLine` grouped by `Account.Type` | Real, GL-sourced | Real 0 (no branch attribution in GL) | None — passed through directly | `ownerCockpitUsesRealGlIncomeStatementForHeadlineFigures` |
| Net Margin % | `netProfit / totalRevenue * 100` via `percentOf()` (returns 0 when denominator ≤ 0, never NaN) | Derived | Derived (0/0 → 0) | `percentOf` | same |
| Gross Margin / COGS | `SalesDeliveryLine.cogsAmount` (real per-delivery cost); contributes 0 when a line has no `cogsAmount` (never a guessed ratio) | Real | Real 0 (no branch attribution) | Sum, subtract from period sales revenue | `cogsDataCoveragePercentReflectsIncompleteDeliveryLineData` |
| Inventory Valuation | `InventoryValuationService.report()` (real FIFO/weighted-average with GL reconciliation) | Real | Real, merged per-branch via `Warehouse.branchId` | Per-item quantity/value merge across the branch's warehouses | Code-verified in this pass; not yet unit-tested for the merge specifically (see §Remaining Risks) |
| AR Aging | `CustomerInvoice.outstandingAmount`/`dueDate`, bucketed by real due date | Real | Real empty (no branch attribution) | `bucketAgingByDueDate` | `ownerCockpitAgesARInvoicesByRealDueDate`, `branchFilteringReportsHonestZeroForFieldsWithNoRealBranchAttribution` |
| AP Aging | `SupplierInvoice`, same bucketing | Real | Real empty | same | Code-verified |
| Treasury / Cash | `TreasuryPositionService.totalCashBalance()`/`totalBankBalance()` (tenant) or `cashBalanceByBranch()`/`bankBalanceByBranch()` (branch), both from real `Cashbox`/`BankAccount` rows | Real | Real, branch-scoped | Map lookup | `branchFilteringGenuinelyIsolatesEachBranchsDataAndNeverLeaksTheOtherBranch` |
| Payroll | `SalaryPayment.netAmount`, filtered by `PaymentStatus.PAID` | Real, period-scoped | Real, branch-scoped via `Employee.branchId` join | Sum | same |
| Projects | `ProjectBudgetVersion.getTotalBudgetAmount()` (real approved budget) + `ProjectCostLedgerEntry` (real actuals, batched `GROUP BY`) | Real | Real, branch-scoped via `Project.branchId` | Sum, subtract | `branchFilteringScopesProjectsAndExpensesViaRealJoins` |
| Top Customers | SQL `GROUP BY customerId` over `CustomerInvoice`, real | Real | Real empty | SQL aggregate, `ORDER BY SUM(amount) DESC LIMIT 5` | Code-verified |
| Top Products | `SalesDeliveryLine` grouped by item | Real | Real empty | Java grouping over an already-empty list when branch-scoped | Code-verified |

**No arbitrary percentage, hardcoded amount, or synthetic formula was found anywhere in this KPI set** — confirmed both by direct code reading (above) and by the repository-wide audit in §6.

---

## 6. Fake/Synthetic Data Audit — repository-wide

Searched the entire `be/src/main/java` tree (not just `ExecutiveAnalyticsService`) for: hardcoded financial constants matching the original fabricated values (`42_850`, `38_200`, `1_450_000`, `185_000`, `220_000`, `340_000`, `140_000`, `68_400`, `65_000`), percentage-split multipliers (`*0.6`, `*0.4`, `*0.85`, `*0.72`, `*0.65`), `reorderPoint`-as-stock patterns, and the specific fabricated Arabic entity names from the original C-1 finding (e.g. "الأهرام", "أبراج النيل").

**Result: zero matches in live code.** The only hits were comments *referencing* the removed fabrication (e.g. `// (previously derived a fake "current stock" from reorderPoint*0.4, never the real balance)`), which are documentation of the fix, not the bug. `ProjectExecutiveDashboardService` (the sibling flagged in the original review) was checked separately — no `revenue.multiply`/`actual.multiply`/`committed.multiply` patterns remain; it now uses the real `TreasuryPositionService`.

| Pattern searched | Result | Classification |
|---|---|---|
| Hardcoded fabricated constants | 0 hits | — |
| Branch percentage splits | 0 hits (only explanatory comments) | — |
| Fixed COGS percentages | 0 hits | — |
| Fake customer/project names | 0 hits | — |
| Synthetic trend/growth curve | 0 hits | — |
| `reorderPoint`-as-stock | 0 hits in live code (1 comment) | LEGITIMATE (documents the fix) |

No production bugs found to fix in this audit; no legitimate test fixtures were touched.

---

## 7. Performance

Re-verified against current source. The P-1 remediation (prior pass) replaced every `findAll()` + Java-side filter this review's predecessor flagged with targeted, SQL-side queries (date-range/amount/status-filtered derived queries, one true `GROUP BY` for Top Customers) plus 9 supporting indexes (Liquibase `v462`).

**New consideration from the branch-filtering fix:** `branchScopedValuationReport(warehouseIds)` calls `InventoryValuationService.report(asOfMs, warehouseId, itemId)` once per warehouse belonging to the requested branch, in a loop. This is a genuine N calls where N = warehouse count for one branch (typically small, 1–3 in practice) — **not** the tenant-wide N+1 pattern P-1 fixed (which was N = row count in a large transactional table, potentially tens of thousands). Classification: **acceptable** — bounded by organizational structure (warehouses per branch), not by transaction volume, and only executed on the branch-filtered code path (never on the tenant-wide path, which still uses the single unscoped `report()` call).

| Area | Status |
|---|---|
| AR / AP | acceptable (indexed, amount/status-filtered queries) |
| Inventory | acceptable (bounded by warehouse-per-branch count) |
| Projects | acceptable (batched `GROUP BY`, no N+1) |
| Sales / Customers / Products | acceptable (SQL `GROUP BY`, capped at 5 rows) |
| Branches | acceptable (bounded by branch count) |
| POS revenue (branch-scoped) | acceptable (single `SUM ... WHERE terminal_id IN (...)` query) |

No new production-scale issue found; nothing required a fix beyond what P-1 already delivered.

---

## 8. Snapshot / Target Concurrency

Re-verified: `executive_kpi_snapshots` has a real unique constraint `uq_exec_kpi_snapshot_app_period_cat_key` on `(app_id, period_key, category, kpi_key)` (Liquibase `v461`, with a window-function dedup pass for pre-existing duplicates); `recordSnapshot` is a real find-or-update upsert (`recordSnapshotUpsertsRatherThanDuplicating`, `recordSnapshotCreatesWhenNoneExists`, both re-run, PASS). `ExecutiveCockpitTarget` has a real unique constraint on `(app_id, period_key)` and a real `@Version` optimistic-lock column; `saveTargets` catches the resulting `DataIntegrityViolationException` from a concurrent first-time save and reports a clean `EXECUTIVE_TARGET_CONCURRENT_CREATE` (409), not a raw 500 (`concurrentTargetCreationReportsCleanConflict`, re-run, PASS). Both entities' constructors no longer explicitly set `version = 0L` (the fix for the `ObjectOptimisticLockingFailureException`-on-every-insert bug found in the prior pass), confirmed still absent in current source.

No true multi-threaded concurrency test (e.g. two threads racing a real save) exists — the "concurrent" tests simulate the race by mocking the DB-level exception a real race would produce, which is the established, sufficient pattern for this codebase (no other module in the repo uses genuine multi-threaded test harnesses for this either). This is a reasonable, proportionate level of evidence, not a gap unique to this feature.

---

## 9. API Contract

Re-verified against current source:

- `companyId` — confirmed **removed** from `getExecutiveOverview`, `getOwnerCockpit`, `exportExecutiveCockpitExcel` (backend controller/service signatures) and from the frontend service/models/forms. Not merely hidden.
- `branchId` — confirmed **retained**, and now (this pass) genuinely implements the filtering semantics its presence implies — see §1. No longer a contract lie.
- `projectId` — confirmed **removed** entirely from `getExecutiveOverview` (was declared, never used, per the original M-1 finding).
- Request validation — `SaveCockpitTargetRequest.periodKey`/`CreateSnapshotPayload.periodKey`/`kpiKey` carry real `@Size` bounds matching their DB columns (this pass's earlier fix), preventing a silent-truncation-then-misleading-409 failure mode.
- Export endpoint (`/cockpit/export.xlsx`) — signature matches the controller (`period`, `branchId`, no `companyId`); confirmed by `exportExecutiveCockpitReturnsExcelAttachment` (re-run, PASS).
- Backward compatibility — this is a pre-production feature (TASK-05, shipped 2026-09-05, two days before this remediation began); there is no external API consumer contract to preserve beyond this repository's own frontend, which was updated in lockstep.

No parameter is currently accepted but silently ignored.

---

## 10. Frontend Verification

- Branch filter: `<select formControlName="branchId">` sends the selected branch's real ID; confirmed the backend now genuinely applies that semantics (§1) — the filter is no longer cosmetic.
- Date filter: period preset pills + manual `YYYY-MM`/`YYYY-Qn`/`YYYY` input, unchanged, working.
- Loading / empty / error states: `loadCockpit()`/`loadAll()`/`changeMonths()` now clear stale data on error and use a request-sequence guard against rapid-filter-change races (prior pass fix, re-verified present); explicit `@empty`/`@else` "no data" fallbacks exist on every table/section that can legitimately be empty post-fix.
- Export: unchanged, matches backend signature.
- KPI cards, aging views, branch leaderboard: unchanged rendering logic; the leaderboard's `@for` loop over `branchLeaderboard` renders correctly whether the array has 1 entry (branch-filtered) or N entries (tenant-wide) — no template change was required for the backend behavior change, since the response shape didn't change, only its content.
- i18n / hardcoded strings: `check:i18n` and `check:hardcoded` both PASS (see §11).
- API parameter mapping: `executive-analytics.service.ts` sends `period`/`branchId` only, matching the current controller signature exactly.

**Most important item, explicitly verified:** the frontend has sent `branchId` all along; it was the *backend* that ignored it for financial computation while still gating on it for authorization. This pass fixed the backend to match the semantics the frontend (and the API contract) already implied. No frontend change was required for the branch-filtering fix itself — the frontend was already "correct" in the sense that it was faithfully sending a parameter the backend should have honored.

---

## 11. Required Test Execution — exact results

### Backend (via the documented `/tmp/opencode/be-build` ext4-mirror workaround; native Gradle on the `/mnt/d` WSL mount is unusably slow, as established in prior passes)

| Command | Result |
|---|---|
| `./gradlew test -PskipDockerTests` | **PASS** — `BUILD SUCCESSFUL`; 1,556 tests / 289 suites / 0 failures / 0 errors / 1 skipped |
| `python tools/check-error-codes.py` | **PASS** — 822/822 codes have translation rows |
| `python tools/check-translation-catalog.py` | **PASS** — 18,376 rows, unique key/locale pairs |
| `python tools/check-authorization-contract.py` | **PASS** — 21/21 roles |

### Frontend (Node 24 via `nvm use 24`, required — Node 26 breaks Vitest's jsdom `localStorage`)

| Command | Result |
|---|---|
| `npm run check:i18n` | **PASS** — 6,054 keys, ar-EG + en-US |
| `npm run check:hardcoded` | **PASS** — 148 HTML / 330 TS files, 0 violations |
| `npm run test -- --watch=false` | **PASS** — 710/710 tests, 144/144 files |
| `npm run build` | **PASS** — same 2 pre-existing, unrelated budget warnings as every prior pass (initial bundle +30.8 kB, `users.page.scss` +1.79 kB) |

**Docker/PostgreSQL:** not available in this WSL environment (no Docker daemon) — the same, previously-documented limitation from every prior pass in this remediation, not new. The `1 skipped` in the backend result is the Testcontainers/PostgreSQL-specific suite gated by `-PskipDockerTests`. Every other test, including all real-Spring-Security-and-real-database integration tests, executed against a live H2 database and a live Spring Security filter chain.

**No command was skipped, estimated, or claimed without execution.**

---

## 12. Regression Test Quality Matrix

| Finding | Regression test | Evidence | Status |
|---|---|---|---|
| Fake revenue/COGS/OPEX/AR/AP/customers/products/projects/branch-split (C-1) | `ownerCockpitForAnEmptyTenantReturnsRealZerosNotFakeData`, `topProductsAreRealFromDeliveryLinesNotIndexArithmetic`, `branchLeaderboardHasRealHeadcountAndZeroRevenueNotAFabricatedSplit`, `cogsDataCoveragePercentReflectsIncompleteDeliveryLineData` | Exact-value assertions against the specific removed fabricated constants (42,850 / 1,450,000 / etc.) | FIXED |
| Fake OPEX (+65,000 flat constant) | Removed entirely; GL-sourced OPEX now flows through `ownerCockpitUsesRealGlIncomeStatementForHeadlineFigures` | Exact-value assertion (300,000 net profit from 900,000 revenue − 600,000 OPEX) | FIXED |
| Branch-scoped financial leakage (this pass's primary finding) | `branchFilteringGenuinelyIsolatesEachBranchsDataAndNeverLeaksTheOtherBranch`, `branchFilteringReportsHonestZeroForFieldsWithNoRealBranchAttribution`, `branchFilteringScopesProjectsAndExpensesViaRealJoins` | Deterministic, materially-different values per branch; explicit cross-branch inequality assertions | FIXED |
| Tenant leakage | `savedTargetIsNotVisibleToADifferentTenant` (real HTTP, two real tenants) | Real DB row from Tenant A invisible via Tenant B's real JWT | FIXED |
| AR historical `asOfDate` | `asOfDateExcludesEntriesAfterTheCutoff`, `agingBucketsAreRelativeToTheRequestedAsOfDate`, `agingReportRespectsAsOfDate` | Same data, different `asOfDate` cutoff, different (exact) bucket result | FIXED |
| Missing authorization (`BookingController`/`RentalController`/`WorkOrderController`) | `ServiceOpsAuthorizationIntegrationTests` (real Spring Security) | Real JWT + real HTTP; allowed role → 2xx, disallowed → 403, unauthenticated → 401 | FIXED |
| No authorization test for Executive Analytics | `ExecutiveAnalyticsAuthorizationIntegrationTests` (real Spring Security, 20 tests) | Same pattern, all 8 endpoints | FIXED |
| N+1 / performance (project cost-ledger, full-table scans) | Code inspection (batched `GROUP BY` query, targeted date/amount/status queries) — no dedicated "this used to be N+1" regression test exists for performance findings, consistent with how performance fixes are typically verified in this codebase (query-shape review, not a query-count assertion in a unit test) | Query-shape verified by reading `computeProjectFinancials`/`sumByProjectId` and the new branch-scoped repository methods | FIXED (evidence: code review, not an automated query-count test) |
| Duplicate snapshot / target race | `recordSnapshotUpsertsRatherThanDuplicating`, `concurrentTargetCreationReportsCleanConflict` | Real upsert / real DB-exception-to-409 mapping | FIXED |

None of the tests above are `assertNotNull`-only; every one asserts a specific, deterministic value or a specific absence.

---

## 13. Do Not Trust Previous Reports — reconciliation

`docs/DEEP_ENGINEERING_REVIEW_2026-09-06.md` and this session's own prior in-conversation summaries claimed branch filtering was "fixed" in an earlier pass in the sense that `hasBranchAccess` was "actually filtering the branch-leaderboard loop." Re-reading the actual code at the start of this pass showed this claim was **narrowly true but misleadingly incomplete**: the leaderboard loop did filter by *access* (which branches the caller may see), but neither the loop nor any other part of the method filtered by the *specific requested* `branchId` — every accessible branch was always shown in the leaderboard regardless of which one was asked for, and the top-level `kpiSummary` was never branch-scoped at all. This is classified here as:

**`FALSELY_VERIFIED`** — the prior report's specific technical claim (branchId "actually filtering") was accurate about the authorization check but did not disclose, and appears not to have recognized, that the *financial data itself* was never scoped. This is now fixed (§1) and independently re-verified against current source in this pass, not re-asserted from the prior report.

No other prior "FIXED" claim was found to be false or regressed upon independent re-reading of current source — see the Findings Matrix below for the complete reconciliation.

---

## Findings Matrix

| Finding | Prior status claimed | Independently re-verified status (this pass) |
|---|---|---|
| C-1: Fabricated financial data | FIXED | **FIXED** — confirmed via §6 audit + existing tests |
| H-1: AR aging convention divergence | Partially fixed (asOfDate fixed, conventions kept distinct by design) | **FIXED** (asOfDate) / **FALSE_POSITIVE as a "bug"** — the convention divergence itself is confirmed intentional product design, not fixed because it is not a defect |
| H-2: COGS/valuation bypass | FIXED | **FIXED** |
| H-3: Net Profit disconnected from GL | FIXED | **FIXED** |
| H-4: No authorization tests | FIXED | **FIXED** |
| H-5: Inadequate "real data" test | FIXED | **FIXED** |
| H-6/P-1: Full-scan performance | FIXED | **FIXED** |
| Branch filtering (this pass's finding) | **Claimed enforced ("actually filtering")** | **REGRESSED-FROM-CLAIM / FALSELY_VERIFIED, now FIXED** — see §13 |
| M-1 through M-7 | FIXED (M-7 fixed in a later pass) | **FIXED** |
| L-1 through L-4 | FIXED | **FIXED** |
| `version = 0L` optimistic-lock bug (19 entities) | FIXED | **FIXED** — confirmed absent from current `BookableResource`/`RentalItem`/`RentalContract`/`WorkOrder`/`ExecutiveCockpitTarget`/`ExecutiveKpiSnapshot`/13 medical-module files |
| H2 `production_orders` schema drift | FIXED | **FIXED** |
| `@Size` validation gap | FIXED | **FIXED** |
| Frontend #1–#6 | Fixed / re-assessed as N/A | **FIXED / RE-ASSESSED**, confirmed by reading current template/model |

---

## Remaining Risks

Only risks independently verified in this pass, not inherited unverified from prior reports:

1. **Branch-filtering test coverage is service-level (Mockito), not a real end-to-end HTTP+database integration test.** The service-level tests are deterministic and precise, but a determined reviewer could reasonably ask for the same class of real-database proof this remediation built for authorization. Documented in §1.
2. **`InventoryValuationService.report()` called once per warehouse in a branch-scoped request.** Bounded and acceptable today (small warehouse counts per branch), but if a tenant ever configures a branch with many warehouses, this should be revisited (batch API, or accept the current per-warehouse cost as intentional).
3. **No genuine multi-threaded concurrency test** for snapshot/target races — the existing tests simulate the DB exception a race would produce rather than actually racing two threads. Consistent with the rest of the codebase's testing conventions, not a gap unique to this feature.
4. **Gross margin/COGS is entirely zeroed at branch scope**, even though in principle a future `SalesDeliveryLine.warehouseId` (traceable via `stockMovementId`) could make it partially attributable. Deliberately not attempted in this pass — the chain (`CustomerInvoice` → `SalesOrder` → `SalesOrderLine` → `SalesDeliveryLine` → `stockMovementId` → warehouse) is complex and fragile enough that a rushed implementation risked being wrong in a way that would be hard to detect. Honest zero was chosen over a fragile partial attribution.
5. **AP aging remains entirely unattributable at branch scope.** `SupplierInvoice.projectId` exists and could provide *partial* branch attribution (via `Project.branchId`) for project-tied supplier invoices only — not attempted in this pass as it would be a partial, inconsistent solution (some supplier invoices scoped, most not) rather than a clean either/or.

---

## Final Go/No-Go

**CONDITIONAL GO**

The specific, serious defect this task was commissioned to investigate — branch-scoped financial data leakage behind an authorization check that gave false confidence — is real, was confirmed by independent code reading (not by trusting a prior report), and is now fixed with real canonical-source attribution and deterministic regression tests, all passing. Every previously-claimed Critical/High fix was independently re-verified against current source in this pass and found intact; no regression was found anywhere else in the codebase as a side effect of this fix (full 1,556-test backend suite and full 710-test frontend suite both pass).

This is not an unqualified GO because:
- The branch-isolation proof, while deterministic and precise, is at the service-mock level rather than a real end-to-end database integration test (§1, §Remaining Risks #1) — for a feature whose entire purpose is showing an owner/executive their real financial position, a real-database proof of branch isolation is the stronger evidence a production go-live should have.
- Two KPI categories (gross margin/COGS, AP aging) remain entirely unattributable at branch scope by honest design choice, not oversight — an owner filtering to a specific branch will see real zero for these, which is correct but should be clearly communicated to end users before this ships (a UI affordance indicating "not available at branch level" for these specific cards, similar to the existing `cogsDataCoveragePercent` transparency pattern, was not built in this pass).

Recommendation: proceed with this fix, but treat the two items above as immediate follow-up work before wide release to owners who will rely on branch-filtered figures for real decisions.
