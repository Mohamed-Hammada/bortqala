# WP-39 — Automation Pack: Recurring Documents + Dunning Ladder + Jobs Health
**Priority:** 🟡 · **Owner:** Backend dev D · **Depends on:** WP-32 (scheduler pattern) · **Effort:** ~7 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17F

## Business goal
Three automations that remove monthly grunt work: (1) recurring documents (standing PO, rent invoice, template journal) auto-draft each period; (2) AR dunning ladder escalating reminders at 15/30/60 days; (3) background-jobs health page with retry for failed exports/pushes/schedules.

## Backend steps
1. `recurring_templates` (kind PO|INVOICE|JOURNAL, payload snapshot JSONB validated against target creator, cadence MONTHLY|WEEKLY|CUSTOM_DAYS, next_run_at, active, last_created_ref): runner creates DRAFT documents only (never auto-posts money docs), stamps source template id, advances next_run_at.
2. Dunning: `dunning_rules` (days_overdue 15/30/60 default seed, channel per WP-31 availability, template_key) → daily job over AR aging buckets sends/queues reminders per party with dedupe key (party+bucket+period); escalation = next rule; opt-out flag per party.
3. Jobs health: unify existing async evidence tables behind `GET /api/v1/admin/jobs?status=FAILED` (exports, push sends, schedules, ocr, imports); `POST /admin/jobs/{id}/retry` re-dispatches via original handler registry; poison counter.
4. Codes ~10 across families.

## Frontend steps
1. Settings→Automation tab: templates CRUD with "preview next run" (renders the would-be draft JSON summary); dunning rules editor + per-party opt-out in parties page; Admin jobs page (filter status/type/date, error detail drawer, retry button with spinner).
2. Keys ~24.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Monthly rent template creates exactly one DRAFT invoice on its day; double-run same period dedupes via next_run_at advance (clock test).
- [x] AC-2 Drafts never post ledger until human confirms (invariant test on all three kinds).
- [x] AC-3 Party at 35 days overdue receives 30-bucket reminder once; at 61 gets 60-template; opted-out party logged skipped.
- [x] AC-4 Failed export visible in jobs page with original error text; retry succeeds and flips status; retry of non-retryable kind blocked server-side.
- [x] AC-5 All three runners are tenant-scoped — cross-tenant leakage test on every loop.

## Deliverables Summary
- **Database Schema**: `automation_rules`, `recurring_templates`, `dunning_rules` (Liquibase `v375`, `v376`).
- **Backend Architecture**: Package `com.bemo.hr.automation` (`AutomationService`, `AutomationController`, recurring document draft generator, dunning reminder engine, jobs health and retry registry).
- **Frontend Architecture**: `AutomationPage` (`fe/src/app/features/automation/automation.page.ts`), automation rules builder, template preview drawer, and background jobs retry dashboard.

