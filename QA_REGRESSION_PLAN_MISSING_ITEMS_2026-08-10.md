# Bortqala ERP — QA Regression Plan for Confirmed Missing Items

**Repository:** `Mohamed-Hammada/bortqala`  
**Branch:** `fm_bemo_consolidated`  
**Baseline:** `d0750842db64ed8862145df9213dc2cb410cbe66`  
**Date:** 2026-08-10

Purpose: give QA a direct test plan for the missing-item fixes.

---

# 1. Test data preparation

Create at minimum:

```text
Supplier S1
Warehouse W1
Warehouse W2
Inventory item A
Inventory item B
Finished item FG1
Raw material RM1
Raw material RM2
Active budget/department
Trial tenant T1
Subscription plans BASIC and BUSINESS
```

Initial stock example:

```text
RM1 = 100 KG
RM2 = 200 EA
FG1 = 0 EA
```

---

# 2. Procurement receiving tests

## QA-PROC-RCV-001 — Direct receive endpoint must not bypass GRN

Precondition:

```text
PO = ISSUED
No GRN exists
```

Attempt legacy direct receive.

Expected after fix:

```text
endpoint removed/deprecated/rejected
OR
it routes into a valid receipt command requiring receipt data
```

Must never produce:

```text
PO = RECEIVED
with no GRN and no inventory movement
```

Severity if failed: Critical.

---

## QA-PROC-RCV-002 — Partial receipt

PO:

```text
Item A ordered = 100
```

Receive:

```text
Delivered 40
Rejected 5
Deducted 5
Accepted 30
```

Expected:

```text
GRN created
Inventory +30
PO = PARTIALLY_RECEIVED
Remaining receivable = 70
Rejected/deducted do not increase stock
```

---

## QA-PROC-RCV-003 — Final receipt

After previous case, accept remaining 70.

Expected:

```text
Total accepted = 100
PO = RECEIVED
Budget release behavior correct
Inventory total increase = 100
```

---

## QA-PROC-RCV-004 — Over receipt

Remaining = 10.

Attempt accepted = 11.

Expected:

```text
request rejected
no GRN created
no stock change
no PO status change
```

---

## QA-PROC-RCV-005 — Wrong PO line

Try using a line from PO-2 in receipt for PO-1.

Expected: rejected.

---

## QA-PROC-RCV-006 — Wrong item

Correct PO line but different inventory item.

Expected: rejected.

---

## QA-PROC-RCV-007 — Supplier mismatch

Receipt supplier != PO supplier.

Expected: rejected.

---

## QA-PROC-RCV-008 — Retry/idempotency

If idempotency is implemented, submit same receipt twice due to network retry.

Expected:

```text
one business receipt
one stock effect
```

Never double stock.

---

## QA-PROC-RCV-009 — Concurrent final receipt

Remaining quantity = 10.

Two sessions simultaneously attempt receiving 10.

Expected:

```text
only one succeeds
total accepted never becomes 20
```

---

# 3. Procurement cancellation / return tests

## QA-PROC-CAN-001 — Cancel draft

Expected: allowed.

## QA-PROC-CAN-002 — Cancel issued with no receipt

Expected: allowed if business policy allows.

## QA-PROC-CAN-003 — Cancel partially received

Expected:

```text
ordinary cancellation rejected
error explains supplier return/reversal is required
stock unchanged
```

## QA-PROC-CAN-004 — Cancel fully received

Expected: rejected.

## QA-PROC-CAN-005 — Return part of receipt

Received = 100.

Return = 25.

Expected:

```text
Supplier Return posted
Inventory -25
Return history linked to original GRN
Net received = 75
```

## QA-PROC-CAN-006 — Over-return

Received = 100.
Already returned = 25.
Attempt return = 76.

Expected: rejected because returnable = 75.

## QA-PROC-CAN-007 — Lot/location return

Receipt was into:

```text
Warehouse W1
Location L1
Lot LOT-001
```

Return must preserve correct traceability.

## QA-PROC-CAN-008 — Return concurrency

Two sessions try to return the final returnable quantity.

Expected: one wins; net returned cannot exceed received.

## QA-PROC-CAN-009 — PO with supplier invoice/payment

Verify the system either:

```text
handles financial reversal/credit note
```

or:

```text
blocks the return/cancellation with a clear workflow instruction
```

It must never silently leave contradictory financial state.

---

# 4. Manufacturing BOM tests

## QA-MFG-BOM-001 — BOM requires inventory finished item

Create BOM with valid FG1.

Expected: stored by item ID, not only name.

## QA-MFG-BOM-002 — BOM components

Create:

```text
FG1 yield = 10
RM1 = 4 KG
RM2 = 10 EA
```

Expected lines persisted.

## QA-MFG-BOM-003 — Invalid component

Unknown/inactive component should be rejected according to business rule.

## QA-MFG-BOM-004 — Versioning

Create V1, then V2.

Existing production order using V1 must continue to refer to V1.

---

# 5. Manufacturing lifecycle tests

## QA-MFG-LIFE-001 — Legal start

PLANNED → IN_PROGRESS.

Expected: allowed.

## QA-MFG-LIFE-002 — Complete PLANNED directly

Expected: rejected.

## QA-MFG-LIFE-003 — Restart completed order

Expected: rejected.

## QA-MFG-LIFE-004 — Complete cancelled order

Expected: rejected.

## QA-MFG-LIFE-005 — Cancel with issued material

Expected: controlled reversal required; no orphan material issue.

---

# 6. Manufacturing material requirement tests

BOM:

```text
Yield = 10 FG1
RM1 = 4 KG
RM2 = 10 EA
```

Target production:

```text
100 FG1
```

Expected planned requirement:

```text
RM1 = 40 KG
RM2 = 100 EA
```

Add waste and verify formula.

---

# 7. Manufacturing inventory tests

## QA-MFG-INV-001 — Start/issue material

Initial:

```text
RM1 = 100
RM2 = 200
```

Issue:

```text
RM1 = 40
RM2 = 100
```

Expected:

```text
RM1 on hand = 60
RM2 on hand = 100
production issue movements exist
```

## QA-MFG-INV-002 — Insufficient material

Set RM1 available = 20 but required = 40.

Expected according to configured policy:

```text
block
or explicit warning/approval
```

Never silently pretend inventory exists.

## QA-MFG-INV-003 — Return unused material

Issued RM1 = 40.
Consumed = 38.
Return = 2.

Expected net consumption = 38 and stock adjusted correctly.

## QA-MFG-INV-004 — Finished-goods receipt

Complete actual good quantity = 98.

Expected:

```text
FG1 +98
production receipt movement exists
order = COMPLETED
```

## QA-MFG-INV-005 — Scrap/reject

Produce:

```text
Good 95
Rejected 3
Scrap 2
```

Verify only proper available output enters sellable stock.

## QA-MFG-INV-006 — Atomic rollback

Force finished-goods posting to fail after material validation/issue stage.

Expected:

```text
whole completion transaction rolls back
no partial stock effect
order remains non-completed
```

This is a critical test.

## QA-MFG-INV-007 — Concurrent completion

Two sessions complete same production order.

Expected: one completion effect only.

---

# 8. Manufacturing costing tests

## QA-MFG-COST-001

If RM1 issue cost is 5 per KG and consumed quantity is 40:

```text
material cost = 200
```

Verify production actual cost.

## QA-MFG-COST-002

Where more than one material is used, verify summed cost.

## QA-MFG-COST-003

Verify returned material is not counted as consumed cost.

---

# 9. Trial/subscription conversion tests

## QA-SUB-CONV-001 — Successful trial conversion

Precondition:

```text
tenant commercial state = TRIAL
selected plan = BUSINESS
```

Expected after one operation:

```text
commercial state = PAID
TenantSubscription = BUSINESS / ACTIVE (or configured paid status)
entitlements = BUSINESS plan
limits = BUSINESS plan
subscription history contains conversion
audit contains conversion
```

---

## QA-SUB-CONV-002 — Missing plan

Attempt conversion without a derivable/selected plan.

Expected: rejected; tenant remains TRIAL.

---

## QA-SUB-CONV-003 — Inactive plan

Expected: rejected; no state changed.

---

## QA-SUB-CONV-004 — Entitlement failure rollback

Simulate entitlement application failure.

Expected:

```text
commercial state remains TRIAL
subscription remains previous state
no partial conversion
```

---

## QA-SUB-CONV-005 — Same operation ID retry

Send operation ID `ABC` twice.

Expected:

```text
one logical conversion
one subscription change
no duplicate entitlement mutation
```

---

## QA-SUB-CONV-006 — Different operation ID after conversion

Expected behavior must be explicit.

Recommended:

```text
conversion endpoint reports already converted
plan changes use subscription-management flow
```

---

## QA-SUB-CONV-007 — Expired trial can upgrade

Set trial expired.

Normal application write:

Expected:

```text
blocked/read-only
```

Conversion/upgrade operation:

Expected:

```text
allowed through designated commercial endpoint
```

After success:

```text
normal plan-authorized writes restored
```

---

## QA-SUB-CONV-008 — User limits after conversion

Before:

```text
trial limit = X
```

After BUSINESS conversion:

```text
user limit = BUSINESS configured value
```

Try adding users to the new limit boundary.

Expected enforcement uses new subscription immediately.

---

# 10. Security tests

For each new endpoint:

- unauthorized user;
- VIEWER;
- correct business manager role;
- cross-tenant record ID attempt.

Expected:

```text
no unauthorized mutation
no cross-tenant data exposure
```

---

# 11. Audit tests

For receipt, return, production completion, and conversion verify audit includes:

```text
actor
time
record id
operation
relevant related document
```

If before/after data is part of the project's normal audit pattern, verify it too.

---

# 12. Localization tests

Test Arabic and English for all new business errors.

No raw backend code such as:

```text
PROC_ORDER_HAS_RECEIPTS
```

should be the final user-facing message when the UI normally translates error codes.

---

# 13. Final sign-off matrix

| Area | Backend | UI | DB | Audit | Concurrency | Arabic/English | Signed off |
|---|---|---|---|---|---|---|---|
| PO receiving | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| PO cancellation/return | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Manufacturing | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |
| Trial/subscription | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ |

Do not close the parent missing-item issue until every relevant column is complete.
