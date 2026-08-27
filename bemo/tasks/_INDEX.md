# Task Index & Assignment Matrix
> One file per work package in this folder. Every dev gets `_GLOBAL-RULES.md` + only their own WP file.
> Every WP ends with **Acceptance Criteria** — numbered checkboxes; QA ticks them before merge.
> **Liquibase V-numbers:** coordinator assigns concrete V### per WP before branch-out (headers show placeholders). Never pick your own number.

## Wave plan (parallel-safe, no file conflicts)

### Wave 1 — start immediately
| File | Title | Pri | Owner track | Effort |
|---|---|---|---|---|
| WP-01 | Supplier partial payments ✅ **DONE** (partial-pay core pre-shipped; installment plans shipped V344 — `supplier_payment_plans`, `POST /api/v1/supplier-invoices/{id}/payment-plan`, auto-mark on payment, FE dialog, 756 BE / 455 FE green 2026-08-24) | 🔴 | Backend A | closed |
| WP-03 | Purchase request → approval → PO ✅ **DONE** (new `com.bemo.hr.trade.procurement.request` package — V345 `purchase_requests`+`purchase_request_lines`, `/api/v1/purchase-requests` CRUD + submit/approve/reject/cancel/convert, one-PO conversion fills `purchase_orders.purchase_request_id`, PR_* error codes en/ar, FE procurement "طلبات الشراء" tab w/ create-edit modal, convert-to-PO supplier picker, 776 BE / 458 FE green 2026-08-24; approve/reject = direct endpoints + audit note per AC-5 fallback) | 🔴 | Full-stack B | closed |
| WP-06 | Generated-periods registry ✅ ALREADY SHIPPED (`availablePeriods` filters overlaps) | 🟠 | — | closed |
| WP-09 | First-login push prompt ✅ **DONE** (452/452 FE · V341 · H2 ✓) | 🟠 | Frontend E | shipped |
| WP-13 | Shortcut × dialog fixes ✅ **DONE** (BUG-7 was already shipped upstream; 8→7 scope) | 🟡 | Frontend E² | shipped |
| WP-14 | Android Capacitor wrapper ✅ **DONE** (Capacitor 8 scaffold in `fe/android` (com.bemo.erp, versionName mirrors fe); AC-1 server-picker page outside auth guard probing `/api/v1/i18n`; V346 `web_push_subscriptions.platform/fcm_token` + ANDROID FCM branch + `platform='WEB'` delivery filter; AC-3 selfie punch `POST /api/v1/attendance/selfie-punch` idempotent by operationId + IndexedDB offline outbox w/ exactly-once replay; AC-4 barcode scan wired into inventory lookup; AC-5 back-button exit confirm; AC-6 biometric resume gate via custom androidx plugin — 785 BE / 467 FE green 2026-08-24, APK build parked externally) | 🟡 | Mobile G | closed |

### Wave 2
| File | Title | Pri | Owner track | Effort |
|---|---|---|---|---|
| WP-02 | Settlement-discount ledger ✅ **DONE** (`supplier_payments.settlement_discount` (V343) + `original_due` snapshot (V348); finance-role guard `PROC_SETTLEMENT_DISCOUNT_FORBIDDEN` 403; balanced partner-ledger pair (`SUPPLIER_PAYMENT`+`SUPPLIER_SETTLEMENT_DISCOUNT`) zeroes party balance; GL subledger event via posting profile (`AP:DISCOUNT:<opId>`); audit records discount+originalDue; FE collapsible "تسوية بخصم" w/ live preview + submit block, invoice tooltip totals, export columns — 801 BE / 478 FE green 2026-08-24) | 🔴 | Backend A (after 01) | closed |
| WP-04 | Fixed assets module (B-4) ✅ **DONE** (`com.bemo.hr.assets` package: `FixedAsset` straight-line entity w/ remainder-in-final-month math + `FixedAssetDepreciationPost` exactly-once evidence (uq app/asset/month); `AssetDepreciationService` month-end run → one balanced approved journal per asset (Dr 5300/Cr 1280, configurable `hr.finance.*-account-code`), period-locked & missing-account per-asset skips; balanced disposal journals w/ gain(4100)/loss(5310) plugs; `AssetDepreciationScheduler` cron + manual `/run-depreciation` under ReentrantLock; CRUD+dispose REST `/api/v1/fixed-assets`; V347 tables+menu grant+130 translations; AccessCatalog P_ASSET_READ/MANAGE + FINANCE page; FE register page w/ dispose dialog, run modal + results table, XLSX export scope — 800 BE / 475 FE green 2026-08-24) | 🔴 | Backend C | closed |
| WP-05 | Inventory valuation report (verify-first) ✅ **DONE** (report endpoint extended `?asOf=&warehouseId=&itemId=` w/ as-of repo queries (balanceAsOf/inventoryValueAsOf/revaluationValueAsOf), per-row method badge, `glInventoryAccountBalance`+variance reconciliation, OPS_ITEM_NOT_FOUND 404; FE as-of/warehouse filters + reconciliation banner + method column + `/api/v1/exports/inventory-valuation.xlsx`; V349 16 translations — 805 BE / 480 FE green 2026-08-24) | 🟠 | Backend C² | closed |
| WP-07 | Loans deduction policy switcher ✅ **DONE** (reuses `workforce_advance_policies` V58 — no new table; entity canonicalizes AUTO_IN_PAYROLL→AUTO / MANUAL_BUTTON→MANUAL / MID_MONTH_SPLIT; resolver `resolveDeductionPolicy` w/ EMPLOYEE→CATEGORY→GLOBAL→defaults(AUTO,MONTHLY) precedence + `GET /api/v1/workforce/advances/resolved-policy`; payroll gate skips auto-collection under MANUAL (`isManualDeductionPolicy`) + ADVANCE_DEDUCTION explanation row records skip reason JSON; idempotent manual apply `POST .../apply-deduction` per (employee,period) via ledger notes suffix, collects overdue installments capped by balance, ADVANCE_MANUAL_NOT_DUE/ADVANCE_NOTHING_DUE/ADVANCE_POLICY_INVALID/ADVANCE_POLICY_EXISTS codes en/ar (V350); FE settings business-tab policy card (global+category drafts, Save-All/dirty/cancel) + employees toolbar action gated on MANUAL w/ affected-count confirm loop — 816 BE / 487 FE green 2026-08-24) | 🟠 | Backend D | closed |
| WP-08 | Peak clock-in analytics ✅ **DONE** (BE 3/3 · FE 453/453 · V342 · H2 ✓) | 🟠 | Backend D + FE | shipped |
| WP-10 | Vertical role templates (users page) | 🟠 | Full-stack F | 3–4d |
| WP-19 | Single-punch rule surface ✅ ALREADY SHIPPED (category checkbox + ONE_PUNCH bulk accept) | 🟠 | — | closed |
| WP-20 | Employee form groups + preview ✅ **DONE 2026-08-27** (FE-only: `FORM_GROUPS` 7-group accordion single-source driving both form + preview; AC-2 auto-expand on invalid submit; AC-3 preview 1:1 salary-gated parity; V362 + `employees.group.invalidFields`; 553 FE / 5,182 i18n green 2026-08-27) | 🟠 | Full-stack F² | 2d |

### Wave 3 — product expansion
| File | Title | Pri | Owner track | Effort |
|---|---|---|---|---|
| WP-11 | Employee expense claims | 🟡 | Full-stack F | 4d |
| WP-12 | Sales targets & commissions engine | 🟡 | Backend D | 4d |
| WP-15 | Medical clinic MVP slice | 🟢 | Squad H+E | ~3wk |
| WP-16 | Agri-export documentation pack | 🟢 | Full-stack B | ~2wk |
| WP-17 | Manpower-supply client billing | 🟢 | Backend D | ~8d |
| WP-18 | Tech-debt bundle (GraalVM/CSV-gen/cache) | 🟢 | Any filler | 1d ea |

### Wave 4 — medical depth (after WP-15 ships)
WP-21 EMR depth · WP-22 appointments/rosters · WP-23 pharmacy+narcotics · WP-24 lab/imaging orders · WP-25 insurance & claims · WP-26 hospital ops (ADT/OT/nursing) · WP-27 dental/specialty charting

### Wave 5 — market & platform packs (any order)
WP-28 OCR invoice capture · WP-29 payment gateways + public pay page · WP-30 bank reconciliation · WP-31 WhatsApp outbound templates · WP-32 scheduled report delivery · WP-33 security pack (2FA/policy/IP/devices) · WP-34 SSO · WP-35 PDPL privacy toolkit · WP-36 command palette + saved views · WP-37 bulk edit + help · WP-38 onboarding wizard · WP-39 automation pack · WP-40 API/webhooks/keys · WP-41 report builder · WP-42 growth pack (loyalty/memberships/referrals) · WP-43 rentals/work-orders/bookings · WP-44 fleet & maintenance · WP-45 helpdesk+KB · WP-46 marketing lite · WP-47 eSign + GED · WP-48 finance extras trio · WP-49 ESS mobile surfaces · WP-50 recruitment ATS · WP-51 multi-country e-invoicing abstraction

### Phase-gated
| File | Gate |
|---|---|
| WP-52 | AI pack — needs ≥6 months real data; deterministic Phase-1 first |

## Cross-WP dependency quick map
- WP-02 ← WP-01 · WP-21..27 ← WP-15 · WP-49 ← WP-14+09 · WP-36 ← WP-13 · WP-39 ← WP-32 pattern · WP-46 ← WP-31 pattern · WP-51 refactors existing ETA · WP-44 benefits from WP-04
