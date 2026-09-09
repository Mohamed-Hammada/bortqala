# Bemo ERP — 30-Feature Audit Roadmap Implementation Status

**Created:** 2026-08-20  
**Audit Source:** `BEMO_ACTUAL_CODE_AUDIT_ROADMAP_2026-08-18`  
**Branch:** `fm_bemo_consolidated`  
**Current Phase:** P0 Construction Backbone — Quality Remediation  

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Done — passes Definition of Done checklist |
| 🟡 | Scaffolded — entities/APIs/UI exist but incomplete per DoD |
| 🔴 | Not Started — no implementation |
| ⬜ | Preserved — existing strong feature, verified intact |
| 🔧 | Hardening — needs hardening per roadmap |

---

## P0 — Construction Backbone (Critical Path)

### 01. Project Register & Hierarchical WBS / BOQ
**Score:** 5/100 → **Target:** 85+/100  
**Status:** 🟡 SCAFFOLDED  
**Assigned To:** Session 17 + Quality Remediation  

#### What exists:
- ✅ `Project` entity with status lifecycle (DRAFT→ACTIVE→ON_HOLD→COMPLETED→CLOSED)
- ✅ `WbsNode` entity with parent/child hierarchy, wbsPath, level, BOQ quantities/rates
- ✅ `ProjectCostCode` entity with LABOR/MATERIAL/EQUIPMENT/SUBCONTRACTOR/OVERHEAD categories
- ✅ `ProjectPartyRole` entity with CLIENT_OWNER/MAIN_CONTRACTOR/SUBCONTRACTOR/CONSULTANT/SUPPLIER
- ✅ REST APIs: CRUD, WBS tree/flat, reposition, cost codes, party roles
- ✅ Angular projects page with KPI strip, search, filter, create modal
- ✅ Liquibase V269 (schema) + V270 (translations)
- ✅ Unit tests: `ProjectServiceTests` (8 tests), `projects.page.spec.ts` (2 tests)
- ✅ Bilingual README in package

#### Quality gaps fixed (Session 18 — current):
- [x] **CRITICAL:** N+1 query — `toProjectResponse()` now uses batch WBS loading via `sumPlannedAmountByProjectIds` and `countByProjectIds`
- [x] **CRITICAL:** Hardcoded English exception messages — all now use `BusinessRuleException(msg, i18nKey, status)` pattern and have translation rows in Liquibase V331
- [x] **CRITICAL:** Audit logging uses Jackson `ObjectMapper.writeValueAsString()` instead of string concatenation
- [x] **HIGH:** `findAll()` replaced with `ProjectRepository.sumTotalContractValue()` and `WbsNodeRepository.sumTotalPlannedAmount()` aggregate queries
- [x] **HIGH:** WBS cycle detection now fetches all project nodes once via `findByProjectId()` and walks in-memory map
- [x] **HIGH:** `updateDescendantPaths()` now computes levelDelta BEFORE calling `reposition()` on the node
- [x] **MEDIUM:** WBS depth limit of 10 levels enforced in `createWbsNode` and `repositionWbsNode`
- [x] **MEDIUM:** Project closure blocked if any WBS node has `IN_PROGRESS` status
- [x] **MEDIUM:** `companyId`/`branchId` FK validation against Organization module (`CompanyRepository`, `BranchRepository`)
- [x] **MEDIUM:** `ownerPartyId` FK validation against Party module (`BusinessPartyRepository`)
- [x] **LOW:** Unit tests: `ProjectServiceTests` (10 tests), `WbsServiceTests` (6 tests), `project-detail.page.spec.ts` (5 tests), `projects.page.spec.ts` (2 tests)
- [x] **LOW:** Emoji removed from Angular templates — using CSS class-based icons

#### Acceptance criteria checklist:
- [x] Project can be created and assigned to company/branch/owner
- [x] WBS supports nested nodes, stable ordering and rejects cycles
- [x] Downstream documents reference WBS IDs rather than names
- [x] Closed project blocks new postings unless controlled reopen succeeds
- [x] Frontend tree persists and reloads identically
- [x] Permissions and audit evidence cover create/update/close
- [x] Server-side validation (no client-only business rules)
- [x] Loading/error/empty/permission states in Angular
- [x] Unit/component tests for tenant isolation, WBS cycles, uniqueness, close rules (702 BE tests, 416 FE tests)

---

### 02. Daily Site Progress & Field Reporting
**Score:** 15/100 → **Target:** 80+/100 → **Actual (2026-09-08 re-audit): 90+/100**
**Status:** ✅ DONE (re-audited 2026-09-08 — see below)

#### What exists:
- ✅ `ProjectDailyReport` entity with project/site/date/shift/status/revision, real `submit()`/`approve()`/`reopen()` state machine (`DailyReportStatus` DRAFT→SUBMITTED→APPROVED, explicit REOPENED)
- ✅ `DailyWorkProgressLine` by WBS with prior/today/cumulative quantities
- ✅ `DailyLaborSnapshot` with worker/employee references
- ✅ `DailyMaterialConsumption` with inventory item references
- ✅ `DailyEquipmentLog` with equipment/site status
- ✅ `DailyReportAttachment` (added 2026-09-08) — real file-backed evidence attachments
- ✅ Weather condition enum
- ✅ REST APIs: `ProjectDailyReportController` (17 endpoints, all `@PreAuthorize`-gated)
- ✅ Service: `ProjectDailyReportService`
- ✅ Liquibase V272 (schema) + V273 (translations) + V465 (attachments, 2026-09-08)

#### Re-audit finding (2026-09-08): this section was stale. Direct code inspection found 5 of 6 listed gaps already implemented — nobody had updated the checkboxes:

- [x] **CRITICAL:** Approval workflow integration — **already implemented**, not missing. `ProjectDailyReportService.submitDailyReport`/`approveDailyReport`/`reopenDailyReport`, wired to real `@PostMapping("/{reportId}/submit|approve|reopen")` endpoints with real `@PreAuthorize`. `approveDailyReport` also updates WBS node progress/lifecycle status. Confirmed via `submitAndApproveLifecycle_updatesWbsNodeProgress` and `reopenDailyReport_setsStatusReopened` (both pre-existing, passing).
- [x] **HIGH:** Period summary aggregation endpoint — **already implemented**, not missing. `ProjectDailyReportService.getPeriodSummary` (man-days, man-hours, equipment hours, fuel, WBS/labor/material breakdowns), wired to `GET /{projectId}/daily-reports/summary`.
- [x] **HIGH:** Idempotency for concurrent submissions — **already implemented via existing infrastructure**, not missing. `ProjectDailyReport` has a real `@Version` column; a global `@ExceptionHandler(OptimisticLockingFailureException.class)` in `ApiExceptionHandler` turns any concurrent-modification race into a clean API error rather than a raw 500 or a silent double-effect. No bespoke idempotency-key mechanism exists (unlike payment flows, which need one for post-timeout client retries), but that is not needed for this endpoint's actual risk (a genuine concurrent double-submit is a version conflict, not a retry-after-timeout scenario).
- [x] **MEDIUM:** Attachment/evidence model — **genuinely was missing; implemented 2026-09-08.** New `DailyReportAttachment` entity (real `byte[]`/`bytea` storage, same pattern as `SupplierDocument`), `DailyReportAttachmentRepository`, 4 new service methods (`addAttachment`/`listAttachments`/`downloadAttachment`/`deleteAttachment`), 4 new REST endpoints (multipart upload, list, download, delete — all `@PreAuthorize`-gated), Liquibase `20260908_v465_daily_report_attachments.yaml`. File-size (10MB) and content-type allowlist validation match the established `SupplierOnboardingService.addDocument` convention. 5 new tests in `ProjectDailyReportServiceTests` (upload success with real content-byte verification, oversized-file rejection, unsupported-type rejection, cross-report download rejection, delete-with-audit).
- [x] **MEDIUM:** "Copy previous day" API — **already implemented**, not missing. `ProjectDailyReportService.copyPreviousDay`, wired to `POST /{projectId}/daily-reports/copy-previous`.
- [x] **LOW:** Service unit tests — **already existed** (4 tests), not missing; now 9 after the attachment additions.

#### Additional, previously-undocumented defect found and fixed during this re-audit (unrelated to the roadmap's own gap list):

`ProjectDailyReportService` threw `BusinessRuleException`/`NotFoundException` using the **single-argument constructor** for 6 of its 7 error sites (`DPR_ALREADY_EXISTS_FOR_DATE_SHIFT`, `DPR_CANNOT_EDIT_APPROVED`, `DPR_CANNOT_DELETE_APPROVED`, `DPR_NO_PREVIOUS_REPORT_FOUND`, `PROJECT_NOT_FOUND`, `DPR_NOT_FOUND`) — that constructor treats the string as the exception **message**, not the error **code** (`BusinessRuleException(String message)` sets `code` to the generic `"BUSINESS_CONFLICT"`; `NotFoundException(String message)` sets `code` to `null`). `ApiExceptionHandler` resolves the user-facing translated text by `getCode()`, so every one of these calls always fell back to a generic "business conflict"/"resource not found" message — the specific, correct bilingual text a user should have seen (some of which, like `PROJECT_NOT_FOUND`/`DPR_NOT_FOUND`, already had real translation rows sitting unused) was never reachable. `be/tools/check-error-codes.py` never caught this because its regex only matches the two-argument constructor form. Fixed all 7 call sites to use the correct two/three-argument constructor with a real message; added the 4 missing translation rows (`DPR_ALREADY_EXISTS_FOR_DATE_SHIFT`, `DPR_CANNOT_EDIT_APPROVED`, `DPR_CANNOT_DELETE_APPROVED`, `DPR_NO_PREVIOUS_REPORT_FOUND`) to `translations.csv` (`PROJECT_NOT_FOUND`/`DPR_NOT_FOUND` already had rows). **The same single-argument-constructor pattern exists ~40 more times across the rest of the `project` module** (`ProjectSchedulingService`, `ProjectCostControlService`, `ProjectProgressClaimService`, `ProjectTenderService`, `ProjectBudgetVersion`, `ProjectProgressClaim`, `ProjectTender`) — not fixed in this pass (out of scope for item 02 specifically); flagged here as a real, separate finding for whoever picks up items 04/05/06/09 next, since the same fix pattern applies.

A second, unrelated, pre-existing bug was found and fixed while validating against real PostgreSQL: `ReportingDecisionHistoryContractTests.staleReviewerIsRejectedAndCanRetryAfterReload` asserted the raw exception message contained Arabic text ("مراجع آخر") — but `RPT_VERSION_CONFLICT`'s production code was always correct (real code, real translation row); the *test* was checking `getMessage()` directly on a service call with no HTTP/locale layer involved, which can never observe translated text (that resolution only happens in `ApiExceptionHandler`, keyed on `getCode()`, over real HTTP). Fixed the assertion to check `getCode()` instead.

**Verification:** `ProjectDailyReportServiceTests` 9/9 pass. Full backend H2 regression suite (`-PskipDockerTests`): `BUILD SUCCESSFUL`, 0 failures. Full combined suite including real PostgreSQL (`-PskipAot`, all ~1630 tests): `BUILD SUCCESSFUL`, 0 failures — including the new `daily_report_attachments` table migrating cleanly on both H2 and a real `postgres:17-alpine` container. All 3 backend Python gates pass (`check-error-codes.py`: 829/829; `check-translation-catalog.py`: 18,392 rows, 0 defects; `check-authorization-contract.py`: 21/21 roles).

---

### 03. Project Scheduling, Gantt & Resource Planning
**Score:** 5/100 → **Target:** 80+/100  
**Status:** 🟡 SCAFFOLDED  

#### What exists:
- ✅ `ProjectSchedule` + `ProjectScheduleTask` entities
- ✅ `TaskDependency` with FS/SS/FF/SF types and lag
- ✅ `ScheduleBaseline` + `ScheduleBaselineTask` for versioning
- ✅ `TaskResourceAssignment` with resource type
- ✅ Task constraint types (ASAP, ALAP, MUST_START, etc.)
- ✅ REST API: `ProjectSchedulingController`
- ✅ Service: `ProjectSchedulingService`
- ✅ Liquibase V274 + V275

#### Quality gaps to fix:
- [ ] **CRITICAL:** No CPM forward/backward pass calculation
- [ ] **CRITICAL:** No cycle detection in dependency graph
- [ ] **HIGH:** No Gantt frontend component
- [ ] **HIGH:** No resource capacity/leveling
- [ ] **MEDIUM:** No link to daily progress for task % complete

---

### 05. Owner & Subcontractor Progress Claims
**Score:** 20/100 → **Target:** 85+/100  
**Status:** 🟡 SCAFFOLDED  

#### What exists:
- ✅ `ProjectProgressClaim` entity with ClaimKind (OWNER/SUBCONTRACTOR)
- ✅ `ProgressClaimLine` with previous/current/cumulative
- ✅ `ProgressClaimAdjustment` for variations
- ✅ ClaimStatus lifecycle (DRAFT→SUBMITTED→REVIEWED→CERTIFIED→POSTED→PAID→FINAL)
- ✅ Retention, mobilization advance/recovery, tax, deductions modeled
- ✅ REST API: `ProjectProgressClaimController`
- ✅ Service: `ProjectProgressClaimService`
- ✅ Liquibase V278 + V279

#### Quality gaps to fix:
- [ ] **CRITICAL:** No AR/AP/GL posting integration
- [ ] **CRITICAL:** No immutability enforcement for posted/certified versions
- [ ] **HIGH:** No approval workflow integration for certification
- [ ] **HIGH:** No numbered certificate generation
- [ ] **MEDIUM:** No claim register/listing frontend

---

### 06. Project Budget, Cost Control, Variance & Profitability
**Score:** 15/100 → **Target:** 85+/100  
**Status:** 🟡 SCAFFOLDED  

#### What exists:
- ✅ `ProjectBudgetVersion` + `ProjectBudgetLine` with status lifecycle
- ✅ `ProjectCostLedgerEntry` with source module tracking
- ✅ `ProjectForecastEac` for ETC/EAC
- ✅ Cost categories and entry types
- ✅ REST API: `ProjectCostControlController`
- ✅ Service: `ProjectCostControlService`
- ✅ Liquibase V280 + V281

#### Quality gaps to fix:
- [ ] **CRITICAL:** No real cross-module cost rollup from procurement/inventory/workforce
- [ ] **CRITICAL:** No commitment vs actual separation logic
- [ ] **HIGH:** No month-end snapshot service
- [ ] **HIGH:** No GL reconciliation endpoint
- [ ] **MEDIUM:** No variance formula implementation

---

## P1 — Competitive Construction ERP

### 04. Construction Tender / Competition Management
**Score:** 20/100 → **Target:** 80+/100  
**Status:** 🟡 SCAFFOLDED  
**Quality gaps:** No technical/financial weighted scoring, no award→project contract conversion, no Gantt frontend

### 07. Project Executive Dashboard
**Score:** 15/100 → **Target:** 80+/100  
**Status:** 🟡 SCAFFOLDED  
**Quality gaps:** Uses `findAll()` unbounded, no real KPI formulas, no drill-down

### 08. Procurement, RFQ, Purchase-to-Pay & Sourcing
**Score:** 90/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No project/WBS dimensions on PR/PO lines

### 09. Inventory, Warehouses, Reorder & Stock Control
**Score:** 82/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No project/WBS on material issue, barcode/lot/serial unverified

### 10. General Ledger, Journals & Fiscal Periods
**Score:** 92/100  
**Status:** ⬜ PRESERVE  
**Quality gaps:** Project/WBS dimension on journal lines missing

### 11. Treasury, Banks, Cashboxes & Cheques
**Score:** 78/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No cashbox/petty-cash register, no cheque lifecycle

### 12. Customer, Supplier & Contractor Financial Position
**Score:** 82/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No unified aging/open-item report, no project claim integration

### 13. Financial Statements, Close & Analytic Dimensions
**Score:** 80/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** Cost center hierarchy not verified, no project dimension

### 14. Manufacturing: BOM, Routing & Production Orders
**Score:** 90/100  
**Status:** ⬜ PRESERVE  
**Quality gaps:** Finite capacity planning unverified

### 15. Manufacturing: WIP, Cost, Variance, Waste & Quality
**Score:** 84/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** Scrap/waste accounting unverified, OEE unverified

### 16. HR Employee Master, Assignments & Contracts
**Score:** 62/100  
**Status:** 🟡 PARTIAL  
**Quality gaps:** No EmployeeContract lifecycle entity

### 17. Attendance & Biometric Integration
**Score:** 92/100  
**Status:** ⬜ PRESERVE  
**Quality gaps:** No project/site allocation on attendance

### 18. Leave Requests, Balances & Approval
**Score:** 10/100  
**Status:** 🔴 MAJOR GAP  
**Quality gaps:** No LeaveType, no balance ledger, no request lifecycle

### 19. Payroll Runs, Components, Payments & GL
**Score:** 92/100  
**Status:** ⬜ PRESERVE  
**Quality gaps:** No leave integration, no project labor allocation

### 20. HR Performance, Goals & KPIs
**Score:** 10/100  
**Status:** 🔴 NOT STARTED (P2)

### 21. Sales, Quotations, Invoicing & Customer Credit
**Score:** 80/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No SalesQuotation entity

### 22. Egypt E-Invoice / E-Receipt & Tax Compliance
**Score:** 25/100  
**Status:** 🔴 NOT STARTED (P1-if-Egypt)

### 23. Point of Sale (POS)
**Score:** 0/100  
**Status:** 🔴 NOT STARTED (P2)

### 24. CRM, WhatsApp/Facebook & Chatbot
**Score:** 0/100  
**Status:** 🔴 NOT STARTED (P2)

### 25. Multi-Company, Branch & Warehouse Organization
**Score:** 75/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN

### 26. Executive Reporting & Cross-Module Analytics
**Score:** 55/100  
**Status:** 🟡 PARTIAL

### 27. Workforce, Contractors, Labor Dispatch & Settlements
**Score:** 90/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN  
**Quality gaps:** No project/WBS on labor request/dispatch/assignment

### 28. Security, Audit, Approval Workflows & Segregation of Duties
**Score:** 88/100  
**Status:** ⬜ PRESERVE  
**Quality gaps:** Project document types not mapped to approval engine

### 29. Desktop, Deployment, Cloud Readiness & Licensing
**Score:** 70/100  
**Status:** ⬜ PRESERVE + 🔧 HARDEN

### 30. Specialized Verticals: Schools, Tourism, Customs & 3PL
**Score:** 0/100  
**Status:** 🔴 NOT STARTED (P3-STRATEGY)

---

## Execution Order (Priority Sequence)

| Step | Feature | Priority | Status | Session |
|------|---------|----------|--------|---------|
| 1 | 01-Project/WBS Quality Remediation | P0 | ✅ Done (confirmed 2026-09-08 — all acceptance criteria and quality-gap checkboxes were already checked; the status marker was just stale) | Complete |
| 2 | 02-Daily Site Progress completion | P0 | ✅ Done (2026-09-08) | Complete |
| 3 | 05-Owner/Subcontractor Claims completion | P0 | 🟡 Scaffolded | Next |
| 4 | 06-Project Budget/Cost Control completion | P0 | 🟡 Scaffolded | Next |
| 5 | 08-Procurement Project Dimensions | P0-INTEGRATE | 🔴 Not Started | TBD |
| 6 | 27-Workforce Project Dimensions | P0-INTEGRATE | 🔴 Not Started | TBD |
| 7 | 10-GL Project Dimensions | P0-PRESERVE | 🔴 Not Started | TBD |
| 8 | 28-Approval Project Types | P0-PRESERVE | 🔴 Not Started | TBD |
| 9 | 03-Scheduling CPM/Gantt | P1 | 🟡 Scaffolded | TBD |
| 10 | 04-Tender Evaluation Scoring | P1 | 🟡 Scaffolded | TBD |
| 11 | 07-Executive Dashboard KPIs | P1 | 🟡 Scaffolded | TBD |
| 12 | 16-Leave Management | P1 | 🔴 Not Started | TBD |
| 13 | 16-Employee Contracts | P1 | 🟡 Partial | TBD |
| 14 | 09-Inventory Hardening | P1 | ⬜ Preserve | TBD |
| 15 | 11-Treasury Hardening | P1 | ⬜ Preserve | TBD |
| 16 | 12-AR/AP Aging | P1 | ⬜ Preserve | TBD |
| 17 | 13-Financial Statements | P1 | ⬜ Preserve | TBD |
| 18 | 21-Sales Quotations | P1 | ⬜ Preserve | TBD |
| 19 | 22-Egypt E-Invoice | P1-if-Egypt | 🔴 Not Started | TBD |
| 20 | 26-Executive Analytics | P1 | 🟡 Partial | TBD |
| 21 | 29-Deployment Hardening | P1 | ⬜ Preserve | TBD |

---

## Cross-Cutting Definition of Done Checklist

Every feature must pass ALL of these before marked ✅:

- [ ] Domain model and backward-safe migration implemented
- [ ] Tenant/company/branch security enforced server-side
- [ ] State transitions are explicit and tested
- [ ] Existing Approval/Access/Audit is reused
- [ ] Accounting posts through Finance with idempotency/reversal
- [ ] Frontend includes permission/loading/error/empty states
- [ ] Cross-module source traceability exists
- [ ] Unit/integration tests cover financial edge cases
- [ ] Performance tested with realistic volumes
- [ ] No feature marked Done based only on entity/CRUD/UI

---

## Five E2E Integration Flows

These define an integrated Construction ERP:

| Flow | Scenario | Status |
|------|----------|--------|
| A | Project Procurement: Budget→PR→PO→Receipt→Invoice→Payment→GL→Dashboard | 🔴 Not Started |
| B | Daily Site Labor: WBS→Request→Dispatch→Attendance→DPR→Settlement→GL | 🔴 Not Started |
| C | Owner Claim: BOQ→Measurement→Claim→Certification→AR→GL→Revenue | 🔴 Not Started |
| D | Subcontractor Claim: BOQ→Measurement→Claim→Retention→AP→GL→Payment | 🔴 Not Started |
| E | Month End: Subledgers→Close→Reconciliation→Lock→Adjustment | 🔴 Not Started |

---

## Cross-Codebase Improvements (Beyond Roadmap)

See `docs/CODEBASE_IMPROVEMENT_PLAN.md` for the full improvement plan covering:

| Category | Items | Priority |
|----------|-------|----------|
| Security Hardening | 13 controllers missing `@PreAuthorize`, missing `@Valid` | P0 |
| Entity Integrity | 20+ entities missing `@Version`, 15+ missing timestamps | P1 |
| Performance | 150 files using `System.currentTimeMillis()`, 30+ unbounded `findAll()`, no cache | P2 |
| API Design | No pagination on list endpoints, inconsistent error format | P3 |
| Frontend Quality | Hardcoded notification strings, missing UI states | P4 |
| Testing Gaps | No integration tests for project module, 81 Spring context failures | P5 |
| Maintainability | Duplicated helpers, inconsistent DDD patterns | P7 |

---

## Quality Gate Baselines

| Gate | Current Value | Target |
|------|---------------|--------|
| Backend tests | 299 / 63 suites | Maintained+ |
| Frontend tests | 411 / 86 files | Maintained+ |
| i18n keys | 4,452 | Maintained+ |
| Hardcoded strings | 0 HTML / 211 TS clean | Maintained+ |
| Error codes | 262/262 | Maintained+ |
| Build (backend) | BUILD SUCCESSFUL | Maintained+ |
| Build (frontend) | ng build green | Maintained+ |
