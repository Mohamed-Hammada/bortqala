# TASK 10 — PostgreSQL Production Integrity
## Goal
H2 may be used for fast tests, but PostgreSQL must verify production-sensitive behavior.
## Verify
Transactions, locking, constraints, isolation, native queries, JSON behavior, pagination, tenant isolation and migrations.
## Acceptance Criteria
- [ ] Fast H2 tests remain where useful.
- [ ] Critical integration tests run on PostgreSQL.
- [ ] Concurrency tests run on PostgreSQL.
- [ ] Rollback tests run on PostgreSQL.
- [ ] Tenant isolation tests run on PostgreSQL.
- [ ] Fresh and upgrade migrations succeed.
