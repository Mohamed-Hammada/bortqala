# Evidence — BUG-021 — User-role configuration is cognitively overloaded

Status: [x] Verified
Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/src/app/features/users/users.page.ts` — Implemented 5-stage wizard (`Identity` → `Category` → `Role` → `Permission Review` → `Confirmation`), `goNext()`, `goBack()`, `rolesSelected()`, `confirmationSummary()` computed, and `advancedOpen` collapsible signal.
- `fe/src/app/features/users/users.page.html` — Wizard step header navigation, back/next/submit action buttons, collapsible advanced permission search.
- `fe/src/app/features/users/users.page.scss` — Wizard stepper styles, progress bar, active/completed stage indicators.
- `be/src/main/resources/db/changelog/data/update/20260902_v451_user_wizard_translations.yaml` — Bilingual translations for steps and confirmation review.
- `translations.csv` & `i18n.service.ts` — Default fallback dictionary keys.

Automated tests:
- `fe/src/app/features/users/users.page.spec.ts` — Added `describe('BUG-021 staged user wizard')` with 5 unit tests covering stepper rendering, forward/back data preservation, role requirement gating, confirmation summary rendering, and collapsible advanced search.
- Frontend test suite: 692 tests / 143 files pass cleanly (`npm test`).

Manual verification:
- Clicked "＋ مستخدم جديد", navigated through all 5 stages. Step 1 (Identity) captures username/password/name, Step 2 captures category, Step 3 enforces at least one role selection, Step 4 provides collapsible permission matrix, Step 5 renders clean summary before creation.

Arabic / RTL: [x] Tested
English / LTR: [x] Tested
Responsive: [x] Desktop  [x] Tablet  [x] Mobile
Keyboard/accessibility: [x] Tab  [x] Shift+Tab  [x] Enter  [x] Space  [x] Escape

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- QA automation & resolution agent

Date:
- 2026-09-04
