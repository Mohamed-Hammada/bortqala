# WP-21 — Medical EMR Depth & Patient Chart (extends WP-15)
**Priority:** 🟢 · **Owner:** BE dev H + FE dev E · **Depends on:** WP-15 shipped · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §14.4

## Business goal
Turn the clinic MVP visit record into a real patient chart: structured allergies, chronic problems, vitals, attachments and consent forms — so history is visible before prescribing.

## Backend steps
1. Tables: `patient_allergies` (patient FK, substance, severity MILD|MODERATE|SEVERE, reaction note) · `patient_conditions` (code ICD-10 nullable, label, chronic bool, noted_on) · `visit_vitals` (visit FK, systolic, diastolic, pulse, temp_c, spo2, weight_kg, height_cm — BMI computed backend-side) · `patient_documents` (REM-005 attachment trio + kind LAB|IMAGING|REPORT|CONSENT) · `consent_forms` (patient/visit FK, template_key, signed_by_name, signed_at).
2. Chart endpoint `GET /api/v1/clinic/patients/{id}/chart`: demographics + allergies (red-banner first) + chronic list + last N visits w/ vitals trend + documents — single aggregated payload.
3. Interaction guard hook: prescription create checks SEVERE allergies text-match vs drug_name (v1 keyword warn, not block; `DRUG_ALLERGY_WARNING` in response payload).
4. Codes: `CHART_*`, `VITALS_RANGE_INVALID`.

## Frontend steps
1. Patient detail page = chart layout: header banner (allergy red strip), tabs History / Vitals (CSS sparkline of BP/pulse per visit — no chart lib) / Documents / Consents.
2. Visit form gains vitals grid with auto-BMI display and allergy warning toast on Rx save.
3. Keys `clinic.chart*` (~18).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Chart loads in one request ≤300ms on 50-visit fixture; allergy banner always visible on every tab.
- [x] AC-2 Vitals save validates physiological ranges server-side (BP 60–260 etc.) with translated errors; BMI matches manual calc to 1 decimal.
- [x] AC-3 Prescribing to a SEVERE-allergy patient shows warning naming the substance; proceed requires confirm click (audited).
- [x] AC-4 Consent print includes patient name, template body, signature line + timestamp; stored as document row.
- [x] AC-5 Tenant isolation on all new tables proven by cross-app test.
