# Treasury & Bank — Shared Payment Batch and Reconciliation

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

Unify approved supplier, contractor, payroll and other payable execution into treasury-controlled payment batches and preserve the existing deterministic bank statement matching/reversal logic.

## Existing code — preserve and extend

- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/TreasuryController.java`.
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/BankReconciliationController.java`.
- Existing bank account master, statement import, auto/manual match, reversal and cash-position capabilities should be extended, not replaced.

## Target end-to-end cycle

`Approved Payables → Payment Proposal → Funding/Bank Selection → Payment Batch → Maker/Checker Approval → Execution → Bank Statement → Match → Difference Posting → Reconciliation Close`

## Data model changes

### `[NEW/EXTEND] PaymentBatch`
source bank account, currency, payment/value date, status, approval instance, totals, operation/version.

### PaymentBatchItem
sourceType/sourceId, beneficiary party + bank snapshot, amount/currency, payment method, reference, execution status/error, journal/payment reference.
Source types: supplier invoice, contractor settlement, payroll employee/run, approved other payment.

### Difference reason
bank fee, interest, withholding, FX difference, transfer fee, suspense. Each maps to configured posting profile.

## API/command changes

Suggested treasury endpoints:

```text
POST /.../treasury/payment-batches
POST /.../payment-batches/{id}/submit
POST /.../payment-batches/{id}/approve-or-use-shared-approval
POST /.../payment-batches/{id}/execute
POST /.../payment-batches/{id}/cancel/reverse
GET  /.../payment-batches/{id}/items
POST /.../bank-reconciliation/{lineId}/match
POST /.../bank-reconciliation/{lineId}/post-difference
```

Execution abstraction should support manual confirmation first if no bank-file/API integration exists. Do not fake bank success.

## Backend implementation sequence

1. Trace current treasury/reconciliation services/entities.
2. Add common source reference to payment records/batch items.
3. Build payment proposal adapters from AP/payroll/contractor without moving their liability logic into treasury.
4. Validate source still approved/open/not already paid when batch is created and again before execution.
5. Snapshot beneficiary banking at batch approval/execution boundary; later master-data changes do not alter historical payment.
6. Integrate maker/checker approval.
7. Mark source paid only according to execution success; support partial batch failures item by item without double pay.
8. Reuse current bank statement matcher. Deterministic priority: exact ref+amount; exact amount+date tolerance; reference token; grouped batch total.
9. Never auto-post ambiguous match; expose candidates.
10. Add difference posting profiles and reconciliation close checks.

## Frontend implementation

Treasury workbench:
- due approved sources grouped by currency/bank;
- batch builder;
- maker/checker state;
- item execution results;
- cash availability warning;
- bank statement unmatched candidates;
- difference reason dialog;
- reconciliation completion KPI.

## Cross-module integration

AP/contractor/payroll provide payable source state; approval controls batch; accounting posts payments/differences; bank reconciliation closes execution; fiscal close checks unmatched/suspense values.

## Required automated and manual tests

Source becomes paid after proposal but before execution (must revalidate); duplicate batch execution; partial failure; changed bank master after approval; exact/ambiguous bank match; grouped total; fee difference; FX difference; reversal; payment in closed period; unauthorized maker/checker same-user policy.



## Junior developer — exact execution order

1. Map existing payment and bank-recon entities.
2. Add cross-source batch header/items.
3. Integrate one source type (supplier invoice) end to end.
4. Add contractor settlement.
5. Add payroll.
6. Add approval/execution states.
7. Reuse/extend bank matching.
8. Add difference posting.
9. Add close checks and UI workbench.
