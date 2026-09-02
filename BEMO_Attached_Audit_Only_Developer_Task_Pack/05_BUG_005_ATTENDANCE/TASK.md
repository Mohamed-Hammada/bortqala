# BUG-005 — Manual attendance decision does not persist

Priority: **HIGH**

Report 4c2c55df-3a5f-4da0-b4db-92b210d4a63e for 2026-08-01 to 2026-08-15: resolving a row closes the modal but progress remains 22% (2/9), unresolved remains 7, and reload restores the unresolved row.

Acceptance:
- [ ] API completion is awaited.
- [ ] Saving state is visible.
- [ ] Progress becomes 3/9 after successful decision.
- [ ] Unresolved count decreases to 6.
- [ ] Decision survives reload.
- [ ] Audit entry exists.
- [ ] Failure cannot look like success.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.