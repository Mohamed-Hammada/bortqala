# WP-27 — Dental & Specialty Charting Add-ons
**Priority:** 🟢 · **Owner:** Full-stack F · **Depends on:** WP-21 · **Effort:** ~4 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §15 dental row

## Business goal
Dental clinics need an odontogram (32-tooth chart) with per-tooth treatment plans; other specialties need template-driven exam forms. v1 ships dental + generic template engine.

## Backend steps
1. `dental_records` (patient FK, tooth_number FDI 11..48, condition enum CARIES|FILLED|CROWN|MISSING|IMPLANT|ROOT_CANAL|EXTRACTED_PLANNED…, surface enum nullable, noted_on, visit FK nullable) — history = all rows (evidence), latest per tooth = current state view.
2. `treatment_plans` header + `treatment_plan_items` (tooth_number, procedure_code/text, price, status PLANNED|DONE|CANCELLED) — completing an item can auto-create visit charge line.
3. Generic templates: `exam_templates` (specialty, sections JSON schema: fields with type select/text/number/checkbox) rendered dynamically; answers stored as `exam_answers` (visit FK, template FK, payload JSONB).
4. Codes: `DENTAL_TOOTH_INVALID`, `TEMPLATE_*`.

## Frontend steps
1. Odontogram component: SVG adult chart, quadrant colors by condition legend (color+symbol+text), click tooth → condition dialog + history popover; child dentition toggle.
2. Treatment plan tab: planned items checklist → mark done creates charge preview → confirm posts to visit billing.
3. Dynamic form renderer for exam templates (typed reactive form built from schema — no innerHTML).
4. Keys `clinic.dental*` / `clinic.exam*` (~18).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 FDI numbering validated 11–48 (invalid rejected translated); chart state renders latest-per-tooth correctly after mixed history.
- [ ] AC-2 Marking plan item DONE posts exactly one charge line; double-mark idempotent.
- [ ] AC-3 Template admin can build a 5-field form that renders and saves answers round-trip intact (JSON diff test).
- [ ] AC-4 Odontogram keyboard-accessible (tab per tooth, Enter opens dialog) and screen-reader labels announce tooth number + state.
