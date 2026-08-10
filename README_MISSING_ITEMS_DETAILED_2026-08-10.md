# Bortqala ERP — Confirmed Missing / Incomplete Items

**Repository:** `Mohamed-Hammada/bortqala`  
**Branch:** `fm_bemo_consolidated`  
**Audited commit:** `d0750842db64ed8862145df9213dc2cb410cbe66`  
**Audit date:** 2026-08-10  
**Document purpose:** Technical/business handoff for developers, QA, product owners, and future reviewers.

---

## 1. Scope and audit rule

This document records only issues that were confirmed from the current source code at the audited commit. It is intentionally different from older roadmap or QA Markdown files.

A feature is **not** listed here merely because an older document said that it was missing. It is listed only when the current implementation still contains a concrete business, data-integrity, workflow, or architecture gap.

The four confirmed high-value remaining items are:

1. Procurement can mark a Purchase Order as `RECEIVED` without creating a Goods Receipt or inventory movement.
2. Procurement can cancel an already received or partially received Purchase Order without reversing the received stock.
3. Manufacturing is still primarily a status workflow; BOMs do not contain real component lines and production completion does not consume raw materials or receive finished goods.
4. Trial conversion can set a tenant's commercial state to `PAID` without atomically creating/activating the corresponding subscription plan and entitlements.

These items should be treated as **business-integrity issues**, not only UI enhancements.

---

# 2. Priority summary

| ID | Area | Severity | Main risk | Recommended priority |
|---|---|---:|---|---:|
| MISS-001 | Procurement receiving | Critical / High | PO says received while stock and GRN say otherwise | P0 |
| MISS-002 | Procurement cancellation / reversal | High | Stock remains after PO is cancelled | P0 |
| MISS-003 | Manufacturing transaction model | Critical / High | Production can complete with no material/stock impact | P1 |
| MISS-004 | Trial → Subscription conversion | High | Tenant can be PAID but have missing/stale subscription entitlements | P1 |

---

# 3. MISS-001 — Purchase Order can be received without Goods Receipt

## 3.1 Current behavior

The current backend exposes two separate receiving mechanisms.

### Path A — direct PO receive

API:

```text
POST /api/v1/trade/procurement/orders/{id}/receive
```

Current controller:

```text
be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementController.java
```

The controller delegates to:

```text
ProcurementService.receive(id)
```

Current service:

```text
be/src/main/java/com/bemo/hr/trade/procurement/application/ProcurementService.java
```

The current `receive()` implementation performs the following business operations:

```text
Require PO
→ require status = ISSUED
→ PO status = RECEIVED
→ release budget encumbrance
→ audit RECEIVE
→ return PO
```

It does **not** create a Goods Receipt.

It also does not have any payload containing:

- warehouse;
- received line quantities;
- rejected quantities;
- deducted quantities;
- lot number;
- location;
- quality reason;
- receipt number;
- receiving date per receipt.

Because the endpoint has only the Purchase Order ID, it does not have enough information to execute a proper inventory receipt.

### Path B — Goods Receipt

API:

```text
POST /api/v1/trade/procurement/goods-receipts
```

This path delegates to:

```text
ProcurementService.createGoodsReceipt(...)
```

That implementation is materially different and significantly more correct. It:

- validates that the PO is open;
- accepts partial receiving;
- validates that the receipt supplier matches the PO supplier;
- validates PO line ownership;
- validates the inventory item;
- validates delivered/rejected/deducted quantities;
- calculates accepted quantity;
- prevents over-receipt;
- persists a Goods Receipt;
- persists receipt lines;
- records inventory receipt operations;
- determines whether the PO is partial or fully received;
- changes PO state appropriately;
- releases the budget when fully received;
- writes an audit event.

Therefore the system currently has **two different definitions of "receive a PO."**

That should not exist.

---

## 3.2 Failure example

Assume PO `PO-10025` contains:

| Item | Ordered |
|---|---:|
| ITEM-A | 100 |
| ITEM-B | 50 |

User or frontend calls:

```text
POST /orders/{id}/receive
```

Result can become:

```text
Purchase Order status     = RECEIVED
Budget encumbrance        = released
Goods Receipt             = none
PO line received quantity = no valid GRN trail
Inventory movement        = none
Warehouse quantity        = unchanged
```

From the procurement screen the order looks complete.

From the warehouse screen nothing arrived.

From stock valuation nothing arrived.

From audit there is a RECEIVE action but no physical receipt document.

This is a broken business invariant.

---

## 3.3 Required business invariant

A PO must never become fully received merely because its status was directly changed.

The business invariant should be:

```text
PO received quantity
    =
sum(valid, non-reversed Goods Receipt accepted quantities)
```

And:

```text
PO status = RECEIVED
only when
all PO lines have accepted quantity >= ordered quantity
```

The status should be **derived from receipt transactions**, not set independently.

---

## 3.4 Recommended fix

### Preferred solution

Remove or deprecate:

```text
POST /orders/{id}/receive
```

Do not use it for receiving stock.

All receiving must go through:

```text
POST /goods-receipts
```

This is the safest option because a proper receipt requires line quantities and warehouse information.

### Alternative

If backwards compatibility requires keeping the endpoint, it must no longer simply change status. It would need a request payload equivalent to a Goods Receipt and internally call the same receiving application service.

Do **not** duplicate receipt logic in two methods.

Recommended design:

```text
ProcurementController
       |
       v
GoodsReceiptApplicationService
       |
       +--> validate PO
       +--> validate receipt lines
       +--> create GRN
       +--> create inventory movements
       +--> update received quantities/status
       +--> release encumbrance if complete
       +--> audit
```

---

## 3.5 Backend changes

Recommended changes:

1. Remove `ProcurementService.receive(String id)` or make it inaccessible to normal business use.
2. Remove/deprecate the controller endpoint `/orders/{id}/receive`.
3. Keep `createGoodsReceipt(...)` as the single receiving business transaction.
4. Ensure PO state is recomputed after each Goods Receipt.
5. Add a reusable method such as:

```text
recalculateReceivingState(purchaseOrderId)
```

6. Make state calculation use accepted non-reversed receipt quantities.
7. Prevent any other service from directly assigning `RECEIVED`.
8. Add an audit event containing the Goods Receipt ID/number and resulting PO state.
9. Consider a domain method rather than unrestricted `updateStatus(...)`.

For example:

```text
po.markPartiallyReceived(...)
po.markFullyReceived(...)
```

These methods should not be callable unless receipt conditions are satisfied by the application/domain service.

---

## 3.6 Frontend changes

Search the Angular frontend for any call to:

```text
/orders/{id}/receive
```

If a simple **Receive** action exists on a PO:

- remove it;
- replace it with **Create Goods Receipt / تسجيل إذن استلام**;
- open a receipt dialog/page;
- require received quantities;
- require warehouse/location when applicable;
- show ordered, previously received, remaining, delivered, rejected, deducted, and accepted quantities.

Recommended line UI:

| Field | Purpose |
|---|---|
| Ordered | Original PO quantity |
| Previously accepted | Already received |
| Remaining | Maximum still receivable |
| Delivered | Quantity physically delivered |
| Rejected | Quantity rejected |
| Deducted | Quality/commercial deduction |
| Accepted | Delivered - Rejected - Deducted |
| Warehouse | Receiving warehouse |
| Location | Optional bin/location |
| Lot | Optional lot/batch |
| Quality reason | Rejection/deduction reason |

---

## 3.7 Database considerations

The current Goods Receipt model already provides the foundation.

Do not add a second PO "received quantity" source of truth unless necessary.

If a cached received quantity is stored on PO lines for performance, it must be updated transactionally and reconciled against receipt records.

Recommended invariants:

```text
accepted quantity >= 0
accepted quantity <= remaining ordered quantity
received quantity cannot become negative
reversed receipt does not count as received
```

---

## 3.8 Acceptance criteria

MISS-001 is complete only when all are true:

- [ ] A PO cannot become `RECEIVED` without at least one valid Goods Receipt.
- [ ] A full receipt creates inventory movements for every accepted stock item.
- [ ] A partial receipt changes the PO to `PARTIALLY_RECEIVED`.
- [ ] A final receipt changes the PO to `RECEIVED`.
- [ ] Over-receiving is rejected.
- [ ] Rejected/deducted quantities do not increase stock.
- [ ] Budget encumbrance is not fully released until the configured receiving milestone is satisfied.
- [ ] No frontend action calls the old status-only receive operation.
- [ ] Audit history links receipt and PO.
- [ ] Tests prove the invariant.

---

# 4. MISS-002 — Purchase Order cancellation does not reverse received stock

## 4.1 Current behavior

Current service method:

```text
ProcurementService.cancel(String id)
```

Current logic broadly does:

```text
Require PO
→ reject only if already CANCELLED
→ set status = CANCELLED
→ release budget
→ audit cancellation
```

The current guard does not restrict cancellation to safe states such as:

```text
DRAFT
ISSUED with zero receipts
```

Therefore a PO that has already been:

```text
PARTIALLY_RECEIVED
```

or:

```text
RECEIVED
```

can be passed into the same cancellation method unless another layer happens to prevent it.

The cancellation method itself does not:

- inspect Goods Receipts;
- inspect accepted received quantity;
- reverse inventory movements;
- create supplier-return documents;
- reverse lot/location stock;
- reverse warehouse stock;
- reverse downstream invoices;
- reverse payments;
- create credit/debit documents.

This can leave physical and document states inconsistent.

---

## 4.2 Failure example

```text
PO issued for ITEM-A, quantity 100
         ↓
Goods Receipt accepts 100
         ↓
Inventory +100
         ↓
PO status = RECEIVED
         ↓
User cancels PO
         ↓
PO status = CANCELLED
Inventory still +100
Goods Receipt still exists
```

Now the ERP says:

```text
Procurement: the order is cancelled.
Warehouse: the material is present.
Receipt history: the material was received.
```

This is not a valid cancellation.

It is a **return/reversal workflow**.

---

## 4.3 Correct business distinction

The system should distinguish:

### Cancellation

Cancellation means:

> The transaction is stopped before physical/financial execution.

Safe examples:

```text
DRAFT PO → CANCELLED
ISSUED PO with zero receipts → CANCELLED
```

### Return / reversal

Once material has been received, the correct business process is usually:

```text
Supplier Return
→ outbound stock movement
→ receipt reversal/reference
→ received quantity reconciliation
→ invoice/credit-note reconciliation if invoiced
→ financial/budget reconciliation
```

A return is not the same as cancelling the original PO.

The PO remains historical evidence that the purchasing process existed.

---

## 4.4 Minimum safe fix

Before implementing a full supplier-return module, add an immediate safety guard.

Recommended rule:

```text
if PO has any accepted receipt quantity > 0:
    reject ordinary cancellation
```

Suggested error:

```text
PROC_ORDER_HAS_RECEIPTS
```

Suggested Arabic message:

```text
لا يمكن إلغاء أمر شراء يحتوي على كميات مستلمة. يجب تنفيذ مرتجع مورد أو عكس إذن الاستلام أولاً.
```

Suggested English message:

```text
A purchase order with received quantities cannot be cancelled. Reverse the receipt or create a supplier return first.
```

Also reject cancellation where downstream financial documents make cancellation unsafe unless the financial reversal process is executed.

---

## 4.5 Proper supplier-return design

Recommended entities:

```text
SupplierReturn
- id
- appId
- returnNumber
- supplierId
- purchaseOrderId
- goodsReceiptId
- returnDate
- warehouseId
- status
- reason
- notes
- createdBy
- approvedBy
- createdAt
- postedAt
- version
```

```text
SupplierReturnLine
- id
- supplierReturnId
- goodsReceiptLineId
- purchaseOrderLineId
- itemId
- quantity
- unitOfMeasure
- warehouseId
- locationId
- lotNumber
- unitCost
- reasonCode
```

Recommended states:

```text
DRAFT
SUBMITTED
APPROVED
POSTED
CANCELLED
```

Posting a return should:

1. validate original receipt;
2. validate available returnable quantity;
3. prevent return quantity greater than net received quantity;
4. create an outbound inventory movement;
5. preserve item/warehouse/location/lot traceability;
6. update net received quantity;
7. recalculate PO status if business policy allows;
8. link to supplier invoice/credit note when necessary;
9. audit the operation.

---

## 4.6 API recommendation

Possible API:

```text
POST /api/v1/trade/procurement/supplier-returns
GET  /api/v1/trade/procurement/supplier-returns
GET  /api/v1/trade/procurement/supplier-returns/{id}
POST /api/v1/trade/procurement/supplier-returns/{id}/post
POST /api/v1/trade/procurement/supplier-returns/{id}/cancel
```

A direct receipt reversal may also be useful:

```text
POST /goods-receipts/{id}/reverse
```

but only if the business wants complete reversal of the receipt.

Partial physical returns are better represented by a Supplier Return document.

---

## 4.7 Frontend recommendation

On Purchase Order actions:

```text
DRAFT:
  Edit
  Issue
  Cancel

ISSUED with zero receipt:
  Receive
  Cancel

PARTIALLY_RECEIVED:
  Receive remaining
  View receipts
  Create supplier return
  Do not show ordinary Cancel

RECEIVED:
  View receipts
  Create supplier return
  Do not show ordinary Cancel
```

The UI must not be the only enforcement. Backend validation is mandatory.

---

## 4.8 Acceptance criteria

- [ ] `DRAFT` PO can be cancelled.
- [ ] `ISSUED` PO with zero accepted quantity can be cancelled.
- [ ] PO with any received quantity cannot use ordinary cancellation.
- [ ] Error code is stable and translated.
- [ ] Supplier return/reversal posts a negative/outbound inventory movement.
- [ ] Return quantity cannot exceed returnable quantity.
- [ ] Lot/location traceability is retained.
- [ ] Original receipt remains auditable.
- [ ] Downstream invoices/payments are protected from invalid cancellation.
- [ ] Budget behavior is explicitly defined and tested.
- [ ] Concurrent return attempts cannot over-return stock.

---

# 5. MISS-003 — Manufacturing does not yet perform real material and inventory transactions

## 5.1 Why this is a major gap

The application contains manufacturing routes, BOM records, production orders, and quality records. The module therefore exists.

However, the current manufacturing implementation does not yet implement the core ERP manufacturing transaction:

```text
Raw Materials
     ↓ issue/consume
Work In Progress
     ↓ production
Finished Goods
     ↓ receive
Inventory
```

This is not a cosmetic enhancement. It is the difference between a production tracker and a manufacturing ERP subsystem.

---

## 5.2 Current BOM model

Current source:

```text
be/src/main/java/com/bemo/hr/manufacturing/production/domain/BomHeader.java
```

Current BOM fields include:

```text
id
appId
bomCode
finishedGoodName
yieldQuantity
notes
active
createdAt
updatedAt
```

Important observations:

1. Finished good is stored as `finishedGoodName`, not a strong inventory-item reference.
2. There are no BOM component/material lines.
3. There is no quantity-per component.
4. There is no component UOM.
5. There is no wastage/scrap factor.
6. There is no BOM version.
7. There is no effective-from/effective-to date.
8. There is no revision approval/effectivity.
9. There is no alternate/substitute material structure.

Example of what is missing:

```text
BOM: JUICE-1L / Version 3
Yield: 1 bottle

Components
------------------------------------------------
Orange concentrate      0.20 KG
Water                   0.80 L
Bottle                  1 EA
Cap                     1 EA
Label                   1 EA
Expected process loss   1.5 %
```

Without component lines, the ERP cannot calculate material demand for a production order.

---

## 5.3 Current ProductionOrder model

Current source:

```text
be/src/main/java/com/bemo/hr/manufacturing/production/domain/ProductionOrder.java
```

Current fields:

```text
id
appId
orderNumber
bomId
targetQuantity
startDate
status
createdAt
updatedAt
version
```

Current statuses:

```text
PLANNED
IN_PROGRESS
COMPLETED
CANCELLED
```

The entity exposes a generic:

```text
updateStatus(Status status)
```

That allows arbitrary state assignment by callers.

Important missing production transaction information includes:

- finished inventory item;
- BOM version snapshot;
- planned component requirements;
- actual component consumption;
- source warehouse;
- source location;
- output warehouse;
- output location;
- actual produced quantity;
- rejected production quantity;
- scrap quantity;
- start timestamp;
- completion timestamp;
- operator/work center;
- production cost;
- material issue references;
- finished-goods receipt reference.

---

## 5.4 Current start / complete behavior

Current controller:

```text
be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingController.java
```

Start endpoint:

```text
POST /api/v1/manufacturing/orders/{id}/start
```

Current action:

```text
order.updateStatus(IN_PROGRESS)
save(order)
```

Complete endpoint:

```text
POST /api/v1/manufacturing/orders/{id}/complete
```

Current action:

```text
order.updateStatus(COMPLETED)
save(order)
```

The methods do not currently:

- validate material availability;
- reserve material;
- issue raw materials;
- deduct inventory;
- record actual consumption;
- record waste;
- record finished production quantity;
- add finished goods to inventory;
- post production inventory movements;
- create WIP/costing effects;
- enforce a legal transition such as `IN_PROGRESS → COMPLETED`.

This means a production order can become `COMPLETED` even though inventory is unchanged.

---

## 5.5 Invalid state transitions

Because callers can set status directly, scenarios such as these need explicit protection:

```text
PLANNED → COMPLETED
COMPLETED → IN_PROGRESS
CANCELLED → COMPLETED
```

A proper domain lifecycle should define allowed transitions.

Recommended state model:

```text
PLANNED
  ↓ release
RELEASED
  ↓ start
IN_PROGRESS
  ↓ complete
COMPLETED
```

Optional:

```text
PLANNED/RELEASED → CANCELLED
IN_PROGRESS → CANCELLED only through controlled reversal
```

For an MVP the existing statuses may remain, but enforce:

```text
PLANNED → IN_PROGRESS
IN_PROGRESS → COMPLETED
PLANNED → CANCELLED
```

and prohibit all illegal transitions.

---

## 5.6 Required BOM domain model

Recommended:

```text
BomHeader
- id
- appId
- bomCode
- finishedItemId
- version
- baseQuantity
- outputUomId
- status
- effectiveFrom
- effectiveTo
- notes
- version/optimisticLock
```

```text
BomLine
- id
- bomId
- lineNo
- componentItemId
- quantityPer
- uomId
- wastePercent
- issueWarehouseId optional
- allowSubstitution
- notes
```

Do not identify stock items only by text names.

Use inventory item IDs.

---

## 5.7 Production material planning

When a production order targets quantity `Q`, calculate:

```text
required component qty
    =
(target production quantity / BOM base output)
× BOM component quantity
× (1 + waste percentage)
```

Example:

```text
BOM yield = 10 units
Material A = 4 KG per BOM batch
Production target = 100 units

Required A = (100 / 10) × 4 = 40 KG
```

With 2% waste:

```text
40 × 1.02 = 40.8 KG
```

The system should present:

| Component | Required | Available | Reserved | Shortage |
|---|---:|---:|---:|---:|
| Material A | 40.8 | 50 | 0 | 0 |
| Material B | 20 | 12 | 0 | 8 |

Production start should block or warn based on configured shortage policy.

---

## 5.8 Material issue / consumption

Recommended supporting document:

```text
ProductionMaterialIssue
- productionOrderId
- warehouseId
- issueDate
- status
```

Lines:

```text
componentItemId
plannedQuantity
issuedQuantity
actualConsumedQuantity
returnedQuantity
scrapQuantity
lotNumber
locationId
```

When material is issued:

```text
inventory movement = PRODUCTION_ISSUE
quantity = negative
reference = production order / issue document
```

Any unused issued material should be returned through:

```text
PRODUCTION_RETURN
```

rather than silently editing quantities.

---

## 5.9 Finished-goods receipt

Completion should require actual output values:

```text
actualGoodQuantity
rejectedQuantity
scrapQuantity
outputWarehouseId
outputLocationId
lotNumber / batchNumber
```

Posting completion should create:

```text
inventory movement = PRODUCTION_RECEIPT
quantity = actualGoodQuantity
```

Finished goods must reference the production order and BOM version.

---

## 5.10 Atomic production completion

Production completion should be transactional.

Conceptually:

```text
BEGIN TRANSACTION

lock production order

validate state = IN_PROGRESS
validate BOM snapshot
validate actual quantities
validate material balances

post remaining material consumption
post material returns if supplied
post finished-goods receipt
post scrap if applicable
calculate production cost
mark order COMPLETED
audit complete operation

COMMIT
```

If any inventory posting fails:

```text
ROLLBACK EVERYTHING
```

The ERP must never end in:

```text
raw material deducted but output missing
```

or:

```text
output received but raw material not consumed
```

---

## 5.11 Costing recommendations

MVP:

```text
actual material cost
    =
sum(actual consumed quantity × inventory issue cost)
```

Then later add:

```text
labor cost
machine cost
overhead
subcontracting
scrap impact
```

Production order should expose:

```text
planned cost
actual cost
variance
unit cost
```

These values become useful for:

- inventory valuation;
- finance;
- profitability;
- manufacturing analytics;
- AI explanations.

---

## 5.12 Quality integration

Quality inspection currently exists but should become strongly linked to a source record.

Recommended source relationships:

```text
Goods Receipt → Incoming Quality Inspection
Production Order → In-process / Final Inspection
Finished Goods Receipt → Release decision
```

Production output can be split:

```text
Passed → available finished stock
Failed → quarantine/rework/scrap
```

Do not simply record a quality row without affecting inventory disposition where relevant.

---

## 5.13 Frontend changes

Manufacturing screen should evolve from simple status actions to an operational workflow.

Recommended tabs/details:

```text
Overview
BOM
Material Requirements
Material Issues
Actual Consumption
Production Output
Quality
Costs
Timeline / Audit
```

Before starting:

```text
Material readiness
Available / Shortage
```

On completion dialog require:

```text
Produced good quantity
Rejected quantity
Scrap quantity
Output warehouse
Location
Batch/Lot
Actual component consumption or confirmation
```

---

## 5.14 Migration strategy

Recommended sequence:

### Migration A
Create `bom_lines` and update `boms` with:

```text
finished_item_id
version_no
base_quantity
effective dates
```

### Migration B
Add production transactional tables:

```text
production_material_issues
production_material_issue_lines
production_outputs
```

### Migration C
Backfill old BOMs

Existing BOMs use `finishedGoodName`.

Do not automatically guess an inventory item if names are ambiguous.

Migration tool/UI should:

1. find exact matching item names when unique;
2. propose matches;
3. require manual confirmation for ambiguous rows;
4. mark unmapped BOMs as migration-incomplete;
5. prevent operational use until mapped.

---

## 5.15 Acceptance criteria

Manufacturing is considered transactionally complete when:

- [ ] BOM finished good references an inventory item.
- [ ] BOM has versioned component lines.
- [ ] Production order snapshots/uses the intended BOM version.
- [ ] Required material quantities can be calculated.
- [ ] Availability/shortage is visible.
- [ ] Material issue deducts stock.
- [ ] Lot/location information is retained where relevant.
- [ ] Actual consumption is persisted.
- [ ] Unused material can be returned.
- [ ] Scrap/reject quantities are persisted.
- [ ] Production completion receives finished goods into stock.
- [ ] Completion is atomic.
- [ ] Illegal status transitions are rejected.
- [ ] Completed production cannot be silently reopened.
- [ ] Cancellation after material issue requires reversal.
- [ ] Audit links all movements to the production order.
- [ ] Tests cover normal, partial, shortage, failure, concurrency, and rollback cases.

---

# 6. MISS-004 — Trial conversion and subscription activation can diverge

## 6.1 Current architecture

The code contains two related but separate commercial concepts:

### Tenant commercial state

Managed on the tenant/application object.

Examples:

```text
TRIAL
PAID
```

Trial service:

```text
be/src/main/java/com/bemo/hr/product/trial/TrialDemoService.java
```

### Subscription state

Managed using:

```text
SubscriptionPlan
TenantSubscription
SubscriptionChange
EntitlementManagementService
```

Subscription service:

```text
be/src/main/java/com/bemo/hr/product/subscription/SubscriptionService.java
```

The subscription flow selects a plan, applies features/entitlements, stores subscription dates/status, and writes subscription history.

---

## 6.2 Current trial conversion request

Current request type:

```text
TrialDemoApi.ConvertRequest
```

contains only:

```text
operationId
```

It does not contain:

```text
planCode
subscription status
startsAt
renewsAt
endsAt
expected subscription version
payment/reference information
```

The trial service's `convert(...)` operation currently calls the tenant's:

```text
convertTrial(...)
```

and records an audit event.

That can change commercial state without executing `SubscriptionService.change(...)`.

---

## 6.3 Resulting inconsistency

Possible business state:

```text
TenantApplication.commercialState = PAID
```

while:

```text
TenantSubscription = missing
```

or:

```text
TenantSubscription.status = TRIAL
```

or:

```text
TenantSubscription.planCode = old plan
```

or entitlements do not match the commercial state.

This creates questions the system cannot answer consistently:

- Is the customer paid?
- Which plan is active?
- Which features should be available?
- Which user limit should apply?
- What happens at renewal?
- What should billing display?
- What should trial-expiry logic do?
- Which subscription history entry represents conversion?

---

## 6.4 Required invariant

Recommended invariant:

```text
commercialState = PAID
        implies
an active/current TenantSubscription exists
        and
subscription plan exists and is active
        and
entitlements correspond to that subscription
```

If the system intentionally allows a manually paid state with no subscription, that must be explicitly modeled as a separate commercial mode. It should not be an accidental divergence.

---

## 6.5 Recommended conversion design

Preferred application transaction:

```text
ConvertTrialCommand
- operationId
- planCode
- startsAt
- renewsAt
- endsAt
- expectedSubscriptionVersion
- reason
- paymentReference optional
```

Service:

```text
TrialConversionService.convert(command)
```

Transaction:

```text
BEGIN

lock tenant
check operation idempotency

validate tenant is eligible for conversion
validate selected plan
load/lock current subscription

create/change TenantSubscription
apply plan entitlements
record SubscriptionChange

set tenant commercial state = PAID
set convertedAt
set lastConversionOperationId

record audit event

COMMIT
```

If applying entitlements fails, tenant must remain trial.

If setting tenant to paid fails, subscription change must roll back.

---

## 6.6 Avoid service-to-service duplication

Do not copy subscription logic into `TrialDemoService`.

Refactor the actual subscription mutation into a reusable application/domain operation.

Possible design:

```text
SubscriptionLifecycleService.activatePlan(...)
```

Used by:

```text
SubscriptionService.change(...)
TrialConversionService.convert(...)
Admin commercial-management flow
future billing/payment callback
```

This ensures all plan changes apply the same:

- validation;
- entitlements;
- history;
- audit;
- versioning;
- idempotency behavior.

---

## 6.7 Idempotency

The current trial conversion already uses operation IDs. Keep that behavior.

Required behavior:

```text
first request operationId=ABC
→ conversion committed

retry operationId=ABC
→ return same resulting state
→ do not create second SubscriptionChange
→ do not apply entitlements twice
→ do not create duplicate audit/business events
```

A different operation ID after successful conversion should either:

- return "already converted"; or
- be treated as a plan-change operation through subscription management.

Do not silently reconvert.

---

## 6.8 Expired-trial interaction

The current system also has write protection for expired trials.

Conversion/upgrade paths must remain accessible even when the general tenant is read-only.

Otherwise a customer could reach:

```text
trial expired
→ all writes blocked
→ upgrade endpoint also blocked
→ impossible to become paid
```

Ensure the write interceptor/security configuration explicitly allows the commercial activation endpoints required for recovery.

This should be tested, not assumed.

---

## 6.9 Frontend changes

The trial conversion UI should not have a generic **Convert** button that only changes commercial state.

Recommended flow:

```text
Choose plan
→ show features
→ show limits
→ show renewal information
→ confirm conversion
→ execute one atomic conversion command
→ refresh subscription + entitlements + tenant status
```

After success show:

```text
Commercial state: PAID
Plan: BUSINESS
Subscription: ACTIVE
Users: 7 / 20
Renewal: <date>
```

If any part fails, do not show conversion as successful.

---

## 6.10 Acceptance criteria

- [ ] Trial conversion requires/derives a valid plan.
- [ ] Conversion and subscription activation are in one transaction.
- [ ] `PAID` cannot exist without the intended subscription state.
- [ ] Entitlements match the activated plan.
- [ ] Usage limits immediately use the new plan.
- [ ] Subscription history records the conversion.
- [ ] Trial audit records the same operation.
- [ ] Retry with same operation ID is idempotent.
- [ ] Failed entitlement/application update rolls back commercial conversion.
- [ ] Expired trial can still access the allowed upgrade path.
- [ ] Frontend refreshes both tenant and subscription state after success.

---

# 7. Cross-cutting engineering requirements

These four fixes touch shared architectural concerns.

## 7.1 State transitions should be domain operations

Avoid generic methods such as:

```text
updateStatus(anyStatus)
```

for business-critical aggregates.

Prefer:

```text
issue()
markPartiallyReceived(...)
markFullyReceived(...)
cancelBeforeReceipt()
startProduction()
completeProduction(...)
```

Each domain method should enforce its own legal previous states.

---

## 7.2 Idempotency

High-impact operations should be idempotent where retries are possible:

- Goods Receipt creation/posting;
- Supplier Return posting;
- Production completion;
- Trial conversion;
- financial posting related to these flows.

A network retry must not double stock.

---

## 7.3 Optimistic/pessimistic locking

Critical race examples:

```text
two users receive final PO quantity simultaneously
two users return the same receipt quantity
two users complete one production order
two conversion requests race
```

Use the existing optimistic locking approach or explicit row locks where necessary.

Test concurrent scenarios.

---

## 7.4 Audit

Every physical/commercial transition should answer:

```text
Who?
When?
What record?
Previous state?
New state?
Related document?
Operation id?
Quantities?
```

Do not rely only on a generic "status changed" record.

---

## 7.5 Error code stability

Use stable backend error codes and translate in the normal i18n system.

Examples:

```text
PROC_ORDER_DIRECT_RECEIVE_DISABLED
PROC_ORDER_HAS_RECEIPTS
PROC_RETURN_EXCEEDS_RECEIVED
MFG_ILLEGAL_STATE_TRANSITION
MFG_INSUFFICIENT_MATERIAL
MFG_BOM_COMPONENT_REQUIRED
MFG_OUTPUT_REQUIRED
SUBSCRIPTION_REQUIRED_FOR_PAID_STATE
TRIAL_CONVERSION_PLAN_REQUIRED
```

Arabic and English text should remain UI concerns driven through translation keys where possible.

---

# 8. Regression risk areas

After implementation, retest at least:

```text
Procurement
- PO create/edit/issue
- partial Goods Receipt
- multiple Goods Receipts
- final receipt
- rejected/deducted quantities
- budget encumbrance/release
- three-way match
- supplier invoice
- supplier payment
- supplier return/reversal
- Excel export

Inventory
- quantity on hand
- warehouse movement history
- lot/location
- valuation behavior

Manufacturing
- BOM creation/versioning
- production start
- shortages
- issue
- return
- completion
- scrap
- quality
- stock movements
- costing

Commercial
- trial
- demo
- expiry
- conversion
- subscription plan
- entitlements
- user limits
- downgrade/upgrade
```

---

# 9. Recommended implementation order

## P0 — Procurement integrity

Implement MISS-001 and MISS-002 first.

Reason:

These can create direct disagreement between:

```text
document status
inventory
budget
supplier history
```

Suggested sequence:

```text
1. Disable status-only receive
2. Force GRN receiving
3. Protect cancellation after receipt
4. Implement return/reversal
5. Add regression tests
```

## P1 — Manufacturing transaction model

Implement MISS-003 next.

Suggested delivery increments:

```text
1. BOM component lines + finished item reference
2. legal production state transitions
3. material requirements
4. material issue/return
5. finished goods receipt
6. atomic completion
7. costing
8. quality disposition
```

## P1 — Trial/subscription convergence

Can be implemented in parallel with manufacturing because it is largely isolated.

Suggested:

```text
1. define commercial/subscription invariant
2. create atomic conversion service
3. reuse subscription lifecycle code
4. update UI
5. add idempotency/expiry tests
```

---

# 10. Definition of Done for this document

Do not mark this missing-items document obsolete merely because screens were added.

An item is complete only when:

1. backend invariant is enforced;
2. database state remains consistent;
3. frontend uses the correct business API;
4. error cases are translated;
5. audit trail is complete;
6. automated tests cover success and failure;
7. QA executes the specified regression cases;
8. old unsafe endpoints/paths are removed or deliberately blocked;
9. documentation is updated with the final architecture.

---

# 11. Current-source evidence inspected

This handoff was produced from current source at commit:

```text
d0750842db64ed8862145df9213dc2cb410cbe66
```

Primary files inspected:

```text
be/src/main/java/com/bemo/hr/trade/procurement/application/ProcurementService.java
be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementController.java
be/src/main/java/com/bemo/hr/manufacturing/production/domain/BomHeader.java
be/src/main/java/com/bemo/hr/manufacturing/production/domain/ProductionOrder.java
be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingController.java
be/src/main/java/com/bemo/hr/product/trial/TrialDemoService.java
be/src/main/java/com/bemo/hr/product/trial/TrialDemoApi.java
be/src/main/java/com/bemo/hr/product/subscription/SubscriptionService.java
```

The branch HEAD should be rechecked before development starts. If HEAD changes after this document, compare the new implementation against the acceptance criteria rather than assuming the finding is still open.

---

# 12. Important note about older roadmap files

The current branch contains many capabilities that older status files may still describe as missing.

Therefore:

> Do not use an old roadmap line as proof that a feature is absent.

Use the running branch and source code.

This document deliberately focuses only on the still-confirmed items above.
