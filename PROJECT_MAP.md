# PROJECT_MAP.md - Bemo ERP

## System Flow & Architecture
- **Backend (`be/`)**: Spring Boot, PostgreSQL, App-Scoped JWT Auth, Multi-Role Authorization, Multi-Tenant (`@TenantId`), i18n database backend (`/api/v1/i18n/{locale}`).
- **Frontend (`fe/`)**: Angular 22 standalone, SCSS, Signals, Arabic RTL default (`ar-EG`/`en-US`), Excel exports.

---

## [COMPLETED & VERIFIED]
- **API Authorization & Role Security Enforcements (P0-01)**: Enforced `@PreAuthorize` role rules across all Spring Boot controllers. Prevented unauthorized execution of disbursement, posting, and user updates at API layer (returns `403 Forbidden`).
- **Loading State & Hydration UX (P1-01, QA-015, QA-016)**: Added skeleton loaders during cold loads on `/`, `/dashboard`, `/reports`, eliminating premature zero-data empty state flashes.
- **Payroll Lifecycle & State Guardrails (P1-02)**: Blocked individual and bulk salary disbursement at both API service layer and UI components unless the payroll cycle is in `APPROVED` or `POSTED` status. Converted epoch-millisecond dates into localized human-readable dates.
- **Immutable Append-Only Audit Logging (P1-03)**: Connected `AuditService.record()` across user authentication, payroll state transitions, report approvals/reopens, inventory movements, and settings changes.
- **Linked ERP Transactions & Hierarchy (P1-04)**: Added tenant-scoped company, branch, warehouse, and department APIs. Read operations are side-effect free; production organization data is created explicitly by authorized users, while sample organization/fiscal data lives only in the test changelog.
- **QA Defects & UX Enhancements**:
  - **QA-001**: Standardized empty report status UI to "لا توجد سجلات / لا ينطبق" when 0 records exist.
  - **QA-004**: Enforced mandatory biometric device ID field for active biometric employees with inline Arabic validation message.
  - **QA-005**: Standardized inventory movement quantity inputs to positive numbers, deriving direction (issue vs receipt) from movement type.
  - **QA-007**: Synchronized dashboard year switch (2025 vs 2026) with summary cards, canceled stale HTTP requests, and preserved URL query params.
  - **QA-008 & QA-012**: Corrected employee `001` name to `محمد أحمد علي`. Replaced bare action icon buttons (`✎`, `×`) with explicit text labels, tooltips, and keyboard accessibility.
  - **QA-011**: Added missing translation keys (`payroll.maxAdvanceDeduction`, `reports.emptyNoRecords`, etc.) and ensured instant reactive locale toggle.
- **Liquibase Database Migration (`v38`)**: Production V38 now contains only mandatory translation rows and required schema fields. Its former DEMO organization, currency, employee-name, and QA-cleanup records are isolated in the test-only changelog.
- **Attendance Report Bulk Decisions (P0)**: Implemented backend bulk-decision API with operation ID for idempotency (`POST /api/v1/reports/{id}/bulk-decision`), new `AttendanceDecision` types (`ABSENCE`, `OFFICIAL_HOLIDAY`, `INDIVIDUAL_REVIEW`), device/power-outage decision persistence (`PUT /api/v1/reports/{id}/downtime-decision`). Frontend enhanced filter panel (date range, category, attendance condition, review status), recommendation counts computed from filtered data, downtime decisions saved to backend with non-blocking feedback.
- **Business Parties / Supplier Relationship Model (P0)**: Added `nameEn`, `email`, `address`, `relationshipStartDate`, `relationshipEndDate` fields to `BusinessParty` entity. Updated API DTOs with validation (`@Email`, `@Pattern` for phone/tax ID). Enforced managed-type (`DIRECT`/`MANAGED`) with responsible-party requirement for managed suppliers. Added `POST /api/v1/parties/cleanup-phone` admin endpoint. Production V44 contains the schema and generic managed-type normalization; the known invalid QA phone cleanup is test-only. Frontend form updated with new fields, validation errors, conditional date pickers for managed suppliers, and i18n keys.
- **Bilingual Translation Catalog (P1)**: Added Liquibase V52 with the 508 missing Arabic/English database rows. All 714 literal frontend keys now pass `npm run check:i18n`, and the Angular Vitest target is runnable locally and in CI.
- **Procurement Document Flow (P0)**: Implemented the connected PO/GRN/invoice/payment pipeline. PO totals are derived from validated inventory-linked lines. GRNs support partial receiving, delivered/rejected/quality-deducted quantities, warehouse location and lot trace, block over-receipt, and post only accepted quantity to inventory. Supplier invoices preserve PO, GRN, supplier, and responsible-party trace; direct/managed invoice rules are enforced. Partial and multiple payments update outstanding balances and the partner ledger. A real multi-sheet `.xlsx` export is available. Liquibase V45, V49, and V50 carry the document, trace, and inventory-receiving schema.
- **Procurement Entry UX & Configurable Numbering (P0)**: Added tenant settings for either server-generated locked numeric PO/GRN sequences or unique manual numbers. Automatic numbering continues after each company's existing highest document number. PO dates accept historical/current/future values, draft orders can update supplier/date/item/quantity/price, line units come from the inventory master, and eligible draft/issued/partially received orders are selectable with their lines in GRN entry. Supplier selection is restricted to active registered suppliers. Liquibase V53 carries the setting, sequence storage, uniqueness rules, and Arabic/English UI copy.
- **Keyboard, Navigation & Import UX (P1)**: Added form Enter-to-submit, modal Escape-to-cancel, predictable Tab focus, focus-on-open, sidebar expand-all/collapse-all controls, Arabic import completion states, and more robust Excel date parsing including unformatted serial dates and common day/month/year formats.
- **Advanced Keyboard Navigation & Tooltips (P1)**: Added permission-aware `Ctrl+K` quick navigation with search and arrow/Enter selection, `?` shortcut help, `G`-then-letter direct menu chords, a dedicated settings reference tab, and descriptive tooltips carrying shortcut hints across shell navigation and controls. Liquibase V55 carries the Arabic/English copy.
- **Dynamic Dashboard Customization & Motion (P1)**: Added server-backed per-user widget visibility/order and motion preferences, per-user admin permission switches, a Super Admin-only tenant policy for Admin accounts, richer attendance/payroll/department charts, reduced-motion support, and Liquibase V56 bilingual copy.
- **Procurement Totals, Currency & Invoice Evidence (P0)**: Fixed live PO total recalculation and save enablement, added validated transaction currency defaulted from the supplier, separated nullable supplier invoice numbers from internal references, and made delivered/rejected/deducted/accepted receipt quantities explicit. Liquibase V57 carries the schema.
- **Workforce Reliability & Analysis (P0/P1)**: Fixed the hidden manual-attendance matrix, added analytical workforce charts honoring the motion preference, real Excel exports for contractors/workers/categories, and tenant advance policies with global/category/worker precedence. Liquibase V58 carries advance policies.
- **Unified Tooltip Accessibility (P1)**: Standardized 350ms hover/focus delay, two-line RTL surfaces, Escape dismissal, `aria-describedby`, and icon-only `aria-label` coverage in the shell and audited controls; added financial and bulk-action context help.
- **GRN Reliability & Validation (P0)**: Fixed goods-receipt line persistence, server-calculated remaining PO quantities, partial/full PO transitions, accepted-quantity inventory posting, negative/over-receipt validation, explicit Arabic UI feedback, decimal quantity steps, and the responsive accessible GRN dialog.
- **Live Biometric Device Integration (P0)**: Added tenant-owned IP/API device connections, immediate and scheduled synchronization, durable status/cursor state, batch and punch idempotency, identity matching, audit logging, and an Arabic responsive management UI alongside CSV/XLS/XLSX fallback.
- **Production/Test Bootstrap Boundary (P0)**: Production always installs translations and platform roles, then idempotently ensures configured Admin and Super Admin accounts. Demo organization, attendance, currency, supplier, inventory, and legacy QA correction data is reachable only through the test changelog or an explicitly enabled `dev`/`demo` profile.
- **Deployment & PostgreSQL Verification Hardening (P0/P1)**: Production Compose now clears inherited development ports with `!reset []` so PostgreSQL and the backend are never published to the host in production; CI renders the merged production Compose and asserts private ports are absent while the frontend stays public. The backend Docker builder/runtime now use Temurin Java 26, matching the Gradle toolchain, and CI builds the image and smoke-tests the container JVM version. Supplier-payment overpayment concurrency is verified against real PostgreSQL via Testcontainers (`PostgresIntegrationTest` uses the full production Liquibase changelog + `ddl-auto=validate`, `@RepeatedTest(10)` with a thread-safe operation-ID list). This surfaced and fixed two fresh-deploy blockers in the production migrations: V80 `addUniqueConstraint` used YAML list `columnNames` (Liquibase requires a comma-separated string), and the V81 translation CSV had an unquoted embedded semicolon.
- **P0-08 (deferred, external)**: GitHub Actions still requires resolving the account/billing lock before the three CI jobs can produce independent evidence.
- **Technical-Review Hardening Batch (P0)**:
  - **P0-1/P0-2/P0-4 — Tenant & Evidence Integrity**: Tenant-owned aggregates keep `@TenantId` app scoping with immutable audit timestamps on evidence; shared idempotency (`operationId`) handling verified across bulk decisions, payroll transitions, and supplier payments; API dates remain epoch-milliseconds.
  - **P0-3 — Role-Based Security Parity**: Every domain controller now carries explicit `@PreAuthorize` sets matching backend role codes. Finance (`FINANCE_MANAGER`, `ACCOUNTANT`, `TREASURY_USER`, `HR_MANAGER`, `AUDITOR`), procurement (readers include `PROCUREMENT_USER`, `INVENTORY_MANAGER`; writes restricted to `PROCUREMENT_MANAGER`), sales/manufacturing/quality/payroll sets enforced; HR-domain controllers intentionally keep HR/ADMIN roles. Violations return `403`.
  - **P0-5 — Finance Journal State Machine**: Journal entries expose explicit state (`DRAFT`/`POSTED`/`REVERSED`) with guarded post/reverse transitions, and fiscal periods enforce open/closed lifecycle (`FiscalPeriodGuard`) so posting into a closed period is blocked.
  - **P0-6 — Fiscal Close Enforcement in Procurement**: `ProcurementService` now applies `FiscalPeriodGuard` to supplier-invoice and payment posting dates, preventing documents dated inside closed periods.
  - **P0-7 — Optimistic Locking**: `@Version` optimistic locking added to 14 mutable aggregates (PurchaseOrder, GoodsReceipt, SupplierInvoice, SupplierPayment, SalesOrder, ProductionOrder, QualityInspection, WorkforceSettlementPeriod, WorkforceAdvance, ManualAttendanceEntry, LaborRequest, BankAccount, Currency, TaxRate) with Liquibase V84 columns; concurrent edits now fail with a stale-state conflict.
  - **Backend Test Suite 108/108 Green**: Fixed a stale desktop `SpaForwardController` duplicate, corrected the V82 FK to reference the actual `apps` table, replaced a broken `@Lock` derived query with an explicit `@Query`, aligned `SecurityConfig`/access-denied handler with Jackson 3 (`tools.jackson.databind`), and made supplier-payment concurrency tests seed an open fiscal period. Verified against both H2 and PostgreSQL Testcontainers (full production Liquibase changelog + `ddl-auto=validate`).
  - **Frontend Session & Role Hardening**: Access tokens live in memory only (persisted session stores metadata, never the token) with single-flight refresh-on-401 retry and startup rehydration; `RoleCode` extended with the 14 domain roles; forced-password-change flow (`POST /auth/change-password`) with a dedicated change-password page, route, and i18n (V85 translations); workforce menu bypass removed and domain route/nav guards mirror the backend role sets; user-management role options and role labels cover all domain roles.

---

## [ORPHANS & PENDING]
*(See Bemo ERP — Remaining and Incomplete Work document for full backlog)*
- **P1**: Complete remaining procurement commercial metadata (expected delivery, line tax/discount), controlled financial-discount approval, and cancellation/reversal ledger postings. Exchange-rate snapshots are implemented (V63) and verified — see COMPLETED & VERIFIED.
- **P1**: Unified category master (employee/worker scope), enhanced work-schedule rules with overlap detection
- **P1**: Complete one unified employee/worker category source; advance deduction policy inheritance is implemented, while broader payroll deduction-policy lifecycle remains
- **P1**: Finish remaining bilingual labels and per-flow retry feedback standardization
- **P1**: Finish Excel export coverage for remaining operational screens beyond workforce master data
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
- [x] Mandatory translations, roles, Admin, and Super Admin bootstrap with production credentials; DEMO organization/fiscal fixtures isolated from production
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
- [x] Live PO totals, transaction currency, and true supplier-without-invoice references
- [x] Visible manual-attendance matrix independent of the calculation-rules banner
- [x] Workforce analytical charts and Excel exports for contractors, workers, and worker categories
- [x] Global advance policy with category/worker exceptions and per-advance override
- [x] Unified delayed RTL tooltips with Escape and icon-button accessibility labels
- [x] Supplier-scoped invoice payments with outstanding-balance validation and idempotent operation IDs
- [x] Versioned workforce settlement recalculation summaries, stale-input detection, issues, and guarded lifecycle
- [x] Complete workforce Excel import workflow with mapping, validation, preview, duplicate detection, error workbook, commit, and reversal
- [x] Persisted biometric day-anomaly detection with configurable thresholds, impact preview, decisions, defer/reopen, idempotency, audit, and reversal
- [x] Transactional manual-attendance dirty-cell saving with reload persistence, per-cell errors, summary, and unsaved-change guard
- [x] Frozen procurement exchange-rate snapshots, base-currency totals, override reasons, and base-value ledger postings
- [x] Effective-dated versioned advance policies with applied snapshots on advances and settlements
- [x] Employee advances use the shared installment workflow, employee/category policy overrides, payroll allocation, reversal, and employee-ledger synchronization
- [x] URL-persisted workforce dashboard filters with KPI/chart drill-down and a reusable accessible icon-button component
- [x] Issued-PO goods receipts persist, update remaining quantities/status, and post accepted inventory with field-level quantity validation
- [x] Live biometric IP/API device synchronization with scheduling, idempotency, persisted results, and upload fallback
- [x] Legacy QA phone/reversal scenarios retained under the test-only changelog and excluded from production
- [x] `@Version` optimistic locking on 14 mutable aggregates (V84) with stale-state conflict handling
- [x] Fiscal period open/closed guard enforced on journal posting and procurement invoice/payment dates
- [x] Journal entries expose explicit state with guarded post/reverse transitions
- [x] In-memory access tokens with refresh-on-401 retry, startup rehydration, and no token persistence
- [x] Forced password change flow (must-change-password) with dedicated page, route, guard, and V85 translations
- [x] Domain role codes propagated end-to-end: backend `@PreAuthorize`, frontend `RoleCode`, route guards, nav visibility, user-management options
- [x] Backend suite green (108 tests) on H2 and PostgreSQL Testcontainers after Jackson 3, SPA-forward, and V82 FK corrections
