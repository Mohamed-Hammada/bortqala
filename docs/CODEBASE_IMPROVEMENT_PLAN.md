# Bemo ERP — Cross-Codebase Improvement Plan

**Created:** 2026-08-20  
**Scope:** All 30 modules — improvements beyond the audit roadmap  
**Goal:** Production-grade quality, security, performance, and maintainability

---

## Executive Summary

The audit roadmap covers feature gaps. This plan covers **code quality, security, performance, and engineering excellence** across the entire existing codebase. These are the improvements that separate "demo-ready" from "production-ready."

---

## Category 1: Security Hardening (CRITICAL)

### SEC-001: Missing `@PreAuthorize` on Controllers — ✅ RESOLVED (Session 18)
All controllers across all modules now have explicit, verified `@PreAuthorize` annotations matching canonical `RoleCode` definitions.
- Verified by: `python tools/check-authorization-contract.py` (PASS: 20 declared, 20 referenced, 0 unknown).

| Controller | Status | Applied Role Scope |
|-----------|--------|-------------------|
| `EtaComplianceController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `FINANCE_MANAGER`, `ACCOUNTANT` |
| `InventoryAnalyticsController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `INVENTORY_MANAGER`, `FINANCE_MANAGER`, `VIEWER` |
| `PerformanceAppraisalController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `HR_MANAGER`, `HR_REVIEWER` |
| `DashboardController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `HR_MANAGER`, `HR_REVIEWER`, `FINANCE_MANAGER`, `PROJECT_MANAGER`, `VIEWER` |
| `DataExportController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `HR_MANAGER`, `HR_REVIEWER`, `FINANCE_MANAGER`, `VIEWER` |
| `DataExchangeController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN` |
| `SalesQuotationController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `SALES_MANAGER`, `VIEWER` |
| `WorkforceExcelImportController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `WORKFORCE_MANAGER` |
| `SystemAboutController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `HR_MANAGER`, `VIEWER` |
| `SampleTemplateController` | ✅ Resolved | `SUPER_ADMIN`, `ADMIN`, `HR_MANAGER`, `HR_REVIEWER`, `FINANCE_MANAGER`, `WORKFORCE_MANAGER`, `INVENTORY_MANAGER`, `VIEWER` |
| `ScreenShortcutController` | ✅ Resolved | `isAuthenticated()` |
| `TranslationController` | ✅ Verified | Public bundle initialized before auth |

### SEC-002: Missing `@Valid` on `@RequestBody` — ✅ RESOLVED (Session 18)
All `@RequestBody` parameters across all Project, WBS, Approval, and Configuration controllers have `@Valid` annotations to enforce Bean Validation constraints.

### SEC-003: Hardcoded "system" User Fallback
Multiple services use:
```java
private String getCurrentUser() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null && auth.getName() != null) ? auth.getName() : "system";
}
```
In production, if auth context is missing, audit records show "system" — hiding the real actor.

**Fix:** Throw `IllegalStateException` in production profile instead of defaulting to "system".

---

## Category 2: Entity Integrity (HIGH)

### ENT-001: Missing `@Version` on Mutable Entities
**20+ mutable entities** lack optimistic locking. Concurrent updates can silently overwrite:

- `ApprovalDecision`, `ApprovalWorkflowDefinition`, `ApprovalWorkflowStep`
- `BiometricDevice`, `BiometricSource`, `ImportBatch`
- `BudgetRevision`, `BudgetTransfer`
- `EmployeeAssignment`, `ScheduleRule`
- `Account`, `BankAccount`, `FiscalPeriod`
- `JournalEntryLine`, `BankReconciliationMatch`

**Impact:** Race conditions in multi-user scenarios.  
**Effort:** 2-3 hours — add `@Version` + `version` column to Liquibase migration.

### ENT-002: Missing Timestamps on Entities
**15+ entities** lack `created_at`/`updated_at`:

- `ApprovalDecision`, `ApprovalInstance`, `ApprovalWorkflowStep`
- `ImportBatch`, `ImportRowError`, `PunchRecord`
- `AuditLog`, `ConfirmedHoliday`
- `JournalEntryLine`, `BankReconciliationMatch`

**Impact:** No audit trail for when records were created/modified.  
**Effort:** 1-2 hours — add columns + `@PrePersist`/`@PreUpdate`.

### ENT-003: Missing `@TenantId` on Entities
**10 entities** lack multi-tenant isolation (some are intentionally global):

| Entity | Should Have @TenantId? | Reason |
|--------|----------------------|--------|
| `TranslationEntry` | No (global) | Shared translations |
| `AppUser` | Depends | May need cross-tenant admin |
| `TenantApplication` | No (global) | Tenant registry |
| `TenantFeature` | No (global) | Feature flags |
| `SystemSetting` | Depends | May need tenant scoping |

**Action:** Audit each entity — most `shared.security.*` entities are intentionally global, but `IndustryPack` and `SubscriptionPlan` should likely be tenant-scoped.

---

## Category 3: Performance (HIGH)

### PERF-001: 150 Files Using `System.currentTimeMillis()`
The backend skill says: *"Use `Instant` for audit/import timestamps."* But 150 files use `System.currentTimeMillis()`.

**Impact:** 
- Timezone-unaware timestamps (stored as epoch millis, ambiguous)
- Inconsistent with `Instant.now()` used elsewhere
- Makes date-range queries error-prone

**Fix:** Migrate to `Instant.now().toEpochMilli()` or better yet, use `Instant` directly in new code. Existing code can be migrated incrementally.

### PERF-002: 30+ Services Using Unbounded `findAll()`
Services loading entire tables into memory:

| Service | Risk | Fix |
|---------|------|-----|
| `ExecutiveAnalyticsService` | All KPIs in memory | Paginate or filter by period |
| `FinancialStatementsReportService` | All journal entries | Filter by fiscal period |
| `GeneralLedgerReportService` | All accounts | Use pagination |
| `PayrollService` | All employees | Filter by active/period |
| `InventoryValuationSnapshotService` | All items | Paginate |
| `ProjectExecutiveDashboardService` | All projects | Filter by status/company |

**Impact:** OOM at scale (10K+ records).  
**Effort:** 1-2 days across all services.

### PERF-003: No Hibernate Second-Level Cache
No entity uses `@Cache` annotations. Frequently-read master data (accounts, cost centers, departments, roles) is fetched from DB on every request.

**Fix:** Add `@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)` to:
- `Account`, `CostCenter`, `Department`, `Warehouse`
- `PayrollComponent`, `LeaveType`, `Role`
- `ProjectCostCode`, `Currency`

**Impact:** Reduces DB load by 30-50% for master data reads.

### PERF-004: Missing Database Indexes
Repository methods without corresponding indexes cause full table scans:

| Repository Method | Missing Index |
|-------------------|---------------|
| `findByProjectIdAndStatus` | `(app_id, project_id, status)` |
| `findByCompanyIdAndStatus` | `(app_id, company_id, status)` |
| `findByEntityTypeOrderByOccurredAtDesc` | `(entity_type, occurred_at)` |
| `findByBatchIdOrderByRowNumber` | `(batch_id, row_number)` |

---

## Category 4: Frontend Quality (MEDIUM)

### FE-001: Hardcoded Notification Strings in TS
**20+ files** use hardcoded notification messages instead of i18n keys:

```typescript
// BAD:
this.notification.success('تم الحفظ بنجاح');
// GOOD:
this.notification.success(this.i18n.t('common.savedSuccess'));
```

**Affected files:** imports, operations, reports, settings, users, etc.

### FE-002: Missing Error/Loading/Empty States
Some pages don't handle all UI states properly:

| Page | Missing |
|------|---------|
| `project-detail.page.ts` | May lack retry on failed loads |
| `project-schedule-gantt.component.ts` | Error state handling |
| Several feature pages | Empty state after failed search |

### FE-003: Inconsistent Date Formatting
Dates are formatted differently across pages. Should use centralized `formatDate()` with timezone config.

### FE-004: Missing Unsaved-Change Protection
Complex forms (project create, WBS editor, claim forms) don't warn about unsaved changes on navigation.

---

## Category 5: Testing Gaps (MEDIUM)

### TEST-001: No Integration Tests for Project Module
Only unit tests exist (mocked repos). Missing:
- Tenant isolation tests (cross-tenant access denial)
- WBS cycle detection with real DB
- Project closure with in-progress WBS validation
- Concurrent modification (optimistic locking) tests

### TEST-002: Missing Controller Tests
Most controllers lack `@WebMvcTest` tests:
- `ProjectController` — no authorization tests
- `WbsController` — no validation tests
- `ProjectDailyReportController` — no state transition tests

### TEST-003: Missing Frontend Component Tests
- `project-detail.page.spec.ts` — doesn't exist
- `project-schedule-gantt.component.spec.ts` — doesn't exist
- `cost-control.service.spec.ts` — minimal coverage

### TEST-004: 81 Spring Context Failures
The full test suite has 81 failures from `DefaultCacheAwareContextLoaderDelegate` — these are H2/Liquibase compatibility issues with V269-V330 migrations that need investigation.

---

## Category 6: API Design (MEDIUM)

### API-001: No Pagination on List Endpoints
Many endpoints return unbounded lists:

| Endpoint | Issue |
|----------|-------|
| `GET /projects` | Returns all projects |
| `GET /projects/{id}/wbs` | Returns all WBS nodes |
| `GET /projects/{id}/roles` | Returns all roles |
| `GET /cost-codes` | Returns all cost codes |

**Fix:** Add `Pageable` support with sensible defaults (page=0, size=50).

### API-002: No Consistent Error Response Format
Some endpoints return plain strings, others return RFC 9457 Problem Details. Should standardize.

### API-003: No API Versioning Strategy
All endpoints use `/api/v1/`. Need a strategy for breaking changes.

---

## Category 7: Code Maintainability (LOW)

### MAINT-001: Duplicated `getCurrentUser()` Pattern
The same `getCurrentUser()` helper is copy-pasted across 20+ services. Should be extracted to a shared utility or use a `@CurrentSecurityContext` annotation.

### MAINT-002: Duplicated `toEpoch()`/`fromEpoch()` Converters
Date conversion helpers are duplicated in ProjectService, WbsService, and other services. Should be in a shared utility.

### MAINT-003: Entity Business Logic in Domain vs Service
Some entities (Project, WbsNode) have business logic in the entity class (good DDD), but others put all logic in services. Should be consistent.

---

## Priority Execution Order

| Priority | Category | Items | Effort | Impact |
|----------|----------|-------|--------|--------|
| P0 | Security | SEC-001, SEC-002, SEC-003 | 4 hours | Critical — prevents unauthorized access |
| P1 | Entity Integrity | ENT-001, ENT-002 | 4 hours | High — prevents data races |
| P2 | Performance | PERF-001, PERF-002 | 1-2 days | High — prevents OOM at scale |
| P3 | API Design | API-001 | 1 day | High — proper data access patterns |
| P4 | Frontend | FE-001, FE-002 | 1 day | Medium — UX consistency |
| P5 | Testing | TEST-001, TEST-002 | 2 days | Medium — confidence in changes |
| P6 | Performance | PERF-003, PERF-004 | 1 day | Medium — DB load reduction |
| P7 | Maintainability | MAINT-001, MAINT-002 | 1 day | Low — developer experience |
