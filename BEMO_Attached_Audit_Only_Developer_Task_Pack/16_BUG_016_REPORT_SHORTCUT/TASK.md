# BUG-016 — Report shortcut action is ambiguous

Priority: **MEDIUM**

On /reports a period shortcut immediately creates a report although Preview and Create actions also exist.

Acceptance:
- [x] Shortcut either fills the form only OR clearly means Create.
- [x] If it creates, confirmation is explicit.
- [x] Preview never silently creates.
- [x] Keyboard and mouse behavior match.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.