# WP-13 — Fix Shortcut × Dialog Integration (8 verified bugs)
**Priority:** 🟡 · **Owner:** Frontend dev E (needs care — shell architecture) · **Depends on:** — · **Effort:** ~3 days
**Read first:** `_GLOBAL-RULES.md` + defect list in `missing-todo.md` §21

## Business goal
Reported: "some dialogs don't work with shortcuts". Root cause verified: the shell's global key handler is unaware of page dialogs, so `/`, `?`, and `G→key` fire behind open dialogs when focus sits on a button.

## Files
`fe/src/app/core/shell/app-shell.component.ts` (handler :280-382) · `fe/src/app/shared/ui/modal-dialog/modal-dialog.component.ts` (static stack :52) · raw overlays in `app-shell.component.html` (:292 logout, :357 action-center, :401 quick-nav, :462 help) · `shortcut-settings.component.ts`.

## Implementation order (one PR)
1. New `core/shell/dialog-state.service.ts`: signals `modalDepth`, `topmost()`; refactor ModalDialogComponent static stack into it (keep static delegating for compat). Raw overlays call `acquireOverlay()/releaseOverlay()` in open/close paths.
2. BUG-1/2: early-return in `onGlobalShortcut` when any dialog/overlay open EXCEPT Escape passthrough (modals self-handle). Removes shortcuts-behind-modals AND input-vs-button inconsistency.
3. BUG-4: `clearChord()` on overlay acquire + `@HostListener('window:blur')`.
4. BUG-5: Escape precedence = only `dialogState.topmost()` consumer acts; shell checks `topmostIsShellPanel()`.
5. BUG-6: `submitFormOnEnter` honors `data-no-autosubmit` on closest form.
6. BUG-7: duplicate shortcut destination in settings → translated toast `shortcuts.duplicateBlocked`.
7. BUG-3/8: extract `trapFocus/getFocusableElements` to `shared/ui/focus-trap.util.ts`; apply to raw overlays; add `role="dialog"`+`aria-modal="true"`+`aria-labelledby` to all five surfaces.
8. Required regression specs: navigation NOT fired while modal open; chord cleared on blur; quick-nav suppressed over modal; Enter skips marked forms; Escape closes topmost only; focus returns to trigger after close.

## Acceptance Criteria (QA sign-off)
- [ ] **AC-1** With confirm-dialog open and focus on its button: pressing `/`, `?`, or `G→1` does NOTHING (no navigation, no panels) — manual test script included in PR.
- [ ] **AC-2** Same dialog with focus inside a text input behaves IDENTICALLY (no difference between input-focus vs button-focus).
- [ ] **AC-3** Press G then alt-tab away and back within 1.8s: chord cancelled; next G starts fresh.
- [ ] **AC-4** Open quick-nav THEN a page modal above it: Escape closes modal first, second Escape closes quick-nav (order deterministic across browsers).
- [ ] **AC-5** Enter inside a filter form marked `data-no-autosubmit` never submits outer form.
- [ ] **AC-6** Tab cycles within each of the 5 shell overlays and every app-modal-dialog; focus visibly trapped; Shift+Tab wraps backwards; close restores focus to opening trigger.
- [ ] **AC-7** Duplicate destination pick shows translated toast; all existing shortcut-settings specs stay green plus new regression suite above.
