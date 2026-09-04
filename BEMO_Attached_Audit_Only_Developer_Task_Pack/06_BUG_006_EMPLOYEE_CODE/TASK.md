# BUG-006 — Employee code duplicated on create

Priority: **HIGH**

Entered QA-EMP-0807 was stored as QA-EMP-0807-QA-EMP-0807.

Acceptance:
- [x] User-entered code round-trips unchanged.
- [x] Edit form shows the same code.
- [x] Reports/selectors show the same code.
- [x] Uniqueness behavior is defined.
- [x] Regression test added.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.