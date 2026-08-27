# WP-49 — Employee Self-Service Mobile Surfaces (in WP-14 app + web)
**Priority:** 🟢 · **Owner:** Mobile dev G + FE · **Depends on:** WP-14 (wrapper), WP-09 (push) · **Effort:** ~7 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §13.5 ⭐

## Business goal
The ⭐ competitor killer-feature: employees themselves use the app — selfie attendance (already in WP-14), view payslip, request leave, request loan/advance — no HR middleman for routine asks. Web ESS-lite mirrors it for desktop-only users.

## Backend steps
1. ESS role surface: dedicated `ROLE_EMPLOYEE_SELF` authority set (read own payslips, create own leave/advance requests, read own profile) — enforced server-side; existing employee↔user link reused.
2. Payslip endpoint: `GET /api/v1/ess/payslips?year=` returning own frozen payroll snapshots (post-PAID only) with explanation breakdown reuse (`SalaryPaymentExplanation`).
3. Leave: employees may CREATE leave requests into existing leave module with status PENDING_HR; loan/advance request → creates advance plan REQUEST awaiting HR approval before schedule activates (new status layer on workforce advances).
4. All endpoints ownership-scoped by construction (userId→employee resolve; tests prove cross-access impossible).

## Frontend steps
1. App tabs (Capacitor bottom nav): Home (today punch state + quick selfie button), Payslips (list → detail with calculation explanation rows), Requests (leave form w/ balance display, advance form w/ amount+installments count), Profile.
2. Push deep-links: "payslip ready" opens payslip; "leave approved" opens request status.
3. Web mirror: minimal `features/self-service/` pages reusing same endpoints behind same role.
4. Keys ~28 `ess.*`.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Employee token CANNOT list other employees' payslips even by guessing ids (404-scope test matrix).
- [ ] AC-2 Payslip detail figures equal frozen snapshot exactly (no client recomputation — diff test vs admin view).
- [ ] AC-3 Leave request appears in HR approvals inbox; approve/deny reflects in employee app status ≤ poll interval; balance shown pre-submit prevents over-request client-side AND server-side.
- [ ] AC-4 Advance request creates PLAN_REQUESTED (not active); HR approval activates schedule and payroll starts deducting next run (ties WP-07 resolver).
- [ ] AC-5 Offline selfie punch from WP-14 flows unchanged with ESS role; push taps open correct screen cold-started.
