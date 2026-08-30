# WP-12 — Sales Targets & Commissions Engine (Daftra parity)
**Priority:** 🟡 · **Owner:** Backend dev D (sales) · **Depends on:** — · **Effort:** ~4 days
**Liquibase:** V-number from coordinator.
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Sales manager sets monthly revenue targets per salesperson/team/branch and pays commission % on achieved invoiced (or collected) amount. Doctor commissions (§14.13) become seeded rules of this engine later.

## Backend steps
1. Tables: `sales_targets` (id, app_id, scope REP|TEAM|BRANCH, target_ref_id, period 'YYYY-MM' string, metric REVENUE|QUANTITY, target_value >0, version, unique(scope,ref,period)) + `commission_rules` (id, app_id, name, basis INVOICE_TOTAL|COLLECTED, percent 0..100 NUMERIC(5,2), min_amount default 0, active, valid_from/to dates nullable).
2. Achievement endpoint `GET /api/v1/sales/targets/status?period=YYYY-MM`: per target — achieved value joined from existing sales invoice/receipt repos (COLLECTED basis joins payment receipts; REVENUE joins issued invoices net of returns where available).
3. Commission statement `GET /api/v1/sales/commissions?repId=&period=`: rule matching by basis + validity window + min threshold → computed amounts backend-side only.
4. Payout integration v1: expose statement + "send to payroll as bonus" action calling the payroll allowance injection used by other bonuses (no direct cash posting).
5. Codes: `TARGET_DUPLICATE/TARGET_INVALID_PERIOD/RULE_OVERLAP/COMMISSION_PERCENT_INVALID`.

## Frontend steps
1. Inside sales feature: Targets tab (grid + inline period filter + create dialog) with progress bars achieved/target (color+percent text); Commissions tab: rep selector + statement table + export xlsx + "send to payroll" confirm showing total.
2. Keys `sales.targets*` / `sales.commission*` (~16).

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Target 100k vs invoices totaling 70k renders 70% bar with both numbers; QUANTITY metric counts units not money.
- [x] **AC-2** Duplicate target for same scope+period rejected (`TARGET_DUPLICATE`, translated).
- [x] **AC-3** COLLECTED basis ignores uncollected invoice portions on a partial-payment fixture (ties to WP-01 data when both merged).
- [x] **AC-4** Two overlapping active rules for same basis are blocked at create; valid_from/to windows respected in statement math.
- [x] **AC-5** Statement export localized; "send to payroll" appears once per rep/period and is idempotent (second click disabled server-side). ※Implemented 2026-08-29: `GET /api/v1/sales/targets/commissions/export.xlsx` localized via `translateService` (ar-EG/en-US filename + 4 bilingual column headers); `POST /api/v1/sales/targets/commissions/send-to-payroll` idempotent via `SalesCommissionPayout` unique (app_id, rep_id, period) — replay returns `alreadySent=true`, never saves twice; UI button disabled once sent (V409/V410).
