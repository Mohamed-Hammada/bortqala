# AGENT SESSION SUMMARY — July 29, 2026
## Session 1: Attendance Bulk Decisions + Business Parties Extended
- **P0: Attendance Report Bulk Decisions** — Backend: `POST /api/v1/reports/{id}/bulk-decision` (operation-ID idempotency), `PUT /api/v1/reports/{id}/downtime-decision`. New `AttendanceDecision` types: `ABSENCE`, `OFFICIAL_HOLIDAY`, `INDIVIDUAL_REVIEW`. Frontend: enhanced filter panel (date, category, attendance condition, review status), recommendation counts from filtered data, downtime persistence. Audit logging.
- **P0: Business Parties/Suppliers** — Added `nameEn`, `email`, `address`, `relationshipStartDate`, `relationshipEndDate` to entity, API DTOs, frontend models/form/template. Validation: `@Email`, phone pattern, tax ID pattern. Managed-type restricted to `DIRECT`/`MANAGED`; responsible-party required for managed. Data cleanup endpoint `POST /api/v1/parties/cleanup-phone`. Liquibase `v44` migration for columns + data cleanup. New i18n keys for parties page.
- Updated `PROJECT_MAP.md` with both completed items.
## Session 2: Procurement Document Flow (GRN, Invoices, Payments)
- **P0: Procurement Document Flow** — Backend: GoodsReceipt, SupplierInvoice, SupplierPayment entities + repos + API DTOs + service. PO transitions: `receive()` (ISSUED→RECEIVED), `cancel()`. GRN creation auto-transitions PO to RECEIVED. Invoice creation auto-creates partner ledger debit entry. Payment creation auto-marks invoice PAID + creates partner ledger credit entry. Extracted `PartnerLedgerEntryRepository` as public. Controller: `GET/POST /goods-receipts`, `/invoices`, `/payments`, `POST /orders/{id}/receive`, `POST /orders/{id}/cancel`. Liquibase v45: `goods_receipt_lines` table, GRN notes column, invoice discount/tax/net/due_date/notes columns. Frontend: tabbed procurement page (PO/GRN/Invoices/Payments), centered dialogs for each document type, receive/cancel PO actions, invoice with discount/tax calc, payment linked to unpaid invoices. All 5 workflow steps active.

# HR platform handoff

This repository intentionally contains two applications:

- `be/`: Spring Boot backend. Before changing it, read `be/skills/hr-backend/SKILL.md` completely.
- `fe/`: Angular frontend. Before changing it, read `fe/skills/hr-frontend/SKILL.md` completely.

For changes spanning both applications, define the backend API contract first, then update the typed frontend model and data-access layer. Keep business calculations in the backend; the frontend may format results but must not reimplement attendance or payroll rules.

Current phase: the end-to-end MVP is implemented and verified on PostgreSQL. It includes SaaS app-scoped JWT authentication, per-user theme/density/locale preferences, database-backed Arabic/English translations, multi-role authorization, dynamic attendance categories and schedules, custom report ranges and pay-cycle presets, biometric imports, attendance review, approval/reopen, dashboards, Excel exports, epoch-millisecond API dates, and structured tracing. Tenant-owned entities must keep `@TenantId`; mutable aggregates keep `created_at`/`updated_at`, while immutable evidence uses semantic creation/import timestamps. Read both local skills before extending it.

## Menu Registration & Permission Synchronization Protocol

Whenever creating or adding a new feature/module with sidebar menu items, enforce the following 4-part synchronization protocol to guarantee instant menu visibility across all user roles and sessions:

1. **Frontend Visibility (`app-shell.component.ts`)**:
   - Register items in `items` array with appropriate `workspace` group key.
   - Update `visible(item)` and `AuthService.hasMenuAccess(menuId)` so that admin roles (`SUPER_ADMIN`, `ADMIN`) and new feature menu IDs (e.g. `workforce-*`) are explicitly returned as `true`, overriding obsolete local storage session arrays.
2. **Database Translation Keys & Fallbacks (`i18n.service.ts` & CSV)**:
   - Add the workspace section key (`workspace.<name>`) and nav label keys to `DEFAULT_FALLBACKS` in `i18n.service.ts` (both `ar-EG` and `en-US`).
   - Add translation rows to the Liquibase translation CSV (e.g. `workspace.workforce`).
3. **Database User Schema Migration (`v37` Liquibase)**:
   - Add a Liquibase changeset executing SQL update on `app_users.allowed_menus` to append the new menu IDs to existing user rows in PostgreSQL.
   - Update default fallback strings in `AppUser.java` and `AuthService.java` for new user creation.
4. **User Management UI (`users.page.ts`)**:
   - Add the new menu IDs to `menuOptions` in `users.page.ts` for explicit admin toggle control.

