# BUG-018 — Missing accessibility names

Priority: **MEDIUM**

Many form controls lack explicit accessible naming; icon-only controls such as X/edit/expand are ambiguous.

Acceptance:
- [ ] Every input has an explicit label.
- [ ] Icon-only controls have accessible names.
- [ ] Dialog title/description are exposed.
- [ ] Visible focus is preserved.
- [ ] Keyboard-only and screen-reader smoke tests pass.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.