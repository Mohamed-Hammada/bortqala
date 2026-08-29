# WP-24 — Lab & Imaging Orders (external-first LIS/RIS-lite)
**Priority:** 🟢 · **Owner:** Full-stack F · **Depends on:** WP-15 · **Effort:** ~5 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.7–14.8

## Business goal
Clinics send labs/imaging OUT. Need: order tests, track collection/result, attach report PDFs, alert doctor on critical values. Analyzer interfaces (internal labs) deferred.

## Backend steps
1. Tables: `lab_tests_catalog` (code, name_ar/en via translations, sample_type, normal_range_text nullable, price) · `lab_orders` (visit/patient FK, test FK, status ORDERED|COLLECTED|SENT_OUT|RESULTED|VALIDATED|CANCELLED, ordered_by, resulted_at, result_value_text, result_flag NORMAL|LOW|HIGH|CRITICAL, external_lab_party_id nullable, attachment trio).
2. Endpoints `/api/v1/clinic/lab-orders`: order (multi-test), collect (timestamp evidence), result entry (technologist), validate (doctor double-check → VALIDATED terminal; only VALIDATED visible in patient chart), cancel while ORDERED.
3. Critical flag triggers business notification to ordering doctor via NotificationCenterService (`LAB_CRITICAL_VALUE`) + red badge until acknowledged.
4. Imaging orders ride the same tables with `kind=IMAGING` and free-text report instead of value/flag.
5. Codes: `LAB_*` family (~7).

## Frontend steps
1. Order panel inside visit: pick multiple tests (search by name/code), external-lab selector; worklist page per status with Collect/Send-out/Enter-result actions; validation queue for doctors.
2. Chart documents tab lists validated results as downloadable attachments.
3. Keys `clinic.lab*` (~16).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Status machine enforced (validate only after result; cancel only ORDERED) — parameterized matrix green.
- [x] AC-2 CRITICAL result fires notification to correct doctor; badge persists until that doctor acknowledges (ack endpoint idempotent).
- [x] AC-3 Unvalidated results invisible in patient chart but visible in worklist (permission+status filter test).
- [x] AC-4 Result attachments enforce size/type rules like REM-005; download preserves original filename.
- [x] AC-5 External lab appears on order printout; aging list "sent out >3 days" works off sent_out timestamp.
