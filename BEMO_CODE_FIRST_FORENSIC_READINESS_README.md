# Bemo ERP — Code-First Forensic Production Readiness

## Purpose

This is the master checklist for independently verifying whether Bemo ERP is actually implemented and commercially ready.

**Critical rule:** README files, `STATUS.md`, task lists, commit messages, test counts, and an AI agent saying "DONE" are not sufficient evidence.

A requirement is **VERIFIED** only when the relevant implementation, wiring, security, database behavior, tests, and execution evidence support it.

## Status

- 🟢 **VERIFIED** — implementation is present, wired, and meaningfully tested.
- 🟡 **PARTIAL** — implementation exists but important pieces are missing.
- 🔴 **FALSE POSITIVE** — documentation says done, but the claimed implementation is absent, disconnected, or materially incomplete.
- ⚫ **NOT IMPLEMENTED** — no meaningful implementation found.
- 🔵 **UNVERIFIED** — implementation appears present but evidence is insufficient.

---

# 1. Verification methodology

For every requirement inspect:

1. Actual source implementation
2. Database tables/migrations/constraints
3. Domain/business service
4. Backend authorization
5. API endpoint
6. Frontend route/component
7. Audit behavior
8. Idempotency/concurrency where relevant
9. Unit tests
10. Integration tests
11. E2E tests
12. Negative/security tests
13. Actual execution evidence

Required evidence record:

```text
Requirement ID
Status
Source files
Database objects
Production callers
API
Frontend
Authorization
Audit
Tests
E2E
Known gaps
Required work
```

**Dead code, unused services, migrations without callers, frontend-only security, and documentation-only claims are not VERIFIED.**

---

# 2. P0 — Identity and authorization

## P0-01 SUPER_ADMIN → ADMIN

### Requirement

Only SUPER_ADMIN may create, promote, demote, or manage ADMIN users.

### Must verify

```text
SUPER_ADMIN → create ADMIN       ALLOW
ADMIN       → create ADMIN       DENY
USER        → create ADMIN       DENY
ADMIN       → promote USER       DENY
ADMIN       → create USER        ALLOW
```

### Acceptance criteria

- [ ] SUPER_ADMIN can create ADMIN.
- [ ] ADMIN cannot create ADMIN.
- [ ] ADMIN cannot promote USER → ADMIN.
- [ ] ADMIN cannot assign SUPER_ADMIN.
- [ ] USER cannot change protected roles.
- [ ] Direct API privilege escalation is rejected.
- [ ] Cross-tenant role changes are impossible.
- [ ] Every role mutation is audited.

## P0-02 ADMIN → normal users

- [ ] ADMIN can create normal USER.
- [ ] ADMIN can manage users only within authorized scope.
- [ ] ADMIN cannot create/promote ADMIN.
- [ ] ADMIN cannot create/promote SUPER_ADMIN.
- [ ] All changes are audited.

## P0-03 Permission subset rule

An ADMIN may grant only permissions that the ADMIN itself possesses:

```text
GrantablePermissions(ADMIN)
    ⊆
EffectivePermissions(ADMIN)
```

Example:

```text
ADMIN has:
PRODUCT_VIEW
PRODUCT_EDIT

ADMIN attempts:
PAYROLL_APPROVE

Result:
DENY
```

### Acceptance criteria

- [ ] Rule enforced in backend.
- [ ] UI shows only grantable permissions.
- [ ] API rejects forged permission payloads.
- [ ] Permission removal is protected.
- [ ] Negative tests exist.
- [ ] Permission mutations are audited.

## P0-04 Domain-scoped ADMIN

Support domains such as:

```text
Finance
Inventory
Purchasing
Sales
HR
Payroll
Manufacturing
Projects
POS
Laptop Shops
```

Recommended model:

```text
SecurityDomain
  id
  tenant_id
  code
  name
  active

UserDomain
  user_id
  domain_id
  scope_type
  scope_id
```

Effective authorization:

```text
User + Tenant + Role + Domain + Permission + Scope
        ↓
Effective Capabilities
```

Scopes may include:

```text
TENANT
DOMAIN
BRANCH
WAREHOUSE
PROJECT
```

### Acceptance criteria

- [ ] SUPER_ADMIN can create domain ADMIN.
- [ ] Domain ADMIN can access only its domain.
- [ ] Domain ADMIN cannot access another domain through APIs.
- [ ] Domain ADMIN cannot grant another domain.
- [ ] Data queries enforce scope.
- [ ] Mutations enforce scope.
- [ ] Scope changes are audited.
- [ ] Cross-domain negative tests exist.

## P0-05 Menu security

Hiding a menu is **not authorization**.

Required:

```text
Frontend visibility
+
Backend authorization
+
Data-level scope enforcement
```

Tests must attempt direct URLs and direct API calls.

---

# 3. P0 — Device Signing

## Requirement

Implement real cryptographic device-bound signing for sensitive operations.

Expected lifecycle:

```text
Register Device
→ Generate Key Pair
→ Protect Private Key on Device
→ Register Public Key
→ Server Challenge / Nonce
→ Device Signs
→ Server Verifies
→ Sensitive Operation Allowed
```

A device table or fingerprint is **not** device signing.

### Verify

- key generation
- asymmetric cryptography
- private-key protection
- public-key registration
- challenge/nonce
- expiry
- request binding
- signature verification
- replay protection
- revocation
- replacement
- audit

### Sensitive operations to evaluate

```text
ADMIN creation
Role escalation
Permission escalation
Bank-account changes
Supplier payment
Customer receipt
Payroll payment
Journal posting
Journal reversal
Fiscal-period close
ETA signing/submission
```

### Acceptance criteria

- [ ] Private key never reaches backend.
- [ ] Valid signature succeeds.
- [ ] Invalid signature fails.
- [ ] Expired challenge fails.
- [ ] Replayed signature fails.
- [ ] Signature from another device fails.
- [ ] Signature from another user fails.
- [ ] Signature from another tenant fails.
- [ ] Modified payload fails verification.
- [ ] Revoked device cannot sign.
- [ ] Device lifecycle is audited.
- [ ] Cryptographic tests exist.
- [ ] API integration tests exist.

---

# 4. P0 — Audit 2.0

Audit must answer:

```text
WHO
did WHAT
to WHICH ENTITY
WHEN
under WHICH TENANT/DOMAIN
using WHICH DEVICE
with WHAT RESULT
and WHAT CHANGED
```

Recommended fields:

```text
id
tenant_id
actor_user_id
actor_role
actor_domain
action
entity_type
entity_id
entity_reference
occurred_at
request_id
correlation_id
device_id
result
reason
old_values
new_values
metadata
```

Example:

```json
{
  "actor": "Ahmed",
  "action": "UPDATE",
  "entityType": "Customer",
  "entityId": "1234",
  "result": "SUCCESS",
  "changes": {
    "creditLimit": {
      "old": 100000,
      "new": 250000
    }
  }
}
```

Required actions include:

```text
CREATE UPDATE DELETE VIEW_SENSITIVE
SUBMIT APPROVE REJECT POST UNPOST REVERSE CANCEL
PAY RECEIVE ALLOCATE DEALLOCATE RESERVE RELEASE
TRANSFER ADJUST IMPORT EXPORT
LOGIN LOGOUT LOGIN_FAILED
ROLE_ASSIGNED ROLE_REMOVED
PERMISSION_GRANTED PERMISSION_REVOKED
DEVICE_REGISTERED DEVICE_REVOKED
```

### Acceptance criteria

- [ ] Critical operations create audit events.
- [ ] Exact user is recorded.
- [ ] Tenant/domain are recorded.
- [ ] Action/entity are recorded.
- [ ] Timestamp is recorded.
- [ ] Result is recorded.
- [ ] Sensitive changes contain old/new values.
- [ ] Failed authorization attempts are audited.
- [ ] Role/permission/device changes are audited.
- [ ] Normal users cannot modify/delete audit history.
- [ ] Secrets/tokens/private keys are redacted.
- [ ] Audit is tenant-isolated.
- [ ] Audit search is indexed/paginated.
- [ ] Critical audit events have automated tests.

---

# 5. P0 — Database and transaction integrity

## P0-05 Idempotency

Required for:

```text
Payments
Receipts
Invoices
Inventory movements
Payroll payments
Journal posting
ETA submissions
Bank transactions
```

Expected:

```text
Request A → one business effect
Retry A   → original result
A + A concurrently → one effect
```

Acceptance:

- [ ] Idempotency key persisted.
- [ ] Request hash checked.
- [ ] Same key/different payload rejected.
- [ ] Concurrent duplicate requests produce one effect.
- [ ] Keys are tenant-isolated.

## P0-06 PostgreSQL verification

H2 can remain useful for fast unit tests.

It is **not enough** as the only proof of PostgreSQL-sensitive behavior.

Run critical integration/concurrency tests against PostgreSQL for:

```text
transactions
locking
unique constraints
foreign keys
isolation
native queries
JSON behavior
pagination
tenant isolation
```

Acceptance:

- [ ] Critical integration tests run on PostgreSQL.
- [ ] Concurrency tests run on PostgreSQL.
- [ ] Rollback tests run on PostgreSQL.
- [ ] Tenant isolation tests run on PostgreSQL.
- [ ] Empty database migrations work.
- [ ] Upgrade migrations work.

---

# 6. P0 — Financial reconciliation

Required:

```text
AR ↔ GL
AP ↔ GL
Inventory ↔ GL
Payroll ↔ GL
Fixed Assets ↔ GL
Treasury ↔ GL
Projects ↔ GL
Manufacturing ↔ GL
```

Acceptance:

- [ ] Trial balance balances.
- [ ] AR reconciles to GL.
- [ ] AP reconciles to GL.
- [ ] Inventory valuation reconciles.
- [ ] Payroll liabilities reconcile.
- [ ] Cash/bank reconciles.
- [ ] Project cost reconciles.
- [ ] Manufacturing cost reconciles.
- [ ] Reconciliation identifies exact offending transactions.

---

# 7. P0 — End-to-end ERP workflows

## P0-07 Procure to Pay

```text
Purchase Request
→ Approval
→ PO
→ Goods Receipt
→ Inventory
→ Supplier Invoice
→ 3-Way Match
→ AP
→ Payment
→ GL
```

Test:

```text
full receipt
partial receipt
over/short receipt
price variance
tax variance
partial invoice
rejection
cancellation
return
credit note
advance
duplicate request
concurrent payment
```

## P0-08 Order to Cash

```text
Quotation
→ Sales Order
→ Credit Check
→ Reservation
→ Delivery
→ COGS
→ Invoice
→ AR
→ Receipt
→ Cash/Bank
→ GL
```

Test:

```text
full/partial delivery
credit limit breach
partial payment
return
credit note
concurrent reservation
duplicate receipt
```

## P0-09 Inventory

Invariant:

```text
Opening
+ Receipts
+ Production
+ Transfer In
- Issues
- Sales
- Transfer Out
± Adjustments
= Closing
```

Acceptance:

- [ ] No unauthorized negative stock.
- [ ] Concurrent issue/reservation is safe.
- [ ] Transfers cannot lose stock.
- [ ] Serial/lot rules work where applicable.
- [ ] Adjustments are authorized/audited.
- [ ] Valuation reconciles to GL.

## P0-10 Payroll

```text
Live Inputs
→ Freeze
→ Snapshot
→ Calculate
→ Review
→ Approve
→ Post
→ Pay
```

After snapshot:

```text
salary change
attendance change
policy change
employee change
```

must not change the frozen calculation.

## P0-11 Manufacturing

```text
BOM
→ Production Order
→ Material Issue
→ Labor
→ Overhead
→ Scrap/Wastage
→ Finished Goods
→ Actual Cost
→ Inventory
→ GL
```

Verify BOM revision, partial production, scrap, labor, overhead and cost reconciliation.

## P0-12 Projects / Construction

```text
Project
→ WBS/BOQ
→ Tender
→ Contract
→ Budget
→ Procurement
→ Materials
→ Labor
→ DPR
→ IPC
→ Revenue
→ Collection
→ Profitability
```

Verify budget, commitments, actuals, EAC, IPC, subcontractor costs, revenue, profitability and cost/schedule variance.

---

# 8. P1 — Public product catalog

Required public routes:

```text
/products
/products/:slug
/categories/:slug
/brands/:slug
```

Anonymous users must never receive:

```text
supplier cost
internal cost
margin
private stock
tenant-private data
accounting information
```

Acceptance:

- [ ] Anonymous browsing works.
- [ ] Search/filter/pagination work.
- [ ] Product detail works.
- [ ] Draft products are not public.
- [ ] Tenant/catalog isolation works.
- [ ] Mobile layout works.
- [ ] SEO metadata exists where appropriate.

---

# 9. P1 — Laptop / Computer Shop domain

Product attributes:

```text
Brand
Model
CPU
CPU Generation
RAM
RAM Type
Storage Type
Storage Capacity
GPU
Screen Size
Resolution
Refresh Rate
Operating System
Keyboard Layout
Warranty
Serial Number
Condition
Color
```

Lifecycle:

```text
Supplier Purchase
→ Serial Registration
→ Stock
→ Sale
→ Customer
→ Warranty
→ Return/Exchange
→ Repair
```

Required capabilities:

- serialized inventory
- warranty
- used/refurbished condition
- accessories/bundles
- RMA
- repair/service tickets
- customer/device relationship
- POS integration
- margin reporting

Acceptance:

- [ ] Serialized laptop can be received.
- [ ] Serial uniqueness enforced.
- [ ] Exact serial recorded on sale.
- [ ] Customer linked to device.
- [ ] Warranty dates calculated/stored.
- [ ] Return preserves history.
- [ ] Repair references device.
- [ ] Inventory and GL remain correct.
- [ ] Laptop reports exist.

---

# 10. P1 — ETA integration

Lifecycle:

```text
Draft → Validate → Sign → Submit → Accepted
Rejected → Correct → Resubmit
```

Test:

```text
timeout
duplicate submission
ETA outage
retry
certificate expiry
rejection
cancellation
credit note
debit note
status synchronization
```

Acceptance:

- [ ] Submission state persists.
- [ ] ETA identifier persists.
- [ ] Duplicate submission prevented.
- [ ] Retry is safe.
- [ ] Rejection reason visible.
- [ ] External failure cannot corrupt accounting.
- [ ] Submission history audited.

---

# 11. P1 — Outbox / integration reliability

Required:

```text
Business Transaction
→ DB Commit
→ Outbox Event
→ Worker
→ External Service
→ Retry / Dead Letter
```

Acceptance:

- [ ] Outbox event is atomically created with transaction.
- [ ] Failed delivery retries.
- [ ] Duplicate delivery cannot duplicate business effects.
- [ ] Dead-letter events are visible.
- [ ] Operators can inspect event status.

---

# 12. P1 — Migration / onboarding

Support:

```text
Chart of Accounts
Customers
Suppliers
Employees
Products
Warehouses
Opening Stock
Opening AR/AP
Cash
Banks
Fixed Assets
Projects
BOMs
Price Lists
```

Workflow:

```text
Upload
→ Map
→ Validate
→ Error Report
→ Dry Run
→ Approve
→ Import
→ Reconcile
→ Sign Off
```

Acceptance:

- [ ] Templates exist.
- [ ] Mapping is configurable.
- [ ] Validation errors are actionable.
- [ ] Dry run has no production side effects.
- [ ] Duplicate handling is explicit.
- [ ] Import is safe/resumable.
- [ ] Opening balances reconcile.
- [ ] Import is auditable.

---

# 13. P1 — Production reliability

Required:

```text
Structured Logs
Request ID
Correlation ID
Health Endpoint
Readiness Endpoint
Metrics
Failed Job Monitoring
Backup
Restore
Rollback
Migration Strategy
```

Acceptance:

- [ ] Critical requests are traceable.
- [ ] Sensitive values are not logged.
- [ ] Failed jobs are observable.
- [ ] Backup is automated.
- [ ] Restore has actually been tested.
- [ ] RPO/RTO are documented.
- [ ] Rollback is documented and tested.

---

# 14. P1 — Performance

Use representative data:

```text
10,000 customers
10,000 suppliers
50,000 products
100 warehouses
100,000 invoices
500,000 invoice lines
1,000,000 inventory movements
10,000 employees
```

Measure:

```text
Dashboard
Trial Balance
AR Aging
AP Aging
Inventory Valuation
Payroll
Project Profitability
Search
Exports
```

Acceptance:

- [ ] No major N+1 patterns.
- [ ] Major queries are indexed.
- [ ] Large lists are paginated.
- [ ] Large exports are streamed/backgrounded where appropriate.
- [ ] Major screens do not load all records into memory.
- [ ] Response-time targets are documented and measured.

---

# 15. ERP Control Center

Recommended:

```text
AR ↔ GL                 ✓
AP ↔ GL                 ✓
Inventory ↔ GL          ✓
Payroll ↔ GL            ✓
Cash/Bank ↔ GL          ⚠
Projects ↔ GL           ✓
Manufacturing ↔ GL      ✓

Pending Approvals
Failed Jobs
ETA Errors
Negative Stock
Overdue AR
```

The values must come from actual data, with drill-down to exceptions.

---

# 16. Security regression matrix

Every release should execute:

```text
ADMIN creates ADMIN                 → DENY
ADMIN promotes USER → ADMIN        → DENY
ADMIN creates USER                 → ALLOW
ADMIN grants permission it owns    → ALLOW
ADMIN grants permission it lacks   → DENY
DOMAIN ADMIN own domain            → ALLOW
DOMAIN ADMIN other domain          → DENY
USER granted capability            → ALLOW
USER calls hidden endpoint         → DENY
Tenant A reads Tenant B            → DENY
Tenant A updates Tenant B          → DENY
Revoked device signs               → DENY
Replay old signature               → DENY
Duplicate payment                  → ONE EFFECT
Concurrent payment                 → ONE EFFECT
Concurrent inventory issue         → ONE EFFECT
ADMIN assigns SUPER_ADMIN          → DENY
Domain ADMIN escalates domain      → DENY
```

---

# 17. AI-agent false-positive detection

A feature can be incorrectly reported as complete when the agent:

- creates a class but never wires it
- creates a migration but never uses the table
- adds tests that mock away the real behavior
- hides a menu without securing the API
- implements only the happy path
- adds documentation without implementation
- creates a service with no production caller
- creates an endpoint bypassing the real business service
- relies on H2 for PostgreSQL-sensitive behavior
- adds a test whose expected result is generated by the same broken implementation

Therefore:

```text
Search implementation
→ Find production callers
→ Follow execution path
→ Inspect DB
→ Inspect authorization
→ Inspect API
→ Inspect frontend
→ Inspect tests
→ Run negative test
→ Run realistic workflow
```

If code is dead:

```text
NOT VERIFIED
```

If only documentation exists:

```text
FALSE POSITIVE
```

---

# 18. Test-quality rule

Test count is not proof of correctness.

Weak:

```java
assertEquals(service.calculate(x), service.calculate(x));
```

Strong:

```text
Independent expected business result
+
actual result
+
business invariant
+
database verification
+
negative test
+
concurrency test
```

Financial tests should use independently calculated expected values wherever possible.

---

# 19. Commercial readiness gate

Do not label the system **PRODUCTION READY** unless all critical P0 requirements are 🟢 VERIFIED.

The following automatically block production:

```text
Critical security failure
Tenant isolation failure
Financial duplication
Incorrect accounting
Inventory corruption
Incorrect payroll
Authorization bypass
Unverified device signing
Incomplete audit
Unsafe migration
```

Even a high percentage score does not override these blockers.

---

# 20. Recommended scoring

```text
Security & Authorization       20%
Financial Integrity            20%
Core ERP Workflows             20%
Database Integrity             10%
Audit & Compliance             10%
Reliability / Operations       10%
Performance                     5%
UX / Commercial Features        5%
```

```text
90–100  Potentially production ready, if all P0 gates pass
80–89   Strong beta/pilot candidate
70–79   Functional product; hardening required
50–69   Controlled pilot only
<50     Not commercially ready
```

---

# 21. Review output template

Use this exact structure for every requirement:

```text
ID:
Requirement:
Status: 🟢 / 🟡 / 🔴 / ⚫ / 🔵

Implementation:
- file:
- class:
- method:
- production caller:

Database:
- table:
- migration:
- constraint/index:

Backend:
- endpoint:
- service:
- authorization:

Frontend:
- route:
- component:
- permission/menu:

Audit:
- event:
- old/new values:
- actor:

Tests:
- unit:
- integration:
- E2E:
- negative:
- concurrency:

Execution evidence:
- command:
- result:

Problems:
1.
2.
3.

Required work:
1.
2.
3.

Final verdict:
```

---

# 22. Release evidence

Store:

```text
release-evidence/
├── commit.txt
├── backend-tests.txt
├── frontend-tests.txt
├── postgres-tests.txt
├── security-tests.txt
├── concurrency-tests.txt
├── reconciliation.txt
├── migration.txt
├── eta-tests.txt
├── backup-restore.txt
├── performance.txt
└── known-limitations.md
```

Each release should record:

```text
Commit SHA
Build result
Backend tests
Frontend tests
PostgreSQL tests
Security tests
Concurrency tests
Migration result
Reconciliation result
ETA result
Backup/restore result
Performance result
E2E result
Known limitations
Rollback procedure
```

# Final rule

**Never mark a feature DONE because an AI agent says it is DONE.**

The final authority is:

```text
Source Code
+
Database
+
Runtime Wiring
+
Authorization
+
Tests
+
Real Execution
+
Business Invariants
```

If documentation and implementation disagree, **implementation wins and documentation must be corrected**.
