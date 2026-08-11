# Junior Developer — Ordered Execution Plan

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Rule: finish foundations before broad feature work

The safest implementation is not “Procurement first because it is visible.” Shared integrity controls must exist before multiplying new transaction types.

## Stage 0 — Baseline (no feature code)

1. Checkout `fm_bemo_consolidated`.
2. Record `git rev-parse HEAD`; expected reference for this package is `aa3f940cca0119d7f523e03e3fd317fb72684cf3`.
3. Run backend tests/build and frontend tests/build.
4. Record existing failures; do not mix baseline defects with new-feature defects.
5. Open files from `CODE_TOUCHPOINT_INDEX.md` in IDE.
6. Search for persistence base entity, audit helper, security annotations/guards, money/currency type, transaction conventions, exception handler, DB migration mechanism.
7. Update local implementation notes if branch is newer than this package.

**Exit:** project builds/runs and developer understands existing conventions.

## Stage 1 — Shared transition pilot

1. Standard error codes/409 mapping.
2. Standard operationId/expectedVersion convention by extending current finance/workforce pattern.
3. Pilot on one existing settlement/post command.
4. Add duplicate/stale-version tests.

**Exit:** one real command is replay-safe and concurrency-safe.

## Stage 2 — Approval adapter/convention

1. Read existing Approval API/service/entities.
2. Integrate one `LABOR_REQUEST` submit/approve flow.
3. Prove self-approval/amount routing/delegation as configured.
4. Add source approval instance reference/snapshot only where needed.

**Exit:** no module-specific approval clone.

## Stage 3 — Posting profile + journal source link

1. Inspect current account-mapping and journal services.
2. Add/extend posting profile.
3. Post one contractor settlement through it.
4. Store source→journal/profile version.
5. Duplicate-post and reversal tests.

**Exit:** one source financially closes without manual journal recreation.

## Stage 4 — Close/reconciliation skeleton

1. Create close-check aggregation contract.
2. Implement journal draft + one subledger check.
3. Extend fiscal-period UI with precheck response.

**Exit:** close can block on live data.

## Stage 5 — Workforce vertical completion

Follow `README_10`: request dimensions → approval → dispatch/assignment → attendance lock → settlement snapshot/dispute → invoice match → posting → treasury → bank → close.

**Exit:** Scenario A.

## Stage 6 — Attendance and payroll

Follow `README_11` then `README_12`: rule versions → immutable snapshot → calendar/run/components → approval → posting → payment → retro.

**Exit:** Scenario D.

## Stage 7 — Procurement

Follow `README_20`: requisition → sourcing → existing PO/GRN → quality/match → AP posting → payment proposal/batch → bank.

**Exit:** Scenario B.

## Stage 8 — Inventory + Sales

Inventory location/status/reservation foundation first; then Sales lines/confirm/reservation/delivery/returns.

**Exit:** reservation race tests + Scenario C.

## Stage 9 — Manufacturing

BOM snapshot → reservation → actual issue/return → partial receipt → QC → WIP → variance → close.

**Exit:** Scenario E.

## Stage 10 — Budget/Treasury hardening + workbenches

Complete multi-source payments, revisions/transfers, all close providers, dashboard workbenches and reconciliations.

## One-ticket coding loop

For **every** ticket:

```text
1. Read exact existing controller/page.
2. Trace controller → service → repository/entity.
3. Write failing test for desired invariant.
4. Add/alter schema using existing migration mechanism.
5. Implement domain/service rule.
6. Expose API command/query.
7. Add frontend only after API rule works.
8. Add authorization + audit.
9. Add idempotency/version/reversal behavior where relevant.
10. Run module + regression tests.
11. Update README checkbox and code touchpoint if actual path differs.
```

Do not skip step 2. Most duplication in ERP projects happens because a developer starts at step 5 without tracing what already exists.
