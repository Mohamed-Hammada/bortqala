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

- **Backend Tests:** 100% clean execution — **1157 tests / 232 suites / 0 failures / 1 skipped** (`BUILD SUCCESSFUL`, `-PskipDockerTests`).
- **Frontend Unit Tests:** **568 / 568 passed** across 114 test suites (100% green, Node 24).
- **i18n Catalog Validation:** **5,214 literal keys verified** (`ar-EG` & `en-US`); **15,853 translation rows / 0 defects** (`check-translation-catalog`).
- **Hardcoded Strings Scanner:** **0 violations** across 127 HTML templates and 275 TypeScript source files.
- **Error-Code→Translation Gate:** **686 / 686** exception codes covered (0 missing).
- **Production Build (`ng build`):** Production bundle compiled cleanly.
- **Test-count floors:** BE ≥1157/≥232 (`be/tools/check-test-count.py`), FE ≥568/≥114 (`fe/tools/check-test-count.mjs`).

### Session 25 (2026-08-29) — WP-12 AC-5 Commission Export + Send-to-Payroll; full-suite greening
- **WP-12 AC-5 shipped**: `SalesCommissionPayout` entity + repo (unique `app_id, rep_id, period`); localized `GET /api/v1/sales/targets/commissions/export.xlsx` (ar/en filename + bilingual columns, 4 cols incl. rule/basisAmount/percent/commission); idempotent `POST /api/v1/sales/targets/commissions/send-to-payroll` (replay `alreadySent=true`, never double-saves); FE export/send buttons + sent-at badge + disabled-once + `payrollSent` metadata. Liquibase **V409** (`sales_commission_payouts`, registered both masters, `TIMESTAMP` + quoted `decimal(15,2)`), **V410** (translations, registered both masters).
- **Committed-WIP test/artifact fixes** (from `e196a5b`, all now green): `ReportScheduleSchedulerTests` rewritten to real package-private API under `…/scheduled/application/` (old stale path removed both in tree and mirror); `ReportScheduleExecutorTests` ×2 (channel contract + redundant stub); `EinvoicingSettingsServiceTest.listProvidersReturnsAllThree` (nested-stub → plain list); V409 YAML unquoted `decimal(15,2)` → quoted (Liquibase parse); **v406 `content BLOB` → `bytea`** (H2-PG-mode compat, H2 rejects `BLOB`); `SIGN_CONTENT_MISMATCH` translation added (v401 CSV +2 rows). Driver: full-suite gate was broken by these WIP leftovers.
- **Regression fix**: `ExpenseClaimServiceTests.is_over_limit_reports_true_when_exceeded` set `transportLimit` on a MEAL claim → now `mealLimit` (test bug only).
- **Evidence**: BE **1157/232/0** (BUILD SUCCESSFUL 4m04s); FE **568/114/0** (expenses spec flaked once on parallel first run under load, green on isolated + 2 sequential full runs); error-codes **686/686**; catalog **15,853 rows PASS**; `check:i18n` **5,214 keys**; `check:hardcoded` **0** (127 HTML + 275 TS); `ng build` green; test-count floors raised (BE 1157/232, FE 568/114); WP-12 AC-5 ticked; `_INDEX.md` + `PROJECT_MAP.md` updated.
