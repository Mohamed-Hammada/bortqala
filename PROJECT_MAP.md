# PROJECT_MAP.md - Bemo HR & Operations Platform

## System Flow & Architecture
- **Backend (`be/`)**: Spring Boot, PostgreSQL, App-Scoped JWT Auth, Multi-Role Authorization, Multi-Tenant (`@TenantId`), i18n database backend (`/api/v1/i18n/{locale}`).
- **Frontend (`fe/`)**: Angular 22 standalone, SCSS, Signals, Arabic RTL default (`ar-EG`/`en-US`), Excel exports.

---

## [COMPLETED & VERIFIED]
- **API Authorization & Role Security Enforcements (P0-01)**: Enforced `@PreAuthorize` role rules across all Spring Boot controllers. Prevented unauthorized execution of disbursement, posting, and user updates at API layer (returns `403 Forbidden`).
- **Loading State & Hydration UX (P1-01, QA-015, QA-016)**: Added skeleton loaders during cold loads on `/`, `/dashboard`, `/reports`, eliminating premature zero-data empty state flashes.
- **Payroll Lifecycle & State Guardrails (P1-02)**: Blocked individual and bulk salary disbursement at both API service layer and UI components unless the payroll cycle is in `APPROVED` or `POSTED` status. Converted epoch-millisecond dates into localized human-readable dates.
- **Immutable Append-Only Audit Logging (P1-03)**: Connected `AuditService.record()` across user authentication, payroll state transitions, report approvals/reopens, inventory movements, and settings changes.
- **Linked ERP Transactions & Hierarchy (P1-04)**: Auto-bootstrapped default DEMO company (`30000000-0000-0000-0000-000000000001`), main branch, warehouse, and department. Auto-seeded 2026 fiscal periods for accounting entries.
- **QA Defects & UX Enhancements**:
  - **QA-001**: Standardized empty report status UI to "لا توجد سجلات / لا ينطبق" when 0 records exist.
  - **QA-004**: Enforced mandatory biometric device ID field for active biometric employees with inline Arabic validation message.
  - **QA-005**: Standardized inventory movement quantity inputs to positive numbers, deriving direction (issue vs receipt) from movement type.
  - **QA-007**: Synchronized dashboard year switch (2025 vs 2026) with summary cards, canceled stale HTTP requests, and preserved URL query params.
  - **QA-008 & QA-012**: Corrected employee `001` name to `محمد أحمد علي`. Replaced bare action icon buttons (`✎`, `×`) with explicit text labels, tooltips, and keyboard accessibility.
  - **QA-011**: Added missing translation keys (`payroll.maxAdvanceDeduction`, `reports.emptyNoRecords`, etc.) and ensured instant reactive locale toggle.
- **Liquibase Database Migration (`v38`)**: Created `20260728_v38_audit_payroll_erp_fixes.yaml` for database cleanup, employee name fixes, default company bootstrapping, and translation rows.

---

## [ORPHANS & PENDING]
*(None - All launch requirements, P0/P1 items, audit trail logs, payroll guardrails, ERP linkages, and QA defects have been completed, tested, and verified)*

---

## [SYSTEM_FLOW Checklist]
- [x] Skeleton UI rendered on initial load across features
- [x] Dynamic reactive i18n translation without page reload
- [x] Formatted dates across all tables/views
- [x] Visual Payroll Stepper and explicit status flow guardrails
- [x] Tabbed Settings with unified save action
- [x] Collapsible Sidebar with Favorites & Recent Items
- [x] Global Toasts, Unsaved Form Guards & Action Confirmation Dialogs
- [x] High-contrast 40-44px touch targets & informative Empty States
- [x] Append-only Audit Trail logging active across all sensitive actions
- [x] Mandatory biometric device ID validation for active biometric employees
- [x] Single positive quantity input model for inventory movements
- [x] Auto-bootstrapped DEMO company and 2026 fiscal periods
