# Shared Document Lifecycle, Idempotency and Concurrency

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Mandatory engineering rules

1. **Search before create.** Before adding a controller/service/entity/repository/page, search the branch for the same responsibility and extend it if possible.
2. **Backend owns transitions.** The UI may hide/disable actions, but the backend must reject illegal state transitions.
3. **Idempotency for commands.** Financial/stock/workforce commands must accept an `operationId` and return the previous result for an already-completed identical operation instead of executing twice.
4. **Optimistic concurrency.** Use the branch's existing version pattern (`expectedVersion` where already exposed, or entity versioning convention) on material transitions.
5. **No destructive edits after posting/closing.** Use reversal, correction, retro adjustment, or a new version.
6. **Use existing approval engine.** Do not create module-specific approval tables unless the existing engine genuinely cannot represent the requirement.
7. **Use existing inventory and journal services.** Procurement/manufacturing/sales must not maintain their own shadow stock balances or GL balances.
8. **Audit actor + time + reason + source.** Reversal/override/reopen/manual-adjustment reasons are mandatory.
9. **Snapshot effective terms.** Historical transactions must retain the rule/rate/BOM/price/posting-profile version actually used.
10. **Transactions are atomic.** A business transition and its inventory/accounting/audit side effects must either succeed together or fail together where the current architecture supports a DB transaction.
11. **Permissions are server-side.** Route/menu guards improve UX but do not replace backend authorization.
12. **No AI dependency.** Recommendations must be formulas/rules with visible inputs and deterministic output.


## Business goal

Stop each module from inventing unrelated meanings for `active`, `status`, `approved`, and `posted`. Critical documents need explicit server-enforced transitions with repeat-safe commands and concurrency control.

## Target common vocabulary

Use only the subset relevant to each document:

`DRAFT → SUBMITTED → PENDING_APPROVAL → APPROVED → EXECUTING → PARTIALLY_COMPLETED → COMPLETED → POSTED → CLOSED`

Terminal/exception states: `REJECTED`, `CANCELLED`, `REVERSED`.

Do **not** force every state onto every entity. The standard is semantic consistency, not one giant enum.

## Existing anchors

- `[EXISTS] WorkforceApi.java` already demonstrates `operationId` / `expectedVersion` on settlement-related commands.
- `[EXISTS] AccountingApi.java` and finance flows have operation/version controls that should be reused as a convention.
- `[EXISTS] FiscalPeriodController.java` already uses version-aware status changes.
- `[EXISTS] ApprovalApi.java` provides document-centric workflow state/history.

## Proposed shared code

Use the project's existing package/convention if an equivalent exists.

- `[NEW] be/src/main/java/com/bemo/hr/shared/transition/TransitionCommand.java`
- `[NEW] .../TransitionResult.java`
- `[NEW] .../DocumentTransitionService.java`
- `[NEW] .../OperationExecution.java` or equivalent idempotency record
- `[NEW] .../IllegalDocumentTransitionException.java`

Minimum command fields:

```java
record TransitionCommand(
    UUID operationId,
    long expectedVersion,
    String reason
) {}
```

Add actor/document IDs at service boundary or in specialized commands; do not trust actor IDs supplied by the browser when authentication context already identifies the user.

## Idempotency storage

For commands that cause money/stock/posting effects, persist an operation record in the same transaction if architecture permits:

| Field | Rule |
|---|---|
| operation_id | unique, non-null |
| operation_type | e.g. `POST_PAYROLL`, `ISSUE_STOCK`, `PAY_SUPPLIER` |
| aggregate_type / aggregate_id | source document |
| request_hash | optional; detects same operationId with different payload |
| status | `STARTED / SUCCEEDED / FAILED` if durable retry is required |
| result_reference | generated payment/journal/movement ID |
| created_by / created_at | audit |

On duplicate `operationId`:
1. if original succeeded and payload hash matches, return prior result;
2. if operationId is reused with a different payload, return `409 OPERATION_ID_REUSED`;
3. never execute financial/stock side effects again.

## Concurrency pattern

Every material transition must:

```text
load aggregate
→ compare expectedVersion with current version
→ validate current state
→ validate approval/period/permissions
→ execute side effects
→ advance state/version
→ audit
→ commit
```

Return HTTP 409 with a machine-readable error when version changed.

## State transition matrix pattern

Each module README specifies its matrix. Implement either:
- explicit service methods with `requireState(...)`, or
- a small transition registry/service.

Avoid a generic dynamic workflow engine for every domain action in phase 1; business invariants should remain explicit and testable.

## Reversal/correction contract

Posted/closed source document:
- no DELETE;
- no amount/quantity/status direct UPDATE;
- reversal command must create linked reversal side effects;
- correction must reference original document and reason;
- period rules decide whether reversal posts in original open period or current period.

Recommended common metadata:

```text
reversed_document_id
reversal_document_id
reversal_reason
reversed_by
reversed_at
```

## Junior implementation steps

1. Search for all existing `operationId`, `expectedVersion`, `@Version`, and finance/workforce transition helpers.
2. Write a short internal note identifying the existing convention; reuse rather than replacing it.
3. Add shared error codes first (`VERSION_CONFLICT`, `ILLEGAL_STATE`, `DUPLICATE_OPERATION`, `OPERATION_ID_REUSED`).
4. Add/standardize operation execution storage only for commands that cannot safely rely on an already-existing idempotency store.
5. Integrate one pilot: contractor settlement posting or journal posting.
6. Add tests: same operation twice, different payload same ID, stale version, wrong state, unauthorized actor.
7. Integrate remaining financial/stock commands module by module.
8. Keep old APIs working through adapters during migration; do not perform a flag-day rewrite.
