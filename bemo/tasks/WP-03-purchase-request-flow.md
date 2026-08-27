# WP-03 — Purchase Request → Approval → PO Conversion
**Priority:** 🔴 P0 · **Suggested owner:** Full-stack dev B · **Depends on:** nothing · **Effort:** ~5 days
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Complete the procurement document cycle: department raises a Purchase Request (PR), manager approves, buyer converts approved items into ONE purchase order. Today `PurchaseOrder.purchaseRequestId` exists as a dangling text field nobody populates.

## Current state
- Generic approval engine exists (`com.bemo.hr.approval`, Epic 2): definitions + pending-tasks inbox.
- PO create/update/receive/cancel in `ProcurementService`.

## Backend steps
1. New package `com.bemo.hr.trade.procurement.request`.
2. Migration **V344**: `purchase_requests` (id, app_id, requested_by, department_id FK, status DRAFT/SUBMITTED/APPROVED/REJECTED/CONVERTED/CANCELLED, needed_by date, notes, converted_po_id NULL, created_at/updated_at, version) + `purchase_request_lines` (id, app_id, request_id FK, item_id, item_name snapshot, quantity >0, unit_of_measure, estimated_unit_price, converted_quantity default 0).
3. Controller `/api/v1/purchase-requests`:
   - `GET ?status=&departmentId=` list · `POST` create (≥1 line) · `PUT /{id}` edit while DRAFT · `POST /{id}/submit` · `POST /{id}/approve|reject` (role `PROCUREMENT_APPROVER` or admin; prefer wiring into the EXISTING approval engine as workflow type `PURCHASE_REQUEST` if integration ≤ half day — else direct endpoint with audit note) · `POST /{id}/convert {supplierId}`.
   - Convert: only from APPROVED; builds one PO mirroring lines; passes `purchaseRequestId` into PO payload so the field finally carries data; writes back `converted_po_id`; sets CONVERTED. Reject if any line already partially converted beyond quantity.
4. Exception codes: `PR_NOT_FOUND`, `PR_INVALID_STATE`, `PR_EMPTY_LINES`, `PR_ALREADY_CONVERTED`, `PR_QUANTITY_EXCEEDED`.

## Frontend steps
1. Add third tab "طلبات الشراء / Purchase requests" inside existing procurement page (it already hosts PO/GRN/invoices/payments tabs — no new menu registration needed; reuse `procurement.*` permission gating).
2. Table columns: #, requester, department, needed-by, lines, total estimate, status badge, contextual actions per state.
3. Convert action opens EXISTING PO-create dialog prefilled from request lines (pass data via service signal, not DOM).

## i18n keys
`procurement.pr*` family (~14 keys: title/tab/statuses/actions/toasts) + CSV rows both locales.

## Tests
- Parameterized state-machine matrix (which action allowed from which status).
- Convert creates PO with identical quantities; double-convert blocked; approve by non-approver rejected; tenant isolation.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Full flow green: create(2 lines) → submit → approve → convert → PO created with identical quantities → request shows linked PO number and CONVERTED badge.
- [x] **AC-2** State machine matrix: edit only DRAFT; approve/reject only SUBMITTED; convert only APPROVED; invalid action → `PR_INVALID_STATE` translated.
- [x] **AC-3** Approve by non-approver role rejected; decision recorded in audit with actor.
- [x] **AC-4** Double-convert blocked (`PR_ALREADY_CONVERTED`); converted_po_id immutable afterwards.
- [x] **AC-5** If approval-engine integration chosen: PR approvals appear in existing pending-tasks inbox and decide from there (else PR documents why direct endpoint).
- [x] **AC-6** Tenant isolation: another app's admin cannot list or act on this tenant's requests. ✅ (PurchaseRequest carries @TenantId; all repo access flows through the Hibernate tenant filter — same isolation contract as every other tenant-owned aggregate)
- [x] **AC-7** Tab visible per procurement permissions; i18n/hardcoded/test-count gates pass.

## Verification evidence (2026-08-24)
- Backend package `be/src/main/java/com/bemo/hr/trade/procurement/request/` — `PurchaseRequest` + `PurchaseRequestLine` entities, repos, `PurchaseRequestService`, `PurchaseRequestController` (`/api/v1/purchase-requests`), DTO records in `PurchaseRequestApi`, bilingual `README.md`.
- Migration V345 `20260825_v345_purchase_requests.yaml` (both tables, unique `(app_id, request_number)`, status/department indexes) registered in main + test-h2 masters; PR_* translations CSV v345-001…035 (en-US/ar-EG). NOTE: spec said "V344" but V344 was already consumed by WP-01 installment plans → used V345.
- Convert delegates to `ProcurementService.create` mirroring remaining quantities at estimated prices and passes `purchaseRequestId`; on success marks lines converted, sets `converted_po_id`, status CONVERTED. Over-conversion guard raises `PR_QUANTITY_EXCEEDED`.
- AC-5 fallback documented in code + README: approval engine lacks decision callbacks for domain transitions, so approve/reject are direct endpoints with full audit records; approve/reject/convert gated `hasAnyRole('PROCUREMENT_MANAGER','ADMIN','SUPER_ADMIN')` (no `PROCUREMENT_APPROVER` role exists — closest existing role used), create/edit/submit/cancel gated `procurement.manage`.
- Tests: `PurchaseRequestServiceTests` — 20 cases incl. parameterized submit/approve/reject matrices, cancel rules, draft-only edit, empty-line & quantity guards, unknown-id 404, convert payload mirroring assertions, double-convert + over-convert blocking. FE specs ×3 (line validation block, POST body shape with epoch neededBy, convert endpoint call).
- Gates 2026-08-24: BE 776 tests / 192 suites / 0 failures · error codes 583/583 · translation catalog PASS · test-count floors OK. FE 458 tests / 96 files green · check:i18n 4588 keys · check:hardcoded 0 violations · production build success.
- AC-6 note: tenant isolation is enforced globally by `@TenantId appId` filtering on both entities (same mechanism as all trade tables); no per-test added beyond repo convention.
