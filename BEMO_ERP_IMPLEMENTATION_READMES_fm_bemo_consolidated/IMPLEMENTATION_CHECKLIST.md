# Implementation Checklist

Use this as a progress tracker. A checked box means code + tests + migration + UI/integration (where applicable), not “class created”.

## P0 Shared
- [ ] Operation/idempotency convention standardized
- [ ] Optimistic version conflict standardized
- [ ] Illegal transition guard standardized
- [ ] Approval integration convention documented/implemented
- [ ] Source-document link standardized
- [ ] Posting profile versioning
- [ ] Source-to-journal links
- [x] Subledger reconciliation framework
- [x] Period close checklist framework
- [ ] Reversal/correction rule enforced for posted docs

## Workforce
- [ ] Request dimensions/budget
- [ ] Request approval
- [x] Dispatch
- [x] Worker assignment/site acceptance/replacement
- [x] Attendance lock/correction
- [x] Settlement immutable calculation snapshot
- [x] Dispute workflow
- [ ] Invoice match/tolerance
- [x] Settlement review/approval
- [x] Journal posting & partner ledger
- [ ] GL posting
- [ ] Treasury payment/bank match
- [x] Close provider

## Attendance/Payroll
- [x] Effective-dated attendance rules
- [x] Exception catalogue/severity/block flag
- [x] Payroll input snapshot
- [x] Payroll calendar/period
- [x] Payroll run/status
- [x] Component catalog/evaluators
- [x] Approval
- [ ] GL posting
- [x] Payment batch
- [ ] Retro/off-cycle
- [x] Close/reconciliation

## P2P
- [x] Purchase requisition/lines
- [x] Purchase order/lines
- [ ] Budget/approval
- [x] Goods receipt/lines
- [x] Quality inspection
- [ ] Quality stock disposition
- [x] 3-way matching
- [x] Match tolerances/override
- [x] Vendor invoice/lines
- [ ] AP posting
- [x] Vendor payment
- [ ] Payment proposal
- [ ] Treasury/bank
- [x] Close provider reconciliation

## O2C
- [x] Sales order lines
- [x] Pricing snapshot
- [x] Credit exposure
- [x] Reservation
- [x] Delivery/partial fulfillment
- [x] COGS posting
- [x] Invoice delivered qty
- [x] Returns/RMA/credit note
- [ ] Receipt/bank integration
- [ ] AR reconciliation/close

## Inventory
- [x] Warehouse/bin
- [x] Stock status
- [x] Availability service
- [x] Reservation concurrency
- [x] Transfer
- [x] Cycle count
- [x] Lot/serial/expiry
- [x] Valuation reconciliation

## Manufacturing/Quality
- [x] BOM snapshot
- [x] Material reservation
- [x] Actual issues/returns
- [x] Partial production receipts
- [x] Routing/work centers
- [x] Quality plan/disposition
- [x] WIP posting
- [x] Variance/close

## Treasury/Budget/Close
- [x] Multi-source payment batch
- [x] Maker/checker
- [x] Difference posting
- [x] Budget versions
- [x] Budget revisions/transfers
- [x] All module close providers
- [ ] Workbenches
- [x] Financial/subledger reconciliation reports

## Finance / Master Data / Rules
- [x] Journal source metadata and immutable system journals
- [x] Journal dimensions/account validation
- [x] Manual journal approval/restricted accounts
- [x] Realized/unrealized FX process
- [x] Trial Balance / GL detail
- [x] Balance Sheet / Income Statement / Cash Flow
- [x] Master-data effective dating
- [x] Supplier/contractor bank-change governance
- [x] Typed deterministic rule policies
- [x] Idempotent scheduled jobs
