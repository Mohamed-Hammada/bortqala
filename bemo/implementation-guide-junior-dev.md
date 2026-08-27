# Bemo ERP — Implementation Guide for Missed & Incomplete Features

> **Audience:** junior developer picking up work alone.
> **⚠️ SPLIT FORMAT:** the work packages below are also exported as **one self-contained file per task** in `tasks/` (same folder) — `_INDEX.md` + `_GLOBAL-RULES.md` + `WP-01…WP-18`, each with formal Acceptance Criteria checklists for QA sign-off. Assign developers from `_INDEX.md`; each dev reads only `_GLOBAL-RULES.md` + their own WP file.
> **How to use:** read Part A once (global rules). Then take ONE work package (WP) from Part B, follow its steps top-to-bottom, tick its acceptance criteria, run the verification commands, and stop. Never start two WPs at once.
> **Source:** every item here comes from `missing-todo.md` (sections 1–23), verified against the actual codebase on Aug 23, 2026.

---

# PART A — GLOBAL RULES (apply to every WP)

## A.1 Repository map

| Folder | What lives there |
|---|---|
| `be/` | Spring Boot 4.1 backend (Java 21, Gradle, PostgreSQL, Liquibase, JPA) |
| `fe/` | Angular 22 frontend (standalone components, signals, SCSS, Vitest, **Node 24 only**) |
| `desktop/` | Tauri desktop distribution |
| `license-app/` | License activation service |
| `be/modules/device-hub/` | Python multi-vendor biometric device gateway |
| `docs/TEST_EVIDENCE.md` | Current test baselines (backend ≥739 tests/189 suites, frontend ≥429 tests/91 files) |

## A.2 Backend rules (from `be/skills/hr-backend/SKILL.md`) — memorize these

1. One package per capability under `com.bemo.hr.<capability>`; inside it create ONLY the layers you need: `api/`, `application/`, `domain/`, `infrastructure/`. No empty layers.
2. Constructor injection with `final` fields. API DTOs are Java **records**. NEVER return JPA entities from controllers.
3. Every tenant-owned entity gets `@TenantId private String appId;` (+ `@Version` for mutable aggregates).
4. Dates/times: `Instant` for audit stamps, `LocalDate` for work dates, `LocalTime` for schedules. API JSON: instants/dates as **epoch-millisecond numbers**, times as `"HH:mm"`.
5. Schema changes ONLY through versioned Liquibase YAML. **Next free migration number: V341** (V340 exists). Register every new changelog in BOTH `next.changelog-master.yaml` AND `test-h2.changelog-master.yaml`.
6. Errors: throw `BusinessRuleException("human message", "STABLE_CODE_KEY", HttpStatus...)`. Then add translation rows for `STABLE_CODE_KEY` in a new translations CSV (`ar-EG` + `en-US`, ids like `v341-001-en` / `v341-001-ar`). Gate: `python be/tools/check-error-codes.py` must stay green (currently 575/575).
7. Transactions live in application services (`@Transactional` on the service method).
8. Tests: parameterized unit tests for rule combinations + focused integration tests for persistence edges. Run `./gradlew test -PskipDockerTests`.
9. Update the bilingual `README.md` of every Java package you touch (English + Arabic sections).
10. Keep calculations in the BACKEND. The frontend formats results; it never re-implements money/time math.

## A.3 Frontend rules (from `fe/skills/hr-frontend/SKILL.md`) — memorize these

1. Standalone components, `ChangeDetectionStrategy.OnPush`, `inject()`, signals/computed, typed reactive forms, built-in `@if`/`@for`. NO NgModules. NO `any`.
2. Feature folder shape: `features/<name>/pages/ · ui/ · data-access/ · models/ · <name>.routes.ts` — lazy-loaded route in `app.routes.ts`.
3. HTTP calls live in `data-access` services only. Components consume typed state and emit intent.
4. **Zero hardcoded strings**: every user-visible string = `i18n.t('some.key', undefined, 'fallback')` in templates/TS. Gates: `npm run check:i18n` (every literal key must exist in BOTH locales in the DB) and `npm run check:hardcoded` (must report 0 violations). When you invent keys, they ALSO need Liquibase translation CSV rows (ar-EG/en-US) — static fallbacks in code are not enough for the i18n gate.
5. Epoch-millisecond values go through `core/date.ts`; never parse dates by hand.
6. States before happy path: loading, empty, error, stale, permission-denied, success.
7. Confirm destructive/bulk actions showing affected counts. Touch targets ≥40px. Color never alone (add text/icon).
8. Tests: Vitest specs next to what you test. Run `npm run test -- --watch=false` under Node 24.
9. Update the feature's bilingual `README.md`.

## A.4 Menu & permission registration protocol (when your WP adds a new page/menu)

Do ALL FOUR or the menu will be invisible/broken for some roles:

1. **Shell nav**: register the item in `items[]` in `fe/src/app/core/shell/app-shell.component.ts` with its workspace group; make `visible(item)` and `AuthService.hasMenuAccess(menuId)` return `true` for `SUPER_ADMIN`/`ADMIN` explicitly.
2. **Translations + fallbacks**: add `workspace.<group>` and `nav.<menuId>` keys to `DEFAULT_FALLBACKS` in `fe/src/app/core/i18n.service.ts` (both locales) AND to the new Liquibase translations CSV.
3. **DB migration**: append the new menu id to existing users: SQL UPDATE on `app_users.allowed_menus` (idempotent — check `NOT LIKE '%<menuId>%'` first). Also update default allowed-menus strings in `AppUser.java` and `AuthService.java` for future users.
4. **Users UI**: add the menu id to `USER_MENU_OPTIONS` in `fe/src/app/features/users/users.page.ts`. Also extend `AccessCatalog` (backend) + `access-catalog-contract` (fe) if the page has permissions, and update `check-authorization-contract.py` expectations if adding `@PreAuthorize` codes.

## A.5 Definition of Done (every WP)

```
[ ] ./gradlew test -PskipDockerTests            → 0 failures (baseline ≥739/189 grows)
[ ] python be/tools/check-error-codes.py        → all codes translated
[ ] python be/tools/check-translation-catalog.py→ 0 defects
[ ] cd fe && nvm use 24 && npm run test -- --watch=false → 0 failures
[ ] npm run check:i18n                          → 0 missing keys
[ ] npm run check:hardcoded                     → 0 violations
[ ] npm run build                               → success (SCSS budget warnings pre-existing OK)
[ ] New exception codes have ar-EG + en-US rows
[ ] New pages registered per protocol A.4
[ ] Touched packages' README.md updated (ar + en)
```

## A.6 Git discipline

Branch per WP: `feat/wp-XX-short-name`. One vertical slice per PR. Do not commit secrets, `.env`, or local scripts. The working tree already contains unrelated uncommitted edits (`start-backend-*.bat`) — do NOT stage them.

---

# PART B — WORK PACKAGES

Priority legend: 🔴 P0 = business-critical · 🟠 P1 = product completeness · 🟡 P2 = polish/growth.

---

## WP-01 🔴 Supplier Partial Payments & Installment Plans

**Business goal:** today paying a supplier invoice marks it fully PAID. Real life: we owe 10,000 EGP, pay 4,000 now, rest over 2 months. Buyers demand this daily; competitor Daftra ships it as a standalone module.

**Current state**
- `be/src/main/java/com/bemo/hr/trade/procurement/domain/SupplierInvoice.java` — has status transitions; payment sets PAID.
- `ProcurementService.recordPayment(...)` — finds invoice, validates unpaid, creates `PartnerLedgerEntry` credit, flips status to PAID (all-or-nothing).

**Backend steps**
1. Migration V341: on `supplier_invoices` add `amount_paid NUMERIC(18,4) NOT NULL DEFAULT 0` and `payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID'` (values UNPAID/PARTIAL/PAID). Backfill: `amount_paid = net_amount, payment_status='PAID'` where status='PAID'.
2. Domain: add `applyPayment(BigDecimal amount)` to `SupplierInvoice`: amount > 0, ≤ remaining (`net - paid`); sets `amount_paid`, recomputes `payment_status` (PAID when remaining == 0 else PARTIAL); keeps legacy `status` in sync so old reports don't break.
3. Service: `recordPayment` takes optional applied amount (default = full remaining, keeps old callers working). Create ledger entry for the APPLIED amount only. Idempotency: reuse the existing `operationId` pattern (see `WorkforceSettlementService` for reference).
4. Installments (optional second slice): table V342 `supplier_payment_plans` (`id`, `app_id`, `invoice_id FK`, `installment_no`, `due_date`, `amount`, `paid_at NULL`). Endpoint `POST /api/v1/supplier-invoices/{id}/payment-plan` creates N equal installments. A scheduler-less approach: the invoices page shows "due installments" list; paying one calls the same `recordPayment` with its amount.
5. New error codes: `PAYMENT_EXCEEDS_REMAINING`, `PAYMENT_AMOUNT_INVALID`, `PLAN_ALREADY_EXISTS` → translation CSV rows.

**Frontend steps**
1. `features/trade/procurement/models`: add `amountPaid`, `paymentStatus` to invoice model; union type `'UNPAID'|'PARTIAL'|'PAID'`.
2. Invoices tab: replace binary paid badge with badge + "remaining" column (`formatCurrency` helper).
3. Payment dialog: input "amount to pay now" prefilled with remaining; show computed "remaining after"; block submit > remaining client-side AND rely on backend guard.
4. Optional: "Create installment plan" button → small dialog (N months, start date) → renders plan table inline with per-row Pay action.

**Tests:** service test paying half twice then rest (status path UNPAID→PARTIAL→PAID); overpay rejection; concurrent double-pay guarded by operationId. FE spec: dialog math + disabled state.

**Acceptance:** an invoice shows partial state after a 40% payment; ledger entries sum exactly to paid amounts; Excel export includes paid/remaining columns.

---

## WP-02 🔴 Payment-Time Settlement Discount (خصم على الفلوس)

**Business goal:** supplier we owe 1,000 settles for 900 cash. Finance must record: original debt, cash paid, written-off discount — three separate figures, auditable.

**Current state**
- Invoice-level discount exists (`discount` column, V45). Payments record only the paid amount. Nothing captures "settled for less".

**Backend steps**
1. V341/V342 (same release window): on `supplier_payments` add `settlement_discount NUMERIC(18,4) NOT NULL DEFAULT 0` and `original_due NUMERIC(18,4) NULL`.
2. Extend `recordPayment(request)` DTO: `appliedAmount` + optional `settlementDiscount`. Validation: `appliedAmount + settlementDiscount <= remaining`; discount requires role `FINANCE_MANAGER` or admin (add `@PreAuthorize` note or service-level role check consistent with neighbors).
3. Ledger: post TWO entries — credit cash for `appliedAmount`, credit "settlement discount granted" expense-like account for `discount` (account configurable via property `hr.finance.settlement-discount-account-code`, default `5200`; resolve via existing account lookup, skip posting with warning log if absent).
4. Error codes: `SETTLEMENT_DISCOUNT_EXCEEDS_REMAINING`, `SETTLEMENT_DISCOUNT_INVALID`.

**Frontend steps**
1. Payment dialog gains collapsible "تسوية بخصم / Settle with discount" section: discount input, live preview lines (Original due / Cash now / Discount / Remaining after=0 forced when discount used).
2. Invoice row tooltip/export column shows total discounts taken.
3. Keys: `procurement.settlementDiscount*` family (≈8 keys) + CSV rows.

**Tests:** exact ledger pair assertion (debit bank 900 / debit discount 100 / credit AP 1000); discount > remaining rejected; non-finance actor rejected.

**Acceptance:** party statement (AR/AP aging module) nets to zero after discounted settlement; audit log records actor + both amounts.

---

## WP-03 🔴 Purchase Request → Approval → PO Conversion

**Business goal:** complete the procurement document cycle: department asks (PR) → manager approves → buyer converts approved items into a PO. Currently `PurchaseOrder.purchaseRequestId` is a dangling text field nothing populates.

**Current state**
- Approval workflow engine exists generically (`com.bemo.hr.approval`, Epic 2): definitions + pending tasks inbox.
- `ProcurementApi` has PO create/update/receive/cancel.

**Backend steps**
1. New package `com.bemo.hr.trade.procurement.request` (keep inside trade.procurement to reuse repos).
2. V343 tables: `purchase_requests` (id, app_id, requested_by, department_id, status DRAFT/SUBMITTED/APPROVED/REJECTED/CONVERTED/CANCELLED, needed_by date, notes, created_at/updated_at, version) and `purchase_request_lines` (id, app_id, request_id FK, item_id, item_name snapshot, quantity, unit_of_measure, estimated_unit_price, converted_quantity default 0).
3. Endpoints (`PurchaseRequestController`, prefix `/api/v1/purchase-requests`):
   - `GET ?status=&departmentId=` list · `POST` create (lines ≥1) · `PUT /{id}` edit while DRAFT · `POST /{id}/submit` · `POST /{id}/approve|reject` (role `PROCUREMENT_APPROVER` or admin; reuse `@auth.hasPermission('approvals.decide')` style evaluator if present) · `POST /{id}/convert` body `{supplierId}` → creates ONE PO whose lines mirror request lines, marks request CONVERTED, stores returned po.id back on request (`converted_po_id` column), and passes `purchaseRequestId` into PO payload so the existing field finally carries data.
4. Guard rails: convert only APPROVED; reject conversion if any line `converted_quantity > quantity`; cancel releases nothing (no budget link yet — note for WP-04).
5. Wire into the EXISTING approval engine instead of bespoke statuses if trivially possible: prefer registering a `WorkflowDefinition` of type `PURCHASE_REQUEST` — check `approval` package docs first; fall back to direct approve endpoint if integration would take >1 day.

**Frontend steps**
1. New lazy feature `features/trade/purchase-requests/` (models/data-access/page) OR a third tab inside existing procurement page (choose tabs — less routing work; page already hosts PO/GRN/invoices/payments tabs).
2. Tab columns: #, requester, department, needed-by, lines count, total estimate, status badge, actions per state (Submit/Approve/Reject/Convert/Cancel).
3. Convert flow opens the EXISTING PO-create dialog prefilled from request lines.
4. Register menu only if standalone page chosen (protocol A.4); tabs need no menu change but DO need permission gating reuse of `procurement.*`.

**Tests:** state machine matrix (parameterized); convert creates PO with identical line quantities and flips status; double-convert blocked; approval by non-approver rejected.

**Acceptance:** from PR creation to received PO without touching PO form manually; request shows linked PO number.

---

## WP-04 🔴 Fixed Assets Module (roadmap B-4, Daftra parity gap)

**Business goal:** companies own cars, machines, PCs. Accountant needs monthly depreciation posted automatically, plus disposal/sale handling. Today: nothing.

**Current state**
- GL/journal services exist (`JournalEntryService`, accounts CRUD). Multi-dimensional GL (V288–V289). Nothing asset-specific.

**Backend steps**
1. New package `com.bemo.hr.assets`: `FixedAsset` entity — id, app_id, name, category (enum VEHICLE/MACHINERY/EQUIPMENT/BUILDING/OTHER or free category FK), acquisition_date, acquisition_cost, salvage_value, useful_life_months, depreciation_method (STRAIGHT_LINE only v1), accumulated_depreciation (derived, store cached), status ACTIVE/FULLY_DEPRECIATED/DISPOSED, disposal fields (date, proceeds, gain_loss), branch_id/cost_center_id (existing org FKs!), version.
2. V344 table + indexes (app_id, status), (cost_center_id).
3. Monthly job: `AssetDepreciationScheduler` (`@Scheduled(cron)` + manual `POST /api/v1/fixed-assets/run-depreciation?yearMonth=`) — for each active asset compute monthly charge `(cost - salvage)/life`; skip months already posted (track last_posted_year_month per asset); post ONE journal entry per asset per month: Dr depreciation-expense (config `hr.finance.depreciation-expense-account-code` default `5300`), Cr accumulated-depreciation (default `1280`). Idempotent via unique `(asset_id, year_month)` on a `fixed_asset_depreciation_posts` evidence table.
4. Disposal: `POST /{id}/dispose {date, proceeds}` → reverse remaining book value: Dr accumulated, Dr cash(proceeds), Dr loss / Cr gain plug, Cr cost. Compute gain/loss server-side.
5. Export: reuse `DataExportService` pattern → `/api/v1/fixed-assets/export.xlsx` (Arabic filename convention like other exporters).
6. Error codes: `ASSET_LIFE_INVALID`, `ASSET_DISPOSAL_INVALID`, `DEPRECIATION_PERIOD_LOCKED`.

**Frontend steps**
1. New feature `features/finance/fixed-assets/`: table (name, category, cost, monthly charge, accumulated, net book value, status), create/edit dialog, dispose dialog with computed preview, "Run month-end depreciation" button (admin) with year-month picker + result toast (assets processed count).
2. Full protocol A.4 (new menu `fixedAssets` under finance workspace).
3. Permission codes `P_ASSET_READ`/`P_ASSET_MANAGE` in AccessCatalog → catalog count becomes 35 (update PROJECT_MAP `catalog_entries`!).

**Tests:** straight-line math incl. final short month; idempotent double-run same month (one journal); disposal gain vs loss branches; tenant isolation (two apps don't see each other).

**Acceptance:** accountant sees net book value per asset; month-end run posts balanced journals visible on trial balance.

---

## WP-05 🟠 Inventory Valuation Reporting Completion (B-5 residual)

**IMPORTANT correction:** FIFO/weighted-average valuation + GL posting ALREADY EXIST in the operations inventory control (see `be/skills/hr-backend/SKILL.md` "Inventory control now includes FIFO/weighted-average valuation and GL posting"). What's likely missing is the *continuous-valuation report surface*. VERIFY FIRST:

**Step 0 (half a day):** grep `operations` package for valuation endpoints (`valuation`, `FIFO`, `weightedAverage`, `costLayer`). List what exists. Only build what's actually absent below.

**Likely gaps to close**
1. Report endpoint `GET /api/v1/operations/valuation-report?itemId=&warehouseId=&asOf=` returning per-item: on-hand qty, unit cost (method-aware), inventory value, plus method used. If layers table lacks an as-of query, add repo query summing layers consumed ≤ asOf.
2. Frontend: card/table inside operations workbench "Valuation" section + Excel export button (reuse exporter).
3. Method switch display: show whether WA or FIFO produced the figure (evidence, not a toggle — changing methods retroactively is OUT of scope).
**Acceptance:** report total reconciles to GL inventory-control account balance within tolerance shown on screen.

---

## WP-06 🟠 Generated-Report-Period Registry (hide already-generated months)

**Business goal:** HR picks July 2026 to generate the attendance/payroll report — system must gray out months already finalized so nobody regenerates/conflicts.

**Current state**
- Reports are ad-hoc ranges (`reporting` package); approval freezes snapshots; there IS approval history but no lightweight "this period exists" registry the picker reads.

**Backend steps**
1. Repo query is probably enough BEFORE a new table: derive from existing approved reports (`SELECT DISTINCT period_start/period_end` where status=APPROVED). Add `GET /api/v1/reports/generated-periods?year=` → `[{from,to,type}]` (epoch-millis dates!).
2. Only add a dedicated table if distinct-query performance is bad (>200ms realistic data). Prefer query first.

**Frontend steps**
1. Reports page period picker: fetch generated periods per year; render those month chips disabled with tooltip `reports.alreadyGenerated` ("تم إنشاء تقرير معتمد لهذه الفترة") + link "view existing report" navigating filtered to it.
2. Keys: `reports.alreadyGenerated`, `reports.viewExisting` (+CSV rows).

**Tests:** BE endpoint returns expected ranges; FE spec asserts chip disabled + navigation link emitted.

**Acceptance:** generating July then reopening picker shows July disabled; August still enabled.

---

## WP-07 🟠 Loans Cadence Switcher + Global Deduction Policy

**Business goal:** admin decides PER CATEGORY how advance repayment works: automatic inside payroll vs manual button, monthly vs every-15-days. Today behavior is implied by document type; there's no switch, and no global policy that individuals inherit.

**Current state**
- `WorkforceAdvanceInstallment` + `WorkforceAdvanceService.calculateEmployeePayrollDeduction(...)` auto-deducts during payroll; contractor settlements apply adjustments per 15-day cycle. Per-employee plans exist.

**Backend steps**
1. V345: table `advance_deduction_policies` (id, app_id, scope GLOBAL/CATEGORY, category_id NULL, mode AUTO_IN_PAYROLL / MANUAL_BUTTON, cadence MONTHLY / MID_MONTH_SPLIT, created_by…, version). Enforce single GLOBAL row + ≤1 per category.
2. Resolution helper in `WorkforceAdvanceService`: `resolvePolicy(appId, categoryId)` → category override ?? global ?? defaults (AUTO_IN_PAYROLL, MONTHLY) so existing tenants see zero behavior change.
3. Payroll call site (`PayrollService` around line 260) consults resolver: MANUAL mode → skip auto deduction (leave advanceBalance untouched, deduction 0).
4. Manual application endpoint: `POST /api/v1/advances/apply-deduction {employeeId, periodId}` → computes installment due and posts it (reuses settlement logic); guarded by permission `advances.apply`.
5. Error codes: `ADVANCE_POLICY_EXISTS`, `ADVANCE_MANUAL_NOT_DUE`.

**Frontend steps**
1. Settings → Advances section (or inside categories page per-category): policy editor cards (scope selector, mode radio, cadence radio), save-all button following the settings-page save pattern (only persists on explicit Save).
2. Employees page bulk action "Apply deduction now" visible only when resolved policy is MANUAL (fetch resolved policy per employee category via new light endpoint `GET /api/v1/advances/resolved-policy?categoryId=`).
3. Keys ≈12 (`settings.advancePolicy*`, `employees.applyDeduction`…) + CSV.

**Tests:** resolver precedence (category beats global beats default); payroll skips deduction under MANUAL; manual apply rejects when AUTO; idempotent double-apply per period.

**Acceptance:** switching Security category to MANUAL stops payroll auto-deduction for its members and enables the bulk button; switching back resumes.

---

## WP-08 🟠 Peak Clock-In Analytics

**Business goal:** owner asked "around what hour does each category actually arrive?" — helps shift planning. Zero hour-of-day aggregation exists today.

**Backend steps**
1. Repo query over `punch_records` joined to employee category: bucket `EXTRACT(HOUR FROM punched_at AT TIME ZONE hr.company-zone)` per category per selected range. IMPORTANT: use the configurable zone (A.2 rule 4) — never hardcode Cairo.
2. Endpoint `GET /api/v1/dashboard/clock-in-histogram?months=3&categoryId=` → `[{hour:6..11, countsByCategory:{catId:count}}]` capped range 1..24 months like trends endpoint.
3. Performance: aggregate query, add index if needed `(app_id, punched_at)` — check existing indexes first.

**Frontend steps**
1. Dashboard section beside multi-period trends: horizontal bar list (no chart library! skill forbids until justified — CSS bars suffice): rows = hours 05:00–12:00, bar width = share, per category color dot + legend, category filter select.
2. Excel export column set added to trends exporter OR separate `clock-in-histogram.xlsx` endpoint following `DataExportService.trends` pattern.
3. Keys: `dashboard.peakClockIn*` family (≈8) + CSV.

**Tests:** BE histogram math on fixed fixture punches crossing DST-free zone; cap validation; FE renders bars sorted desc and empty-state.

**Acceptance:** security category visibly peaks at 06:00 while admins peak 08:30 on demo data.

---

## WP-09 🟠 First-Login "Enable Notifications?" Prompt

Full functional spec already written in `missing-todo.md` §16 — implement THAT. Technical pointers:

- Infra ready: `fe/src/app/core/notification-center/web-push.service.ts` (`enable()` triggers browser permission via SwPush; per-device registration POSTs to `/api/v1/notifications/push/subscriptions`).
- Prompt component: place in `core/shell/` next to logout dialog; reuse `.shortcut-overlay/.shortcut-dialog` classes for visual consistency BUT register it in the modal-stack fix from WP-13 (BUG-3) so Escape/tab behave.
- Device memory: `localStorage['bemo_push_prompt_v1'] = JSON {userId, answer:'enabled'|'later'|'never', askedAt: epochMillis}`; 'later' snoozes by comparing `askAt + 14d`.
- Show conditions (ALL): supported() && configured() && !subscribed() && Notification.permission==='default' && stored-answer-not-blocking && user just logged in (fire ~2s after shell init effect).
- Never blocks navigation; if permission==='denied' show inline hint in Settings instead.
- i18n keys: `auth.pushPromptTitle/Hint/Enable/NotNow/NeverAsk` (+CSV rows). DOM tests: shows once; snooze respected (fake timers or injectable clock); enable calls service; denied → never shows.

---

## WP-10 🟠 Vertical-Aware User Creation & Job Templates

Spec in `missing-todo.md` §19. Junior implementation order:

1. **BE endpoint first:** `GET /api/v1/auth/users/menu-options` in `AuthController` area — source list = same constant the FE uses today, moved to backend as enum/config; filter by tenant active feature flags (`TenantFeatureService` exists) and attach `verticalTags:Set<String>` + `enabled:boolean`. Response records only.
2. **Template seed:** extend `TenantSetupService.getDefaultSpecsForVertical` catalogs (MEDICAL→Doctor/Nurse/Pharmacist/Lab Tech/Radiologist/Insurance Officer/Clinic Cashier; MANUFACTURING→Plant Supervisor/QC Inspector/Storekeeper/Maintenance Planner/Production Planner; RETAIL→Cashier/Merchandiser/Van-Sales Rep/Branch Manager; CIVIL→Site Engineer/Quantity Surveyor/Subcontractor Coordinator; SERVICES→Consultant/Support Agent; GENERAL→Accountant/HR Officer/Purchasing Officer). Store as data rows (V346 translations + template tables `user_role_templates` (vertical, code, name_key, menu_ids[], permission_prefixes[]) seeded idempotently ON CONFLICT DO NOTHING.
3. **Endpoint:** `GET /api/v1/auth/users/role-templates?vertical=` returns templates; applying happens CLIENT-side by pre-checking boxes (simplest) — no magic backend apply in v1.
4. **FE:** users page replaces static `USER_MENU_OPTIONS` import with fetched catalog (fallback to constant when endpoint fails — keep tests stable); add "Job template" select in add-user dialog; choosing it checks matching menus + selects suggested policy group; everything stays editable.
5. Gray-out disabled menus with tooltip key `users.menuNotEnabledForVertical`.
6. Tests: BE filtering by flags; FE template applies expected checkbox set; fallback path still works offline.

---

## WP-11 🟡 Employee Expense Claims (Odoo parity)

1. V347 tables: `expense_claims` (id, app_id, employee_id, category MEAL/TRANSPORT/LODGING/SUPPLIES/OTHER, spent_on LocalDate, amount, currency default EGP, description, receipt_attachment_name/content_type/size nullable — copy REM-005 attachment column trio, status DRAFT/SUBMITTED/APPROVED/REJECTED/REIMBURSED, approver_id, decided_at, reimbursement_payrun_id NULL) + evidence `expense_claim_events` optional.
2. Endpoints `/api/v1/expenses`: CRUD own (employee sees only theirs — enforce `employee.userId == principal` unless role HR), `POST /{id}/submit|approve|reject`, `POST /{id}/reimburse` (finance) creating partner-ledger-style credit or flagging inclusion in next payroll run (choose ledger credit v1 — simpler, matches advances precedent).
3. Limits: per-policy max per category (property map `hr.expenses.limit.<CATEGORY>`, default none) → warn above limit, require HR approval anyway.
4. FE: new feature `features/expenses/` — my-claims list + new claim dialog w/ photo upload (reuse attachment util from operations), approvals inbox filter for HR, reimburse action for finance. Full menu protocol.
5. Codes: `EXPENSE_*` family (≥6) + translations. Tests: ownership isolation, state matrix, limit warning, reimbursement ledger entry.

---

## WP-12 🟡 Sales Targets & Commissions Engine (Daftra parity)

1. V348: `sales_targets` (id, app_id, scope REP/TEAM/BRANCH, target_ref_id, period YEAR_MONTH string 'YYYY-MM', metric REVENUE/QUANTITY, target_value) + `commission_rules` (id, app_id, name, basis INVOICE_TOTAL/COLLECTED, percent NUMERIC(5,2), min_amount, active, valid_from/to).
2. Achievement calc endpoint `GET /api/v1/sales/targets/status?period=` joining sales invoices per rep vs target (reuse sales quotation/invoice repos; COLLECTED basis joins receipts).
3. Commission statement endpoint per rep per month; post to payroll as bonus line OR partner-ledger payout — choose payroll-bonus integration reusing `PayrollExecutionService` allowance injection point.
4. FE: targets grid inside sales feature + per-rep progress bars + commission statement print/export.
5. Doctor commissions (§14.13) become just seeded rules on medical vertical later — note in README, don't special-case.

---

## WP-13 🟡 Fix Shortcut × Dialog Integration (BUG-1…BUG-8)

All defects verified in code — fix in ONE PR, order matters:

1. **DialogStateService (new, core/shell):** signals `modalDepth` incremented by `ModalDialogComponent.activateModal/deactivateModal` (it already keeps a static stack — refactor that static into the service; keep static as delegate for backward compat). Raw shell overlays (logout :292, action-center :357, quick-nav :401, help :462) call `acquireOverlay()/releaseOverlay()` too.
2. **BUG-1/2:** in `onGlobalShortcut` early-return when `dialogState.blocksShortcuts()` (= depth>0) EXCEPT allow Escape passthrough which modals handle themselves. This kills shortcuts-behind-modals AND the input-vs-button inconsistency.
3. **BUG-4:** clearChord() on (a) any overlay acquire, (b) `@HostListener('window:blur')`.
4. **BUG-5:** Escape precedence becomes explicit: DialogState exposes `topmost()`; only topmost consumer handles Escape (shell checks `dialogState.topmostIsShellPanel()`).
5. **BUG-6:** honor `data-no-autosubmit` on closest form in `submitFormOnEnter`.
6. **BUG-7:** toast on duplicate shortcut destination in shortcut-settings using existing notification service + key `shortcuts.duplicateBlocked` (+CSV).
7. **BUG-3/8:** migrate raw overlays onto focus-trap: extract `trapFocus/getFocusableElements` from ModalDialogComponent into shared `focus-trap.util.ts`; apply to shell panels; add `role="dialog"` + `aria-modal="true"` + `aria-labelledby` everywhere.
8. **Regression specs (required):** navigation NOT fired while modal open; chord cleared on blur; quick-nav suppressed when modal open; Enter skips marked forms; Escape closes topmost only.

---

## WP-14 🟡 Android Wrapper (Capacitor)

Follow `missing-todo.md` §23 plan. Junior concrete steps:

1. `cd fe && npm i @capacitor/core @capacitor/cli && npx cap init "Bemo ERP" com.bemo.erp --web-dir=dist/bemo-web` (check real dist output dir in angular.json first).
2. Config: `server.url` loaded at runtime from secure storage — first-launch screen asks for company URL (component `pages/server-picker` outside auth guard), validate via `GET /api/v1/i18n/ar-EG` reachability ping.
3. Plugins: `@capacitor/push-notifications` (FCM — wire token into existing push-subscription endpoint with `platform:'ANDROID'` field; BE: accept optional platform column V349), `@capacitor-community/camera` (selfie punch → new endpoint `POST /api/v1/attendance/selfie-punch` storing evidence like imports, geolocation optional), `@capacitor-community/barcode-scanner` (fills barcode lookup search), `@capacitor-app` share sheet for payslip PDF, Android back-button → `App.addListener('backButton')` mapping to `Location.back()` except on root.
4. Offline outbox v1: queue failed POSTs of selfie-punch in IndexedDB with `operationId` UUID; retry on `online` event; dedupe server-side via existing idempotency.
5. Play release: signed AAB, internal testing track; versioning `versionName = fe package version`.
**Acceptance:** backgrounded FCM arrives; airplane-mode selfie punch syncs once online (no duplicates after 3 retries); camera scan fills lookup; back button exits only at root.

---

## WP-15 🟢 Medical CLINIC MVP Slice (first shippable medical revenue)

Deliberately minimal — sellable to a single-doctor clinic in 3 weeks:

1. Patient PMI-lite: `patients` table (V350) — mrn sequence per tenant (copy employee-code-sequence pattern!), national_id (parse birthdate/gender from Egyptian 14-digit format — pure function + tests), phones, gender, birth_date, notes, allergies_text. CRUD + duplicate search by phone/national_id.
2. Visit + queue: `clinic_visits` (patient_id, doctor_employee_id, visit_date, status WAITING/IN_ROOM/DONE/CANCELLED, chief_complaint, diagnosis_icd nullable, fee_charged, insurance_covered default 0). Queue board page: waiting list with "call next" big buttons (token = visit seq of day), TV-friendly fullscreen route.
3. e-Rx-lite: prescriptions as structured lines on visit (drug_name free-text v1, dose, frequency, duration) + printable Arabic Rx (print CSS, clinic header).
4. Billing: visit close → invoice line into EXISTING sales/invoicing OR treasury receipt (choose simpler: treasury cashbox receipt V290 pattern) + optional co-pay split when patient has insurer ref.
5. Doctor commission: single % per doctor on visit fee → month statement (feeds WP-12 engine later; v1 = simple report + manual payroll bonus note).
6. Vertical gate: whole feature hidden unless tenant vertical==MEDICAL or feature flag `medical.enabled` (entitlement infra).
7. Full A.4 menu registration for `patients`, `clinicQueue`; PBAC groups already provisioned (Clinic Administrator/Medical Receptionist exist in TenantSetupService — reuse!).
**Hospital extras (ADT/beds/LIS/pharmacy) stay OUT until clinic ships.**

---

## WP-16 🟢 Agri-Export Documentation Pack

1. Shipment entity: `export_shipments` (contract/customer from parties, container_no, booking_no, ACID number field, port_of_loading/discharge, ETB/ETA dates, status). Lines = packhouse lot references (link inventory lots).
2. Doc generation: printable COO + packing-list + phytosanitary application sheet from shipment data (server-side HTML → reuse Excel exporter for xlsx; PDF later).
3. Compliance registers: pesticide/MRL application log per lot (chemical, dose, date, pre-harvest interval days → auto earliest-safe-pickup date + alert when violated).
4. Proceeds tracker: expected FX amount vs realized (manual entry v1; bank-feed later) + days-outstanding aging.
5. CargoX/Nafeza = manual-number fields v1 with help tooltips; API integration is a separate future WP.
Gate behind `agri.enabled` entitlement; menus `exports`, `lots`.

---

## WP-17 🟢 Manpower-Supply Revenue Side

The contractor workforce module covers deployment+wages (cost side). Missing: billing clients.

1. `client_billing_periods` (client_party_id, month, status OPEN/INVOICED): auto-collect deployed workers × attendance-approved days × agreed day-rate (rate table `client_worker_rates` per client×worker_category effective-dated — copy ScheduleRule effectiveFrom/To pattern).
2. Generate draft invoice lines per worker/category; review screen with variance vs prior month; confirm → existing supplier-invoice-like customer invoice + ledger debit (mirror of contractor settlement flow, reversed direction — reuse its posting helpers).
3. Margin report: client billed vs worker wage cost per period.
Menus inside workforce workspace; permission `workforce.clientBilling`.

---

## WP-18 🟢 Small Tech-Debt Tasks (can interleave between WPs)

- **GraalVM launchers:** add `start-backend-graal.bat`/`.sh` setting `JAVA_HOME` to detected GraalVM path (registry/env probe), falling back with clear message; document native-image build command in be README. Do NOT attempt native compile in CI.
- **CSV-ID generator:** `be/tools/gen-translations-csv.py` taking a YAML of key→{ar,en} and emitting sequential-id CSV rows starting after latest existing id for that changeset (prevents the duplicate-PK class of bugs hit in V126/V146).
- **Cache expansion:** annotate dashboard aggregation service methods + access-catalog lookups with `@Cacheable` mirroring TranslationService config; add TTL via caffeine config property `hr.cache.ttl-seconds` default 300. Measure before/after with one integration test asserting repository not called twice.
- **Dynamic IDs note:** do NOT try to remove explicit PKs from existing shipped CSVs (Liquibase loadData needs them); generator above fixes future ones only.

---

# PART C — SUGGESTED ORDER FOR ONE JUNIOR DEV

1. WP-06 (smallest, teaches reports/i18n/menu flow end-to-end) → 2. WP-01 → 3. WP-02 (same domain, builds confidence) → 4. WP-09 (pure FE, fast win) → 5. WP-13 (FE architecture lesson) → 6. WP-08 → 7. WP-07 → 8. WP-03 → 9. WP-10 → 10. WP-04 (biggest P0) → 11. WP-11 → 12. WP-05 → 13. WP-14 → 14. WP-15 → 15. WP-16/17/12 → 16. WP-18 sprinkled anytime.

Rule: if a WP blocks >1 day, STOP and write the blocker into `docs/` notes + ask; never silently improvise around a failing gate.
