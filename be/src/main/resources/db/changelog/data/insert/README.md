# Data insert migrations

Put production reference-data and translation inserts here. Use tenant predicates and idempotent preconditions where applicable.

Test fixtures do not belong here; use `src/test/resources/db/changelog/test-data/`.

The platform role catalog and all UI translations are mandatory release data. They must remain reachable from the production release master. User accounts are created idempotently after migration by `BootstrapAdminInitializer`, using environment-provided credentials outside development.
