# TASK 02 — Shortcut Settings

Priority: P1
Status: ☑ Verified

## Scope
Shortcut Settings page: add/edit/delete/enable/disable/capture/reset/save.

## Required behavior
1. Editing any shortcut makes the page dirty.
2. Dirty indicator is visible.
3. Save persists the complete draft atomically.
4. Successful save clears dirty state.
5. Discard restores the last persisted server/profile state.
6. Discard shows a success confirmation.
7. Navigating away while dirty prompts the user.
8. Reset means reset-to-defaults; it must not be confused with Discard.
9. Duplicate key/chord detection blocks save and identifies the conflicting shortcut.
10. Invalid capture is rejected with a clear message.
11. Letters/digits allowed by capture must exactly match the allowed-key model.
12. Loading/error states do not lose the local draft.
13. Keyboard-only operation works for every control.

## Acceptance evidence
- Add → edit → save
- Edit → discard
- Edit → navigate away
- Duplicate shortcut
- Invalid key
- Reset → save
