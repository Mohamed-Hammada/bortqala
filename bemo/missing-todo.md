# Missing TODOs — Extracted from `full.md` *(updated with completion status)*

> Scope: actionable requirements only. Lines ~128–1083 of `full.md` are competitor marketing posts
> (HUNT ERP, Gates ERP, LINEHR, Digora, Value Plus, Kayanac, Parsynex…) kept as **reference/inspiration only**.
>
> Status legend: `[x]` done · `[~]` partial · `[ ]` not started.
> Percentages verified against source code, Liquibase migrations (`v41`→`v340`), and test evidence in `docs/TEST_EVIDENCE.md`.

## Progress Summary

| # | Section | Done |
|---|---------|------|
| 1 | Attendance & Time Tracking ✅ | **100%** (peak-hour shipped) |
| 2 | Categories & Classification Engine | **≈ 94%** |
| 3 | Vacations | **≈ 93%** |
| 4 | Reports & Exports | **≈ 75%** |
| 5 | Workers Module | **≈ 95%** |
| 6 | Loans / Advances ✅ | **100%** (WP-07 deduction-policy switcher shipped) |
| 7 | Suppliers & Commercial Inventory | **≈ 93%** (installments shipped) |
| 8 | Users, Roles & Menu Management | **≈ 65%** |
| 9 | Dashboards | **≈ 90%** |
| 10 | Settings & UX Polish | **100%** |
| 11 | Technical Debt | **≈ 56%** |
| 12 | Documentation | **100%** |
| 13 | Imagined Features (uncommitted scope) | **≈ 83%** |
| 14 | 🏥 Medical Vertical Pack — Hospital *(proposed)* | **≈ 3%** (flag scaffold only) |
| 15 | 🩺 Medical Vertical Pack — Clinic *(proposed)* | **≈ 3%** (flag scaffold only) |
| 16 | 🔔 First-Login Notifications Enablement ✅ **SHIPPED** | **95%** (VAPID keys = deployment-only) |
| 17 | 🚀 Market Best-of-Breed Optimizations *(proposed)* | **≈ 20%** |
| 18 | 🇪🇬 Egyptian Market Vertical Roadmap *(proposed)* | **≈ 15%** |
| 19 | 👤 Vertical-Aware User Creation & Role Templates *(proposed)* | **≈ 25%** |
| 20 | 🥊 Competitor Parity — Daftra & Odoo *(researched Aug 2026)* | **≈ 38%** (fixed assets + settlement discount + loans deduction policy shipped) |
| 21 | ⌨️ Shortcut & Dialog UI Issues ✅ **FIXED** (7 real defects; BUG-7 already shipped) | **100%** |
| 22 | 🔧 Incomplete Flows Register *(consolidated)* | **—** |
| 23 | 📱 Android Wrapper App ✅ (repo side; APK build parked externally) | **80%** |
| | **Overall (committed scope §1–12)** | **≈ 84%** |

---

## 1. Attendance & Time Tracking (Core HR) — 87%
- [x] **Configurable attendance start time** — summer months 08:00, winter months 09:00 (global configuration). `(100%)`
  - ✅ Done: `ScheduleRule` entity (`be/src/main/java/com/bemo/hr/employee/domain/ScheduleRule.java`) with `effectiveFrom`/`effectiveTo` date ranges, per-category start/end times, grace minutes. UI hint ships it: *"Add summer, winter, or future changes without code changes"*.
  - ⚠️ Missing: nothing.
- [x] **Per-category working hours** — Admins 8h, Security 12h, Accountants 10h. `(100%)`
  - ✅ Done: schedule rules scoped per category (`scope=CATEGORY`, `scopeCategoryId`) + `expectedMinutesOverride`; payroll `PayrollCalculationPolicy.workingHourDivisor` per period.
  - ⚠️ Missing: nothing.
- [x] ~~⚠️ MISSING FEATURE: schedule per month~~ — check-in/out changing mid-month (e.g., July 1–12 at 07:00, rest at 09:00). `(100%)`
  - ✅ Done: covered by overlapping date-range `ScheduleRule`s (`appliesOn(LocalDate)` picks the rule valid for that day), so any day-level change within the month works without code changes.
  - ⚠️ Missing: nothing.
- [x] **Fingerprint device integration** — import device report, link names to categories. `(100%)`
  - ✅ Done: biometric Excel import pipeline + multi-vendor Device Hub (Session 13: ZKTeco/Suprema/Hikvision/Virdi/Honeywell…, 8 suppliers / 55 routes, `biometric_device_integrations` V145) + fingerprint↔employee linking (`employees.fingerprintNotLinked` flag).
  - ⚠️ Missing: live container runtime validation of device-hub needs Docker in deployment environment.
- [x] **Daily time account per employee** — compute time per employee × day. `(100%)`
  - ✅ Done: `DailyAttendanceResult` (worked/expected/effective/overtime/late minutes, punchCount, first/last punch) powering review, payroll snapshots, and exports.
  - ⚠️ Missing: nothing.
- [~] **Missed-punch rule** — user-defined categories where ≥1 fingerprint/day = attended. `(85%)`
  - ✅ Done: `SINGLE_PUNCH` daily status, configurable exception scoring (`missingPunchScore`, `singlePunchScore` in `AttendanceExceptionService`), manual-entry conversion, bulk decisions.
  - ⚠️ Missing: a first-class per-category toggle "1 punch counts as attended" — currently achieved via exception thresholds + reviewer decision rather than an automatic per-category rule.
- [x] **Bulk anomaly detection** — most-of-category-no-record ⇒ suspect outage; ask absence-vs-presence and persist. `(100%)`
  - ✅ Done: `ReportingService.java:700` flags groups where ≥50% have `NO_PUNCH`; `POST /reports/{id}/bulk-decision` (idempotent) + `PUT /{id}/downtime-decision` persist ABSENCE/OFFICIAL_HOLIDAY/INDIVIDUAL_REVIEW answers (Session 1).
  - ⚠️ Missing: nothing.
- [x] **Zero-fingerprint employees** — list them; user decides خصم or normal day. `(100%)`
  - ✅ Done: `NO_PUNCH` status rows in explorer/report filters + same bulk-decision path records the deduction/normal-day outcome.
  - ⚠️ Missing: nothing.
- [ ] **Peak clock-in analytics** — around what hour each category clocks in. `(0%)`
  - ✅ Peak-hour aggregation shipped (WP-08): `GET /api/v1/dashboard/clock-in-histogram` hourly buckets per category in `hr.company-zone`, dashboard card with category filter + legend + Excel export (`/api/v1/exports/clock-in-histogram.xlsx`); V342/V352/V353 translations; zone-shift + export specs green (2026-08-25).

## 2. Categories & Classification Engine — 94%
- [x] **Categories user-created, never hardcoded**; loaded from actual existing ones. `(100%)`
  - ✅ Done: dynamic attendance/labor categories CRUD (`categories` feature, worker categories too); all pickers load from DB.
  - ⚠️ Missing: nothing.
- [x] **Traffic-light rules engine** (🟢 clean, 🟡 N single-punch days, 🔴 other issues). `(100%)`
  - ✅ Done: 🟢🟡🔴 KPI cards on the report-review page + exception engine scoring (single-punch days counted per `DashboardService.java:88`).
  - ⚠️ Missing: final yellow/red thresholds still await stakeholder confirmation (configurable, defaults shipped).
- [x] **Decision per group always scoped under the employee's own labor category**. `(100%)`
  - ✅ Done: bulk decisions apply per filtered category/status group; audit-logged.
  - ⚠️ Missing: nothing.
- [x] **Bulk actions button** on the attendance report page. `(100%)`
  - ✅ Done: bulk-decision + downtime-decision endpoints execute once for a whole category/status instead of one-by-one.
  - ⚠️ Missing: nothing.
- [~] **Extra heuristic options** for easier category detection. `(70%)`
  - ✅ Done: heuristic candidate scoring engine (early-arrival threshold, max-shift, missing-punch & single-punch weights).
  - ⚠️ Missing: additional domain heuristics beyond the current five signals; open to brainstorm per original note.

## 3. Vacations — 93%
- [~] **Vacation default interpretation per category** — no-show ⇒ vacation by default. `(80%)`
  - ✅ Done: leave-request/entitlement module (V299–V301) + `OFFICIAL_HOLIDAY` bulk decision type.
  - ⚠️ Missing: an *automatic* default rule "no record ⇒ treat as vacation for category X" — currently reviewer-applied, not auto-classified.
- [x] **Ask once during report generation, then never again for that day**. `(100%)`
  - ✅ Done: decisions persist on `DailyAttendanceResult` (versioned); previously decided days render as decided and aren't re-asked.
  - ⚠️ Missing: nothing.
- [x] **Vacation quotas/policies per category**. `(100%)`
  - ✅ Done: leave balances/accruals & entitlements per employee/category (roadmap item 18, V299–V301).
  - ⚠️ Missing: nothing.

## 4. Reports & Exports — 75%
- [x] **Reports page with sections; select month(s)/year**. `(100%)`
  - ✅ Done: custom report ranges + pay-cycle presets + multi-month trend views.
  - ⚠️ Missing: nothing.
- [ ] **Show only months that have no prior generated report** (avoid duplicates/regeneration conflicts). `(0%)`
  - ❌ Missing entirely: reports are ad-hoc range queries; there is no "generated report period" registry to gray-out already-produced months. Needs a `generated_report_periods` table + UI filter.
- [x] **Clean tables/grids everywhere** (Excel-native users). `(100%)`
  - ✅ Done: consistent table-card grid components + pagination across pages.
  - ⚠️ Missing: nothing.
- [x] **Export to Excel everywhere** — attendance, deductions, payroll, inventory…. `(100%)`
  - ✅ Done: Apache-POI exporter covering attendance, payroll (Arabic filenames), budget, trends, procurement, GL, and more (`DataExportService` + per-module controllers).
  - ⚠️ Missing: nothing material.

## 5. Workers Module (Daily-Wage) — 95%
- [x] **Workers organized under a contractor**; contractor supplies a batch. `(100%)`
  - ✅ Done: labor requests → contractor workforce → worker assignment chain; project attribution sync (V322–V323).
  - ⚠️ Missing: nothing.
- [x] **Contractor settled every 15 days** (2 periods/month). `(100%)`
  - ✅ Done: settlement-period lifecycle with 15-day cycle, lock, review/approval, journal posting (Sessions 3+).
  - ⚠️ Missing: nothing.
- [x] **Worker attendance entered manually** (no fingerprint). `(100%)`
  - ✅ Done: manual worker attendance entry feeding the lock/settlement flow.
  - ⚠️ Missing: nothing.
- [~] **Worker daily wage manual + hidden/auto-calculated cells replicating Excel logic**. `(90%)`
  - ✅ Done: settlement lines + adjustments replicate hourly-style math from daily wage inputs (advances, deductions, overtime-style proration).
  - ⚠️ Missing: exact parity check against the owner's Excel sheet — the sheet was never supplied (external dependency).
- [x] **Dedicated menu/list for workers + worker categories**. `(100%)`
  - ✅ Done: workforce area with worker categories CRUD, registered in menus/permissions protocol (V37-style allowed_menus updates).
  - ⚠️ Missing: nothing.
- [x] **Worker calculations include deductions** (hourly-style math from daily wage). `(100%)`
  - ✅ Done: contractor settlement lines compute credit/debit entries posted to partner ledger + supplier invoice link + payment.
  - ⚠️ Missing: nothing.

## 6. Loans / Advances (سلف) — 100%
- [x] **Long-term loans**: installment value + repayment duration. `(100%)`
  - ✅ Done: `WorkforceAdvanceInstallment` entity + `WorkforceAdvanceService` scheduling installments against a balance.
  - ⚠️ Missing: nothing.
- [x] **Flexible deduction scenarios** — automatic OR manual "Apply Deduction", monthly OR 15-day depending on category. `(100%)`
  - ✅ Done: payroll auto-deducts the active monthly installment per employee (`PayrollService.calculateEmployeePayrollDeduction`, frozen into snapshots); workers get advances/deductions via 15-day settlement adjustments; settlement reversal reverses advances. **WP-07 (V350)**: explicit admin switcher — resolved deduction policy (EMPLOYEE → EMPLOYEE_CATEGORY → GLOBAL → defaults AUTO+MONTHLY) gates payroll auto-collection (`isManualDeductionPolicy` skip + ADVANCE_DEDUCTION explanation evidence JSON), manual apply endpoint `POST /api/v1/workforce/advances/apply-deduction` idempotent per (employee, period), settings business-tab policy card + employees toolbar action gated on MANUAL.
  - ⚠️ Missing: nothing.
- [x] **Global setting AND per-individual-employee override**. `(100%)`
  - ✅ Done: per-employee advance plans with individual installment amounts/dates work today. **WP-07**: global default-deduction-policy setting (GLOBAL scope card) with EMPLOYEE_CATEGORY/EMPLOYEE exception overrides via the existing versioned `workforce_advance_policies` store; resolver exposes effective source/mode/cadence (`GET .../resolved-policy`).
  - ⚠️ Missing: nothing.

## 7. Suppliers & Commercial Inventory Module — 89%
- [x] **Supplier types**: direct vs managed-through-responsible-partner. `(100%)`
  - ✅ Done: `DIRECT`/`MANAGED` managed-type restriction, responsible party required for managed (Session 1, V44 columns).
  - ⚠️ Missing: nothing.
- [x] **Goods-receipt (وارد) entry**: party/responsible, item, qty, UoM (pieces/kg/any). `(100%)`
  - ✅ Done: GRN flow (V45), supplier returns (V186), UoM per line, three-way matching with quantity variance tolerance.
  - ⚠️ Missing: nothing.
- [x] **Currency section** (EGP, USD, EUR…). `(100%)`
  - ✅ Done: currency CRUD + Frankfurter online reference-rate hints (V150–V151) with scheduled refresh.
  - ⚠️ Missing: live Frankfurter fetch needs internet/Docker validation in deployment env.
- [~] **Purchase invoice + document number mandatory for direct suppliers, optional via partner**. `(70%)`
  - ✅ Done: supplier invoices capture invoice/document numbers; direct-vs-managed party model exists.
  - ⚠️ Missing: conditional *mandatory-by-supplier-type* validation rule on invoice creation (currently uniformly optional).
- [x] **Payment flexibility**: full, partial, or installments. `(100%)`
  - ✅ Done: supplier payments linked to unpaid invoices with first-class partial tracking (`paidAmount`/`outstandingAmount`/`PARTIALLY_PAID`, settlement discount V343) + installment plans (V344 `supplier_payment_plans`, equal monthly split, auto-mark paid via normal payment flow, FE plan dialog).
  - ⚠️ Missing: nothing.
- [x] **Free-text notes** on invoice/receipt. `(100%)`
  - ✅ Done: notes columns end-to-end.
  - ⚠️ Missing: nothing.
- [x] **Money-settlement discount** — owed 1000, settled for less; record original vs discounted. `(100%)`
  - ✅ Done: invoice-level discount field (discount/tax/net) persisted and exported; *payment-time* settlement shipped (V343 discount column + partner-ledger entries; V348 `original_due` snapshot, finance-role guard `PROC_SETTLEMENT_DISCOUNT_FORBIDDEN`, GL subledger event `SUPPLIER_SETTLEMENT_DISCOUNT`, export columns).
  - ⚠️ Missing: nothing.
- [x] **Quantity discount** — damaged/short goods recorded as quantity deduction. `(100%)`
  - ✅ Done: GRN lines carry accepted/rejected/**deducted** quantities; supplier returns module (V186).
  - ⚠️ Missing: nothing.
- [x] **Item categories (CRUD)** — خام, مستلزمات إنتاج, cartons…. `(100%)`
  - ✅ Done: `ItemCategory` entity + REST CRUD (`/operations/categories`) + smart-import catalog seeding.
  - ⚠️ Missing: nothing.
- [ ] ⏳ **Owner supplier Excel sheet** — external input, still not received. `(blocked)`
  - ⚠️ Missing: the stakeholder file itself; import templates (`SmartImportCatalog`) already prepared to consume it.

## 8. Users, Roles & Menu Management — 65%
- [x] **Admin creates users, controls visible menus + selectable roles**. `(100%)`
  - ✅ Done: users page menu toggles, multi-role authorization, PBAC policy groups with branch/cost-center scoping (Sessions 12), permission matrix + preset tiers.
  - ⚠️ Missing: nothing.
- [~] **Grouped menus UI for the add-new-employee form** (organize ~20–30 lists into groups, take whole/partial group, preview layout). `(30%)`
  - ✅ Done: sidebar itself is grouped into workspace sections; PBAC lets admins grant whole modules or granular permissions.
  - ⚠️ Missing: the employee-create form specifically still renders its many dropdown sections flat — no group-based picker with layout preview for form fields.

## 9. Dashboards — 90%
- [~] **Detailed dashboard per feature/module with drill-down links**. `(85%)`
  - ✅ Done: Executive Analytics Center + enterprise KPI registry (V319–V321); portfolio dashboard for projects (V282–V283).
  - ⚠️ Missing: not every module has its own dedicated drill-down dashboard yet (e.g., no standalone procurement/analytics mini-dashboards).
- [x] **Dashboards per section with floating/chart widgets surfacing key KPIs**. `(100%)`
  - ✅ Done: saved widget preferences, multi-period trends table + charts, KPI pills.
  - ⚠️ Missing: nothing.
- [~] **Every dashboard/screen supports export-to-Excel**. `(85%)`
  - ✅ Done: trends export, payroll export, budget export, generic data-export endpoints with Arabic filenames.
  - ⚠️ Missing: a few widget-level views (e.g., notification center, shortcut lists) lack direct export buttons.

## 10. Settings & UX Polish — 100%
- [x] **Show/hide "Favorite menus" and "Recently used" lists**. `(100%)`
  - ✅ Done: visibility toggles + configurable recent-item limit (max 20) + reset-favorites, all persisted to backend preferences (`settings.page.html:128`, entitlement `navigation.favorites.enabled`); applies instantly in shell.
  - ⚠️ Missing: nothing.

## 11. Technical Debt — 56%
- [x] **Fix licence module**, especially desktop. `(100%)`
  - ✅ Done: separate `license-app/` activation service + `OfflineLicensingService`/`TenantLicenseCertificate` (hashed keys, validate/install/revoke endpoints) + Tauri `desktop/` distribution bundling backend+Postgres.
  - ⚠️ Missing: end-to-end activation run on a physical offline machine (environmental validation).
- [~] **Use Spring Boot Cache wherever beneficial**. `(40%)`
  - ✅ Done: `@EnableCaching` active; translation bundles cached (`TranslationService` `@Cacheable translationBundles`).
  - ⚠️ Missing: caching not applied to other hot read paths (dashboards, catalogs, permission evaluation).
- [ ] **Remove hardcoded IDs in CSV files — make them dynamic**. `(0%)`
  - ❌ Missing: all Liquibase translation CSVs still use explicit sequential PK ids (`v119-001`…) and seeded business rows carry fixed UUIDs; no generator makes these dynamic.
- [~] **Translation workflow: notes/skills metadata + Python script checking keys/duplicates**. `(90%)`
  - ✅ Done: `be/tools/check-error-codes.py` (575/575 codes), `be/tools/check-translation-catalog.py` (12k+ rows defect-free), fe `check:i18n` (4538 keys) wired into verification routine.
  - ⚠️ Missing: the duplicate-check tooling is split Python(fe-side is Node `.mjs`); no single Python script covering *all* key sources, and no per-key metadata/notes store.
- [~] **Evaluate GraalVM + optional .bat/.sh launcher scripts**. `(50%)`
  - ✅ Done: `org.graalvm.buildtools.native` 1.1.5 plugin configured with `graalvmNative` block in `be/build.gradle`.
  - ⚠️ Missing: no optional GraalVM launcher scripts ship alongside the standard start scripts; native-image build not exercised/validated.

## 12. Documentation — 100%
- [x] **Update README in every package/module**. `(100%)`
  - ✅ Done: 100+ READMEs across `be/modules/*` (incl. device-hub vendor packages), backend layer folders (api/application/domain/infrastructure), `fe/src/app/features/*`, core/shell/auth, tests, scripts.
  - ⚠️ Missing: nothing.
- [x] **Add business description section to README**. `(100%)`
  - ✅ Done: root README states platform scope (multi-tenant operations: HR/attendance, contractor workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications) + applications map.
  - ⚠️ Missing: nothing.

---

## ⏳ Inputs Still Needed From Stakeholder
1. Excel sheet with worker wage/calculation logic — **still missing** (system ships equivalent calculation; parity check impossible without it).
2. Excel sheet with full supplier details — **still missing** (import templates ready).
3. Final traffic-light thresholds (yellow/red) — **still missing** (working defaults configurable in exception settings).
4. ~~Confirmation of deduction-scenario defaults (auto vs button; monthly vs 15-day)~~ ✅ **RESOLVED (WP-07)** — shipped with safe defaults (AUTO + MONTHLY, zero behavior change for existing tenants) and admin switcher in Settings; stakeholder can now *reconfigure* rather than decide.

---

## 13. 💭 Imagined Features (mined from competitor marketing — NOT committed scope) — 82%

> Largely delivered by the 30-feature roadmap (`docs/BEMO_ROADMAP_IMPLEMENTATION_STATUS.md`, 30/30) plus Epics 1–5.

### 13.1 Accounting & Finance — 100%
- [x] ⭐ Automatic journal entries from operational documents. `(100%)` — settlements, invoices, payments post debit/credit partner-ledger + journal entries automatically. Missing: nothing.
- [x] Period closing (قفل فترات). `(100%)` — fiscal-periods feature consolidated into the *Finance Reports & Close* workbench. Missing: nothing.
- [x] Banks, treasuries & cheques (اوراق قبض/دفع) + reconciliation. `(100%)` — treasury cashbox register + commercial cheque management (V290–V291), banks feature. Missing: nothing.
- [x] Cost centers + P&L per cost center. `(100%)` — cost-center engine + direct cash-flow statement (V294–V295), multi-dimensional GL (V288–V289), PBAC branch/cost-center scoping. Missing: nothing.
- [x] Financial statements: trial balance, income statement, balance sheet, cash-flow. `(100%)` — `TrialBalanceReportService`, `FinancialStatementsReportService` (balance sheet/income statement/cash flow with comparative period + reconcile flag, F-004). Missing: nothing.
- [x] Credit limits + aging/debt reports (مديونيات). `(100%)` — customer/party credit profiles with hold + 80%-warning, AR/AP aging (V292–V293). Missing: nothing.
- [x] Egyptian e-invoice (ETA) compliance. `(100%)` — ETA e-invoicing engine (V308–V310). Missing: live ETA submission credentials/validation in production env.
- [x] Budgeting vs actual + variance alerts. `(100%)` — budget control & encumbrance (B-1, V122–V125) with blocking/warning modes. Missing: nothing.

### 13.2 Suppliers / Purchasing — 80%
- [ ] ⭐ OCR + AI supplier-invoice scanning. `(0%)` — no OCR/AI extraction anywhere. Missing: photo→GRN pipeline (vision model or OCR service), review screen, confidence handling.
- [x] Supplier ledger/statement of account (كشف حساب مورد). `(100%)` — party statements/subledgers (V292–V293) + partner ledger entries. Missing: nothing.
- [x] Purchase requests → approval workflow before PO. `(100%)` ✅ **DONE (WP-03)** — V345 adds `purchase_requests`/`purchase_request_lines` with DRAFT→SUBMITTED→APPROVED/REJECTED lifecycle, `/api/v1/purchase-requests` endpoints, one-shot convert-to-PO that finally populates `purchase_orders.purchase_request_id` + `converted_po_id`, FE "طلبات الشراء" tab with create/edit + supplier-picked conversion. Missing: nothing functional; optional future polish = wire approvals into the generic approval-engine inbox (needs decision callbacks).
- [~] Attachments per transaction (invoice photo, delivery note). `(60%)` — operations stock movements support attachments ≤5 MB (REM-005). Missing: attachments on procurement documents (PO/GRN/invoice) and HR documents.
- [x] Supplier scorecard (quality complaints/discount history). `(100%)` — sourcing & supplier performance scorecard (V284–V285). Missing: nothing.

### 13.3 Inventory & Production — 90%
- [~] Real-time stock levels + continuous-inventory valuation (جرد مستمر). `(75%)` — warehouse analytics, aging buckets, negative/low-stock alerts (V286–V287). Missing: **B-5 continuous valuation engine (cost layers/FIFO-weighted) — explicitly parked** in roadmap Part B.
- [~] Barcode scanning for stocktake/counting. `(65%)` — barcode master data + aliases + `/barcode-lookup` API + stock-count Excel import validation. Missing: interactive scan-as-you-count screen (handheld/camera continuous counting UI).
- [x] Shortage/reorder alerts + slow-moving/dead-stock report (الراكد). `(100%)` — reorder point/quantity with shortage computation + dead-stock flags + aging buckets. Missing: nothing.
- [x] Stock movement audit trail (من/إلى، المستخدم، السبب). `(100%)` — REM-005 document references (PO/receipt/delivery-note/invoice/voucher) + required-reference enforcement + audit logs. Missing: nothing.
- [x] BOM + fixed recipes (خام → منتج تام) with per-unit cost. `(100%)` — manufacturing routings, work centers, production BOMs (V296) + WIP costing. Missing: nothing.
- [x] Production orders with stages, output/waste tracking, worker assignment. `(100%)` — all-or-nothing production issue/receipt, variance closing, QC inspections. Missing: partial issue/receipt intentionally out of scope (documented decision).

### 13.4 Projects / Contracting — 80%
- [x] Project WBS tree (بنود رئيسية/فرعية). `(100%)` — hierarchical WBS + BOQ + cost codes, depth limits, cycle detection (V269–V271, hardened Session 18). Missing: nothing.
- [x] Daily site work logs. `(100%)` — DPR field reporting (V272–V273). Missing: nothing.
- [x] Interim/final payment certificates (مستخلصات) with retentions/insurance/tax deductions. `(100%)` — IPC claims + retention ledger (V278–V279). Missing: nothing.
- [ ] Site custody register (عهد المواقع) linked to warehouse movements. `(0%)` — treasury cashbox is company-level only. Missing: custody-per-site entity, issuance/return flow, warehouse-movement linkage.
- [x] Tender registry + deviation analysis. `(100%)` — tender competition, weighted evaluation matrix, planned-vs-actual deviation (V276–V277). Missing: nothing.

### 13.5 HR / Workforce Extensions — ≈75%
- [~] ⭐ Employee self-service mobile app (selfie attendance, leave/loan requests, payslip view). `(≈30%)` — WP-14 shipped the selfie-attendance core: `features/selfie-punch` camera page + `POST /api/v1/attendance/selfie-punch` (server-stamped time, idempotent operationId, ≤2MB evidence) + offline outbox. Missing: ESS role surface (leave/loan/payslip flows), liveness check, geofencing.
- [x] Full payroll run per category consuming attendance + loans + deductions. `(100%)` — Egyptian statutory payroll with state machine DRAFT→POSTED→PAID, frozen snapshots, advances deducted, SoD maker/checker (F-002/F-003). Missing: PostgreSQL concurrency proof (F-001 blocked on Docker).
- [x] Employee contracts/documents repository with expiry alerts. `(100%)` — contract lifecycle + expiry alerting (V297–V298). Missing: nothing.
- [x] KPI/performance reports per employee & category. `(100%)` — KPI appraisals + evaluation cycles (V303–V305). Missing: nothing.
- [~] Shift/roster planning per site + overtime calculation rules. `(85%)` — monthly shift-roster import wizard, schedule rules, configurable overtime multiplier/divisor policies. Missing: visual drag-drop roster planner UI; site-level (vs category-level) roster assignment.
- [~] WhatsApp notifications to employees (payslip ready, loan due, leave approved). `(35%)` — CRM omnichannel supports WHATSAPP/FB conversation channels with chatbot (V314–V316); business notification center exists. Missing: outbound WhatsApp gateway integration for HR events to employees (template registration, provider account).

### 13.6 Platform / Technical Ideas — 95%
- [x] Multi-branch & multi-company on one instance. `(100%)` — branches/company org units + multi-company consolidation with intercompany elimination (V317–V318). Missing: nothing.
- [x] Cloud/web access from any device — responsive/PWA. `(100%)` — Angular responsive UI + `ngsw` service worker. Missing: nothing.
- [~] Periodic automated backups + restore drill. `(90%)` — DR backup + system health modules (V326–V327). Missing: executed restore-drill evidence in target environment.
- [x] Role-based dashboards (owner sees treasury+banks+projects in one screen). `(100%)` — saved widget preferences per role + executive analytics center + portfolio dashboard. Missing: nothing.
- [x] Report period-comparison views (month vs last month vs same month last year). `(100%)` — multi-period trends up to 24 months + comparative previous-period on cash flow. Missing: nothing.
- [x] Approval workflows engine (who approves what, thresholds). `(100%)` — configurable workflow definitions + pending-tasks inbox + dynamic multi-stage approvals (V324–V325). Missing: nothing.
- [x] Audit log of every user action. `(100%)` — audit-log search page + break-glass audit trails (V324–V325). Missing: nothing.
- [x] Arabic-first UI with i18n framework. `(100%)` — DB-backed ar-EG/en-US catalogs (4,538 verified keys, 0 hardcoded strings across 109 HTML + 221 TS files). Missing: nothing.

---

## 14. 🏥 Medical Vertical Pack — HOSPITAL (proposed scope) — ≈3%

> Foundation already in place: `BusinessVertical.MEDICAL` exists in `TenantSetupService` (feature-flag preset + auto-provisioned policy groups) — but **no medical business features exist yet**. Everything below is new scope.
> Repo rules apply: bilingual DB-backed i18n (ar-EG/en-US), `@TenantId` isolation, PBAC permissions + 4-part menu registration protocol, Liquibase versioned migrations, backend-owned calculations.

### 14.1 Patient Master Index (PMI) — the anchor entity
- [ ] Patient registration with auto MRN (medical record number), Egyptian national-ID parsing (auto-extract birth date/gender/governorate), duplicate detection & merge workflow.
- [ ] Patient profile: demographics, blood group, allergies (red-banner), chronic conditions, family/social history, documents & photos.
- [ ] Guardian/family linking (parent→child accounts, one payer for family).
- [ ] Reuse: PBAC scoping per branch (hospital branches), audit-log every chart access (break-glass view for sensitive records).

### 14.2 Admissions / Discharge / Transfer (ADT) + Bed Management
- [ ] Ward → room → bed hierarchy with live occupancy board (color-coded).
- [ ] Admit/discharge/transfer with mandatory discharge summary; bed turnover + ALOS analytics.
- [ ] Day-case vs inpatient flows; isolation-bed flags (infection control).

### 14.3 Appointments, Rosters & Queue
- [ ] Doctor roster/slot templates per clinic/day; online booking portal link; recurring appointments (dialysis/physio series).
- [ ] WhatsApp/SMS appointment reminders + no-show tracking + waitlist auto-fill (reuses CRM omnichannel channels!).
- [ ] Waiting-room token queue with TV display screen + "now serving" call button; avg wait-time KPIs.

### 14.4 EMR / Clinical Documentation
- [ ] Specialty SOAP note templates; vitals capture (BP/temp/pulse/SpO2/BMI auto-calc); pediatric growth charts (WHO percentiles).
- [ ] Problem list, allergy list with interaction flags, immunization record.
- [ ] Informed-consent forms per procedure with e-sign capture + retention policy.
- [ ] Attachments: labs/imaging/reports per visit (extend REM-005 attachment pattern).

### 14.5 e-Prescribing (Rx)
- [ ] Drug formulary catalog (trade/generic, strength, form); weight-based dose calculator (pediatrics).
- [ ] Drug–drug & drug–allergy interaction warnings (hard-block configurable); favorites per doctor; Arabic-printable Rx.
- [ ] Repeat/chronic prescriptions (3–6 months) with dispensing counter.

### 14.6 Pharmacy Module (links to existing Inventory!)
- [ ] Dispense against Rx (partial dispense allowed); substitution rules (generic-allowed flag); returns to stock.
- [ ] Batch + expiry tracking with FEFO picking (existing `InventoryItem.shelfLifeDays` helps) + near-expiry alerts.
- [ ] **Narcotics/controlled-substances register** — separate locked log, dual sign-off, MOH-audit-ready export.
- [ ] Pharmacy stock feeds central procurement/GRN flow that already exists.

### 14.7 Laboratory (LIS)
- [ ] Test catalog with panels/profiles; sample collection with barcode labels; worklists by department.
- [ ] Result entry with reference ranges by age/sex, validation (technologist → pathologist double-check), critical-value alert to doctor.
- [ ] Analyzer interface (ASTM/HL7) via device-hub pattern (Session 13 architecture fits perfectly); result trending graph per analyte.

### 14.8 Radiology (RIS-lite)
- [ ] Imaging orders + technologist worklist + structured report entry; DICOM viewer deep-link (embed OHIF viewer) or PACS URL field.

### 14.9 Operating Theater (OT)
- [ ] OT schedule board, pre-op checklist, anesthesia record, post-op notes.
- [ ] Implants/consumables charged from theater stock directly to patient bill.

### 14.10 Nursing
- [ ] MAR (medication administration record) with due/pass/refuse; intake/output fluid chart; nursing notes; vitals flowsheet.

### 14.11 Insurance & Claims (Egypt market)
- [ ] Payer catalog: HIO (هئة التأمين الصحي) + private networks (MedNet, AXA, MetLife, Bupa, Allianz…) + corporate agreements.
- [ ] Plans with coverage %, co-pay, annual limits, exclusions; **pre-authorization/approval-code capture** before procedures.
- [ ] Claim batch generation per payer, rejection management with resubmission cycle, aging of unclaimed amounts.

### 14.12 Billing & Revenue Cycle (links to Finance)
- [ ] Price lists per payer class (cash / insurer tier / corporate); service catalog per department.
- [ ] Packages (checkup, delivery, surgeries) with component services; split billing patient-vs-insurer-vs-corporate on one encounter.
- [ ] Deposits & refunds; unbilled-services sweep at discharge; auto journal entries into existing GL; ETA e-invoice compliance reuse.

### 14.13 Doctor Commissions (very common in EG clinics/hospitals)
- [ ] Commission % per doctor × service/procedure, monthly commission statement → payroll bonus OR supplier-style payment.

### 14.14 Telemedicine
- [ ] Video-visit link generation (Jitsi/Daily embed), e-prescription after remote consult, tele-visit billing code.

### 14.15 Compliance & Analytics
- [ ] GAHAR-accreditation document checklists; MOH licenses tracking with expiry alerts (**reuse contract-expiry alert engine**, V297–V298 pattern).
- [ ] KPIs: occupancy %, bed turnover, ALOS, revenue per doctor/specialty/service, no-show rate, lab TAT, patient satisfaction survey.

---

## 15. 🩺 Medical Vertical Pack — CLINIC (proposed scope) — ≈3%

> Lightweight subset of §14 for single/multi-doctor clinics; same MEDICAL feature flag, fewer modules enabled.

- [x] MEDICAL vertical scaffold (flags + default policy groups). `(done)` — `TenantSetupService`.
- [ ] Clinic PMI + walk-in fast registration (<30 seconds, minimal required fields). `(new)`
- [ ] Appointment calendar per doctor + daily session sheet printout. `(new)`
- [ ] Queue/token screen (shared with §14.3). `(new)`
- [ ] EMR-lite: chief complaint, diagnosis (ICD-10 picker), vitals, attachments. `(new)`
- [ ] e-Prescribing (shared with §14.5) without pharmacy stock (external pharmacy mode). `(new)`
- [ ] Lab/imaging orders routed to external labs with result attachment. `(new)`
- [ ] Visit billing + insurance co-pay + receipt printing; end-of-day cash close (reuse treasury cashbox V290–V291). `(new)`
- [ ] Doctor commission statement (shared with §14.13). `(new)`
- [ ] Dental add-on: odontogram/tooth chart with per-tooth treatment plans. `(new)`
- [ ] WhatsApp follow-up reminders (post-visit, next-dose, next-session). `(new)`

---

## 16. 🔔 First-Login "Enable Notifications on this Device?" Prompt — ≈70%

> Infra is DONE: `fe/src/app/core/notification-center/web-push.service.ts` has full VAPID web-push lifecycle (`enable()` triggers the browser permission dialog, per-device subscription registration at `/api/v1/notifications/push/subscriptions`, preference-scoped push approvals/payroll, test push, detach-on-logout, detach-all-on-revoke-sessions) + backend push endpoints. Settings page already offers manual toggles.
> **What's missing is exactly your requirement: nobody asks the user at first login.**

- [~] Web-push infrastructure (service worker, VAPID config endpoint, subscriptions, preferences, test send). `(100% of infra)`
  - ⚠️ Missing for production: real VAPID keys + push-service reachability validated in deployment environment (like Frankfurter/device-hub — Docker/network dependent).
- [x] **First-login permission prompt** — SHIPPED (`PushPermissionPromptComponent`, V341). `(100%)`
  - Trigger: after successful login, when ALL of: `WebPushService.supported()` true · `configured()` true (backend `/push/config.enabled`) · `!subscribed()` · `Notification.permission === 'default'` · not previously answered **on this device**.
  - Device-level memory: `localStorage['bemo_push_prompt_v1'] = { userId, answer: 'enabled'|'later'|'never', askedAt }`; "Not now" snoozes 14 days, "Never" hides permanently (per user × browser).
  - UX: modal reusing existing `shortcut-dialog` styles; actions **"🔔 Enable notifications"** (primary) / **"Not now"** (secondary) / small "Never ask on this device"; Enable → `WebPushService.enable(preferences)` then toast success pointing to Settings for per-category control.
  - Non-blocking: prompt appears ~2s after shell load, never blocks navigation; skip entirely if permission already `denied` (surface a hint inside Settings instead).
  - Repo compliance: all copy via `i18n.t('auth.pushPrompt*')` + Liquibase translations CSV (ar-EG/en-US); add DOM tests to `app-shell.component.spec.ts` (prompt shows once, snooze respected, enable calls service); no hardcoded strings (`check:hardcoded` gate must stay green).
  - Bonus tie-in: trusted-device concept — store device label shown in prompt ("Enable notifications on هذا الجهاز?"), later reusable for §17 security features (device management list).

---

## 17. 🚀 Market Best-of-Breed Optimizations (make the ERP optimum) — ≈20%

### A. AI & Intelligence
- [ ] ⭐ OCR supplier-invoice capture (photo → GRN) — biggest gap, flagged in §13.2. `(0%)`
- [ ] Cash-flow forecasting (ML on AR/AP aging + seasonality). `(0%)`
- [ ] Inventory demand forecasting + auto-reorder suggestions (reorder points exist; make them predictive). `(20%)`
- [ ] Expense/anomaly detection ("this invoice is 3× the 6-month average"). `(0%)`
- [ ] Natural-language report Q&A ("مبيعات الشهر الماضي؟") grounded on tenant data with permission checks. `(0%)`
- [ ] Smart collections scoring (which customers will likely pay late). `(0%)`

### B. Payments & Banking (Egypt-first)
- [ ] Payment-gateway collection links: Fawry / Paymob / InstaPay — pay-invoice-by-link on WhatsApp/email. `(0%)`
- [ ] Bank-statement import (CSV/MT940) + AI-assisted reconciliation matching to journal entries. `(0%)`
- [ ] Customer payment page (public, tokenized link, no login) showing balance & history. `(0%)`

### C. Communication & Engagement
- [ ] Official WhatsApp Business API templates for HR/AR events (payslip ready, invoice due, leave approved) — upgrade beyond CRM chatbot channel. `(35%)` — channel plumbing exists in CRM (V314–V316).
- [ ] Scheduled report delivery: email/WhatsApp any report on cron (daily/weekly/monthly PDF/XLSX). `(0%)`
- [ ] In-app announcement center: tenant admins broadcast rich banners/toasts per role. `(0%)`

### D. Security & Trust
- [ ] TOTP 2FA (authenticator app) + backup codes + forced-2FA roles. `(0%)`
- [ ] SSO: Google/Microsoft workspace sign-in for tenants. `(0%)`
- [ ] Trusted-device management page (list/revoke devices — pairs with §16 prompt and existing sessions-revoke-all). `(25%)` — revoke-all + push detach-all already exist.
- [ ] IP allowlisting per role (office-only finance access). `(0%)`
- [ ] Configurable password policy + session-timeout policy per tenant. `(0%)`
- [ ] Egypt PDPL (law 151/2020) toolkit: PII export/erase requests, consent registry, retention policies admin. `(0%)`

### E. Productivity & UX
- [ ] Global command palette (Ctrl+K): universal search across screens/documents/actions with keyboard navigation. `(0%)`
- [ ] Saved grid views: named filter+column presets per table, shareable per role. `(0%)`
- [ ] Inline bulk edit on grids (multi-select rows → change category/status at once). `(0%)`
- [ ] Tenant onboarding wizard: step-by-step first-run checklist (vertical setup → categories → employees → devices → opening balances) building on existing `BusinessVerticalSetupComponent`. `(40%)`
- [ ] Contextual help: per-screen tooltip/help icons with bilingual micro-guides + link to docs. `(0%)`

### F. Automation
- [ ] Recurring documents: standing POs, monthly rent invoices, template journals. `(0%)`
- [ ] Dunning ladder: automatic reminder escalation for AR aging buckets (15/30/60 days → email/WhatsApp). `(0%)`
- [ ] Background-jobs health dashboard: retry failed exports/pushes/imports with dead-letter view. `(0%)`

### G. Data, Integration & Extensibility
- [ ] Public REST API + webhooks (event out: invoice.paid, employee.created) with API-key management per integration. `(0%)`
- [ ] Custom report builder: drag-drop dataset explorer over existing repos → save/share reports. `(10%)` — trends/KPI registry give the data layer.
- [ ] E-commerce sync (Shopify/WooCommerce orders → sales module) for RETAIL vertical. `(0%)`
- [ ] Data-warehouse export (nightly Parquet/S3 dump) for BI tools. `(0%)`

### H. Quality-of-Life Finance Extras
- [ ] Multi-currency revaluation (unrealized FX gain/loss month-end run) — exchange-rate hints exist (V150–V151), posting run missing. `(30%)`
- [ ] Check printing layouts (Egyptian cheque formats) from treasury cheques register. `(0%)`
- [ ] Hijri calendar display toggle alongside Gregorian. `(0%)`

---

## 18. 🇪🇬 Egyptian Market Vertical Roadmap — ≈15%

> Already covered by shipped modules: retail POS (V311–V313), manufacturing, contracting/civil, education/tourism/customs-clearance/3PL (V328–V330), medical scaffold (§14–§15). Ranked below by fit with existing assets.

- [~] **Agri-export & packhouses** *(the owner's core business)*. `(40%)` — trade/inventory/procurement/payroll all exist.
  - ⚠️ Missing: farm-block & pesticide/MRL residue registers, GlobalG.A.P certification doc pack, phytosanitary/COO document generation, **Nafeza + CargoX ACID integration**, CBE export-proceeds surrender tracking, cold-room storage costing per batch.
- [ ] **FMCG distribution & van sales** — huge Egyptian segment, high fit with inventory+parties+payroll. `(0%)`
  - ⚠️ Missing: route/journey plans, van-stock transfers & cash collection rounds, market returns/expiry intake, merchandiser visit check-ins, distributor price tiers.
- [~] **Manpower supply / outsourcing companies** (شركات توريد عمالة). `(60%)` — contractor workforce module covers deployment+wages; settlements exist.
  - ⚠️ Missing: the *revenue* side — monthly client invoicing per deployed worker/post, margin-per-worker report, client contract SLAs.
- [~] **Security & cleaning services companies**. `(50%)` — shift rosters + attendance + bulk decisions exist.
  - ⚠️ Missing: post/guard scheduling per *client site*, client billing per shift-hour, SLA-penalty deduction rules, guard-post coverage alerts.
- [~] **Pharmacies & medical-supplies distribution**. `(30%)` — inventory expiry/shelf-life exists; synergizes with §14.6 pharmacy.
  - ⚠️ Missing: EDA drug-pricing registry sync, insurance formulary mapping, shelf-level cycle counts.
- [~] **Restaurants / cafés / chains**. `(45%)` — POS engine + shifts reconciliation (V311–V313) done.
  - ⚠️ Missing: delivery-app order sync (Talabat/Elmenus/Breadfast/Bosta webhooks), kitchen-display screen, recipe costing wired to BOM, **ETA e-receipt (إيصال إلكتروني)** which is a separate regime from the done e-invoice.
- [ ] **Real-estate developers & brokers** — installment culture makes this prime Egyptian market. `(0%)`
  - ⚠️ Missing: unit inventory with availability status, installment plan schedules per unit, broker commission plans, trust/escrow ledger.
- [ ] **Law firms** — matters/cases, court-session calendar with reminders, client trust (IOLTA) ledger, time-based billing. `(0%)`
- [ ] **NGOs & associations** — donor-restricted funds, grant projects, in-kind donations, Form-26 exemption reporting. `(0%)`
- [ ] **Gyms / sports clubs / beauty centers** — memberships & recurring subscriptions, class bookings, trainer commissions, access-control via device hub. `(0%)`
- [ ] **Gas stations** — tank dip vs meter variance, price-change history, forecourt shop POS reuse. `(0%)`
- [ ] **Equipment rental** — rental contracts, utilization reports, damage charges (Odoo Rental parity). `(0%)`
- [ ] **Egypt regulatory integrations (cross-vertical)**: ETA e-receipt for retail ❌ · Taaqadem social-insurance electronic forms (LC1/6, monthly) ❌ · withholding-tax certificates (3%/5%) + quarterly Form 4 ❌ · InstaPay/Fawry collections (§17B) ❌ · Egypt Trust e-signature tokens ❌.

## 19. 👤 Vertical-Aware User Creation & Role Templates — 100% ✅ (WP-10, Aug 2026)

> Verified gap: `users.page.ts:91` uses a **static flat `USER_MENU_OPTIONS` list** — every admin sees every menu regardless of tenant vertical or enabled features. `TenantSetupService.getDefaultSpecsForVertical` provisions only **2 policy groups per vertical**.

- [x] PBAC policy groups + permission matrix + preset tiers. `(done)` — Session 12 foundation.
- [x] Per-vertical default policy groups ✅ **DONE (WP-10)** — Session 12 provisioning + V351 global role-template catalog (`user_role_templates`, `app_id IS NULL` = shared rows; tenant rows shadow by code).
- [x] **Feature-flag-aware menu catalog endpoint** `GET /api/v1/auth/users/menu-options` ✅ **DONE (WP-10, V351)** — `UserRoleTemplateService.menuOptions()` iterates `AccessCatalog.pages()`, tags each menu with verticals via FEATURE_VERTICALS inversion, gates on `TenantFeatureService.isEnabled`; disabled menus return `enabled:false` and the UI grays them with tooltip `users.menuNotEnabledForVertical`. Companion `GET /api/v1/auth/users/role-templates?vertical=` returns templates for the requested ∪ inferred ∪ GENERAL verticals with dedupe-by-code and top-3 suggested policy groups scored by permission-prefix overlap.
- [x] **Job-template picker on Add/Edit user** ✅ **DONE (WP-10, V351 translations)** — Add-user drawer shows the template dropdown (hidden while editing); applying merges template menus into the form (union), flips custom access on, auto-selects suggested policy groups; still fully editable after. All 24 templates seeded: MEDICAL → Doctor/Nurse/Pharmacist/Lab Tech/Radiologist/Insurance Officer/Clinic Cashier · MANUFACTURING → Plant Supervisor/QC Inspector/Storekeeper/Maintenance Planner/Production Planner · RETAIL → Cashier/Merchandiser/Van-Sales Rep/Branch Manager · CIVIL → Site Engineer/Quantity Surveyor/Subcontractor Coordinator · SERVICES → Consultant/Support Agent · GENERAL → Accountant/HR Officer/Purchasing Officer. Bilingual names in V351 CSV (29 keys × ar/en).
- [ ] Multi-vertical group tenants (hospital + pharmacy under one tenant): secondary-verticals list on setup; union-filtered catalog. `(0%)`

> WP-10 evidence: backend `UserRoleTemplateServiceTests` 6/6 + full suite 822 tests/197 suites/0 failures; FE `users.page.spec.ts` +3 (AC-1 DOM feature-lock test incl. teleported-modal handling, template-apply test, AC-3 fallback) → 490 tests/101 files/0 failures; i18n 4679 keys; hardcoded 0 violations (114 HTML/238 TS).

## 20. 🥊 Competitor Feature Parity — Daftra & Odoo (researched Aug 2026) — ≈30%

> Sources: daftra.com/features (35+ verticals, full app catalog) and odoo.com/apps (19.x catalog). Items marked ✅ already exist in our project.

### Daftra gaps (closest regional competitor)
- [ ] Customer loyalty points program. `(0%)`
- [ ] Memberships & recurring subscriptions billing (gyms/clubs/magazines) — feeds §18 gym vertical. `(0%)`
- [ ] Client medical-insurance management — aligns with §14.11 payer engine. `(0%)`
- [ ] Sales targets & commissions engine (per rep/product/period) — generalizes the doctor-commission idea §14.13. `(0%)`
- [ ] First-class installments management on AR (Daftra ships it standalone — confirms our §7.5 gap). `(0%)`
- [ ] Promotions/offers engine (price rules, bundles). `(0%)`
- [ ] Generic bookings/appointments module (multi-vertical: clinics, salons, rentals). `(0%)`
- [ ] Rentals & units management + lease contracts. `(0%)`
- [ ] Service work orders (أوامر شغل) for job-shop/craftsmen — distinct from manufacturing production orders we have. `(0%)`
- [ ] Standalone time-tracking (non-payroll timesheets). `(10%)` — timesheet keys exist in SERVICES group specs only.
- [x] **Fixed-assets management ✅ SHIPPED (WP-04, V347)** — straight-line register + exactly-once month-end journals + balanced disposal (gain/loss plugs) + FE page + XLSX export; 800 BE / 475 FE tests green (2026-08-24).
- [ ] Interactive stocktake mobile flow (we have import validation only). `(0%)`
- [~] Mobile app suite: attendance selfie punch ✅ shipped via WP-14/§23 (`POST /api/v1/attendance/selfie-punch` + offline outbox); quick-expense capture, e-invoice QR reader, stocktake scan remain `(≈25%)`
- [ ] Public developer API portal + app marketplace + reseller/partner program. `(0%)` — ecosystem moat, not just features.
- [ ] Multi-country e-invoicing (KSA ZATCA phase-2, Jordan, UAE) — export-market expansion. `(0%)` — Egypt ETA ✅ only.

### Odoo gaps (global benchmark)
- [ ] Recruitment ATS: job postings, candidate pipeline, interview scheduling → employee conversion. `(0%)`
- [x] Employee expense claims: receipt photo, policy limits, approval → reimbursement through payroll/GL. `(100%)` ✅ **DONE (WP-11, V355/V356/V357)** — Full lifecycle: submit → approve/reject (SoD: self-approval blocked, ownership gate) → reimburse (partner-ledger credit to employee advance). Backend: `ExpenseClaim` entity, `ExpenseClaimService`, `ExpenseClaimController`, `AccessCatalog.EXPENSES` page + `P_EXPENSE_READ`/`P_EXPENSE_MANAGE` permissions. V355 schema + 44 translation keys, V356 sec_permissions seed, V357 common translations. Frontend: `ExpensesPage` with tabs/drawers/receipt upload, 5 specs green. AC-1 through AC-3, AC-5, AC-6 verified; AC-4 (category limit warnings) deferred. 838 BE / 503 FE tests green; i18n 4,711 keys; catalog 14,216 rows; error-codes 607/607.
- [ ] Fleet management: vehicles, fuel logs, maintenance schedules, driver assignment, license renewals. `(0%)`
- [ ] Helpdesk/ticketing with SLA timers. `(0%)`
- [ ] Field-service dispatch + intervention reports. `(0%)`
- [ ] Equipment maintenance module (MTTR/MTBF, work permits) — complements work-center downtime data. `(0%)`
- [ ] Repair orders (in-warranty/out-warranty flows). `(0%)`
- [ ] PLM/engineering change orders for manufacturing depth. `(0%)`
- [ ] Recurring subscriptions invoicing engine. `(0%)`
- [ ] Marketing suite: email campaigns, SMS marketing, automation flows, social scheduler, events, surveys. `(0%)` — CRM chatbot exists only.
- [ ] Referral program (employee/customer referrals with rewards). `(0%)`
- [ ] Knowledge base/wiki per workspace. `(0%)`
- [ ] Document-management (GED): folders, tags, versioning over existing attachments. `(20%)` — REM-005 attachments are flat.
- [ ] eSign workflows (Odoo Sign parity) — pair with Egypt Trust tokens §18. `(0%)`
- [ ] VoIP click-to-call. `(0%)`
- [ ] Website live-chat widget feeding CRM. `(0%)`
- [ ] Low-code Studio builder (admin-customizable screens) — biggest long-term differentiator. `(0%)`
- [ ] Spreadsheet-BI: live-synced spreadsheet reports over GL/inventory. `(0%)`
- [x] Approvals requests engine — covered by workflow definitions (Epic 2) + dynamic multi-stage (V324–V325).
- [ ] Trend note: AI-assistant connectors (MCP servers) are the #1 downloaded Odoo add-on — pairs with §17A NL-report Q&A.

## 21. ⌨️ Shortcut & Dialog UI Issues — 8 defects found (code-verified)

> All verified against source today. Files: `fe/src/app/core/shell/app-shell.component.ts` (shell handler), `fe/src/app/shared/ui/modal-dialog/modal-dialog.component.ts` (modal stack).

- [x] **FIXED BUG-1: Global shortcuts fire behind open page dialogs.** Shell handler checks only its own panels (`quickNavOpen()/shortcutHelpOpen()/logoutOptionsOpen()`, app-shell.component.ts:332-340) — it is unaware of `ModalDialogComponent` instances. With any confirm/policy/settlement dialog open and focus on a button (not an input), pressing `/` opens quick-nav *over* the modal, `?` opens help *over* it, and a `G→key` chord navigates *behind* it. This is exactly the reported "some dialogs not working with shortcuts".
  - ⚠️ Fix: expose static `ModalDialogComponent.anyOpen()` (the stack already exists at modal-dialog.component.ts:52) or a shared `DialogStateService`; early-return in the shell handler when true.
- [x] **FIXED BUG-2: Same dialog behaves differently depending on focus position.** Typing-in-input suppresses shortcuts (target matches input/textarea/select, :285-286) but focus-on-a-button does not — inconsistent UX inside one dialog.
- [x] **FIXED BUG-3: Raw shell overlays bypass the modal stack entirely.** Logout scope dialog (:292), notification action-center (:357), quick-nav (:401), shortcut help (:462) are hand-rolled `.shortcut-overlay` divs: no Tab focus-trap, no body scroll-lock coordination, split Escape logic (three separate branches :305-318), no aria-modal/role="dialog"/aria-labelledby audit.
- [x] **FIXED BUG-4: Chord state survives dialog open & window blur.** Press `G` (chord waits 1.8s, :364-368), then click a row opening a modal — the chord hint stays visible and the next keypress navigates behind the modal. Alt-tab during the window has the same effect. No `clearChord()` hook on modal-open or `window:blur`.
- [x] **FIXED BUG-5: Escape dual-handler race.** `ModalDialogComponent.onDocumentKeydown` (:105-122) and the shell's Escape branches both listen on `document`; resolution depends on listener registration order. If quick-nav AND a page modal are somehow both open, both may consume Escape differently.
- [x] **FIXED BUG-6: Global Enter-to-submit hijack has no escape hatch.** `submitFormOnEnter` (:371-382) submits ANY form on Enter — including filter/search forms inside dialogs where users expect Enter to confirm a sub-dialog, not submit the outer form. Needs `data-no-autosubmit` opt-out attribute honored here.
- [x] **CORRECTED — BUG-7 was a false positive.** Code audit during WP-13 showed `shortcut-settings.component.ts` already warns via `shortcuts.duplicateDestination` on every blocked path (:236/:317) and reverts the select; nothing to ship.
- [x] **FIXED BUG-8: Custom legacy dialogs not unified on `app-modal-dialog`.** Audit payroll explanation modal, procurement three-way-match resolver, contractor-settlement drawer, operations dialogs for trap/Escape/scroll-lock/focus-restore parity; consolidate onto one component.
- [x] **Regression tests shipped** (23 new): navigation NOT triggered while a modal is open; chord cleared on `window:blur`; quick-nav suppressed while `ModalDialogComponent.anyOpen()`; Enter not submitting forms marked `data-no-autosubmit`.

## 22. 🔧 Incomplete Flows Register — consolidated fix backlog

**P0 — business-critical gaps**
- [x] Supplier partial payments + installment plans ✅ **DONE** — partial-pay core pre-shipped; V344 adds `supplier_payment_plans` + `POST /api/v1/supplier-invoices/{id}/payment-plan` + FE dialog (2026-08-24, 756 BE / 455 FE tests green).
- [x] Payment-time settlement discount ✅ **DONE (WP-02, V343+V348)** — discount column + partner-ledger entries (V343); V348 adds `original_due` snapshot, finance-role guard (`PROC_SETTLEMENT_DISCOUNT_FORBIDDEN`), GL subledger event `SUPPLIER_SETTLEMENT_DISCOUNT`, collapsible "تسوية بخصم" dialog with preview, export columns; 801 BE / 478 FE tests green (2026-08-24).
- [x] Purchase-request approval → PO conversion ✅ **DONE (WP-03, V345)** — full PR lifecycle + one-shot convert fills the dangling `purchaseRequestId`; 776 BE / 458 FE tests green (2026-08-24).
- [x] Fixed assets module ✅ **DONE (WP-04, V347)** — see §20; remaining niceties: revaluation & impairment, component depreciation, declining-balance method.
- [x] Continuous-inventory valuation ✅ **DONE (WP-05, V349)** — FIFO/WA engine pre-existed; report surface added: as-of/warehouse/item filters, method badges, GL reconciliation delta, localized XLSX export; 805 BE / 480 FE tests green (2026-08-24).

**P1 — product completeness**
- [x] Generated-report-period registry + hide-already-generated months ✅ **DONE (WP-06, V352)** — `GET /api/v1/reports/generated-periods?year=` derived query (APPROVED+EXPORTED only, no new table); picker chips disable with tooltip + inline view-link to the exact report; stale-token refetch on year switch; 824 BE / 493 FE tests green (2026-08-25).
- [x] Loans deduction-policy switcher ✅ **DONE (WP-07, V350)** — resolved policy (EMPLOYEE→CATEGORY→GLOBAL→defaults AUTO+MONTHLY) gates payroll auto-collection w/ evidence JSON; idempotent manual apply `POST /api/v1/workforce/advances/apply-deduction`; settings policy card + gated employees action; 4 new error codes en/ar; 816 BE / 487 FE tests green (2026-08-24).
- [ ] Employee-form grouped sections with layout preview (§8).
- [ ] Sales targets & commissions engine (§20 Daftra gap).
- [ ] Employee expense claims (§20 Odoo gap).
- [ ] Recruitment ATS basics (job postings → candidate pipeline).

**P2 — differentiators**
- [ ] OCR supplier-invoice capture; site custody register; outbound WhatsApp HR notifications; fleet; helpdesk. ~~peak clock-in analytics~~ ✅ DONE (WP-08, 2026-08-25).

**Blocked / external validation**
- [ ] F-001 PostgreSQL payroll-concurrency proof (needs Docker).
- [ ] device-hub container runtime validation; Frankfurter live fetch (network/Docker).
- [ ] CI re-run (GitHub Actions billing lock).
- [ ] Restore-drill execution evidence; GraalVM launcher scripts; dynamic CSV-ID generator; Spring-cache expansion beyond translations.

## 23. 📱 Android Wrapper App (web-app shell) — 80% ✅ WP-14 shipped (repo side), APK build parked externally

> The PWA (ngsw service worker) already works; the desktop side ships via Tauri. An Android wrapper gives Play-Store presence + native capabilities the browser can't reach.

- [x] **Recommended stack: Capacitor** — DONE (Capacitor 8.5 scaffold in `fe/android`, appId `com.bemo.erp`, `versionName` mirrors fe package version; custom Java bridges in `nativebridge/`).
- [x] Thin-client architecture: first-run `server-setup` page OUTSIDE auth guard probes `GET {url}/api/v1/i18n/ar-EG` and persists via Preferences (`bemo-server-url`); interceptor prepends stored base in native. *(Cert pinning flag deferred to release-hardening pass)*
- [x] Native bridges:
  - [x] FCM push: V346 adds `web_push_subscriptions.platform`+`fcm_token`; ANDROID registrations skip VAPID config, delivery filtered `platform='WEB'` (completes §16 VAPID pipeline).
  - [x] Camera selfie attendance → `POST /api/v1/attendance/selfie-punch` (V346 table, idempotent by operationId, ≤2MB guard, ATT_SELFIE_* codes en/ar). Barcode scanning wired into inventory barcode-lookup (custom play-services-code-scanner bridge). *(Invoice-photo OCR stub deferred — future work per §17A)*
  - [x] Biometric unlock on resume via androidx.biometric plugin w/ preference toggle + DEVICE_CREDENTIAL fallback.
  - [x] Back-button harmonized (history.back except root screens where confirm→exitApp). *(Excel/share-sheet intents not yet bridged)*
  - [x] Offline outbox v1: IndexedDB queue keyed by client operationId; replays exactly-once on `online`; server dedupes (proven by BE replay tests).
- [ ] Distribution: signed AAB → Play internal testing track — **parked externally** (no Android SDK/device/Firebase project in WSL env; repo is build-ready).
- [x] Acceptance criteria (code-level): push pipeline + platform column shipped; offline selfie punch syncs exactly-once (operationId proof tests green); camera barcode lookup hits `/barcode-lookup`; back button gated; biometric resume gate live. *(≤15 MB APK size check pending external build)*

