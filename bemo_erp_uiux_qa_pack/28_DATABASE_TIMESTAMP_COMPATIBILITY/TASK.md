# TASK 28 — Database Timestamp Compatibility

Priority: P0/P1
Status: ☑ Verified

## Context
Commit c64962d changes multiple created_at/updated_at columns from timestamp to bigint and changes epoch generation to EXTRACT(EPOCH FROM now()) * 1000.

## Required
1. Identify every changed column.
2. Identify every Java entity field mapped to each column.
3. Confirm Java type is compatible with BIGINT.
4. Confirm JDBC/JPA converters are compatible.
5. Confirm DTO/request/response serialization remains correct.
6. Confirm sorting/filtering by timestamps still works.
7. Confirm existing data migration is safe.
8. Confirm H2/test schema matches PostgreSQL behavior.
9. Confirm Liquibase rollback is valid or explicitly documented as irreversible.
10. Test create/update/read for every affected domain.
11. Test API JSON timestamp format expected by frontend.
12. Verify no code still assumes SQL TIMESTAMP for these columns.

## Acceptance
No merge based solely on a successful Liquibase startup; entity/API integration tests are required.
