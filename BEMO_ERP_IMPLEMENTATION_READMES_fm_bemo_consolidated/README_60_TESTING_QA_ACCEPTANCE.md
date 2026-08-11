# Testing and QA Acceptance Plan

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Test pyramid for each slice

1. **Pure rule unit tests** — formulas, state guards, tolerances, effective-date resolution.
2. **Service integration tests** — persistence + transaction + idempotency + concurrency + side effects.
3. **Controller/API tests** — validation, authorization, error contract.
4. **Frontend component/page tests** — actions/states/forms/error handling.
5. **End-to-end business scenarios** — source through posting/payment/reconciliation.

## Mandatory cross-cutting cases

Every financially/operationally critical command should test:

- happy path;
- missing/invalid input;
- wrong state;
- stale expectedVersion;
- same operationId replay;
- same operationId with changed payload;
- unauthorized user;
- approval missing/rejected;
- source changed after approval where snapshot requires protection;
- closed-period attempt;
- downstream service failure rollback/consistent partial state;
- reversal/correction;
- audit record created.

## Scenario A — Contractor labor

1. Create request for 20 category A workers with budget/site.
2. Approve.
3. Dispatch 22 workers.
4. Accept 20, reject 2 with reasons.
5. Record period attendance.
6. Resolve exceptions and lock.
7. Calculate settlement v1.
8. Correct prior attendance; verify recalculation flag.
9. Calculate v2; verify traceable delta.
10. Approve settlement.
11. Match contractor invoice within tolerance.
12. Post contractor payable journal.
13. Pay via payment batch.
14. Match bank statement.
15. Close checklist PASS.

## Scenario B — Purchase-to-pay

Requisition → budget commitment → approval → 3-supplier RFQ → award → existing PO → partial GRN/rejection → quality release → invoice → over-tolerance block → approved resolution → AP/GL post → payment proposal → treasury → bank match → AP control reconciliation.

## Scenario C — Sales-to-cash

Multi-line SO → credit → reservation → partial delivery + COGS → invoice delivered qty → partial receipt/allocation → aging balance → final receipt → two bank matches → AR control reconciliation.

## Scenario D — Payroll

Attendance blockers → snapshot → run calculation components → deterministic variance review → approval → journal → payment batch → bank reconciliation → payroll payable zero → close → later correction produces retro, not rewrite.

## Scenario E — Manufacturing

Released BOM snapshot → shortage blocks → stock/PO resolves → reservation → partial actual issues → partial FG receipt → QC quarantine part → remaining production → WIP actuals → variance post → close immutable.

## Reconciliation assertions

For test fixtures assert exact equations, not only “status success”:

```text
AP subledger - AP control = 0
AR subledger - AR control = 0
Inventory valuation - Inventory GL = 0
Payroll payable open items - Payroll payable GL = 0
Payment batch executed total - matched bank total - approved differences = 0
```

## Performance/concurrency tests

At minimum test reservation and payment/posting race conditions. Two concurrent calls must not reserve the same stock beyond availability or pay/post the same source twice.

## QA evidence per PR

Developer includes:
- automated test names/results;
- API request/response example for new command;
- screenshots for new UI state if relevant;
- migration validation counts;
- one negative business rule demonstration;
- one duplicate-operation demonstration for critical commands.
