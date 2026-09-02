# BUG-016 — Report shortcut action is ambiguous

Priority: **MEDIUM**

On /reports a period shortcut immediately creates a report although Preview and Create actions also exist.

Acceptance:
- [ ] Shortcut either fills the form only OR clearly means Create.
- [ ] If it creates, confirmation is explicit.
- [ ] Preview never silently creates.
- [ ] Keyboard and mouse behavior match.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.