# Database Migration and Backfill Plan

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Critical instruction

The exact migration technology/directory was **not assumed** by this package. Before creating a migration:

1. inspect `be/src/main/resources`, build files and existing schema/migration scripts;
2. identify whether the repository currently uses Flyway, Liquibase, Hibernate DDL, SQL scripts, or another mechanism;
3. add migrations in that existing mechanism;
4. **do not introduce Flyway/Liquibase just because this README mentions migrations.**

## Safe migration pattern

For high-volume/current tables use expand → backfill → dual-read/write if needed → validate → constrain → remove legacy.

Example adding `warehouse_id`:

```text
Migration A: create warehouse master + nullable warehouse_id
Deploy code that writes warehouse_id for new records
Backfill existing rows to DEFAULT warehouse
Validate no nulls
Migration B: add FK/index/not-null if required
Only later remove old location free-text after compatibility period
```

## Schema groups and recommended order

### P0 shared
1. operation/idempotency storage if not already present;
2. source-document linkage/journal metadata additions;
3. posting-profile tables/config if not equivalent already;
4. close-check metadata only if checklist needs persisted runs; live checks can avoid extra tables;
5. normalized dimension FKs where required.

### Workforce
request dimensions → dispatch/assignment → attendance correction/lock metadata → settlement dispute → invoice match snapshot.

### Attendance/payroll
attendance rule versions → payroll input snapshot → payroll calendar/period → payroll run/employee → component definitions/results → retro adjustments.

### Procurement
requisition/lines → RFQ/suppliers/lines → quotes → award → PO source linkage → receiving stock status → landed cost.

### Sales
order line → pricing → reservation reference → delivery/lines → return/credit-note links.

### Inventory
warehouse/bin/status fields → reservation → transfer → count → lot/serial constraints.

### Manufacturing
BOM/order snapshot → material issue/return references → partial receipts → routing/work center/operation → quality plan/result → WIP/variance metadata.

### Treasury/budget
payment batch/items → budget versions/revisions/transfers.

## Index/constraint checklist

Every new transactional table should consider:
- FK to source header;
- unique business number per tenant/app scope if current system uses numbering;
- unique `operation_id` for command execution where stored on transaction;
- index status + date for queues/workbenches;
- index party/source document references;
- unique active reservation/serial constraints where business requires;
- check constraints for non-negative quantities where valid;
- currency precision/scale consistent with current money type;
- `created_at/by`, `updated_at/by`, version field following current base entity convention.

Do not invent different decimal precision per module.

## Backfill rules

Historical records must be preserved. When new source linkage cannot be known, use explicit `LEGACY`/null semantics rather than fabricating a source. When new status replaces a boolean, map deterministically and document the mapping.

## Rollback

For each migration PR include:
- rollback feasibility;
- whether backfilled data would be lost;
- whether application code can run against both pre/post schema during deployment;
- data validation SQL/query counts before and after.
