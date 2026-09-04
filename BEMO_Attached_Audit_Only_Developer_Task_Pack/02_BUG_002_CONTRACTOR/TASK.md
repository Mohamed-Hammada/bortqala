# BUG-002 — Contractor creation fails silently

Priority: **CRITICAL**

Valid contractor creation leaves the form open with no actionable error and an opaque Angular console error.

Acceptance:
- [x] Valid contractor is created.
- [x] Invalid data produces field/form validation.
- [x] Server failure preserves entered data.
- [x] Retry works.
- [x] API error is converted to localized user feedback.
- [x] Worker flow can consume the contractor.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.