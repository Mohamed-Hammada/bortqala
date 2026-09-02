# Evidence — BUG-013 — Required-field validation lacks visible explanation

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/src/app/features/workforce/pages/categories/categories.component.html` — worker-category code/name inputs carry `[attr.aria-invalid]="submitted() && !form.x?.trim()"`, `aria-describedby="<field>-error"`, and a localized inline `<small id="…-error" class="field-error">` rendered only while the required field is blank; the whole validated earlier in prior sessions (localized `codeError`/`nameError`/`requiredError` keys).
- `fe/src/app/features/workforce/pages/categories/categories.component.ts` — `submitted` signal gates the inline errors; `saveCategory` sets `submitted=true` on submit, clears it on cancel/close; errors disappear automatically once the field is corrected (inline render is bound to `submitted() && !field.trim()`).
- DB translations: `workforce.ui.categories.codeError` / `nameError` / `requiredError` / `rateHoursError` (ar-EG + en-US) present in `data/insert/files/translations.csv`.

Automated tests:
- Frontend: `ng test --watch=false` 687 tests / 143 files, 0 failures (categories component covered).
- Gates: `check:i18n` 5,884 keys PASS; `check:hardcoded` 147 HTML + 326 TS PASS; `check:translation-catalog.py` 17,888 rows PASS.

Manual verification:
- Submitting a blank worker-category name focuses the field, sets `aria-invalid="true"`, and shows the localized inline `Name is required` / `الاسم مطلوب` error.
- Correcting the field clears the inline error (bound to the trimmed value), satisfying "error clears after correction".
- Form is short (single modal) — no separate long-form validation summary needed (N/A criterion).

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [x] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (aria-invalid + aria-describedby verified on the field)

Screenshots/video:
- N/A

Known limitations / N/A:
- Long-form "validation summary" acceptance is N/A for this short single-entity modal.

QA reviewer:
- (open)

Date:
- 2026-09-02
