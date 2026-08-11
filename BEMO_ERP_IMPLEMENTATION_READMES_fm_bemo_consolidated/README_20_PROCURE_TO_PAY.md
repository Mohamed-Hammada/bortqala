# Procure-to-Pay — Requisition, Sourcing, PO, Match and Payment

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

Complete the existing PO-centric procurement module by adding upstream demand/sourcing and downstream controlled AP payment without duplicating the already-implemented PO, GRN, return, supplier invoice/payment and three-way-match behavior.

## Existing code — preserve and extend

- `[EXISTS] ProcurementApi.java`, `ProcurementController.java` under `com.bemo.hr.trade.procurement.api`.
- `[EXISTS] /api/v1/trade/procurement` current controller base.
- Current domain already covers PO lines/status, GRN accepted/rejected/deducted quantities, returns, supplier invoice with PO/GRN/FX/tax/due/outstanding, supplier payment, 3-way match/tolerances/resolution.
- `[EXISTS] fe/src/app/features/trade/procurement/procurement.page.ts`, `.html`, `.scss`, `.spec.ts`.
- `[EXISTS] budget commitment/encumbrance concepts elsewhere; reuse them.

## Target end-to-end cycle

`Purchase Requisition → Budget Check → Approval → RFQ → Supplier Quotes → Evaluation/Award → PO Approval/Issue → GRN/Service Entry → Quality Disposition → Supplier Invoice → 2/3-Way Match → AP Approval → Payment Proposal → Treasury Approval/Payment → Bank Reconciliation → AP/GL Reconciliation → Close`

## Data model changes

### `[NEW] PurchaseRequisition`
Header: requester, department, branch, costCenter, requiredDate, justification, currency, budget status, approval ref, status/version.
Lines: item/service, qty, UOM, estimated unit price, preferred supplier optional, project, estimated total.

### `[NEW] RFQ/SupplierQuotation/SourcingAward`
RFQ references approved requisition lines. Quote stores supplier commercial values as snapshot. Award stores selected quote/lines + human decision reason and approval.

### `[NEW] SupplierContract` phase after sourcing if recurring purchases are required
validity, max qty/value, tier price, SLA, payment/tax/withholding terms, released usage.

### Extend receiving stock disposition
`RECEIVED_PENDING_INSPECTION / AVAILABLE / QUARANTINED / REJECTED / RETURN_TO_VENDOR` integrated with operations inventory; rejected stock never counted as available.

### `[NEW] LandedCost` later P2
freight/customs/insurance/handling/inspection allocation by quantity/weight/value/manual%; update valuation via existing operations costing service.

### Payment proposal
Prefer treasury-owned shared `PaymentBatch`; procurement/AP can own selection/proposal view. Block disputed/match-failed invoices.

## API/command changes

Add under existing procurement module; retain current endpoint style:

```text
POST /api/v1/trade/procurement/requisitions
POST /.../requisitions/{id}/submit
POST /.../requisitions/{id}/rfqs
POST /.../rfqs/{id}/quotes
POST /.../rfqs/{id}/award
POST /.../awards/{id}/create-po   -> calls existing PO creation
POST /.../invoices/{id}/match    -> extend current match semantics
POST /.../invoices/{id}/resolve-match-exception
POST /.../payment-proposals      -> selection only; treasury creates/executes batch
```

Do not add a second PO API. Award-to-PO must create the existing PO type and link `sourceDocumentType/Id`.

## Backend implementation sequence

1. Trace `ProcurementController` endpoints → services/entities/repositories.
2. Add source linkage fields to existing PO safely/backward compatible.
3. Add requisition header/line and CRUD while DRAFT only.
4. Add submit → budget check → existing approval engine.
5. Add RFQ/quotes; calculated comparison metrics: total landed estimate, lead time, terms, historical rejection/OTIF; final award remains human.
6. Award → existing PO creation service; snapshot awarded price/terms.
7. Extend GRN with stock status/inspection hook; invoke operations service for availability only after release.
8. Extend current match configuration/result codes (`MATCHED`, `WITHIN_TOLERANCE`, blocked reasons, manual review, resolved).
9. Post supplier invoice/AP through shared posting profile.
10. Add due-invoice payment proposal query; exclude blocked/disputed; group supplier/currency/bank.
11. Send approved items to treasury payment batch; reuse bank reconciliation.
12. Add AP-to-GL and GR/IR close checks.

## Frontend implementation

The existing procurement page is already large (~single feature page). Do not continue adding every new workflow into one giant component. Keep its route and data-access ownership, but split child components/tabs such as:
- requisitions queue/detail;
- sourcing/RFQ comparison;
- current PO/GRN tabs (preserve);
- invoice match exceptions;
- due-for-payment proposal.

If child routes are introduced, keep a Procurement Workbench entry and reuse shared tables/dialogs. Update `procurement.page.spec.ts` and add child specs.

## Cross-module integration

Budget: requisition/PO commitment policy; Approval: requisition/PO/override/payment; Operations: GRN stock and landed cost; Quality: release/quarantine; Finance: AP/GRIR/tax; Treasury: payment; Bank: reconciliation; Close: unmatched/unposted/GRIR reconciliation.

## Required automated and manual tests

No-line requisition; insufficient blocking budget; approval amount band; quote from uninvited supplier policy; award without approved requisition; duplicate PO creation operation; partial GRN with rejected qty; inspection quarantine; invoice before receipt policy; price/qty/tax tolerance boundaries; override approval; duplicate invoice posting; due-date proposal grouping; payment excludes blocked invoice; AP/GL recon.



## Junior developer — exact execution order

1. Read existing ProcurementApi/Controller + current page.
2. Add PO source linkage first.
3. Add requisition.
4. Integrate budget + approval.
5. Add RFQ/quote/award.
6. Award into existing PO.
7. Add quality stock disposition.
8. Harden three-way match.
9. Add AP posting.
10. Add payment proposal/treasury integration.
11. Add close/reconciliation.
12. Run Scenario B end to end.
