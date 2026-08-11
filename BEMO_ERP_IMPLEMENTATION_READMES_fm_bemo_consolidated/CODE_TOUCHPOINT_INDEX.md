# Code Touchpoint Index

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


This file is a lookup sheet, not a claim that suggested `[NEW]` names already exist.

## Backend — verified anchors

| Module | File | What to inspect/change first |
|---|---|---|
| Workforce | `be/src/main/java/com/bemo/hr/workforce/WorkforceApi.java` | command/response records, operation/version fields |
| Workforce settlement | `be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementController.java` | settlement calculate/approve/post/payment transitions |
| Reporting/attendance | `be/src/main/java/com/bemo/hr/reporting/api/ReportController.java` | report decisions, approval/reopen, snapshot hook |
| Payroll | `be/src/main/java/com/bemo/hr/payroll/api/PayrollApi.java` | current sheet/payment contract |
| Payroll | `be/src/main/java/com/bemo/hr/payroll/api/PayrollController.java` | current endpoints, compatibility adapters |
| Payroll | `be/src/main/java/com/bemo/hr/payroll/application/PayrollService.java` | current monthly calculation/readiness/payment/reversal |
| Procurement | `be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementApi.java` | PO/GRN/invoice/payment/match DTOs |
| Procurement | `be/src/main/java/com/bemo/hr/trade/procurement/api/ProcurementController.java` | `/api/v1/trade/procurement` current flow |
| Sales | `be/src/main/java/com/bemo/hr/trade/sales/api/SalesApi.java` | header DTO → line aggregation |
| Sales | `be/src/main/java/com/bemo/hr/trade/sales/api/SalesController.java` | create/confirm + current credit check |
| Inventory | `be/src/main/java/com/bemo/hr/operations/OperationsApi.java` | stock/UOM/movement DTOs |
| Inventory | `be/src/main/java/com/bemo/hr/operations/OperationsController.java` | current movement/valuation actions |
| Manufacturing | `be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingApi.java` | production DTOs |
| Manufacturing | `be/src/main/java/com/bemo/hr/manufacturing/production/api/ManufacturingController.java` | start/complete/cancel API |
| Manufacturing | `be/src/main/java/com/bemo/hr/manufacturing/production/application/ManufacturingService.java` | planned full issue + one completion receipt refactor |
| Accounting | `be/src/main/java/com/bemo/hr/finance/api/AccountingApi.java` | journal commands/source metadata |
| Accounting | `be/src/main/java/com/bemo/hr/finance/api/AccountingController.java` | create/post/reverse ownership |
| Period | `be/src/main/java/com/bemo/hr/finance/api/FiscalPeriodController.java` | status/version → precheck/close/reopen |
| Treasury | `be/src/main/java/com/bemo/hr/finance/api/TreasuryController.java` | payment/cash position anchors |
| Bank | `be/src/main/java/com/bemo/hr/finance/api/BankReconciliationController.java` | import/match/reverse |
| Budget | `be/src/main/java/com/bemo/hr/budget/api/BudgetApi.java` | budget/commitment DTOs |
| Budget | `be/src/main/java/com/bemo/hr/budget/api/BudgetController.java` | budget operations |
| Approval | `be/src/main/java/com/bemo/hr/approval/ApprovalApi.java` | workflow model/DTOs |
| Approval | `be/src/main/java/com/bemo/hr/approval/ApprovalController.java` | workflow execution/history |

## Frontend — verified anchors

| Area | File/folder | Developer action |
|---|---|---|
| Root routes | `fe/src/app/app.routes.ts` | add workbench/new child routes using current lazy/guard conventions |
| Workforce routes | `fe/src/app/features/workforce/workforce.routes.ts` | add dispatch/dispute/workbench routes |
| Workforce | `fe/src/app/features/workforce/pages/labor-requests/` | request dimensions/approval/dispatch link |
| Workforce | `.../pages/manual-attendance/` | lock/correction state |
| Workforce | `.../pages/settlement-periods/` | version/dispute/invoice-match/post/payment |
| Workforce | `.../pages/workers/` | assignment history |
| Workforce | `.../pages/contractors/` | commercial terms/performance |
| Workforce | `.../pages/advances/` | deduction schedule/approval integration |
| Workforce | `.../pages/contractor-accounts/` | payable reconciliation |
| Procurement | `fe/src/app/features/trade/procurement/procurement.page.ts` | shell/state/actions; split children gradually |
| Procurement | `.../procurement.page.html` | tabs/forms/workbench |
| Procurement | `.../procurement.page.spec.ts` | regression/action tests |
| Sales | `fe/src/app/features/trade/sales/sales.page.ts` | lines/fulfillment/reservation state |
| Sales | `.../sales.page.html` | line grid/deliveries/returns |
| Sales | `.../sales.page.spec.ts` | line/credit/action tests |
| Approvals | `fe/src/app/features/approvals/` | shared approval queue/status |
| Audit | `fe/src/app/features/audit-logs/` | source history links |
| Finance accounts | `fe/src/app/features/finance/accounts/` | COA/account posting/dimension rules |
| Finance banks | `fe/src/app/features/finance/banks/` | bank master/treasury links |
| Finance budgets | `fe/src/app/features/finance/budgets/` | budget UI; reconcile with budget module APIs |
| Finance journals | `fe/src/app/features/finance/journal-entries/` | source links/manual controls/reversal |
| Finance tax/currency | `fe/src/app/features/finance/tax-currency/` | tax/FX versioning and period-end rates |
| Period | `fe/src/app/features/fiscal-periods/` | close workbench |
| Manufacturing | `fe/src/app/features/manufacturing/` | actual execution/WIP/QC |
| Inventory | `fe/src/app/features/operations/` | warehouse/reservation/transfer/count |

## Mandatory “locate before create” targets

Exact persistence service/repository/entity names were not guessed when not verified. In IDE, locate them by tracing the verified controllers above. Proposed new classes in module READMEs are architectural names; place them beside the existing package conventions.
