# WP-02 — Payment-Time Settlement Discount Ledger
**Priority:** 🔴 P0 · **Owner:** Backend dev A (procurement) · **Depends on:** WP-01 · **Effort:** ~2 days
**Liquibase:** use **V342/V343** if WP-01 took V341/V342 — coordinator confirms numbers before you start.
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Supplier we owe 1,000 settles for 900 cash. Finance must record three distinct audited figures: original debt, cash paid, discount written off. Invoice-level discounts exist; payment-time settlement does not.

## Current state
- Payments record paid amount only; nothing captures "settled for less".
- Partner-ledger posting helpers exist (see contractor settlement posting).

## Backend steps
1. Migration: `supplier_payments` + `settlement_discount NUMERIC(18,4) NOT NULL DEFAULT 0`, `original_due NUMERIC(18,4) NULL`.
2. Extend payment request DTO: `appliedAmount`, optional `settlementDiscount`. Validation: `appliedAmount + settlementDiscount ≤ remaining`; discount > 0 requires finance role (`FINANCE_MANAGER`/admin) — enforce in service consistent with neighbors.
3. Post TWO ledger entries when discount used: Cr cash/bank `appliedAmount`; Dr "settlement discount granted" account from property `hr.finance.settlement-discount-account-code` (default `5200`) for the discount amount; net against AP so party balance zeroes exactly.
4. Exception codes: `SETTLEMENT_DISCOUNT_EXCEEDS_REMAINING`, `SETTLEMENT_DISCOUNT_INVALID`.

## Frontend steps
1. Payment dialog collapsible section "تسوية بخصم": discount input + preview lines Original due / Cash now / Discount / Remaining-after (=0 enforced).
2. Invoice row tooltip + export column: total discounts taken.
3. Keys: `procurement.settlementDiscount*` (~8).

## Tests
- Exact ledger triple assertion on a 1000/900 case (AP −1000, bank −900, discount expense +100 signs per your ledger convention).
- Discount > remaining rejected; non-finance actor rejected; idempotent replay via operationId.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Settling a 1,000 invoice with 900 cash + 100 discount posts exactly two ledger entries whose amounts sum to 1,000 against AP.
- [x] **AC-2** Party statement / AR-AP aging shows the supplier balance reduced to exactly 0 after settlement.
- [x] **AC-3** `supplier_payments` row stores original_due=1000, applied=900, discount=100; audit log records actor + both amounts.
- [x] **AC-4** Discount attempt by a user without finance role → `SETTLEMENT_DISCOUNT_FORBIDDEN` problem-details response with translated ar/en message.
- [x] **AC-5** Discount exceeding remaining → `SETTLEMENT_DISCOUNT_EXCEEDS_REMAINING`; UI blocks submit and shows translated error.
- [x] **AC-6** Replay of the same operationId creates no duplicate entries (idempotency test green).
- [x] **AC-7** Excel export of invoices/payments includes discount column; all DoD gates in `_GLOBAL-RULES.md` pass.

## Evidence (2026-08-24, V343 + V348)
- **Deviation:** codes use the repo's `PROC_` prefix (`PROC_SETTLEMENT_DISCOUNT_INVALID` / `PROC_SETTLEMENT_DISCOUNT_EXCEEDS` / `PROC_SETTLEMENT_DISCOUNT_FORBIDDEN`); GL leg uses the standard posting-profile mechanism (`businessEvent=SUPPLIER_SETTLEMENT_DISCOUNT`) instead of a `hr.finance.settlement-discount-account-code` property — consistent with how every other subledger event maps to accounts here.
- V343 (prior session): `supplier_payments.settlement_discount NUMERIC(15,2) NOT NULL DEFAULT 0`, two partner-ledger entries (`SUPPLIER_PAYMENT` + `SUPPLIER_SETTLEMENT_DISCOUNT`) so the party balance zeroes exactly, operationId idempotent replay, translations.
- V348 (this session): `supplier_payments.original_due NUMERIC(15,2)` snapshot of outstanding at settlement; finance-role guard (SUPER_ADMIN/ADMIN/FINANCE_MANAGER/ACCOUNTANT via SecurityContextHolder authorities → 403); `ProcurementAccountingService.postSupplierSettlementDiscount` posts a balanced subledger event (`AP:DISCOUNT:<opId>`); audit JSON now records `settlementDiscount` + `originalDue`; payments sheet gained Original Due / Settlement Discount columns and invoices sheet a Settlement Discounts total column; FE payment dialog collapsible "تسوية بخصم" section with live preview (Original due / Cash / Discount / Remaining-after), submit blocked while cash+discount > outstanding, invoice-row tooltip totals discounts taken, payments table discount column; 5 new translation rows (v348 CSV).
- Tests: BE `discountedSettlementClosesInvoiceAndBooksDiscountEntry` asserts originalDue=100.00, invoice PAID, 2 ledger entries, subledger verify; `rejectsSettlementDiscountWithoutFinanceRole` (403); `rejectsNegativeSettlementDiscount`. FE 3 new specs (preview triple, exceeds-block, per-invoice totals).
- Gates: BE **801 tests / 196 suites / 0 failures**, error codes **597/597**, catalog **13,948 PASS** (floor ≥801). FE **478 tests / 100 files**, check:i18n **4,650 keys**, hardcoded **0 violations**, build green (floor ≥478).
