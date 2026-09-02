# Evidence — BUG-016 — Report shortcut action is ambiguous

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; fix shipped in prior REM-006 session)

Files/components changed:
- `fe/src/app/features/reports/reports.page.ts` — renamed the shortcut handler to `applyPreset(period)` (previously `create(period)`). It only fills the period form (`periodStart`/`periodEnd` via `PeriodOption` and clears stale `previewResult`); it has **no create side effect** (documented at lines 91-92: "no create side effect. The user still has Preview and Create as explicit actions."). `create()` (line 118) remains the single explicit Create action. `previewCustom()` (line 126) only calls `store.preview()` and never creates.
- `fe/src/app/features/reports/reports.page.html` — the period-preset shortcut buttons call `applyPreset(period)` (line 152); the Preview button calls `previewCustom()` (line 68); Create uses the form submit path. Mouse (`(click)`) and keyboard (focused button `Enter`/`Space`) both route to the same methods.

Automated tests:
- `reports.page.spec.ts` "**fills the period form on preset click and does not create a report**" (line 69).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:hardcoded` PASS; `check:i18n` 5,884 keys PASS.

Manual verification:
- Clicking a period preset on /reports fills the from/to/pay-cycle controls and shows the Preview area; it does NOT generate a report.
- Preview shows the generated-record preview without persisting; Create is the only action that calls `store.create()`.
- Keyboard activation matches mouse behavior (same handlers).

Arabic / RTL: [x] Tested (Preview/Create labels localized via `reports.*` keys)

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [x] Tab  [x] Enter  [x] Space  [ ] Shift+Tab  [ ] Escape (buttons focusable; identical handlers)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
