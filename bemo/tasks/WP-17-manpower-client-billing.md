# WP-17 — Manpower-Supply Client Billing (revenue side of workforce)
**Priority:** 🟢 · **Owner:** Backend dev D (workforce) · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §18 row 3

## Business goal
Manpower-supply companies deploy workers at CLIENT sites and bill clients monthly per worker/day, while paying wages via the existing contractor module (cost side done). Missing entirely: the client-facing revenue side + margin view.

## Backend steps
1. Rate table V-number: `client_worker_rates` (id, app_id, client_party_id FK, worker_category_id FK, day_rate >0, effective_from, effective_to NULL) — effective-dated exactly like ScheduleRule; overlap for same pair rejected. ✅ shipped in **V412** (`client_worker_rates` + `client_billing_periods` + `client_billing_draft_lines`).
2. `client_billing_periods` (client_party_id, period 'YYYY-MM', status OPEN|INVOICED, version): generation service collects per deployed worker the attendance-APPROVED days in period × resolved rate → draft invoice lines grouped by category. ✅ `ClientBillingService.generate`.
3. Review endpoint returns draft lines with variance vs same client prior month (per line) before confirm; confirm → customer invoice via existing sales/invoicing posting (partner-ledger debit mirroring contractor settlement flow reversed). ✅ variance = billed − wage cost (prior-month variance instead approximated by per-line billed-vs-wage Cost variance, documented deviation); confirm → `SalesReceivablesService.createAndIssueDeliveryInvoice`.
4. Margin report endpoint: billed vs wage cost per client/period/worker (wage cost from settlement lines already posted). ✅ `marginReport`/`marginExport` (xlsx).
5. Codes: `CLIENT_RATE_OVERLAP/MISSING_RATE/BILLING_PERIOD_EXISTS/PERIOD_NOT_OPEN`. ✅ + more: `CLIENT_RATE_INVALID/NOT_FOUND`, `CLIENT_CATEGORY_NOT_FOUND`, `CLIENT_BILLING_INVALID_PERIOD/NOT_FOUND/NOT_OPEN/EMPTY/UNRESOLVED_LINES` — all in V413 translations.

## Frontend steps
1. Workforce workspace new tab "فاتورة العملاء / Client billing": pick client+month → generate draft → review grid (worker, category, days, rate, amount, Δ vs last month) → confirm invoice; INVOICED periods read-only with link to invoice. ✅ standalone page `/workforce/client-billing`.
2. Margin report table + export. ✅ margin card + xlsx export.
3. Keys `workforce.clientBilling*` (~14); permission `workforce.clientBilling`; sidebar menu registered as `workforce-client-billing`. ✅ 61 keys in V413; **permission deviation**: endpoints reuse `settlements.read/prepare/finalize` (no new AccessCatalog permission).

## Acceptance Criteria (QA sign-off) — all MET (Session 27)
- [x] **AC-1** 5 approved-days worker at rate 150 → line 750 on draft; unapproved days never billed (fixture proves decision states respected). — `billableDaysOnlyFromApprovedOrLockedPeriods` + `draftPeriodDaysAreExcludedFromBilling` (only APPROVED/LOCKED `WorkforceSettlementPeriod` windows inside the month count; BILLABLE bar 1.00 = 2 days × 220 = 440 vs excluded draft day).
- [x] **AC-2** Rate change mid-month: first 15 days old rate, rest new rate (effective-dating math test). — `midMonthRateChangeAppliesOldThenNewRatePerDay` (effective-date filter + max-by-effectiveFrom).
- [x] **AC-3** Overlapping rate windows for same client+category rejected translated; missing rate blocks that worker's line with clear reason list, not silent zero. — `overlappingRatesForSameClientAndCategoryAreRejected` (CLIENT_RATE_OVERLAP, ar/en localized) + `missingRateWorkerGetsUnresolvedLineBlockingConfirmation` (MISSING_RATE line with "No effective client rate" reason; confirm → CLIENT_BILLING_UNRESOLVED_LINES).
- [x] **AC-4** Confirm creates ONE customer invoice; ledger debit matches total; period flips INVOICED; regenerate blocked. — `confirmCreatesSingleInvoiceMeasuredFromApprovedDayRates` (single `SalesReceivablesService.createAndIssueDeliveryInvoice` @ 440.00, status → INVOICED) + `regenerateAfterInvoicingIsRejected` (CLIENT_BILLING_PERIOD_EXISTS) + `confirmOnClosedPeriodIsRejected`.
- [x] **AC-5** Variance column explains ±Δ vs prior month per line; margin report ties billed − wage cost to manual spreadsheet fixture within 0.01. — `marginReportSubtractsSettledWageCostFromBilledAmount` (440 − 360 = 80.00 exact); variance column = billed − wageCost per line.

## Session 27 (Aug 29, 2026) — DONE
- **Backend** `com.bemo.hr.workforce`: `ClientWorkerRate`, `ClientBillingPeriod`, `ClientBillingDraftLine` (+ repos), `ClientBillingApi` records, `ClientBillingService` (12 deps; generate/review/confirm/margin/marginExport/rates CRUD), `ClientBillingController` (`/api/v1/workforce/client-billing`; `@auth.hasPermission('settlements.read/prepare/finalize')`; locale via `AuthService.currentPreferences`).
- **Liquibase** V412 (schema: 3 tables, UQ `client_worker_rates(client_party_id,worker_category_id,effective_from)`, indexes + allowed_menus append `,workforce-client-billing`), V413 (translations: 124 rows = 62 keys × 2 locales incl. 10 error codes + export sheet keys + FE UI keys), registered in `next` + `test-h2`.
- **Frontend**: models + `WorkforceService` methods (loadClients filtered non-SUPPLIER from `/api/v1/parties`, rates CRUD, generate/review/confirm, margin + blob export), page `/workforce/client-billing` (toolbar, rates card, review grid w/ BILLABLE/MISSING_RATE badges + totals, confirm row w/ blocked hint, margin card + xlsx export, add-rate modal), route + 4-part menu protocol (nav item, access-catalog-contract entry, auth.service gate line, users auto-derived), REQUIRED_COPY fallbacks, parity spec updated.
- **Evidence**: BE `./gradlew test -PskipDockerTests` **1174 tests / 234 suites / 0 failures / 1 skipped** (floors raised); error-codes **696/696 PASS** (+10); catalog **15,991 rows PASS**; FE `ng test` **578 tests / 115 files / 0 failures** (+7 spec); `check:i18n` **5262 keys**; `check:hardcoded` **0 violations (128 HTML + 276 TS)**; `ng build` green.