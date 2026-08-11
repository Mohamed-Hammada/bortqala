# Posting Profiles, Subledgers and General Ledger Integration

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Goal

Every posted business event must produce a deterministic, linked financial result without users manually recreating its journal effect.

## Existing anchors

- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingApi.java`
- `[EXISTS] be/src/main/java/com/bemo/hr/finance/api/AccountingController.java`
- `[EXISTS]` journal create/post/reverse and fiscal-period concepts in finance.
- `[EXISTS]` inventory valuation/movement logic behind `OperationsApi` / `OperationsController`.

## Proposed posting profile model

Before creating new tables, inspect finance entities/config tables for account mappings. Extend if possible.

Logical model:

```text
posting_profile
- id
- code
- business_event
- effective_from / effective_to
- version
- active

posting_profile_line
- profile_id
- line_no
- side (DEBIT/CREDIT)
- account_source (FIXED/ITEM_PROFILE/PARTY_PROFILE/TAX_PROFILE/...)
- fixed_account_id nullable
- amount_source
- dimension_source
```

Posted source stores `postingProfileId` + `postingProfileVersion` used.

## Initial business events

| Event | Debit | Credit |
|---|---|---|
| INVENTORY_RECEIPT_UNINVOICED | Inventory | GR/IR |
| SUPPLIER_INVOICE_MATCHED | GR/IR + recoverable tax | AP control |
| SUPPLIER_PAYMENT | AP control | Bank/cash |
| SALES_INVOICE | AR control | Revenue + output tax |
| SALES_DELIVERY | COGS | Inventory |
| CUSTOMER_RECEIPT | Bank/cash | AR control |
| PAYROLL_POST | Salary/employer expense | payroll/tax/insurance liabilities |
| PAYROLL_PAYMENT | Payroll payable | Bank/cash |
| CONTRACTOR_SETTLEMENT_POST | Labor expense/project | Contractor payable |
| INVENTORY_REVALUATION_UP | Inventory | Revaluation gain |
| MATERIAL_ISSUE_TO_WIP | WIP | Raw material inventory |
| FG_RECEIPT_FROM_WIP | Finished goods | WIP |

## Source journal linkage

Each generated journal must retain:

```text
sourceModule
sourceDocumentType
sourceDocumentId
sourceDocumentNumber
sourceEvent
postingProfileId
postingProfileVersion
fiscalPeriodId
transactionCurrency
baseCurrency
exchangeRate
exchangeRateSource
operationId
```

Use existing journal metadata fields where present before adding columns.

## Posting service contract

`[NEW]` suggested only if no current equivalent: `SubledgerPostingService`.

Pseudo-flow:

```text
post(source, event, operationId, expectedVersion)
  validate source state and period
  resolve effective posting profile
  resolve accounts/dimensions
  compute lines deterministically
  validate debit == credit
  create journal through existing Accounting service
  post journal using existing posting logic
  persist source ↔ journal link + profile version
  mark source POSTED
  audit
```

Do not update GL balances directly from procurement/payroll/workforce code.

## Reconciliation model

For each subledger create a query/service that calculates:

```text
subledger control balance
minus GL control account balance
= difference
```

Drill-down difference by source document. Initial recon types:
- AP supplier balance ↔ AP control;
- AR customer balance ↔ AR control;
- inventory valuation ↔ inventory control;
- payroll liability ↔ payroll payable;
- contractor payable ↔ contractor control if separated.

## Tests

- missing posting profile blocks posting with clear error;
- profile version selected by document/effective date;
- journal balanced;
- duplicate operation creates no second journal;
- stale source version rejected;
- closed fiscal period rejected;
- reversal creates linked opposite journal, not edit;
- source/journal currency and dimensions retained;
- recon returns zero for controlled scenario and exact source difference for deliberately broken test fixture.
