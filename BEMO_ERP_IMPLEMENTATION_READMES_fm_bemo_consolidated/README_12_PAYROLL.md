# Payroll — Run, Components, Posting and Payment

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

Replace the current “monthly sheet + pay” mental model with controlled pay runs while preserving current functionality during migration. Calculation, approval, accounting posting, and payment must be separate auditable stages.

## Existing code — preserve and extend

- `[EXISTS] PayrollApi.java`, `PayrollController.java`, `PayrollService.java`.
- Current capabilities: payroll sheet by month/category, base salary + attendance OT/late deductions, advance deductions, payment/bulk payment, status, reversal, readiness checks, deterministic explanations.
- Current calculation includes hard-coded policy examples (monthly/hour basis and OT multiplier). These must move into effective-dated component/rule configuration, not be edited inline each time a customer policy changes.

## Target end-to-end cycle

`DRAFT → INPUT_READY → CALCULATED → VALIDATED → PENDING_APPROVAL → APPROVED → POSTED → PAYMENT_PREPARED → PAID → CLOSED`

## Data model changes

### `[NEW] PayrollCalendar`
frequency: MONTHLY/SEMI_MONTHLY/BIWEEKLY/WEEKLY/OFF_CYCLE/FINAL; period generation rules, pay date rule, active/effective dates.

### `[NEW] PayrollRun`
calendarPeriodId, inputSnapshotVersion/set, status, totals, calculationVersion, journalBatchId, paymentBatchId, operation/version fields.

### `[NEW] PayrollRunEmployee`
employee, gross, deductions, employerCost, net, status, cost allocation snapshot.

### `[NEW] PayrollComponentDefinition`
code/name/type, taxable/insurance flags, GL profile, basis, rule type, effective dates, order, rounding, min/max, allocation rule.

Safe deterministic rule types phase 1: `FIXED`, `PERCENT_OF_COMPONENT`, `QUANTITY_X_RATE`, `TABLE_LOOKUP`, `MIN_MAX_CAP`, `CONDITIONAL_ELIGIBILITY`.

### `[NEW] PayrollComponentResult`
runEmployeeId, componentDefinition/version, quantity, rate/basis, amount, explanation inputs.

### Retro
`PayrollRetroAdjustment`: source closed run/snapshot, target run, component, old/new/delta, reason, approval reference.

## API/command changes

Keep old endpoints temporarily, add run APIs:

```text
POST /api/.../payroll/runs
POST /.../runs/{id}/refresh-readiness
POST /.../runs/{id}/calculate
POST /.../runs/{id}/validate
POST /.../runs/{id}/submit
POST /.../runs/{id}/post
POST /.../runs/{id}/prepare-payment
POST /.../runs/{id}/close
POST /.../runs/{id}/reverse
GET  /.../runs/{id}/employees
GET  /.../runs/{id}/employees/{employeeId}/components
```

All commands take operationId; calculate/post/payment/close/reverse also take expectedVersion.

## Backend implementation sequence

1. Add calendar/period/run tables without changing old payroll results.
2. Create adapter that turns current monthly request into a `PayrollRun` for backward compatibility.
3. Extract each hard-coded earning/deduction into component evaluator classes/functions.
4. Keep explanation records; enrich them with rule definition/version and inputs.
5. Calculate only from `PayrollInputSnapshot`, not live attendance.
6. Add validation stage: negative net policy, missing bank details, extreme variance thresholds, unresolved advance state.
7. Integrate `PAYROLL_RUN` approval.
8. Posting: create balanced journal through shared posting service; store journal IDs.
9. Payment preparation: create treasury payment batch items with beneficiary snapshot; no direct bank execution in calculation service.
10. Paid only after payment batch success; bank reconciliation is separate.
11. Close freezes run. Later attendance/rate changes create retro/off-cycle delta.
12. Keep legacy pay/bulk-pay endpoint as adapter until frontend migrated, then deprecate.

## Frontend implementation

Create/extend Payroll Workbench:
- run selector/calendar period;
- readiness blockers;
- CALCULATE with operation progress/error state;
- current vs prior run deterministic variance columns;
- employee component breakdown and explanation;
- approval/posting/payment cards;
- journal/payment batch links;
- disable mutation after POSTED/CLOSED; show “Create correction/retro” instead.

## Cross-module integration

Attendance snapshot → payroll input; advances → deduction components; approval engine → run approval; GL posting → expense/liabilities; treasury → payment batch; bank reconciliation → payment clearing; period close → payroll run/liability checks.

## Required automated and manual tests

Monthly and semi-monthly; one employee joins/leaves mid-period; missing snapshot; blocking exception; duplicate calculate; stale version; fixed/percentage/quantity/table/cap rule; advance deduction; approval denied; balanced journal; duplicate post; missing bank beneficiary; partial payment failure handling; reversal; retro after closed run; payroll payable reconciliation zero after paid scenario.



## Junior developer — exact execution order

1. Add calendar + run schema.
2. Build current-month adapter.
3. Build component catalog + evaluators.
4. Move OT/late/base calculations into components one by one.
5. Add run calculation tests before UI.
6. Add approval.
7. Add posting.
8. Add payment batch.
9. Build workbench.
10. Add retro/off-cycle.
11. Deprecate direct legacy payment only after equivalent E2E passes.
