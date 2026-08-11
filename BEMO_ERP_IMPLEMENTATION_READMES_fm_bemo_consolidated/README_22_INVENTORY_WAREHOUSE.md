# Inventory / Warehouse — Location, Status, Reservation and Count

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Business outcome

Evolve existing item/UOM/movement/costing/valuation capabilities into a warehouse-control model without replacing current inventory costing or posting code.

## Existing code — preserve and extend

- `[EXISTS] OperationsApi.java`, `OperationsController.java` and operations package valuation/costing logic.
- Existing behavior includes item/category, UOM conversions, stock movements, negative-balance reporting, adjustments, valuation policy, movement costing, revaluation, account configuration and optional GL posting.
- `[EXISTS] fe/src/app/features/operations/`.
- This module must remain the owner of stock movement/balance semantics used by procurement/sales/manufacturing.

## Target end-to-end cycle

`Inbound/Production/Transfer Source → Stock Status/Location → Reservation → Pick/Issue/Transfer/Count → Valuation → GL Reconciliation → Period Close`

## Data model changes

### Warehouse hierarchy
`Warehouse → Zone → Bin`. Start with warehouse+bin if zones add no immediate value.

### Balance key
Target logical key: `item + warehouse + bin + lot/serial + stockStatus`.
Do not immediately replace existing calculated balances if movements are source-of-truth; introduce a balance projection/cache only with reconciliation tests.

### Status
`AVAILABLE`, `RESERVED`, `QUALITY_HOLD`, `QUARANTINE`, `DAMAGED`, `REJECTED`, `IN_TRANSIT`.

### Reservation
Source type/id/line, item, location preference, qty, reservedAt, expiry, status. Unique/idempotent operation reference.

### Transfer
Header + lines; `REQUESTED → APPROVED(optional) → PICKED → DISPATCHED → IN_TRANSIT → RECEIVED → CLOSED`. Use paired movements.

### Stock count
Plan/scope, blind count lines, recount, variance, approval, adjustment reference. Counter cannot see book quantity during blind entry.

### Lot/serial
Item policy none/lot/serial, expiry requirement, shelf-life at receipt, FIFO/FEFO picking policy.

## API/command changes

Extend existing operations API rather than creating warehouse API elsewhere:

```text
POST /.../operations/reservations
POST /.../operations/reservations/{id}/release
POST /.../operations/transfers
POST /.../operations/transfers/{id}/dispatch
POST /.../operations/transfers/{id}/receive
POST /.../operations/counts
POST /.../operations/counts/{id}/submit-count
POST /.../operations/counts/{id}/approve-adjustment
GET  /.../operations/availability?itemId=&warehouseId=
```

Availability response should return onHand, reserved, blocked/quarantine, available and shortages by location/status.

## Backend implementation sequence

1. Trace current stock movement entity/service and valuation service.
2. Add warehouse/location/status references to movement model in backward-compatible stages.
3. Backfill existing stock to a default warehouse/bin/status before making fields mandatory.
4. Implement availability query from movements/balances.
5. Add reservation service with row/transaction locking strategy appropriate to existing persistence to prevent double promise.
6. Integrate procurement quality statuses.
7. Add transfer paired movements; dispatch changes source to in-transit, receive creates destination stock.
8. Add blind count and approved adjustment using existing adjustment movement path.
9. Add lot/serial validation at receive/pick.
10. Keep valuation method/version logic intact; ensure new location/status dimensions do not double-value reserved/in-transit stock.
11. Add inventory valuation ↔ GL reconciliation provider.

## Frontend implementation

Extend operations feature with:
- warehouse/bin master;
- availability view by item/warehouse/status;
- reservations queue;
- transfer workbench;
- cycle-count workbench with blind-entry mode;
- lot/serial/expiry filters.

Never calculate authoritative availability only in Angular; display backend response.

## Cross-module integration

Procurement creates inbound pending inspection/available; Sales reserves/issues; Manufacturing reserves/issues/receives; Quality changes status; Finance receives valuation posting; period close checks negative stock, unfinished transfers/counts, valuation/recon.

## Required automated and manual tests

Reservation race between two orders; release/expiry; blocked stock excluded; transfer partial dispatch/receive; lost in-transit prevention; blind count hides book qty; variance threshold second count; duplicate adjustment operation; lot/serial uniqueness; expiry/FEFO; negative stock policy; valuation unchanged by AVAILABLE→RESERVED status move unless accounting policy says otherwise.



## Junior developer — exact execution order

1. Map stock movement and valuation code.
2. Add default warehouse migration.
3. Add location/status dimensions.
4. Add availability service.
5. Add reservation.
6. Integrate Sales confirm.
7. Add transfers.
8. Add stock count.
9. Add lot/serial controls.
10. Add reconciliation/close checks.
