# Workforce / Contractor Labor — Technical Implementation

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

Turn the existing strong workforce module into a fully traceable commercial cycle: the system must prove which approved labor need was served by which contractor and workers, what attendance was accepted, how the settlement was calculated/versioned, how disputes and invoice variance were resolved, what journal was posted, and what payment/bank match closed the liability.

## Existing code — preserve and extend

- `[EXISTS] be/src/main/java/com/bemo/hr/workforce/WorkforceApi.java` — DTO/command layer; already carries settlement calculation/version, invoice/payment and operation/version concepts.
- `[EXISTS] be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementController.java` — settlement transition anchor.
- `[EXISTS] be/src/main/java/com/bemo/hr/workforce/` — current entities/services/controllers; inspect before adding each proposed class.
- `[EXISTS] fe/src/app/features/workforce/workforce.routes.ts`.
- `[EXISTS] workforce pages`: `contractors/`, `workers/`, `labor-requests/`, `manual-attendance/`, `settlement-periods/`, `advances/`, `contractor-accounts/`.
- Preserve existing settlement calculation versioning, issues/warnings, advance policy snapshots, accounting posting/payment links and audit behavior.

## Target end-to-end cycle

`Labor Need → Labor Request → Approval → Contractor Dispatch → Worker Allocation → Site Acceptance → Daily Attendance → Attendance Review/Lock → Settlement Calculation → Exception/Dispute Resolution → Finance Approval → Contractor Invoice Match → GL Posting → Payment Batch → Bank Reconciliation → Close`

## Data model changes

### Extend labor request
Add fields/normalized references: requesting department, project, cost center, site/work location, start/end, shift/calendar, budget reference. If current request already has category-count lines, extend those lines with max daily rate and required skill/category instead of replacing them.

### `[NEW]` dispatch/allocation domain, only after IDE search
Suggested package under existing workforce module:
- `LaborDispatch` header: requestId, contractorId, dispatchDate, status, operation/version metadata.
- `LaborDispatchLine`: requestLineId/categoryId, proposed count.
- `WorkerAssignment`: workerId, requestLineId, contractorId, from/to, site/branch/project, agreedRateSnapshot, agreedHoursSnapshot, status.

Worker assignment status: `PROPOSED / ACCEPTED / REJECTED / REPLACED / COMPLETED`.

Constraints:
- one active overlapping assignment per worker/site policy;
- assignment contractor must match worker/dispatch policy;
- accepted count cannot exceed configured overfill policy without override;
- rate/hours snapshots immutable after first accepted attendance.

### Site acceptance
Persist acceptance outcome and reason. Do not encode only header counts. A replacement references the rejected/no-show assignment.

### Attendance lock/correction
Per worker/day state: `OPEN → ENTERED → VALIDATED → APPROVED → LOCKED`. After APPROVED/LOCKED, correction creates a correction/audit record and triggers settlement recalculation if included in an open settlement snapshot.

### Settlement dispute
Suggested entities: `SettlementDispute`, `SettlementDisputeItem/Evidence` if attachments need separate storage.
Status: `OPEN → UNDER_REVIEW → ACCEPTED/PARTIALLY_ACCEPTED/REJECTED → SETTLED`.
Fields: settlement/version, target line/day/type, claimedAmount, acceptedAmount, reason, evidence reference, decision actor/time.

### Invoice match
Persist match snapshot: settlementNet, invoiceSubtotal, tax, withholding, advances, additions/deductions, invoiceTotal, variance, tolerance, overrideApprovalId, result. Posting/payment blocked when absolute/percentage tolerance fails.

## API/command changes

Keep current workforce API base/conventions. Suggested additions, adapted to current endpoint naming:

```text
POST /.../labor-requests/{id}/submit
POST /.../labor-requests/{id}/dispatches
POST /.../dispatches/{id}/assignments
POST /.../assignments/{id}/accept
POST /.../assignments/{id}/reject
POST /.../assignments/{id}/replace
POST /.../attendance/{date}/lock
POST /.../attendance/{id}/corrections
POST /.../settlements/{id}/disputes
POST /.../settlements/{id}/invoice-match
POST /.../settlements/{id}/post
POST /.../settlements/{id}/prepare-payment
```

Every state-changing POST must take `operationId`; material settlement actions also take `expectedVersion`. Return next allowed actions and blocking reasons in detail views.

## Backend implementation sequence

1. Inspect all workforce entities/repositories/services behind `WorkforceSettlementController`; document actual class names before creating new files.
2. Extend labor request source dimensions and validations; add budget check adapter to existing budget service.
3. Wire labor request SUBMIT to existing approval engine.
4. Implement dispatch + worker assignment aggregate without changing current historical request counts; derive counts from assignments for new records and keep compatibility view if needed.
5. Implement site acceptance/replacement and KPI events.
6. Add attendance lock state/correction record; make settlement calculator read approved/locked effective rows according to policy.
7. Keep current calculation-version code; add immutable `SettlementCalculationSnapshot` inputs if not already persisted in sufficient detail.
8. Implement dispute service and `needsRecalculation` triggers.
9. Implement invoice-match service and tolerance configuration.
10. Connect settlement POST to shared posting profile service; store journal ID/profile version.
11. Replace/route direct settlement payment through treasury payment batch when available while keeping compatibility endpoint temporarily.
12. Add bank match/reconciliation drill-down and close checker.

## Frontend implementation

- `[MODIFY] labor-requests page`: source dimensions, estimated cost/budget result, approval state, dispatch status, accepted shortage.
- `[NEW]` dispatch/allocation page or child route under workforce only if current labor-request page would become overloaded. It must link back to request and worker details.
- `[MODIFY] manual-attendance`: show assignment/site, validation/approval/lock state; corrections are a separate action with reason.
- `[MODIFY] settlement-periods`: calculation version diff, unresolved issues/disputes, invoice-match panel, posting journal link, payment-batch/bank-match status.
- `[MODIFY] contractor-accounts`: reconcile settlement/invoice/payment/open balance with drill-down.

UX must show formula inputs for fill rate, invoice variance, settlement totals; no AI text.

## Cross-module integration

- Approval: labor request, settlement, advance, variance override.
- Budget: estimated labor request cost + actual labor expense.
- Finance: contractor settlement posting.
- Treasury: payment batch item source type `CONTRACTOR_SETTLEMENT`.
- Bank reconciliation: payment batch reference.
- Period close: unresolved settlement/dispute/unposted liability checks.
- Audit: assignment acceptance/rejection, corrections, calculation versions, overrides, posting/reversal.

## Required automated and manual tests

Automated: request empty lines; budget fail; approval required; duplicate submit/dispatch; stale version; overlapping assignment; rejected/replacement; attendance correction after lock; settlement version 1→2; dispute partial accept; invoice tolerance pass/fail/override; duplicate post; reversal; unauthorized finance action.

Manual E2E: request 20 workers → propose 22 → accept 20/reject 2 → attendance → correction → settlement v2 → invoice within tolerance → post → pay → bank match → close precheck PASS.



## Junior developer — exact execution order

1. Open `WorkforceApi.java` and settlement controller; list current request/settlement entity/service/repository names.
2. Extend request fields and tests first.
3. Integrate request approval.
4. Add dispatch/assignment schema and backend only; seed/test through API.
5. Add UI for dispatch/acceptance.
6. Add attendance lock/correction.
7. Add settlement snapshots/disputes.
8. Add invoice match.
9. Add GL posting link.
10. Add treasury/bank link.
11. Add close checker.
12. Run the complete Scenario A before moving to another module.
