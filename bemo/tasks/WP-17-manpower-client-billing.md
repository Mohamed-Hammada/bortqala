# WP-17 — Manpower-Supply Client Billing (revenue side of workforce)
**Priority:** 🟢 · **Owner:** Backend dev D (workforce) · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §18 row 3

## Business goal
Manpower-supply companies deploy workers at CLIENT sites and bill clients monthly per worker/day, while paying wages via the existing contractor module (cost side done). Missing entirely: the client-facing revenue side + margin view.

## Backend steps
1. Rate table V-number: `client_worker_rates` (id, app_id, client_party_id FK, worker_category_id FK, day_rate >0, effective_from, effective_to NULL) — effective-dated exactly like ScheduleRule; overlap for same pair rejected.
2. `client_billing_periods` (client_party_id, period 'YYYY-MM', status OPEN|INVOICED, version): generation service collects per deployed worker the attendance-APPROVED days in period × resolved rate → draft invoice lines grouped by category.
3. Review endpoint returns draft lines with variance vs same client prior month (per line) before confirm; confirm → customer invoice via existing sales/invoicing posting (partner-ledger debit mirroring contractor settlement flow reversed).
4. Margin report endpoint: billed vs wage cost per client/period/worker (wage cost from settlement lines already posted).
5. Codes: `CLIENT_RATE_OVERLAP/MISSING_RATE/BILLING_PERIOD_EXISTS/PERIOD_NOT_OPEN`.

## Frontend steps
1. Workforce workspace new tab "فاتورة العملاء / Client billing": pick client+month → generate draft → review grid (worker, category, days, rate, amount, Δ vs last month) → confirm invoice; INVOICED periods read-only with link to invoice.
2. Margin report table + export.
3. Keys `workforce.clientBilling*` (~14); permission `workforce.clientBilling`; no new sidebar menu if placed as tab (protocol only if standalone).

## Acceptance Criteria (QA sign-off)
- [ ] **AC-1** 5 approved-days worker at rate 150 → line 750 on draft; unapproved days never billed (fixture proves decision states respected).
- [ ] **AC-2** Rate change mid-month: first 15 days old rate, rest new rate (effective-dating math test).
- [ ] **AC-3** Overlapping rate windows for same client+category rejected translated; missing rate blocks that worker's line with clear reason list, not silent zero.
- [ ] **AC-4** Confirm creates ONE customer invoice; ledger debit matches total; period flips INVOICED; regenerate blocked.
- [ ] **AC-5** Variance column explains ±Δ vs prior month per line; margin report ties billed − wage cost to manual spreadsheet fixture within 0.01.
