# BUG-021 — User-role configuration is cognitively overloaded

Priority: **UX MEDIUM**

New-user dialog combines credentials, category, role matrix, guided capabilities and permission search in one long modal.

Acceptance:
- [x] Flow is staged: Identity → Category → Role → Permission Review → Confirmation.
- [x] Back/Next preserves data.
- [x] Advanced permission search is collapsible.
- [x] Keyboard navigation works throughout.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.