# Manufacturing & Quality — Actual Consumption, Partial Output and WIP

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

Refactor the existing production service from “start = consume full planned BOM” and “complete = single FG receipt” into a controlled production execution model with BOM snapshots, reservations, multiple actual issues/returns, partial receipts, QC disposition, WIP and variance.

## Existing code — preserve and extend

- `[EXISTS] ManufacturingApi.java`, `ManufacturingController.java`, `ManufacturingService.java`.
- Current BOM has revision/effective dates/yield/waste; production order has target/output/scrap and actual material/unit cost; readiness check exists; start issues raw material; completion receives finished goods; cancel reverses issue; quality inspection records exist.
- `[EXISTS] OperationsService` usage inside manufacturing: keep inventory movement ownership there.
- `[EXISTS] fe/src/app/features/manufacturing/`.

## Target end-to-end cycle

`Demand → Production Plan/Order → BOM Snapshot → Material Check/Reservation → Release → Actual Material Issue/Return → Routing Operations → In-Process QC → Partial FG Receipt → Final QC → WIP/Variance Posting → Close`

## Data model changes

### BOM snapshot
On production order release copy BOM revision header + component qty/UOM/waste/substitution constraints into immutable order snapshot tables/JSON according to current persistence convention.

### `[NEW] ProductionMaterialReservation/Issue/Return`
Multiple issue transactions referencing production order and component snapshot; actual qty/cost from operations movement result.

### `[NEW] ProductionReceipt`
Multiple receipts: good/rejected/rework/scrap, lot/batch, shift/timestamp. Closing remaining target requires explicit close action/reason.

### `[NEW] WorkCenter/Routing/RoutingOperation` P2
setup/run time, labor rate, machine/overhead rate, capacity.

### Quality
Evolve generic inspection into plan + characteristic + result + disposition. Sources: purchase receipt, production in-process, finished output, customer return. Disposition `ACCEPT / CONDITIONAL_ACCEPT / QUARANTINE / REWORK / SCRAP / RETURN_TO_VENDOR`.

### WIP
Track actual material/labor/overhead accumulated and finished-goods relieved. Store variance at close.

## API/command changes

Preserve existing start/complete endpoints as compatibility adapters initially:

```text
POST /.../production-orders/{id}/release
POST /.../production-orders/{id}/reserve-material
POST /.../production-orders/{id}/material-issues
POST /.../production-orders/{id}/material-returns
POST /.../production-orders/{id}/operations/{opId}/report
POST /.../production-orders/{id}/receipts
POST /.../production-orders/{id}/close
POST /.../quality/inspections/{id}/disposition
```

Legacy `start` may call release+reservation and optionally an initial issue policy; it must no longer force full planned issue once new flow is enabled. Legacy `complete` can create final receipt+close only when no partial/rework obligations remain.

## Backend implementation sequence

1. Write characterization tests for current `ManufacturingService.startProductionOrder()` and `completeProductionOrder()` before changing them.
2. Add immutable BOM snapshot on release/start.
3. Extract current raw material issue loop into `ProductionMaterialIssueService` that calls existing operations service.
4. Change start from issue-all to reservation/readiness; use feature/config migration if backward compatibility required.
5. Add multiple material issues and returns; actual consumption becomes sum of posted movements.
6. Add partial production receipt service using existing operations FG movement logic.
7. Update completion to explicitly close remaining quantity; no silent planned-cost assumption.
8. Add routing/work-center actuals; post labor/overhead absorption to WIP.
9. Add QC disposition and stock-status integration.
10. At close calculate planned-vs-actual material/output/labor/overhead variance and route posting through posting profiles.
11. Add cancellation/reversal handling for every posted issue/receipt; do not rely on one original start movement anymore.

## Frontend implementation

Manufacturing workbench should show:
- readiness/reservation shortages;
- BOM revision snapshot;
- planned vs reserved vs issued vs returned per material;
- routing operation status/time/cost;
- partial good/reject/rework/scrap receipts;
- QC status/quarantine;
- WIP and variance explanation;
- journal/movement links.

Action buttons come from backend allowed-action/status response.

## Cross-module integration

Operations for reservations/movements/location/lot; Quality for disposition; Posting profiles for WIP entries; Procurement/transfer resolve material shortages; Approval for BOM release/large variance; Close blocks open production/WIP discrepancies according to policy.

## Required automated and manual tests

BOM edited after order release does not alter snapshot; material shortage; substitute component; multiple partial issues; unused return; excess issue reason; partial FG receipts; quarantine portion; rework; cancel after some movements; duplicate issue operation; WIP journal sequence; variance tolerance approval; closed order immutable.



## Junior developer — exact execution order

1. Characterize current start/complete tests.
2. Add BOM snapshot.
3. Extract actual issue/return.
4. Add reservation and change start semantics.
5. Add partial receipt.
6. Add explicit close/variance.
7. Add QC disposition.
8. Add routing/labor/overhead.
9. Add WIP accounting.
10. Run Scenario E.
