# WP-11 — Employee Expense Claims (Odoo parity)
**Priority:** 🟡 · **Owner:** Full-stack dev F · **Depends on:** — · **Effort:** ~4 days
**Liquibase:** V-number from coordinator.
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Employees currently report costs on paper. Need: submit claim with receipt photo → manager approves → finance reimburses through ledger, all audited.

## Backend steps
1. Tables: `expense_claims` (id, app_id, employee_id FK, category MEAL|TRANSPORT|LODGING|SUPPLIES|OTHER, spent_on LocalDate, amount >0, currency default EGP, description, receipt_attachment_name/content_type/size nullable — copy REM-005 attachment column trio incl. ≤5MB/images-PDF rules, status DRAFT|SUBMITTED|APPROVED|REJECTED|REIMBURSED, approver_id NULL, decided_at NULL, decision_note NULL, reimbursement_reference NULL, created_at/updated_at, version).
2. Endpoints `/api/v1/expenses`: create/edit own while DRAFT; `POST /{id}/submit`; approve/reject (HR roles; self-approval blocked — reuse SoD precedent); `POST /{id}/reimburse` (finance) → partner-ledger credit to employee OR explicit "include next payroll" flag v1=ledger credit (matches advances precedent); employee sees ONLY own claims (ownership check in repo query by user link).
3. Policy limits per category via properties `hr.expenses.limit.<CATEGORY>` (default unlimited): exceeding → warning flag on claim requiring HR note (not hard block v1).
4. Codes: `EXPENSE_NOT_FOUND/NOT_OWN/INVALID_STATE/SELF_APPROVAL/LIMIT_EXCEEDED_NEEDS_NOTE/AMOUNT_INVALID`.

## Frontend steps
1. New feature `features/expenses/`: my-claims table + new-claim dialog with photo upload (reuse operations attachment util), approvals inbox filter for HR, reimburse action for finance with reference input. Full menu protocol (`expenses` under workforce or finance workspace — pick workforce).
2. Status badges color+text; rejected shows decision note inline.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Employee A cannot list/open/approve Employee B's claim (integration test proves 404/403, not UI hiding).
- [x] **AC-2** State machine matrix green: only DRAFT editable; submit→approve→reimburse happy path; reject requires note; reimburse only from APPROVED.
- [x] **AC-3** Self-approval blocked with translated error (SoD parity with payroll F-003).
- [x] **AC-4** Claim above configured limit renders warning badge and forces HR decision note; limit absent → no warning. ※Implemented (V408): per-category `hr.expenses.limit.<CATEGORY>` properties, over-limit flag in responses, `.badge-limit` badge + forced-note approve modal, `EXPENSE_LIMIT_*` codes en/ar (2026-08-29).
- [x] **AC-5** Receipt upload enforces ≤5MB + allowed types client AND server side; remove works before submit.
- [x] **AC-6** Reimbursement creates exactly one partner-ledger credit visible in party statement; replaying endpoint idempotent.
