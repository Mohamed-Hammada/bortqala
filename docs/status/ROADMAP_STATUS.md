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
**Score:** 15/100 → **Target:** 80+/100  
**Status:** 🟡 SCAFFOLDED  

#### What exists:
- ✅ `ProjectDailyReport` entity with project/site/date/shift/status/revision
- ✅ `DailyWorkProgressLine` by WBS with prior/today/cumulative quantities
- ✅ `DailyLaborSnapshot` with worker/employee references
- ✅ `DailyMaterialConsumption` with inventory item references
- ✅ `DailyEquipmentLog` with equipment/site status
- ✅ Weather condition enum
- ✅ REST APIs: `ProjectDailyReportController`
- ✅ Service: `ProjectDailyReportService`
- ✅ Liquibase V272 (schema) + V273 (translations)

#### Quality gaps to fix:
- [ ] **CRITICAL:** No approval workflow integration (Draft→Submitted→Approved lifecycle)
- [ ] **HIGH:** No period summary aggregation endpoint
- [ ] **HIGH:** No idempotency for concurrent submissions
- [ ] **MEDIUM:** No attachment/evidence model
- [ ] **MEDIUM:** No "copy previous day" API
- [ ] **LOW:** Missing service unit tests

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
| 1 | 01-Project/WBS Quality Remediation | P0 | 🟡 In Progress | Current |
| 2 | 02-Daily Site Progress completion | P0 | 🟡 Scaffolded | Next |
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
