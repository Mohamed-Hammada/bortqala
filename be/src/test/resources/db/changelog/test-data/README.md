# Test-only Liquibase data

All Liquibase fixtures used only by automated tests belong in this folder and must be included only by `test.changelog-master.yaml`.

Never reference this folder from the production `db.changelog-master.yaml`.
