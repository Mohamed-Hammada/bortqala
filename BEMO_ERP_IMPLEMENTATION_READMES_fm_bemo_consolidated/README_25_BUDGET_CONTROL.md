# Budget-to-Control — Versions, Commitments, Revisions and Transfers

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

Keep the current planned/committed/actual/available and PO encumbrance behavior, but make budgets versioned and auditable so approved values are never overwritten silently and all cycles use one commitment policy.

## Existing code — preserve and extend

- `[EXISTS] BudgetApi.java`, `BudgetController.java`.
- Existing budget tracks planned amount, department, period/year, blocking mode, committed/actual/available/utilization.
- Existing PO encumbrance tracks committed/liquidated/released amounts. Reuse it; do not add a second commitment ledger.

## Target end-to-end cycle

`Budget Preparation → Review/Approval → Active Version → Requisition/PO Check → Commitment → Actualization → Revision/Transfer → Forecast → Period/Year Close`

## Data model changes

### Budget key
Phase 1: fiscal year/period + department + account/category. Extend with branch/cost center/project/currency as business needs mature.

### Versioning
Store original approved version + revisions/transfers; derive current budget. Suggested `BudgetVersion`, `BudgetRevision`, `BudgetTransfer` only after inspecting current entities.

Revision status: `DRAFT → SUBMITTED → APPROVED → APPLIED`.

### Commitment policy config
`RESERVE_AT_REQUISITION_APPROVAL` or `RESERVE_AT_PO_APPROVAL/ISSUE`. Release on PO cancel, closed remainder, or conversion to actual. Exactly one policy active per scope/effective date.

## API/command changes

Add endpoints consistent with current budget API:

```text
POST /.../budgets/{id}/revisions
POST /.../revisions/{id}/submit
POST /.../budgets/transfers
POST /.../transfers/{id}/submit
GET  /.../availability?dimension...&amount=
POST /.../commitments/reserve
POST /.../commitments/liquidate
POST /.../commitments/release
```

Normally source modules call a budget service internally rather than invoking HTTP against the same application.

## Backend implementation sequence

1. Trace current budget and encumbrance services.
2. Add immutable version/history tables or version rows around current budget without breaking reads.
3. Implement a single `BudgetAvailabilityService` used by requisition, PO and labor request.
4. Configure commitment point; prevent double reserve when requisition becomes PO.
5. Add revision/transfer workflows using shared approval.
6. Liquidate commitment to actual from posted invoice/expense according to accounting policy.
7. Add release for cancellation/unfulfilled closure.
8. Add budget close report/reconciliation of committed vs source open documents.

## Frontend implementation

Budget screen/workbench:
- Original / revisions / transfers / current;
- planned, committed, actual, available;
- blocking reason with source drill-down;
- revision/transfer approval state;
- deterministic forecast can be simple actual+open commitments, clearly labelled formula not AI.

## Cross-module integration

Procurement and workforce consume availability; Finance actualizes; Approval controls budget/revision/transfer; Period close validates dangling commitments and source consistency.

## Required automated and manual tests

Exact budget boundary; blocking vs warning; duplicate reserve operation; requisition→PO no double commitment; PO partial invoice liquidates proportionally; PO cancel releases; revision rejected; transfer insufficient source budget; period transition; stale version.



## Junior developer — exact execution order

1. Map current budget/encumbrance code.
2. Add history/version support.
3. Build availability service.
4. Integrate procurement requisition.
5. Integrate labor request.
6. Add revision/transfer approval.
7. Add commitment lifecycle/reconciliation.
8. Add UI and close checks.
