# Release ordering

Release masters are the only place where new focused migrations are ordered across operation folders.

Example:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/schema/create/20260801_v68_example_table.yaml
  - include:
      file: db/changelog/data/insert/20260801_v69_example_reference_data.yaml
```

After a release is applied, rename `next.changelog-master.yaml` to a dated release master, create a new empty `next` master, and never reorder the applied release. `20260729_v1_v67.changelog-master.yaml` is the categorized baseline created with an explicitly approved development-database rebuild.
