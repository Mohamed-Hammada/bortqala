# Approval, Segregation of Duties and Audit Integration

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Existing code to reuse

- `[EXISTS] be/src/main/java/com/bemo/hr/approval/ApprovalApi.java`
- `[EXISTS] be/src/main/java/com/bemo/hr/approval/ApprovalController.java`
- `[EXISTS] fe/src/app/features/approvals/`
- `[EXISTS] fe/src/app/features/audit-logs/`

The existing approval engine already supports document type, multiple steps, user/role approvers, amount bands, minimum approvals, self-approval policy, versioned workflow snapshot, delegation, reassignment, deadlines/escalation, and history. **Do not add separate `purchase_approval`, `payroll_approval`, etc. tables.**

## Integration contract

Every critical document must expose a stable `documentType` and `documentId` to the approval engine. Suggested types:

```text
LABOR_REQUEST
CONTRACTOR_SETTLEMENT
WORKER_ADVANCE
PURCHASE_REQUISITION
PURCHASE_ORDER
SUPPLIER_INVOICE_OVERRIDE
PAYMENT_BATCH
SALES_DISCOUNT_OVERRIDE
CUSTOMER_CREDIT_LIMIT_CHANGE
INVENTORY_ADJUSTMENT
INVENTORY_REVALUATION
BOM_REVISION_RELEASE
PRODUCTION_VARIANCE_OVERRIDE
PAYROLL_RUN
MANUAL_JOURNAL
BUDGET_REVISION
BUDGET_TRANSFER
FISCAL_PERIOD_REOPEN
```

## Service sequence for submit

```text
validate document DRAFT
→ calculate approval basis amount/dimensions
→ freeze approval snapshot fields needed for routing
→ create approval instance using existing engine
→ set document SUBMITTED/PENDING_APPROVAL
→ audit SUBMIT
```

Approval callback/poll integration:

```text
approval completed
→ re-load source document and expected version
→ verify approval instance belongs to source + workflow version
→ set source APPROVED
→ audit approval link
```

Do not let frontend call a hidden `/approve-source` shortcut without validating the approval instance.

## Segregation of Duties

Add a configurable conflict layer only if no equivalent exists. Suggested conceptual rule:

| Rule | Example |
|---|---|
| requester != final approver when self approval disabled | purchase requisition |
| master creator != first payment approver | supplier banking |
| preparer != approver/executor above threshold | payroll/payment batch |
| counter != large variance approver | stock count |
| journal creator != sensitive-account poster | manual journal |

`[NEW]` suggested configuration only after IDE verification:

```text
sod_rule(id, source_action, conflicting_action, scope, amount_threshold, active, effective_from, effective_to)
```

## Audit requirements

For every transition write structured audit context:

```text
module
sourceDocumentType
sourceDocumentId
sourceDocumentNumber
oldStatus
newStatus
operationId
approvalInstanceId
actorId
reason
beforeVersion
afterVersion
relatedJournalId / movementId / paymentBatchId when relevant
```

Avoid serializing secrets/bank full details/passwords into audit payloads.

## Junior implementation order

1. Read `ApprovalApi` and `ApprovalController` and locate service/repository/entity classes used behind them.
2. Create constants/enums for document types only if current engine does not already have a registry.
3. Integrate `LABOR_REQUEST` as first non-finance pilot.
4. Add approval status + instance reference to the source aggregate where required; avoid copying approval history.
5. Add backend authorization to submit/cancel/execute.
6. Add module UI actions that route the user to existing approvals or show filtered approval status.
7. Add SoD checks at service transition boundary, not only UI.
8. Add tests for self-approval disabled, delegation, amount-band routing, stale source version, cancelled source, approval after source changed.
