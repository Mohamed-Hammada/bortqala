# TASK 01 — Global Shortcuts: Single Gate & Modal Suppression

Priority: P0/P1
Owner: Frontend
Status: ☑ Verified

## Objective
Make every application-level keyboard shortcut pass through one authoritative resolver/gate. No shortcut may execute behind a modal, command palette, help overlay, or text-entry control unless explicitly allowed.

## Current risk
The repository status claims SHORTCUT-001 complete, but this task must be accepted only after runtime verification of every shortcut path. Earlier code had a direct Ctrl/Cmd+K path outside the modal guard; the latest work claims this was unified.

## Required shortcuts
- Ctrl/Cmd+K — command palette
- `/` — quick navigation/search
- `?` — shortcut help
- `G → X` — configured navigation chords
- Esc — close the highest-priority active overlay
- configured single-key/chord shortcuts

## Acceptance criteria
1. Normal page: all enabled/available shortcuts work.
2. Text input/textarea/select/contenteditable: global shortcuts are suppressed unless explicitly documented.
3. Modal open: `/`, `?`, Ctrl/Cmd+K and G-chords do not operate behind the modal.
4. Modal-specific Esc is handled by the modal first.
5. Opening a modal clears an active G-chord.
6. Window blur clears an active G-chord.
7. Chord timeout is deterministic and documented.
8. Disabled/unavailable shortcuts never execute.
9. Permission-restricted destinations never execute.
10. Shortcut help matches actual runtime behavior.
11. There is no second/rogue keydown path in AppShell or another global component.
12. Add automated regression tests for the matrix above.

## Evidence required
- Test names/results
- Screen recording or screenshots for modal suppression
- List of files/functions implementing the final gate
- FE test output
