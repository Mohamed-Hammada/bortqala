# Database / Liquibase Rules

Use for schema, data, migration, or query changes.

1. Inspect the current Liquibase changelog sequence before adding a migration.
2. Follow existing naming/versioning conventions.
3. Keep migrations deterministic and safe on a fresh database.
4. Check unique constraints, foreign keys, indexes, nullability, and tenant/app scoping.
5. Consider PostgreSQL behavior separately from H2; H2 green is not sufficient for PostgreSQL-specific/concurrent logic.
6. Add or update tests for migration-sensitive behavior.
7. Do not place secrets or environment-specific credentials in migrations.
