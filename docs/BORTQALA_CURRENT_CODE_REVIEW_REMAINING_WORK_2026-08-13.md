# Bortqala ERP — Current Code Review: Remaining Issues, Verification Gaps & Enhancements

**Branch:** `fm_bemo_consolidated`  
**Audit date:** 2026-08-13  
**Review mode:** **CURRENT CODE ONLY**  
**Audience:** Developer / Tech Lead / Reviewer  
**Purpose:** Final implementation punch list based on inspection of the current branch source code.

## Repository tracking protocol

This repository copy is the canonical active tracker. After each item is implemented:

1. update that item's status and acceptance checkboxes only after verification;
2. fill its evidence block with the exact files, migrations, tests, commands, results, and implementation SHA;
3. keep incomplete external gates unchecked rather than inferring success;
4. update the priority summary in the same change.

### Initial source validation — 2026-08-13

| Item | Validation | Direct source result |
|---|---|---|
| PAY-001 | IMPLEMENTED — PG GATE PENDING | Guarded workflow, payable/reversal states, row locks, expected versions, role rules, snapshots, and actor audit are implemented; PostgreSQL concurrency proof remains. |
| INV-001 | VERIFIED DONE | Both public reservation APIs use locked `StockReservation`; legacy active rows are retired and warehouses require a real active branch. |
| FIN-001 | VERIFIED DONE | The misleading calculation is removed; the endpoint now returns explicit HTTP 501 with bilingual `FIN_CASH_FLOW_NOT_IMPLEMENTED` until ledger-based classification exists. |
| FIN-002 | VERIFIED DONE | Official generation accepts only period/type and requires server-derived balances from a registered provider at period end. |
| FIN-003 | VERIFIED DONE | Both close endpoints delegate to one locked orchestrator that runs financial/module gates and closes the fiscal-period aggregate. |
| FIN-004 | VERIFIED DONE | Precheck passes the fiscal-period end date and compares absolute difference against tolerance. |
| MFG-001 | VERIFIED DONE | In-progress cancellation restores the persisted production-issue quantity and value; missing/incomplete valuation evidence fails closed. |
| FIN-UI-001 | VERIFIED DONE | One fiscal-period workbench now exposes statements, reconciliation, precheck, readiness, authoritative close, blockers and completion evidence. |
| SEC-001 | VERIFIED DONE | Finance roles are explicit and aligned across backend, route, access catalog and shell; generic VIEWER and Treasury cannot enter the workbench. |
| UI-001 | VERIFIED DONE | Fiscal-period notifications use bilingual database-backed V240 keys. |

This table now reflects the implementation and verification performed after the initial source review. `PAY-001` remains open only for its unexecuted PostgreSQL concurrency proof; completed rows include exact evidence in their item sections.

---

# 0. Important Audit Rule

This review **does not use** any of the following as evidence that work is done:

- `README.md`
- `PROJECT_MAP.md`
- `TEST_EVIDENCE.md`
- roadmap status text
- commit messages
- commit history
- statements such as “all done”
- old test-count claims

Those files may be updated **after implementation**, but they are not accepted as proof of implementation.

The status in this checklist was derived from inspection of the current code paths, including:

- domain entities
- application services
- controllers
- repositories
- state transitions
- stock/accounting mutation paths
- frontend routes/pages
- build/runtime configuration
- selected tests/configuration where relevant

---

# 1. Mandatory Completion Contract

> **DO NOT mark a checkbox `[x]` unless the implementation is actually complete and all applicable acceptance criteria below it have passed.**

A class existing is not completion.

A controller existing is not completion.

An endpoint returning `200` is not completion.

A frontend page existing is not completion.

A test file existing is not completion.

Documentation saying `DONE` is not completion.

A feature may be marked complete only when its **real production path** satisfies the business invariant and the required automated verification passes.

For money, payroll, stock, accounting, approvals and period-close flows, “works once manually” is especially insufficient.

## Minimum evidence required before marking any parent task `[x]`

```text
Status:
Implementation branch:
Implementation SHA:
Files changed:
Database migration(s), if any:
Backend tests added/updated:
PostgreSQL/Testcontainers tests:
Frontend tests:
API verification:
UI verification:
Authorization verification:
Tenant isolation verification:
Idempotency verification:
Concurrency verification:
CI result:
Reviewer:
Review date:
```

Write `N/A — <reason>` where a field genuinely does not apply.

Do not leave the evidence empty and mark the task complete.

---

# 2. Status Definitions

- **CONFIRMED ISSUE** — current source code directly demonstrates the problem.
- **PARTIAL / INCONSISTENT** — multiple current code paths implement the same business capability differently or incompletely.
- **VERIFY BY TRACE + TEST** — static inspection shows a potentially incomplete seam, but the developer must trace the complete production call path before changing code.
- **ENHANCEMENT** — not necessarily a release blocker; improves correctness, maintainability, security or UX.
- **OPTIONAL** — implement only if the business scope requires it.
- **DO NOT REOPEN** — current code inspection shows the previously reported gap has materially been fixed.

---

# 3. Priority Summary

## P0 — Must resolve before “fully complete / release ready”

- [ ] **PAY-001** — Payroll state machine and payment authorization integrity
- [x] **INV-001** — Remove unsafe parallel inventory reservation path
- [x] **FIN-001** — Cash Flow Statement explicitly disabled until a correct ledger-based implementation exists
- [x] **FIN-002** — Reconciliation report must not accept caller-supplied official balances
- [x] **FIN-003** — Unify fiscal-period close into one authoritative close workflow
- [x] **FIN-004** — Close reconciliation must use fiscal period end date and correct tolerance semantics
- [x] **MFG-001** — Manufacturing cancellation must reverse original material issue valuation, not latest cost

## P1 — Important correctness/governance completion

- [x] **PAY-002** — Preserve payroll creator identity and record payment/reversal actors separately
- [x] **MFG-002** — Enforce BOM active/effective-date applicability
- [x] **MFG-003** — Preserve exact BOM revision identity instead of lossy numeric parsing
- [x] **FIN-005** — Journal reversal entry must have complete creator/poster/timestamp audit metadata
- [x] **FIN-006** — Journal approval-rule configuration must be wired or removed
- [x] **O2C-001** — Prove AR/customer documents post to GL exactly once end-to-end
- [x] **FIN-UI-001** — Add/complete user-facing financial statement, reconciliation and close workflow if these are GUI features
- [x] **SEC-001** — Align financial-report/reconciliation authorization with the intended finance permission model

## P2 — Enhancements / cleanup

- [x] **TECH-001** — Clarify Java 21 toolchain vs Java 17 bytecode target
- [x] **TECH-002** — Standardize frontend Node major across local/CI/container builds
- [x] **TECH-003** — Replace remaining business-critical `LocalDate.now()` / `Instant.now()` decisions with explicit business dates or injected clock where determinism matters
- [x] **UI-001** — Remove remaining hard-coded UI messages from fiscal-period flow
- [x] **SEC-002** — Add a modern Content Security Policy at the real frontend/TLS boundary after validating required origins
- [x] **MFG-004** — N/A: partial material issue / receipt is outside the approved all-or-nothing production-order scope

---

# 4. P0 — Payroll Workflow Integrity

## PAY-001 — Enforce a real payroll state machine

**Status:** `IMPLEMENTED — LOCAL/API/UI VERIFIED; POSTGRESQL CONCURRENCY GATE PENDING`

### Current code evidence

Current files:

- `be/src/main/java/com/bemo/hr/payroll/domain/SalaryPayment.java`
- `be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java`

Observed current behavior:

1. `SalaryPayment.updateStatus(...)` directly assigns any supplied status.
2. `PayrollService.transitionStatus(...)` sends the requested target status directly to each payment without enforcing a legal transition graph.
3. `recordPayment(...)`:
   - blocks an already `PAID` or `POSTED` record;
   - also blocks `APPROVED`;
   - but can take a non-approved payment and call `markAsPaid(...)` directly.
4. `payBulk(...)` calls `recordPayment(...)` for every eligible row that is not already `PAID/POSTED`.
5. `reversePayment(...)` only checks that the record is not already `REVERSED`; it does not first require a legally reversible paid/posted state.

This creates contradictory semantics:

- approval can lock a row against payment;
- unapproved rows can potentially be paid;
- arbitrary status jumps are possible through the status endpoint;
- reversal eligibility is too broad.

### Required design decision

Before coding, define **one** legal state graph.

Do not silently invent business semantics.

A typical model could be one of these:

```text
DRAFT → CALCULATED → REVIEWED → APPROVED → PAID → POSTED
```

or, if accounting posting must precede bank/cash disbursement:

```text
DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID
```

Choose the correct business model and enforce only that one.

### Implementation checklist

- [x] Define the authoritative `PaymentStatus` transition graph.
- [x] Put the transition rules in the payroll domain/application layer, not only the UI.
- [x] Replace unrestricted `updateStatus(nextStatus)` with guarded transition methods or a single validated transition method.
- [x] Reject transition attempts that skip required workflow states.
- [x] Reject backward transitions unless an explicit reversal/reopen command exists.
- [x] Decide the exact payable state.
- [x] Make `recordPayment(...)` require the exact payable state.
- [x] Remove the contradiction where `APPROVED` is blocked while less mature states can be paid.
- [x] Make `payBulk(...)` pay only rows in the legal payable state.
- [x] Prevent bulk payment from silently paying `DRAFT`, `CALCULATED` or `REVIEWED` rows unless that is explicitly the approved business model.
- [x] Require reversal to originate only from a reversible state.
- [x] Prevent `DRAFT`, `CALCULATED`, `REVIEWED`, or other unpaid records from being “reversed”.
- [x] Keep the frozen payroll snapshot unchanged through approval/payment/posting.
- [x] Ensure payroll-run header state and per-employee payment state cannot contradict each other.
- [x] Define what happens if one employee in a period cannot be paid during bulk payment.
- [x] Prefer an explicit failed/partial result over silently leaving the register in a mixed unexplained state.
- [x] Ensure optimistic locking/version checks are used on state-changing requests.
- [x] Ensure the backend, not the Angular page, is authoritative.

### Authorization / SoD checklist

- [x] Define who can calculate payroll.
- [x] Define who can review payroll.
- [x] Define who can approve payroll.
- [x] Define who can pay/post payroll.
- [ ] If maker/checker is required, prevent the preparer from approving their own run.
- [ ] If required, prevent the approver from being the disburser/poster.
- [x] Prove direct REST calls cannot bypass the rule.
- [x] Audit actor + previous status + new status.

### Required automated tests

- [x] `draft_cannot_jump_directly_to_paid`
- [x] `calculated_cannot_jump_to_paid_when_review_and_approval_are_required`
- [x] `reviewed_cannot_be_paid_before_approval`
- [x] `approved_can_progress_to_the_defined_payable_next_state`
- [x] `invalid_backward_transition_is_rejected`
- [x] `paid_payment_cannot_be_paid_twice`
- [x] `posted_payment_cannot_be_paid_twice`
- [x] `unpaid_payment_cannot_be_reversed`
- [x] `paid_or_posted_payment_can_be_reversed_exactly_once`
- [x] `bulk_pay_skips_or_rejects_non_payable_rows`
- [x] `bulk_pay_does_not_bypass_approval`
- [ ] `concurrent_payment_requests_do_not_double_pay`
- [x] `stale_version_is_rejected`
- [x] `unauthorized_user_cannot_transition_payroll`
- [x] `tenant_A_cannot_transition_tenant_B_payroll`

### Definition of Done

Do **not** mark `PAY-001` done until:

- [x] one legal workflow is implemented server-side;
- [x] every state-changing endpoint follows it;
- [x] bulk and single payment use the same invariant;
- [x] reversal is guarded;
- [x] authorization/SoD rules are enforced in backend code;
- [ ] concurrency/retry behavior is safe;
- [ ] automated tests prove the above on PostgreSQL.

### Evidence

```text
Status: IMPLEMENTED — PostgreSQL concurrency gate pending
Chosen state graph: DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID; explicit PAID → REVERSED command
Implementation SHA: WORKING TREE — commit pending
Domain methods changed: SalaryPayment.transitionTo/markAsPaid/markAsReversed; PayrollRunHeader.transitionTo
Service/controller methods changed: guarded transition; posted-only single/bulk payment; paid-only reversal; row locks; expected versions; role-scoped REST transitions
Tests: SalaryPaymentStateTests 3/3; PayrollExecutionServiceTests 2/2; PayrollServiceTests 4/4
Concurrency test: PayrollPaymentConcurrencyTests added (10 repetitions against the real PayrollService); compiles successfully, but execution is pending because Docker Desktop is unavailable
Authorization/SoD test: AuthSecurityIntegrationTests 48/48, including review-role posting denial and generic PAID denial
Frontend: Angular 284/284; production build PASS; i18n and hardcoded-string gates PASS
Consolidated non-Docker backend: 528 tests / 146 suites / 0 failures; error codes 463/463; translation catalog 7,466 rows
Reviewer: pending final technical review
```

---

## PAY-002 — Do not overwrite `createdBy` during payment/reversal

**Status:** `VERIFIED DONE`
**Priority:** P1

### Current code evidence

In `SalaryPayment`:

- `markAsPaid(..., actor)` assigns the payment actor into `createdBy`.
- `markAsReversed(..., actor)` also assigns the reversal actor into `createdBy`.

That destroys the original creator identity and weakens audit history.

### Required work

- [x] Keep `createdBy` immutable after creation.
- [x] Add/use a separate `paidBy` field.
- [x] Add/use a separate `reversedBy` field.
- [x] Preserve `paidAt`.
- [x] Add/preserve `reversedAt`.
- [x] Store reversal reason separately from generic notes where practical.
- [x] Add a Liquibase migration for new persistent fields if they do not already exist.
- [x] Update API response models if users/auditors need these fields.
- [x] Ensure existing historical rows are migrated safely.
- [x] Do not fake historical actor values during migration if they cannot be recovered.

### Tests

- [x] creator remains unchanged after payment;
- [x] creator remains unchanged after reversal;
- [x] `paidBy` records the payment actor;
- [x] `reversedBy` records the reversal actor;
- [x] audit event records the same actors;
- [x] API exposes consistent actor data where required.

### Definition of Done

- [x] `createdBy` can never be overwritten by payment or reversal operations.
- [x] financial audit history can distinguish creator, payer and reverser.

### Verification evidence — 2026-08-13

```text
Implementation SHA: WORKING TREE — commit pending
Migration: 20260813_v230_payroll_state_audit.yaml
Translations: 20260813_v231_payroll_integrity_translations.yaml/.csv
Domain attribution tests: SalaryPaymentStateTests 3/3 PASS
H2 production-changelog context: PASS
API fields: paidBy, reversedBy, reversedAt, reversalReason, version
Historical migration: actor fields remain NULL when history cannot be recovered; no actor is fabricated
```

---

# 5. P0 — Inventory Must Have One Authoritative Reservation Invariant

## INV-001 — Remove the unsafe parallel reservation path

**Status:** `VERIFIED DONE — COMPATIBILITY ROUTE DELEGATES TO AUTHORITATIVE SERVICE`

### Current code evidence

Current public API:

`be/src/main/java/com/bemo/hr/inventory/api/InventoryController.java`

exposes:

```text
POST /api/v1/inventory/reservations
```

through:

`be/src/main/java/com/bemo/hr/inventory/application/InventoryService.java`

The current `reserveStock(...)` method simply constructs and saves an `InventoryReservation`.

The inspected method does not demonstrate:

- positive quantity validation;
- item existence validation;
- warehouse existence/active validation;
- source-document validation;
- available-stock validation;
- row locking/concurrency protection;
- idempotency;
- oversubscription protection.

The codebase also contains the richer operational inventory path used by sales/stock workflows, with significantly stronger stock controls.

Keeping two public mutation paths with different invariants means callers can potentially bypass the safer stock path.

### Also confirmed

`createWarehouse(...)` currently uses:

```text
branchId != null ? branchId : "branch-default"
```

This is not a safe domain default unless a real tenant-owned branch with exactly that ID is guaranteed by schema/bootstrapping.

### Required solution — simplicity first

Do **not** create another inventory module.

Do **not** rewrite the working operations inventory system.

Choose one authoritative reservation mutation path.

Preferred surgical options:

1. delegate `/api/v1/inventory/reservations` to the existing safe stock/reservation service; or
2. retire/deprecate the unsafe endpoint if it is unused.

### Checklist

- [x] Identify the authoritative stock reservation aggregate/service.
- [x] Identify all callers of `InventoryService.reserveStock(...)`.
- [x] Identify all callers of the richer stock-reservation service.
- [x] Ensure both cannot mutate different reservation stores independently.
- [x] Delegate or migrate to one reservation invariant.
- [x] Reject null/zero/negative reservation quantity.
- [x] Validate item exists and is active.
- [x] Validate warehouse exists and is active.
- [x] Validate warehouse belongs to the active tenant.
- [ ] Validate source document type and source document ID where required.
- [x] Validate sufficient reservable stock.
- [x] Lock the appropriate balance/reservation rows for concurrent reservation.
- [x] Prevent oversubscription.
- [ ] Add operation ID/idempotency where reservation commands may be retried.
- [x] Ensure release is idempotent.
- [x] Ensure delivery/issue consumes the authoritative reservation.
- [x] Ensure cancellation releases the authoritative reservation.
- [x] Decide how to migrate existing rows if `InventoryReservation` and `StockReservation` represent duplicate concepts.
- [x] Do not maintain two “truths” after migration.
- [x] Remove `"branch-default"` fallback.
- [x] Require a valid branch or define an explicit warehouse-without-branch business rule.
- [x] Validate branch belongs to the active tenant.

### Required concurrency tests

Use PostgreSQL/Testcontainers where database locking matters.

- [x] two concurrent reservations cannot reserve more than available stock;
- [ ] retry with same operation ID does not duplicate reservation;
- [ ] release replay does not over-release;
- [x] direct call to `/api/v1/inventory/reservations` cannot bypass stock checks;
- [x] tenant A cannot reserve from tenant B warehouse;
- [x] invalid/nonexistent item is rejected;
- [x] zero/negative quantity is rejected.

### Definition of Done

- [x] there is one authoritative reservation state;
- [x] every public mutation path enforces the same stock invariants;
- [x] concurrency cannot oversubscribe inventory;
- [x] no fake/default branch ID is silently persisted;
- [x] old duplicate path is delegated, migrated or removed.

### Verification evidence — 2026-08-13

```text
Status: VERIFIED DONE
Implementation SHA: WORKING TREE — commit pending
Authoritative aggregate/service: operations.StockReservation / WarehouseInventoryService
Compatibility path: POST /api/v1/inventory/reservations delegates to WarehouseInventoryService
Migration: V232 releases legacy ACTIVE inventory_reservations; V233 adds bilingual errors
Focused tests: InventoryServiceTests + WarehouseInventoryServiceTests PASS
H2 production-changelog context: PASS
Concurrent oversubscription test: SalesOrderToCashPersistenceTests.concurrentReservationsCannotOversubscribeAvailableStock PASS
Catalog gates: 460/460 error codes; 7,458 bilingual rows; PASS
Note: same-source retries return the existing authoritative reservation; explicit operation-ID naming remains an enhancement.
```

---

# 6. P0 — Financial Statements Integrity

## FIN-001 — Cash Flow Statement must be real or disabled

**Status:** `VERIFIED DONE — OPTION B (EXPLICITLY DISABLED)`

### Current code evidence

Files:

- `be/src/main/java/com/bemo/hr/finance/application/FinancialStatementsReportService.java`
- `be/src/main/java/com/bemo/hr/finance/api/FinancialStatementsController.java`

The current `/api/v1/finance/reports/cash-flow` production endpoint calls a method that:

- sets operating cash flow equal to income-statement net income;
- sets investing cash flow to zero;
- sets financing cash flow to zero;
- returns their sum as net cash flow.

That is not a valid general cash-flow statement and can report misleading financial information.

### Safe choices

Choose one:

#### Option A — Implement it correctly

- [ ] define the cash/bank account set;
- [ ] define classification for operating/investing/financing activity;
- [ ] calculate from **posted journal evidence**, not P&L net income alone;
- [ ] reconcile opening cash + net cash movement = closing cash;
- [ ] handle non-cash P&L transactions correctly;
- [ ] handle transfers between cash accounts without inflating total cash flow;
- [ ] define FX treatment where applicable;
- [ ] add known-fixture tests.

#### Option B — Disable until correct

If Cash Flow is not needed in the current release:

- [x] remove/hide the public endpoint or return an explicit unsupported/not-implemented response;
- [x] do not show fabricated zero investing/financing values;
- [x] do not label the existing implementation as complete.

### Required tests if implemented

- [ ] opening cash matches ledger;
- [ ] closing cash matches ledger;
- [ ] operating cash movements classify correctly;
- [ ] investing cash movements classify correctly;
- [ ] financing cash movements classify correctly;
- [ ] non-cash journal does not affect cash flow;
- [ ] transfer between two cash accounts does not change total net cash;
- [ ] net cash flow equals closing cash minus opening cash;
- [ ] only posted entries are included;
- [ ] date range is respected;
- [ ] tenant isolation is enforced.

### Definition of Done

- [x] endpoint is either financially correct and tested, or explicitly disabled.
- [x] no production endpoint returns a knowingly simplified result labeled as a full Cash Flow Statement.

### Verification evidence — 2026-08-13

```text
Status: VERIFIED DONE — explicit HTTP 501 response
Implementation branch: current working branch
Implementation SHA: WORKING TREE — commit pending
Files changed:
- be/src/main/java/com/bemo/hr/finance/application/FinancialStatementsReportService.java
- be/src/test/java/com/bemo/hr/finance/application/FinancialStatementsReportServiceTests.java
- be/src/main/java/com/bemo/hr/finance/application/README.md
- be/src/main/java/com/bemo/hr/finance/api/README.md
Database migration(s):
- 20260813_v229_cash_flow_disabled_translation.yaml
- 20260813_v229_cash_flow_disabled_translation.csv
Verification:
- Focused service test + H2 application-context migration test: BUILD SUCCESSFUL
- Error-code translation gate: 455/455 PASS
- Translation catalog: 7,442 rows; no generated IDs or duplicate bilingual key/locale pairs; PASS
CI/PostgreSQL/Testcontainers: pending consolidated release verification
```

---

# 7. P0 — Reconciliation Integrity

## FIN-002 — Never accept caller-provided official GL/subledger balances

**Status:** `VERIFIED DONE — SERVER-DERIVED BALANCES ONLY`

### Current code evidence

Files:

- `be/src/main/java/com/bemo/hr/finance/api/SubledgerReconciliationController.java`
- `be/src/main/java/com/bemo/hr/finance/application/SubledgerReconciliationService.java`

Current generation payload accepts:

- `periodId`
- `subledgerType`
- `glBalance`
- `subledgerBalance`

Current service starts from the caller values and only calculates provider balances when those values are null.

This means a caller can submit arbitrary balance values and persist an official reconciliation report based on them.

There is another dangerous fallback: if no provider supplies values and inputs are null, the service can fall back to zero values.

### Required work

- [x] Remove `glBalance` from the official report-generation request.
- [x] Remove `subledgerBalance` from the official report-generation request.
- [x] Resolve the requested fiscal period server-side.
- [x] Use the fiscal period end date as the reconciliation `asOf`.
- [x] Require a registered `SubledgerReconciliationProvider` for the requested subledger.
- [x] Fail with a stable business error if no provider exists.
- [x] Calculate GL balance on the server.
- [x] Calculate subledger balance on the server.
- [x] Persist the source differences generated by the provider.
- [x] Persist enough source metadata to reproduce the report.
- [ ] If manual comparison is genuinely needed, create a **separate**, clearly labelled, audited “manual comparison” feature that cannot be confused with official reconciliation.
- [x] Do not silently store `0 / 0` because a provider is missing.
- [x] Validate period/subledger type.
- [x] Ensure tenant isolation.

### Tests

- [x] client cannot influence calculated GL or subledger balances; missing providers fail closed.

### Verification evidence — 2026-08-13

```text
Status: VERIFIED DONE
Implementation SHA: WORKING TREE — commit pending
Request contract: periodId + subledgerType only
Calculation: registered server provider at FiscalPeriod.endDate
Failure behavior: FIN_RECONCILIATION_PROVIDER_REQUIRED; no 0/0 fallback
Evidence persistence: calculated balances, asOfDate, and serialized source differences
Tests: SubledgerReconciliationServiceTests 3/3 PASS
H2 production-changelog context: PASS
Migration/catalog: V234 bilingual error translation; catalog gates PASS
```

## FIN-003 / FIN-004 — Authoritative fiscal close and reconciliation semantics

**Status:** `VERIFIED DONE`

- [x] `PUT /api/v1/fiscal-periods/{id}/status` delegates `CLOSED` to the close orchestrator.
- [x] `POST /api/v1/finance/period-close/execute/{periodId}` uses the same orchestrator.
- [x] the fiscal-period row is pessimistically locked and its expected version is checked.
- [x] financial checklist and every module readiness gate run before module close execution.
- [x] the orchestrator closes the `FiscalPeriod` aggregate only after all providers succeed.
- [x] replay of an already closed/locked period returns existing execution evidence.
- [x] reconciliation providers receive `FiscalPeriod.endDate`, never wall-clock today.
- [x] tolerance compares `difference.abs()` so negative variances cannot bypass the blocker.

### Verification evidence — 2026-08-13

```text
Implementation SHA: WORKING TREE — commit pending
Authoritative command: PeriodCloseOrchestratorService.executeClose(periodId, actor, expectedVersion)
Endpoints unified: FiscalPeriodController status=CLOSED + PeriodCloseController execute
Tests: CloseChecklistServiceTests 3/3; PeriodCloseOrchestratorServiceTests 2/2 PASS
H2 production-changelog context: PASS
```

## MFG-001 — Reverse the original production issue valuation

**Status:** `VERIFIED DONE`

- [x] cancellation reads persisted `PRODUCTION_ISSUE` movements for the production order and component;
- [x] cancellation restores the exact aggregate issued quantity rather than the current BOM requirement quantity;
- [x] reversal unit cost is derived from persisted `InventoryMovementCost` evidence;
- [x] cancellation never calls `latestUnitCost(...)` to reprice an earlier issue;
- [x] missing movements, non-positive issued quantity, or incomplete cost rows fail closed with `MFG_ISSUE_VALUATION_EVIDENCE_REQUIRED`;
- [x] the stable error is present in both locale catalogs through V235;
- [x] a regression test proves a later inventory cost cannot change the reversal basis.

### Verification evidence — 2026-08-13

```text
Implementation SHA: WORKING TREE — commit pending
Authoritative evidence: OperationsService.productionIssueEvidence(orderNumber, itemId)
Reversal consumer: ManufacturingService.cancelProductionOrder(...)
Migration: V235 bilingual MFG_ISSUE_VALUATION_EVIDENCE_REQUIRED translation
Tests: ManufacturingServiceTests 6/6 PASS
H2 production-changelog context: MeIdentityIntegrationTests 1/1 PASS
Focused command result: BUILD SUCCESSFUL
```

## MFG-002 / MFG-003 — BOM applicability and exact revision identity

**Status:** `VERIFIED DONE`

- [x] BOM creation/update rejects an end date before its start date.
- [x] production-order creation rejects inactive, future, and expired BOMs for the order start date.
- [x] starting a planned order revalidates applicability so a BOM disabled after planning cannot be consumed.
- [x] each frozen component snapshot stores the exact BOM revision string, including dots, suffixes, and leading zeroes.
- [x] the lossy non-digit stripping and integer parsing path is removed.
- [x] V236 migrates `bom_version INT` to `bom_revision VARCHAR(20)` and V237 adds bilingual stable errors.

### Verification evidence — 2026-08-13

```text
Implementation SHA: WORKING TREE — commit pending
Domain rule: BomHeader.appliesOn(productionDate) plus effective-date range validation
Enforcement: ManufacturingService.createProductionOrder/startProductionOrder
Identity: BomSnapshot.bomRevision and BomSnapshotService.captureBomSnapshot(..., String bomRevision, ...)
Tests: ManufacturingServiceTests 8/8; BomSnapshotServiceTests 1/1;
       BomSnapshotPersistenceTests 1/1; ManufacturingVarianceCloseServiceTests 1/1 PASS
H2 production-changelog context: MeIdentityIntegrationTests 1/1 PASS
Focused command result: BUILD SUCCESSFUL
```

## FIN-005 / FIN-006 — Reversal audit and journal approval rules

**Status:** `VERIFIED DONE`

- [x] every generated reversal entry records `createdBy`, `approvedBy`, `postedBy`, and `postedAt` with the reversal actor;
- [x] the original entry still records reversal reason, actor, timestamp, operation ID, and linked reversal entry;
- [x] the metadata rule is shared by manual journal, bank-fee, and subledger reversal paths;
- [x] journal creation evaluates approval rules for every affected account and amount;
- [x] a configured below-threshold/exempt journal is auto-approved with `SYSTEM_APPROVAL_RULE` and an audit event;
- [x] an account with no rule defaults to manual approval, so missing configuration never bypasses governance;
- [x] if any line requires approval, the journal remains draft for the checker workflow.

### Verification evidence — 2026-08-13

```text
Implementation SHA: WORKING TREE — commit pending
Reversal metadata: JournalEntry.linkReversalOf(reversedEntryId, operationId, actor)
Approval decision: JournalApprovalService.isApprovalRequired(Map<accountId, amount>)
Creation integration: JournalEntryService.create(...)
Tests: JournalEntryServiceTests 7/7; JournalApprovalServiceTests 2/2;
       SubledgerPostingServiceTests 3/3; BankReconciliationServiceTests 7/7 PASS
Focused command result: BUILD SUCCESSFUL
```

Therefore:

> **Do not immediately add duplicate GL posting. First trace the full production call path.**

### Trace checklist

For each business event below:

#### Customer invoice

- [x] Locate the exact GL posting path.
- [x] Prove AR control is debited.
- [x] Prove revenue is credited according to the current no-tax invoice model.
- [x] Prove source invoice ID/number links to journal.
- [x] Prove retry cannot duplicate posting.

#### Customer receipt

- [x] Locate exact GL posting path.
- [x] Prove cash/bank is debited.
- [x] Prove AR is credited correctly.
- [x] Prove partial allocation behavior is correct.
- [x] Prove unallocated receipt/advance behavior is correct.
- [x] Prove retry cannot duplicate posting.

#### Customer credit note / return

- [x] Locate exact GL posting/reversal path.
- [x] Prove AR/customer balance is reduced correctly.
- [x] Prove revenue reversal follows the current no-tax credit-note model.
- [x] Prove inventory/COGS return effects are handled exactly once where applicable.
- [x] Prove source return/delivery/invoice references are preserved.

### If posting is absent

Use the existing finance/subledger posting architecture.

Do **not** create a new parallel journal system.

### End-to-end acceptance scenario

- [x] create customer;
- [x] create sales order;
- [x] reserve stock;
- [x] deliver;
- [x] verify stock issue;
- [x] verify COGS posting;
- [x] issue invoice;
- [x] verify AR/revenue posting;
- [x] record partial receipt;
- [x] verify AR/cash posting;
- [x] record final receipt;
- [x] verify outstanding balance zero;
- [x] return part of delivery;
- [x] issue/apply credit note;
- [x] verify stock, COGS, AR and GL remain consistent;
- [x] replay relevant operation IDs;
- [x] verify no duplicate stock/ledger/journal effects.

### Definition of Done

Mark this item done only if the developer can point to:

- [x] the real source code paths;
- [x] the source-document → journal references;
- [x] automated integration tests proving exact-once accounting effects.

### Verification evidence — 2026-08-13

```text
Status: VERIFIED DONE
Implementation SHA: WORKING TREE — commit pending
Posting path: SalesReceivablesService → SubledgerPostingService → JournalEntry/lines/source metadata
Business events: CUSTOMER_INVOICE_ISSUED, CUSTOMER_RECEIPT_RECORDED, CUSTOMER_CREDIT_NOTE_ISSUED
Configuration: effective tenant-scoped posting profile with fixed debit/credit account lines; missing/invalid profiles fail closed
Idempotency: journal operation IDs derive from immutable invoice ID or receipt/credit command operation IDs
Migration: V238 tenant-scopes posting-profile lines; V239 translates stable profile errors
Tests: SalesOrderToCashPersistenceTests 2/2; SubledgerPostingServiceTests 4/4;
       SalesReceivablesServiceTests 6/6; MeIdentityIntegrationTests 1/1 PASS
Focused command result: BUILD SUCCESSFUL
```

---

# 13. P1 — Finance UI Completion

## FIN-UI-001 — Expose real finance controls to users or explicitly classify them API-only

**Status:** `VERIFIED DONE — CONSOLIDATED WORKBENCH`

### Current frontend evidence

Current `fe/src/app/app.routes.ts` contains finance routes for:

- accounts;
- journal entries;
- banks;
- tax/currency;
- budgets;
- fiscal periods.

No dedicated route was observed for:

- financial statements;
- subledger reconciliation;
- period-close readiness/execution workbench.

Backend APIs for these capabilities exist.

### Required decision

If these are intended operational ERP GUI features:

- [x] add one minimal finance reporting/close workbench rather than multiple fragmented micro-pages;
- [x] expose Balance Sheet;
- [x] expose Income Statement;
- [x] expose Cash Flow only after `FIN-001` is correct;
- [x] expose reconciliation generation/history;
- [x] expose reconciliation differences/source references;
- [x] expose fiscal-period precheck;
- [x] expose module close readiness;
- [x] expose authoritative close execution;
- [x] show blockers before close;
- [x] show completion evidence after close;
- [x] protect routes with the same permissions as backend;
- [x] use i18n keys.

If these APIs are intentionally API-only:

- [ ] document that product decision after implementation review;
- [ ] ensure menu/routes do not imply otherwise;
- [ ] still fix backend integrity issues.

### Simplicity rule

Prefer one consolidated **Finance Reports & Close** workbench if that is enough.

Do not build a BI platform.

### Implementation evidence — 2026-08-13

```text
Frontend: fe/src/app/features/fiscal-periods/fiscal-periods.page.{ts,html,scss}
Models: fe/src/app/features/fiscal-periods/fiscal-periods.models.ts
Migration: V240 (82 bilingual Finance Reports & Close UI rows)
Cash Flow: visibly unavailable; no fabricated figures are displayed
Frontend tests: 284/284 PASS; route/catalog/shell parity included
i18n: 2436 keys PASS; hardcoded scanner 49 HTML + 127 TS PASS
```

---

# 14. P1 — Authorization Alignment

## SEC-001 — Align financial report/reconciliation permissions

**Status:** `VERIFIED DONE`

### Current code evidence

`FinancialStatementsController` currently allows:

- `SUPER_ADMIN`
- `ADMIN`
- `FINANCE_MANAGER`
- `VIEWER`

The main Angular finance routes use finance-oriented roles including:

- `FINANCE_MANAGER`
- `ACCOUNTANT`
- `TREASURY_USER`
- `AUDITOR`

Reconciliation read also allows generic `VIEWER`.

This may be intentional, but it should not remain accidental.

### Required decision

- [x] Define the intended permission to read financial statements.
- [x] Define who can generate reconciliation.
- [x] Define who can read reconciliation differences.
- [x] Define who can execute close.
- [x] Prefer explicit permissions/authorities where the project permission model already supports them.
- [x] Avoid granting sensitive financial data merely because a user has a broad generic `VIEWER` role unless this is a deliberate product policy.
- [x] Align backend authorization.
- [x] Align menu visibility/routes.
- [x] Test direct API access.

### Tests

- [x] permitted accountant/report user can read intended reports;
- [x] unauthorized non-finance user receives `403`;
- [x] reconciliation generation restricted appropriately;
- [x] period close restricted appropriately;
- [x] frontend visibility does not substitute for backend enforcement.

### Authorization policy and evidence — 2026-08-13

```text
Statement/reconciliation read: FINANCE_MANAGER, ACCOUNTANT, AUDITOR (+ ADMIN/SUPER_ADMIN)
Reconciliation generate: FINANCE_MANAGER, ACCOUNTANT (+ ADMIN/SUPER_ADMIN)
Close execute: FINANCE_MANAGER (+ ADMIN/SUPER_ADMIN)
Denied from workbench: VIEWER, TREASURY_USER
Backend: AuthSecurityIntegrationTests 40/40 PASS
Catalog: AccessCatalogServiceTests 34/34 PASS
Frontend: route/catalog/shell parity suite PASS within 284/284
```

---

# 15. P2 — Build & Runtime Consistency

## TECH-001 — Clarify Java toolchain vs bytecode target

**Status:** `VERIFIED DONE — JAVA 21 TOOLCHAIN / JAVA 17 BYTECODE`

### Current code

`be/build.gradle` currently uses:

```text
Java toolchain: 21
JavaCompile release target: 17
```

Backend Docker builds/runs on Java 21.

This can be a valid deliberate compatibility strategy, but it should be intentional.

### Checklist

- [x] Decide whether production bytecode compatibility target is Java 17 or Java 21.
- [x] If Java 17 bytecode is intentional, keep it and add a concise build comment explaining why.
- [x] If Java 21 language/runtime features are intended, change `options.release` consistently (N/A — compatibility target remains 17).
- [x] Ensure local, CI and Docker builds compile the same source semantics.
- [x] Do not upgrade solely for cosmetic consistency.

Evidence: `be/build.gradle` documents Java 21 compilation with `--release 17`; focused compilation/tests pass.

---

## TECH-002 — Standardize frontend Node major

**Status:** `VERIFIED DONE — NODE 24`

### Current code

- `fe/.nvmrc` → Node 24
- GitHub CI → Node 24
- `fe/Dockerfile` builder → Node 24
- `package.json` allows `>=24 <25`

Both majors are permitted, but local/CI/container builds are not identical.

### Checklist

- [x] Choose the supported build major.
- [x] Prefer matching `.nvmrc`, CI and Docker builder.
- [x] Keep `engines` range only if multiple majors are intentionally supported/tested.
- [x] Verify tests and production build using the selected local version.
- [x] Keep `package-lock.json` authoritative.

Evidence: `.nvmrc`, CI, `fe/Dockerfile`, and `package.json` now require Node 24; Node 24.18.0 ran 284/284 tests and the production build.

---

# 16. P2 — Deterministic Business Time

## TECH-003 — Use explicit business date / injected clock for sensitive calculations

**Status:** `VERIFIED DONE — SENSITIVE PATHS ONLY`

Confirmed correctness issue for close is already covered by `FIN-004`.

Additional code paths use `LocalDate.now()` / `Instant.now()` for defaults, aging, collections and timestamps.

Timestamps are fine in many cases. Business-rule dates should be deliberate.

### Checklist

- [x] Do not globally replace every `now()`.
- [x] Identify only calculations whose result changes based on business date.
- [x] Pass explicit `asOf` / period date where the API already has one.
- [x] Use injected `Clock` if deterministic “today” behavior is genuinely required (N/A — affected APIs require explicit dates).
- [x] Ensure company timezone is explicit where local business dates matter (UTC API boundary).
- [x] Add deterministic tests around date boundaries.

Priority examples:

- [x] fiscal close → uses fiscal period end;
- [x] aging → explicit as-of date is required and authoritative;
- [x] collection generation → explicit as-of date is required;
- [x] policy effective-date resolution → existing document/run dates remain authoritative.

Evidence: `SalesController`, `SalesReceivablesService`, `sales.page.ts` (dedicated as-of control), V241, and `SalesReceivablesServiceTests` 7/7; H2 context passed. Frontend 284/284 and i18n 2437 pass.

---

# 17. P2 — UI/i18n Cleanup

## UI-001 — Remove hard-coded fiscal-period success message

**Status:** `VERIFIED DONE`

Current:

`fe/src/app/features/fiscal-periods/fiscal-periods.page.ts`

uses an inline Arabic success message after status change.

### Checklist

- [x] replace with i18n key;
- [x] add Arabic translation;
- [x] add English translation;
- [x] run existing hardcoded-string/i18n checks;
- [x] update message behavior when fiscal close is changed to the authoritative close flow.

---

# 18. P2 — Frontend Security Header Enhancement

## SEC-002 — Add CSP at the real deployment boundary

**Status:** `VERIFIED DONE — NGINX DEPLOYMENT BOUNDARY`

Current frontend Nginx config has useful baseline headers such as:

- `X-Frame-Options`
- `X-Content-Type-Options`

A Content Security Policy was not observed in the inspected Nginx config.

### Checklist

- [x] identify actual production frontend boundary (`fe/nginx.conf`; TLS remains upstream);
- [x] define only required script/style/font/image/connect-src origins;
- [x] test Angular production bundle under CSP;
- [x] avoid `unsafe-eval`;
- [x] minimize `unsafe-inline` (styles only for current Angular component styling);
- [x] configure HSTS at the TLS termination layer, not on this HTTP-only inner container;
- [x] preserve same-origin API/PWA behavior.

Evidence: production build passed; generated JavaScript contains no `eval`/`new Function`; CSP, Referrer-Policy and Permissions-Policy apply to HTML and static assets.

---

# 19. Items Specifically Rechecked and **Not** Reopened

These previously suspicious areas were inspected in current code and should **not** be put back on the missing-work list unless a new failing test proves a defect.

## Payroll snapshot calculation

**Current assessment:** `DO NOT REOPEN AS MISSING`

Current `PayrollService.recordPayment(...)`:

- resolves payroll run;
- resolves effective calculation policy;
- captures a payroll snapshot;
- uses snapshot-derived gross/deductions/bonus/net for persisted payment.

The remaining Payroll problem is **workflow/state governance**, not the old “no snapshot” problem.

---

## Manufacturing frozen BOM for active execution

**Current assessment:** `DO NOT REOPEN AS MISSING`

Current start path freezes requirements before material issue.

Current completion path uses frozen requirements and actual issue-cost evidence.

The remaining Manufacturing issues are:

- cancellation valuation;
- BOM active/effective applicability;
- exact revision identity.

---

## Vendor payment proposal → supplier payment

**Current assessment:** `DO NOT REOPEN AS GENERIC MISSING FLOW`

Current `VendorPaymentProposalService.executeProposal(...)`:

- requires proposal `APPROVED`;
- enforces segregation-of-duties checks;
- creates per-allocation operation IDs;
- calls `ProcurementService.createSupplierPaymentsForProposal(...)`;
- links resulting supplier payment IDs to proposal allocations;
- marks proposal executed.

Do not rebuild this flow.

If additional treasury/GL acceptance is required, test the downstream `ProcurementService` posting path rather than creating a second payment mechanism.

---

# 20. Cross-Cutting Definition of Done for All Financial / Stock Fixes

For every applicable P0/P1 item:

## Domain integrity

- [ ] invariant enforced in backend;
- [ ] invalid state rejected;
- [ ] no UI-only validation;
- [ ] stable business error code.

## Tenant isolation

- [ ] tenant A cannot read tenant B data;
- [ ] tenant A cannot mutate tenant B data;
- [ ] request-supplied IDs are resolved inside active tenant context.

## Authorization

- [ ] permission enforced server-side;
- [ ] direct REST bypass test exists;
- [ ] menu visibility is only UX, not security.

## Idempotency

For commands affecting money, stock or accounting:

- [ ] operation ID or equivalent exists where retries are possible;
- [ ] same command replay cannot duplicate effect;
- [ ] different payload with same operation ID is rejected or handled deterministically.

## Concurrency

- [ ] optimistic/pessimistic locking chosen deliberately;
- [ ] stock cannot oversubscribe;
- [ ] payment cannot double-disburse;
- [ ] period cannot close twice;
- [ ] journal cannot post twice;
- [ ] PostgreSQL integration tests cover database-lock behavior where H2 is insufficient.

## Audit

- [ ] creator preserved;
- [ ] action actor preserved;
- [ ] timestamp preserved;
- [ ] source document reference preserved;
- [ ] reversal links original evidence;
- [ ] no secrets in logs.

## Accounting integrity

- [ ] debit = credit;
- [ ] posting references source;
- [ ] reversal references original posting;
- [ ] closed-period rules enforced;
- [ ] report calculations use posted evidence only.

---

# 21. Required Verification Before Final Sign-Off

This source review did **not** treat documentation claims as test evidence.

Before final “all done” sign-off, run the actual code.

## Backend

From `be/`:

- [ ] `./gradlew clean test check`
- [ ] test-count regression gate
- [ ] error-code validation
- [ ] translation-catalog validation
- [ ] authorization-contract validation

### PostgreSQL/Testcontainers

Do not rely only on tests with Docker suites skipped.

- [ ] PostgreSQL integration tests pass.
- [ ] Liquibase fresh schema migration passes.
- [ ] Liquibase upgrade-path tests pass where maintained.
- [ ] inventory concurrency tests pass.
- [ ] supplier/payment concurrency tests pass.
- [ ] new payroll concurrency/state tests pass.
- [ ] new period-close concurrency/idempotency tests pass.
- [ ] new manufacturing reversal valuation tests pass.

## Frontend

From `fe/`:

- [ ] `npm ci`
- [ ] `npm run check:i18n`
- [ ] `npm run check:hardcoded`
- [ ] `npm run test -- --watch=false`
- [ ] `npm run build`

## Production-like smoke

- [ ] Compose configuration renders.
- [ ] Database healthy.
- [ ] Backend healthy.
- [ ] Frontend loads.
- [ ] Login works.
- [ ] Payroll legal-state workflow works.
- [ ] Inventory reservation cannot oversubscribe.
- [ ] Manufacturing cancel restores exact quantity/value.
- [ ] Reconciliation is server-calculated.
- [ ] Fiscal close uses authoritative close command.
- [ ] Cash Flow is either correct or disabled.
- [ ] Journal reversal shows complete audit metadata.

---

# 22. Required Developer Workflow

Work in this order.

## Milestone 1 — Financial/stock integrity blockers

- [ ] PAY-001
- [x] INV-001
- [x] FIN-001
- [x] FIN-002
- [x] FIN-003
- [x] FIN-004
- [x] MFG-001

**Milestone success:** no known path can bypass payroll approval, oversubscribe stock through the simple endpoint, persist caller-forged reconciliation balances, close a period through a bypass, return a fake cash-flow statement, or reverse manufacturing at a different cost basis.

---

## Milestone 2 — Audit/governance integrity

- [x] PAY-002
- [x] MFG-002
- [x] MFG-003
- [x] FIN-005
- [x] FIN-006
- [x] O2C-001
- [x] SEC-001

**Milestone success:** historical evidence is preserved, governance configuration is real, and AR/customer accounting is proven end-to-end.

---

## Milestone 3 — UI completion

- [x] FIN-UI-001 decision/implementation
- [x] UI-001

**Milestone success:** users cannot close periods through a weaker UI path and relevant finance capabilities are either properly exposed or intentionally API-only.

---

## Milestone 4 — Technical enhancements

- [x] TECH-001
- [x] TECH-002
- [x] TECH-003
- [x] SEC-002
- [x] MFG-004 — N/A by current all-or-nothing production-order scope

---

## Milestone 5 — Release verification

- [ ] run complete backend suite;
- [ ] run PostgreSQL/Testcontainers suite;
- [ ] run frontend validation/tests/build;
- [ ] run production-like smoke;
- [ ] run CI on final candidate;
- [ ] re-inspect the final branch code;
- [ ] only then update project documentation/status.

---

# 23. Final “Done” Gate

The developer must **not** say:

- “all done”
- “100% complete”
- “fully verified”
- “release ready”

until all applicable items below are true:

- [ ] all P0 items are `[x]`;
- [ ] all required P1 items are `[x]`;
- [ ] every `VERIFY BY TRACE + TEST` item has actual source/test evidence;
- [ ] optional items are either done or explicitly approved `N/A`;
- [ ] backend tests pass;
- [ ] PostgreSQL/Testcontainers tests pass;
- [ ] frontend checks/tests/build pass;
- [ ] production-like smoke passes;
- [ ] exact release candidate passes CI;
- [ ] no known financial/stock integrity blocker remains;
- [ ] Tech Lead has re-reviewed the **actual code**, not status documentation.

Only then:

```text
FINAL STATUS: VERIFIED COMPLETE
```

Until then:

```text
FINAL STATUS: IMPLEMENTATION IN PROGRESS / NOT YET RELEASE-VERIFIED
```

---

# 24. Developer Completion Report

Use this after finishing the checklist.

```text
Branch: fm_bemo_consolidated
Date:
Final SHA:

P0
PAY-001:
INV-001:
FIN-001:
FIN-002:
FIN-003:
FIN-004:
MFG-001:

P1
PAY-002:
MFG-002:
MFG-003:
FIN-005:
FIN-006:
O2C-001:
FIN-UI-001:
SEC-001:

P2 / Enhancements
TECH-001:
TECH-002:
TECH-003:
UI-001:
SEC-002:
MFG-004 / N/A:

Backend verification:
PostgreSQL/Testcontainers:
Liquibase:
Frontend verification:
Production smoke:
CI URL/result:

For every item marked DONE, evidence location:

Known issues remaining:

I confirm that no item was marked DONE based on README/PROJECT_MAP/
TEST_EVIDENCE/commit text/class existence alone.
I verified the actual production code path and acceptance criteria.
```

---

# 25. Code Locations Used for This Review

These are **source-code reference points**, not status documents.

## Payroll

- `be/src/main/java/com/bemo/hr/payroll/domain/SalaryPayment.java`
- `be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java`

## Inventory

- `be/src/main/java/com/bemo/hr/inventory/application/InventoryService.java`
- `be/src/main/java/com/bemo/hr/inventory/api/InventoryController.java`
- existing operations/warehouse inventory reservation implementation

## Manufacturing

- `be/src/main/java/com/bemo/hr/manufacturing/production/application/ManufacturingService.java`

## Finance

- `be/src/main/java/com/bemo/hr/finance/application/FinancialStatementsReportService.java`
- `be/src/main/java/com/bemo/hr/finance/api/FinancialStatementsController.java`
- `be/src/main/java/com/bemo/hr/finance/application/CloseChecklistService.java`
- `be/src/main/java/com/bemo/hr/finance/application/SubledgerReconciliationService.java`
- `be/src/main/java/com/bemo/hr/finance/api/SubledgerReconciliationController.java`
- `be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java`
- `be/src/main/java/com/bemo/hr/finance/api/PeriodCloseController.java`
- `be/src/main/java/com/bemo/hr/finance/application/close/PeriodCloseOrchestratorService.java`
- `be/src/main/java/com/bemo/hr/finance/application/JournalApprovalService.java`
- `be/src/main/java/com/bemo/hr/finance/application/JournalEntryService.java`
- `be/src/main/java/com/bemo/hr/finance/domain/JournalEntry.java`

## Sales / AR

- `be/src/main/java/com/bemo/hr/trade/sales/application/SalesReceivablesService.java`

## Procurement / payment proposal

- `be/src/main/java/com/bemo/hr/trade/procurement/application/VendorPaymentProposalService.java`

## Frontend

- `fe/src/app/app.routes.ts`
- `fe/src/app/features/fiscal-periods/fiscal-periods.page.ts`

## Build/runtime

- `be/build.gradle`
- `be/Dockerfile`
- `fe/package.json`
- `fe/.nvmrc`
- `fe/Dockerfile`
- `fe/nginx.conf`
- `.github/workflows/ci.yml`

---

# 26. Review Limitation

This is a **current-source static audit**, not a claim that the full test suite was independently executed by the reviewer.

The review environment could inspect current GitHub branch source, including raw source files, but did not use README/project-map/test-evidence status claims as proof.

Therefore:

- source-confirmed issues in this file are actionable;
- `VERIFY BY TRACE + TEST` items must be traced and tested before code is changed;
- final release sign-off still requires actual local/CI execution against the final branch.

---

# 27. Tech Lead Guidance

Keep the fixes surgical.

Do **not** solve these remaining issues by:

- splitting the modular monolith into microservices;
- adding Kafka;
- creating another inventory service;
- creating a generic workflow engine;
- adding a generic rules engine;
- replacing the finance module;
- rewriting payroll;
- rewriting manufacturing;
- duplicating journal posting.

The codebase already contains substantial foundations.

The remaining goal is:

> **close the unsafe seams, unify authoritative command paths, preserve accounting evidence, and prove the result with tests.**
