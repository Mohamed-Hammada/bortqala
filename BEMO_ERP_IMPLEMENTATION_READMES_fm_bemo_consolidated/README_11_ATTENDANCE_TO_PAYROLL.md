# Attendance-to-Payroll — Effective Rules and Immutable Input

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

Make payroll independent from mutable raw punches/manual entries. Attendance decisions must be calculated with the rule version effective on the work date, resolved through explicit exceptions, then frozen into a versioned payroll-input snapshot.

## Existing code — preserve and extend

- `[EXISTS] be/src/main/java/com/bemo/hr/reporting/api/ReportController.java` — report creation, daily/bulk decisions, exception/anomaly/holiday actions, approval/reopen paths.
- `[EXISTS] PayrollService.java` checks attendance readiness.
- `[EXISTS] Workforce manual attendance page` for contractor labor attendance; do not confuse employee attendance reporting with workforce contractor attendance when implementing shared rules.
- Preserve current report approval/reopen and exception-decision behavior.

## Target end-to-end cycle

`Raw Punch / Manual Entry → Normalize → Daily Result → Exception Catalogue → Reviewer Decision → Manager Approval → Attendance Lock → PayrollInputSnapshot → Payroll Run`

## Data model changes

### `[NEW/EXTEND] AttendanceRuleVersion`
Effective from/to and fields: scheduled start/end, grace, minimum hours, OT threshold/max, break policy, weekend/holiday, overnight handling, missing-punch policy, rounding.

Use current settings tables if they already represent these values; version them instead of creating duplicates.

### Exception catalogue
Use explicit codes: `MISSING_IN`, `MISSING_OUT`, `SINGLE_PUNCH`, `LATE`, `EARLY_LEAVE`, `EXCESSIVE_OVERTIME`, `ABSENT`, `WORKED_ON_HOLIDAY`, `DUPLICATE_PUNCH`, `OUT_OF_SCHEDULE`, `NEGATIVE_OR_INVALID_DURATION`.
Each type has severity + `blocksPayroll`.

### `[NEW] PayrollInputSnapshot`
Header: employee/pay period/report ID, version, approvedAt/by, rule-version references, hash/count totals.
Lines/day or aggregate components: paid days/minutes, OT minutes, late minutes, absence units, holidays, decision references.
Immutable once referenced by a calculated payroll run.

## API/command changes

Suggested commands under current reporting/payroll API conventions:

```text
GET  /.../attendance-rules?date=YYYY-MM-DD
POST /.../attendance-reports/{id}/validate
POST /.../attendance-reports/{id}/approve   (extend existing behavior)
POST /.../attendance-reports/{id}/snapshot
POST /.../attendance-reports/{id}/reopen
GET  /.../payroll-input-snapshots/{id}
```

Prefer generating snapshot automatically in the approval transaction/event, not requiring a fragile extra manual click.

## Backend implementation sequence

1. Trace existing report approval and payroll readiness code.
2. Identify every setting currently read “as current”; create effective-dated resolver.
3. Add exception type metadata with payroll-block flag.
4. Update calculation/normalization to call `ruleResolver.resolve(workDate)`.
5. At report approval, ensure no blocking exception, then persist immutable payroll input snapshot.
6. Update new payroll-run calculator to read snapshot ID only.
7. Reopen of an approved attendance report must not mutate a snapshot already used by a posted/closed payroll; create a newer snapshot version and retro delta instead.
8. Audit rule version + decisions in snapshot metadata.

## Frontend implementation

Extend current attendance/reporting screens:
- show rule version/date used;
- filter exception type/severity/blocking;
- bulk action preview count before commit;
- show “Payroll blocked: N unresolved exceptions”;
- after approval show snapshot ID/version and lock badge;
- reopen dialog requires reason and warns whether payroll already consumed the snapshot.

## Cross-module integration

Payroll reads snapshot; approval engine controls report approval if policy requires; period close checks open blocking exceptions; retro adjustment service consumes differences between approved snapshots rather than silently recalculating closed payroll.

## Required automated and manual tests

Rule boundary dates; overnight shift; holiday/weekend; one missing punch; duplicate punches; rounding; bulk decision idempotency; approve with blocker; stale report version; reopen; snapshot immutability; two snapshots after correction; closed-payroll correction produces retro item.



## Junior developer — exact execution order

1. Map current settings/readiness methods.
2. Add version resolver + tests.
3. Add exception metadata.
4. Add snapshot schema/service.
5. Hook snapshot into existing approval.
6. Add UI blockers/snapshot display.
7. Update payroll run input adapter.
8. Test a correction after payroll close.
