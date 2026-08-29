# Bemo ERP Feature & Architectural Status Matrix
*Last Updated: 2026-08-29*

This document tracks the end-to-end implementation and verification status of all developer stories, architecture requirements, and business modules across:
1. **`yalla.md`** (Advanced ERP Lifecycle & Dynamic Security Roadmap)
2. **`bemo-flexible-auth-stories.md`** (Flexible Authorization & Dynamic RBAC Specification)
3. **`bemo-all-developer-stories-v2.md`** (Dynamic RBAC & Business Setup/Onboarding Engine)

---

## Master Status Summary

| # | Feature / Story ID | Domain | Backend Status | Frontend Status | Test Verification | Overall State |
|---|---|---|---|---|---|---|
| **E1-1** | PBAC Dynamic Policy Engine & Granular Privilege Matrix (`yalla.md` 1.1) | Security & Access | ✅ Implemented (`@auth`, `PolicyGroupService`, `AccessPolicyController`) | ✅ Implemented (`PolicyMatrixComponent`, `*hasPermission`, `permissionGuard`) | ✅ 100% Passed (428 FE / Full BE) | **COMPLETED** |
| **E1-2** | Dynamic Tenant-Scoped Roles (`STORY-01`) | Security & Access | ✅ Implemented (`SecurityPolicyGroup`, Liquibase `v332`) | ✅ Implemented (`/access/policy-groups`, Drawer) | ✅ 100% Passed | **COMPLETED** |
| **E1-3** | Granular Privilege Guarding (`STORY-02`) | Security & Access | ✅ Implemented (200+ seed permissions, `@auth.hasPermission`, role-derived fallback) | ✅ Implemented (`*hasPermission`, `PolicyService`) | ✅ 100% Passed | **COMPLETED** |
| **E1-4** | Decoupled Workflow State Transitions & Lock Safety (`STORY-03`) | Core Workflows | ✅ Implemented (`PayrollController` PreAuthorize + Optimistic/Pessimistic Locking) | ✅ Implemented (Status workflow progress indicators & actions) | ✅ 100% Passed | **COMPLETED** |
| **E1-5** | Row-Level Data Scoping & Multi-Level Filtering (`STORY-04`) | Data Scoping | ✅ Implemented (`@auth.hasBranchAccess`, `scopeBranchId`, `scopeCostCenterId`) | ✅ Implemented (`UserPolicyAssignmentModal`) | ✅ 100% Passed | **COMPLETED** |
| **E1-6** | Abstracted Access Tiers & Dynamic Shell (`STORY-05`) | UI & Navigation | ✅ Implemented (`AccessPolicyController`) | ✅ Implemented (Preset Tiers: View-Only, Manage, Approver, Full Control in `PolicyMatrixComponent`) | ✅ 100% Passed | **COMPLETED** |
| **E1-7** | Dynamic Business Vertical Setup & Module Provisioning (`STORY-06`) | Onboarding Engine | ✅ Implemented (`TenantSetupService`, `BusinessVertical`, `TenantSetupController`) | ✅ Implemented (`BusinessVerticalSetupComponent` in Settings) | ✅ 100% Passed | **COMPLETED** |
| **E2-1** | Project WBS, BOQ & Earned Value Management (`yalla.md` 2.1) | Contracting | ✅ Implemented (`com.bemo.hr.project.*`) | ✅ Implemented (`projects.page`, WBS tree) | ✅ 100% Passed | **COMPLETED** |
| **E2-2** | Daily Progress Reports (DPR) & Site Weather Logs (`yalla.md` 2.2) | Contracting | ✅ Implemented (`DprService`, weather tracking) | ✅ Implemented (`daily-reports-list.component`) | ✅ 100% Passed | **COMPLETED** |
| **E2-3** | Subcontractor Progress Claims & Retention Holdbacks (`yalla.md` 2.3) | Contracting | ✅ Implemented (`ClaimService`, retention calc) | ✅ Implemented (`claims-list.component`) | ✅ 100% Passed | **COMPLETED** |
| **E3-1** | Multi-Level BOM & Routing Steps (`yalla.md` 3.1) | Manufacturing | ✅ Implemented (`com.bemo.hr.manufacturing.*`) | ✅ Implemented (`production.page`) | ✅ 100% Passed | **COMPLETED** |
| **E3-2** | Real-Time Work Orders & Machine Downtime OEE (`yalla.md` 3.2) | Manufacturing | ✅ Implemented (`ProductionService`) | ✅ Implemented (`production.page`) | ✅ 100% Passed | **COMPLETED** |
| **E3-3** | Continuous Inventory Valuation & Landed Costs (`yalla.md` 3.3) | Operations | ✅ Implemented (`OperationsService`, FIFO/AVCO) | ✅ Implemented (`operations.page`) | ✅ 100% Passed | **COMPLETED** |
| **E4-1** | Omnichannel Van Sales & Route Settlement (`yalla.md` 4.1) | Trade & Sales | ✅ Implemented (`com.bemo.hr.trade.sales.*`) | ✅ Implemented (`sales.page`) | ✅ 100% Passed | **COMPLETED** |
| **E4-2** | Offline Mobile POS & Shift Reconciliation (`yalla.md` 4.2) | Trade & POS | ✅ Implemented (`com.bemo.hr.trade.pos.*`) | ✅ Implemented (`pos.page`) | ✅ 100% Passed | **COMPLETED** |
| **E4-3** | WhatsApp Cloud CRM & Quotation Automation (`yalla.md` 4.3) | CRM | ✅ Implemented (`com.bemo.hr.crm.*`) | ✅ Implemented (`crm.page`) | ✅ 100% Passed | **COMPLETED** |
| **E4-4** | ETA e-Invoicing & e-Receipt SDK Integration (`yalla.md` 4.4) | Compliance | ✅ Implemented (`com.bemo.hr.compliance.eta.*`) | ✅ Implemented (`eta-tax.page`) | ✅ 100% Passed | **COMPLETED** |

---

## Detailed Security & Controller Authorization Architecture

### Controller Method Security & Dynamic PBAC Interoperability
1. **Dynamic Evaluation (`@auth`)**:
   - Registered custom SpEL evaluator bean `SecurityAuthorizationEvaluator` (`@Component("auth")`).
   - Supports:
     - `@PreAuthorize("@auth.hasPermission('module:entity:action')")`
     - `@PreAuthorize("@auth.hasAnyPermission('perm1', 'perm2')")`
     - `@PreAuthorize("@auth.hasBranchAccess(#branchId)")`
     - `@PreAuthorize("@auth.hasCostCenterAccess(#costCenterId)")`
2. **Seamless Dual-Mode Compatibility (Legacy Roles & PBAC Groups)**:
   - `PolicyGroupService.getEffectivePermissions(username)` automatically includes permissions from custom PBAC policy groups as well as inherited permissions from existing legacy assigned roles (`RoleCode`).
   - Administrators (`SUPER_ADMIN`, `ADMIN`) retain full wildcard bypass (`*`).
   - Standard roles (`ACCOUNTANT`, `HR_MANAGER`, `PROJECT_MANAGER`, `SALES_MANAGER`, `PROCUREMENT_MANAGER`, `TREASURY_USER`, etc.) map to their exact module permission prefixes.
   - Restricted roles (`VIEWER`) strictly map to read-only capabilities without access to confidential payroll or sensitive financial actions.

---

## Verification & CI Gate Summary

- **Backend Tests:** 100% clean execution — **1,238 tests / 248 suites / 0 failures / 0 skipped** (`BUILD SUCCESSFUL`, `./gradlew test -PskipDockerTests`).
- **Frontend Unit Tests:** **610 / 610 passed** across 127 test files (100% green, Node 24).
- **i18n Catalog Validation:** **5,594 literal keys verified** (`ar-EG` & `en-US`); **16,963 translation rows / 0 defects / bilingual pairs unique** (`check-translation-catalog.py`).
- **Hardcoded Strings Scanner:** **0 violations** across 140 HTML templates and 291 TypeScript source files (`check-hardcoded-strings.mjs`).
- **Error-Code→Translation Gate:** **756 / 756** exception codes covered (0 missing) (`check-error-codes.py`).
- **Authorization Contract Gate:** **21 declared roles / 21 referenced roles / unknown: 0** (`check-authorization-contract.py`).
- **Production Build (`ng build`):** Production bundle compiled cleanly.
- **Test-count floors:** BE ≥1238/≥248 (`be/tools/check-test-count.py`), FE ≥610/≥127 (`fe/tools/check-test-count.mjs`).

### Session 27 (2026-08-29) — WP-17 Manpower-Supply Client Billing (AC-1..AC-5 all MET)
- **WP-17 revenue side shipped**: `ClientBillingService` (`com.bemo.hr.workforce`) — `generate` collects attendance-APPROVED days only from APPROVED/LOCKED `WorkforceSettlementPeriod` windows overlapping the month → draft lines resolved per-day against effective `ClientWorkerRate` (max-by-effectiveFrom; mid-month split = first 15 days old rate, rest new); MISSING_RATE lines carry clear reasons and confirm is blocked (`CLIENT_BILLING_UNRESOLVED_LINES`); overlapping rate windows rejected (`CLIENT_RATE_OVERLAP`, translated ar/en). Confirm issues ONE `CustomerInvoice` via `SalesReceivablesService.createAndIssueDeliveryInvoice` (number `CLB-YYYYMM-<PARTY6>`), period → INVOICED; regenerate → `CLIENT_BILLING_PERIOD_EXISTS`; closed → `CLIENT_BILLING_PERIOD_NOT_OPEN`. Margin = billed − settled `grossWage` per worker (440−360=80 exact) + localized xlsx export. Endpoints `/api/v1/workforce/client-billing` (7) reuse `settlements.read/prepare/finalize` (permission deviation documented; no new AccessCatalog perm).
- **Liquibase V412** (3 tables `client_worker_rates`/`client_billing_periods`/`client_billing_draft_lines` + UQ + indexes + `allowed_menus` append) + **V413** translations (124 rows = 62 keys × ar-EG/en-US: 10 error codes + `workforce.clientBilling.*` UI/export keys), both registered in `next` + `test-h2`; H2 context load verified.
- **Frontend**: `WorkforceService` methods (clients from `/api/v1/parties` non-SUPPLIER filter, rates CRUD, generate/review/confirm, margin + blob export); page `/workforce/client-billing` (client+month toolbar, rates card + add modal, review grid w/ BILLABLE/MISSING_RATE badges + totals + confirm row w/ blocked hint, margin card + xlsx export); route + 4-part menu protocol (nav item, access-catalog-contract entry gated on `workforce.contractorAccounts.enabled`, auth.service gate line, users auto-derived); REQUIRED_COPY fallbacks; parity spec updated.
- **Specs**: BE `ClientBillingServiceTests` 9 (approved-days billing, mid-month rate split, overlap rejection, MISSING_RATE blocking, single invoice + INVOICED flip, regenerate/closed rejection, margin exact); FE `client-billing.component.spec.ts` 7 (clients filter, scoped rates, generate+review, confirm flip, flags, blob export, no-client guard).
- **Evidence**: BE **1174/234/0** (+1 skipped; V412/V413 clean on H2); FE **578/115/0**; error-codes **696/696 PASS** (+10, service now uses `error(code,status)` helper matchable by scanner); catalog **15,991 rows PASS**; `check:i18n` **5,262 keys**; `check:hardcoded` **0** (128 HTML + 276 TS); `ng build` green; floors raised (BE 1174/234, FE 578/115); WP-17 AC-1..AC-5 ticked in `bemo/tasks/WP-17-manpower-client-billing.md`; `_INDEX.md` + `PROJECT_MAP.md` updated.

### Session 26 (2026-08-29) — WP-16 Agri-Export Documentation Pack (AC-1..AC-5 all MET)
- **WP-16 doc generators closed (AC-1 + AC-5)**: new `ExportShipmentDocService` renders COO / packing list / phytosanitary xlsx from persisted `ExportShipmentLine` quantities — no re-entry, totals summed (2250 for the 1000/500/750 test lot); bilingual ar-EG (RTL, Arabic headers) + en-US (LTR, English headers) sheets from the DB translation catalog; treatments table (lotReference/chemical/dose/treatmentDate/phiDays/earliestSafePickup) via `ComplianceRegister` for phyto. Endpoints `GET /api/v1/trade/export-shipments/{id}/docs/{coo|packing-list|phytosanitary}.xlsx` with localized Content-Disposition filenames. **Liquibase V411** `20260829_v411_export_doc_translations.{yaml,csv}` (7 keys × 2 locales, ids v411-001..007; trailing-`;` bug fixed), registered in `next` + `test-h2`.
- **FE DOCS tab**: export-shipments page tab (`export.tabDocs`) with COO/packing-list/phytosanitary download buttons for the selected shipment (blob-download pattern from `sales.page`), `export.selectShipmentFirst` warning. New SCSS `.doc-buttons`/`.muted`/`.docs-section`.
- **Specs**: BE `ExportShipmentDocServiceTests` 8 (lot exactness, total rows, RTL vs LTR, treatments per lot, not-found, docType route parse); FE +3 (tab buttons, no-shipment warn, blob download).
- **Evidence**: BE **1165/233/0** (BUILD SUCCESSFUL; V411 clean on H2); FE **571/114/0**; error-codes **686/686**; catalog **15,867 rows PASS**; `check:i18n` **5,221 keys**; `check:hardcoded` **0** (127 HTML + 275 TS); `ng build` green; floors raised (BE 1165/233, FE 571/114); WP-16 AC-1..AC-5 ticked in `bemo/tasks/WP-16-agri-export-pack.md`; `_INDEX.md` + `PROJECT_MAP.md` + `missing-todo.md` updated.

### Session 25 (2026-08-29) — WP-12 AC-5 Commission Export + Send-to-Payroll; full-suite greening
- **WP-12 AC-5 shipped**: `SalesCommissionPayout` entity + repo (unique `app_id, rep_id, period`); localized `GET /api/v1/sales/targets/commissions/export.xlsx` (ar/en filename + bilingual columns, 4 cols incl. rule/basisAmount/percent/commission); idempotent `POST /api/v1/sales/targets/commissions/send-to-payroll` (replay `alreadySent=true`, never double-saves); FE export/send buttons + sent-at badge + disabled-once + `payrollSent` metadata. Liquibase **V409** (`sales_commission_payouts`, registered both masters, `TIMESTAMP` + quoted `decimal(15,2)`), **V410** (translations, registered both masters).
- **Committed-WIP test/artifact fixes** (from `e196a5b`, all now green): `ReportScheduleSchedulerTests` rewritten to real package-private API under `…/scheduled/application/` (old stale path removed both in tree and mirror); `ReportScheduleExecutorTests` ×2 (channel contract + redundant stub); `EinvoicingSettingsServiceTest.listProvidersReturnsAllThree` (nested-stub → plain list); V409 YAML unquoted `decimal(15,2)` → quoted (Liquibase parse); **v406 `content BLOB` → `bytea`** (H2-PG-mode compat, H2 rejects `BLOB`); `SIGN_CONTENT_MISMATCH` translation added (v401 CSV +2 rows). Driver: full-suite gate was broken by these WIP leftovers.
- **Regression fix**: `ExpenseClaimServiceTests.is_over_limit_reports_true_when_exceeded` set `transportLimit` on a MEAL claim → now `mealLimit` (test bug only).
- **Evidence**: BE **1157/232/0** (BUILD SUCCESSFUL 4m04s); FE **568/114/0** (expenses spec flaked once on parallel first run under load, green on isolated + 2 sequential full runs); error-codes **686/686**; catalog **15,853 rows PASS**; `check:i18n` **5,214 keys**; `check:hardcoded` **0** (127 HTML + 275 TS); `ng build` green; test-count floors raised (BE 1157/232, FE 568/114); WP-12 AC-5 ticked; `_INDEX.md` + `PROJECT_MAP.md` updated.
