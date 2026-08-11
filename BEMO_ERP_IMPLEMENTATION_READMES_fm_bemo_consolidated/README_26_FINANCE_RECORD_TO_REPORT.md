# Finance / Record-to-Report — Technical Implementation

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

Make the general ledger the controlled accounting destination of all operational cycles. Users should not manually recreate journals that procurement, sales, payroll, workforce, inventory, manufacturing or treasury can generate deterministically. Finance must support source traceability, dimensions, controlled manual journals, multi-currency period-end processes, reconciliation and financial statements.

## Existing code to preserve

- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingApi.java`.
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingController.java`.
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java`.
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/TreasuryController.java`.
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/BankReconciliationController.java`.
- Current finance capabilities include chart of accounts, journal create/post/reverse, fiscal-period linkage, operation/version controls, bank accounts, taxes/currencies, FX hints, statement import/matching/reversal and cash position.
- `[EXISTS] frontend finance folders`: `fe/src/app/features/finance/accounts/`, `banks/`, `budgets/`, `journal-entries/`, `tax-currency/`.
- `[EXISTS] fe/src/app/features/fiscal-periods/`.

## Target cycle

`Operational Source → Subledger Event → Posting Profile → Journal Draft/System Batch → Post → Control-Account Reconciliation → Period-End Adjustments/FX → Trial Balance → Financial Statements → Close`

## Data model extensions

### Source journal metadata

Prefer extending existing journal header/line fields before new tables. Every generated journal needs:

```text
source_module
source_document_type
source_document_id
source_document_number
source_event
posting_profile_id
posting_profile_version
fiscal_period_id
transaction_currency
base_currency
exchange_rate
exchange_rate_source
operation_id
```

### Journal dimensions

Normalize references on journal lines where current design permits:

- branch;
- department;
- cost center;
- project/job;
- party;
- item/product category when needed.

Account configuration should define which dimensions are required/allowed. Reject an invalid account-dimension combination server-side.

### Manual journal controls

Extend current journal model with/configure:
- manual/system source indicator;
- approval instance for manual journals over threshold;
- restricted account flag/policy;
- recurring template reference;
- accrual reversal date;
- batch ID;
- evidence attachment/reference;
- mandatory reason/reference for sensitive accounts.

Do not allow a manual journal to impersonate a system source document.

## Posting integration

Implementation ownership:

1. source module calculates business quantity/amount only;
2. shared posting profile resolves accounts/dimensions;
3. finance creates/posts journal using current accounting service;
4. source stores journal reference and posting profile version;
5. reversal goes through existing journal reversal behavior and source reversal rules.

Never let source services directly update GL balances.

## Multi-currency close

### Realized FX
When foreign-currency AR/AP/bank item is settled, compute realized difference using transaction/book rate vs settlement rate and post to configured gain/loss accounts.

### Unrealized FX revaluation
At period end:
1. select open foreign-currency bank/AP/AR balances;
2. resolve approved period-end rate, not merely an external hint;
3. calculate base-currency revalued balance;
4. post unrealized gain/loss batch with source detail;
5. store rate/source/date/version;
6. reverse next period if policy requires.

External rates remain hints unless the current policy explicitly adopts them.

## Financial statements

Build reports from posted ledger, not duplicated summary tables unless they are cache/materialized projections reconciled to GL.

Minimum reports:
- Trial Balance;
- General Ledger detail;
- Balance Sheet;
- Income Statement;
- Cash Flow (indirect is acceptable phase 1);
- AP aging reconciliation;
- AR aging reconciliation;
- Inventory valuation reconciliation;
- Payroll liability reconciliation.

Filters: fiscal period/date range, comparative period, branch, department, cost center, project and currency where meaningful.

## API additions

Follow current finance endpoint style. Suggested query/actions:

```text
GET  /.../accounting/trial-balance
GET  /.../accounting/general-ledger
GET  /.../accounting/statements/balance-sheet
GET  /.../accounting/statements/income-statement
GET  /.../accounting/statements/cash-flow
GET  /.../accounting/reconciliations
POST /.../accounting/manual-journals/<built-in function id>/submit
POST /.../accounting/fx-revaluation/preview
POST /.../accounting/fx-revaluation/post
POST /.../accounting/recurring-journals/run
```

Posting/reversal requests must use `operationId` + current expected-version convention.

## Frontend implementation

### `[MODIFY] finance/accounts/`
Account setup: posting behavior, manual-journal restrictions, required dimensions/control-account designation.

### `[MODIFY] finance/journal-entries/`
Show source module/document link, system/manual badge, approval, posting/reversal chain, dimensions and attachment/reason. System journals should not be editable like manual drafts.

### `[MODIFY] finance/tax-currency/`
Show system transaction rates separately from external hint rates. Add approved period-end rate selection/history if not already represented.

### `[NEW]` financial reporting route/page only after checking current report features
Statement selector, period/comparison, dimension filters, drill-down from balance → account → journal → source document.

### `[MODIFY] fiscal-periods/`
Link close checklist, FX revaluation and reconciliation status.

## Implementation steps

1. Trace journal entity/service/repository behind `AccountingController`.
2. Inventory existing journal metadata/account mapping fields.
3. Add missing source metadata and source→journal link without breaking old manual journals.
4. Add normalized dimensions incrementally; backfill only data that can be derived truthfully.
5. Add account dimension/restricted-account validation.
6. Integrate system posting profiles from `README_04`.
7. Add subledger reconciliation services.
8. Add manual journal approval threshold/restricted account checks.
9. Add realized FX during settlement flows.
10. Add preview/post/reverse workflow for period-end unrealized FX.
11. Build Trial Balance and GL detail first; validate totals against posted journals.
12. Build Balance Sheet/Income Statement; then indirect Cash Flow.
13. Add drill-down and fiscal-close blockers.

## Tests

- system journal source is immutable;
- manual journal cannot use restricted account without policy/approval;
- required dimension missing rejected;
- invalid dimension combination rejected;
- posted debit equals credit;
- duplicate posting creates one journal;
- journal reversal linked to original;
- Trial Balance debit-credit equality;
- statement line drill-down totals exactly equal report value;
- realized FX example exact amount;
- unrealized revaluation exact amount + optional next-period reversal;
- AP/AR/inventory/payroll reconciliation fixtures equal zero;
- closed-period posting rejected.

## Junior developer execution order

1. Open Accounting API/controller and current finance pages.
2. Map current journal model and account configuration.
3. Add source metadata first.
4. Add dimensions/validation.
5. Complete posting profile integration.
6. Add reconciliation queries.
7. Harden manual journal approval/restrictions.
8. Add Trial Balance + GL detail.
9. Add Balance Sheet + Income Statement.
10. Add FX period-end process.
11. Add Cash Flow/report drill-down.
12. Wire close checklist and regression-test every source module.
