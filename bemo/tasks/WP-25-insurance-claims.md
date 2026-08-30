# WP-25 — Insurance Payers, Pre-Authorization & Claims (Egypt)
**Priority:** 🟢 · **Owner:** Backend dev C + FE · **Depends on:** WP-15 · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.11–14.12

## Business goal
Egyptian reality: patients covered by HIO (هئة التأمين الصحي) or private networks (MedNet, AXA, MetLife, Bupa…). Need payer catalog with plan rules, pre-auth approval codes before procedures, and monthly claim batches with rejection handling.

## Backend steps
1. Tables: `insurance_payers` (name, type HIO|PRIVATE|CORPORATE, contact, active) · `insurance_plans` (payer FK, coverage_percent 0..100, copay_flat, annual_limit nullable, exclusions_text) · `patient_insurance` (patient FK, plan FK, member_no, policy_valid_to; multiple rows allowed, one primary flag).
2. Visit billing integration: on visit close with insurance — split `fee_charged` into insurer_share / patient_share using plan coverage+copay (backend math); patient pays share at cashbox, insurer balance tracked per payer.
3. `pre_authorizations` (payer FK, patient FK, procedure_text, approval_code, requested_amount, approved_amount nullable, status REQUESTED|APPROVED|REJECTED|EXPIRED, decided_on): visits can reference one; billing without required pre-auth warns (`PRE_AUTH_MISSING`) configurable to block via property.
4. Claims: `claim_batches` (payer FK, period, status DRAFT|SUBMITTED|PARTIALLY_PAID|PAID|REJECTED) gathering billed insurer shares as claim lines from visits; submission marks lines SUBMITTED; payment entry records realized vs claimed; per-line rejection reason + resubmit action creating corrective line.
5. Codes: `INS_*` family (~10).

## Frontend steps
1. Admin: payers/plans CRUD pages. Registration: attach insurance card to patient (member no scan-ready field). Visit close: insurance summary box showing computed split before finalizing.
2. Claims workspace: build batch by payer+period → review totals → mark submitted → record payment/rejections grid with resubmit flow.
3. Keys `clinic.insurance*` (~22).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Split math: 1000 fee, 80% coverage, 50 copay → insurer 750 / patient 250 exactly (property-based test over coverage 0..100 × copay set).
- [x] AC-2 Expired policy at visit time blocks insurance path (falls back to cash with warning) — validity date fixture.
- [x] AC-3 Pre-auth REQUIRED mode blocks finalizing visit without APPROVED code; warn mode allows with badge.
- [x] AC-4 Claim batch totals = Σ insurer shares of member visits in period; partial rejection leaves remainder reconcilable (paid + rejected + outstanding = claimed).
- [x] AC-5 Resubmitted corrective line links original rejection reason (audit trail visible in UI).
