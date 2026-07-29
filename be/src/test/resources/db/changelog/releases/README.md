# Test release ordering

`test-h2.changelog-master.yaml` preserves production migration order while inserting H2-only compatibility changes at the exact dependency points required by the automated test database.

Test data remains separate under `test-data/`.
