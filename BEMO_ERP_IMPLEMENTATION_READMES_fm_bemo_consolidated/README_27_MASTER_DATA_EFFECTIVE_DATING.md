# Master Data Governance and Effective Dating — Technical Implementation

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

Prevent advanced business cycles from producing inconsistent historical results because supplier bank data, worker rates, tax rates, BOMs, price lists or posting rules were edited in place. Critical master data must be validated, audited and—where the value affects historical calculations—effective-dated or snapshotted onto transactions.

## Existing areas to inspect first

Before adding generic master tables, locate current entities/services/pages for:
- customers and suppliers in trade modules;
- contractors/workers/employees in workforce/HR;
- item/category/UOM under operations;
- accounts, banks, currencies and tax under finance;
- BOMs under manufacturing;
- users/roles/audit/approval infrastructure.

Known frontend anchors include `[EXISTS] fe/src/app/features/finance/accounts/`, `banks/`, `tax-currency/`, workforce contractors/workers/categories and operations/manufacturing features.

## Party governance

A future shared `Party` abstraction is optional and **must not be introduced as a disruptive rewrite**. Phase 1 can keep current customer/supplier/contractor tables and add shared validation/service conventions.

Required controls:
- duplicate tax ID check by configured scope;
- duplicate bank-account fingerprint/check where permitted;
- active/inactive status with reason/date;
- bank-account change audit;
- optional approval before changed supplier/contractor bank account can be used for payment;
- historical payment keeps beneficiary bank snapshot.

If a unified Party model is later required, migrate incrementally with role links rather than replacing all foreign keys in one release.

## Item master

Ensure current item model can represent:

```text
itemType = STOCK | SERVICE | RAW | WIP | FINISHED | EXPENSE
baseUom
purchaseUom
salesUom
costingMethod/category
lotSerialPolicy
expiryPolicy
defaultTaxCode
defaultInventoryProfile
defaultRevenueProfile
defaultCogsProfile
defaultVarianceProfile
```

Do not duplicate inventory/revenue account fields if posting-profile/account mapping already provides the same function; define one source of truth.

## Effective-dated concepts

Use effective-from/effective-to + version for data that determines historical calculations:

- employee/worker rates;
- contractor commercial terms;
- attendance/payroll rules;
- tax rates;
- approved FX transaction/period-end rates;
- BOM revisions;
- price lists;
- budget versions;
- posting profiles.

### Resolver rule

`resolve(businessDate)` returns exactly one active version. Database/service validation prevents overlapping active versions for the same scope unless explicit priority semantics exist.

### Snapshot rule

Even with effective dating, a posted/approved transaction stores the resolved version ID and critical values used. Historical re-display must not resolve “today's version.”

## Suggested reusable service

Only after IDE search:

```text
EffectiveVersionResolver<T>
- resolve(scope, businessDate)
- assertNoOverlap(scope, from, to, excludedId)
- closeCurrentAndCreateNew(...)
```

Do not build a dynamic generic entity framework. A small reusable overlap/date utility plus typed repositories is safer.

## Bank master change flow

Recommended supplier/contractor banking flow:

`DRAFT_CHANGE → SUBMITTED → APPROVED → EFFECTIVE`

1. User creates change request/new bank version.
2. Existing active bank record remains usable according to policy until approval/effective date.
3. Approval uses existing approval engine.
4. On effective activation, previous version closes.
5. Treasury payment batch snapshots selected account.
6. Audit never logs unmasked secret fields beyond policy.

## Backend validation pattern

For master update affecting transactional calculations:

```text
if no transactions depend on current version and status=DRAFT:
  edit allowed
else:
  create new version effective from future/approved date
```

Never cascade an edited current rate into already calculated settlement/payroll/invoice history.

## Frontend behavior

For versioned master records show:
- Current version;
- Future scheduled version;
- History;
- Effective dates;
- Used-by count / transaction drill-down where feasible;
- “Create new version” instead of edit when history is protected.

For bank changes show masked values, approval state and effective date.

## Tests

- overlapping versions rejected;
- boundary date resolves correct version;
- historical transaction still displays old version after new master version;
- payment batch bank snapshot unchanged after master update;
- duplicate tax ID warning/block according to config;
- inactive master cannot be selected for new transaction but remains visible historically;
- unauthorized bank change/approval rejected;
- audit includes old/new non-secret metadata and reason.

## Junior developer execution order

1. Inventory current master entities and identify which are currently edited in place.
2. Prioritize master data that affects money/stock calculations.
3. Add effective dating to one pilot (contractor rate or payroll rule).
4. Build overlap resolver tests.
5. Store resolved version on new transactions.
6. Add UI history/version action.
7. Apply pattern to price/tax/BOM/posting/budget rules.
8. Add bank-change approval/snapshot controls.
9. Consider unified Party only after all modules are stable and a real business need justifies migration.
