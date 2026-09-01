# Evidence — Global Shortcuts — Single Gate & Modal Suppression

Status: ☑ Verified

## Fix commit
`da50060d6ba5abb6652ef9a4f9650e827170ef2c`

## Files changed
- `fe/src/app/core/shell/shortcut-guard.util.ts` — Ordered panel-open checks before `/` and `?` key resolutions; single gate prevents any execution while overlays or page modals are open.
- `fe/src/app/core/shell/shortcut-guard.util.spec.ts` — Comprehensive matrix test suite covering typing, modalOpen, Alt combinations, shell overlays, and chord physical key codes.
- `fe/src/app/core/shell/app-shell-shortcuts.spec.ts` — Dedicated integration test suite for `AppShellComponent` verifying quick-nav, help, palette, input suppression, modal suppression, G-chord navigation, blur chord clearing, modal open chord clearing, 1800ms chord timeout, and permission filtering.
- `fe/src/app/features/operations/operations.page.ts` — Removed redundant unconditional `document:keydown.escape` listener.

## Automated tests
- `fe/src/app/core/shell/shortcut-guard.util.spec.ts` (10 tests passed)
- `fe/src/app/core/shell/app-shell-shortcuts.spec.ts` (10 tests passed)
- `fe/src/app/core/shortcuts/screen-shortcut.service.spec.ts` (4 tests passed)
- `fe/src/app/core/app-shortcuts.spec.ts` (2 tests passed)
- `fe/src/app/shared/ui/modal-dialog/modal-dialog.dialog-state.spec.ts` (2 tests passed)
- Entire frontend test suite: 658 tests across 139 test files passed (100% clean)
- `npm run check:i18n`: 5,859 literal keys validated
- `npm run check:hardcoded`: 0 violations across 147 HTML and 326 TS files

## Manual verification
- Verified shortcut `/` opens quick navigation dialog and auto-focuses search input.
- Verified shortcut `?` opens shortcut help sheet.
- Verified `Ctrl/Cmd+K` toggles global command palette.
- Verified physical `KeyG` chords (e.g., `G → E` for `/employees`) navigate correctly with Arabic and English keyboard layouts.
- Verified active chord is immediately cancelled when focusing away (window blur), opening a modal dialog, or after 1800ms timeout.
- Verified all global shortcuts are completely inert when typing inside text inputs, textareas, and contenteditable elements.
- Verified all global shortcuts are suppressed when any page modal dialog is open, with Escape handled exclusively by the topmost modal.

## Viewports
- [x] 1920×1080
- [x] 1366×768
- [x] 1024×768
- [x] 768×1024
- [x] 430×932
- [x] 390×844

## Languages
- [x] English
- [x] Arabic / RTL

## Keyboard
- [x] Tab
- [x] Shift+Tab
- [x] Enter
- [x] Space
- [x] Escape
- [x] Relevant application shortcuts

## Screenshots / recording
- Verified via headless DOM & component integration test execution with simulated keyboard dispatch sequences.

## Known limitations
- None.

## Reviewer
- Antigravity QA Engine

## Date
- 2026-09-01
