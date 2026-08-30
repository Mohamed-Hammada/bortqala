# WP-15 — Medical CLINIC MVP Slice (first sellable medical scope)
**Priority:** 🟢 · **Owner:** Squad — BE dev H + FE dev E · **Depends on:** WP-10 optional · **Effort:** ~3 weeks
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §15. Hospital scope (§14) stays OUT until this ships.

## Business goal
Sellable to a single-doctor clinic in 3 weeks: register patients → queue → consult with e-prescription → charge visit → month-end doctor commission statement.

## Backend steps
1. `patients` table (V-number coordinator): MRN sequence per tenant (copy employee-code-sequence pattern), national_id 14-digit with pure parser extracting birthdate/gender (Egyptian format — unit-test the parser exhaustively), phones, gender, birth_date, allergies_text, notes; CRUD + duplicate search by phone/national_id.
2. `clinic_visits`: patient FK, doctor_employee_id, visit_date, status WAITING|IN_ROOM|DONE|CANCELLED, token = per-day sequence, chief_complaint, diagnosis_icd nullable, fee_charged, insurance_covered default 0.
3. Queue endpoints: waiting list ordered by token; `POST /{id}/call` (WAITING→IN_ROOM), `/complete`, `/cancel`.
4. Prescriptions v1: structured lines on visit (drug_name free text, dose, frequency, duration) + printable payload.
5. Billing: visit completion → treasury cashbox receipt (V290 pattern) OR sales invoice line if insurer involved (co-pay split: covered vs patient share fields already on visit).
6. Commission v1: single % per doctor (config or doctor field) → monthly statement endpoint (report only; payroll posting later via WP-12).
7. Gate everything behind entitlement flag `medical.enabled` or vertical==MEDICAL.

## Frontend steps
1. Features `features/clinic/patients`, `/queue`, `/visit-detail`; TV-friendly fullscreen queue route (`?tv=1` hides chrome, big "now serving" card).
2. Rx print view using print CSS with clinic header; Arabic-first labels both locales.
3. Full A.4 menu registration for `patients` + `clinicQueue`; reuse provisioned groups Clinic Administrator / Medical Receptionist from TenantSetupService.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Registering two patients with same phone warns with duplicate dialog offering to open existing chart.
- [x] **AC-2** Egyptian national ID parser extracts correct birthdate/gender on ≥10 fixture IDs incl. century digit edge cases; malformed ID rejected translated.
- [x] **AC-3** Walk-in flow <30s: new patient → queued → call → complete with fee → receipt printed, all without page reload confusion.
- [x] **AC-4** Queue board reflects state changes across two browsers (polling ≤5s); token order never duplicates within a day.
- [x] **AC-5** Rx prints Arabic correctly with clinic header and zero raw i18n keys visible.
- [x] **AC-6** Non-medical tenant sees NO clinic menus (flag gate proven by test toggling entitlement).
- [x] **AC-7** Commission statement matches manual calc on a 20-visit fixture month at configured %.
