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
- **Attendance Report Bulk Decisions (P0)**: Implemented backend bulk-decision API with operation ID for idempotency (`POST /api/v1/reports/{id}/bulk-decision`), new `AttendanceDecision` types (`ABSENCE`, `OFFICIAL_HOLIDAY`, `INDIVIDUAL_REVIEW`), device/power-outage decision persistence (`PUT /api/v1/reports/{id}/downtime-decision`). Frontend enhanced filter panel (date range, category, attendance condition, review status), recommendation counts computed from filtered data, downtime decisions saved to backend with non-blocking feedback.
- **Business Parties / Supplier Relationship Model (P0)**: Added `nameEn`, `email`, `address`, `relationshipStartDate`, `relationshipEndDate` fields to `BusinessParty` entity. Updated API DTOs with validation (`@Email`, `@Pattern` for phone/tax ID). Enforced managed-type (`DIRECT`/`MANAGED`) with responsible-party requirement for managed suppliers. Added `POST /api/v1/parties/cleanup-phone` admin endpoint. Created Liquibase `v44` migration for new columns + data cleanup (`NOT-A-PHONE`, legacy managed types). Frontend form updated with new fields, validation errors, conditional date pickers for managed suppliers, and i18n keys.
- **Bilingual Translation Catalog (P1)**: Added Liquibase V52 with the 508 missing Arabic/English database rows. All 714 literal frontend keys now pass `npm run check:i18n`, and the Angular Vitest target is runnable locally and in CI.
- **Procurement Document Flow (P0)**: Implemented the connected PO/GRN/invoice/payment pipeline. PO totals are derived from validated inventory-linked lines. GRNs support partial receiving, delivered/rejected/quality-deducted quantities, warehouse location and lot trace, block over-receipt, and post only accepted quantity to inventory. Supplier invoices preserve PO, GRN, supplier, and responsible-party trace; direct/managed invoice rules are enforced. Partial and multiple payments update outstanding balances and the partner ledger. A real multi-sheet `.xlsx` export is available. Liquibase V45, V49, and V50 carry the document, trace, and inventory-receiving schema.
- **Procurement Entry UX & Configurable Numbering (P0)**: Added tenant settings for either server-generated locked numeric PO/GRN sequences or unique manual numbers. Automatic numbering continues after each company's existing highest document number. PO dates accept historical/current/future values, draft orders can update supplier/date/item/quantity/price, line units come from the inventory master, and eligible draft/issued/partially received orders are selectable with their lines in GRN entry. Supplier selection is restricted to active registered suppliers. Liquibase V53 carries the setting, sequence storage, uniqueness rules, and Arabic/English UI copy.
- **Keyboard, Navigation & Import UX (P1)**: Added form Enter-to-submit, modal Escape-to-cancel, predictable Tab focus, focus-on-open, sidebar expand-all/collapse-all controls, Arabic import completion states, and more robust Excel date parsing including unformatted serial dates and common day/month/year formats.
- **Advanced Keyboard Navigation & Tooltips (P1)**: Added permission-aware `Ctrl+K` quick navigation with search and arrow/Enter selection, `?` shortcut help, `G`-then-letter direct menu chords, a dedicated settings reference tab, and descriptive tooltips carrying shortcut hints across shell navigation and controls. Liquibase V55 carries the Arabic/English copy.
- **Dynamic Dashboard Customization & Motion (P1)**: Added server-backed per-user widget visibility/order and motion preferences, per-user admin permission switches, a Super Admin-only tenant policy for Admin accounts, richer attendance/payroll/department charts, reduced-motion support, and Liquibase V56 bilingual copy.

---

## [ORPHANS & PENDING]
*(See Bemo ERP — Remaining and Incomplete Work document for full backlog)*
- **P0**: Execute and verify historical negative-stock cleanup using the implemented approved adjustment workflow and before/after exception report
- **P0/P1**: Complete procurement commercial metadata (currency/exchange rate, expected delivery, line tax/discount), controlled financial-discount approval, and cancellation/reversal ledger postings
- **P1**: Unified category master (employee/worker scope), enhanced work-schedule rules with overlap detection
- **P1**: Complete one unified employee/worker category source and the deduction-policy inheritance/lifecycle
- **P1**: Finish remaining bilingual labels and per-flow retry feedback standardization
- **P1**: Excel export coverage across all operational screens
- **P2**: Detailed dashboards with charts and drill-down
- **P2**: Extend detailed charts/drill-down beyond the main dashboard to every department dashboard

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
- [x] Attendance report bulk decisions with idempotent backend API, filtered recommendation counts, device outage persistence
- [x] Supplier direct/managed relationship model, responsible partner, email/address/nameEn fields, field validation, data cleanup
- [x] Full procurement document flow: GRN with lines, supplier invoices with discount/tax, payments with auto-ledger, PO receive/cancel
- [x] Configurable automatic/manual PO and GRN numbering with per-company locked sequences and immutable automatic numbers
- [x] Procurement date, supplier, unit, draft editing, and PO-to-GRN selection UX
- [x] Form/modal keyboard navigation plus sidebar expand-all/collapse-all controls
- [x] Arabic import statuses and corrected Excel serial/common-format date parsing
- [x] Inventory-linked partial GRN receiving with delivered/rejected/deducted/accepted quantities and accepted-stock posting
- [x] Server-backed Favorites and Recently Used visibility, limits, history clearing, and favorite reset
- [x] Manual worker attendance contractor/category/status filters and selected worker/date bulk preview controls
- [x] Per-user dynamic dashboard layouts, Super Admin/Admin policy hierarchy, richer charts, and optional accessible motion
