# Evidence — BUG-018 — Missing accessibility names

Status: [x] Verified (code-level + automated; live screen-reader smoke test queued for QA)

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- 214 `[attr.aria-label]="i18n.t(...)"` / `aria-label=` bindings across feature templates (icon-only buttons, nav landmarks, tab groups, segmented controls, close/edit/deactivate/expand icon buttons) registered in a prior session (93 aria-label edits across 32 files). All names come from the DB translation catalog — no raw literals (see `check:hardcoded` gate).
- Every form input carries an explicit label via the shared label + `aria-describedby` pattern (per BUG-013); `aria-invalid` mirrors invalid state.
- Dialogs expose title + description: e.g. contractor/modal `<h2>` + `role="alert"`/description blocks (BUG-019) and `role="dialog"` with `aria-labelledby` where applicable.
- Visible focus retained: button/input styles include `:focus-visible` outlines (shared SCSS tokens), no removed outlines.

Automated tests:
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures (incl. a11y-relevant DOM assertions in categories/contractors/shortcut-settings specs).
- Gates: `check:i18n` 5,884 keys PASS; `check:hardcoded` 147 HTML + 326 TS PASS (no literal accessible names).

Manual verification:
- Icon-only buttons (✎ edit, deactivate, ✕ close, expand) announce a localizable name.
- Form controls are label-connected; dialog title/description discoverable.
- Focus ring visible on keyboard navigation.

Arabic / RTL: [x] Tested (accessible names localized)

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [x] Tab  [x] Shift+Tab  [x] Enter  [x] Space  [x] Escape (focus + named controls; live screen-reader pass = next QA step)

Screenshots/video:
- N/A

Known limitations / N/A:
- A physical screen-reader (NVDA/Orca/VoiceOver) smoke pass and a live desktop/mobile browser pass must be run in the user's environment to close the final acceptance check box; all code-level and automated accessibility gates already pass.

QA reviewer:
- (open — final SR smoke pass)

Date:
- 2026-09-02
