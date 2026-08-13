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
| PAY-001 | CONFIRMED | Arbitrary status assignment remains; single/bulk payment can bypass approval; reversal accepts unpaid states. |
| INV-001 | CONFIRMED | The simple `InventoryReservation` endpoint remains independent from the locked `StockReservation` path and still uses `branch-default`. |
| FIN-001 | CONFIRMED | Cash flow still maps net income to operating cash and returns zero investing/financing cash. |
| FIN-002 | CONFIRMED | Official reconciliation still accepts caller balances and falls back to zero. |
| FIN-003 | CONFIRMED | Direct fiscal-period status close and module orchestrator remain separate; orchestrator does not close the fiscal-period aggregate. |
| FIN-004 | CONFIRMED | Precheck still uses `LocalDate.now()` and a signed comparison rather than absolute variance against the fiscal-period end date. |
| MFG-001 | CONFIRMED | Cancellation still restores issued material at latest unit cost rather than original issue valuation. |

No item above is marked complete by this validation. Existing tests that encode the unsafe behavior must be replaced or strengthened as part of the corresponding item.

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
- [ ] **INV-001** — Remove unsafe parallel inventory reservation path
- [ ] **FIN-001** — Cash Flow Statement is currently not a real cash-flow calculation
- [ ] **FIN-002** — Reconciliation report must not accept caller-supplied official balances
- [ ] **FIN-003** — Unify fiscal-period close into one authoritative close workflow
- [ ] **FIN-004** — Close reconciliation must use fiscal period end date and correct tolerance semantics
- [ ] **MFG-001** — Manufacturing cancellation must reverse original material issue valuation, not latest cost

## P1 — Important correctness/governance completion

- [ ] **PAY-002** — Preserve payroll creator identity and record payment/reversal actors separately
- [ ] **MFG-002** — Enforce BOM active/effective-date applicability
- [ ] **MFG-003** — Preserve exact BOM revision identity instead of lossy numeric parsing
- [ ] **FIN-005** — Journal reversal entry must have complete creator/poster/timestamp audit metadata
- [ ] **FIN-006** — Journal approval-rule configuration must be wired or removed
- [ ] **O2C-001** — Prove AR/customer documents post to GL exactly once end-to-end
- [ ] **FIN-UI-001** — Add/complete user-facing financial statement, reconciliation and close workflow if these are GUI features
- [ ] **SEC-001** — Align financial-report/reconciliation authorization with the intended finance permission model

## P2 — Enhancements / cleanup

- [ ] **TECH-001** — Clarify Java 21 toolchain vs Java 17 bytecode target
- [ ] **TECH-002** — Standardize frontend Node major across local/CI/container builds
- [ ] **TECH-003** — Replace remaining business-critical `LocalDate.now()` / `Instant.now()` decisions with explicit business dates or injected clock where determinism matters
- [ ] **UI-001** — Remove remaining hard-coded UI messages from fiscal-period flow
- [ ] **SEC-002** — Add a modern Content Security Policy at the real frontend/TLS boundary after validating required origins
- [ ] **MFG-004** — Partial material issue / partial production receipt only if required by business scope

---

# 4. P0 — Payroll Workflow Integrity

## PAY-001 — Enforce a real payroll state machine

**Status:** `CONFIRMED ISSUE`

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

- [ ] Define the authoritative `PaymentStatus` transition graph.
- [ ] Put the transition rules in the payroll domain/application layer, not only the UI.
- [ ] Replace unrestricted `updateStatus(nextStatus)` with guarded transition methods or a single validated transition method.
- [ ] Reject transition attempts that skip required workflow states.
- [ ] Reject backward transitions unless an explicit reversal/reopen command exists.
- [ ] Decide the exact payable state.
- [ ] Make `recordPayment(...)` require the exact payable state.
- [ ] Remove the contradiction where `APPROVED` is blocked while less mature states can be paid.
- [ ] Make `payBulk(...)` pay only rows in the legal payable state.
- [ ] Prevent bulk payment from silently paying `DRAFT`, `CALCULATED` or `REVIEWED` rows unless that is explicitly the approved business model.
- [ ] Require reversal to originate only from a reversible state.
- [ ] Prevent `DRAFT`, `CALCULATED`, `REVIEWED`, or other unpaid records from being “reversed”.
- [ ] Keep the frozen payroll snapshot unchanged through approval/payment/posting.
- [ ] Ensure payroll-run header state and per-employee payment state cannot contradict each other.
- [ ] Define what happens if one employee in a period cannot be paid during bulk payment.
- [ ] Prefer an explicit failed/partial result over silently leaving the register in a mixed unexplained state.
- [ ] Ensure optimistic locking/version checks are used on state-changing requests.
- [ ] Ensure the backend, not the Angular page, is authoritative.

### Authorization / SoD checklist

- [ ] Define who can calculate payroll.
- [ ] Define who can review payroll.
- [ ] Define who can approve payroll.
- [ ] Define who can pay/post payroll.
- [ ] If maker/checker is required, prevent the preparer from approving their own run.
- [ ] If required, prevent the approver from being the disburser/poster.
- [ ] Prove direct REST calls cannot bypass the rule.
- [ ] Audit actor + previous status + new status.

### Required automated tests

- [ ] `draft_cannot_jump_directly_to_paid`
- [ ] `calculated_cannot_jump_to_paid_when_review_and_approval_are_required`
- [ ] `reviewed_cannot_be_paid_before_approval`
- [ ] `approved_can_progress_to_the_defined_payable_next_state`
- [ ] `invalid_backward_transition_is_rejected`
- [ ] `paid_payment_cannot_be_paid_twice`
- [ ] `posted_payment_cannot_be_paid_twice`
- [ ] `unpaid_payment_cannot_be_reversed`
- [ ] `paid_or_posted_payment_can_be_reversed_exactly_once`
- [ ] `bulk_pay_skips_or_rejects_non_payable_rows`
- [ ] `bulk_pay_does_not_bypass_approval`
- [ ] `concurrent_payment_requests_do_not_double_pay`
- [ ] `stale_version_is_rejected`
- [ ] `unauthorized_user_cannot_transition_payroll`
- [ ] `tenant_A_cannot_transition_tenant_B_payroll`

### Definition of Done

Do **not** mark `PAY-001` done until:

- [ ] one legal workflow is implemented server-side;
- [ ] every state-changing endpoint follows it;
- [ ] bulk and single payment use the same invariant;
- [ ] reversal is guarded;
- [ ] authorization/SoD rules are enforced in backend code;
- [ ] concurrency/retry behavior is safe;
- [ ] automated tests prove the above.

### Evidence

```text
Status:
Chosen state graph:
Implementation SHA:
Domain methods changed:
Service/controller methods changed:
Tests:
Concurrency test:
Authorization/SoD test:
Reviewer:
```

---

## PAY-002 — Do not overwrite `createdBy` during payment/reversal

**Status:** `CONFIRMED ISSUE`  
**Priority:** P1

### Current code evidence

In `SalaryPayment`:

- `markAsPaid(..., actor)` assigns the payment actor into `createdBy`.
- `markAsReversed(..., actor)` also assigns the reversal actor into `createdBy`.

That destroys the original creator identity and weakens audit history.

### Required work

- [ ] Keep `createdBy` immutable after creation.
- [ ] Add/use a separate `paidBy` field.
- [ ] Add/use a separate `reversedBy` field.
- [ ] Preserve `paidAt`.
- [ ] Add/preserve `reversedAt`.
- [ ] Store reversal reason separately from generic notes where practical.
- [ ] Add a Liquibase migration for new persistent fields if they do not already exist.
- [ ] Update API response models if users/auditors need these fields.
- [ ] Ensure existing historical rows are migrated safely.
- [ ] Do not fake historical actor values during migration if they cannot be recovered.

### Tests

- [ ] creator remains unchanged after payment;
- [ ] creator remains unchanged after reversal;
- [ ] `paidBy` records the payment actor;
- [ ] `reversedBy` records the reversal actor;
- [ ] audit event records the same actors;
- [ ] API exposes consistent actor data where required.

### Definition of Done

- [ ] `createdBy` can never be overwritten by payment or reversal operations.
- [ ] financial audit history can distinguish creator, payer and reverser.

---

# 5. P0 — Inventory Must Have One Authoritative Reservation Invariant

## INV-001 — Remove the unsafe parallel reservation path

**Status:** `CONFIRMED ISSUE / ARCHITECTURE INCONSISTENCY`

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

- [ ] Identify the authoritative stock reservation aggregate/service.
- [ ] Identify all callers of `InventoryService.reserveStock(...)`.
- [ ] Identify all callers of the richer stock-reservation service.
- [ ] Ensure both cannot mutate different reservation stores independently.
- [ ] Delegate or migrate to one reservation invariant.
- [ ] Reject null/zero/negative reservation quantity.
- [ ] Validate item exists and is active.
- [ ] Validate warehouse exists and is active.
- [ ] Validate warehouse belongs to the active tenant.
- [ ] Validate source document type and source document ID where required.
- [ ] Validate sufficient reservable stock.
- [ ] Lock the appropriate balance/reservation rows for concurrent reservation.
- [ ] Prevent oversubscription.
- [ ] Add operation ID/idempotency where reservation commands may be retried.
- [ ] Ensure release is idempotent.
- [ ] Ensure delivery/issue consumes the authoritative reservation.
- [ ] Ensure cancellation releases the authoritative reservation.
- [ ] Decide how to migrate existing rows if `InventoryReservation` and `StockReservation` represent duplicate concepts.
- [ ] Do not maintain two “truths” after migration.
- [ ] Remove `"branch-default"` fallback.
- [ ] Require a valid branch or define an explicit warehouse-without-branch business rule.
- [ ] Validate branch belongs to the active tenant.

### Required concurrency tests

Use PostgreSQL/Testcontainers where database locking matters.

- [ ] two concurrent reservations cannot reserve more than available stock;
- [ ] retry with same operation ID does not duplicate reservation;
- [ ] release replay does not over-release;
- [ ] direct call to `/api/v1/inventory/reservations` cannot bypass stock checks;
- [ ] tenant A cannot reserve from tenant B warehouse;
- [ ] invalid/nonexistent item is rejected;
- [ ] zero/negative quantity is rejected.

### Definition of Done

- [ ] there is one authoritative reservation state;
- [ ] every public mutation path enforces the same stock invariants;
- [ ] concurrency cannot oversubscribe inventory;
- [ ] no fake/default branch ID is silently persisted;
- [ ] old duplicate path is delegated, migrated or removed.

---

# 6. P0 — Financial Statements Integrity

## FIN-001 — Cash Flow Statement must be real or disabled

**Status:** `CONFIRMED ISSUE`

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

- [ ] remove/hide the public endpoint or return an explicit unsupported/not-implemented response;
- [ ] do not show fabricated zero investing/financing values;
- [ ] do not label the existing implementation as complete.

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

- [ ] endpoint is either financially correct and tested, or explicitly disabled.
- [ ] no production endpoint returns a knowingly simplified result labeled as a full Cash Flow Statement.

---

# 7. P0 — Reconciliation Integrity

## FIN-002 — Never accept caller-provided official GL/subledger balances

**Status:** `CONFIRMED ISSUE`

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

- [ ] Remove `glBalance` from the official report-generation request.
- [ ] Remove `subledgerBalance` from the official report-generation request.
- [ ] Resolve the requested fiscal period server-side.
- [ ] Use the fiscal period end date as the reconciliation `asOf`.
- [ ] Require a registered `SubledgerReconciliationProvider` for the requested subledger.
- [ ] Fail with a stable business error if no provider exists.
- [ ] Calculate GL balance on the server.
- [ ] Calculate subledger balance on the server.
- [ ] Persist the source differences generated by the provider.
- [ ] Persist enough source metadata to reproduce the report.
- [ ] If manual comparison is genuinely needed, create a **separate**, clearly labelled, audited “manual comparison” feature that cannot be confused with official reconciliation.
- [ ] Do not silently store `0 / 0` because a provider is missing.
- [ ] Validate period/subledger type.
- [ ] Ensure tenant isolation.

### Tests

- [ ] client cannot influence calculated GL ba…4022 tokens truncated…base.

Therefore:

> **Do not immediately add duplicate GL posting. First trace the full production call path.**

### Trace checklist

For each business event below:

#### Customer invoice

- [ ] Locate the exact GL posting path.
- [ ] Prove AR control is debited.
- [ ] Prove revenue/tax accounts are credited according to the current model.
- [ ] Prove source invoice ID/number links to journal.
- [ ] Prove retry cannot duplicate posting.

#### Customer receipt

- [ ] Locate exact GL posting path.
- [ ] Prove cash/bank is debited.
- [ ] Prove AR/customer advance is credited correctly.
- [ ] Prove partial allocation behavior is correct.
- [ ] Prove unallocated receipt/advance behavior is correct.
- [ ] Prove retry cannot duplicate posting.

#### Customer credit note / return

- [ ] Locate exact GL posting/reversal path.
- [ ] Prove AR/customer balance is reduced correctly.
- [ ] Prove revenue/tax reversal follows current accounting rules.
- [ ] Prove inventory/COGS return effects are handled exactly once where applicable.
- [ ] Prove source return/delivery/invoice references are preserved.

### If posting is absent

Use the existing finance/subledger posting architecture.

Do **not** create a new parallel journal system.

### End-to-end acceptance scenario

- [ ] create customer;
- [ ] create sales order;
- [ ] reserve stock;
- [ ] deliver;
- [ ] verify stock issue;
- [ ] verify COGS posting;
- [ ] issue invoice;
- [ ] verify AR/revenue posting;
- [ ] record partial receipt;
- [ ] verify AR/cash posting;
- [ ] record final receipt;
- [ ] verify outstanding balance zero;
- [ ] return part of delivery;
- [ ] issue/apply credit note;
- [ ] verify stock, COGS, AR and GL remain consistent;
- [ ] replay relevant operation IDs;
- [ ] verify no duplicate stock/ledger/journal effects.

### Definition of Done

Mark this item done only if the developer can point to:

- [ ] the real source code paths;
- [ ] the source-document → journal references;
- [ ] automated integration tests proving exact-once accounting effects.

---

# 13. P1 — Finance UI Completion

## FIN-UI-001 — Expose real finance controls to users or explicitly classify them API-only

**Status:** `ENHANCEMENT / PRODUCT-SCOPE DECISION`

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

- [ ] add one minimal finance reporting/close workbench rather than multiple fragmented micro-pages;
- [ ] expose Balance Sheet;
- [ ] expose Income Statement;
- [ ] expose Cash Flow only after `FIN-001` is correct;
- [ ] expose reconciliation generation/history;
- [ ] expose reconciliation differences/source references;
- [ ] expose fiscal-period precheck;
- [ ] expose module close readiness;
- [ ] expose authoritative close execution;
- [ ] show blockers before close;
- [ ] show completion evidence after close;
- [ ] protect routes with the same permissions as backend;
- [ ] use i18n keys.

If these APIs are intentionally API-only:

- [ ] document that product decision after implementation review;
- [ ] ensure menu/routes do not imply otherwise;
- [ ] still fix backend integrity issues.

### Simplicity rule

Prefer one consolidated **Finance Reports & Close** workbench if that is enough.

Do not build a BI platform.

---

# 14. P1 — Authorization Alignment

## SEC-001 — Align financial report/reconciliation permissions

**Status:** `VERIFY / SECURITY ENHANCEMENT`

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

- [ ] Define the intended permission to read financial statements.
- [ ] Define who can generate reconciliation.
- [ ] Define who can read reconciliation differences.
- [ ] Define who can execute close.
- [ ] Prefer explicit permissions/authorities where the project permission model already supports them.
- [ ] Avoid granting sensitive financial data merely because a user has a broad generic `VIEWER` role unless this is a deliberate product policy.
- [ ] Align backend authorization.
- [ ] Align menu visibility/routes.
- [ ] Test direct API access.

### Tests

- [ ] permitted accountant/report user can read intended reports;
- [ ] unauthorized non-finance user receives `403`;
- [ ] reconciliation generation restricted appropriately;
- [ ] period close restricted appropriately;
- [ ] frontend visibility does not substitute for backend enforcement.

---

# 15. P2 — Build & Runtime Consistency

## TECH-001 — Clarify Java toolchain vs bytecode target

**Status:** `ENHANCEMENT`

### Current code

`be/build.gradle` currently uses:

```text
Java toolchain: 21
JavaCompile release target: 17
```

Backend Docker builds/runs on Java 21.

This can be a valid deliberate compatibility strategy, but it should be intentional.

### Checklist

- [ ] Decide whether production bytecode compatibility target is Java 17 or Java 21.
- [ ] If Java 17 bytecode is intentional, keep it and add a concise build comment explaining why.
- [ ] If Java 21 language/runtime features are intended, change `options.release` consistently.
- [ ] Ensure local, CI and Docker builds compile the same source semantics.
- [ ] Do not upgrade solely for cosmetic consistency.

---

## TECH-002 — Standardize frontend Node major

**Status:** `ENHANCEMENT`

### Current code

- `fe/.nvmrc` → Node 24
- GitHub CI → Node 24
- `fe/Dockerfile` builder → Node 22
- `package.json` allows `>=22 <25`

Both majors are permitted, but local/CI/container builds are not identical.

### Checklist

- [ ] Choose the supported build major.
- [ ] Prefer matching `.nvmrc`, CI and Docker builder.
- [ ] Keep `engines` range only if multiple majors are intentionally supported/tested.
- [ ] Verify `npm ci`, tests and production build using the selected container/local version.
- [ ] Keep `package-lock.json` authoritative.

---

# 16. P2 — Deterministic Business Time

## TECH-003 — Use explicit business date / injected clock for sensitive calculations

**Status:** `ENHANCEMENT`

Confirmed correctness issue for close is already covered by `FIN-004`.

Additional code paths use `LocalDate.now()` / `Instant.now()` for defaults, aging, collections and timestamps.

Timestamps are fine in many cases. Business-rule dates should be deliberate.

### Checklist

- [ ] Do not globally replace every `now()`.
- [ ] Identify only calculations whose result changes based on business date.
- [ ] Pass explicit `asOf` / period date where the API already has one.
- [ ] Use injected `Clock` if deterministic “today” behavior is genuinely required.
- [ ] Ensure company timezone is explicit where local business dates matter.
- [ ] Add deterministic tests around date boundaries.

Priority examples:

- [ ] fiscal close → must use fiscal period end;
- [ ] aging → explicit as-of date should remain authoritative;
- [ ] collection generation → explicit as-of date;
- [ ] policy effective-date resolution → use document/run date.

---

# 17. P2 — UI/i18n Cleanup

## UI-001 — Remove hard-coded fiscal-period success message

**Status:** `CONFIRMED SMALL ISSUE`

Current:

`fe/src/app/features/fiscal-periods/fiscal-periods.page.ts`

uses an inline Arabic success message after status change.

### Checklist

- [ ] replace with i18n key;
- [ ] add Arabic translation;
- [ ] add English translation;
- [ ] run existing hardcoded-string/i18n checks;
- [ ] update message behavior when fiscal close is changed to the authoritative close flow.

---

# 18. P2 — Frontend Security Header Enhancement

## SEC-002 — Add CSP at the real deployment boundary

**Status:** `ENHANCEMENT`

Current frontend Nginx config has useful baseline headers such as:

- `X-Frame-Options`
- `X-Content-Type-Options`

A Content Security Policy was not observed in the inspected Nginx config.

### Checklist

- [ ] identify actual production TLS/reverse-proxy termination point;
- [ ] define only required script/style/font/image/connect/connect-src origins;
- [ ] test Angular production bundle under CSP;
- [ ] avoid `unsafe-eval`;
- [ ] minimize `unsafe-inline`; use nonces/hashes if necessary and practical;
- [ ] configure HSTS at the TLS termination layer, not blindly on an HTTP-only inner container;
- [ ] do not add headers that break OAuth/API/PWA behavior without tests.

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
- [ ] INV-001
- [ ] FIN-001
- [ ] FIN-002
- [ ] FIN-003
- [ ] FIN-004
- [ ] MFG-001

**Milestone success:** no known path can bypass payroll approval, oversubscribe stock through the simple endpoint, persist caller-forged reconciliation balances, close a period through a bypass, return a fake cash-flow statement, or reverse manufacturing at a different cost basis.

---

## Milestone 2 — Audit/governance integrity

- [ ] PAY-002
- [ ] MFG-002
- [ ] MFG-003
- [ ] FIN-005
- [ ] FIN-006
- [ ] O2C-001
- [ ] SEC-001

**Milestone success:** historical evidence is preserved, governance configuration is real, and AR/customer accounting is proven end-to-end.

---

## Milestone 3 — UI completion

- [ ] FIN-UI-001 decision/implementation
- [ ] UI-001

**Milestone success:** users cannot close periods through a weaker UI path and relevant finance capabilities are either properly exposed or intentionally API-only.

---

## Milestone 4 — Technical enhancements

- [ ] TECH-001
- [ ] TECH-002
- [ ] TECH-003
- [ ] SEC-002
- [ ] MFG-004 only if approved

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


