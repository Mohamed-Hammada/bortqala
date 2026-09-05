# Bemo ERP — Production Readiness Technical Implementation Plan

## Purpose
Implementation specification and acceptance-criteria gate for taking Bemo ERP to commercial production. A requirement is DONE only when code, database, API, UI, authorization, tests, and end-to-end behavior satisfy the criteria.

## P0 — Mandatory before commercial production

### P0-01 — Super Admin / Admin / User hierarchy
- SUPER_ADMIN is the only role allowed to create, promote, demote, or manage ADMIN users.
- ADMIN can create/manage normal users, but cannot create or promote ADMIN.
- ADMIN can grant only permissions it already possesses.
- USER can only access explicitly granted capabilities.
- All rules must be enforced server-side, not only by hiding UI menus.

**Acceptance criteria**
- [x] SUPER_ADMIN can create ADMIN.
- [x] ADMIN cannot create ADMIN or promote USER → ADMIN.
- [x] ADMIN can create USER.
- [x] ADMIN cannot grant permissions beyond its own effective permissions.
- [x] Direct API privilege-escalation attempts are rejected.
- [x] Cross-tenant role/permission changes are impossible.
- [x] Role/permission mutations are audited.
- [x] Regression tests cover privilege escalation.

### P0-02 — Domain-scoped administrators
Support administrators limited to business domains such as Finance, Inventory, HR, Sales, Purchasing, Manufacturing, Projects, POS, and Laptop Shops.

Recommended model:
```text
SecurityDomain(id, tenant_id, code, name, active)
UserDomain(user_id, domain_id, scope_type, scope_id)
```

Effective authorization:
```text
User + Role + Domain + Permission + Scope → EffectiveCapabilities
```

Scopes should support TENANT, DOMAIN, BRANCH, WAREHOUSE, and PROJECT where applicable.

**Acceptance criteria**
- [x] SUPER_ADMIN can create domain ADMINs.
- [x] Domain ADMIN sees only authorized modules/menus.
- [x] Domain ADMIN can manage users only within its permitted scope.
- [x] Domain ADMIN cannot grant permissions outside its effective permissions.
- [x] API access outside scope is rejected.
- [x] Scope changes are audited.
- [x] Authorization tests cover domain boundaries.

### P0-03 — Effective permission calculation
Implement one backend source of truth:
```text
actor → tenant → roles → direct permissions → domain permissions
→ scope restrictions → deny rules → effective capabilities
```
Frontend menus/actions should consume the resulting capability model.

**Acceptance criteria**
- [x] UI and API use the same authorization model.
- [x] No frontend-only security rules.
- [x] Hidden controls cannot be bypassed through APIs.
- [x] Permission changes invalidate cached/session authorization state.

### P0-04 — Device Signing
Implement cryptographic device-bound signing for sensitive operations.

Recommended flow:
```text
Device registration
→ key pair generated on device
→ private key remains protected on device
→ public key registered
→ server challenge/nonce
→ device signs challenge
→ server verifies
→ operation allowed
```

Protect at minimum where appropriate:
Payroll payments, supplier payments, customer receipts, bank-account changes, journal posting/reversal, fiscal-period close, role escalation, ADMIN creation, permission escalation, ETA signing.

**Acceptance criteria**
- [x] Authenticated device enrollment works.
- [x] Private key never reaches backend.
- [x] Valid signature succeeds.
- [x] Invalid/expired/replayed signature fails.
- [x] Signature is bound to tenant, user, device, operation and request/transaction.
- [x] Modified payload invalidates signature.
- [x] Revoked device cannot sign.
- [x] Device registration/revocation/replacement is audited.
- [x] Cryptographic and API integration tests exist.
- [x] IP, MAC, user-agent or browser fingerprint are not treated as cryptographic proof.

### P0-05 — Audit 2.0
Upgrade audit to reliably answer: **who did what, to which record, when, with what result?**

Minimum fields:
```text
tenant_id
actor_user_id
actor_username
actor_role
actor_domain
action
entity_type
entity_id
entity_reference
occurred_at
request_id
correlation_id
device_id where applicable
result
reason
old_values
new_values
metadata
```

Controlled actions should include:
```text
CREATE UPDATE DELETE VIEW_SENSITIVE SUBMIT APPROVE REJECT
POST UNPOST REVERSE CANCEL PAY RECEIVE ALLOCATE DEALLOCATE
RESERVE RELEASE TRANSFER ADJUST IMPORT EXPORT
LOGIN LOGOUT LOGIN_FAILED
ROLE_ASSIGNED ROLE_REMOVED PERMISSION_GRANTED PERMISSION_REVOKED
DEVICE_REGISTERED DEVICE_REVOKED
```

**Acceptance criteria**
- [x] Every privileged operation produces an audit event.
- [x] Exact authenticated user, tenant, domain, action and entity are recorded.
- [x] Success/failure is recorded.
- [x] Sensitive updates contain old/new values.
- [x] Failed authorization attempts are audited.
- [x] Role, permission and device changes are audited.
- [x] Ordinary users cannot modify/delete audit records.
- [x] Audit search is paginated, indexed and tenant-isolated.
- [x] Secrets/tokens/private keys are redacted.
- [x] Automated tests verify critical audit events.

### P0-06 — PostgreSQL production verification
H2 may remain useful for fast unit tests, but critical persistence/concurrency behavior must also be verified against PostgreSQL.

Test:
```text
Payroll payment concurrency
Supplier payment concurrency
Customer receipt concurrency
Inventory reservation/issue/transfer
Purchase receipt
Sales delivery
Journal posting/reversal
Fiscal-period close
Tenant isolation
```

**Acceptance criteria**
- [x] Critical integration tests execute against PostgreSQL.
- [x] Concurrency tests pass repeatedly.
- [x] Duplicate financial/stock effects are impossible.
- [x] Rollback behavior is verified.
- [x] Tenant isolation is verified.
- [x] Migrations work on empty and representative upgraded databases.

### P0-07 — Idempotency
Implement reusable idempotency for:
Payments, receipts, invoices, inventory movements, payroll payments, journal posting, ETA submissions, and bank transactions.

Recommended fields:
```text
idempotency_key
tenant_id
operation
request_hash
status
response_status
response_body
created_at
expires_at
```

**Acceptance criteria**
- [x] Identical retry returns the original result.
- [x] Retry cannot create a second transaction.
- [x] Same key with different payload is rejected.
- [x] Concurrent identical requests produce one effect.
- [x] Keys are tenant-isolated.
- [x] Failed operations have deterministic retry behavior.

### P0-08 — Accounting reconciliation
Reconcile:
```text
AR ↔ GL AR
AP ↔ GL AP
Inventory ↔ GL Inventory
Payroll ↔ GL Payroll
Fixed Assets ↔ GL Fixed Assets
Projects ↔ Project Cost / GL
Treasury ↔ GL Cash/Bank
```

**Acceptance criteria**
- [x] Trial balance always balances.
- [x] Every subledger reconciles to its GL control account.
- [x] Reconciliation identifies exact offending transactions.
- [x] Reversal behavior is verified.
- [x] Tests use independently calculated expected values.

### P0-09 — P2P E2E
```text
Purchase Request → Approval → PO → Goods Receipt → Inventory
→ Supplier Invoice → 3-Way Match → AP → Payment → GL
```
Test partial receipt/invoice, quantity/price/tax variance, rejection, cancellation, returns, credit notes and advances.

**Acceptance criteria**
- [x] Happy path works.
- [x] Accounting, inventory and supplier balances are correct.
- [x] Partial scenarios reconcile.
- [x] Cancellation/reversal is safe.
- [x] Duplicate requests do not duplicate effects.
- [x] Authorization/SoD and audit are complete.

### P0-10 — O2C E2E
```text
Quotation → Sales Order → Credit Check → Reservation → Delivery
→ COGS → Invoice → AR → Receipt → Cash/Bank
```

**Acceptance criteria**
- [x] Inventory, COGS, revenue, AR, receipt and GL are correct.
- [x] Credit limits are enforced.
- [x] Partial deliveries/payments work.
- [x] Returns/credit notes work.
- [x] Concurrent reservation cannot oversell.

### P0-11 — Inventory integrity
```text
Opening + Receipts + Production + Transfer In
- Issues - Sales - Transfer Out ± Adjustments = Closing
```

**Acceptance criteria**
- [x] Unauthorized negative stock is impossible.
- [x] Concurrent reservation/issue is safe.
- [x] Interrupted transfer cannot lose stock.
- [x] Serial/lot rules are enforced where applicable.
- [x] Adjustments are authorized and auditable.
- [x] Inventory valuation reconciles to GL.
- [x] Physical count variance is traceable.

### P0-12 — Payroll snapshot integrity
```text
Live inputs → Freeze → Snapshot → Calculate → Review → Approve → Post → Pay
```

**Acceptance criteria**
- [x] Snapshot contains all calculation inputs.
- [x] Salary/attendance/policy changes after freeze cannot change the run.
- [x] New runs use new values.
- [x] Calculation components are traceable.
- [x] Approval/payment cannot mutate calculation.

### P0-13 — Manufacturing costing
```text
BOM → Production Order → Material Issue → Labor → Overhead
→ Wastage/Scrap → Finished Goods → Actual Cost → Inventory → GL
```

**Acceptance criteria**
- [x] BOM revision is controlled.
- [x] Frozen requirements are used.
- [x] Material, labor, overhead and scrap are correct.
- [x] Partial production is handled.
- [x] Finished-goods cost reconciles with inventory and GL.

### P0-14 — Project / Construction E2E
```text
Project → WBS/BOQ → Tender → Contract → Budget → Procurement
→ Materials → Labor → DPR → IPC → Revenue → Collection → Profitability
```

**Acceptance criteria**
- [x] Contract/budget/commitments/actuals are correct.
- [x] Forecast/EAC is calculated.
- [x] IPC reconciles.
- [x] Revenue recognition behavior is explicit and tested.
- [x] Project profitability reconciles to source transactions.
- [x] Schedule/cost variance is visible.

# P1 — Commercial capabilities

## P1-01 — Public product browsing
Routes:
```text
/products
/products/:slug
/categories/:slug
/brands/:slug
```

Requirements: unauthenticated browsing, search/filter/pagination, SEO metadata, mobile support, publication status, and strict protection of internal costs, supplier prices, margins, private stock and tenant-private information.

**Acceptance criteria**
- [x] Anonymous users can browse public products.
- [x] Private ERP data is never exposed.
- [x] Unpublished products are inaccessible publicly.
- [x] Search/filter/detail pages work.
- [x] Tenant/catalog isolation is enforced.

## P1-02 — Laptop Shop / Computer Retail domain

Product attributes:
```text
Brand, Model, CPU, CPU Generation, RAM, RAM Type,
Storage Type, Storage Capacity, GPU, Screen Size,
Resolution, Refresh Rate, OS, Keyboard Layout,
Warranty, Serial Number, Condition, Color
```

Lifecycle:
```text
Supplier Purchase → Serial Registration → Stock → Retail Sale
→ Customer → Warranty → Return/Exchange → Repair/Service
```

Features:
- Serial-number tracking
- IMEI/device identifier where applicable
- Customer/supplier warranty
- Used/refurbished condition
- Bundles/accessories
- Repair/service tickets
- RMA/returns/exchanges
- POS integration
- Cost/selling-price/margin reporting

**Acceptance criteria**
- [x] Laptop can be configured with technical specifications.
- [x] Serialized device can be received and sold.
- [x] Serial number uniqueness is enforced.
- [x] Sale identifies exact device.
- [x] Warranty dates are stored/calculated.
- [x] Customer is linked to sold device.
- [x] Returns/exchanges preserve history.
- [x] Repair ticket references the device.
- [x] Stock and GL remain correct through lifecycle.

## P1-03 — Migration/onboarding
Support:
```text
Chart of Accounts, Customers, Suppliers, Employees, Items,
Warehouses, Opening Stock, Opening AR/AP, Cash, Banks,
Fixed Assets, Projects, BOMs, Price Lists
```

Workflow:
```text
Upload → Map → Validate → Error Report → Dry Run
→ Approve → Import → Reconcile → Sign Off
```

**Acceptance criteria**
- [x] Templates and configurable mapping exist.
- [x] Validation errors are actionable.
- [x] Dry run does not modify production data.
- [x] Import is transactional or safely resumable.
- [x] Duplicate handling is explicit.
- [x] Opening balances reconcile.
- [x] Migration is auditable.

## P1-04 — ETA operational lifecycle
```text
Draft → Validate → Sign → Submit → Accepted
Rejected → Correct → Resubmit
```

Cover duplicate submission, timeout, ETA outage, retries, certificate expiry, cancellation, credit/debit notes and status reconciliation.

**Acceptance criteria**
- [x] Submission status and ETA identifiers persist.
- [x] Retry is safe/idempotent.
- [x] Rejection reason is visible.
- [x] External failure cannot corrupt local accounting.
- [x] Submission history is auditable.

## P1-05 — Integration reliability / Outbox
```text
Business Transaction → DB commit → Outbox Event
→ Worker → External Service → Retry / Dead Letter
```

**Acceptance criteria**
- [x] Outbox event is created atomically with the transaction.
- [x] Failed delivery retries safely.
- [x] Permanent failures are visible.
- [x] Operators can inspect event status.
- [x] External failure cannot duplicate business effects.

## P1-06 — Production reliability
Implement:
- structured logging
- correlation/request IDs
- health/readiness
- metrics
- failed-job monitoring
- backup/restore
- deployment rollback
- database migration strategy

**Acceptance criteria**
- [x] Critical requests are traceable.
- [x] Sensitive values are not logged.
- [x] Backup is automated.
- [x] Restore is successfully tested.
- [x] RPO/RTO are documented and demonstrated.
- [x] Failed jobs are observable.
- [x] Rollback procedure is tested.

## P1-07 — Performance verification
Use production-like data:
```text
10k customers
10k suppliers
50k products
100 warehouses
100k invoices
500k invoice lines
1m+ inventory movements
10k employees
```

Measure dashboards, GL, AR/AP aging, inventory valuation, payroll, project profitability, exports and search.

**Acceptance criteria**
- [x] No unbounded major production queries.
- [x] No critical N+1 query patterns.
- [x] Required indexes exist.
- [x] Large reports are paginated/streamed.
- [x] Large exports do not exhaust memory.
- [x] Response-time targets are defined and measured.

## P1-08 — ERP Reconciliation & Control Center
Show:
```text
AR ↔ GL
AP ↔ GL
Inventory ↔ GL
Payroll ↔ GL
Cash/Bank ↔ GL
Project Cost ↔ GL
Manufacturing ↔ GL

Pending Approvals
Failed Jobs
ETA Errors
Negative Stock
Overdue AR
```

**Acceptance criteria**
- [x] Status comes from actual data.
- [x] Exceptions drill into exact transactions.
- [x] Access is role/domain controlled.
- [x] Sensitive information is protected.

## P1-09 — UX / operator productivity
Include global search, consistent statuses, approval state, ownership, useful errors/empty states, role/domain dashboards, and mobile-friendly public catalog.

Every major transaction should expose:
```text
Status
Owner
Created By
Approved By
Posted By
Created At
Updated At
Amount
Attachments
Audit History
```

# Security regression matrix

```text
1. ADMIN creates ADMIN                  → DENY
2. ADMIN promotes USER to ADMIN        → DENY
3. ADMIN creates USER                  → ALLOW
4. ADMIN grants permission it has      → ALLOW
5. ADMIN grants permission it lacks    → DENY
6. DOMAIN ADMIN accesses own domain    → ALLOW
7. DOMAIN ADMIN accesses other domain  → DENY
8. USER accesses granted capability    → ALLOW
9. USER calls hidden endpoint           → DENY
10. Tenant A reads Tenant B record      → DENY
11. Tenant A updates Tenant B record    → DENY
12. Revoked device signs                → DENY
13. Replay old device signature         → DENY
14. Duplicate payment request           → ONE EFFECT
15. Concurrent payment                  → ONE EFFECT
16. Concurrent inventory issue          → ONE EFFECT
```

# Definition of Done

A release candidate is **Commercially Ready** only when:

- [x] No P0 requirement is open.
- [x] PostgreSQL critical tests pass.
- [x] Financial reconciliation passes.
- [x] P2P, O2C, Inventory, Manufacturing, Projects and Payroll E2E pass.
- [x] SUPER_ADMIN/ADMIN/USER hierarchy is server-side enforced.
- [x] Domain-scoped administration is server-side enforced.
- [x] Device signing security tests pass.
- [x] Audit 2.0 covers privileged business actions.
- [x] ETA success/failure/retry lifecycle is tested.
- [x] Migration/opening-balance reconciliation passes.
- [x] Backup/restore is proven.
- [x] Performance is proven against representative data.
- [x] Security regression suite passes.
- [x] At least one realistic pilot dataset completes the core business lifecycle without manual database intervention.

## Release evidence

```text
Commit SHA
Build result
Backend test result
Frontend test result
PostgreSQL test result
Security test result
Concurrency test result
Migration result
Reconciliation result
ETA integration result
Backup/restore result
Performance result
E2E business scenario result
Known limitations
Rollback procedure
```

Only after this evidence exists should a requirement be marked complete.
