# Bemo ERP — PBAC Controller Authorization Migration & Architecture Implementation Plan

**Last Updated:** 2026-08-20
**Current Status:** All Controller Waves (Waves 1–4) Migrated & Compilation Verified (BUILD SUCCESSFUL)

---

## 1. Executive Summary & Progress Dashboard

This document tracks the complete transition of controller endpoint security across the entire Bemo ERP platform from hardcoded `RoleCode` sets (`hasAnyRole(...)`) to granular Policy-Based Access Control (PBAC) using `@auth.hasPermission(...)` and `@auth.hasAnyPermission(...)` SpEL expressions backed by `AccessCatalog` permission constants.

### Master Progress Dashboard

| Milestone | Scope / Domain | Status | Key Permissions Enforced |
|---|---|---|---|
| **M1** | Architecture Plan & Decision Matrix | ✅ **COMPLETED** | PBAC Strategy & In-Memory Cache Design |
| **M2** | In-Memory Request-Scoped Permission Cache | ✅ **COMPLETED** | `EffectivePermissionCache` (@Component, RequestScope) |
| **M3** | Wave 1: Core Admin, Settings, Users, Organization, Audit | ✅ **COMPLETED** | `audit.read`, `organization.manage`, `users.read`, `users.manage`, `settings.read`, `settings.manage` |
| **M4** | Wave 2: HR, Employee, Attendance, Reports, Leave, Dashboard | ✅ **COMPLETED** | `employees.read`, `employees.edit`, `employees.deactivate`, `categories.manage`, `imports.read`, `imports.manage`, `reports.read`, `reports.decide`, `reports.approve`, `leaves.read`, `leaves.manage`, `dashboard.view` |
| **M5** | Wave 3: Finance, Budget, Workforce, Settlements, Approvals | ✅ **COMPLETED** | `finance.read`, `finance.manage`, `journal.create`, `journal.post`, `journal.reverse`, `budget.read`, `budget.manage`, `workers.read`, `workers.create`, `workers.edit`, `contractors.read`, `contractors.manage`, `laborRequests.read`, `laborRequests.manage`, `settlements.read`, `settlements.prepare`, `settlements.finalize`, `approvals.read`, `approvals.decide`, `workflowDefinitions.read`, `workflowDefinitions.manage` |
| **M6** | Wave 4: Trade, Procurement, Sales, POS, CRM, Manufacturing, ETA, Project | ✅ **COMPLETED** | `procurement.read`, `procurement.manage`, `sales.read`, `sales.manage`, `pos.read`, `pos.manage`, `pos.operate`, `crm.read`, `crm.manage`, `crm.omnichannel`, `manufacturing.read`, `manufacturing.manage`, `projects.read`, `projects.manage`, `projects.close`, `etaTax.read`, `etaTax.manage` |
| **M7** | Security Audit & Denied Authorization Logging | ✅ **COMPLETED** | Request-scoped evaluation with safe warning logs in `SecurityAuthorizationEvaluator` |
| **M8** | Verification, CI Gates & Contract Synchronization | ✅ **COMPLETED** | Full Gradle compileJava verified (`BUILD SUCCESSFUL`) |

---

## 2. Migrated Controller Directory

| Controller File | Permissions Enforced |
|---|---|
| `AuditLogController.java` | `audit.read` (preserves break-glass admin check) |
| `OrganizationController.java` | `organization.manage`, `finance.manage` |
| `AuthController.java` | `users.read`, `users.manage`, `settings.read`, `settings.manage` |
| `EmployeeController.java` | `employees.edit`, `employees.deactivate` |
| `EmployeeContractController.java` | `employees.read`, `employees.edit` |
| `CategoryController.java` | `categories.manage` |
| `BiometricImportController.java` | `imports.read`, `imports.manage` |
| `AttendanceExplorerController.java` | `imports.read` |
| `DeviceIntegrationController.java` | `imports.read`, `imports.manage` |
| `AttendanceExceptionController.java` | `attendance.read`, `attendance.review`, `reports.read`, `reports.decide` |
| `AttendanceReportRefreshController.java` | `reports.decide` |
| `ReportController.java` | `reports.read`, `reports.decide`, `reports.approve` |
| `LeaveManagementController.java` | `leaves.read`, `leaves.manage` |
| `DashboardController.java` | `dashboard.view` |
| `DataExportController.java` | `reports.read`, `dashboard.view`, `finance.read`, `employees.read` |
| `AccountingController.java` | `finance.read`, `finance.manage`, `journal.create`, `journal.post`, `journal.reverse` |
| `BudgetController.java` | `budget.read`, `budget.manage` |
| `BudgetVersionController.java` | `budget.read`, `budget.manage` |
| `WorkerController.java` | `workers.read`, `workers.create`, `workers.edit` |
| `ContractorController.java` | `contractors.read`, `contractors.manage` |
| `LaborRequestController.java` | `laborRequests.read`, `laborRequests.manage` |
| `WorkforceSettlementController.java` | `settlements.read`, `settlements.prepare`, `settlements.finalize` |
| `ApprovalController.java` | `approvals.read`, `approvals.decide`, `workflowDefinitions.read`, `workflowDefinitions.manage` |
| `ProcurementController.java` | `procurement.read`, `procurement.manage`, 3-way matching |
| `SalesController.java` | `sales.read`, `sales.manage` |
| `PosController.java` | `pos.read`, `pos.manage`, `pos.operate` |
| `CrmController.java` | `crm.read`, `crm.manage`, `crm.omnichannel` |
| `ManufacturingController.java` | `manufacturing.read`, `manufacturing.manage` |
| `ProjectController.java` | `projects.read`, `projects.manage`, `projects.close` |
| `EtaComplianceController.java` | `etaTax.read`, `etaTax.manage` |

---

## 3. Verification & Acceptance Criteria

- **Compilation:** `gradlew compileJava` passing with 0 errors across all 30+ packages.
- **Dual-Mode Compatibility:** Admin roles (`SUPER_ADMIN`, `ADMIN`) bypass fine-grained checks via `isAdmin(auth)` fast-path in `SecurityAuthorizationEvaluator`, while standard users evaluate effective policy sets through `PolicyGroupService` with request-scoped caching via `EffectivePermissionCache`.
- **Frontend Alignment:** Directives (`*hasPermission`) and route guards (`permissionGuard`) use the exact same canonical string keys from `AccessCatalog.java`.
