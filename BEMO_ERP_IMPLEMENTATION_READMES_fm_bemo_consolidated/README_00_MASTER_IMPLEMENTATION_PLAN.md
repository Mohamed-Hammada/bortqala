# BEMO ERP — Junior-Developer Implementation Package

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Purpose

This package converts the business-cycle review into an **implementation-oriented work plan**. It is not a second conceptual roadmap. Every module README starts from the existing branch and tells the developer what to inspect, what to preserve, what to add, the state machine, data model, API changes, UI changes, integration points, tests, and an ordered implementation checklist.

The target architecture is one traceable ERP chain:

`source/request → approval → operational execution → immutable result/snapshot → accounting/subledger → payment/receipt → bank reconciliation → period close`


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


## How a junior developer should use this package

Do **not** open all modules and start coding at once.

1. Checkout `fm_bemo_consolidated` and confirm `git rev-parse HEAD` is `aa3f940cca0119d7f523e03e3fd317fb72684cf3` or deliberately record the newer commit being implemented.
2. Run the existing backend and frontend test suites before changes; save the baseline failures separately.
3. Read `README_01_ARCHITECTURE_AND_CODEBASE_MAP.md`.
4. Implement P0 shared controls before module-specific P1/P2 work.
5. For each module, implement one vertical slice at a time: schema → domain/entity → service → API → frontend → integration → tests.
6. Do not mark a slice complete until positive, negative, duplicate-operation, concurrent-update, reversal/correction, and authorization cases pass where applicable.
7. Keep each PR focused. Prefer one shared-control PR or one business slice per PR instead of a single enormous branch.

## Package index

| Order | File | Developer outcome |
|---:|---|---|
| 00 | `README_00_MASTER_IMPLEMENTATION_PLAN.md` | Overall sequencing and gates |
| 01 | `README_01_ARCHITECTURE_AND_CODEBASE_MAP.md` | Where current code lives and what must be reused |
| 02 | `README_02_SHARED_DOCUMENT_LIFECYCLE_IDEMPOTENCY.md` | Shared commands, states, concurrency, reversals |
| 03 | `README_03_APPROVAL_SOD_AUDIT.md` | Existing approval engine integration and SoD |
| 04 | `README_04_POSTING_SUBLEDGER_GL.md` | Deterministic posting profiles and journal linkage |
| 05 | `README_05_PERIOD_CLOSE_RECONCILIATION.md` | Close checklist and reconciliations |
| 10 | `README_10_WORKFORCE_CONTRACTOR.md` | Labor need → contractor settlement → payment |
| 11 | `README_11_ATTENDANCE_TO_PAYROLL.md` | Effective-dated attendance → immutable payroll input |
| 12 | `README_12_PAYROLL.md` | Payroll run/component/posting/payment architecture |
| 20 | `README_20_PROCURE_TO_PAY.md` | Requisition/RFQ → PO/GRN/AP → payment |
| 21 | `README_21_ORDER_TO_CASH.md` | SO lines/reservation/delivery/AR/returns |
| 22 | `README_22_INVENTORY_WAREHOUSE.md` | Warehouse/bin/status/reservation/transfer/count |
| 23 | `README_23_MANUFACTURING_QUALITY.md` | BOM snapshot, actual issues, partial receipt, WIP/QC |
| 24 | `README_24_TREASURY_BANK.md` | Shared payment batches and bank reconciliation |
| 25 | `README_25_BUDGET_CONTROL.md` | Versioned budgets, commitments, revisions/transfers |
| 26 | `README_26_FINANCE_RECORD_TO_REPORT.md` | GL controls, FX close and financial statements |
| 27 | `README_27_MASTER_DATA_EFFECTIVE_DATING.md` | Govern/version master data and historical terms |
| 28 | `README_28_NON_AI_RULES_SCHEDULERS.md` | Deterministic rules/recommendations/jobs |
| 30 | `README_30_FRONTEND_WORKBENCHES.md` | Cycle-oriented Angular UX |
| 40 | `README_40_DATABASE_MIGRATIONS.md` | Safe DB evolution and backfill sequence |
| 50 | `README_50_API_SECURITY_ERROR_CONVENTIONS.md` | Command/API/error/authorization conventions |
| 60 | `README_60_TESTING_QA_ACCEPTANCE.md` | Automated + end-to-end QA matrix |
| 70 | `README_70_JUNIOR_DEVELOPER_EXECUTION_ORDER.md` | Exact recommended coding order |
| — | `CODE_TOUCHPOINT_INDEX.md` | Existing class/page lookup |
| — | `IMPLEMENTATION_CHECKLIST.md` | Cross-module progress checklist |
| — | `SOURCE_ROADMAP.md` | Original business review copied unchanged |
| — | `MANIFEST.md` | File inventory and checksums |

## Priority sequence

### P0 — integrity first

- shared lifecycle/transition command contract;
- approval integration adapter/convention;
- source-document and posting-profile linkage;
- subledger-to-GL reconciliation framework;
- period-close blocker framework;
- immutable snapshots for attendance/payroll/settlements/commercial terms;
- reversal/correction instead of post-posting edits.

### P1 — close current cycles end to end

- Workforce settlement → AP/GL → treasury → bank.
- Payroll → GL → payment batch → bank.
- Procurement → AP/GL → treasury → bank.
- Sales → inventory/COGS + AR/GL → receipts → bank.
- Inventory valuation → GL reconciliation.

### P2 — fill operational gaps

- requisition/RFQ/quote/award;
- sales lines/reservation/delivery/returns;
- warehouse/bin/reservations/transfers/counts;
- contractor dispatch/allocation/disputes;
- payroll calendars/components/retro/off-cycle;
- manufacturing routing/WIP/partial execution/quality;
- landed cost, FX close, budget revision/forecast.

## Release gate for every business cycle

A cycle is **not done** because screens exist. It is done only when:

- state transitions are server-enforced;
- documents are traceably linked;
- approval is configurable;
- duplicate commands are safe;
- concurrency is detected;
- posted results are immutable/reversible;
- stock/money side effects are reconcilable;
- journal linkage exists where financially relevant;
- exception queues have resolution paths;
- audit history is complete;
- effective-dated versions are preserved;
- period close can detect unfinished work;
- QA covers normal/negative/partial/reversal/concurrency paths.
