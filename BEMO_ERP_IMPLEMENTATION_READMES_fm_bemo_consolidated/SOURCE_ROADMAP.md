# BEMO ERP — Business Cycles & Advanced Non‑AI Roadmap

**Repository:** `Mohamed-Hammada/bortqala`  
**Branch:** `fm_bemo_consolidated`  
**Reviewed commit:** `aa3f940cca0119d7f523e03e3fd317fb72684cf3`  
**Review date:** 2026-08-11  
**Review type:** Static code/business-process review of executable backend/frontend paths. Existing Markdown documentation was not used as evidence for implemented behavior.  
**Constraint:** The target solution must not depend on AI/LLMs. All recommendations below use deterministic rules, workflow engines, configuration, calculations, validations, schedulers, and auditable human decisions.

---

## 1. Executive Summary

The current codebase is no longer a simple HR/workforce application. It already contains the foundations of a modular ERP with connected areas for:

- Workforce contractors, workers, labor requests, attendance, settlements, advances and contractor accounts.
- Employee attendance reporting and exception handling.
- Payroll calculation/payment/reversal.
- Procurement, goods receipt, supplier invoices, supplier payments, returns and three-way matching.
- Sales orders, receivables, receipts, customer credit, aging and collections.
- Inventory, UOMs, stock movements, valuation and revaluation.
- Manufacturing BOMs, production orders, material issues, finished-goods receipts and quality inspections.
- General ledger, journals, fiscal periods, treasury configuration, currencies/taxes and bank reconciliation.
- Budgeting and purchase-order encumbrances.
- A configurable approval workflow engine with amount bands, multi-approval, delegation, escalation and history.
- Audit logging and role-based authorization.

From a business perspective, the next major improvement should **not** be adding more disconnected CRUD screens. The next step should be to make every major process a controlled end-to-end business cycle with clear states, ownership, approval gates, accounting effects, exception handling, reconciliation and period close.

### Recommended ERP operating principle

Every important business document should answer these questions:

1. **Why does the document exist?** — source/request/reference.
2. **Who owns it?** — requester, preparer, reviewer, approver, executor.
3. **What state is it in?** — explicit lifecycle, not only `active=true/false`.
4. **What may happen next?** — allowed transitions enforced by backend.
5. **What business rules were applied?** — deterministic and versioned.
6. **What approval was required?** — use the existing approval engine.
7. **What money/stock/workforce effect occurred?** — immutable operational ledger.
8. **What accounting entry occurred?** — linked journal/subledger transaction.
9. **Can it be reversed?** — reversal instead of destructive editing after posting.
10. **Can the period be closed?** — unresolved exceptions must block closing where appropriate.

---

## 2. Current Business Maturity Assessment

| Cycle | Current maturity | Main strength | Main gap to reach advanced ERP behavior |
|---|---|---|---|
| Workforce / Contractor labor | High | Dedicated requests, attendance, settlements, issues, advances, posting/payment | Commercial contract terms, worker allocation/acceptance, disputes, AP reconciliation |
| Attendance-to-Payroll | Medium-High | Exceptions, bulk decisions, approval/reopen, payroll readiness checks | Effective-dated rules, multiple pay calendars, retro/off-cycle/final settlement |
| Payroll | Medium | Calculation, advances, bulk payment, statuses, reversal, explanations | Component/rules engine, statutory components, treasury/GL posting, pay-run control |
| Procure-to-Pay | High foundation | PO, GRN, invoice, payment, returns, FX, three-way matching | Requisition/RFQ/contracts, approval integration, AP schedule, landed cost, accounting closure |
| Order-to-Cash | Medium | SO, credit, AR invoices, receipts/allocations, aging, collections | SO lines, pricing/tax, inventory reservation, shipment/delivery, returns/credit notes, COGS/GL |
| Inventory / Warehouse | Medium-High | Movements, UOM, costing, valuation/revaluation, optional GL posting | Warehouse/bin model, reservations, transfers, cycle counts, lot/serial controls, landed cost |
| Manufacturing | Medium | BOM revision/effective dates, readiness, raw-material issue, FG receipt, scrap/cost | Routing/operations, WIP, labor/overhead, partial production, QC disposition, variance accounting |
| Quality | Basic-Medium | Inspection records and pass/fail quantities | Inspection plans, sampling, quarantine/release, NCR/CAPA, source linkage and disposition |
| Record-to-Report / GL | Medium-High foundation | COA, journal create/post/reverse, fiscal period, operation/version controls | Automatic subledger posting matrix, close checklist, dimensions, statements, reconciliation |
| Treasury / Bank | Medium-High | Bank accounts, statement import, deterministic auto-match, reversal, cash position | Payment batches, approval, bank file, forecast, value dates, fees/interest, bank transfer cycle |
| Budget-to-Control | Medium | Budget, committed/actual/available, blocking flag, PO encumbrance | Budget workflow, revisions/transfers, multi-dimensional control, forecast and close handling |
| Approval / SoD | High foundation | Configurable workflow steps, amounts, min approvals, delegation/escalation/history | Make it mandatory and transactional across all critical document transitions |
| Period Close | Basic-Medium | Fiscal periods and status/version tracking | Operational subledger close, prerequisite checklist, reconciliation, controlled reopen |

---

## 3. Cross-Cycle Architecture Recommendation

Before expanding individual modules, introduce a small set of shared ERP concepts.

### 3.1 Standard document lifecycle

Use a common state vocabulary where appropriate:

`DRAFT → SUBMITTED → PENDING_APPROVAL → APPROVED → EXECUTING → PARTIALLY_COMPLETED → COMPLETED → POSTED → CLOSED`

Alternative terminal states:

`REJECTED`, `CANCELLED`, `REVERSED`.

Not every document needs every state, but the meaning should be consistent across modules.

### 3.2 Standard transition command

For financially or operationally important transitions, require:

- `operationId` — idempotency.
- `expectedVersion` — optimistic concurrency.
- `reason` — mandatory for exception/reversal actions.
- authenticated actor.
- timestamp.
- approval instance ID when applicable.

This pattern already exists in parts of finance and workforce and should become system-wide.

### 3.3 Shared source-document linkage

Add a generic reference structure to transactional documents:

- `sourceDocumentType`
- `sourceDocumentId`
- `sourceDocumentNumber`
- `originatingDepartmentId`
- `branchId`
- `costCenterId`
- `projectId` (optional)
- `externalReference`

This creates traceability such as:

`Purchase Requisition → RFQ → Supplier Quote → PO → GRN → Supplier Invoice → Payment → Journal Entry → Bank Match`

and:

`Sales Quotation → Sales Order → Reservation → Delivery → Invoice → Receipt → Bank Match → Journal Entry`.

### 3.4 Business dimensions

The current system has departments/branches in several areas but accounting should consistently support dimensions on source transactions and journal lines:

- Branch
- Department
- Cost center
- Project / job
- Product/item category
- Warehouse
- Customer/supplier/contractor/employee party

These dimensions should be validated, not stored only as free text.

### 3.5 Central posting profile engine — deterministic, not AI

Create configurable posting rules per document event. Example:

| Business event | Debit | Credit |
|---|---|---|
| Inventory receipt before supplier invoice | Inventory | GR/IR clearing |
| Supplier invoice matched to inventory | GR/IR + recoverable tax | AP supplier control |
| Supplier payment | AP supplier control | Bank/cash |
| Sales invoice | AR customer control | Revenue + output tax |
| Goods delivery | COGS | Inventory |
| Customer receipt | Bank/cash | AR customer control |
| Payroll approval/posting | Salary expense / cost centers | Payroll payable + deduction liabilities |
| Payroll payment | Payroll payable | Bank/cash |
| Contractor settlement | Labor expense / project | Contractor payable |
| Inventory revaluation increase | Inventory | Revaluation gain |

Posting profiles should be effective-dated and versioned. Once a document is posted, store the posting-profile version used.

---

# 4. Workforce / Contractor Labor Cycle

## 4.1 What already exists in code

The workforce module already has substantial business depth:

- Contractor master data including accounting model, payment routing, settlement cycle days, rates and fee structure.
- Dynamic worker categories and default rates/hours.
- Workers linked to contractor/category/branch and attendance mode.
- Labor requests with requested/sent/accepted/variance counts by category.
- Manual attendance matrix with attendance value, check-in/out, actual/overtime/deduction hours and effective daily rate.
- Settlement periods with calculation versions, success/failure metadata, recalculation flag, result totals and warning/error counts.
- Contractor settlement headers, worker-level lines and adjustments.
- Invoice linkage, posting to accounting and payment recording.
- Short/long-term advance concepts, installments, deduction frequency/mode, policy version/snapshot and deferral.
- Excel import/export support.

## 4.2 Target advanced business cycle

Recommended full cycle:

`Labor Need → Labor Request → Approval → Contractor Dispatch → Worker Allocation → Site Acceptance → Daily Attendance → Attendance Review → Settlement Calculation → Exception Resolution → Settlement Review → Finance Approval → Contractor Invoice Match → Accounting Posting → Payment → Bank Reconciliation → Close`

### 4.2.1 Labor need and request

Extend labor requests with:

- Requesting department/project/cost center.
- Work location/site.
- Requested start/end dates.
- Shift/calendar.
- Skill/category.
- Requested quantity.
- Maximum acceptable daily rate.
- Request reason.
- budget reference.

Controls:

- Request cannot be submitted with no items.
- Request cannot exceed budget when blocking control is enabled.
- Required approval based on total estimated cost, site or category.

### 4.2.2 Contractor dispatch and worker allocation

Add a distinct dispatch/allocation document rather than representing only counts.

Suggested entities:

- `LaborDispatch`
- `LaborDispatchLine`
- `WorkerAssignment`

Each worker assignment should record:

- worker ID
- request item ID
- contractor ID
- assignment from/to
- branch/site/project
- agreed daily rate snapshot
- agreed hours snapshot
- status: `PROPOSED / ACCEPTED / REJECTED / REPLACED / COMPLETED`

This allows the business to prove **which workers actually fulfilled a request**, not just requested/sent counts.

### 4.2.3 Site acceptance

On the first attendance date or dispatch date, support:

- accepted worker
- rejected worker
- absent/no-show
- replacement worker
- rejection reason

These should feed contractor performance KPIs.

### 4.2.4 Attendance locking

Recommended states per day/worker:

`OPEN → ENTERED → VALIDATED → APPROVED → LOCKED`

Corrections after approval should create a correction record, not silently overwrite the original value.

### 4.2.5 Settlement calculation

Keep the current calculation-version concept and make each calculation snapshot immutable.

Recommended snapshot inputs:

- worker/category/contractor terms version
- effective daily rate
- standard hours
- overtime rule version
- deduction rule version
- attendance rows included
- approved advances/installments
- contractor commission/fixed fee terms
- manual adjustments

Calculation formula should be explainable line by line.

### 4.2.6 Settlement disputes and claims

Add a dispute cycle:

`OPEN → UNDER_REVIEW → ACCEPTED / PARTIALLY_ACCEPTED / REJECTED → SETTLED`

A dispute may target:

- attendance day
- rate
- overtime
- deduction
- worker count
- contractor fee
- invoice mismatch

Store claimed amount, accepted amount, reason, evidence attachment and approver.

### 4.2.7 Contractor invoice match

Do not only link an invoice number. Perform a deterministic settlement-vs-invoice match:

- settlement net payable
- invoice subtotal
- tax
- withholding
- previous advances
- additions/deductions
- invoice total
- variance
- allowed tolerance

Block posting/payment if variance exceeds tolerance unless an approved override exists.

### 4.2.8 Contractor KPIs

No AI is needed. Use deterministic metrics:

- Fill rate = accepted workers / requested workers.
- No-show rate.
- Replacement rate.
- Attendance compliance.
- Average settlement disputes per period.
- Cost per attendance day.
- Overtime percentage.
- Invoice variance rate.
- On-time invoice rate.

---

# 5. Attendance-to-Payroll Cycle

## 5.1 What already exists

The reporting layer supports:

- period preview and report creation by pay cycle.
- daily decisions and bulk decisions.
- downtime decisions.
- anomaly detection and explicit anomaly decision/reverse/reopen flows.
- holiday proposal decisions.
- report approval and controlled reopen.
- attendance exception checks used as a payroll readiness gate.

This is a strong basis for a controlled time-to-pay process.

## 5.2 Recommended target cycle

`Raw Punch / Manual Entry → Normalization → Daily Result → Exceptions → Reviewer Decision → Manager Approval → Attendance Lock → Payroll Input Snapshot → Payroll Run`

### 5.2.1 Effective-dated attendance rules

Attendance calculations must use rule versions valid on the work date:

- scheduled start/end
- grace period
- minimum hours
- overtime start threshold
- maximum overtime
- break rules
- weekend policy
- holiday policy
- overnight shift handling
- missing-punch policy
- rounding rules

Do not use a single current setting for old periods.

### 5.2.2 Exception catalogue

Create explicit types and severity:

- `MISSING_IN`
- `MISSING_OUT`
- `SINGLE_PUNCH`
- `LATE`
- `EARLY_LEAVE`
- `EXCESSIVE_OVERTIME`
- `ABSENT`
- `WORKED_ON_HOLIDAY`
- `DUPLICATE_PUNCH`
- `OUT_OF_SCHEDULE`
- `NEGATIVE_OR_INVALID_DURATION`

Each type should define whether payroll is blocked until resolved.

### 5.2.3 Payroll input freeze

When attendance is approved, generate a versioned `PayrollInputSnapshot` containing calculated minutes/hours/days and decisions. Payroll should calculate from this immutable snapshot instead of re-reading mutable raw attendance.

---

# 6. Payroll Cycle

## 6.1 Current behavior observed

The payroll service currently provides:

- payroll sheet by year/month/category.
- base salary and attendance-derived overtime/late deductions.
- advance deductions.
- payment and bulk payment.
- status transitions.
- reversal.
- payroll readiness checks against attendance exceptions.
- deterministic explanation records.

At the reviewed service layer, the calculation is centered on a monthly/full-month model and includes hard-coded examples such as base salary divided by 240 hours and overtime multiplied by 1.5.

## 6.2 Major recommendation: separate five stages

Do not combine calculation and payment behavior conceptually.

Use:

1. **Payroll Calendar / Run Creation**
2. **Calculation**
3. **Review & Approval**
4. **Accounting Posting**
5. **Payment & Bank Reconciliation**

Recommended payroll run states:

`DRAFT → INPUT_READY → CALCULATED → VALIDATED → PENDING_APPROVAL → APPROVED → POSTED → PAYMENT_PREPARED → PAID → CLOSED`

Alternative: `CANCELLED / REVERSED`.

## 6.3 Configurable payroll components — deterministic rules

Introduce a component catalog:

### Earnings
- Basic salary
- Attendance days
- Overtime
- Shift allowance
- Transportation allowance
- Housing allowance
- Bonus
- Commission
- Production incentive
- Other earning

### Deductions
- Lateness
- Absence
- Employee advances/installments
- Penalties
- Insurance contribution
- Tax
- Other deduction

### Employer-only costs
- Employer insurance contribution
- Benefits
- Other employer cost

Each component should have:

- code/name
- earning/deduction/employer-cost type
- taxable flag
- pension/insurance flag
- GL account/profile
- calculation basis
- formula/rule ID
- effective from/to
- priority/order
- rounding policy
- min/max cap
- cost-center allocation rule

The rules do not need a scripting engine initially. Support a safe catalog of deterministic operations such as:

- fixed amount
- percentage of another component
- quantity × rate
- bracket/table lookup
- min/max/cap
- conditional eligibility

## 6.4 Multiple payroll calendars

Support:

- Monthly
- Semi-monthly (e.g. 1–15 and 16–end)
- Biweekly
- Weekly
- Off-cycle
- Final settlement

The payroll calendar should generate periods and payment dates; do not infer everything only from `YearMonth`.

## 6.5 Retroactive adjustments

If an approved prior-period attendance/rate correction occurs:

- never rewrite the already-closed payroll result.
- calculate the difference.
- create a retro adjustment in the next open payroll or an approved off-cycle run.

## 6.6 Payroll accounting

On posting, generate a balanced journal batch. Example:

**Debit**
- Salary expense by department/cost center/project.
- Employer contribution expense.

**Credit**
- Payroll payable.
- Tax payable.
- Insurance payable.
- Advance receivable/clearing as appropriate.

At payment:

- Dr Payroll payable
- Cr Bank/cash

Every payroll run must link to the generated journal IDs.

## 6.7 Payment preparation

Create a payment batch with:

- payroll run ID
- employee
- bank/cash method
- bank account
- beneficiary details snapshot
- amount
- payment reference
- execution status

Support maker/checker approval before payment execution.

---

# 7. Procure-to-Pay (P2P)

## 7.1 What already exists

The reviewed code already provides a strong purchase-order-centric cycle:

- PO lines with ordered/received/remaining quantity.
- PO status actions including issue/receive/cancel.
- GRN with accepted/rejected/deducted quantities, warehouse location, lot and quality reason.
- Supplier return.
- Supplier invoice with PO/GRN linkage, discounts/taxes, FX/base currency information, due date and outstanding amount.
- Supplier payment.
- Three-way match with price/quantity variance, tolerance and resolution.
- Budget encumbrance support elsewhere in the codebase.

## 7.2 Recommended target P2P cycle

`Purchase Requisition → Budget Check → Approval → RFQ → Supplier Quotes → Bid Evaluation → Award → PO Approval → PO Issue → Receipt / Service Entry → Quality Decision → Invoice Capture → 2/3-Way Match → AP Approval → Payment Proposal → Treasury Approval → Payment → Bank Reconciliation → AP/GL Reconciliation → Close`

## 7.3 Purchase requisition

Add first-class requisition entities:

- Header: requester, department, branch, cost center, required date, justification, currency.
- Lines: item/service, quantity, UOM, estimated price, preferred supplier, project.

Budget should be checked at requisition submission and committed either at requisition approval or PO issue depending on policy.

## 7.4 Sourcing / RFQ

Add:

- RFQ header and suppliers.
- RFQ lines.
- Supplier quotation header/lines.
- Commercial comparison.
- Award decision.

Deterministic supplier comparison metrics:

- total landed price
- lead time
- payment terms
- historical rejection rate
- historical on-time delivery percentage

The system should show metrics; the final choice remains a human approval.

## 7.5 Supplier contract / framework agreement

For recurring purchases, support:

- validity dates
- maximum amount/quantity
- contracted price
- tier pricing
- delivery SLA
- payment terms
- tax/withholding terms
- approved items
- release-order usage

POs can consume contract quantity/value.

## 7.6 Receiving and quality

Introduce stock disposition:

- `RECEIVED_PENDING_INSPECTION`
- `AVAILABLE`
- `QUARANTINED`
- `REJECTED`
- `RETURN_TO_VENDOR`

Rejected material must never become available inventory.

## 7.7 Landed cost

Support allocation of:

- freight
- customs/duty
- insurance
- handling
- inspection
- other charges

Allocation methods:

- quantity
- weight
- value
- manual percentage

Landed-cost adjustment should update inventory valuation and post the accounting difference.

## 7.8 AP invoice and match controls

Configurable tolerances:

- price variance % / amount
- quantity variance
- freight variance
- tax variance
- early invoice before receipt allowed/not allowed

Exception states:

`MATCHED / WITHIN_TOLERANCE / BLOCKED_PRICE / BLOCKED_QUANTITY / BLOCKED_TAX / MANUAL_REVIEW / RESOLVED`

## 7.9 Payment proposal

Instead of paying invoices one by one only, add a proposal process:

1. Select due invoices.
2. Apply payment terms/discounts.
3. Exclude blocked/disputed invoices.
4. Group by supplier/currency/bank.
5. Validate cash availability.
6. Submit for approval.
7. Execute payment batch.
8. Reconcile bank statement.

---

# 8. Order-to-Cash (O2C)

## 8.1 What already exists

The sales layer currently includes:

- Sales order headers.
- Order confirmation with customer-credit availability check.
- AR invoices and issue action.
- Receipts with invoice allocations and unallocated amount.
- Customer credit profiles and credit hold.
- Aging buckets.
- Collection tasks.

## 8.2 Important structural gap

The reviewed sales-order API is primarily header-level. An advanced ERP cycle needs line-level commercial and fulfillment control.

## 8.3 Target O2C cycle

`Quotation → Customer Approval → Sales Order → Credit Check → Inventory ATP/Reservation → Picking → Packing → Delivery → Customer Acceptance → Invoice → Receipt → Allocation → Collections/Dispute → Bank Reconciliation → AR/GL Reconciliation → Close`

## 8.4 Sales order lines

Add:

- item/service
- quantity/UOM
- unit price
- discount
- tax
- requested/promised date
- warehouse
- reserved quantity
- delivered quantity
- invoiced quantity
- returned quantity
- line status

## 8.5 Pricing

Deterministic pricing engine:

- price list by customer/customer group
- currency
- effective dates
- quantity tiers
- promotional discount dates
- manual discount threshold requiring approval
- tax code

Store price-rule version/snapshot on the order line.

## 8.6 Available-to-Promise and reservation

At confirmation:

- available stock = on-hand − reservations − blocked/quarantine stock.
- reserve if policy says so.
- create shortage/backorder if insufficient.

Do not allow the same stock to be promised to multiple confirmed orders.

## 8.7 Delivery

Create delivery documents supporting:

- partial delivery
- picking quantities
- warehouse/bin/lot/serial
- delivery note number
- carrier
- dispatch/receipt time
- customer acceptance/rejection

Delivery should create inventory issue and COGS posting when configured.

## 8.8 Returns and credit notes

Add RMA/customer return cycle:

`REQUESTED → AUTHORIZED → RECEIVED → INSPECTED → RESTOCK / SCRAP / REPAIR → CREDIT_NOTE / REPLACEMENT → CLOSED`

## 8.9 Credit management

Use credit exposure:

`Open AR + Confirmed uninvoiced orders + Delivered uninvoiced value − approved deposits/guarantees`

Configurable action when exceeding credit:

- warn
- block confirmation
- require credit-manager approval

---

# 9. Inventory / Warehouse Cycle

## 9.1 Existing strengths

Inventory already supports:

- item master and categories.
- UOM and conversions.
- stock movement transactions.
- negative-balance reporting.
- stock adjustments.
- valuation policy.
- movement costing.
- revaluation.
- configurable inventory/offset/COGS/adjustment accounts and optional GL posting.

## 9.2 Advanced warehouse model

Add hierarchy:

`Warehouse → Zone → Bin`

Inventory balance key should eventually be:

`item + warehouse + bin + lot/serial + stock status`.

## 9.3 Stock statuses

Use:

- Available
- Reserved
- Quality hold
- Quarantine
- Damaged
- Rejected
- In transit

## 9.4 Warehouse transfers

Cycle:

`Transfer Request → Approval (optional) → Pick → Dispatch → In Transit → Receive → Close`

Two-sided movements prevent inventory disappearing between warehouses.

## 9.5 Physical count / cycle count

Cycle:

`Count Plan → Freeze/Count Scope → Blind Count → Recount if variance → Approval → Adjustment Posting → Close`

Controls:

- counter should not see theoretical quantity during blind count.
- large variance requires second count/approval.
- adjustment reason mandatory.

## 9.6 Lot / serial controls

Item configuration should specify:

- none / lot / serial tracking
- expiry required
- minimum shelf-life at receipt
- FIFO/FEFO picking policy

## 9.7 Costing

Keep valuation policy versioned. If supporting multiple methods, make method changes effective only at controlled boundaries. Do not silently recalculate previously closed inventory periods.

---

# 10. Manufacturing & Quality Cycle

## 10.1 What exists

Manufacturing currently includes:

- BOM header/lines with revision, effective dates, yield and waste percent.
- Production orders with target/output/scrap and actual material/unit cost.
- Material-readiness check against stock.
- Raw-material issue when starting a production order.
- Finished-goods receipt when completing.
- Cancellation with material issue reversal.
- Quality inspection records.

## 10.2 Target production cycle

`Demand → Production Plan → Planned Order → Material Check/Reservation → Release → Material Issue → Operations/Execution → In-Process QC → Partial Output → Final QC → Finished Goods Receipt → Variance Calculation → Accounting Posting → Close`

## 10.3 BOM improvements

Add:

- approved/released status.
- alternate BOM.
- substitute components.
- scrap/waste reason.
- co-products/by-products where required.
- change approval and revision history.

A production order must preserve a BOM snapshot so later BOM edits do not change historical costing.

## 10.4 Routing / work centers

Add:

- Work center
- Routing
- Routing operation
- setup time
- run time
- labor rate
- machine/overhead rate
- capacity per shift

This enables true production cost beyond raw materials.

## 10.5 Material reservation and actual consumption

Do not consume the whole planned BOM automatically only because an order started. Advanced behavior should support:

- reserve planned material.
- issue actual material in one or multiple issues.
- return unused material.
- record substitute material.
- record excess consumption with reason.

## 10.6 Partial production

Allow multiple production receipts against one work order:

- good quantity
- rejected quantity
- rework quantity
- scrap
- lot/batch
- timestamp/shift

Order completion occurs only when user intentionally closes remaining quantity.

## 10.7 WIP accounting

Recommended accounting:

On material issue:
- Dr WIP
- Cr Raw Material Inventory

On labor/overhead absorption:
- Dr WIP
- Cr Labor/Overhead Absorption

On finished goods receipt:
- Dr Finished Goods Inventory
- Cr WIP

At close:
- production variance posted to configured variance accounts.

## 10.8 Quality management

The current generic inspection should evolve into:

- inspection plans by item/source.
- characteristics (numeric/boolean/list).
- sample size.
- specification min/max.
- inspection result.
- disposition.

Source types:

- Purchase receipt
- Production in-process
- Finished production
- Customer return

Disposition:

`ACCEPT / CONDITIONAL_ACCEPT / QUARANTINE / REWORK / SCRAP / RETURN_TO_VENDOR`

Add NCR (non-conformance) and CAPA only if business requires formal quality management.

---

# 11. Finance / Record-to-Report (R2R)

## 11.1 What exists

Finance already has:

- Chart of accounts.
- Journal creation.
- Journal posting.
- Journal reversal.
- fiscal-period linkage.
- journal operation IDs and optional expected version controls.
- bank accounts.
- taxes and currencies.
- online exchange-rate **hints** while preserving system rates.
- bank statement import, auto/manual match, match reversal and cash position.
- fiscal period generation and status update.

## 11.2 Main ERP requirement: automatic subledger closure

The GL should not rely on users manually recreating the financial effect of business documents.

Every posted source document should link to a journal entry, and every generated journal entry should retain:

- source module
- source document type
- source document ID/number
- source event
- posting profile/version
- fiscal period
- transaction currency
- base currency
- exchange rate and source

## 11.3 Financial dimensions

Extend journal lines with normalized dimensions:

- branch
- department
- cost center
- project
- party
- item category/product if needed

Validate allowed combinations based on account configuration.

## 11.4 Journal controls

Add/configure:

- manual journal approval threshold.
- restricted accounts not usable by ordinary manual journals.
- recurring journal template.
- accrual/reversal date.
- journal batch.
- attachment/evidence.
- mandatory reference/reason for sensitive accounts.

## 11.5 Multi-currency close

Add deterministic period-end processes:

- realized FX gain/loss on settlement.
- unrealized revaluation of foreign-currency bank/AP/AR balances.
- reversal of unrealized revaluation next period when policy requires.

## 11.6 Financial statements

Build from posted ledger/dimensions:

- Trial Balance
- General Ledger detail
- Balance Sheet
- Income Statement
- Cash Flow (indirect first is sufficient)
- AP aging reconciliation
- AR aging reconciliation
- Inventory valuation reconciliation
- Payroll liability reconciliation

Reports should support period, comparative period and dimensions.

---

# 12. Treasury & Bank Cycle

## 12.1 Current strengths

The system has bank account master data, statement import, matching, reversal and cash position. This is enough to build a proper treasury execution cycle.

## 12.2 Target treasury cycle

`Approved Payables → Payment Proposal → Funding/Bank Selection → Payment Batch → Approval → Execution → Bank Statement → Match → Fees/Differences → Reconciliation Close`

## 12.3 Payment batch

A single payment batch can include:

- supplier invoices
- contractor settlements
- payroll payments
- expense/other approved payments

Fields:

- source type/id
- beneficiary
- beneficiary bank snapshot
- currency/amount
- source bank account
- payment date/value date
- payment method
- reference
- status

## 12.4 Bank reconciliation rules — deterministic

Auto-match can score using rules, not AI:

1. exact reference + exact amount.
2. exact amount + date within configured days.
3. invoice/payment reference token match.
4. grouped batch total.

Never auto-post ambiguous matches. Present candidates for human selection.

## 12.5 Bank differences

Support explicit posting for:

- bank fee
- interest
- withholding
- FX difference
- transfer fee
- unidentified receipt/payment suspense

---

# 13. Budget-to-Control Cycle

## 13.1 What exists

Budget code already tracks:

- planned amount.
- department.
- period/year.
- blocking/non-blocking mode.
- committed amount.
- actual amount.
- available amount/utilization.
- PO encumbrances with committed/liquidated/released amounts.

## 13.2 Target cycle

`Budget Preparation → Review → Approval → Active Budget → Requisition Check → Commitment → Actualization → Revision/Transfer → Forecast → Period/Year Close`

## 13.3 Budget dimensions

Recommended budget key:

`fiscal year + period + branch + department + cost center + account/category + project (optional) + currency`.

Start with department + account/category if full dimensional budgeting is too large for phase 1.

## 13.4 Budget revisions and transfers

Never overwrite the original approved budget without history.

Add:

- budget version
- original budget
- approved revisions
- transfers in/out
- current budget

Revision cycle:

`DRAFT → SUBMITTED → APPROVED → APPLIED`

## 13.5 Commitment policy

Define when funds are reserved:

- requisition approval, or
- PO approval/issue.

Define when commitment is released:

- PO cancellation.
- undelivered remainder closed.
- invoice/actual replaces commitment.

---

# 14. Approval, Segregation of Duties & Audit

## 14.1 Existing approval engine

The code already supports:

- workflow by document type.
- multiple steps.
- role or user approver.
- amount-from/to ranges.
- minimum number of approvals.
- self-approval option.
- workflow versioning and document snapshot.
- delegation.
- reassignment.
- due dates/escalation.
- approval history.

This should become a **shared mandatory control layer**.

## 14.2 Recommended integration points

Require approval at configurable thresholds for:

- labor request.
- settlement approval.
- worker/employee advance.
- purchase requisition.
- purchase order.
- supplier invoice exception override.
- payment proposal/batch.
- sales discount override.
- customer credit-limit change.
- inventory adjustment/revaluation.
- BOM revision release.
- production variance above tolerance.
- payroll run approval.
- manual journal posting.
- budget/revision/transfer.
- fiscal-period reopen.

## 14.3 Segregation of duties examples

Configurable SoD rules should prevent, for example:

- requester approving own purchase when self-approval disabled.
- supplier master creator approving first payment to same supplier.
- payroll preparer approving and executing same payroll batch above threshold.
- inventory counter approving own large adjustment.
- journal creator approving/posting sensitive manual journal if policy requires checker.

Do not hard-code every SoD combination. Store role/action conflicts in configuration.

---

# 15. Fiscal Period & Month-End Close

## 15.1 Current state

Fiscal periods can be generated and their status changed with optimistic version validation. That is a good foundation, but an advanced ERP needs a close process, not only a status switch.

## 15.2 Recommended period statuses

`OPEN → SOFT_CLOSE → CLOSING → CLOSED`

Exceptional transition:

`CLOSED → REOPENED` only through privileged approval and mandatory reason.

## 15.3 Close checklist

Before closing, calculate blockers:

### Inventory
- negative stock requiring resolution.
- unposted/rejected adjustments.
- incomplete transfers.
- inventory valuation not generated/reconciled.

### Procurement/AP
- unmatched invoices above tolerance.
- received-not-invoiced balances reviewed.
- unposted supplier invoices/payments.

### Sales/AR
- unposted invoices/receipts.
- unapplied receipts reviewed.
- AR aging reconciled to control account.

### Payroll/workforce
- open attendance exceptions.
- payroll run not closed.
- contractor settlement pending posting/payment where policy blocks close.

### Banking
- bank statements not reconciled.
- suspense/unmatched lines above threshold.

### GL
- draft journals.
- unbalanced journals must already be impossible.
- subledger-to-GL reconciliation differences.
- FX revaluation incomplete where applicable.

Show each prerequisite as `PASS / WARNING / BLOCKER` with drill-down.

---

# 16. Master Data Governance

Advanced cycles will fail if master data is uncontrolled. Introduce governance for:

## 16.1 Party master

A party can have one or more roles:

- customer
- supplier
- contractor
- employee/worker relation where appropriate

Avoid duplicate tax IDs/bank accounts based on configurable checks.

## 16.2 Supplier/customer banking changes

Bank-account change should be audited and optionally require approval, especially before payment.

## 16.3 Item master

Recommended fields:

- item type: stock/service/raw/WIP/finished/expense.
- base UOM.
- purchase UOM.
- sales UOM.
- costing method/category.
- lot/serial/expiry policy.
- default tax.
- default inventory/revenue/COGS/variance profiles.

## 16.4 Effective dating

Use effective dates for:

- employee/worker rates.
- contractor commercial terms.
- payroll rules.
- tax rates.
- FX rates.
- BOM revisions.
- price lists.
- budget versions.
- posting profiles.

Historical documents must preserve the version used.

---

# 17. Non-AI Automation Strategy

The requested architecture can be advanced without any AI feature.

Use these mechanisms:

### 17.1 Rules tables
Examples:

- approval amount thresholds.
- three-way matching tolerance.
- overtime multiplier.
- late/absence deduction rules.
- customer credit policy.
- reorder/min-max values.
- quality acceptance tolerances.
- budget blocking limits.

### 17.2 State machines
Explicit backend transition tables for each document.

### 17.3 Scheduled jobs
Examples:

- generate payroll/settlement periods.
- flag overdue invoices.
- escalate approval tasks.
- refresh exchange-rate hints.
- generate recurring journals.
- expire reservations/contracts/price lists.

### 17.4 Deterministic recommendations
The UI can provide useful “recommendations” without AI, such as:

- “Invoice blocked: quantity variance 7.2% > tolerance 5%.”
- “Customer credit exceeded by EGP 25,000.”
- “18 workers requested; 15 accepted; shortage = 3.”
- “Material X shortage = 120 kg.”
- “Budget available = EGP 40,000; request = EGP 55,000.”

Every recommendation should show the formula/input values.

---

# 18. Suggested New/Extended Backend Components

The following names are implementation suggestions, not claims about current files.

## Shared

- `DocumentTransitionService`
- `BusinessDimensionService`
- `PostingProfileService`
- `SubledgerPostingService`
- `ReconciliationService`
- `CloseChecklistService`
- `RuleVersionService`

## Procurement

- `PurchaseRequisition*`
- `Rfq*`
- `SupplierQuotation*`
- `SourcingAward*`
- `SupplierContract*`
- `PaymentProposal*`
- `LandedCost*`

## Sales

- `SalesOrderLine`
- `PriceList*`
- `InventoryReservation*`
- `Delivery*`
- `CustomerReturn*`
- `CreditNote*`

## Inventory

- `Warehouse`
- `WarehouseBin`
- `InventoryBalance`
- `InventoryReservation`
- `WarehouseTransfer*`
- `StockCount*`
- `Lot/Serial*`

## Manufacturing

- `Routing*`
- `WorkCenter*`
- `ProductionMaterialIssue*`
- `ProductionReceipt*`
- `ProductionOperation*`
- `ProductionCost/Variance*`

## Payroll

- `PayrollCalendar`
- `PayrollRun`
- `PayrollRunEmployee`
- `PayrollComponentDefinition`
- `PayrollComponentResult`
- `PayrollRuleVersion`
- `PayrollPaymentBatch`
- `PayrollPostingBatch`

---

# 19. Frontend UX Recommendation: Cycle Workbenches

Avoid forcing users to jump through many unrelated menu pages. Add “workbench” screens for each cycle.

## 19.1 Procurement Workbench

Tabs/cards:

- Requisitions awaiting approval
- RFQs awaiting quotes
- POs due/late
- Receipts pending inspection
- Invoice match exceptions
- Invoices due for payment

## 19.2 Sales Workbench

- Orders blocked by credit
- Orders waiting stock
- Deliveries due today
- Uninvoiced deliveries
- Overdue invoices
- Unallocated receipts

## 19.3 Payroll Workbench

- input readiness
- attendance blockers
- payroll run status
- variance vs previous period
- approval tasks
- posting status
- payment batch status

## 19.4 Period Close Workbench

One page showing all close prerequisites and drill-down actions.

### UX rule

Every workbench action should show:

- current state
- blocking reason
- next allowed action
- role required
- financial/stock impact where relevant

---

# 20. Business KPIs — No AI Required

## Workforce
- request fill rate
- worker acceptance/no-show rate
- labor cost per day/hour/project
- overtime ratio
- settlement variance/dispute rate

## Procurement
- requisition-to-PO lead time
- supplier OTIF
- purchase price variance
- rejection rate
- three-way match first-pass rate
- invoice-to-payment lead time

## Sales
- order fulfillment rate
- on-time delivery
- gross margin
- DSO
- overdue AR percentage
- credit utilization

## Inventory
- inventory turnover
- stockout rate
- negative stock count
- count accuracy
- obsolete/slow-moving stock
- valuation by category/warehouse

## Manufacturing
- material yield
- scrap percentage
- planned vs actual consumption
- planned vs actual output
- production lead time
- unit-cost variance

## Finance
- days to close
- unreconciled bank value
- AP/AR-to-GL difference
- manual journal count/value
- budget utilization/variance

---

# 21. Critical Business Controls to Implement First

## P0 — ERP integrity controls

1. Standardize lifecycle/status transitions and idempotency/version checks.
2. Integrate the existing approval engine with critical transactions.
3. Introduce central source-document → journal posting profiles.
4. Implement subledger-to-GL reconciliation.
5. Build period-close checklist/blockers.
6. Create immutable snapshots for approved attendance, payroll, settlement and commercial terms.
7. Prevent destructive edits after posting; require reversal/correction.

## P1 — Complete core end-to-end cycles

1. Procurement requisition/RFQ/quotation/award.
2. Sales lines/reservations/delivery/returns.
3. Payroll run/component/calendar/posting/payment separation.
4. Warehouse/bin/reservation/transfer/count.
5. Contractor dispatch/worker assignment and invoice matching.
6. Payment proposal/batch across AP/payroll/contractors.

## P2 — Advanced operational control

1. Manufacturing routing/WIP/partial production/variance.
2. Quality inspection plans and quarantine/disposition.
3. Landed cost.
4. Budget revisions/transfers/forecast.
5. Multi-currency revaluation.
6. recurring/accrual journals.

## P3 — Enterprise expansion if needed

Only if required by target customers:

- multi-company/intercompany.
- fixed assets.
- consolidation.
- formal CAPA/QMS.
- advanced MRP/capacity planning.
- project accounting.

Do not implement these merely because large ERPs have them; make them customer-driven modules.

---

# 22. Detailed Acceptance Scenarios for QA

The following scenarios should eventually pass end-to-end.

## Scenario A — Contractor labor settlement

1. Department requests 20 workers of category A for a site.
2. Request passes budget and approval.
3. Contractor sends 22 proposed workers.
4. Site accepts 20, rejects 2 with reasons.
5. Attendance is entered for the settlement period.
6. Exceptions are resolved.
7. Settlement calculation generates version 1.
8. A late correction causes `needsRecalculation=true`.
9. Version 2 is calculated with traceable differences.
10. Settlement is reviewed and finance-approved.
11. Contractor invoice is matched; variance within tolerance.
12. Posting creates contractor payable journal.
13. Payment batch pays contractor.
14. Bank statement line is matched.
15. Period close shows no blocker.

## Scenario B — Purchase-to-pay

1. User creates requisition.
2. Budget is sufficient and commitment is created.
3. Approval completed.
4. RFQ sent to three suppliers.
5. Quotes compared; one supplier awarded.
6. PO approved/issued.
7. Supplier delivers partially; GRN records rejected quantity.
8. Accepted quantity becomes available only after quality release if inspection required.
9. Supplier invoice is entered.
10. Three-way match finds variance.
11. Variance over tolerance blocks payment until approved resolution.
12. Invoice posts to AP/GL.
13. Payment proposal selects invoice at due date.
14. Treasury approves and pays.
15. Bank statement auto-match finds exact reference/amount.
16. AP control account reconciles.

## Scenario C — Sales-to-cash

1. Sales order has multiple lines.
2. Credit check passes.
3. Inventory is reserved.
4. Partial delivery issued; stock and COGS are posted.
5. Invoice created for delivered quantity only.
6. Customer pays partially.
7. Receipt allocation reduces invoice outstanding.
8. Aging shows remaining amount.
9. Second receipt closes invoice.
10. Bank reconciliation matches both receipts.
11. AR control account reconciles to customer balances.

## Scenario D — Payroll

1. Attendance period is open and contains exceptions.
2. Payroll run creation reports blockers.
3. HR resolves/approves exceptions and locks attendance snapshot.
4. Payroll calculates configurable components.
5. User compares current vs previous run and reviews anomalies using deterministic thresholds.
6. Payroll run is approved by authorized role.
7. Posting generates expense/liability journal.
8. Payment batch is created and approved.
9. Payment is executed.
10. Bank statement is reconciled.
11. Payroll payable account becomes zero for paid items.
12. Closed run cannot be edited; correction requires retro/off-cycle/reversal flow.

## Scenario E — Manufacturing

1. Released BOM revision is selected.
2. Production order snapshots BOM.
3. Material shortage blocks release/start.
4. Procurement/stock transfer resolves shortage.
5. Materials are reserved and issued partially.
6. Production records first partial finished quantity.
7. QC quarantines part of output.
8. Remaining output is completed.
9. Material/labor/overhead actual cost is accumulated in WIP.
10. Finished goods are valued and received.
11. Production variance is calculated and posted.
12. Work order closes and can no longer be silently changed.

---

# 23. Code Areas Reviewed

The conclusions above were based on executable code paths including, but not limited to:

### Frontend routing
- `fe/src/app/app.routes.ts`
- `fe/src/app/features/workforce/workforce.routes.ts`

### Workforce
- `be/src/main/java/com/bemo/hr/workforce/WorkforceApi.java`
- `be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementController.java`
- workforce entities/services/controllers visible under `be/src/main/java/com/bemo/hr/workforce/`

### Attendance/reporting
- `be/src/main/java/com/bemo/hr/reporting/api/ReportController.java`
- attendance-exception API/service structure under `be/src/main/java/com/bemo/hr/reporting/`

### Payroll
- `be/src/main/java/com/bemo/hr/payroll/api/PayrollApi.java`
- `be/src/main/java/com/bemo/hr/payroll/api/PayrollController.java`
- `be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java`

### Procurement
- `be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementApi.java`
- `be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementController.java`

### Sales
- `be/src/main/java/com/bemo/hr/trade/sales/api/SalesApi.java`
- `be/src/main/java/com/bemo/hr/trade/sales/api/SalesController.java`

### Inventory / operations
- `be/src/main/java/com/bemo/hr/operations/OperationsApi.java`
- `be/src/main/java/com/bemo/hr/operations/OperationsController.java`
- inventory valuation/cost-layer classes visible under the operations package

### Manufacturing
- `be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingApi.java`
- `be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingController.java`
- `be/src/main/java/com/bemo/hr/manufacturing/production/application/ManufacturingService.java`

### Finance / treasury
- `be/src/main/java/com/bemo/hr/finance/api/AccountingApi.java`
- `be/src/main/java/com/bemo/hr/finance/api/AccountingController.java`
- `be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java`
- `be/src/main/java/com/bemo/hr/finance/api/TreasuryController.java`
- `be/src/main/java/com/bemo/hr/finance/api/BankReconciliationController.java`

### Budget
- `be/src/main/java/com/bemo/hr/budget/api/BudgetApi.java`
- `be/src/main/java/com/bemo/hr/budget/api/BudgetController.java`

### Approval
- `be/src/main/java/com/bemo/hr/approval/ApprovalApi.java`
- `be/src/main/java/com/bemo/hr/approval/ApprovalController.java`

---

# 24. Implementation Rule: Extend, Do Not Duplicate

Several capabilities that are often recommended for ERP systems **already exist in this branch**. New development should reuse them:

- Use the existing approval engine instead of creating a new approval table per module.
- Use existing audit infrastructure instead of ad-hoc text logs.
- Use existing journal posting/reversal concepts rather than direct balance updates.
- Use existing inventory valuation services instead of creating parallel costing logic inside procurement/manufacturing.
- Use existing budget encumbrance concepts instead of a second commitment ledger.
- Use existing workforce settlement calculation versioning/issues rather than replacing the settlement module.
- Keep external FX rates as reference/hints unless a human/system policy chooses the transaction rate.

The goal is to transform the existing modules into one coherent ERP transaction network.

---

# 25. Recommended Development Sequence

A practical sequence with lowest architectural rework is:

### Phase 1 — Shared controls

1. Common transition/idempotency/version contract.
2. Approval-engine integration adapter.
3. Posting-profile/subledger adapter.
4. shared dimensions.
5. close-checklist framework.

### Phase 2 — Financially close current modules

1. Workforce settlement → AP/GL → payment → bank.
2. Payroll → GL → payment → bank.
3. Procurement → AP/GL → payment → bank.
4. Sales → inventory/COGS/AR/GL → receipt → bank.
5. Inventory → GL reconciliation.

### Phase 3 — Fill operational gaps

1. Requisition/RFQ/sourcing.
2. Sales fulfillment/returns.
3. Warehouse transfer/count/reservation.
4. Contractor dispatch/allocation/dispute.
5. Payroll component/calendar engine.

### Phase 4 — Manufacturing depth

1. routings/work centers.
2. actual material issue/return.
3. partial production.
4. WIP/labor/overhead costing.
5. quality disposition.
6. production variance close.

### Phase 5 — Closing/reporting maturity

1. subledger reconciliation dashboards.
2. period-close workbench.
3. financial statements.
4. FX revaluation.
5. budget revisions/forecast.

---

# 26. Definition of Done for an “Advanced Cycle”

A cycle should not be considered finished merely because its screens and APIs exist. It is complete when:

- all important states and transitions are enforced server-side.
- source/target documents are linked and traceable.
- approval rules are configurable.
- duplicate execution is prevented by idempotency.
- concurrent edits are detected.
- posted transactions are immutable and reversible.
- stock/financial effects are reconciled.
- GL posting is automatic or explicitly controlled.
- exceptions have a queue and resolution path.
- audit history identifies actor/time/reason.
- effective-dated rule versions are preserved.
- period close can identify unfinished work.
- reports/KPIs derive from transactional data.
- QA has positive, negative, partial, reversal and concurrency scenarios.

---

# 27. Final Business Recommendation

The project should be positioned internally as a **transaction-and-control ERP**, not a collection of forms. The codebase already has enough foundations that the highest-value work is now integration and control.

The most important design decision is this:

> A business action should create one traceable chain from operational source, through approval and execution, to accounting, payment/reconciliation and period close.

For this branch, the recommended priorities are therefore:

1. unify lifecycle/approval/posting/reversal patterns;
2. financially close every existing operational cycle;
3. add the missing upstream/downstream documents around the already strong procurement/workforce foundations;
4. deepen sales fulfillment and payroll-run architecture;
5. evolve inventory/manufacturing toward reservations, WIP, partial execution and quality disposition;
6. add a real month-end close workbench and reconciliation layer;
7. keep all automation deterministic, configurable and auditable — **no AI dependency is necessary**.

