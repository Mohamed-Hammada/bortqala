# Order-to-Cash — Sales Lines, Reservation, Delivery and Returns

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

Evolve the current header-level sales order into line-level commercial and fulfillment control while preserving the current customer-credit check, AR invoice, receipt allocation, aging and collection capabilities.

## Existing code — preserve and extend

- `[EXISTS] SalesApi.java`, `SalesController.java`.
- Existing confirmation performs customer credit availability check.
- Current capabilities include sales-order headers, AR invoices/issue, receipts/allocations, customer credit profiles/hold, aging and collection tasks.
- `[EXISTS] fe/src/app/features/trade/sales/sales.page.ts`, `.html`, `.scss`, `.spec.ts`.
- Structural gap: order lines/fulfillment are not yet the controlling aggregate.

## Target end-to-end cycle

`Quotation → Sales Order → Credit Check → Price Snapshot → ATP/Reservation → Picking/Packing → Delivery → Customer Acceptance → Invoice Delivered Qty → Receipt/Allocation → Collections/Dispute → Bank Reconciliation → AR/GL Reconciliation → Close`

## Data model changes

### `[NEW] SalesOrderLine`
item/service, qty/UOM, unitPrice, discount, taxCode, requested/promised date, warehouse, reserved/delivered/invoiced/returned qty, status, price-rule snapshot/version.

### Pricing
`PriceList/PriceListLine` with customer/group, currency, effective dates, quantity tiers, tax, promotion window. Manual discount above threshold requires approval.

### `[NEW/EXTEND] InventoryReservation`
Prefer operations module ownership. Sales stores reservation references, not its own on-hand balance.
Available = on-hand - active reservations - blocked/quarantine.

### `[NEW] Delivery/DeliveryLine`
SO line, picking qty, warehouse/bin/lot/serial, dispatch/receipt, carrier, customer acceptance. Partial delivery supported.

### `[NEW] CustomerReturn/RMA + CreditNote`
`REQUESTED → AUTHORIZED → RECEIVED → INSPECTED → RESTOCK/SCRAP/REPAIR → CREDIT_NOTE/REPLACEMENT → CLOSED`.

### Credit exposure
Open AR + confirmed uninvoiced + delivered uninvoiced − approved deposits/guarantees. Policy: warn/block/approval.

## API/command changes

Extend existing sales controller/module:

```text
POST /.../sales/orders                 (request includes lines)
POST /.../sales/orders/{id}/confirm    (price + credit + reservation)
POST /.../sales/orders/{id}/reserve
POST /.../sales/orders/{id}/deliveries
POST /.../sales/deliveries/{id}/post
POST /.../sales/orders/{id}/invoice-delivered
POST /.../sales/returns
POST /.../sales/returns/{id}/inspect
POST /.../sales/returns/{id}/credit-note
```

Confirm must be idempotent. If reservation policy is automatic, credit approval and reservation result must be coordinated; a failed reservation must not leave a misleading fully-confirmed state unless backorder is an explicit allowed result.

## Backend implementation sequence

1. Add order line entity/repository and migrate old header orders with zero/legacy line state safely.
2. Extend SalesApi request/response while preserving backward compatibility if current frontend depends on header-only payload.
3. Add pricing resolver with effective version snapshots; manual override approval.
4. Replace simple credit check with exposure service but preserve current profile/hold logic.
5. Add reservation orchestration via operations inventory service.
6. Add delivery aggregate with partial line quantities; stock issue and COGS through operations/posting services.
7. Invoice only delivered quantity by policy; link invoice lines to delivery/SO lines.
8. Extend receipts allocations already present; no duplicate receivables ledger.
9. Add RMA/returns, inventory disposition and credit-note path.
10. Add AR-to-GL reconciliation and period-close checks.

## Frontend implementation

Refactor `[EXISTS] sales.page.*` into line and fulfillment panels/components:
- order header + editable line grid in DRAFT;
- price/tax/discount explanation per line;
- credit exposure card with exact formula;
- reservation/backorder status;
- deliveries tab with partial quantities;
- invoices/receipts allocation timeline;
- returns/credit notes;
- journal/bank reconciliation links.

Update existing `sales.page.spec.ts`; add child specs for line calculations and action enablement.

## Cross-module integration

Operations owns reservations/issues/returns; Finance posts AR/revenue/tax and COGS/inventory; Approval handles discount/credit overrides; Bank reconciliation matches receipts; period close checks unposted invoices/unapplied receipts/AR recon.

## Required automated and manual tests

Multiple lines; price effective-date boundary; manual discount threshold; credit pass/block/approval; two orders competing for same stock; partial reservation/backorder; duplicate confirm; partial delivery; invoice only delivered qty; customer rejection; partial receipt; unallocated receipt; return restock vs scrap; credit note; COGS journal; AR/GL zero difference.



## Junior developer — exact execution order

1. Add `SalesOrderLine`.
2. Extend API + UI line grid.
3. Add pricing snapshot.
4. Extend credit exposure.
5. Add reservation.
6. Add partial delivery + stock/COGS.
7. Link invoice to delivered lines.
8. Add returns/credit notes.
9. Add workbench/reconciliation.
10. Run Scenario C.
