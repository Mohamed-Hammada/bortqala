# Evidence — BUG-002 — Contractor creation fails silently

Status: [x] Verified (resolved together with BUG-019 failure-recovery work)

Fix commit SHA: `________________` (fill after commit; shipped in a prior session + verified this session)

Files/components changed:
- `fe/src/app/features/workforce/pages/contractors/contractors.component.{ts,html}` — on invalid input, `saveContractor` guards on required fields (code/name/type) and blocks submission; on server failure the modal stays open with a `role="alert"` block (`saveErrorTitle` + localized cause + `saveErrorHelp` "Review the data or connection and try again…") and the Save button remains enabled for retry. API errors are converted to localized user feedback (never a raw console-only failure). Success fires only after the server confirms and the reloaded list contains the new record.
- `fe/src/app/features/workforce/pages/workers/workers.component.html` — worker flow REQUIRES a contractor (`contractorId` is a required select populated from `workforceService.contractors()`, lines 42-44, 110), so a valid contractor is immediately consumable in the worker flow.
- DB translations `workforce.ui.contractors.saveErrorTitle` / `saveErrorHelp` (ar-EG + en-US).

Automated tests:
- `contractors.component.spec.ts` 4/4 (valid create, offline-network localized error, verify-before-close, missing-required-fields block).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:hardcoded` PASS; `check:i18n` 5,884 keys PASS.

Manual verification:
- Valid contractor data → server-confirmed create → success toast + list reload (no silent failure).
- Invalid/missing data → field-level validation, submission blocked (no accidental silent failure).
- Server/network failure → modal stays open, entered data preserved, localized cause + recovery hint shown, Retry works, dialog only closes on confirmed save.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [x] Tab  [x] Enter  [ ] Shift+Tab  [ ] Space  [ ] Escape (`role="alert"` announced; Save/retry focusable)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
