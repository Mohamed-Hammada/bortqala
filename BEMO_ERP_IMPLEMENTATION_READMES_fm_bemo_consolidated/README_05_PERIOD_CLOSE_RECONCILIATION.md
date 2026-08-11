# Period Close and Reconciliation Framework

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Existing anchor

- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java`
- `[EXISTS] fe/src/app/features/fiscal-periods/`
- `[EXISTS] bank reconciliation and finance controllers.

Current fiscal status control is a foundation; advanced ERP behavior needs a computed checklist before changing period status.

## Target statuses

`OPEN → SOFT_CLOSE → CLOSING → CLOSED`

Exceptional: `CLOSED → REOPENED` only with privileged approval + mandatory reason.

## Proposed service

- `[NEW] CloseChecklistService` if no equivalent exists.
- Each module contributes a `CloseCheckProvider` or explicit checker method.
- The close service aggregates `PASS`, `WARNING`, `BLOCKER` with count/value and drill-down route/reference.

Logical DTO:

```json
{
  "periodId": 123,
  "status": "CLOSING",
  "checks": [
    {"code":"AP_UNMATCHED_INVOICE", "severity":"BLOCKER", "count":2, "amount":15000, "drilldown":"..."}
  ]
}
```

## Required blockers/checks

### Inventory
negative stock, pending adjustments, incomplete transfers, valuation/reconciliation incomplete.

### Procurement/AP
match exceptions above tolerance, GR/IR not reviewed, unposted invoices/payments.

### Sales/AR
unposted invoices/receipts, unapplied receipts, AR-to-GL difference.

### Payroll/workforce
attendance blockers, payroll not closed, contractor settlement not posted/paid where configured as blocker.

### Bank
statements unreconciled, suspense above configured threshold.

### GL
unposted draft journals, subledger differences, FX revaluation incomplete.

## Close command sequence

```text
user requests precheck
→ calculate live checklist
→ user resolves blockers
→ user requests close with operationId/expectedVersion
→ re-run checklist inside close command
→ reject if blockers remain
→ set CLOSING
→ run deterministic close jobs if configured
→ verify jobs/reconciliation
→ set CLOSED
→ audit
```

Never trust a checklist calculated minutes earlier without revalidating during the close transaction/action.

## Reopen

1. User supplies reason.
2. Approval workflow `FISCAL_PERIOD_REOPEN` must finish.
3. Verify later period policy (for example, whether later closed periods prevent reopening).
4. Set REOPENED with new version.
5. Audit approver, reason, timestamp.
6. Any post-reopen corrections must use normal reversal/correction semantics.

## Frontend

Extend `[EXISTS] fe/src/app/features/fiscal-periods/` with a workbench view:
- cards per module;
- PASS/WARNING/BLOCKER;
- count and amount;
- “open items” drill-down;
- refresh precheck;
- close/reopen buttons only when backend says action is allowed.

## Tests

Create fixtures with exactly one blocker per module and prove closing rejects it; then resolve it and prove close succeeds. Test stale version, duplicate close operation, unauthorized reopen, failed approval, later-period restriction, and concurrent new transaction during close.
