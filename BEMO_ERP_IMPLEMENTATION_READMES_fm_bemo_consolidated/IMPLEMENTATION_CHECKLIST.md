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
- [ ] Attendance lock/correction
- [ ] Settlement immutable calculation snapshot
- [x] Dispute workflow
- [ ] Invoice match/tolerance
- [ ] GL posting
- [ ] Treasury payment/bank match
- [ ] Close provider

## Attendance/Payroll
- [x] Effective-dated attendance rules
- [x] Exception catalogue/severity/block flag
- [x] Payroll input snapshot
- [x] Payroll calendar/period
- [ ] Payroll run/status
- [ ] Component catalog/evaluators
- [ ] Approval
- [ ] GL posting
- [ ] Payment batch
- [ ] Retro/off-cycle
- [ ] Close/reconciliation

## P2P
- [x] Purchase requisition/lines
- [ ] Budget/approval
- [x] RFQ/quotes/evaluation/award
- [x] Award creates existing PO
- [ ] Quality stock disposition
- [ ] Match tolerances/override
- [ ] AP posting
- [ ] Payment proposal
- [ ] Treasury/bank
- [ ] AP/GRIR close reconciliation

## O2C
- [x] Sales order lines
- [x] Pricing snapshot
- [x] Credit exposure
- [x] Reservation
- [x] Delivery/partial fulfillment
- [ ] COGS posting
- [ ] Invoice delivered qty
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
- [ ] Valuation reconciliation

## Manufacturing/Quality
- [x] BOM snapshot
- [ ] Material reservation
- [x] Actual issues/returns
- [x] Partial production receipts
- [x] Routing/work centers
- [x] Quality plan/disposition
- [ ] WIP posting
- [ ] Variance/close

## Treasury/Budget/Close
- [x] Multi-source payment batch
- [x] Maker/checker
- [ ] Difference posting
- [ ] Budget versions
- [x] Budget revisions/transfers
- [ ] All module close providers
- [ ] Workbenches
- [ ] Financial/subledger reconciliation reports

## Finance / Master Data / Rules
- [ ] Journal source metadata and immutable system journals
- [x] Journal dimensions/account validation
- [x] Manual journal approval/restricted accounts
- [ ] Realized/unrealized FX process
- [x] Trial Balance / GL detail
- [x] Balance Sheet / Income Statement / Cash Flow
- [x] Master-data effective dating
- [x] Supplier/contractor bank-change governance
- [ ] Typed deterministic rule policies
- [ ] Idempotent scheduled jobs
