# WP-23 — Pharmacy Dispensing + Narcotics Register
**Priority:** 🟢 · **Owner:** Backend dev C (inventory synergy) · **Depends on:** WP-15 · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.6

## Business goal
Dispense prescribed drugs against real pharmacy stock with batch/expiry safety, and keep the legally-required narcotics/controlled-substances register (dual sign-off, MOH-audit export).

## Current state
Inventory items already carry barcode, aliases, reorder point, shelf-life/dead-stock flags; operations movements are immutable signed evidence — reuse them; do NOT create a parallel stock ledger.

## Backend steps
1. Drug catalog table: `pharmacy_items` extending/mapping InventoryItem FK (form TABLET|SYRUP|INJECTION|…, strength_text, is_controlled bool, control_schedule enum nullable).
2. Dispense endpoint `POST /api/v1/clinic/prescriptions/{id}/dispense {lines:[{drugId, qty, batchId}]}`: validates stock via existing balance service, creates OUT stock movement per line (FEFO hint: earliest-expiry batch suggested server-side), partial dispense allowed with remaining counter on Rx line.
3. Narcotics register: every dispense of `is_controlled` writes `narcotics_register` row (drug, batch, qty, patient MRN, prescriber, dispenser, second_signer_id required — second approval endpoint before movement commits). Export endpoint produces MOH-format xlsx.
4. Expiry guard: dispensing expired batch hard-blocked (`BATCH_EXPIRED`); near-expiry (≤90d configurable) warns.
5. Codes: `PHARM_*` family (~8).

## Frontend steps
1. Dispense dialog from prescription: lines prefilled, batch picker showing expiry dates (expired disabled), FEFO suggestion preselected, remaining-qty tracker for partial fills.
2. Narcotics flow: after first signer submits, task appears to second signer (reuse approvals inbox); register page filterable + export button.
3. Keys `clinic.pharm*` / `clinic.narcotics*` (~16).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Dispensing decrements central inventory balances through signed movements (no new ledger) — reconciliation test vs balance service passes.
- [ ] AC-2 FEFO suggestion picks earliest-expiry in-stock batch; selecting expired batch impossible client AND server side.
- [ ] AC-3 Partial dispense of 5/10 leaves remaining=5 visible on Rx; completing later closes line.
- [ ] AC-4 Controlled dispense without second signer stays PENDING and moves no stock; after second approval exactly one movement exists; register export rows match movements 1:1.
- [ ] AC-5 Near-expiry warning text shows configured threshold; threshold property change reflected without redeploy.
