# Implementation Checklist — Code-Verified Status

> **Repository:** `Mohamed-Hammada/bortqala`  
> **Branch:** `fm_bemo_consolidated`  
> **Developer-fix checkpoint:** `a102a92a9127ab13f862048a2586a933efa50912`  
> **Reviewed HEAD:** `8595cfa1b600f2bb4ac39fa52d32debcca5cb2ce`  
> **Review date:** 2026-08-12
>
> This file replaces the previous all-checked checklist.
>
> **Strict rule:** a Markdown checkbox is checked only when the item has been verified end-to-end against its applicable Definition of Done. A class/controller/entity/migration existing in source is **not** sufficient by itself.

## Status legend

- `[x] VERIFIED` — end-to-end implementation and applicable acceptance evidence verified.
- `[ ] SOURCE PRESENT` — substantial source implementation exists, but complete runtime/DoD verification is not established.
- `[ ] PARTIAL` — implementation exists but a known business/integration gap remains.
- `[ ] VERIFY` — source/supporting pieces exist, but the complete behavior was not proven by the audit.
- `[ ] MISSING` — required implementation was not found after focused verification.

---

## P0 Shared

- [ ] **VERIFY** — Operation/idempotency convention standardized
- [ ] **VERIFY** — Optimistic version conflict standardized
- [ ] **VERIFY** — Illegal transition guard standardized
- [ ] **VERIFY** — Approval integration convention documented/implemented
- [ ] **VERIFY** — Source-document link standardized
- [ ] **VERIFY** — Posting profile versioning
- [ ] **VERIFY** — Source-to-journal links
- [ ] **PARTIAL** — Subledger reconciliation framework — provider integration exists, but the current default AP provider returns synthetic `0` GL vs `0` subledger balances.
- [ ] **SOURCE PRESENT** — Period close checklist framework — close checklist/provider framework exists; authoritative reconciliation must be completed before this is verified.
- [ ] **VERIFY** — Reversal/correction rule enforced for posted docs

## Workforce

- [ ] **SOURCE PRESENT / VERIFY** — Request dimensions/budget
- [ ] **SOURCE PRESENT / VERIFY** — Request approval
- [ ] **SOURCE PRESENT / VERIFY** — Dispatch
- [ ] **SOURCE PRESENT / VERIFY** — Worker assignment/site acceptance/replacement
- [ ] **SOURCE PRESENT / VERIFY** — Attendance lock/correction
- [ ] **SOURCE PRESENT / VERIFY** — Settlement immutable calculation snapshot
- [ ] **SOURCE PRESENT / VERIFY** — Dispute workflow
- [ ] **SOURCE PRESENT / VERIFY** — Invoice match/tolerance
- [ ] **SOURCE PRESENT / VERIFY** — Settlement review/approval
- [ ] **SOURCE PRESENT / VERIFY** — Journal posting & partner ledger
- [ ] **SOURCE PRESENT / VERIFY** — GL posting
- [ ] **SOURCE PRESENT / VERIFY** — Treasury payment/bank match
- [ ] **SOURCE PRESENT / VERIFY** — Close provider

**Why not checked:** the codebase contains both older workforce paths and newer domain/application/API structures. Before checking these items, prove that legacy endpoints cannot bypass the full approved request → dispatch → attendance → settlement → dispute/adjustment → invoice → GL → Treasury → bank → close flow.

## Attendance / Payroll

- [ ] **SOURCE PRESENT / VERIFY** — Effective-dated attendance rules
- [ ] **SOURCE PRESENT / VERIFY** — Exception catalogue/severity/block flag
- [ ] **SOURCE PRESENT** — Payroll input snapshot
- [ ] **SOURCE PRESENT** — Payroll calendar/period
- [ ] **PARTIAL** — Payroll run/status
- [ ] **SOURCE PRESENT / PARTIAL** — Component catalog/evaluators
- [ ] **SOURCE PRESENT / VERIFY** — Approval
- [ ] **SOURCE PRESENT / VERIFY** — GL posting
- [ ] **SOURCE PRESENT / VERIFY** — Payment batch
- [ ] **SOURCE PRESENT / VERIFY** — Retro/off-cycle
- [ ] **PARTIAL** — Close/reconciliation

**Known payroll blocker:** `PayrollExecutionService.addRunLine(...)` still accepts caller-supplied `basicSalary`, `allowances`, and `deductions`, and `calculateRun(...)` aggregates those values. The immutable/versioned payroll-input and component engine must become authoritative for normal payroll; manual corrections must be explicit controlled adjustments.

## P2P / Procurement

- [ ] **SOURCE PRESENT / VERIFY** — Purchase requisition/lines
- [ ] **SOURCE PRESENT / VERIFY** — Purchase order/lines
- [ ] **SOURCE PRESENT / VERIFY** — Budget/approval
- [ ] **SOURCE PRESENT / VERIFY** — Goods receipt/lines
- [ ] **SOURCE PRESENT / VERIFY** — Quality inspection
- [ ] **SOURCE PRESENT / VERIFY** — Quality stock disposition
- [ ] **SOURCE PRESENT / VERIFY** — 3-way matching
- [ ] **SOURCE PRESENT / VERIFY** — Match tolerances/override
- [ ] **SOURCE PRESENT / VERIFY** — Vendor invoice/lines
- [ ] **SOURCE PRESENT / VERIFY** — AP posting
- [ ] **SOURCE PRESENT / VERIFY** — Vendor payment
- [ ] **SOURCE PRESENT / VERIFY** — Payment proposal
- [ ] **SOURCE PRESENT / VERIFY** — Treasury/bank
- [ ] **PARTIAL** — Close provider reconciliation

**Known procurement blocker:** current sourcing now maps real `SupplierQuoteLine` records into PO lines when present, but if quote lines are absent it still creates a synthetic `ITEM-1`, qty `1`, UOM `PCS` line using the quote total. Source-line integrity is therefore not guaranteed.

Required traceability: `PurchaseRequisitionLine -> RFQLine -> SupplierQuoteLine -> SourcingAward -> PurchaseOrderLine`.

## O2C / Sales

- [ ] **SOURCE PRESENT / VERIFY** — Sales order lines
- [ ] **SOURCE PRESENT / VERIFY** — Pricing snapshot
- [ ] **SOURCE PRESENT / VERIFY** — Credit exposure
- [ ] **PARTIAL** — Reservation
- [ ] **PARTIAL** — Delivery/partial fulfillment
- [ ] **PARTIAL / VERIFY** — COGS posting
- [ ] **PARTIAL / VERIFY** — Invoice delivered qty
- [ ] **PARTIAL** — Returns/RMA/credit note
- [ ] **SOURCE PRESENT / VERIFY** — Receipt/bank integration
- [ ] **PARTIAL / VERIFY** — AR reconciliation/close

**Known sales blocker:** current `SalesOrderFullService.createDelivery(...)` creates the delivery, then immediately calls `ship()` and `deliver()`. That does not prove a staged allocation/pick/partial-shipment/inventory/COGS/invoice lifecycle.

## Inventory

- [ ] **SOURCE PRESENT / PARTIAL** — Warehouse/bin
- [ ] **VERIFY** — Stock status
- [ ] **PARTIAL** — Availability service
- [ ] **PARTIAL** — Reservation concurrency
- [ ] **VERIFY** — Transfer
- [ ] **VERIFY** — Cycle count
- [ ] **VERIFY** — Lot/serial/expiry
- [ ] **VERIFY** — Valuation reconciliation

**Current verified progress:** a dedicated `inventory` package exists with `InventoryController`, `InventoryService`, `InventoryReservation`, and `InventoryReservationRepository`. The inspected reservation path persists a reservation but does not demonstrate `onHand - reserved` checking, over-reservation rejection, concurrency-safe allocation, or bin/status/lot/serial allocation.

## Manufacturing / Quality

- [ ] **SOURCE PRESENT** — BOM snapshot
- [ ] **SOURCE PRESENT / PARTIAL** — Material reservation
- [ ] **PARTIAL** — Actual issues/returns
- [ ] **SOURCE PRESENT / VERIFY** — Partial production receipts
- [ ] **SOURCE PRESENT / VERIFY** — Routing/work centers
- [ ] **SOURCE PRESENT / VERIFY** — Quality plan/disposition
- [ ] **SOURCE PRESENT / VERIFY** — WIP posting
- [ ] **SOURCE PRESENT / VERIFY** — Variance/close

**Known manufacturing blocker:** the current production-start path calculates the planned material requirement and calls `recordProductionIssue(...)` for each requirement when the order starts. Starting/releasing production should reserve material; consumption should be controlled by actual issue/return documents.

Required canonical lifecycle: `PLAN -> RELEASE -> RESERVE -> ACTUAL ISSUE/RETURN -> PARTIAL RECEIPT -> QUALITY -> WIP/COST -> VARIANCE -> CLOSE`.

## Treasury / Budget / Close

- [ ] **VERIFY** — Multi-source payment batch
- [ ] **VERIFY** — Maker/checker
- [ ] **VERIFY** — Difference posting
- [ ] **SOURCE PRESENT / VERIFY** — Budget versions
- [ ] **SOURCE PRESENT / VERIFY** — Budget revisions/transfers
- [ ] **PARTIAL / VERIFY** — All module close providers
- [ ] **SOURCE PRESENT / VERIFY** — Workbenches
- [ ] **PARTIAL** — Financial/subledger reconciliation reports

**Known close blocker:** the close checklist now invokes reconciliation providers, but the current default AP provider sets GL balance to `0` and subledger balance to `0`. It must query authoritative posted GL control-account and AP balances before reconciliation can be checked.

## Finance / Master Data / Rules

- [ ] **SOURCE PRESENT / VERIFY** — Journal source metadata and immutable system journals
- [ ] **SOURCE PRESENT / VERIFY** — Journal dimensions/account validation
- [ ] **SOURCE PRESENT / VERIFY** — Manual journal approval/restricted accounts
- [ ] **VERIFY** — Realized/unrealized FX process
- [ ] **SOURCE PRESENT / VERIFY** — Trial Balance / GL detail
- [ ] **SOURCE PRESENT / VERIFY** — Balance Sheet / Income Statement / Cash Flow
- [ ] **SOURCE PRESENT / VERIFY** — Master-data effective dating
- [ ] **VERIFY** — Supplier/contractor bank-change governance
- [ ] **SOURCE PRESENT / VERIFY** — Typed deterministic rule policies
- [ ] **VERIFY** — Idempotent scheduled jobs

## Platform additions after `a102a92...`

### Web Push / Notifications

- [ ] **SOURCE PRESENT / RUNTIME VERIFY** — VAPID Web Push configuration
- [ ] **SOURCE PRESENT / RUNTIME VERIFY** — Browser subscription/unsubscription
- [ ] **SOURCE PRESENT / RUNTIME VERIFY** — Backend subscription persistence
- [ ] **SOURCE PRESENT / RUNTIME VERIFY** — Notification-created push listener
- [ ] **SOURCE PRESENT / RUNTIME VERIFY** — Admin bulk notification send UI/API
- [ ] **VERIFY** — Tenant/user ownership enforcement
- [ ] **VERIFY** — Notification preference enforcement
- [ ] **VERIFY** — 404/410 stale endpoint cleanup
- [ ] **VERIFY** — Production mobile/PWA delivery
- [ ] **VERIFY** — Admin-send audit/rate limiting

### About

- [ ] **PARTIAL** — `/api/v1/system/about`
- [ ] **SOURCE PRESENT** — Angular About page/route
- [ ] **PARTIAL** — Build metadata

**Known About issue:** version/build/git SHA/environment are hardcoded and build time is generated at request time. The hardcoded git SHA is already older than the reviewed branch HEAD.

### Support / Feedback

- [ ] **SOURCE PRESENT / VERIFY** — Existing support tickets frontend
- [ ] **SOURCE PRESENT / VERIFY** — Existing feedback frontend
- [ ] **VERIFY** — Backend contracts for support/tickets/feedback
- [ ] **VERIFY** — Diagnostic metadata/correlation IDs
- [ ] **VERIFY** — Secure attachment handling, if attachments are enabled

Do **not** create a second feedback subsystem; extend the existing Support feature.

## Repository hygiene / documentation

- [ ] **PARTIAL** — Canonical `docs/` hierarchy
- [ ] **PARTIAL** — Backup directories ignored
- [ ] **NOT DONE** — Tracked backup directories removed
- [ ] **NOT DONE** — All machine-local `file:///...` links removed from current docs
- [ ] **PARTIAL** — Historical documentation archived
- [ ] **DONE IN THIS UPDATE PACKAGE** — This checklist no longer treats `[x]` as a synonym for “class exists”

The current Git tree still contains tracked `.bemo-*-backup-*` directories, so the previous claim “no backup code tracked” must not be used.

---

# Strict completion gate

An item may be changed to `[x] VERIFIED` only when the applicable evidence exists for:

1. domain/state-machine invariants;
2. service orchestration;
3. migration and upgrade path;
4. authorization and tenant ownership;
5. idempotency for retryable commands;
6. concurrency/optimistic locking;
7. audit and source-document traceability;
8. balanced finance posting where applicable;
9. UI/workbench for human actions;
10. unit/integration/negative/concurrency tests;
11. no active legacy bypass;
12. documentation;
13. runtime acceptance evidence.

If only classes/controllers/entities exist, use `SOURCE PRESENT`, not `[x]`. If an integration path has a known gap, use `PARTIAL`.
