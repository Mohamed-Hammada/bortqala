# Bortqala ERP — Remaining Work & Release Acceptance Checklist

**Branch:** `fm_bemo_consolidated`  
**Audit date:** 2026-08-12  
**Audit anchor / reviewed HEAD:** `a9430d3`  
**Purpose:** This file is the execution and acceptance checklist for the remaining work after the Staff/Tech Lead re-review.

## Repository status update — 2026-08-12

**Reviewed baseline SHA:** `a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8`  
**Current changes:** working tree; no implementation commit SHA exists yet.  
**Release status:** **NOT RELEASE READY** — final CI, PostgreSQL/Testcontainers, full-suite, smoke, reviewer, and final-SHA evidence remain.

| Item | Current status | Evidence / remaining gate |
|---|---|---|
| DOC-001 | VERIFY | `README.md` and `PROJECT_MAP.md` now point here, contradictory “none/all complete” text is removed, status categories are explicit; reviewer/final commit SHA remain. |
| PAY-001 | VERIFY | Snapshot is wired into calculation/payment/status paths; focused payroll suites pass. PostgreSQL, full-suite, frontend/API acceptance, concurrency, and final-SHA evidence remain. |
| PAY-002 | VERIFY | Effective-dated tenant policy supplies divisor/multiplier and snapshot stores both; focused policy/snapshot tests pass. Policy UI and final release evidence remain. |
| REL-001 | BLOCKED | GitHub Actions account/billing lock is external and unresolved. |
| REL-002 | OPEN | Historical counts are not carried forward; all final candidate commands and smoke checks must be rerun. |
| MFG-001 | VERIFY | `PLANNED → IN_PROGRESS` freezes requirements before issue; active readiness/completion/cancel use snapshots and completion reads issue valuation evidence. PostgreSQL, concurrency, full-suite, and final-SHA evidence remain. |
| P2P-001 | VERIFY | Source/E2E characterization required before modification. |
| O2C-001 | VERIFY | Source/E2E characterization required before modification. |
| INV-001 | VERIFY | Reconcile existing delivered inventory functions; do not rebuild without a proven gap. |
| SHARED-001…004 | VERIFY | Real command-path characterization required. |
| TRS-001…004 | VERIFY | Real command-path characterization required. |
| FIN-001…006 | VERIFY | Real command-path characterization required. |
| Final ALL DONE gate | OPEN | Cannot pass while any P0 gate is open/blocked or evidence is not tied to the final SHA. |

This table is a status index only. The detailed criteria below remain authoritative; unchecked criteria are still required.

---

# 0. Mandatory Rule — When a Checkbox May Be Marked Done

> **DO NOT mark any item `[x]` just because a class, entity, controller, endpoint, page, migration, or test file exists.**
>
> A task may be marked `[x]` **only when every acceptance criterion under that task is satisfied and evidence is recorded.**

A feature is **NOT DONE** when any required layer is missing:

- Domain/business behavior
- Persistence/schema, when required
- Service/application wiring
- API/controller wiring
- Authorization
- Tenant isolation
- Idempotency/concurrency protection where money, stock, approvals, imports, or posting are involved
- Frontend/UI wiring for user-facing flows
- Validation and error handling
- Automated tests
- End-to-end verification
- Documentation that matches the real code
- Green verification on the exact commit being signed off

The following **do not count as completion by themselves**:

- “The entity exists.”
- “The service exists.”
- “The controller exists.”
- “The frontend service has the endpoint.”
- “The page compiles.”
- “Unit tests pass.”
- “The README says DONE.”
- “PROJECT_MAP says DONE.”
- “It worked manually once.”
- “CI could not run for an external reason.”
- “The implementation was already there but nobody verified the full flow.”

If an item was already implemented before this checklist:

1. Do **not** rewrite it.
2. Verify it against the acceptance criteria.
3. Add the evidence.
4. Only then mark it `[x]`.

If an item is truly out of scope:

- Do **not** silently mark it done.
- Mark it `N/A` only after Tech Lead/product approval.
- Record the reason and approver.

---

# 1. Status Legend

- `[ ]` = OPEN, not yet accepted
- `[x]` = VERIFIED DONE, all criteria passed
- `VERIFY` = implementation may already exist, but release-level evidence is missing
- `CONFIRMED GAP` = source review shows a real remaining problem
- `BLOCKED` = work cannot be accepted because an external/internal gate is blocked
- `N/A` = explicitly approved as out of scope

---

# 2. Evidence Required for Every Completed Item

Before changing `[ ]` to `[x]`, fill this template beneath the item:

```text
Status:
Implementation commit SHA:
Files changed:
Database migration(s):
Backend tests:
Frontend tests:
Integration/PostgreSQL tests:
API evidence:
UI evidence:
Authorization evidence:
Tenant-isolation evidence:
Idempotency/concurrency evidence:
CI run URL/result:
Manual acceptance notes:
Reviewer:
Review date:
```

If a field does not apply, write `N/A — <reason>`.

Do not leave evidence fields blank and still mark the parent item done.

---

# 3. Release Sign-Off Summary

The project must **not** be described as “all done”, “fully complete”, “fully verified”, or “release ready” until all P0 tasks and the Final Release Gate are `[x]`.

Current Staff/Tech Lead position:

- Implementation maturity: **Mostly complete**
- Documentation/source-of-truth consistency: **Not complete**
- Payroll deterministic snapshot calculation: **Not complete**
- Manufacturing immutable execution source: **Not complete**
- Several advanced vertical flows: **Need source/E2E verification**
- Independent CI verification: **Blocked / not green on reviewed HEAD**

---

# 4. P0 — Documentation Must Match Reality

## DOC-001 — Fix the PROJECT_MAP source-of-truth contradiction

**Status:** `VERIFY — documentation reconciled in working tree; final SHA/reviewer pending`

The roadmap currently contains a high-level `[ORPHANS & PENDING]` statement indicating that nothing remains, while the later source-verified section still identifies open integration work.

This is dangerous because a developer can point to the “None / all implemented” statement even while source-verified work remains below it.

### Required work

- [x] Keep only one authoritative interpretation of remaining work.
- [x] Keep `[SOURCE-VERIFIED OPEN WORK]` for anything that still requires implementation or verification.
- [x] Change `[ORPHANS & PENDING]` so it does **not** say “None / all complete” while open work exists.
- [x] Remove stale numeric claims such as `84 / 86 pending` if those counts no longer reflect source reality.
- [x] Do not list already-delivered primitives as missing.
- [x] Separate these states clearly:
  - [x] `DONE`
  - [x] `VERIFIED`
  - [x] `OPEN`
  - [x] `VERIFY`
  - [x] `BLOCKED`
  - [x] `OUT OF SCOPE`
- [ ] Add the verification SHA for every final release claim.
- [x] Ensure `README.md`, `PROJECT_MAP.md`, and `docs/TEST_EVIDENCE.md` do not contradict each other.
- [x] Do not write “all done” until the Final Release Gate in this document passes.

### Definition of Done

Mark `DOC-001` complete only when:

- [x] There is no contradictory “nothing pending” statement while an open-work section still exists.
- [x] Every open item has a clear status.
- [ ] Every completed item has source/test evidence.
- [x] Historical roadmap notes are clearly marked historical and are not confused with current status.
- [x] The current HEAD SHA is recorded in the status documentation.
- [x] Tech Lead can read one section and understand the actual current state without inspecting Git history.

### Evidence

```text
Status: VERIFY — implementation complete; reviewer/final commit pending
Commit SHA: N/A — working tree based on a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8
PROJECT_MAP section changed: [ORPHANS & PENDING] and [SOURCE-VERIFIED OPEN WORK]
README section changed: authoritative documentation paragraph
TEST_EVIDENCE section changed: 2026-08-12 release-checklist remediation entry
Reviewer: PENDING
```

---

# 5. P0 — Payroll Must Be Snapshot-Driven and Deterministic

## PAY-001 — Make `PayrollInputSnapshot` the sole calculation source

**Status:** `VERIFY — implementation and focused tests added; release-level evidence pending`

The current payroll calculation path still reads live employee/payroll inputs during calculation. For example, `PayrollService` still reads the current employee base salary directly.

A snapshot service/entity existing in the repository is **not enough**. The actual calculation path must consume the frozen snapshot.

### Required behavior

The intended flow should be:

```text
Payroll Period / Run
        ↓
Collect all calculation-relevant inputs
        ↓
Create immutable PayrollInputSnapshot
        ↓
Freeze snapshot
        ↓
Calculate payroll ONLY from snapshot
        ↓
Persist calculated result
        ↓
Approve / Post / Pay
```

### Implementation checklist

- [ ] Identify the exact command that starts payroll calculation for a period/run.
- [ ] Create or load a `PayrollRun` for that execution.
- [ ] Capture an immutable `PayrollInputSnapshot` before calculating employee results.
- [ ] Snapshot all inputs that can affect the result, including as applicable:
  - [ ] Employee ID
  - [ ] Period start/end
  - [ ] Base salary used for this run
  - [ ] Attendance totals used by payroll
  - [ ] Late minutes
  - [ ] Overtime minutes
  - [ ] Approved attendance adjustments
  - [ ] Payroll policy/version
  - [ ] Working-hour divisor used
  - [ ] Overtime multiplier used
  - [ ] Deductions used
  - [ ] Bonuses used
  - [ ] Advance/installment amount used
  - [ ] Any category/employee override that changes calculation
- [ ] Ensure the calculation engine reads snapshot fields instead of querying current employee/attendance values.
- [ ] Ensure recalculation of the same frozen run does not silently replace snapshot inputs.
- [ ] Prevent later salary edits from changing an already-frozen payroll run.
- [ ] Prevent later attendance edits from changing an already-frozen payroll run.
- [ ] Prevent later payroll-policy edits from changing an already-frozen payroll run.
- [ ] Ensure the payroll explanation/breakdown uses the **same frozen inputs** as the stored calculation.
- [ ] Store enough evidence to explain why a payroll value was produced.
- [ ] Keep the implementation simple; do not build a generic rules engine unless the current requirements genuinely need one.

### Required automated tests

- [ ] `snapshot_is_created_before_calculation`
- [ ] `calculation_reads_snapshot_not_live_employee_salary`
- [ ] `salary_change_after_snapshot_does_not_change_existing_run`
- [ ] `attendance_change_after_snapshot_does_not_change_existing_run`
- [ ] `policy_change_after_snapshot_does_not_change_existing_run`
- [ ] `same_snapshot_produces_same_result`
- [ ] `approved_or_posted_run_cannot_mutate_snapshot`
- [ ] `payroll_explanation_matches_snapshot_and_persisted_result`
- [ ] Tenant A cannot read or calculate using Tenant B snapshots.
- [ ] Repeated calculate command is idempotent or explicitly version-controlled.

### End-to-end acceptance scenario

- [ ] Create employee with salary `S1`.
- [ ] Create attendance data `A1`.
- [ ] Create/freeze payroll snapshot.
- [ ] Calculate payroll and save result `R1`.
- [ ] Change live employee salary to `S2`.
- [ ] Change live attendance to `A2`.
- [ ] Re-open the same payroll run.
- [ ] Result remains `R1`.
- [ ] Explanation still references `S1/A1`, not `S2/A2`.
- [ ] Start a **new** payroll run after the changes.
- [ ] New run correctly uses the newer effective inputs.

### Definition of Done

- [ ] No production payroll calculation path uses live salary/attendance/config after the snapshot has been frozen.
- [ ] Snapshot immutability is proven by tests.
- [ ] Explanation and stored result are based on the same snapshot.
- [ ] Relevant backend tests are green.
- [ ] PostgreSQL integration behavior is green where persistence/concurrency matters.
- [ ] UI/API still work end-to-end.

### Evidence

```text
Status: VERIFY — focused implementation/tests green; release acceptance incomplete
Commit SHA: N/A — working tree based on a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8
Payroll calculation entry point: PayrollService.transitionStatus / recordPayment
Snapshot creation method: PayrollSnapshotService.captureSnapshot
Calculation method: PayrollSnapshotService.captureSnapshot from frozen CalculationInputs
Tests added: PayrollSnapshotServiceTests replay test; PayrollCalculationPolicyServiceTests
PostgreSQL test result: NOT RUN
UI/API scenario: NOT RUN; existing UI contract compiles in source but final frontend verification remains
Reviewer: PENDING
```

---

## PAY-002 — Remove hard-coded payroll calculation constants

**Status:** `VERIFY — effective-dated policy implemented; release-level evidence pending`

The reviewed `PayrollService` contains fixed calculation values equivalent to:

```java
base / 240
overtime * 1.5
```

These must not remain hidden magic constants in the production calculation path.

### Required work

- [ ] Remove the hard-coded monthly-hours divisor from payroll calculation logic.
- [ ] Remove the hard-coded overtime multiplier from payroll calculation logic.
- [ ] Store the effective values in the simplest correct source:
  - [ ] Existing payroll policy/configuration if already available, **or**
  - [ ] A small effective-dated payroll calculation policy if no suitable source exists.
- [ ] Copy the effective values into the `PayrollInputSnapshot`.
- [ ] Calculate from the snapshotted values.
- [ ] Do not read the current policy during recalculation of a frozen run.
- [ ] Validate policy values:
  - [ ] Working-hour divisor > 0
  - [ ] Overtime multiplier >= 0
  - [ ] Effective date range is valid
- [ ] Document the source of each payroll formula input.

### Tests

- [ ] Different configured divisor produces the expected result.
- [ ] Different configured overtime multiplier produces the expected result.
- [ ] Changing policy after snapshot does not change an existing run.
- [ ] Invalid divisor is rejected.
- [ ] Invalid effective dates are rejected.
- [ ] Policy selection is tenant-scoped.
- [ ] Policy selection is deterministic for a period date.

### Definition of Done

- [ ] No unexplained `240`, `1.5`, or equivalent magic payroll constants remain in the active calculation path.
- [ ] Values are configurable/effective-dated at the appropriate level.
- [ ] Frozen snapshot stores the values actually used.

### Evidence

```text
Status: VERIFY — active path policy-backed; release acceptance incomplete
Commit SHA: N/A — working tree based on a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8
Policy/config source: effective-dated tenant-owned PayrollCalculationPolicy; /api/v1/payroll/calculation-policies
Snapshot fields: payrollPolicyId/version, workingHourDivisor, overtimeMultiplier
Tests: PayrollCalculationPolicyServiceTests and PayrollSnapshotServiceTests focused suite PASS
Reviewer: PENDING
```

---

# 6. P0 — Final CI / Release Verification

## REL-001 — Get a green CI run on the exact final SHA

**Status:** `BLOCKED`

The latest reviewed GitHub Actions run for `a9430d3` failed in only a few seconds because the jobs were not started due to a GitHub account/billing lock.

This is not a code-test failure, but it is also **not green verification**.

### Required work

- [ ] Resolve the GitHub Actions account/billing lock, or move the same trusted pipeline to an approved runner.
- [ ] Push the final implementation commit.
- [ ] Run CI on the exact SHA that is proposed for release.
- [ ] Backend job starts and passes.
- [ ] Frontend job starts and passes.
- [ ] Compose validation/backend image job starts and passes.
- [ ] No job is skipped because of account lock.
- [ ] No required job is manually bypassed.
- [ ] Save the CI run URL in `docs/TEST_EVIDENCE.md`.

### Definition of Done

- [ ] CI status is green on the final candidate SHA.
- [ ] All required jobs actually executed.
- [ ] The recorded SHA matches the code being signed off.

### Evidence

```text
Status:
Final SHA:
CI run URL:
Backend job:
Frontend job:
Compose/image job:
Reviewer:
```

---

## REL-002 — Refresh full release evidence on the final SHA

**Status:** `OPEN`

Historical green test counts are useful but do not replace a final verification run after the last fixes.

### Mandatory backend verification

From `be/`:

- [ ] `./gradlew test -PskipDockerTests`
- [ ] `python tools/check-error-codes.py`
- [ ] `python tools/check-translation-catalog.py`
- [ ] `python tools/check-authorization-contract.py`

For release sign-off:

- [ ] Run the PostgreSQL/Testcontainers test suite with Docker available.
- [ ] Verify fresh Liquibase migration on PostgreSQL.
- [ ] Verify upgrade-path migration if the project release gate requires it.
- [ ] Verify `ddl-auto=validate` succeeds against the migrated schema.
- [ ] Do not reduce a test-count baseline to hide a regression.

### Mandatory frontend verification

From `fe/`:

- [ ] `npm ci`
- [ ] `npm run check:i18n`
- [ ] `npm run check:hardcoded`
- [ ] `npm run test -- --watch=false`
- [ ] `npm run build`
- [ ] Use the Node version required by `.nvmrc` / package engines.
- [ ] No new ignored/focused/disabled tests are introduced to force green.

### Mandatory release smoke verification

- [ ] Application starts from production-like Compose.
- [ ] Database becomes healthy.
- [ ] Backend becomes healthy.
- [ ] Frontend loads.
- [ ] Login works.
- [ ] Critical API authorization is enforced.
- [ ] At least one critical business flow per financial/stock module is exercised.
- [ ] No fatal migration errors.
- [ ] No console/runtime errors that block core workflows.

### `docs/TEST_EVIDENCE.md` rules

- [ ] Add current date.
- [ ] Add exact final HEAD SHA.
- [ ] Record exact commands.
- [ ] Record exact pass/fail counts observed.
- [ ] Record PostgreSQL/Testcontainers result.
- [ ] Record frontend build result.
- [ ] Record CI URL.
- [ ] Do not copy old counts forward without rerunning.
- [ ] Do not call a run verified if it did not execute.

### Definition of Done

- [ ] All required verification commands pass on the final candidate.
- [ ] Test evidence is reproducible and SHA-specific.
- [ ] No required release gate remains blocked.

---

# 7. P1 — Manufacturing Must Use a Frozen BOM for Execution

## MFG-001 — Stop using the live BOM after production execution is frozen

**Status:** `VERIFY — frozen execution implemented; release-level evidence pending`

The reviewed manufacturing flow captures BOM snapshot rows when production starts, but important execution paths still read the live BOM.

Observed behavior includes:

- Production order is linked to a BOM ID/revision.
- Material readiness reads the current `BomHeader` and its current lines.
- Start performs readiness from the current BOM and then captures snapshots.
- Completion calls readiness again, which can resolve against the current live BOM.

That means a BOM edit after production starts can potentially influence later execution/costing behavior.

### Correct simple design

Use this lifecycle:

```text
PLANNED order
    ↓
Pre-start readiness may inspect current BOM
    ↓
START / RELEASE
    ↓
Freeze BOM snapshot for this production order
    ↓
Issue/reserve materials from frozen requirements
    ↓
All IN_PROGRESS / completion behavior reads frozen production evidence
    ↓
Complete / close
```

Do not create a separate microservice or rewrite the manufacturing module.

### Required work

- [ ] Define the exact state transition when the BOM becomes frozen.
- [ ] Capture the complete material requirement snapshot **before** irreversible material issue/posting.
- [ ] Ensure each production order has one authoritative frozen BOM requirement set/version.
- [ ] After freeze, do not read live BOM lines for:
  - [ ] Material issue quantity
  - [ ] Remaining material requirement
  - [ ] Completion validation
  - [ ] Expected material usage
  - [ ] Standard/expected variance basis
- [ ] Completion must not call a helper that re-reads the live BOM.
- [ ] If actual cost is based on actual stock issues, calculate from actual issued-movement evidence.
- [ ] If expected cost/variance is required, compare actual evidence against the frozen BOM snapshot.
- [ ] Editing the master BOM after work-order start must not alter the active order.
- [ ] Preserve historical BOM revision/version evidence.

### Required tests

- [ ] Start order from BOM revision R1.
- [ ] Freeze snapshot.
- [ ] Change master BOM to R2.
- [ ] Active order still uses R1 frozen requirements.
- [ ] Material issue quantities do not change after master BOM edit.
- [ ] Completion does not use R2.
- [ ] Expected-versus-actual variance uses frozen R1 basis.
- [ ] New production order created/started later can use R2.
- [ ] Tenant isolation for BOM snapshot data.
- [ ] Retry/start command does not duplicate snapshot rows or stock issues.

### Definition of Done

- [ ] No `IN_PROGRESS` or completion calculation depends on mutable BOM lines.
- [ ] Snapshot behavior is proven by an automated test that changes the BOM after start.
- [ ] Stock movements remain idempotent.
- [ ] Production result is reproducible from persisted evidence.

### Evidence

```text
Status: VERIFY — focused implementation/tests green; release acceptance incomplete
Commit SHA: N/A — working tree based on a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8
Freeze transition: ProductionOrder PLANNED → IN_PROGRESS
Snapshot source: BomSnapshot rows captured from pre-start readiness before stock issue
Methods changed: ManufacturingService.checkMaterialReadiness/startProductionOrder/completeProductionOrder/cancelProductionOrder
Tests: ManufacturingServiceTests.activeOrderUsesFrozenBomAfterMasterBomChanges; focused suite PASS
Stock-movement idempotency evidence: optimistic order transition + unique app/order/component snapshot constraint; PostgreSQL concurrency NOT RUN
Reviewer: PENDING
```

---

# 8. P1 — Procure-to-Pay: Prove Payment Proposal Is Truly End-to-End

## P2P-001 — Payment proposal execution must create/drive the real payment lifecycle

**Status:** `VERIFY — do not mark DONE without E2E proof`

The repository contains many P2P foundations. Do **not** rebuild requisition/RFQ/PO/GRN/invoice capabilities that already exist.

The remaining acceptance question is whether a payment proposal is a real connected business flow or merely changes its own status.

### Required verification first

- [ ] Locate the payment-proposal aggregate.
- [ ] Locate the proposal application/service execution method.
- [ ] Locate approval/maker-checker handling.
- [ ] Locate supplier-payment creation.
- [ ] Locate treasury/disbursement integration.
- [ ] Locate invoice outstanding-balance update.
- [ ] Locate partner-ledger posting.
- [ ] Locate finance journal posting if required by current architecture.
- [ ] Locate operation-ID/idempotency protection.
- [ ] Locate UI action and user-visible execution result.
- [ ] Locate tests proving the complete chain.

### Required end-to-end flow

The accepted flow must be equivalent to:

```text
Eligible supplier invoices
        ↓
Payment proposal
        ↓
Approval / maker-checker
        ↓
Execute proposal
        ↓
Create real supplier payment/disbursement
        ↓
Update invoice outstanding balance
        ↓
Update partner ledger
        ↓
Treasury / bank / accounting posting as applicable
```

### Acceptance criteria

- [ ] Proposal contains traceable invoice/payment candidates.
- [ ] Approval is enforced before execution when required.
- [ ] Executor cannot bypass required SoD/maker-checker rules.
- [ ] Execution creates or links the real supplier payment record.
- [ ] Partial payment behaves correctly.
- [ ] Multiple invoice allocation behaves correctly.
- [ ] Overpayment is rejected.
- [ ] Supplier bank validation remains enforced.
- [ ] Invoice outstanding balance changes exactly once.
- [ ] Ledger/posting changes exactly once.
- [ ] Retrying the same operation ID does not duplicate money movement.
- [ ] A failed posting does not leave the proposal marked successfully executed with missing downstream records.
- [ ] Transaction boundaries keep proposal/payment/accounting consistent.
- [ ] UI displays actual execution outcome and references.

### Required tests

- [ ] Happy path proposal → approval → payment.
- [ ] Partial invoice payment.
- [ ] Multiple invoice allocation.
- [ ] Overpayment rejection.
- [ ] Missing/unverified bank rejection.
- [ ] Unauthorized executor rejection.
- [ ] Maker/checker self-approval rejection where applicable.
- [ ] Same operation ID replay creates no duplicate payment.
- [ ] Concurrent execution creates no duplicate payment.
- [ ] Downstream posting failure rolls back or records a recoverable failed state correctly.

### Completion rule

If the current implementation already passes all criteria, **add evidence and mark it done**.

If the current implementation only changes proposal state to something like `EXECUTED`, it remains **OPEN**.

---

# 9. P1 — Order-to-Cash Must Be Verified as One Connected Vertical Slice

## O2C-001 — Verify Sales Order → Cash + Return/Credit lifecycle

**Status:** `VERIFY`

Do not mark O2C complete because `SalesOrder`, reservation, invoice, receipt, or return services exist separately.

### Required vertical slice

- [ ] Sales order contains real line items.
- [ ] Price used by the order is frozen/versioned appropriately.
- [ ] Reservation is created against available stock.
- [ ] Reservation affects available-to-promise correctly.
- [ ] Delivery consumes/releases reservation correctly.
- [ ] Delivery creates the correct inventory movement.
- [ ] COGS posting is generated once.
- [ ] Customer invoice is created/linked.
- [ ] Receipt can be allocated partially.
- [ ] Receipt can be allocated across valid invoices according to current rules.
- [ ] Customer balance/AR is updated correctly.
- [ ] Return/RMA is linked to the original sale/delivery.
- [ ] Returned stock disposition is correct.
- [ ] Credit note is created/linked correctly.
- [ ] Reversal/cancellation rules prevent double financial/stock effects.
- [ ] Permissions are enforced at API level.
- [ ] Tenant isolation is proven.

### Pricing snapshot acceptance

- [ ] Freeze pricing inputs at the correct order lifecycle state.
- [ ] Later price-list/config changes do not alter an already-frozen order.
- [ ] New orders can use the new price.
- [ ] Pricing explanation/reference remains traceable.

### Required E2E test

```text
Create item/customer
→ Sales Order
→ Reserve
→ Deliver
→ Verify stock
→ Verify COGS
→ Invoice
→ Partial Receipt
→ Final Receipt
→ Verify AR
→ Return part of delivery
→ Credit Note
→ Verify stock + AR + GL consistency
```

- [ ] Automated integration coverage exists for the critical path.
- [ ] UI can execute/inspect each user-facing step.
- [ ] Each document shows upstream/downstream references.

### Definition of Done

- [ ] One test or test family proves the entire chain.
- [ ] Stock, AR, partner/customer balance, and GL remain balanced/traceable.
- [ ] Retry/reversal paths do not duplicate effects.

---

# 10. P1 — Inventory: Verify Existing Capabilities, Do Not Rewrite Them

## INV-001 — Reconcile roadmap claims with actual inventory implementation

**Status:** `VERIFY`

The roadmap historically listed the following as missing:

- warehouse/bin hierarchy
- stock-status balances
- reservation
- transfer
- cycle count
- lot/serial tracking

Some or many of these primitives already exist in the repository.

**Do not implement a second inventory subsystem.**

For each capability below, mark it done only if all layers are verified.

### INV-001A — Warehouse / bin hierarchy

- [ ] Persistent model exists.
- [ ] Tenant ownership is correct.
- [ ] API exists.
- [ ] Authorization exists.
- [ ] UI exists if operational users must maintain it.
- [ ] Stock movement can reference the required location level.
- [ ] Invalid cross-warehouse/bin references are rejected.
- [ ] Tests exist.

### INV-001B — Stock status balances

Required statuses as applicable:

- [ ] `AVAILABLE`
- [ ] `QUARANTINE`
- [ ] `BLOCKED`

Acceptance:

- [ ] Status is persisted, not UI-only.
- [ ] Available-to-promise excludes non-available stock.
- [ ] Status changes are auditable.
- [ ] Moving stock between statuses preserves quantity.
- [ ] Negative invalid status balances are prevented.
- [ ] Tests cover status availability rules.

### INV-001C — Reservation

- [ ] Reservation aggregate/model exists.
- [ ] Reservation references item/location/document.
- [ ] Reservation reduces available-to-promise but does not fake physical movement.
- [ ] Delivery/issue consumes reservation.
- [ ] Cancellation/expiry releases reservation.
- [ ] Concurrent reservation cannot oversubscribe stock.
- [ ] Idempotency is enforced.
- [ ] Tests prove race/concurrency behavior.

### INV-001D — Warehouse transfer

- [ ] Transfer has a controlled state machine.
- [ ] Source stock decreases at the correct point.
- [ ] Destination stock increases at the correct point.
- [ ] In-transit quantity is traceable if the business flow requires it.
- [ ] Repeated receive cannot duplicate stock.
- [ ] Invalid source/destination combinations are rejected.
- [ ] UI exposes transfer state/reference.
- [ ] Tests cover partial/retry behavior if supported.

### INV-001E — Cycle count

- [ ] Count document exists.
- [ ] Expected quantity is snapshotted/frozen appropriately.
- [ ] Counted quantity is recorded.
- [ ] Variance is calculated.
- [ ] Approval is required for material adjustments if required by current policy.
- [ ] Adjustment posts a traceable stock movement.
- [ ] Adjustment is idempotent.
- [ ] Audit trail exists.
- [ ] Tests cover positive and negative variance.

### INV-001F — Lot / serial traceability

- [ ] Lot/serial master/evidence exists.
- [ ] Receipt can capture required lot/serial.
- [ ] Issue/delivery can consume a valid lot/serial.
- [ ] Duplicate serial is rejected.
- [ ] Quantity/serial consistency is enforced.
- [ ] Trace query can identify upstream/downstream document references.
- [ ] Returns preserve traceability.
- [ ] Tests exist.

### Definition of Done for INV-001

- [ ] Every applicable sub-capability is individually verified.
- [ ] Missing sub-capabilities are implemented without duplicating existing inventory architecture.
- [ ] One inventory source of truth remains.
- [ ] No parallel “new inventory module” is introduced unnecessarily.

---

# 11. P1 — Shared Financial/Document Controls

## SHARED-001 — Verify shared document transition wiring

**Status:** `VERIFY`

A `DocumentTransitionService` existing is not enough.

- [ ] Identify every financial/stock document with controlled lifecycle states.
- [ ] Verify allowed transitions use the shared transition rules or an explicitly justified domain-specific equivalent.
- [ ] Invalid transitions return stable business errors.
- [ ] Transition authorization is enforced server-side.
- [ ] Transition audit is recorded.
- [ ] Optimistic/pessimistic concurrency protection is appropriate.
- [ ] Repeated transition commands are idempotent where required.
- [ ] Tests cover invalid state changes.

### Done only when

- [ ] No critical document controller/service bypasses the intended transition guardrails.

---

## SHARED-002 — Verify Segregation of Duties is enforced in real command paths

**Status:** `VERIFY`

- [ ] Locate `SegregationOfDutiesService`.
- [ ] List the business commands that require SoD.
- [ ] Verify those commands call/enforce SoD in the actual execution path.
- [ ] Approval UI restrictions are not the only protection.
- [ ] Backend rejects forbidden self-approval/self-execution.
- [ ] Maker/checker identity is persisted.
- [ ] Audit records both actors.
- [ ] Tests prove direct API bypass is impossible.

### Done only when

- [ ] A user cannot bypass SoD by calling the API directly.

---

## SHARED-003 — Fiscal close precheck must block unsafe close

**Status:** `VERIFY`

- [ ] `/precheck` returns real blockers.
- [ ] Close command internally enforces the same conditions.
- [ ] Calling close directly without first opening the UI precheck cannot bypass rules.
- [ ] Open subledgers/unposted documents/reconciliation issues are included according to current scope.
- [ ] Close is idempotent.
- [ ] Reopen permissions are controlled.
- [ ] Tests cover blocked and successful close.

---

## SHARED-004 — Subledger posting must create balanced accounting evidence

**Status:** `VERIFY`

- [ ] Subledger posting produces debit and credit lines.
- [ ] Total debit = total credit.
- [ ] Source document ID/type is retained.
- [ ] Operation ID prevents duplicate posting.
- [ ] Closed-period posting is rejected.
- [ ] Currency handling follows finance rules.
- [ ] Reversal references the original posting.
- [ ] Tests prove balanced posting and replay safety.

---

# 12. P1 — Treasury / Budget / Close Verification

## TRS-001 — Multi-source payment batch

**Status:** `VERIFY`

- [ ] Batch can include only eligible approved payable sources.
- [ ] Duplicate source inclusion is prevented.
- [ ] Batch total is derived server-side.
- [ ] Approval/maker-checker is enforced.
- [ ] Execution creates traceable payment/disbursement records.
- [ ] Partial failure behavior is explicitly designed.
- [ ] Replay does not duplicate payments.
- [ ] Tests cover concurrent execution.

---

## TRS-002 — Budget revision versioning

**Status:** `VERIFY`

- [ ] Revision produces a new version instead of mutating approved historical evidence.
- [ ] Effective/current version is deterministic.
- [ ] Approval is required where configured.
- [ ] Old versions remain queryable.
- [ ] Encumbrance/available-budget calculations use the correct version.
- [ ] Audit reason is mandatory.
- [ ] Tests prove historical immutability.

---

## TRS-003 — Module close providers

**Status:** `VERIFY`

- [ ] Each in-scope module exposes deterministic close blockers.
- [ ] Fiscal close aggregates module blockers.
- [ ] No module silently reports green when unfinished transactions remain.
- [ ] Provider failures do not incorrectly permit close.
- [ ] Tests cover at least one blocker from each in-scope financial module.

---

## TRS-004 — Financial/subledger reconciliation

**Status:** `VERIFY`

- [ ] Inventory-to-GL reconciliation is available if inventory posting is in scope.
- [ ] AP-to-GL reconciliation is available.
- [ ] AR-to-GL reconciliation is available.
- [ ] Bank/Treasury-to-GL reconciliation is available.
- [ ] Differences identify source documents.
- [ ] Reports are tenant-scoped.
- [ ] Closed-period reports are reproducible.
- [ ] Tests verify balanced and intentionally mismatched examples.

---

# 13. P1 — Finance / Master Data / Governance Verification

## FIN-001 — Journal dimensions

**Status:** `VERIFY`

- [ ] Required dimensions are persisted on journal lines.
- [ ] Dimension requirements are validated.
- [ ] Reports can filter/group by dimensions.
- [ ] Reversal preserves dimensions.
- [ ] Tests exist.

---

## FIN-002 — Manual journal approval

**Status:** `VERIFY`

- [ ] Draft manual journal cannot post before required approval.
- [ ] Maker/checker rule is enforced.
- [ ] Direct API posting cannot bypass approval.
- [ ] Rejection reason is stored.
- [ ] Audit contains maker, approver, poster.
- [ ] Tests exist.

---

## FIN-003 — Realized / unrealized FX

**Status:** `VERIFY`

- [ ] Exchange-rate source and effective date are explicit.
- [ ] Unrealized revaluation creates balanced journal entries.
- [ ] Revaluation is period-scoped and replay-safe.
- [ ] Reversal/next-period handling is defined.
- [ ] Realized gain/loss on settlement is correct.
- [ ] Tests cover gain and loss scenarios.

---

## FIN-004 — Core financial statements/APIs

**Status:** `VERIFY`

Required according to current roadmap scope:

- [ ] Trial Balance
- [ ] General Ledger Detail
- [ ] Balance Sheet

Acceptance:

- [ ] Derived from posted journals only.
- [ ] Date/fiscal-period filters are correct.
- [ ] Debit/credit sign treatment is consistent.
- [ ] Tenant isolation is enforced.
- [ ] Export matches screen/API totals.
- [ ] Tests use known journal fixtures with expected totals.

---

## FIN-005 — Effective-dated master data

**Status:** `VERIFY`

- [ ] Changes that affect historical financial calculations are effective-dated where required.
- [ ] Historical documents resolve the historically applicable value.
- [ ] New documents resolve the current effective value.
- [ ] Overlapping invalid effective ranges are rejected.
- [ ] Tests exist.

---

## FIN-006 — Bank-change governance

**Status:** `VERIFY`

- [ ] Sensitive bank-account changes require appropriate permission.
- [ ] Before/after values are audited safely.
- [ ] Approval is required if current business rules demand maker/checker.
- [ ] Previously approved payments cannot silently switch beneficiary bank details.
- [ ] Tests cover direct API bypass attempts.

---

# 14. Cross-Cutting Acceptance Rules for Every Financial/Stock Feature

Every applicable remaining item must also satisfy these controls.

## Security

- [ ] Backend authorization exists.
- [ ] Frontend guard/menu visibility is consistent but not relied on for security.
- [ ] `SUPER_ADMIN`/domain roles follow the project authorization model.
- [ ] Direct unauthorized API call returns `403`.
- [ ] Stable error code is returned.

## Multi-tenancy

- [ ] Tenant A cannot read Tenant B data.
- [ ] Tenant A cannot mutate Tenant B data.
- [ ] IDs supplied in a request are revalidated under the active tenant.
- [ ] Background/scheduled jobs preserve tenant context correctly.

## Idempotency

For commands with financial/stock effect:

- [ ] `operationId` or equivalent is required where appropriate.
- [ ] Same command replay returns the existing result or a safe deterministic response.
- [ ] Replay does not duplicate:
  - [ ] Stock
  - [ ] Money
  - [ ] Journal entries
  - [ ] Ledger entries
  - [ ] Notifications with financial meaning
  - [ ] Approval decisions

## Concurrency

- [ ] Concurrent stock reservation cannot oversell.
- [ ] Concurrent payment cannot overpay.
- [ ] Concurrent posting cannot duplicate journals.
- [ ] Concurrent approval cannot corrupt state.
- [ ] Tests use PostgreSQL/Testcontainers for database-lock behavior where H2 is not sufficient.

## Audit

- [ ] Sensitive command is audited.
- [ ] Actor is correct.
- [ ] Timestamp is immutable.
- [ ] Source document is included.
- [ ] Before/after or action context is sufficient.
- [ ] Passwords/tokens/secrets are never logged.

## Error handling

- [ ] Stable backend error code.
- [ ] Arabic/English translation exists.
- [ ] UI shows a useful user message.
- [ ] No raw stack trace is exposed to the user.

---

# 15. Do Not Rebuild Already-Delivered Work

The following areas were addressed in the latest remediation commits and should **not** be recreated unless a regression is demonstrated:

- Workforce dispatch REST wiring
- Workforce dispute REST wiring
- `/workforce/dispatch-disputes` frontend workbench
- Workforce permissions for dispatch/disputes
- Java 21 Gradle toolchain alignment
- Java 21 container/runtime alignment
- Non-blocking asynchronous Logback wrapper

For these areas:

- [ ] Keep existing implementation.
- [ ] Add missing characterization/E2E tests if needed.
- [ ] Only reopen implementation when a failing acceptance test proves a real defect.

---

# 16. Recommended Execution Order

Do not work on everything at once.

## Milestone 1 — Correct the truth

- [ ] DOC-001 — Fix source-of-truth status.
- [ ] Record current baseline SHA.
- [ ] Create a short table of actual OPEN vs VERIFY items.

**Milestone success:** Documentation no longer claims “all done” while open acceptance items remain.

---

## Milestone 2 — Payroll integrity

- [ ] PAY-001 — Snapshot-only calculation.
- [ ] PAY-002 — Configurable/effective calculation constants.
- [ ] Payroll automated tests.
- [ ] Payroll E2E acceptance.

**Milestone success:** An old frozen payroll run cannot change when employee salary, attendance, or payroll policy is edited later.

---

## Milestone 3 — Manufacturing integrity

- [ ] MFG-001 — Frozen BOM execution source.
- [ ] Snapshot/BOM mutation test.
- [ ] Verify material reservation/partial receipt/WIP/variance only after frozen-source fix.

**Milestone success:** Editing the master BOM after order start cannot alter the active production order's material/cost basis.

---

## Milestone 4 — Financial vertical slices

- [ ] P2P-001 — Proposal to real payment.
- [ ] O2C-001 — Order to cash and return.
- [ ] INV-001 — Verify inventory capability matrix.
- [ ] SHARED-001..004 — Shared guards/posting.
- [ ] TRS-001..004 — Treasury/close.
- [ ] FIN-001..006 — Finance/governance.

**Milestone success:** Each module has source + API + UI + automated E2E evidence, not only isolated primitives.

---

## Milestone 5 — Release gate

- [ ] REL-001 — CI actually executes and is green.
- [ ] REL-002 — Refresh full evidence on final SHA.
- [ ] Production-like smoke test.
- [ ] Update PROJECT_MAP only after evidence is complete.

**Milestone success:** One exact SHA has reproducible green backend, frontend, PostgreSQL, build, Compose, and critical-flow evidence.

---

# 17. Final “ALL DONE” Gate

The developer may write **ALL DONE / FULLY VERIFIED / RELEASE READY** only when every checkbox below is true:

- [ ] DOC-001 complete.
- [ ] PAY-001 complete.
- [ ] PAY-002 complete.
- [ ] MFG-001 complete.
- [ ] P2P-001 either VERIFIED complete or explicitly approved N/A.
- [ ] O2C-001 either VERIFIED complete or explicitly approved N/A.
- [ ] INV-001 applicable sub-items verified.
- [ ] Shared control items verified.
- [ ] Treasury/close applicable items verified.
- [ ] Finance/governance applicable items verified.
- [ ] Backend unit/service tests green.
- [ ] Backend PostgreSQL/Testcontainers tests green.
- [ ] Liquibase fresh migration green.
- [ ] Liquibase upgrade-path verification green when required.
- [ ] Error-code validation green.
- [ ] Translation-catalog validation green.
- [ ] Authorization-contract validation green.
- [ ] Frontend i18n validation green.
- [ ] Frontend hardcoded-string validation green.
- [ ] Frontend tests green.
- [ ] Frontend production build green.
- [ ] Production-like Compose validation green.
- [ ] Critical business-flow smoke tests green.
- [ ] GitHub/approved CI is green on the exact release SHA.
- [ ] `docs/TEST_EVIDENCE.md` contains the exact SHA and observed results.
- [ ] `PROJECT_MAP.md` has no contradictory open-work statement.
- [ ] No known P0 blocker remains.

Only after all applicable items above are `[x]` may this line be changed:

```text
FINAL STATUS: VERIFIED COMPLETE
```

Until then, keep:

```text
FINAL STATUS: NOT YET RELEASE-VERIFIED
```

---

# 18. Developer Completion Report Template

When you believe the checklist is complete, provide this report to the Tech Lead:

```text
Branch:
Final SHA:
Date:

P0 completed:
- DOC-001:
- PAY-001:
- PAY-002:
- REL-001:
- REL-002:

P1 completed/verified:
- MFG-001:
- P2P-001:
- O2C-001:
- INV-001:
- SHARED:
- TREASURY/CLOSE:
- FINANCE:

Backend:
- Unit/service tests:
- PostgreSQL/Testcontainers:
- Liquibase fresh DB:
- Liquibase upgrade path:
- Error-code check:
- Translation-catalog check:
- Authorization-contract check:

Frontend:
- npm ci:
- check:i18n:
- check:hardcoded:
- tests:
- production build:

CI:
- Run URL:
- Backend job:
- Frontend job:
- Compose/image job:

Manual acceptance:
- Payroll frozen-run scenario:
- Manufacturing frozen-BOM scenario:
- P2P payment scenario:
- O2C scenario:
- Inventory scenario:

Known issues remaining:
- NONE / list them honestly

I confirm I have not marked any item DONE based only on documentation,
class existence, compilation, or an unexecuted CI job.
```

---

# 19. Audit Evidence / Reference Points

This checklist was prepared from source review of branch `fm_bemo_consolidated` with reviewed HEAD `a9430d3`.

Important reference points:

- Repository:
  - `https://github.com/Mohamed-Hammada/bortqala/tree/fm_bemo_consolidated`
- Reviewed HEAD:
  - `https://github.com/Mohamed-Hammada/bortqala/commit/a9430d34fa6f52bd9968ced3e6baa3d718cfc3c8`
- Remediation commit:
  - `https://github.com/Mohamed-Hammada/bortqala/commit/50ab08034483c7d9d29648b00255ace51030f9d1`
- Project map:
  - `https://github.com/Mohamed-Hammada/bortqala/blob/fm_bemo_consolidated/PROJECT_MAP.md`
- Payroll service:
  - `https://github.com/Mohamed-Hammada/bortqala/blob/fm_bemo_consolidated/be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java`
- Manufacturing service:
  - `https://github.com/Mohamed-Hammada/bortqala/blob/fm_bemo_consolidated/be/src/main/java/com/bemo/hr/manufacturing/production/application/ManufacturingService.java`
- Test evidence:
  - `https://github.com/Mohamed-Hammada/bortqala/blob/fm_bemo_consolidated/docs/TEST_EVIDENCE.md`
- CI run for reviewed HEAD:
  - `https://github.com/Mohamed-Hammada/bortqala/actions/runs/31607526971`

---

# 20. Tech Lead Note

The purpose of this checklist is **not** to create more architecture.

Use the smallest correct change.

Do not:

- split the modular monolith into microservices,
- introduce Kafka for these fixes,
- create a second inventory system,
- create a generic workflow/rules engine where the existing domain model is sufficient,
- rewrite already-working features,
- mark roadmap items complete based on names/files alone.

The goal is to close the remaining **integration and verification seams** with reproducible evidence.
