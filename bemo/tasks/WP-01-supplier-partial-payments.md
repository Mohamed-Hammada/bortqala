# WP-01 — Supplier Partial Payments & Installment Plans
**Priority:** 🔴 P0 · **Suggested owner:** Backend dev A · **Depends on:** nothing · **Effort:** 3–4 days
**Read first:** `_GLOBAL-RULES.md`

## Business goal (why)
Today paying a supplier invoice marks it fully PAID. Real life: we owe 10,000 EGP, pay 4,000 now, rest over two months. Buyers do this daily. Competitor Daftra ships installments as a standalone product.

## Current state (verified)
- `be/src/main/java/com/bemo/hr/trade/procurement/domain/SupplierInvoice.java` — status transitions; payment flips to PAID.
- `ProcurementService.recordPayment(...)` — validates unpaid, creates one `PartnerLedgerEntry` credit for full amount, sets PAID.

## Backend steps
1. Migration **V341**: `supplier_invoices` + `amount_paid NUMERIC(18,4) NOT NULL DEFAULT 0`, `payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'` (UNPAID/PARTIAL/PAID). Backfill rows where status='PAID': `amount_paid = net_amount, payment_status='PAID'`.
2. Domain method on `SupplierInvoice`: `applyPayment(BigDecimal amount)` — amount > 0 and ≤ remaining (`net_amount − amount_paid`); updates `amount_paid`; recomputes `payment_status` (PAID iff remaining becomes 0, else PARTIAL); keep legacy `status` column in sync so existing reports/exports don't break.
3. Service: extend payment request DTO with optional `appliedAmount` (null = full remaining → old callers unchanged). Ledger entry records the APPLIED amount only. Reuse `operationId` idempotency pattern (see `WorkforceSettlementService` / `shared/idempotency`).
4. Installments (second slice): **V342** table `supplier_payment_plans` (id, app_id, invoice_id FK, installment_no, due_date, amount, paid_at NULL). Endpoint `POST /api/v1/supplier-invoices/{id}/payment-plan {installmentCount, firstDueDate}` → N equal installments. Paying one = call existing payment endpoint with that amount.
5. New exception codes → translation CSV: `PAYMENT_EXCEEDS_REMAINING`, `PAYMENT_AMOUNT_INVALID`, `PLAN_ALREADY_EXISTS`.

## Frontend steps
1. `features/trade/procurement/models`: add `amountPaid:number|null`, `paymentStatus:'UNPAID'|'PARTIAL'|'PAID'|null`.
2. Invoices tab: status badge shows PARTIAL state + new "remaining" column via currency formatter.
3. Payment dialog: "amount to pay now" prefilled with remaining; live "remaining after payment" line; disable submit when invalid (backend still guards).

## i18n keys (add to CSV both locales)
`procurement.amountPaid`, `procurement.remainingAmount`, `procurement.paymentStatus.UNPAID/PARTIAL/PAID`, `procurement.payPartialTitle`, `procurement.createPlan`, `procurement.planInstallmentNo/DueDate/Amount/PayNow`.

## Tests
- Service: pay 40% → PARTIAL; pay rest → PAID; overpay rejected; double-submit same operationId = one ledger entry.
- FE spec: dialog math + disabled submit state.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Paying 40% of an invoice flips it to PARTIAL badge with correct remaining column; paying the rest flips to PAID. *(core pre-shipped: `paidAmount`/`outstandingAmount`/`PARTIALLY_PAID` end-to-end; re-verified)*
- [x] **AC-2** Overpay attempt → `PAYMENT_EXCEEDS_REMAINING` translated problem-details; UI blocks submit before that. *(shipped as `PROC_PAYMENT_EXCEEDS_BALANCE` + `PROC_SETTLEMENT_DISCOUNT_EXCEEDS`, CSV v343; FE dialog blocks via outstanding check)*
- [x] **AC-3** Ledger entries across all payments sum exactly to amount_paid; party statement nets correctly. *(posted-payment sum drives status; settlement discount booked as separate ledger row)*
- [x] **AC-4** Installment plan of N months creates N rows summing to remaining amount; paying one installment marks its paid_at. *(V344 — `SupplierPaymentPlanService.createPaymentPlan` equal split w/ remainder on last row; `markInstallmentsSettled` hooked into payment transaction; `SupplierPaymentPlanTests` 9 tests)*
- [x] **AC-5** Duplicate payment-plan creation → `PLAN_ALREADY_EXISTS`; replaying same payment operationId = one ledger entry. *(shipped as `PROC_PAYMENT_PLAN_ALREADY_EXISTS` CSV v344; operationId idempotency pre-existing and tested)*
- [x] **AC-6** Migration backfill: pre-existing PAID invoices show amount_paid=net, status PAID, zero data change otherwise. *(equivalent-by-design: paid/outstanding are derived live from posted payments — no stored column to backfill; verified by `partialPaymentsReducePlannedRemainder` etc.)*
- [x] **AC-7** Excel export includes paid/remaining columns localized ar+en; all DoD gates in `_GLOBAL-RULES.md` pass. *(exporter cols 9/10 = paidAmount/outstandingAmount; gates run 2026-08-24)*

## Verification evidence (2026-08-24)
- Backend: `./gradlew test -PskipDockerTests` → **756 tests / 191 suites / 0 failures** (BUILD SUCCESSFUL); `check-error-codes.py` 577/577 PASS; `check-translation-catalog.py` 13,640 rows PASS (also fixed a pre-existing unquoted-semicolon defect in the v343 CSV); `check-test-count.py` floors 700/185 → observed 756/191.
- Frontend (node 24): `ng test --watch=false` → **455/455 passed across 96 files**; `check:i18n` 4,563 keys PASS; `check:hardcoded` 0 violations; `ng build` success.
- New artifacts: V344 schema+translations registered in BOTH masters · `SupplierPaymentPlan` entity/repo/service/controller (`/api/v1/supplier-invoices/{id}/payment-plan`) · FE plan dialog + 2 new specs · READMEs updated.
