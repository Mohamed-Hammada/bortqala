# Bortqala ERP — Implementation Plan for Confirmed Missing Items

**Repo:** `Mohamed-Hammada/bortqala`  
**Branch:** `fm_bemo_consolidated`  
**Baseline commit:** `d0750842db64ed8862145df9213dc2cb410cbe66`  
**Date:** 2026-08-10

This file is the implementation companion to `README_MISSING_ITEMS_DETAILED_2026-08-10.md`.

---

# 1. Workstream map

| Epic | Depends on | Can run in parallel? |
|---|---|---|
| EPIC-PROC-01 Single receiving path | Existing procurement/operations | Yes |
| EPIC-PROC-02 Supplier return / cancellation protection | EPIC-PROC-01 recommended | Yes, partially |
| EPIC-MFG-01 Transactional manufacturing | Inventory item/movement services | Yes |
| EPIC-SUB-01 Atomic trial conversion | Subscription/entitlement services | Yes |

Recommended teams:

```text
Procurement/Inventory developer → EPIC-PROC-01 + EPIC-PROC-02
Manufacturing developer         → EPIC-MFG-01
Platform/Subscription developer → EPIC-SUB-01
QA                              → test design from day one
```

---

# 2. EPIC-PROC-01 — Single Goods Receipt receiving path

## Backend tasks

### PROC-001
Find all usages of:

```text
ProcurementService.receive(...)
POST /orders/{id}/receive
```

Classify every caller.

### PROC-002
Deprecate/remove the direct receive endpoint.

Preferred result:

```text
There is exactly one supported physical receiving command:
create/post Goods Receipt.
```

### PROC-003
Centralize PO receipt-state calculation.

Create a service/domain operation that calculates:

```text
accepted per line
remaining per line
overall state
```

Possible result:

```text
ISSUED
PARTIALLY_RECEIVED
RECEIVED
```

### PROC-004
Ensure the Goods Receipt transaction performs:

```text
validate
persist receipt
persist lines
post inventory
recalculate PO
release budget if complete
audit
```

in one transaction.

### PROC-005
Add idempotency if client retries can create duplicate GRNs.

### PROC-006
Ensure inventory movement references include enough traceability:

```text
PO ID/number
GRN ID/number
supplier
item
warehouse/location
lot if applicable
actor
timestamp
```

## Frontend tasks

### PROC-FE-001
Remove any button that calls status-only receive.

### PROC-FE-002
Use a Goods Receipt dialog/page.

### PROC-FE-003
Show line-level quantities and remaining balance.

### PROC-FE-004
Prevent invalid quantity input before submit, while keeping backend as authority.

### PROC-FE-005
After saving, refresh:

```text
PO
GRN list
stock/movements if shown
budget status
```

## Tests

See QA file cases `QA-PROC-RCV-*`.

---

# 3. EPIC-PROC-02 — Cancellation protection and supplier return

## Immediate hardening

### PROC-RET-001
Before cancelling a PO:

```text
load accepted receipt quantity
if any > 0 → reject normal cancellation
```

### PROC-RET-002
Define safe cancellation states.

Recommended:

```text
DRAFT → cancel allowed
ISSUED with zero accepted receipt → cancel allowed
PARTIALLY_RECEIVED → ordinary cancel forbidden
RECEIVED → ordinary cancel forbidden
CANCELLED → no-op/reject consistently
```

### PROC-RET-003
Protect POs with downstream invoices/payments.

Do not allow cancellation to bypass financial reversal.

## Supplier-return feature

### PROC-RET-010
Add SupplierReturn header.

### PROC-RET-011
Add SupplierReturn lines linked to original receipt/PO lines.

### PROC-RET-012
Calculate returnable quantity.

Recommended formula:

```text
returnable
=
accepted receipts
- already posted returns
```

### PROC-RET-013
Post outbound inventory movement.

### PROC-RET-014
Update net receipt view/status.

### PROC-RET-015
Add audit/timeline relation.

### PROC-RET-016
Define invoice/credit-note behavior.

A minimal implementation may block the return when the financial side cannot yet be handled, but it must never silently corrupt accounting.

---

# 4. EPIC-MFG-01 — Transactional manufacturing

## Phase 1 — BOM correctness

### MFG-001
Replace free-text finished product identity with `finishedItemId`.

Keep a display name only as a denormalized/view field if desired.

### MFG-002
Create `BomLine`.

Required fields:

```text
componentItemId
quantityPer
uom
wastePercent
lineNumber
```

### MFG-003
Add BOM revision/version.

### MFG-004
Add BOM effective dates/status.

### MFG-005
Create migration/backfill strategy for current BOM records.

Do not guess ambiguous inventory matches.

---

## Phase 2 — Production lifecycle

### MFG-010
Replace unrestricted status assignment with guarded operations.

Example:

```text
start()
complete()
cancel()
```

### MFG-011
Enforce:

```text
PLANNED → IN_PROGRESS
IN_PROGRESS → COMPLETED
PLANNED → CANCELLED
```

Add other transitions only after defining reversal behavior.

### MFG-012
Store BOM version/snapshot on the production order.

Historical production must not change when a BOM is later edited.

---

## Phase 3 — Material requirements

### MFG-020
Calculate planned component requirement.

### MFG-021
Read available inventory.

### MFG-022
Show shortages.

### MFG-023
Define shortage policy:

```text
BLOCK
WARN
ALLOW_WITH_APPROVAL
```

if business flexibility is needed.

---

## Phase 4 — Material issue / return

### MFG-030
Create material-issue document/table.

### MFG-031
Post negative production issue inventory movement.

### MFG-032
Track actual consumption separately from planned requirement.

### MFG-033
Support return of unused material.

### MFG-034
Preserve warehouse/location/lot traceability.

---

## Phase 5 — Production output

### MFG-040
Add actual output quantity.

### MFG-041
Add rejected/scrap quantity.

### MFG-042
Add output warehouse/location/lot.

### MFG-043
Post finished-goods inventory receipt.

### MFG-044
Make completion atomic.

No partial commit.

---

## Phase 6 — Cost

### MFG-050
Calculate material actual cost.

### MFG-051
Store total actual production cost and unit cost.

### MFG-052
Later add:

```text
labor
machine
overhead
subcontracting
```

---

## Phase 7 — UI

Production order page:

```text
Overview
Requirements
Issues
Consumption
Output
Quality
Cost
Timeline
```

Start action should show readiness.

Complete action should capture actual output and final consumption.

---

# 5. EPIC-SUB-01 — Atomic trial conversion

## Architecture task

### SUB-001
Define a single invariant for:

```text
TenantApplication.commercialState
TenantSubscription.status
SubscriptionPlan
Entitlements
```

Document it in code and tests.

## Refactor

### SUB-010
Extract reusable plan activation logic from `SubscriptionService.change(...)`.

Candidate:

```text
SubscriptionLifecycleService
```

### SUB-011
Create `TrialConversionService`.

### SUB-012
Conversion command should include or deterministically derive:

```text
planCode
startsAt
renewsAt
endsAt
reason
operationId
expectedVersion
```

### SUB-013
Single transaction:

```text
lock tenant
validate
activate/change subscription
apply entitlements
record subscription history
mark tenant paid
audit
commit
```

### SUB-014
Keep operation-id idempotency.

### SUB-015
Test expired-trial upgrade route is not blocked by read-only interceptor.

## Frontend

### SUB-FE-001
Conversion UI requires plan selection.

### SUB-FE-002
Show plan features/limits.

### SUB-FE-003
After success reload tenant + subscription + usage/entitlements.

### SUB-FE-004
Never display "Paid" success while subscription activation failed.

---

# 6. Migration and rollout

Recommended release sequence:

```text
Release A:
- block unsafe direct receive
- block cancel-after-receipt
- regression tests

Release B:
- supplier returns
- traceability tests

Release C:
- BOM components/versioning
- migrate existing BOMs

Release D:
- material issue/output transaction
- production completion atomicity

Release E:
- atomic trial conversion
```

If operational customers already use the direct receive endpoint, do not remove it without checking stored historical POs.

Create a diagnostic SQL/report before rollout:

```text
Find POs status RECEIVED
where no Goods Receipt exists
```

Those records require reconciliation.

Also check:

```text
Find CANCELLED POs
with Goods Receipts
```

These are historical candidates for inconsistent inventory/document state.

Manufacturing diagnostics:

```text
Find COMPLETED production orders
```

Because current completion may not have inventory postings, these records need a migration decision rather than automatic stock creation.

Commercial diagnostics:

```text
Find PAID tenants
with no current active subscription
```

Do not automatically assign plans without a deterministic rule or manual review.

---

# 7. Pull request boundaries

Prefer small reviewable PRs rather than one giant change.

Suggested PRs:

```text
PR-1 procurement: disable status-only receive
PR-2 procurement: cancellation receipt guard
PR-3 procurement: supplier return domain/posting
PR-4 manufacturing: BOM line + version model
PR-5 manufacturing: legal production lifecycle
PR-6 manufacturing: material issue and output posting
PR-7 manufacturing: costing/quality integration
PR-8 subscription: shared lifecycle + atomic trial conversion
PR-9 frontend/regression cleanup
```

Each PR should include tests.

---

# 8. Completion checklist

Before closing all four findings:

- [ ] Backend compile/tests pass.
- [ ] Frontend build/tests pass.
- [ ] DB migrations tested on existing database.
- [ ] Rollback behavior tested.
- [ ] Audit events verified.
- [ ] Tenant isolation tested.
- [ ] Authorization tested.
- [ ] Arabic/English error translations added.
- [ ] Idempotency/concurrency tested.
- [ ] QA regression completed.
- [ ] Old endpoints/actions are not reachable.
- [ ] Docs updated with final behavior.
