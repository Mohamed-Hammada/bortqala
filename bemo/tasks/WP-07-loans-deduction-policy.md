# WP-07 — Loans Deduction Policy Switcher + Global Override
**Priority:** 🟠 · **Owner:** Backend dev D (HR/payroll) · **Depends on:** — · **Effort:** ~3 days
**Liquibase:** V-number from coordinator (~V346).
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Admin decides PER CATEGORY how advance repayment runs: automatic inside payroll vs manual button; monthly vs every-15-days. Today cadence is implied by document type with no switch, and no global policy individuals inherit.

## Current state
`WorkforceAdvanceInstallment` + `WorkforceAdvanceService.calculateEmployeePayrollDeduction(...)` auto-deduct during payroll (`PayrollService` ~line 260); contractor settlements apply adjustments per 15-day cycle; per-employee plans exist.

## Backend steps
1. Table `advance_deduction_policies` (id, app_id, scope GLOBAL|CATEGORY, category_id NULL, mode AUTO_IN_PAYROLL|MANUAL_BUTTON, cadence MONTHLY|MID_MONTH_SPLIT, created_by, version). Constraint: single GLOBAL row; ≤1 per category (partial unique index or service check).
2. Resolver in `WorkforceAdvanceService`: `resolvePolicy(appId, categoryId)` → category ?? global ?? defaults(AUTO_IN_PAYROLL, MONTHLY) → zero behavior change for existing tenants.
3. Payroll call site consults resolver; MANUAL mode → skip auto deduction entirely (advanceBalance untouched, deduction 0) and note reason in calculation evidence JSON.
4. Manual endpoint `POST /api/v1/advances/apply-deduction {employeeId, periodId}` computing due installment and posting it (reuse settlement posting); reject when resolved policy is AUTO (`ADVANCE_MANUAL_NOT_DUE`) or when nothing due this period; idempotent per (employee, period).
5. Codes: `ADVANCE_POLICY_EXISTS`, `ADVANCE_MANUAL_NOT_DUE`, `ADVANCE_NOTHING_DUE`, `ADVANCE_POLICY_INVALID`.

## Frontend steps
1. Settings → Advances policy section: cards with scope selector, mode radio, cadence radio; persists ONLY on explicit Save All (settings-page pattern); cancel reverts.
2. Employees page bulk action "Apply deduction now" rendered only when employee's resolved policy = MANUAL (light endpoint `GET /api/v1/advances/resolved-policy?categoryId=`); confirm dialog shows affected count.
3. Keys ≈12: `settings.advancePolicy*`, `employees.applyDeduction*` + CSV.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Category MANUAL overrides global AUTO ✅ (delivered V350 — see PROJECT_MAP WP-07 entry) for that category's members; others unaffected (resolver unit tests cover precedence chain incl. missing rows).
- [x] **AC-2** Switching Security ✅ (delivered V350 — see PROJECT_MAP WP-07 entry)→MANUAL then running payroll: their advance deduction lines are 0 and evidence JSON states policy reason; switching back restores auto next run.
- [x] **AC-3** Bulk button hidden for AUTO categories ✅ (delivered V350 — see PROJECT_MAP WP-07 entry), visible+working for MANUAL; applying twice same period is idempotent (one deduction).
- [x] **AC-4** Manual apply when policy=AUTO ✅ (delivered V350 — see PROJECT_MAP WP-07 entry) → translated `ADVANCE_MANUAL_NOT_DUE`; nothing-due case → `ADVANCE_NOTHING_DUE`.
- [x] **AC-5** Settings persist only via Save All ✅ (delivered V350 — see PROJECT_MAP WP-07 entry); Cancel reverts unsaved edits (existing settings regression suite still green).
