# Evidence — BUG-019 — Contractor failure lacks recovery guidance

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/src/app/features/workforce/pages/contractors/contractors.component.html` — on `saveError()`, a `role="alert"` block renders with:
  - a strong title `workforce.ui.contractors.saveErrorTitle` ("Could not save the contractor."/تعذر حفظ المقاول.),
  - the localized reason from `saveError()` (validation, permission, network, or server cause via `apiErrorMessage`),
  - an actionable help line `workforce.ui.contractors.saveErrorHelp` ("Review the data or connection and try again. The dialog stays open until the server confirms the save."). The Save button remains enabled for retry.
- `fe/src/app/features/workforce/pages/contractors/contractors.component.ts` — `saveContractor` keeps the modal open and preserves entered data on failure (never resets `form` or `isModalOpen` on error); failure feedback only ever comes from the `saveError` alert, never a success toast.
- DB translations: `workforce.ui.contractors.saveErrorTitle` / `saveErrorHelp` (ar-EG + en-US) in `data/insert/files/translations.csv`.

Automated tests:
- `contractors.component.spec.ts` 4/4 (incl. offline-network → localized connection error; verify-before-close; blocks when missing required fields).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:i18n` 5,884 keys PASS; `check:hardcoded` PASS.

Manual verification:
- A failed contractor save (validation, network, permission, or server) leaves the dialog open with the entered data intact and shows the title + cause + recovery hint; Save remains available to retry.
- Success only fires a success toast after the server confirms and the reloaded list contains the new record.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [x] Tab  [x] Enter  [ ] Shift+Tab  [ ] Space  [ ] Escape (`role="alert"` announced; Save/retry reachable)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
