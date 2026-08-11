# Architecture and Existing Codebase Map

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Verified architecture

The branch uses an Angular frontend under `fe/src/app` and a Spring-style Java backend under `be/src/main/java/com/bemo/hr`. The implementation approach must follow the module conventions already present rather than introducing a second architectural style.

## Existing frontend anchors

| Area | Existing path | Action |
|---|---|---|
| Root routing | `[EXISTS] fe/src/app/app.routes.ts` | `[MODIFY]` only to add lazy routes/workbenches/guards consistent with current routing |
| Workforce routing | `[EXISTS] fe/src/app/features/workforce/workforce.routes.ts` | `[MODIFY]` for new dispatch/dispute/workbench routes |
| Workforce pages root | `[EXISTS] fe/src/app/features/workforce/pages/` | Reuse existing feature split |
| Contractors | `[EXISTS] fe/src/app/features/workforce/pages/contractors/` | Extend commercial terms/status UI |
| Workers | `[EXISTS] fe/src/app/features/workforce/pages/workers/` | Extend assignment context/read-only history links |
| Labor requests | `[EXISTS] fe/src/app/features/workforce/pages/labor-requests/` | Extend request dimensions/items/approval/dispatch |
| Manual attendance | `[EXISTS] fe/src/app/features/workforce/pages/manual-attendance/` | Extend lock/correction indicators |
| Settlement periods | `[EXISTS] fe/src/app/features/workforce/pages/settlement-periods/` | Extend calculation-version, dispute, invoice match, posting/payment state |
| Advances | `[EXISTS] fe/src/app/features/workforce/pages/advances/` | Reuse for approved installment inputs |
| Contractor accounts | `[EXISTS] fe/src/app/features/workforce/pages/contractor-accounts/` | Extend reconciliation/drill-down |
| Procurement | `[EXISTS] fe/src/app/features/trade/procurement/procurement.page.ts` + `.html` + `.scss` + `.spec.ts` | Existing page is large; extend carefully or split child components without duplicating API state |
| Sales | `[EXISTS] fe/src/app/features/trade/sales/sales.page.ts` + `.html` + `.scss` + `.spec.ts` | Add lines, fulfillment tabs/workbench or child components |
| Approvals | `[EXISTS] fe/src/app/features/approvals/` | Reuse; do not add a separate approvals UI per module unless only a module-filtered view |
| Audit logs | `[EXISTS] fe/src/app/features/audit-logs/` | Reuse for transaction history links |
| Finance | `[EXISTS] fe/src/app/features/finance/` | Add posting/reconciliation drill-down through current finance feature |
| Fiscal periods | `[EXISTS] fe/src/app/features/fiscal-periods/` | Extend to close workbench/status prerequisites |
| Manufacturing | `[EXISTS] fe/src/app/features/manufacturing/` | Extend existing production UI |
| Operations | `[EXISTS] fe/src/app/features/operations/` | Reuse stock services/screens; add warehouse controls here |

## Existing backend anchors

| Area | Existing class/path | Implementation instruction |
|---|---|---|
| Workforce API DTOs | `[EXISTS] be/src/main/java/com/bemo/hr/workforce/WorkforceApi.java` | Extend commands/views; preserve operation/version patterns |
| Workforce settlement | `[EXISTS] be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementController.java` | Add dispute/invoice-match/posting transition endpoints or delegate to new services from here |
| Attendance report | `[EXISTS] be/src/main/java/com/bemo/hr/reporting/api/ReportController.java` | Reuse approve/reopen/decision flow; add snapshot generation at approval boundary |
| Payroll API | `[EXISTS] be/src/main/java/com/bemo/hr/payroll/api/PayrollApi.java` | Evolve from sheet/payment API toward run-based DTOs without breaking current consumers in one step |
| Payroll controller | `[EXISTS] be/src/main/java/com/bemo/hr/payroll/api/PayrollController.java` | Add run endpoints; deprecate direct pay shortcuts after migration |
| Payroll service | `[EXISTS] be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java` | Extract calculation rules/components; remove hard-coded policy from core algorithm incrementally |
| Procurement API | `[EXISTS] be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementApi.java` | Add upstream DTOs and match/payment proposal views where module-owned |
| Procurement controller | `[EXISTS] be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementController.java` | Existing cycle begins at PO; add requisition/RFQ endpoints in same module/package or dedicated controllers under it |
| Sales API | `[EXISTS] be/src/main/java/com/bemo/hr/trade/sales/api/SalesApi.java` | Extend order DTOs with lines/fulfillment summaries |
| Sales controller | `[EXISTS] be/src/main/java/com/bemo/hr/trade/sales/api/SalesController.java` | Extend existing confirm path with price snapshot, credit exposure and reservation orchestration |
| Inventory API | `[EXISTS] be/src/main/java/com/bemo/hr/operations/OperationsApi.java` | Extend stock dimensions/status/reservation/transfer DTOs |
| Inventory controller | `[EXISTS] be/src/main/java/com/bemo/hr/operations/OperationsController.java` | Keep inventory movement ownership here |
| Manufacturing API | `[EXISTS] be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingApi.java` | Add operation/material issue/receipt/QC DTOs |
| Manufacturing controller | `[EXISTS] be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingController.java` | Replace monolithic start/complete semantics gradually with explicit commands |
| Manufacturing service | `[EXISTS] be/src/main/java/com/bemo/hr/manufacturing/production/application/ManufacturingService.java` | Critical refactor point: planned full issue on start → reservations + actual issues; one completion receipt → partial receipts |
| Accounting API | `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingApi.java` | Reuse journal/post/reverse concepts; add source/posting metadata DTOs |
| Accounting controller | `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingController.java` | Keep GL entry control centralized |
| Fiscal period | `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java` | Add close precheck/close/reopen approval semantics |
| Treasury | `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/TreasuryController.java` | Extend toward cross-source payment batches |
| Bank reconciliation | `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/BankReconciliationController.java` | Reuse deterministic matching/reversal |
| Budget API/controller | `[EXISTS] be/src/main/java/com/bemo/hr/budget/api/BudgetApi.java`, `BudgetController.java` | Reuse committed/actual concepts; add versions/revisions/transfers |
| Approval API/controller | `[EXISTS] be/src/main/java/com/bemo/hr/approval/ApprovalApi.java`, `ApprovalController.java` | Shared mandatory approval layer; do not clone |

## Known behavior that constrains design

### Payroll
The existing payroll service is monthly-sheet oriented and currently embeds policy examples such as monthly base-salary/hour conversion and overtime multiplier. The refactor should **extract policy into effective-dated component/rule definitions** while keeping an adapter for the current payroll API until the new `PayrollRun` flow is stable.

### Procurement
Current procurement already covers PO → GRN → supplier return → supplier invoice/payment → three-way match. Therefore:
- requisition/RFQ/quotation/award are **new upstream documents**;
- do not recreate PO/GRN/invoice/payment tables/services merely to support the new upstream chain;
- link new documents into existing PO creation/issue flow.

### Sales
Current order creation is header-focused and confirmation already performs a customer-credit check. Therefore:
- lines must become part of the existing order aggregate;
- reservation must execute inside/after the existing confirmation transaction according to policy;
- delivery/invoice quantities derive from line fulfillment, not free-entered header totals.

### Manufacturing
Current start behavior issues planned raw materials immediately, and current completion produces a single finished-goods receipt. Do not patch around that with a second production module. Refactor `ManufacturingService` so the current endpoints can temporarily call the new material issue/receipt services.

## Before adding any `[NEW]` class

Developer must perform these IDE searches:

```text
1. Search class/responsibility name, not only proposed name.
2. Search endpoint fragment and table/entity concept.
3. Search service calls from current controller.
4. Search repository/package for existing status enum and operationId/version handling.
5. Search frontend data-access service before creating a new HTTP service.
6. Search current test style in the same package and follow it.
```

If an equivalent exists, update this package's proposed path locally and extend the existing component.
