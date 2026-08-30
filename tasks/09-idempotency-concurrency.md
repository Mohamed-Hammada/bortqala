# TASK 09 — Idempotency & Concurrency
## Scope
Payments, receipts, invoices, inventory movements, payroll payments, journal posting, ETA submissions and bank transactions.
## Required behavior
Same request retry → same result. Concurrent duplicates → one business effect. Same key with different payload → reject.
## Acceptance Criteria
- [ ] Idempotency key persisted.
- [ ] Payload consistency checked.
- [ ] Duplicate requests cannot duplicate effects.
- [ ] Concurrent requests are safe.
- [ ] Keys are tenant-isolated.
- [ ] PostgreSQL concurrency tests exist.
