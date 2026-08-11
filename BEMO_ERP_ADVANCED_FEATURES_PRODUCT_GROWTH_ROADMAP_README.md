# Bemo ERP — Advanced ERP, Intelligence, Vertical SaaS & Product Growth Roadmap

> **Repository reviewed:** `Mohamed-Hammada/bortqala`  
> **Branch:** `fm_bemo_consolidated`  
> **Snapshot used for this document:** `2f377d96d9a0cde1aaf3a8f361d0cef2b731eb92` (2026-08-09)  
> **Primary codebase:** Spring Boot backend (`be/`) + Angular frontend (`fe/`) + PostgreSQL  
> **Purpose:** Turn Bemo from a feature-rich ERP into an advanced, automation-first, explainable, modular **vertical SaaS product** that can be sold, trialed, onboarded, expanded and operated at scale **without making paid AI subscriptions a requirement**.

> **Commercial/product expansion:** This edition also converts the niche-marketing, modular-selling, demo, free-trial, first-10-customer and land-and-expand ideas into concrete ERP/SaaS architecture, data models, workflows, controls and acceptance criteria.

---

# 1. Executive Direction

Bemo already contains a strong ERP foundation. The repository currently includes, among other areas:

- Multi-tenant/app-aware architecture.
- App-scoped authentication and authorization.
- Dynamic roles/menu access.
- Attendance and biometric import/integration.
- Employee and workforce/contractor management.
- Contractor labor requests, attendance, advances, settlements and finance linkage.
- Payroll lifecycle and salary calculation explanation.
- Business parties/suppliers.
- Procurement including PO, GRN, supplier invoice and payment flow.
- Procurement three-way matching.
- Inventory/operations master data and movements.
- Finance accounts, journals, banks, taxes, currency, fiscal periods and budgets.
- Budget control and PO encumbrance.
- Sales.
- Manufacturing and quality screens.
- Configurable approval workflows.
- Action Center/business notifications.
- Dashboards, trends and Excel exports.
- Audit logging.
- App-aware Arabic/English translations and Super Admin translation management.
- Session/logout scope controls.
- Permission-aware shortcuts and user preferences.

The next target should **not** be “add a chatbot everywhere.”

The target should be:

> **Bemo should detect problems, explain calculations, recommend actions, automate repetitive work, enforce policies, predict likely issues, and give every role a prioritized work queue.**

That can mostly be achieved without an LLM.

---

# 2. Intelligence Strategy: No Paid AI Required

## 2.1 Level A — Deterministic Smart ERP

This must be the default and should work for every customer with zero AI subscription.

Use:

- Java/Spring business rules.
- SQL aggregation.
- Scheduled jobs.
- Thresholds.
- Weighted scoring.
- Moving averages.
- Statistical forecasting.
- Historical comparisons.
- Rule-based anomaly detection.
- Workflow engine.
- Event notifications.
- Database materialized/snapshot tables when needed.
- Explainable formulas.

Examples:

- Low-stock detection.
- Overdue invoice detection.
- Supplier scoring.
- Contractor scoring.
- Attendance anomaly detection.
- Payroll anomaly detection.
- Duplicate invoice risk.
- Approval SLA escalation.
- Cash forecast.
- Budget overrun warning.
- Reorder recommendation.
- Late PO warning.
- Expiring contract/document warning.
- Bank statement matching.
- “Next best action.”
- Exception-first dashboard.

## 2.2 Level B — Optional Statistical/ML Intelligence

Still no generative-AI subscription required.

Possible tools:

- Java libraries.
- Python service if justified.
- PostgreSQL analytics.
- Lightweight self-hosted models.
- Isolation Forest for anomalies.
- Regression/time-series models.
- Classification models trained from ERP history.

Examples:

- Demand forecasting.
- Stockout prediction.
- Attendance anomaly score.
- Late-payment probability.
- Supplier late-delivery probability.
- Payroll anomaly score.
- Forecast-at-completion for projects.

## 2.3 Level C — Optional Generative AI

Only after the ERP foundation and structured intelligence are mature.

Allow per tenant:

- Disabled.
- Bring Your Own Key.
- OpenAI.
- Anthropic.
- Gemini.
- Azure-hosted provider.
- Local OpenAI-compatible endpoint.
- On-premise/self-hosted model.

Generative AI should initially be allowed only to:

- Summarize.
- Explain.
- Search.
- Recommend.
- Prepare drafts.

High-risk actions such as posting journals, issuing payments, changing payroll, or finalizing purchasing must remain controlled by Bemo services and approval policies.

---

# 3. Feature Maturity Model

Use this scale when evaluating every module:

| Level | Meaning |
|---|---|
| M0 | Missing |
| M1 | Basic CRUD |
| M2 | Connected workflow |
| M3 | Controlled enterprise process with permissions/audit/state machine |
| M4 | Automated, exception-driven and explainable |
| M5 | Predictive / optimized |
| M6 | Optional agentic/autonomous execution under governance |

The near-term goal for Bemo should be **M4 across critical modules**, not M6.

---

# 4. Cross-Cutting Platform Capabilities to Build First

These capabilities should be shared by all modules rather than reimplemented repeatedly.

## 4.1 Business Event Framework

Create a domain event model.

Examples:

```text
PURCHASE_ORDER_CREATED
PURCHASE_ORDER_ISSUED
GOODS_RECEIPT_POSTED
SUPPLIER_INVOICE_CREATED
SUPPLIER_PAYMENT_POSTED
PAYROLL_APPROVED
PAYROLL_POSTED
STOCK_BELOW_REORDER_POINT
ATTENDANCE_EXCEPTION_DETECTED
APPROVAL_OVERDUE
BUDGET_THRESHOLD_REACHED
DOCUMENT_EXPIRING
```

Recommended event fields:

- `id`
- `app_id`
- `company_id`
- `event_type`
- `entity_type`
- `entity_id`
- `occurred_at`
- `actor_user_id`
- `correlation_id`
- `operation_id`
- `payload_json`
- `severity`
- `processed_at`

### Why

It enables:

- Notifications.
- Scheduled automation.
- External webhooks later.
- Process mining later.
- Action Center.
- Analytics.
- Audit correlation.

---

## 4.2 Rule Engine

Create a reusable rule framework rather than hard-coding every future condition.

Example:

```text
WHEN purchase_order.total > 100000
AND supplier.riskLevel = HIGH
THEN require approval workflow "HIGH_RISK_PO"
```

Another:

```text
WHEN inventory.available < inventory.reorderPoint
THEN create action-center alert
```

Rule definition should support:

- Entity type.
- Trigger event.
- Conditions.
- AND/OR grouping.
- Operator.
- Value.
- Effective dates.
- Branch/company scope.
- Severity.
- Action.
- Active/inactive.
- Priority.
- Version.
- Audit history.

### Future actions

- Notify role/user.
- Create approval instance.
- Create task.
- Create draft transaction.
- Block transaction.
- Add warning.
- Require comment.
- Escalate.

---

## 4.3 Recommendation Framework

Do not store recommendations as temporary frontend text.

Create structured recommendations:

```text
Recommendation
- id
- type
- subject_type
- subject_id
- title_key
- reason_key
- severity
- confidence
- impact_amount
- recommended_action
- status
- generated_at
- expires_at
- accepted_by
- dismissed_by
- execution_reference
```

Statuses:

```text
OPEN
ACCEPTED
DISMISSED
EXPIRED
EXECUTED
NO_LONGER_APPLICABLE
```

This powers a real smart Action Center.

---

## 4.4 Explainability Framework

Every important calculated result should be able to return:

- Formula.
- Inputs.
- Source documents.
- Intermediate values.
- Final result.
- Rule/version used.
- Timestamp.
- Overrides.
- Approvals.

Example endpoint concept:

```text
GET /api/v1/explanations/{entityType}/{entityId}
```

This can cover:

- Salary.
- Settlement.
- Stock balance.
- Inventory valuation.
- Supplier balance.
- Customer balance.
- Budget available amount.
- Tax.
- FX gain/loss.
- Project forecast.
- Reorder recommendation.

---

## 4.5 Task / Work Queue Engine

Notifications are not enough.

Create first-class business tasks:

```text
BusinessTask
- assigned user
- assigned role
- entity
- task type
- priority
- due date
- SLA
- status
- escalation level
- instructions
- action URL
```

Examples:

- Review attendance exceptions.
- Resolve three-way mismatch.
- Approve payment.
- Count warehouse bin.
- Verify supplier bank change.
- Renew contract.
- Reconcile statement line.
- Resolve month-close issue.

This becomes the foundation of the role-based home page.

---

# 5. EXISTING FEATURE — Attendance Management: Advanced Target

## Current baseline

Bemo already has significant attendance capability:

- Employees.
- Dynamic categories.
- Effective schedules.
- Biometric imports.
- Live device integration.
- Single-punch classification.
- Missing-punch handling.
- Outage/mass-disruption logic.
- Report review.
- Bulk decisions.
- Attendance approval/reopen.
- Audit and exports.

## Target

Turn attendance into an **exception-driven workforce time engine**.

Users should not manually inspect normal rows.

The system should automatically classify:

```text
NORMAL
LATE
EARLY_LEAVE
SINGLE_PUNCH
NO_PUNCH
EXCESSIVE_OVERTIME
OFF_DAY_WORK
SCHEDULE_MISMATCH
POSSIBLE_DEVICE_OUTAGE
POSSIBLE_DUPLICATE_PUNCH
MANUAL_OVERRIDE
REQUIRES_REVIEW
```

## Advanced additions

### A. Attendance Policy Engine

Policies configurable by:

- Tenant.
- Company.
- Branch.
- Department.
- Category.
- Employee.
- Effective date.

Rules:

- Grace minutes.
- Late thresholds.
- Early-leave thresholds.
- Minimum worked hours.
- Maximum daily hours.
- Break rules.
- Overtime rules.
- Weekend rules.
- Holiday rules.
- Cross-midnight shifts.
- Multiple shifts per day.
- Flexible schedule.
- Ramadan schedules.
- Seasonal schedules.

Precedence:

```text
Employee
> Category
> Department
> Branch
> Company
> Tenant default
```

### B. Exception Workbench

New screen:

`Attendance > Exception Workbench`

Filters:

- Period.
- Employee.
- Category.
- Department.
- Contractor.
- Device.
- Exception type.
- Severity.
- Review status.
- Confidence.

Columns:

- Employee.
- Expected schedule.
- Punches.
- Exception.
- Evidence.
- Recommended action.
- Current decision.
- Reviewer.

Bulk actions should require preview:

```text
Selected: 39 records
Proposed action: Mark normal day
Reason: Confirmed device outage
Payroll impact: EGP 8,240 protected from deduction
```

### C. Rule-Based Anomaly Score

No paid AI needed.

Example:

```text
Missing punch                        +20
Manual correction                    +15
Repeated correction 3+ times         +20
Arrival > 2h outside usual pattern   +15
Same punch timestamp pattern         +10
Device status unhealthy              -10
Confirmed outage                     -30
```

Result:

```text
0–19   Normal
20–39  Review
40–59  Warning
60+    Critical
```

### D. Attendance “Explain”

For any day:

```text
Expected shift: 08:00–17:00
Grace: 15 minutes

First punch: 08:07
Last punch: 16:55
Worked: 8h 48m
Break deduction: 30m
Payable hours: 8h 18m
Late deduction: 0
Early leave: 5m inside tolerance

Decision: NORMAL
```

### E. Attendance KPIs

- Attendance rate.
- Absence rate.
- Late rate.
- Single-punch rate.
- Manual-correction rate.
- Overtime hours.
- Device failure rate.
- Exceptions per employee.
- Exceptions per category.
- Exceptions per device.
- Payroll impact of corrections.

## QA acceptance

- Policy effective date produces correct historical results.
- No future policy changes rewrite locked attendance.
- Bulk decision is idempotent.
- Same operation cannot double-apply.
- Every manual override is audited.
- Payroll cannot silently use unresolved critical attendance.
- Confirmed outages produce explainable evidence.
- Cross-midnight shifts calculate correctly.

---

# 6. EXISTING FEATURE — Biometric Device Integrations: Advanced Target

## Current baseline

The repository includes:

- File imports.
- Device integrations.
- Scheduled sync.
- Cursor/status persistence.
- Punch idempotency.
- Device employee identity matching.
- ZKT-related work.
- Audit trail.

## Advanced additions

### A. Device Fleet Dashboard

Screen:

`Attendance > Devices`

Cards:

- Online.
- Offline.
- Degraded.
- Last sync.
- Punch backlog.
- Unmatched identities.
- Errors in last 24h.

Per device:

- IP/endpoint.
- Model.
- Firmware.
- Time zone.
- Last heartbeat.
- Clock drift.
- Last punch timestamp.
- Sync cursor.
- Worker count.
- Failure history.

### B. Health Rules

Alert when:

- No heartbeat.
- No punches during expected active hours.
- Device clock differs > configured minutes.
- Punch volume drops abnormally.
- Duplicate punch rate spikes.
- Identity mappings suddenly fail.

### C. Device Assignment

Support:

```text
Device → Company → Branch → Location → Gate
```

### D. Device Maintenance History

Store:

- Installation.
- Replacement.
- Firmware update.
- IP change.
- Credentials rotation.
- Maintenance notes.

### E. Failover

If primary device is unhealthy:

- Highlight backup device.
- Allow temporary manual attendance mode.
- Mark period with evidence.

---

# 7. EXISTING FEATURE — Workforce / Contractors: Advanced Target

## Current baseline

Bemo already has a strong contractor workforce lifecycle:

```text
Contractor
→ Worker
→ Category
→ Labor Request
→ Assignment
→ Attendance
→ Settlement Period
→ Advance/Deduction
→ Review/Approval
→ Journal/Partner Ledger
→ Invoice Link
→ Payment
```

This is a differentiating module and should be made significantly deeper.

## Advanced additions

### A. Contractor 360

Screen:

`Workforce > Contractors > Contractor 360`

Tabs:

- Profile.
- Contracts.
- Workers.
- Labor requests.
- Attendance.
- Settlements.
- Advances.
- Invoices.
- Payments.
- Performance.
- Documents.
- Incidents.
- Audit.

### B. Contractor Performance Score

Weighted score configurable by tenant.

Example:

```text
Attendance reliability     25%
Labor fulfillment          20%
Worker turnover            10%
Cost competitiveness       15%
Quality/productivity       15%
Safety/compliance          10%
Disputes                    5%
```

Output:

```text
Overall 86/100
Trend +4 vs previous quarter
```

### C. Labor Request Planning

Labor request should include:

- Needed category.
- Quantity.
- Shift.
- Start/end.
- Location.
- Department/project.
- Skill requirement.
- Maximum rate.
- Reason.
- Priority.
- Required approval.

System shows:

```text
Requested: 50
Assigned: 42
Checked in: 39
Shortfall: 11
```

### D. Fulfillment Recommendation

Without AI:

Rank contractors using:

```text
Available worker count
Historical fulfillment %
Average daily rate
Attendance reliability
Distance/location
Quality score
Open disputes
```

### E. Worker Compliance

Track:

- ID validity.
- Medical certificate.
- Safety training.
- License.
- Work permit.
- Contract.
- Insurance.
- Expiry.

Block assignment when mandatory compliance is expired.

### F. Settlement Reconciliation

For every settlement:

```text
Expected attendance cost
+ approved overtime
+ allowances
- advances
- deductions
± manual adjustments
= settlement gross
- prior payments
= outstanding
```

Provide a complete explanation and linked evidence.

### G. Rate History

Never overwrite wage rates without history.

Store:

- Worker/category rate.
- Effective from/to.
- Contractor.
- Negotiated rate.
- Currency.
- Approved by.

Settlement uses rate effective on the work date.

## QA acceptance

- Historical settlement remains unchanged after rate update.
- Worker cannot belong to invalid overlapping assignments.
- Advance policy precedence is deterministic.
- Settlement payment cannot exceed outstanding.
- Contractor invoice linkage cannot cause double payment.
- Every override records user/reason/before/after.

---

# 8. EXISTING FEATURE — Payroll: Advanced Target

## Current baseline

Bemo includes:

- Payroll cycles.
- Lifecycle/status controls.
- Disbursement guardrails.
- Advances/deductions.
- Salary explanation.
- Attendance linkage.
- Approval/posting behavior.

## Advanced additions

### A. Payroll Definition Engine

Support earning/deduction components:

```text
BASIC
HOUSING
TRANSPORT
OVERTIME
BONUS
COMMISSION
ALLOWANCE
ABSENCE
LATE_DEDUCTION
ADVANCE_INSTALLMENT
LOAN_INSTALLMENT
TAX
INSURANCE
OTHER
```

Each component needs:

- Code.
- Arabic/English name.
- Type.
- Formula.
- Taxable flag.
- Pensionable/insurable flag.
- GL account.
- Cost center rule.
- Effective dates.
- Rounding rule.

### B. Payroll Simulation

Before calculating final payroll:

> Simulate payroll without posting.

Show differences:

```text
Previous payroll: EGP 1,240,000
Simulation:      EGP 1,318,000
Difference:      +78,000 (+6.29%)
```

Break down by reason.

### C. Payroll Variance Workbench

Automatically flag:

- Net salary change > X%.
- Overtime unusually high.
- Employee paid with unresolved attendance.
- New deduction.
- Negative net.
- Duplicate payment.
- Missing bank account.
- Salary above configured limit.
- Unexpected zero salary.

### D. Payroll Control Totals

Before approval:

```text
Employee count
Gross total
Deductions
Net total
Bank transfer total
Cash total
Department totals
Prior-month variance
```

Approval should require reconciliation.

### E. Payroll GL Distribution

Post by:

- Company.
- Branch.
- Department.
- Cost center.
- Project.

Example:

```text
DR Salary Expense — Production
DR Overtime Expense
CR Payroll Payable
CR Tax Payable
CR Insurance Payable
```

### F. Payslips

Generate:

- Arabic.
- English.
- PDF.
- Secure employee portal.
- Optional email.
- Version/snapshot.
- Digital acknowledgment.

### G. Payroll “Explain”

Expand current explanation to show:

- Rule version.
- Attendance source.
- Effective salary.
- Each component.
- Deduction cap.
- Advance installment.
- Rounding.
- Final posting references.

---

# 9. EXISTING FEATURE — Procurement: Advanced Target

## Current baseline

Bemo already includes:

```text
PO → GRN → Supplier Invoice → Payment
```

with:

- Draft/issue/receive/cancel states.
- Partial receipt.
- Quality/rejected/deducted quantity.
- Lot trace.
- Supplier restrictions.
- Currency.
- Document numbering.
- Fiscal-period guard.
- Three-way matching.
- Budget encumbrance.
- Partner ledger.
- Multiple/partial payments.
- Excel export.

This is a strong foundation.

## Advanced additions

### A. Purchase Requisition

Before PO:

```text
Department/User Request
→ Budget Check
→ Approval
→ Sourcing/RFQ
→ PO
```

Fields:

- Department.
- Cost center.
- Project.
- Required date.
- Item/service.
- Quantity.
- Estimated price.
- Suggested supplier.
- Business justification.
- Priority.

### B. RFQ / Supplier Quotation

Flow:

```text
RFQ
→ invited suppliers
→ quotation entries
→ technical evaluation
→ commercial evaluation
→ award
→ PO
```

### C. Quote Comparison

Weighted criteria:

- Price.
- Delivery.
- Payment terms.
- Quality.
- Supplier score.
- Warranty.
- Technical compliance.

### D. Purchase Agreements

Support:

- Blanket orders.
- Framework agreements.
- Contract price lists.
- Quantity commitments.
- Release orders.
- Effective dates.

### E. Procurement Exception Workbench

Show only problems:

- PO overdue.
- Partial receipt overdue.
- Supplier rejected quantity high.
- Invoice mismatch.
- Price variance.
- Budget risk.
- Supplier inactive.
- Missing contract.
- Payment due.
- Duplicate invoice risk.

### F. Procurement Next Best Action

For each document:

```text
Draft PO       → Submit for approval
Approved PO    → Issue to supplier
Issued PO      → Await/record delivery
Partial GRN    → Receive remaining qty
Invoice        → Resolve match
Matched        → Submit payment approval
```

### G. Lead-Time Analytics

Measure:

- Requisition-to-PO.
- PO-to-acknowledgment.
- PO-to-GRN.
- GRN-to-invoice.
- Invoice-to-payment.

Rank delays by supplier/department.

---

# 10. EXISTING FEATURE — Three-Way Matching: Advanced Target

## Current baseline

Bemo already compares PO / GRN / supplier invoice and has configurable variance resolution.

## Advanced additions

### Match dimensions

- Quantity.
- Unit price.
- Tax.
- Discount.
- Currency.
- UoM.
- Supplier.
- Item.
- PO line.
- GRN line.
- Accepted quantity.
- Invoice total.

### Tolerances

By:

- Tenant.
- Supplier.
- Item category.
- PO type.
- Currency.
- Amount band.

Example:

```text
Quantity tolerance: 1%
Price tolerance: 2%
Absolute amount tolerance: EGP 50
```

### Auto-clear policy

If mismatch is below configured tolerance:

```text
AUTO_MATCHED_WITHIN_TOLERANCE
```

No manual work.

### Mismatch reasons

```text
PRICE_VARIANCE
QTY_OVER_INVOICE
QTY_UNDER_INVOICE
UNRECEIVED_QTY
TAX_MISMATCH
DISCOUNT_MISMATCH
WRONG_ITEM
WRONG_PO
CURRENCY_MISMATCH
DUPLICATE_INVOICE
```

### Resolution

- Accept variance.
- Request credit note.
- Adjust GRN if legitimate.
- Reject invoice.
- Escalate.
- Split invoice.

Every resolution requires reason and audit.

---

# 11. EXISTING FEATURE — Business Parties / Suppliers: Advanced Target

## Current baseline

The repository already has business parties with:

- Direct/managed relationships.
- Responsible party.
- Contact data.
- Tax ID/phone validation.
- Relationship dates.
- Active supplier restrictions.

## Advanced additions

### A. Supplier onboarding lifecycle

```text
REQUESTED
→ UNDER_REVIEW
→ APPROVED
→ ACTIVE
→ SUSPENDED
→ BLACKLISTED
→ CLOSED
```

### B. Duplicate Detection

During creation compare:

- Tax ID exact.
- Commercial registration.
- Bank account.
- Phone.
- Email.
- Normalized Arabic/English name.
- Address.
- Responsible party.

Return:

```text
Possible duplicate: 89% match
```

This can be deterministic/fuzzy string matching without paid AI.

### C. Supplier Classification

Fields:

- Commodity categories.
- Strategic/regular/one-time.
- Local/import.
- Manufacturer/distributor/service.
- Risk tier.
- Payment terms.
- Preferred currency.
- Tax treatment.
- Approved branches.

### D. Supplier Compliance

Documents:

- Tax card.
- Commercial registration.
- VAT registration.
- Bank letter.
- Certifications.
- Insurance.
- Contracts.

Each document:

- Issue date.
- Expiry.
- Mandatory flag.
- Verification status.

### E. Bank Change Control

Supplier bank account change must:

1. Capture old/new.
2. Require reason.
3. Require independent verification.
4. Require finance approval.
5. Prevent payment to unverified account.
6. Log evidence.

### F. Supplier 360

Show:

- Spend.
- POs.
- Receipts.
- Rejections.
- Quality.
- Invoices.
- Payment history.
- Outstanding AP.
- Average lead time.
- On-time delivery.
- Price trends.
- Contracts.
- Risk.
- Documents.

---

# 12. EXISTING FEATURE — Inventory / Operations: Advanced Target

## Current baseline

Bemo already contains:

- Item master.
- Item categories.
- UoM.
- Stock movements.
- Warehouse reference.
- Adjustments.
- Business document references.
- Inventory movement evidence.
- Procurement receiving into stock.

## Most important missing maturity step: valuation

### A. Inventory Valuation

Support at least:

- Weighted average.
- FIFO.

Optional later:

- Standard cost.

Store cost-layer data.

Example FIFO layers:

```text
GRN-001 100 KG @ 40
GRN-002 200 KG @ 42
GRN-003 150 KG @ 44
```

Issue 250 KG:

```text
100 @ 40
150 @ 42
COGS/Consumption = 10,300
```

### B. Stock dimensions

Inventory balance should not only be:

```text
item + warehouse
```

Advanced:

```text
item
+ warehouse
+ location/bin
+ lot
+ serial
+ quality status
+ ownership
```

### C. Reservation

Support:

```text
On hand
Reserved
Available
Expected incoming
Allocated
Damaged/blocked
```

### D. Negative-stock policy

Per tenant/item:

- Hard block.
- Warning.
- Allow authorized override.

### E. Reorder Policy

Per item/location:

- Reorder point.
- Safety stock.
- Min.
- Max.
- Preferred supplier.
- Lead time.
- Order multiple.
- Minimum order quantity.

### F. Reorder Recommendation

No AI needed.

Example:

```text
Current available       1,200
Open demand             1,900
Confirmed incoming        500
Safety stock              600
Target max              3,000

Recommended order:
3,800 units
```

Show formula.

### G. Stockout Prediction

Use:

```text
Available qty / rolling average daily consumption
```

Then improve with seasonality later.

### H. Inventory Adjustment Governance

Adjustment requires:

- Reason code.
- Evidence.
- Count reference.
- Cost impact.
- Approval if above threshold.

### I. Inventory GL

Receipt:

```text
DR Inventory
CR GRNI / Accrued receipt
```

Supplier invoice:

```text
DR GRNI
CR AP
```

Consumption:

```text
DR Production / COGS / Expense
CR Inventory
```

Adjustment:

```text
DR/CR Inventory Adjustment
```

---

# 13. NEW/ADVANCED — Warehouse Management System (WMS)

Create explicit hierarchy:

```text
Warehouse
→ Zone
→ Aisle
→ Rack
→ Shelf
→ Bin
```

## Features

- Receiving staging.
- Putaway.
- Bin transfer.
- Picking.
- Packing.
- Dispatch.
- Replenishment.
- Cycle count.
- Barcode/QR scanning.
- Handheld/mobile workflow.
- Stock status.
- Quarantine.
- Damaged stock.
- Expiry.

## Directed Putaway

Rules:

- Item category.
- Temperature.
- Hazard.
- Weight.
- Fast-moving class.
- Capacity.

## Picking Strategies

- FIFO.
- FEFO for expiry-sensitive items.
- Batch picking.
- Zone picking.

## KPIs

- Dock-to-stock time.
- Pick accuracy.
- Inventory accuracy.
- Count variance.
- Space utilization.
- Picks/hour.

---

# 14. NEW/ADVANCED — Lot, Serial and Expiry Traceability

For relevant items track:

- Lot.
- Batch.
- Serial.
- Manufacture date.
- Expiry.
- Supplier lot.
- Internal lot.
- GRN.
- Production order.
- Customer shipment.

Required queries:

> Which supplier/GRN introduced this lot?

> Which finished goods used lot X?

> Which customers received finished goods containing lot X?

Support recall workflow:

```text
LOT_IDENTIFIED
→ IMPACT_ANALYSIS
→ STOCK_BLOCK
→ CUSTOMER_TRACE
→ RECALL
→ CLOSE
```

---

# 15. EXISTING FEATURE — Finance / Chart of Accounts: Advanced Target

## Current baseline

Bemo contains finance account, journal, bank, tax/currency, fiscal-period and budget screens plus guarded journal states.

## Advanced additions

### A. Chart hierarchy

Support:

```text
Assets
  Current Assets
    Cash
    AR
    Inventory
Liabilities
Equity
Revenue
Expenses
```

Fields:

- Account code.
- Name AR/EN.
- Type.
- Parent.
- Posting allowed.
- Currency.
- Reconciliation required.
- Cost-center required.
- Project required.
- Active dates.

### B. Account Control Rules

Example:

- Bank account requires bank dimension.
- Expense account requires department.
- Project expense requires project.
- AP control account prohibits manual journal except authorized role.

### C. Trial Balance

Columns:

- Opening debit/credit.
- Period debit/credit.
- Closing debit/credit.

### D. Financial Statements

- Balance sheet.
- Income statement.
- Cash-flow statement.
- Changes in equity.
- Comparative periods.
- Department/company segments.

### E. Drill-down

Financial statement number must drill:

```text
Statement
→ Account
→ Journal
→ Journal line
→ Source document
```

---

# 16. EXISTING FEATURE — Journal Entries: Advanced Target

## Current baseline

The repository has:

- Draft/posted/reversed state.
- Fiscal-period blocking.
- Idempotent posting.
- Document numbering.
- Role security.
- Audit.

## Advanced additions

### A. Journal Templates

Examples:

- Accrual.
- Prepayment.
- Payroll.
- Depreciation.
- Bank fee.
- Closing.

### B. Recurring Journals

Configure:

- Frequency.
- Start/end.
- Amount/formula.
- Accounts.
- Auto-create draft.
- Approval workflow.

### C. Reversing Journals

Post accrual on Aug 31:

```text
DR Expense
CR Accrual
```

Automatically generate reversal on Sep 1.

### D. Journal Validation

Block:

- Debit != credit.
- Closed period.
- Inactive account.
- Missing mandatory dimension.
- Invalid control-account use.

### E. Suspicious Journal Rules

Flag:

- High-value manual journal.
- Weekend/night posting.
- Rare account combination.
- Manual AP/AR control account.
- Journal immediately reversed.
- Repeated round-number entries.

No AI needed.

---

# 17. EXISTING FEATURE — Fiscal Periods & Month Close: Advanced Target

## Current baseline

Bemo already guards posting based on fiscal periods.

## Advanced target: Close Command Center

Checklist:

```text
Bank reconciliation complete
Payroll posted
Depreciation posted
Inventory valuation posted
GRNI reconciled
AP period reviewed
AR period reviewed
FX revaluation posted
Accruals posted
Intercompany reconciled
Pending journals resolved
```

Statuses:

```text
NOT_STARTED
IN_PROGRESS
BLOCKED
READY_TO_CLOSE
CLOSED
REOPENED
```

## Close readiness score

Example:

```text
Close readiness: 87%

✓ Payroll
✓ Depreciation
✓ Bank A
⚠ Bank B: 14 unmatched
⚠ 5 GRNI balances unresolved
⚠ 3 journals awaiting approval
```

## Controls

- Soft close.
- Hard close.
- Reopen requires approval.
- Reopen reason mandatory.
- Limit backdated entries.

---

# 18. EXISTING FEATURE — Budgets / Encumbrance: Advanced Target

## Current baseline

Budgeting and PO encumbrance are already present.

## Advanced additions

### A. Budget Types

- Annual.
- Monthly.
- Project.
- Department.
- Cost center.
- Capex.
- Opex.

### B. Budget Versions

```text
Original
Revision 1
Revision 2
Forecast
Latest approved
```

Never overwrite the original budget.

### C. Transfers

Transfer:

```text
Department A → Department B
```

requires approval and history.

### D. Budget Consumption

Expose:

```text
Budget
Actual
Committed
Encumbered
Available
Forecast
```

### E. Threshold alerts

Examples:

```text
80% → warning
90% → manager notification
100% → hard block or escalation
```

### F. Forecast

Basic:

```text
Annual forecast =
actual YTD / elapsed-period ratio
```

Better:

- Seasonality.
- Contracted commitments.
- Payroll commitments.
- Open POs.

---

# 19. EXISTING FEATURE — Banking: Advanced Target

## Current baseline

Bank account management exists.

## Advanced additions

### A. Bank Statement Import

Formats:

- CSV.
- XLSX.
- MT940 later.
- CAMT.053 later.
- Bank-specific templates.

Fields:

- Booking date.
- Value date.
- Description.
- Debit.
- Credit.
- Reference.
- Balance.

### B. Matching Engine

Rule priority:

1. Exact reference + exact amount.
2. Exact amount + date tolerance.
3. Supplier/customer reference.
4. Transfer reference.
5. Fuzzy description.

### C. Reconciliation statuses

```text
UNMATCHED
SUGGESTED
MATCHED
PARTIALLY_MATCHED
MANUAL
IGNORED
```

### D. Bank Charges

If statement differs by fee:

```text
Payment 15,000
Statement 15,020
Difference 20
```

Allow:

```text
DR Bank Fees 20
```

### E. Daily Cash Position

Per bank/currency:

```text
Book balance
Bank balance
Uncleared payments
Uncleared deposits
Available cash
```

---

# 20. NEW/ADVANCED — Cash-Flow Forecast

Forecast daily/weekly:

```text
Opening cash
+ expected customer receipts
+ financing
- supplier payments
- payroll
- taxes
- loan installments
- planned purchases
- capex
= projected closing cash
```

Scenarios:

- Conservative.
- Base.
- Optimistic.

Show:

- Minimum cash date.
- Shortage.
- Surplus.
- Top inflows/outflows.
- Currency exposure.

Recommended actions can remain rule-based:

> Collect customer X.

> Delay non-critical PO Y.

> Move payment Z within allowed payment terms.

---

# 21. EXISTING FEATURE — Currency & Tax: Advanced Target

## Current baseline

Bemo has currency/tax configuration and recent work around external exchange-rate integration.

## Advanced additions

### A. Exchange Rate Types

- Spot.
- Accounting.
- Month-end.
- Budget.
- Customs.

### B. Rate Source

Store:

- Manual.
- Frankfurter/external API.
- Central bank.
- Imported file.

### C. Rate Approval

External rate can be:

```text
FETCHED
→ REVIEWED
→ APPROVED
→ ACTIVE
```

Configurable auto-approval for trusted feed.

### D. Revaluation

At month-end:

- Foreign AP.
- Foreign AR.
- Bank balances.
- Intercompany.

Calculate unrealized gain/loss.

### E. Realized FX

When invoice and payment use different rates, automatically post gain/loss.

### F. Tax Engine

Tax determination based on:

- Supplier/customer.
- Item/service.
- Location.
- Tax category.
- Effective date.

Support tax reports and audit drilldown.

---

# 22. NEW/ADVANCED — Accounts Receivable / Customer Finance

This is a major ERP maturity area.

## Customer master

Fields:

- Credit limit.
- Payment terms.
- Currency.
- Tax profile.
- Risk level.
- Collector.
- Account manager.

## Customer invoices

States:

```text
DRAFT
APPROVED
POSTED
PARTIALLY_PAID
PAID
OVERDUE
DISPUTED
WRITTEN_OFF
```

## Receipts

Support:

- Exact payment.
- Partial.
- Advance.
- Unallocated.
- Multi-invoice allocation.

## Aging

Buckets configurable:

```text
Current
1–30
31–60
61–90
91–120
120+
```

## Credit Control

Before confirming sales order:

```text
Credit limit        500,000
Current exposure    470,000
New order           120,000
Projected           590,000

Over limit: 90,000
```

Action:

- Block.
- Approval.
- Finance override.

## Collections Workbench

Prioritize:

- Amount overdue.
- Days overdue.
- Customer risk.
- Dispute.
- Payment behavior.

---

# 23. EXISTING FEATURE — Sales: Advanced Target

## Current baseline

A sales module/route exists.

## Advanced sales lifecycle

```text
Lead/Customer
→ Quotation
→ Sales Order
→ Credit Check
→ Reservation
→ Picking
→ Delivery
→ Invoice
→ Receipt
→ Return/Credit Note
```

## Quotation

- Expiry.
- Version.
- Price list.
- Discount.
- Approval if discount > threshold.

## Sales Order

- Customer.
- Ship-to.
- Required date.
- Warehouse.
- Currency.
- Price list.
- Credit status.

## Availability to Promise

Show:

```text
On hand
Reserved
Available
Incoming
Can fulfill date?
```

## Sales Return

Must reference original shipment/invoice.

## Margin Control

Before approval:

```text
Sales price
- standard/actual cost
= margin
```

Warn/block below margin threshold.

---

# 24. EXISTING FEATURE — Manufacturing: Advanced Target

## Current baseline

Production and quality routes exist. The project roadmap correctly identifies full BOM/work-order/costing maturity as an open area.

## A. BOM

Fields:

- Product.
- Version.
- Effective dates.
- Component.
- Quantity.
- Scrap factor.
- UoM.
- Substitute item.

## B. Routing

Operations:

```text
Sorting
Washing
Processing
Packing
Quality
```

Each operation:

- Work center.
- Setup time.
- Run time.
- Labor.
- Machine requirement.

## C. Production Order

Lifecycle:

```text
PLANNED
→ RELEASED
→ IN_PROGRESS
→ COMPLETED
→ CLOSED
```

Support:

- Material reservation.
- Material issue.
- Material return.
- Scrap.
- By-product.
- Finished goods receipt.

## D. Actual Cost

Capture:

```text
Material
Labor
Machine
Overhead
Scrap
Subcontracting
```

Compare:

```text
Standard cost vs Actual cost
```

## E. Production Variance

Break down:

- Material price variance.
- Material usage variance.
- Labor rate.
- Labor efficiency.
- Scrap.
- Overhead.

---

# 25. NEW/ADVANCED — MRP

Inputs:

- Sales demand.
- Forecast.
- Production plan.
- Current inventory.
- Open PO.
- Open production.
- BOM.
- Safety stock.
- Lead time.

Output:

```text
Item
Gross requirement
On hand
Scheduled receipts
Net requirement
Recommended order date
Recommended quantity
Suggested supply type
```

Supply type:

- Purchase requisition.
- Production order.
- Transfer order.

## MRP should generate proposals, not silently create final documents.

---

# 26. NEW/ADVANCED — Production Scheduling / Capacity

Create:

- Work centers.
- Machines.
- Shifts.
- Capacity/hour.
- Maintenance downtime.
- Changeover/setup.
- Operator requirement.

Answer:

> Can order X finish by Aug 20?

Calculate:

- Material availability.
- Capacity.
- Queue.
- Setup.
- Shift hours.

Show bottleneck.

---

# 27. EXISTING FEATURE — Quality: Advanced Target

## Current baseline

A quality module exists and procurement receipts already expose rejected/deducted quantities.

## Advanced quality

### Inspection Plans

By:

- Item.
- Supplier.
- Production stage.

Tests:

- Visual.
- Weight.
- Size.
- Temperature.
- Defect percentage.
- Lab result.

### Quality statuses

```text
PENDING
PASSED
FAILED
CONDITIONAL
QUARANTINED
RELEASED
SCRAPPED
```

### NCR — Non-Conformance

Record:

- Source.
- Item/lot.
- Defect.
- Severity.
- Quantity.
- Root cause.
- Corrective action.
- Owner.
- Due date.

### Supplier quality score

Use:

- Reject rate.
- NCR count.
- Repeat defect.
- Claim value.

---

# 28. NEW/ADVANCED — Maintenance / Equipment

Create:

- Equipment.
- Asset link.
- Machine.
- Location.
- Meter.
- Runtime.
- Maintenance plan.
- Spare parts.
- Failure history.

## Maintenance types

- Preventive.
- Corrective.
- Predictive later.
- Calibration.

## Work order

```text
REQUESTED
→ PLANNED
→ ASSIGNED
→ IN_PROGRESS
→ COMPLETED
→ VERIFIED
```

## Preventive scheduling

By:

- Calendar.
- Runtime hours.
- Production units.

## KPIs

- MTBF.
- MTTR.
- Downtime.
- Maintenance cost.
- Planned vs unplanned.

---

# 29. EXISTING FEATURE — Approval Workflows: Advanced Target

## Current baseline

Bemo already has a reusable multi-step approval engine with role/user criteria, minimum approvals, self-approval controls and decision audit.

## Important enhancements

### A. Delegation

User can define:

```text
Delegate from Aug 10–Aug 20 to User B
```

Rules:

- Scope.
- Effective dates.
- Reason.
- Approval if required.

### B. Reassignment

Authorized manager can reassign a stuck approval with audit.

### C. SLA

Every step:

- Target minutes/hours/days.
- Reminder threshold.
- Escalation threshold.

### D. Multi-signature

Policies:

```text
ANY_ONE
ALL
ANY_N_OF_M
SEQUENTIAL
PARALLEL
```

### E. Approval Snapshot

At workflow start, save:

- Definition version.
- Step configuration.
- Approver criteria.
- Document summary.

Changing the workflow later must not alter historical approval evidence.

### F. Risk-based routing

Deterministic rule:

```text
PO <= 10k         manager
10k–100k          manager + finance
>100k             director + finance

Add CFO if:
supplier risk HIGH
OR budget exceeded
OR non-contract purchase
```

### G. Approval Summary

Before user decides, show:

- What is being approved.
- Financial impact.
- Budget status.
- Risk flags.
- Exceptions.
- Prior decisions.
- Attachments.

---

# 30. EXISTING FEATURE — Notifications / Action Center: Advanced Target

## Current baseline

Business notifications and unread Action Center already exist.

## Upgrade into Smart Action Center

Categories:

- Critical.
- Financial.
- Operational.
- Approval.
- Compliance.
- Information.

Each actionable item should contain:

```text
Problem
Why it happened
Business impact
Recommended action
Due time
Responsible role/user
Open record
Perform safe action
```

Example:

```text
CRITICAL — Stockout risk

ITEM: BOX-12
Available: 1,200
Average usage: 340/day
Supplier lead time: 5 days
Predicted stockout: 3.5 days

Recommended:
Create requisition for 2,400 units.
```

### Suppression

Avoid notification spam:

- Deduplicate.
- Group.
- Snooze.
- Acknowledge.
- Escalate only if unresolved.
- Auto-close when condition disappears.

---

# 31. EXISTING FEATURE — Dashboard & Reporting: Advanced Target

## Current baseline

Bemo already has:

- Dashboards.
- Configurable widgets.
- Attendance/payroll/department metrics.
- Multi-period trends.
- Excel exports.
- User display preferences.

## Advanced role dashboards

### CFO

- Cash.
- AP.
- AR.
- Budget.
- Revenue.
- Expenses.
- Close readiness.
- FX exposure.

### HR

- Attendance.
- Payroll.
- Overtime.
- Headcount.
- Workforce exceptions.

### Procurement

- Open requisitions.
- Late POs.
- Supplier score.
- Spend.
- Savings.
- Match exceptions.

### Warehouse

- Stockouts.
- Slow-moving.
- Expiry.
- Pending receipts.
- Picks/counts.

### Production

- Plan vs actual.
- Delays.
- Material shortage.
- Scrap.
- OEE later.

## Four questions every dashboard should answer

1. What happened?
2. Why?
3. What is likely to happen?
4. What should I do?

---

# 32. NEW/ADVANCED — Custom Report Builder

User selects:

- Entity.
- Columns.
- Filters.
- Grouping.
- Sorting.
- Totals.
- Chart.
- Date range.

Save report.

Permissions:

- Private.
- Team.
- Role.
- Tenant shared.

Outputs:

- On-screen.
- Excel.
- PDF later.
- Scheduled subscription.

Never allow arbitrary SQL from browser.

---

# 33. NEW/ADVANCED — Scheduled Reports

Examples:

```text
Every Monday 08:00
→ CFO weekly finance pack

Daily 07:30
→ HR attendance exception summary

Month end
→ inventory valuation report
```

Delivery:

- Action Center.
- Email.
- Download center.
- External webhook later.

---

# 34. EXISTING FEATURE — Audit Logs: Advanced Target

## Current baseline

Append-only auditing already covers multiple critical operations.

## Advanced audit

Capture standard fields:

- App.
- Company.
- User.
- Role.
- Session.
- IP.
- Entity.
- Entity ID.
- Action.
- Before.
- After.
- Reason.
- Correlation ID.
- Operation ID.
- Timestamp.

## Business-friendly audit screen

Instead of technical JSON only:

```text
محمد changed Supplier ABC bank account

Old: xxxx1234
New: xxxx9876
Reason: supplier bank migration
Approved by: Finance Manager
```

## Audit export

- Date range.
- Entity.
- User.
- Action.
- Hash/signature manifest if needed.

---

# 35. EXISTING FEATURE — Security & Sessions: Advanced Target

## Current baseline

Bemo already has strong measures including:

- Multi-role authorization.
- Menu access.
- Server-side security.
- In-memory access token pattern.
- Refresh rotation.
- Lockout/rate limiting.
- Scoped logout.
- Revoke all devices.
- Audit.

## Advanced additions

### A. MFA / 2FA

Options:

- TOTP authenticator.
- Email OTP as fallback.
- Recovery codes.

Enforce by:

- Tenant.
- Role.
- High-risk action.

### B. Session Center

User screen:

```text
Current browser
Windows Chrome — Cairo
Android — Cairo
Unknown — Alexandria
```

Allow revoke individual session.

### C. IP / Network Policy

Per tenant:

- Allowlist.
- Denylist.
- Office-only admin access.
- VPN range.

### D. Step-up Authentication

For:

- Change supplier bank.
- Execute large payment.
- Reopen closed fiscal period.
- Change security configuration.

Require password/MFA again.

### E. Segregation of Duties

Rules:

- Creator cannot approve own payment.
- Supplier bank editor cannot execute payment.
- Journal creator cannot post over threshold.

---

# 36. EXISTING FEATURE — Users / Roles / Access: Advanced Target

## Advanced additions

### A. Permission model

Move gradually from menu-only concepts to explicit permissions:

```text
supplier.read
supplier.create
supplier.update
supplier.approve
payment.create
payment.approve
payment.execute
journal.post
journal.reverse
```

Menus remain presentation.

Backend permissions remain authoritative.

### B. Data scope

Role may see:

- All companies.
- Selected company.
- Branch.
- Department.
- Own records.

### C. Temporary access

Grant permission until date/time.

Example:

> Acting Finance Manager until Aug 20.

### D. Access review

Quarterly:

- Who has finance posting?
- Who has supplier bank edit?
- Who has Admin?
- Unused accounts.

---

# 37. EXISTING FEATURE — Organization / Multi-Tenant: Advanced Target

## Current baseline

Tenant/app/company/branch/warehouse/department structures exist.

## Advanced company hierarchy

```text
Tenant
→ Legal Company
→ Business Unit
→ Branch/Site
→ Department
→ Cost Center
→ Warehouse
```

Define legal-company ownership of financial transactions.

## Master-data scope

Decide for each master:

- Tenant shared.
- Company-specific.
- Branch-specific.

Examples:

```text
Currency      shared
Item          optionally shared
Supplier      shared with company approval
Bank account  company-owned
Fiscal period company-owned
```

---

# 38. NEW/ADVANCED — Multi-Company Consolidation

Create:

- Consolidation group.
- Company membership.
- Chart mapping.
- Reporting currency.
- FX translation method.
- Intercompany relationships.

## Consolidation

```text
Company A trial balance
+ Company B
+ Company C
± currency translation
- elimination entries
= consolidated trial balance
```

## Elimination

Examples:

- Intercompany sales/purchases.
- Receivable/payable.
- Loans.
- Dividends.

Store elimination journals separately from legal books.

---

# 39. NEW/ADVANCED — Intercompany Automation

If Company A purchases from Company B:

```text
A: Purchase Order
B: Sales Order
```

Then:

```text
B shipment
→ A expected receipt

B AR invoice
→ A AP invoice
```

Reconcile automatically and flag differences.

---

# 40. NEW/ADVANCED — Fixed Assets

This is a major classical ERP module still open in the roadmap.

## Asset register

Fields:

- Asset number.
- Category.
- Description.
- Company.
- Location.
- Department.
- Custodian.
- Acquisition date.
- Capitalization date.
- Cost.
- Residual value.
- Useful life.
- Depreciation method.
- GL accounts.

## Depreciation methods

At minimum:

- Straight-line.
- Declining balance.
- Units of production.

## Lifecycle

```text
PURCHASED
→ CAPITALIZED
→ IN_SERVICE
→ TRANSFERRED
→ IMPAIRED/REVALUED
→ DISPOSED
```

## Procurement linkage

```text
PO
→ GRN
→ Invoice
→ Asset capitalization candidate
```

## Depreciation run

Monthly:

- Calculate.
- Preview.
- Exception report.
- Approve.
- Post journal.

## Transfers

- Location.
- Department.
- Custodian.
- Cost center.

## Disposal

- Sale.
- Scrap.
- Write-off.
- Gain/loss calculation.

---

# 41. NEW/ADVANCED — Project Accounting

## Project structure

```text
Project
→ WBS
→ Task
```

Fields:

- Customer.
- Manager.
- Start/end.
- Budget.
- Currency.
- Cost center.
- Status.

## Capture costs from

- Payroll.
- Contractor settlement.
- Inventory.
- PO/invoice.
- Expenses.
- Assets.

## Measures

```text
Budget
Actual
Committed
Remaining
Forecast at completion
Variance
```

## Revenue recognition

Optional methods:

- Completed contract.
- Percentage of completion.

## Project P&L

Revenue vs all project-related cost.

---

# 42. NEW/ADVANCED — Contract Management

Contracts:

- Supplier.
- Customer.
- Employee.
- Rental.
- Maintenance.

## Fields

- Contract number.
- Parties.
- Type.
- Currency.
- Value.
- Effective date.
- Expiry.
- Renewal.
- Payment terms.
- Owner.
- Status.

## Obligations

Example:

```text
Supplier must deliver in <= 5 days
Penalty 1% per late week
```

Track breaches.

## Amendments

Never overwrite original.

```text
Contract v1
Amendment 1
Amendment 2
```

## Alerts

- Expiring.
- Renewal window.
- Value utilization.
- Missing obligation.

---

# 43. NEW/ADVANCED — Document Management

Central attachment model:

```text
Document
- file
- content type
- size
- hash
- version
- category
- linked entity
- uploaded by
- security scope
- retention policy
```

Link to:

- Supplier.
- PO.
- GRN.
- Invoice.
- Payment.
- Employee.
- Worker.
- Contract.
- Asset.
- Journal.
- Quality case.

## Features

- Versioning.
- Preview.
- Download.
- Access control.
- Required document lists.
- Expiry.
- Virus scanning.
- Hash evidence.
- OCR later.

---

# 44. NEW/ADVANCED — Egyptian ETA E-Invoicing

For Egyptian customers this can become a key commercial capability.

## Configuration

- ETA environment.
- Taxpayer ID.
- branch code.
- signing configuration.
- activity code.
- item code mapping.
- tax code mapping.

## Workflow

```text
Bemo Sales Invoice
→ Validation
→ ETA JSON/XML payload
→ Digital signature
→ Submission
→ UUID
→ Status polling/callback
```

Statuses:

```text
PENDING_SUBMISSION
SUBMITTED
VALID
INVALID
REJECTED
CANCELLED
```

## Error Center

Store:

- ETA error code.
- Original message.
- Localized explanation.
- Field.
- Suggested correction.

Most explanations can be rule-based from a maintained error catalog.

---

# 45. NEW/ADVANCED — Employee Self-Service

Employee portal:

- Profile.
- Attendance.
- Attendance correction request.
- Leave request.
- Overtime request.
- Payslip.
- Advance/loan request.
- Documents.
- Notifications.
- Approval status.

Mobile-first.

Do not expose HR administrative data outside employee scope.

---

# 46. NEW/ADVANCED — Manager Self-Service

Manager sees own organizational scope:

- Team attendance.
- Exceptions.
- Leave.
- Overtime.
- Headcount.
- Payroll anomaly summary.
- Requests.
- Approvals.
- Expiring employee documents.

---

# 47. NEW/ADVANCED — Leave Management

## Leave types

- Annual.
- Sick.
- Unpaid.
- Emergency.
- Custom.

Fields:

- Accrual policy.
- Carry forward.
- Maximum.
- Documentation requirement.
- Paid/unpaid.
- Approval workflow.

## Balance

Explain:

```text
Opening
+ accrued
+ adjustment
- taken
- reserved
= available
```

Attendance must automatically respect approved leave.

---

# 48. NEW/ADVANCED — Overtime Management

Flow:

```text
Planned/Requested
→ Approved
→ Attendance evidence
→ Calculated
→ Payroll
```

Rules:

- Weekday.
- Weekend.
- Holiday.
- Night premium.
- Maximum hours.
- Approval required.

Prevent unapproved overtime silently entering payroll unless tenant policy allows.

---

# 49. NEW/ADVANCED — Customer/Supplier Portals

## Supplier portal

- View PO.
- Acknowledge.
- Confirm delivery.
- Upload invoice.
- View invoice status.
- View payment status.
- Upload compliance docs.

## Customer portal

- View quotation/order.
- Invoice.
- Statement.
- Payment status.
- Dispute.
- Documents.

Use separate external-user authorization model.

---

# 50. EXISTING FEATURE — i18n / Translation: Advanced Target

## Current baseline

The project has DB-backed Arabic/English translations, app-specific overrides and Super Admin translation management.

This is already advanced.

## Additional maturity

### A. Translation lifecycle

Statuses:

```text
DRAFT
REVIEWED
PUBLISHED
```

### B. Missing-key dashboard

Show:

- Missing per locale.
- Missing per app override.
- Fallback being used.

### C. Import/export translation pack

CSV/XLSX:

- Key.
- Default Arabic.
- Default English.
- App override Arabic.
- App override English.

### D. Version/audit

Who changed translation and when.

### E. Locale expansion readiness

Do not assume only AR/EN in schema.

---

# 51. EXISTING FEATURE — Imports: Advanced Target

## Current baseline

Bemo already has robust Excel import handling, preview limits, validation, error localization and workforce import safety.

## Advanced generic import center

Create reusable import jobs:

```text
UPLOADED
→ PARSED
→ VALIDATED
→ PREVIEWED
→ COMMITTED
→ PARTIAL_FAILURE / COMPLETED
→ REVERSED
```

## Mapping templates

Per tenant/user:

- Source column.
- Destination field.
- Transformation.
- Default.
- Required.

## Error workbook

Download rejected rows with:

- Row number.
- Field.
- Error code.
- Error explanation.
- Suggested correction.

## Import audit

Keep:

- Original file hash.
- User.
- Row counts.
- Created/updated/skipped.
- Reversal reference.

---

# 52. EXISTING FEATURE — Excel Exports: Advanced Target

Add export service capabilities:

- Background export for large data.
- Export center.
- Status/progress.
- Signed/expiring download URL if needed.
- Export manifest.
- Applied filters embedded in workbook.
- Generated by.
- Generated time.
- Source report version.

For financial reports include:

- Company.
- Period.
- Currency.
- Filters.
- “As of” time.

---

# 53. NEW/ADVANCED — Data Warehouse / BI Layer

Do not make Power BI query production transactional tables directly for heavy reporting.

Build analytical model.

Facts:

```text
fact_attendance
fact_payroll
fact_procurement
fact_inventory
fact_sales
fact_finance
fact_workforce
```

Dimensions:

```text
dim_date
dim_company
dim_branch
dim_department
dim_employee
dim_worker
dim_supplier
dim_customer
dim_item
dim_currency
```

Benefits:

- Faster executive analytics.
- Historical snapshots.
- External BI.
- Forecasting.
- Stable semantic definitions.

---

# 54. NEW/ADVANCED — Universal Search

Global search should cover:

- Employee.
- Worker.
- Contractor.
- Supplier.
- Customer.
- Item.
- PO.
- GRN.
- Invoice.
- Payment.
- Journal.
- Asset.
- Contract.
- Project.

Support:

- Exact IDs.
- Partial codes.
- Arabic names.
- English names.
- Phone.
- Tax ID.
- Document number.

Later semantic/fuzzy search can be optional.

---

# 55. NEW/ADVANCED — Business Timeline / Document Graph

Every document should expose related records.

Example PO:

```text
Purchase Requisition
→ Approval
→ RFQ
→ PO
→ GRN
→ Supplier Invoice
→ Match
→ Payment
→ Journal
```

Users should not manually search different modules.

Add:

`Related Documents` tab.

Store/document relationships explicitly where appropriate.

---

# 56. NEW/ADVANCED — Next Best Action

Implement as deterministic state rules.

Examples:

```text
Supplier REQUESTED
→ Complete compliance review

PO DRAFT
→ Submit approval

PO APPROVED
→ Issue

PO ISSUED
→ Receive / Follow up

Invoice MATCH_FAILED
→ Resolve mismatch

Payroll CALCULATED
→ Review variances

Fiscal Period OPEN
→ Complete close checklist
```

Expose on:

- Detail page.
- Action Center.
- Dashboard.

---

# 57. NEW/ADVANCED — Exception-First ERP

Design principle:

> Normal transactions should flow quietly; users should work on exceptions.

Examples:

### Attendance

```text
Normal: 1,921
Auto-resolvable: 67
Manual review: 12
```

### AP

```text
Invoices processed: 4,862
Within tolerance: 4,710
Review: 120
High risk: 32
```

### Inventory

```text
Items healthy: 1,800
Below reorder: 31
Stockout risk: 8
Count discrepancy: 5
```

This is one of the highest-value UX upgrades.

---

# 58. NEW/ADVANCED — Smart Scoring Framework

Reusable scoring engine.

Score definition:

```text
Metric
Weight
Direction
Thresholds
Missing-data behavior
Effective date
```

Use for:

- Supplier.
- Contractor.
- Customer credit.
- Attendance risk.
- Invoice risk.
- Inventory criticality.

Every score must be explainable.

Example:

```text
Supplier score 86

On-time         92 × 30%
Quality         88 × 25%
Price           75 × 20%
Invoice accuracy94 × 15%
Disputes        90 × 10%
```

---

# 59. NEW/ADVANCED — Fraud / Risk Rules

Do not call it “fraud confirmed.” Use:

> Risk / Requires Review

## Supplier invoice risk

- Duplicate invoice number.
- Same supplier + amount near same date.
- Price above PO.
- Bank changed recently.
- Manual override.
- Missing GRN.
- Large amount.

## Payroll risk

- Net increase above threshold.
- Duplicate bank account among employees.
- Manual attendance changes near payroll lock.
- Unexpected overtime.
- Payment to inactive employee.

## Journal risk

- Manual control-account entry.
- Unusual posting time.
- High amount.
- Immediate reversal.
- Round-number pattern.

---

# 60. NEW/ADVANCED — Forecasting Without Paid AI

## Simple first version

### Demand

```text
Forecast = moving average of previous N periods
```

### Stockout

```text
Days remaining = available / average daily consumption
```

### Cash

Use scheduled receivables/payables.

### Payroll

Use active employees + salary components + expected attendance.

### Budget

Use YTD run rate + open commitments.

## Improve gradually

- Weighted moving average.
- Exponential smoothing.
- Seasonal indexes.
- Confidence range.
- Backtesting.

Always show forecast error so users know reliability.

---

# 61. NEW/ADVANCED — What-If Simulation

Must never alter production data.

Create isolated scenarios.

Examples:

### Currency

> USD +10%.

Impact:

- AP.
- Inventory.
- Product cost.
- Cash.

### Payroll

> Wage +8%.

Impact by:

- Department.
- Contractor.
- Monthly payroll.

### Demand

> Sales +20%.

Impact:

- Stock.
- Purchasing.
- Production capacity.
- Cash.

### Supplier

> Lead time changes from 5 to 12 days.

Impact:

- Stockout risk.
- Production orders.

---

# 62. NEW/ADVANCED — Process Mining Readiness

Because business events are stored, later calculate:

```text
PR created → PO issued
PO issued → GRN
GRN → invoice
invoice → payment
```

Find:

- Average cycle.
- Slow path.
- Rework.
- Bottleneck.
- Excess approvals.

This does not require generative AI.

---

# 63. NEW/ADVANCED — No-Code Custom Fields

Essential for SaaS/productization.

Admin defines:

- Text.
- Number.
- Money.
- Date.
- Boolean.
- Dropdown.
- Multi-select.
- Reference.

Attach to supported entities.

Fields:

- Code.
- Label key.
- Entity.
- Required.
- Default.
- Validation.
- Role visibility.
- Effective date.

Avoid client-specific forks.

---

# 64. NEW/ADVANCED — Configurable Forms

Later allow tenant admin to configure:

- Field ordering.
- Sections.
- Visibility.
- Required flags.
- Help text.
- Role-specific layout.

Do not allow admin to bypass backend mandatory business rules.

---

# 65. NEW/ADVANCED — Automation Builder

Admin interface:

```text
WHEN
[Event]

IF
[Conditions]

THEN
[Actions]
```

Example:

```text
WHEN Supplier Invoice Created
IF amount > 50000
AND price variance > 5%
THEN
assign Finance Manager approval
AND create Critical task
```

Another:

```text
WHEN Stock Balance Changed
IF available < reorder point
THEN create purchasing recommendation
```

---

# 66. NEW/ADVANCED — Public API & Webhooks

## API

Provide:

- OpenAPI.
- API keys/service accounts.
- OAuth scopes.
- Rate limits.
- Tenant isolation.
- Idempotency.
- Audit.

## Webhooks

Events:

```text
purchase_order.issued
goods_receipt.posted
supplier_invoice.posted
payment.posted
payroll.posted
employee.created
stock.low
```

Security:

- HMAC signing.
- Retry.
- Dead-letter.
- Delivery logs.

---

# 67. NEW/ADVANCED — Compliance / Data Governance

## Data retention

Policies by record category.

## Personal data

Support data export subject to legal requirements.

## Sensitive field masking

Examples:

- National ID.
- Bank account.
- Salary.

Display based on permission.

## Encryption

- TLS in transit.
- Database/storage encryption strategy.
- Key rotation plan.

## Audit retention

Configurable but protected against normal-user deletion.

---

# 68. NEW/ADVANCED — Backup / Disaster Recovery / On-Premise Pack

For serious ERP adoption document:

- Backup schedule.
- Point-in-time recovery.
- Restore test.
- RPO.
- RTO.
- DB migration process.
- File/document backups.
- Secret management.
- TLS.
- Monitoring.

Add a disaster-recovery test checklist.

---

# 69. Platform Observability

Add:

- Structured logs.
- Trace/correlation ID.
- Metrics.
- Health checks.
- Slow API metrics.
- DB connection metrics.
- Scheduled job status.
- Device sync metrics.
- Export/import job status.

Admin dashboard:

```text
API health
DB
background jobs
device sync
email
exchange-rate feed
disk/storage
```

---

# 70. Optional AI-Ready Architecture — Build Later, Design Now

Do not connect an LLM directly to PostgreSQL.

Architecture:

```text
Optional Copilot
      |
      v
AI Tool Gateway
      |
      v
Existing Bemo Services
      |
 Authorization
 Tenant Scope
 Validation
 Approval
 Audit
      |
      v
 PostgreSQL
```

## Provider configuration

Per tenant:

```text
AI_ENABLED=false

PROVIDER:
DISABLED
BYOK_OPENAI
BYOK_ANTHROPIC
BYOK_GEMINI
LOCAL_OPENAI_COMPATIBLE
MANAGED
```

## Tool permissions

Example:

```text
attendance.read       allowed
payroll.explain       allowed
po.createDraft        allowed
po.issue              denied
journal.createDraft   allowed
journal.post          denied
payment.execute       denied
```

---

# 71. Optional Future AI Features

These are deliberately **not required for the current roadmap**.

## Copilot

Questions:

- “Why did payroll increase?”
- “Show overdue supplier invoices.”
- “Which items may stock out?”

## Document OCR

Extract supplier invoice fields.

## Natural-language reports

Convert question to safe analytical query/tool calls.

## Contract summarization

Summarize clauses/obligations.

## Approval summaries

Summarize evidence before decision.

## AI-generated automation

User describes rule, system drafts deterministic rule for review.

---

# 72. AI Governance If Enabled Later

Every AI action should be classified:

```text
L0 Explain
L1 Recommend
L2 Create Draft
L3 Execute after Approval
L4 Autonomous Low-Risk
```

Default Bemo policy should start at L0–L2.

Log:

- User.
- Provider/model.
- Request.
- ERP tools used.
- Records read.
- Recommendation.
- User confirmation.
- Records changed.

Never store confidential prompts with providers unnecessarily.

---

# 73. Proposed New Main Navigation Architecture

As Bemo grows, avoid adding a flat list of 60 menu entries.

Suggested workspaces:

## People

- Employees.
- Attendance.
- Leave.
- Payroll.
- Self Service.

## Workforce

- Contractors.
- Workers.
- Labor requests.
- Attendance.
- Settlements.
- Contractor accounts.

## Procurement

- Requisitions.
- RFQ.
- Purchase Orders.
- Receipts.
- Supplier Invoices.
- Payments.
- Suppliers.
- Contracts.

## Inventory

- Items.
- Warehouses.
- Stock.
- Movements.
- Transfers.
- Lots/serials.
- Counts.
- Reorder.

## Sales

- Customers.
- Quotations.
- Sales orders.
- Deliveries.
- Invoices.
- Receipts.
- Collections.

## Manufacturing

- BOM.
- Production orders.
- MRP.
- Scheduling.
- Quality.
- Maintenance.

## Finance

- Accounts.
- Journals.
- AP.
- AR.
- Banks.
- Reconciliation.
- Cash forecast.
- Budgets.
- Assets.
- Tax.
- Currency.
- Fiscal close.

## Projects

- Projects.
- WBS.
- Costs.
- Billing.
- Reports.

## Approvals & Tasks

- My tasks.
- Approvals.
- Workflow definitions.
- Delegations.

## Analytics

- Dashboards.
- Reports.
- Smart Action Center.
- Forecast.
- Scenarios.

## Admin

- Organization.
- Users.
- Roles.
- Audit.
- Settings.
- Translations.
- Integrations.
- Automation rules.

---

# 74. Proposed Priority Roadmap

The order matters more than the number of features.

## Phase 0 — Stabilize Current Baseline

Before large new modules:

- Keep branch CI green.
- Maintain integration test coverage.
- Resolve known QA issues.
- Keep translation checks.
- Keep error-code parity.
- Add migration rollback/upgrade-path tests.
- Maintain permission parity.

---

## Phase 1 — Make Existing Modules Enterprise-Grade

### P0

1. Inventory valuation + Inventory GL.
2. Bank statement import + reconciliation.
3. Supplier onboarding + bank verification.
4. Approval delegation + SLA + snapshot.
5. Payroll variance workbench + GL distribution.
6. Attendance exception workbench + policy engine.
7. Contractor 360 + performance scoring.
8. Procurement requisition/RFQ.
9. Sales AR/receipts/credit control.
10. Close Command Center.

### Why first

These features directly improve financial integrity, control and day-to-day business value.

---

## Phase 2 — Smart ERP Without AI

1. Smart Action Center.
2. Business tasks/work queues.
3. Recommendation engine.
4. Explainability framework.
5. Next Best Action.
6. Supplier score.
7. Contractor score.
8. Reorder recommendation.
9. Stockout forecast.
10. Payroll anomaly rules.
11. Attendance anomaly score.
12. Payment/invoice risk scoring.
13. Cash-flow forecast.
14. Budget forecast.
15. Scheduled reports.

At this point Bemo should already feel highly intelligent.

---

## Phase 3 — Operational Depth

1. Advanced WMS.
2. Lots/serial/expiry.
3. Manufacturing BOM/routing/costing.
4. MRP.
5. Capacity planning.
6. Quality NCR/CAPA.
7. Maintenance.
8. Fixed assets.
9. Contract management.
10. Document management.

---

## Phase 4 — Enterprise Finance

1. Multi-company consolidation.
2. Intercompany.
3. Multi-currency revaluation.
4. Advanced AR/collections.
5. Project accounting.
6. ETA e-invoice.
7. Close automation.
8. Financial statements and reporting packs.

---

## Phase 5 — Productization

1. Self-service portals.
2. Custom fields.
3. Configurable forms.
4. Report builder.
5. Automation builder.
6. Public API.
7. Webhooks.
8. BI warehouse.
9. On-premise deployment pack.
10. Compliance enhancements.

---

## Phase 6 — Optional AI

Only when commercially justified.

1. BYOK/local provider framework.
2. Read-only Copilot.
3. Explain/summarize.
4. Document OCR.
5. Natural-language analytics.
6. Draft-generation actions.
7. Controlled agent workflows.

---

# 75. Recommended P0 Detailed Implementation Sequence

## P0.1 Inventory Valuation

**Implementation status: DONE 2026-08-09.** V154/V155, FIFO/weighted-average evidence, Inventory GL, fiscal/backdating controls, revaluation, Angular valuation views and export are implemented and regression verified.

Backend:

- Valuation policy entity.
- Cost layers.
- Movement-cost records.
- Revaluation service.
- GL integration.
- Lock/concurrency tests.

Frontend:

- Valuation method setting.
- Item valuation view.
- Inventory valuation report.
- Movement cost drill-down.

QA:

- FIFO.
- Weighted average.
- Returns.
- Adjustments.
- Backdated posting.
- Closed period.
- Concurrent receipt/issue.

---

## P0.2 Supplier Onboarding

**Implementation status: DONE 2026-08-09.** V156/V157, controlled lifecycle, compliance-file evidence, duplicate detection, verified bank controls, shared approval integration, Supplier 360, and procurement/payment enforcement are implemented and regression verified (backend 320/66; frontend 227/36).

Backend:

- Lifecycle status.
- Supplier request.
- Documents.
- Bank accounts.
- Duplicate detection.
- Approval integration.

Frontend:

- Supplier request wizard.
- Duplicate warning.
- Compliance checklist.
- Bank verification.
- Supplier 360.

QA:

- Duplicate tax ID.
- Duplicate bank.
- Expired mandatory doc.
- Suspended supplier blocked from PO.
- Unverified bank blocked from payment.

---

## P0.3 Bank Reconciliation

**Implementation status: DONE 2026-08-09.** V158/V159, balance-validated CSV import, exact/partial/fee matching, reversible journal linkage, fiscal controls, reconciliation workbench, and currency-separated cash position are implemented and regression verified (backend 327/67; frontend 231/37).

Backend:

- Statement import.
- Statement lines.
- Match engine.
- Reconciliation.
- Journal linkage.

Frontend:

- Statement import.
- Reconciliation workbench.
- Suggested matches.
- Manual match.
- Bank fee handling.

QA:

- Exact match.
- Partial.
- Fee difference.
- Duplicate statement.
- Reversal.
- Closed period.

---

## P0.4 Approval Advanced

**Implementation status: DONE 2026-08-09.** V160/V161 deliver immutable workflow/document snapshots, dated scoped delegation, audited reassignment, concurrency-safe ANY_N signatures, per-step SLA and tenant-aware escalation with legacy-request backfill. The workspace provides aging/delegation filters, summaries, signature progress and delegation management. Regression evidence: backend 332/67 and frontend 236/38 with migration, error, i18n, hardcoded and build gates green.

Backend:

- Delegation.
- SLA.
- Escalation.
- snapshot.
- ANY_N policy.

Frontend:

- Delegation settings.
- Aging badges.
- Overdue filters.
- Approval summary.

QA:

- Delegation date range.
- Original user unavailable.
- Self-approval rule.
- Definition changes after instance start.

---

## P0.5 Attendance Advanced

**Implementation status: DONE 2026-08-09.** V162/V163 deliver effective-dated tenant/category/employee policy hierarchy, deterministic anomaly scoring, immutable policy snapshots, exception aggregation, idempotent bulk preview/apply, cross-midnight punch assignment, approval blocking, and employee/all-report payroll gates. The report-review workspace provides summary/filter/explanation, policy evidence, selection, preview-without-mutation, and controlled resolution. Regression evidence: backend 338/68 and frontend 238/38 with H2, 284/284 error-code, 2024-key i18n, hardcoded-UI and production-build gates green.

Backend:

- Policy hierarchy.
- anomaly scoring.
- exception aggregation.
- payroll gating.

Frontend:

- Exception workbench.
- explanation.
- bulk preview.

QA:

- cross-midnight.
- outage.
- manual override.
- locked period.

---

# 76. Data Integrity Rules for Every New Feature

Every new business aggregate should answer:

1. Is it tenant-owned?
2. Is company ownership required?
3. Does it need optimistic locking?
4. Does it need immutable evidence?
5. Does it need an operation ID?
6. What are legal state transitions?
7. Can it be reversed instead of deleted?
8. Does it affect finance?
9. Is fiscal-period validation required?
10. Does it require approval?
11. What audit event is generated?
12. What translation/error keys are required?

---

# 77. API Standards

Every new API should follow:

- Versioned `/api/v1`.
- Backend authorization.
- Tenant scope.
- Explicit DTOs.
- Validation.
- Stable error code.
- Localized message.
- Optimistic version where needed.
- Idempotency for retry-sensitive actions.
- Pagination for lists.
- Filter/sort contract.
- No business calculation duplicated in Angular.

For command operations prefer semantic endpoints:

```text
POST /purchase-orders/{id}/issue
POST /supplier-invoices/{id}/approve
POST /journal-entries/{id}/post
```

rather than generic status editing.

---

# 78. State Machine Standard

Avoid free-form status changes.

Example pattern:

```text
DRAFT
→ SUBMITTED
→ APPROVED
→ POSTED
→ REVERSED
```

Each transition should specify:

- Allowed source status.
- Required permission.
- Required fields.
- Approval requirement.
- Fiscal-period requirement.
- Journal effect.
- Notification event.
- Audit event.
- Idempotency behavior.

---

# 79. Deletion Policy

Financial/business evidence should rarely be hard-deleted.

Use:

- Cancel.
- Reverse.
- Deactivate.
- Void.
- Archive.

Hard delete only for safe draft/master-data cases and only if no downstream references exist.

---

# 80. Numbering Standard

Extend current numbering service concept to:

- PO.
- GRN.
- Supplier invoice internal ref.
- Payment.
- Customer invoice.
- Receipt.
- Journal.
- Asset.
- Contract.
- Project.
- Production order.
- Maintenance work order.
- NCR.

Pattern configurable per company/year.

---

# 81. Permissions Standard

Every screen needs at least:

```text
READ
CREATE
UPDATE
APPROVE
POST/EXECUTE
REVERSE/CANCEL
EXPORT
```

Not every module uses all.

High-risk permissions separate from general update.

Example:

```text
supplier.bank.update
supplier.bank.verify
payment.prepare
payment.approve
payment.execute
```

---

# 82. Audit Standard

Audit at minimum:

- Create.
- Update critical fields.
- State transitions.
- Approval.
- Rejection.
- Posting.
- Reversal.
- Payment.
- Bank change.
- Rate change.
- Manual attendance override.
- Permission change.
- Configuration change.

For financial records include before/after where appropriate.

---

# 83. Notification Standard

Notifications should be generated for a business reason, not every CRUD action.

Good:

- Approval required.
- Payment overdue.
- Stockout risk.
- Document expires.
- Match failed.
- Payroll anomaly.
- Device offline.

Bad:

- “Record opened.”
- “User viewed report.”

---

# 84. QA Strategy for Advanced Features

For every feature, define tests in six groups.

## Business rules

Correct calculation and states.

## Security

Role/permission/data scope.

## Tenant isolation

No cross-app/company leakage.

## Concurrency

Two users/processes act simultaneously.

## Idempotency

Retry does not duplicate.

## Integration

Financial and downstream effects.

Example procurement test chain:

```text
Requisition
→ Approval
→ PO
→ Encumbrance
→ GRN
→ Inventory
→ Invoice
→ Match
→ AP
→ Payment
→ Partner ledger
→ Journal
```

Verify every link.

---

# 85. Performance Requirements

Before large customer usage define targets.

Examples:

- Common screen API p95 < 500 ms under normal load.
- Search p95 < 1 sec.
- Large export background job.
- Import bounded rows/files.
- Pagination mandatory.
- No unbounded `findAll()` for large transactional entities.
- Indexed tenant/company/date/status filters.
- Bulk operations batched.

---

# 86. Background Jobs

Central job framework should expose:

- Job name.
- Schedule.
- Last run.
- Duration.
- Status.
- Failure.
- Retry count.

Jobs:

- Device sync.
- Exchange-rate refresh.
- Expiry alerts.
- Approval escalation.
- Reorder evaluation.
- Forecast refresh.
- Scheduled reports.
- Fiscal close checks.

Admin can see failures.

---

# 87. Configuration Precedence

As Bemo grows, define reusable precedence.

Example:

```text
User
> Department
> Branch
> Company
> App/Tenant
> Platform default
```

But not every setting should support every scope.

Each configuration key declares allowed scopes.

---

# 88. Financial Integration Standard

Every operational module should define accounting.

Examples:

## Procurement

- Inventory.
- GRNI.
- AP.
- Tax.
- FX.

## Sales

- AR.
- Revenue.
- Tax.
- COGS.
- Inventory.

## Payroll

- Salary expense.
- Liability.
- Tax/insurance.
- Department/project.

## Fixed assets

- Asset.
- AP/cash.
- Depreciation.
- Accumulated depreciation.
- Gain/loss disposal.

## Projects

- Cost.
- WIP.
- Revenue.

---

# 89. Master Data Governance

Critical masters:

- Account.
- Item.
- Supplier.
- Customer.
- Employee.
- Worker.
- Currency.
- Tax.
- Warehouse.

Need:

- Status.
- Effective date.
- Duplicate detection.
- Approval where risky.
- Import.
- Audit.
- Merge strategy where appropriate.

Avoid silently creating master data from transactional screens unless intentionally designed.

---

# 90. KPI Catalog

Define KPI formulas centrally.

Examples:

## Procurement

- PO cycle time.
- On-time delivery.
- Purchase price variance.
- Spend by supplier.
- Match failure rate.

## Inventory

- Inventory turnover.
- Days on hand.
- Stockout rate.
- Adjustment value.
- Count accuracy.

## HR

- Attendance rate.
- Absence.
- Overtime.
- Payroll variance.

## Finance

- AP aging.
- AR aging.
- Cash.
- Budget utilization.
- Close duration.

Central definitions prevent dashboards showing inconsistent values.

---

# 91. “Explain Every Number” UX Standard

Whenever a number can affect a business decision, provide drill-down.

Examples:

```text
Available Budget: EGP 420,000
[Explain]
```

Shows:

```text
Approved budget        1,000,000
Actual                   300,000
Open commitments         180,000
Encumbrances             100,000
Available                420,000
```

Similarly:

- Supplier balance.
- Stock balance.
- Payroll.
- Project cost.
- Cash forecast.
- Inventory value.

This is one of the best “intelligent ERP” features and requires no LLM.

---

# 92. “Why Is This Blocked?” Standard

When action is disabled, never show only a disabled button.

Example:

```text
Issue PO — unavailable
```

Explanation:

```text
Cannot issue because:
• Approval is pending.
• Department available budget is EGP 12,000.
• PO requires EGP 18,400.
```

Provide links to resolve each blocker.

---

# 93. “Impact Preview” Standard

Before significant bulk/action operations show impact.

Examples:

## Attendance bulk action

- Records.
- Employees.
- Payroll impact.

## Budget transfer

- Source after transfer.
- Destination after transfer.

## Fiscal close

- Unresolved items.

## PO cancellation

- Encumbrance released.
- Linked receipts/invoices blocking cancellation.

---

# 94. “Undo / Reverse” Standard

Where legally/business appropriate:

- Reversal rather than destructive edit.
- Show exact consequence.

Example:

```text
Reverse payment
→ restore invoice outstanding
→ create ledger reversal
→ create journal reversal
```

---

# 95. Recommended Starter Focus — What NOT to Build Yet

Do not prioritize yet:

- Fully autonomous AI purchasing.
- AI posting journals.
- AI executing bank payments.
- Complex vector database.
- Fine-tuning custom LLMs.
- Large microservice split.
- Kubernetes unless deployment scale requires it.
- Exotic blockchain audit.
- Custom ML platform.

These can consume time without improving the ERP fundamentals.

---

# 96. Recommended Definition of “Advanced Bemo v1”

Bemo can be called an advanced ERP when it has:

### Finance

- Full AP/AR.
- Bank reconciliation.
- Fixed assets.
- Inventory GL.
- Cash forecast.
- Period close.
- Multi-currency revaluation.

### Operations

- Procurement requisition/RFQ.
- Supplier lifecycle.
- Inventory valuation.
- WMS basics.
- Lot/serial.
- Manufacturing BOM/MRP/costing.

### People

- Advanced attendance exception engine.
- Payroll controls.
- Leave/overtime.
- Workforce/contractor scoring.
- Self-service.

### Control

- Approval SLA/delegation.
- Segregation of duties.
- Complete audit.
- Business tasks.
- Smart Action Center.

### Intelligence without AI

- Explain every calculation.
- Reorder recommendations.
- Stockout prediction.
- Supplier score.
- Contractor score.
- Risk scoring.
- Forecasting.
- Next Best Action.
- Exception-first UX.
- Scenario simulation.

### Platform

- Custom reports.
- Scheduled reports.
- API/webhooks.
- BI layer.
- Product customization.
- Strong backup/security/observability.

Paid AI can then become an optional premium feature instead of a requirement.

---

# 97. Suggested Tracking Format in the Repository

Create one backlog entry for each roadmap item with:

```text
ID:
Module:
Business objective:
Current behavior:
Target behavior:
Priority:
Dependencies:
Backend:
Frontend:
Database:
Permissions:
Audit:
Notifications:
Translations:
API:
Acceptance criteria:
Tests:
Migration:
Rollout flag:
```

Example:

```text
ID: INV-VAL-001
Module: Inventory
Business objective:
Provide auditable inventory valuation and GL integration.

Current:
Quantity movements exist; full FIFO/weighted-average valuation is incomplete.

Target:
Weighted-average + FIFO valuation with cost layer trace.

Dependencies:
Fiscal period, chart of accounts, warehouses.

Acceptance:
Every stock movement has quantity and cost effect;
inventory valuation report equals inventory GL;
backdated movement policy enforced.
```

---

# 98. Final Architecture Principle

The future Bemo architecture should be:

```text
                    UI / Mobile / Portals
                            |
                    Role Workspaces
                            |
                 Action Center / Tasks
                            |
                Business Application APIs
                            |
      ---------------------------------------------
      |              |             |              |
 Workflow        Rule Engine    Scoring       Recommendations
      |              |             |              |
      ---------------------------------------------
                            |
                    Domain Services
                            |
      Authorization / Tenant / Audit / Idempotency
                            |
                    PostgreSQL + Documents
                            |
            Analytics / BI / Events / Integrations
```

Optional AI later sits **above the domain services**, not inside the database:

```text
Optional AI
    |
Bemo Tool Gateway
    |
Domain Services
```

This guarantees that:

- Bemo works without AI.
- Customer data permissions remain controlled.
- Business calculations stay deterministic.
- Audit remains reliable.
- Customers can enable AI only if they want it.
- The product can support BYOK or local AI later.

---

# 99. Final Recommendation

The shortest path to making Bemo feel dramatically more advanced is not adding more screens.

Prioritize:

1. **Financial completeness.**
2. **Inventory valuation/integrity.**
3. **Exception-first workflows.**
4. **Strong supplier/customer processes.**
5. **Explainability.**
6. **Action Center + tasks.**
7. **Rule-based recommendations.**
8. **Forecasting.**
9. **Approval automation and SLA.**
10. **Deep integration across modules.**

A user should increasingly experience Bemo like this:

```text
Login
↓
See what needs attention
↓
Understand why
↓
See financial/operational impact
↓
Receive recommended action
↓
Review evidence
↓
Approve/execute through controlled workflow
↓
Everything is audited
```

That is a genuinely intelligent ERP even with **AI completely disabled**.

---

# 100. Repository Implementation Rule

When implementing this roadmap in `fm_bemo_consolidated`, preserve the project's existing conventions:

- Backend business logic remains authoritative.
- Angular does not duplicate financial/attendance calculations.
- App/tenant isolation remains mandatory.
- Security is enforced at API level, not UI only.
- New mutable aggregates use optimistic locking where concurrency matters.
- Retry-sensitive commands are idempotent.
- Financial postings respect fiscal periods.
- Business errors use stable localized error keys.
- Arabic and English translations are added with every feature.
- Menu/permission/access catalog changes stay synchronized.
- Audit events accompany material business changes.
- New features ship with backend/frontend tests and migration coverage.

This roadmap should be maintained as Bemo evolves. Completed items should be moved into `PROJECT_MAP.md` evidence and the remaining sections should be reprioritized based on customer demand rather than implemented mechanically in numerical order.


---

# 101. EXISTING FEATURE — User Preferences, Navigation & Shortcuts: Advanced Target

## Current baseline

Bemo already contains unusually good navigation foundations:

- Permission-aware navigation.
- `Ctrl+K` quick navigation.
- Keyboard chords.
- User-defined screen shortcuts.
- Cross-tab shortcut synchronization.
- Favorites/recent pages.
- Per-user dashboard/widget preferences.
- Theme/density/locale behavior.
- Reduced-motion support.

## Advanced additions

### A. Role-Based Workspace Home

Instead of every role opening a generic dashboard, route users to a role workspace:

```text
HR Manager         → HR Work Center
Finance Manager    → Finance Work Center
Procurement        → Procurement Work Center
Warehouse          → Warehouse Work Center
Workforce Manager  → Workforce Work Center
```

Workspace contains:

- My tasks.
- My overdue items.
- Most-used shortcuts.
- Recent records.
- Exceptions.
- Role KPIs.
- Quick create actions.

### B. Command Palette Actions

Extend `Ctrl+K` from navigation into safe commands:

```text
Create employee
Create supplier
Create draft PO
Open current payroll
Open month close
Search invoice
```

Commands must still obey permissions.

### C. Record-Level Favorites

Allow bookmarking:

- Supplier ABC.
- Project Alpha.
- Warehouse A.
- Payroll cycle.
- Favorite reports.

### D. Saved Views

For every large list let user save:

```text
My Overdue POs
Critical Attendance
Supplier Invoices > 100k
Unmatched Bank Lines
```

Saved view stores:

- Filters.
- Sort.
- Columns.
- Grouping.

Can be private/shared.

### E. Personalized Columns

Users can:

- Hide/show columns.
- Reorder.
- Resize.
- Freeze.
- Reset to default.

Tenant admin can define role default.

### F. Accessibility

Continue maturity:

- Full keyboard navigation.
- Focus management.
- High contrast.
- Screen-reader names.
- Reduced motion.
- RTL.
- Responsive tables.

---

# 102. EXISTING FEATURE — Desktop / On-Premise Client: Advanced Target

## Current baseline

The repository README identifies a Tauri Windows desktop package in addition to the web frontend.

## Product goal

The desktop client should not become a separate ERP implementation. It should remain a secure shell around the same API/business rules.

## Advanced capabilities

### A. Connection Profiles

Support:

```text
Production
Demo
Local/on-premise
Test
```

Store only safe endpoint configuration, not plain-text secrets.

### B. Auto Update

Signed application updates:

- Version check.
- Release notes.
- Signature validation.
- Controlled rollout.
- Rollback path.

### C. Offline Awareness

Do not promise full offline ERP unless explicitly developed.

Instead support:

- Clear offline status.
- Retry queue only for actions proven safe/idempotent.
- Read cache for selected non-sensitive reference data if needed.

Never queue a financial posting offline unless strict idempotency and conflict handling are implemented.

### D. Device Integration Helper

Desktop can be useful as a local bridge for:

- Biometric devices.
- Local printers.
- Scanners.
- Barcode hardware.
- Signature hardware.

The bridge should authenticate to Bemo using scoped machine credentials.

### E. Local Print Service

Support controlled printing of:

- Payslips.
- Purchase orders.
- GRNs.
- Labels.
- Barcodes.
- Warehouse pick lists.

---

# 103. EXISTING FEATURE — Licensing / Commercial Controls: Advanced Target

## Current baseline

The repository README identifies a separate Ed25519-based licensing application/service.

## Advanced licensing model

Licensing should support product commercialization without contaminating business data.

### A. License dimensions

Possible limits:

- Tenant/company.
- Expiration.
- User count.
- Branch count.
- Device count.
- Enabled modules.
- Support tier.

### B. Feature Entitlements

Example:

```text
CORE_HR
ATTENDANCE
WORKFORCE
PAYROLL
PROCUREMENT
INVENTORY
SALES
MANUFACTURING
ADVANCED_FINANCE
FIXED_ASSETS
PROJECTS
API
OPTIONAL_AI
```

Entitlements must be distinct from user permissions.

```text
License entitlement → Can tenant use the module?
User permission      → Can this user access/action it?
```

Both must pass.

### C. Grace Period

If license expires:

- Do not corrupt/lock data instantly.
- Configurable grace.
- Clear admin warnings.
- Read-only mode can be considered after grace.
- Avoid blocking statutory exports needed by customer without policy decision.

### D. License Audit

Record:

- License installed.
- License changed.
- Feature activated.
- Expiry.
- Validation failures.

### E. Offline/on-premise license verification

Use signed license payload and secure public-key verification so the customer server can validate without needing permanent internet.

---

# 104. EXISTING FEATURE — External Integrations: Advanced Target

## Current baseline

Bemo already has integration patterns for biometric devices and exchange-rate work.

## Create an Integration Center

Screen:

`Admin > Integrations`

Each connector shows:

```text
Name
Type
Enabled
Last success
Last failure
Next run
Authentication status
Records processed
Error count
```

## Connector framework

Standard capabilities:

- Configuration.
- Secret storage.
- Connectivity test.
- Scheduled sync.
- Manual sync.
- Cursor.
- Idempotency.
- Retry.
- Backoff.
- Dead-letter/failure queue.
- Audit.
- Health metrics.

## Examples

- Biometric.
- Frankfurter/rates.
- ETA e-invoice.
- Email.
- Bank feed.
- SMS.
- ERP partner APIs.
- File drop/SFTP.

## Integration error inbox

Do not hide failures in logs.

Example:

```text
Frankfurter Sync
FAILED
09 Aug 2026 16:00
HTTP timeout
Retry 2/5
Next retry 16:15
```

Authorized admin can retry.

## Rate Integration Specific Enhancements

For external FX rates:

- Preserve raw provider response/evidence if appropriate.
- Store source and retrieved timestamp.
- Never overwrite manually approved historical rates.
- Prevent duplicate rate rows.
- Set stale-rate warning.
- Configurable refresh interval.
- Provider failover later.
- Alert if variance from prior rate exceeds configurable percentage.

---

# 105. EXISTING FEATURE — Deployment, Upgrades & Operations: Advanced Target

## Current baseline

The project already includes Docker deployment, production/test profile boundaries, migration tooling, CI checks and PostgreSQL verification hardening.

## Advanced production maturity

### A. Upgrade Safety

Every release should provide:

- Application version.
- DB schema version.
- Migration list.
- Breaking-change notes.
- Backup prerequisite.
- Rollback procedure.
- Post-upgrade validation.

### B. Upgrade Readiness Endpoint

Admin can check:

```text
Current app: 1.8.0
DB schema: V148
Target: 1.9.0
Migration required: yes
Backup verified: yes/no
Disk capacity: okay
```

### C. Database Backup

Automate:

- Daily full backup.
- Retention.
- Encryption.
- Restore verification.
- Off-host copy.

A backup that has never been restore-tested should not be considered reliable.

### D. Monitoring

Operational dashboard:

- API status.
- DB.
- disk.
- memory.
- background jobs.
- queue depth.
- device sync.
- integration failures.
- last backup.
- email delivery.

### E. Release Feature Flags

Large new modules should be activated by tenant feature flags.

This enables:

- Gradual rollout.
- Pilot customer.
- Easy disable.
- Safer migration.

### F. Data Migration Tools

For customers coming from Excel/old ERP:

- Template downloads.
- Mapping.
- Dry run.
- Validation.
- Import reconciliation.
- Opening balances.
- Cutover checklist.

---

# 106. Current-to-Advanced Capability Matrix

This table should be used for quarterly roadmap review.

| Capability | Current foundation | Advanced target |
|---|---|---|
| Attendance | Strong | Exception workbench, policy hierarchy, anomaly score, explainability |
| Biometric | Strong | Fleet health, clock drift, failover, maintenance history |
| Workforce | Strong | Contractor 360, scoring, compliance, fulfillment optimization |
| Payroll | Strong | Component engine, simulation, variance controls, GL distribution, ESS payslip |
| Suppliers | Good | Onboarding, duplicate detection, compliance, bank verification, Supplier 360 |
| Procurement | Strong | Requisition, RFQ, agreements, sourcing analytics, exception workbench |
| Three-way match | Strong | Multi-dimensional tolerance, auto-clear, structured mismatch resolution |
| Inventory | Medium/Good | Valuation, reservation, reorder, WMS, GL, lot/serial |
| Sales | Foundation | Full quote-to-cash, credit control, ATP, returns, margin control |
| Manufacturing | Foundation | BOM, routing, costing, MRP, capacity scheduling |
| Quality | Foundation | Inspection plans, NCR/CAPA, quarantine, supplier quality |
| Finance | Good | Full statements, controls, close center, AR/AP depth, consolidation |
| Journals | Strong | Templates, recurring, auto-reversal, risk rules |
| Banks | Foundation/Good | Statement import, matching, reconciliation, cash position |
| Budgets | Strong | Versions, transfers, forecast, threshold escalation |
| Currency/Tax | Good | Revaluation, realized FX, approved external rates, tax determination |
| Approvals | Strong | Delegation, SLA, multi-signature, snapshots, risk routing |
| Notifications | Strong foundation | Task-oriented Action Center, suppression, auto-close |
| Dashboards | Strong foundation | Role workspaces, exception-first, forecast/next action |
| Reporting | Good | Report builder, scheduling, export center, BI layer |
| Audit | Good | Business-friendly before/after, export, correlation |
| Security | Strong | MFA, session center, step-up auth, SoD, data scope |
| Users/Access | Strong | Fine-grained permissions, temporary access, access reviews |
| Organization | Good | Legal-company ownership, hierarchy, consolidation |
| Translation | Strong | Lifecycle, missing-key dashboard, version/audit, more locales |
| Imports | Strong | Generic job framework, mapping templates, rejection workbook |
| Desktop | Existing | Signed updates, hardware bridge, safe offline behavior |
| Licensing | Existing | Entitlements, module packs, grace/read-only policy |
| Integrations | Existing patterns | Unified connector framework and integration health center |
| Deployment | Good | DR, restore tests, upgrade readiness, feature-flag rollout |
| Fixed Assets | Missing/Open | Complete lifecycle/depreciation/posting |
| AR/Collections | Missing/Open | Aging, credit, receipts, collections |
| Projects | Missing/Open | WBS, cost capture, WIP/P&L/forecast |
| Contracts | Missing/Open | Lifecycle, obligations, amendments, expiry |
| Maintenance | Missing/Open | Equipment, PM, work orders, cost/downtime |
| ETA | Missing/Open | Egypt tax e-invoice integration |
| WMS | Partial | Locations, putaway, picking, scanning, counts |
| Smart ERP | Partial building blocks | Rules, scoring, recommendation, explain, task engine |
| Generative AI | Not required | Optional BYOK/local premium layer only |

---

# 107. Commercial Packaging Recommendation

Because Bemo is still growing, avoid forcing every customer to buy every advanced module.

Possible packaging later:

## Core

- Organization.
- Users/security.
- Attendance basics.
- Reporting.
- Audit.

## Workforce & HR

- Advanced attendance.
- Workforce.
- Payroll.
- Leave.
- Self service.

## Trade

- Suppliers.
- Procurement.
- Inventory.
- Sales.

## Manufacturing

- BOM.
- MRP.
- Production.
- Quality.
- Maintenance.

## Finance

- GL.
- AP.
- AR.
- Banks.
- Budget.
- Assets.
- Multi-currency.
- Close.

## Enterprise

- Multi-company consolidation.
- Projects.
- Contracts.
- API/webhooks.
- BI.

## Intelligence Pack

Should work without paid AI:

- Smart Action Center.
- Recommendations.
- Forecasts.
- Risk scoring.
- Explainability.
- Scenario planning.

## Optional AI Pack

Only later:

- Copilot.
- Natural-language analytics.
- Document AI.
- Generative summaries.
- Draft agent actions.

This packaging allows Bemo to become commercially advanced while the **Intelligence Pack remains deterministic and inexpensive to operate**.

---

# 108. Product Strategy Upgrade — Bemo as a Vertical, Modular SaaS Platform

The commercial ideas behind niche positioning, modular pricing, short demos, free trials, sector-specific landing pages, direct sales, and gradual upsell should influence **the architecture of Bemo itself**.

The product should not be implemented as one generic ERP with one fixed menu, one fixed workflow, one fixed set of labels, and every customer seeing every feature. The target should be:

```text
Bemo Platform Core
    ↓
Country Pack
    ↓
Industry / Vertical Pack
    ↓
Purchased Modules
    ↓
Company Configuration
    ↓
Role / User Experience
    ↓
Optional Add-ons
```

Example:

```text
Bemo Platform
├── Egypt Country Pack
│   ├── EGP defaults
│   ├── Egyptian tax configuration
│   ├── ETA integration
│   └── Arabic defaults
│
├── Food Distribution Pack
│   ├── Batch / expiry tracking
│   ├── Customer credit controls
│   ├── Route / territory concepts
│   └── Distribution KPIs
│
└── Tenant: Nile Foods
    ├── Sales
    ├── Inventory
    ├── Procurement
    ├── Accounting
    └── 12 users
```

The same codebase can serve another customer as:

```text
Bemo Platform
├── Egypt Country Pack
├── Contractor Workforce Pack
└── Tenant: ABC Contracting
    ├── Workforce
    ├── Attendance
    ├── Contractor settlements
    └── Payroll
```

The most important architectural rule is:

> **Vertical specialization must be configuration-driven, not branch-driven.**

Do not create separate application branches such as `branch-food`, `branch-export`, or `branch-contractors`. Instead create reusable concepts such as:

```text
IndustryPack
CountryPack
ModuleEntitlement
IndustryWorkflowTemplate
IndustryDashboardTemplate
IndustryTranslationOverride
IndustryMasterDataTemplate
IndustryKpiTemplate
```

A customer then receives a configured product assembled from these layers.

This lets Bemo market itself very specifically while keeping one maintainable platform.

---

# 109. Vertical Industry Pack Framework

## 109.1 Purpose

Because the recommended go-to-market strategy is to begin with one niche, Bemo should support an explicit `IndustryPack` model so the niche is reflected throughout the actual product rather than only in marketing copy.

Suggested entity:

```text
industry_pack
-------------
id
code
name_key
description_key
status
version
country_code
minimum_platform_version
created_at
updated_at
```

Examples:

```text
FOOD_DISTRIBUTION_EG
CITRUS_EXPORT_EG
SMALL_MANUFACTURING_EG
CONTRACTOR_WORKFORCE_EG
SPARE_PARTS_DISTRIBUTION_EG
GENERAL_TRADING_EG
```

## 109.2 What an industry pack can define

An industry pack should be able to provide:

- Required modules.
- Recommended modules.
- Optional modules.
- Default menu groups.
- Industry terminology.
- Default roles.
- Default permission bundles.
- Default approval workflows.
- Default document numbering.
- Default reports.
- Default dashboards.
- Default KPIs.
- Default alert rules.
- Default custom fields.
- Default master data.
- Default transaction types.
- Default units of measure.
- Default scheduled jobs.
- Import templates.
- Demo data.
- Demo scripts.
- Onboarding checklist.
- Go-live validation checks.
- Industry-specific validations.
- Industry-specific help content.
- Recommended integrations.
- Country-specific requirements.
- Feature dependencies.

## 109.3 Pack installation

When a tenant is provisioned:

```text
Select Industry
    ↓
Resolve Pack Version
    ↓
Resolve Required Modules
    ↓
Create Default Settings
    ↓
Create Roles and Permissions
    ↓
Create Menus
    ↓
Install Translations
    ↓
Create Workflow Templates
    ↓
Create Dashboard Templates
    ↓
Create KPI Definitions
    ↓
Create Alert Rules
    ↓
Optionally Seed Demo Data
```

Every installation step must be:

- Idempotent.
- Versioned.
- Audited.
- Tenant-scoped.
- Retry-safe.

## 109.4 Pack upgrades

Pack upgrades must never silently overwrite customer changes.

Example:

```text
Food Distribution Pack v1.2
adds:
- expiry-risk dashboard
- FEFO picking recommendation
- customer route field
```

Tenant upgrade screen:

```text
Industry Pack Update Available

Current: 1.1
Available: 1.2

Changes:
✓ 3 new KPI definitions
✓ 1 new dashboard widget
✓ 4 new translation keys
⚠ 1 workflow template changed

[Preview Changes]
[Apply Non-Destructive Changes]
```

If a tenant customized a workflow or report template, preserve its current version and show a comparison instead of overwriting it.

---

# 110. Recommended Initial Vertical Packs

The platform should support several verticals eventually, but the business should initially perfect one or two.

## 110.1 Food Distribution Pack

Business outcomes:

- Know true stock by warehouse and batch.
- Reduce expired inventory.
- Control customer debt.
- Know margin by product/customer/route.
- Prevent overselling.
- Improve replenishment.

Recommended modules:

```text
Customers
Sales
Inventory
Warehouses
Procurement
Suppliers
Accounts Receivable
Accounting
Cash
Reports
```

Advanced pack features:

### Batch, expiry and FEFO

```text
Item: Juice 1L

LOT-A expiry: 2026-08-20 quantity 100
LOT-B expiry: 2026-09-15 quantity 400

Recommended issue:
LOT-A first
```

### Route / territory

Customer master additions:

```text
route_id
sales_representative_id
visit_days
delivery_zone
credit_days
credit_limit
```

### Distribution KPIs

- Expiry value in 7/30/60 days.
- Stockout items.
- Slow-moving inventory.
- Sales by route.
- Gross margin by route.
- Customer overdue amount.
- Delivery success rate.
- Return rate.
- Fill rate.

## 110.2 Citrus / Agricultural Packing & Export Pack

This vertical aligns strongly with Bemo's existing factory, workforce and produce-processing context.

Recommended modules:

```text
Suppliers / Farms
Procurement
Quality
Inventory
Production
Packing
Sales / Export
Workforce
Attendance
Finance
```

Important entities:

```text
farm
crop_season
crop_variety
field
harvest_batch
receiving_batch
sorting_grade
packing_specification
export_container
export_document
```

Traceability target:

```text
Farm
→ Harvest Batch
→ Receiving GRN
→ Sorting Batch
→ Packing Run
→ Finished Lot
→ Container
→ Customer Shipment
```

Advanced outcomes:

- Yield by farm.
- Rejection percentage by supplier.
- Packing yield.
- Waste percentage.
- Cost per packed ton.
- Export margin per container.
- Labor cost per ton.
- Quality claims by origin.
- Full recall traceability.

## 110.3 Contractor Workforce Pack

Build on Bemo's existing workforce strength.

Target customers can include:

- factories using outsourced labor.
- farms.
- warehouses.
- construction subcontracting operations.
- security / cleaning contractors.

Features:

- Contractor master.
- Worker registry.
- Worker categories.
- Labor requests.
- Assignments.
- Attendance.
- Daily wages.
- Advances.
- Deductions.
- 15-day settlements.
- Contractor invoice linkage.
- Contractor payment.
- Contractor scoring.
- Worker compliance.
- Workforce demand planning.

KPIs:

- Fill rate.
- Attendance reliability.
- Cost per worker-day.
- Absence rate.
- Contractor dispute rate.
- Settlement variance.
- Labor cost by site/department.

## 110.4 Small Manufacturing Pack

Recommended modules:

- Inventory.
- Procurement.
- BOM.
- Production.
- Quality.
- Maintenance.
- Sales.
- Finance.

Core outcomes:

- Material requirements.
- Work-order progress.
- Actual versus standard cost.
- Scrap.
- Machine downtime.
- Production efficiency.
- Customer delivery risk.

## 110.5 Spare Parts Distribution Pack

Important differentiators:

- Vehicle compatibility / fitment.
- Brand.
- OEM number.
- Alternative part numbers.
- Cross-reference search.
- Serial tracking for selected products.
- Supplier price history.
- Fast/slow-moving analysis.
- Branch transfers.
- Customer-specific pricing.
- Credit control.

Universal search should understand both internal and alternative part numbers.

---

# 111. Industry Terminology and UI Alias Engine

A specialized ERP should speak the customer's language without changing the underlying canonical data model.

Example:

```text
Canonical           Factory UI             Distribution UI
-----------------------------------------------------------
Business Party      Supplier / Customer    Supplier / Customer
Stock Movement      Material Movement      Warehouse Transaction
Production Order    أمر تشغيل              hidden if not relevant
Goods Receipt       إذن استلام              استلام مشتريات
```

Suggested entity:

```text
terminology_alias
-----------------
tenant_id
industry_pack_id
locale
canonical_key
display_value
```

Precedence:

```text
Tenant override
> Industry pack
> Country pack
> Platform default
```

This should reuse Bemo's existing app-aware i18n architecture rather than creating a separate competing translation mechanism.

Important rule:

> API/database concepts remain canonical; only presentation terminology changes.

---

# 112. Module Catalog and SaaS Entitlement Engine

The modular architecture should become a first-class platform capability.

## 112.1 Module catalog

Suggested module codes:

```text
CORE
HR
ATTENDANCE
WORKFORCE
PAYROLL
PROCUREMENT
INVENTORY
WMS
SALES
AR
FINANCE
BUDGETS
FIXED_ASSETS
MANUFACTURING
QUALITY
MAINTENANCE
PROJECTS
CONTRACTS
DOCUMENTS
ETA_EINVOICE
BI
ADVANCED_INTELLIGENCE
AI
```

Suggested entity:

```text
module_definition
-----------------
code
name_key
description_key
category
status
minimum_plan
dependencies_json
```

## 112.2 Tenant entitlements

```text
tenant_module_entitlement
-------------------------
tenant_id
module_code
status
source
starts_at
ends_at
grace_until
quantity_limit
configuration_json
```

`source` values:

```text
PLAN
ADD_ON
TRIAL
PROMOTION
MANUAL
LEGACY
```

## 112.3 Feature-level entitlements

A module may contain smaller commercial features.

Example:

```text
INVENTORY
├── inventory.basic
├── inventory.multiWarehouse
├── inventory.lotTracking
├── inventory.serialTracking
├── inventory.valuation
├── inventory.cycleCount
└── inventory.advancedForecast
```

## 112.4 Enforcement layers

Entitlement must be enforced in:

1. Backend API/service.
2. Route guards.
3. Navigation visibility.
4. Scheduled jobs.
5. Integration jobs.
6. Export endpoints.
7. Public APIs.
8. Background automation.

Never rely only on hiding the menu.

## 112.5 Dependency validation

Examples:

```text
WMS requires INVENTORY.
PAYROLL requires HR.
MRP requires INVENTORY + MANUFACTURING.
ETA_EINVOICE requires SALES + FINANCE.
```

When disabling a module with historical transactions, do not delete data. Support a `READ_ONLY` state where appropriate.

---

# 113. Subscription Plan Catalog

Do not hard-code only `Starter`, `Business`, and `Enterprise` into controllers or templates. Those are commercial configurations and can change.

Suggested entities:

```text
subscription_plan
plan_version
plan_module
plan_feature
plan_limit
plan_price
plan_country_price
```

Example:

```text
STARTER-EG v3
-------------
Core
Customers
Sales
Inventory Basic
Procurement Basic
5 users
1 company
1 branch
2 warehouses
```

Business:

```text
BUSINESS-EG v4
--------------
Everything in Starter
Finance
AR
Advanced Procurement
Budgets
Advanced Inventory
20 users
5 branches
10 warehouses
Approval workflows
Advanced dashboards
```

Enterprise:

```text
ENTERPRISE
----------
Configurable modules
Multi-company
Advanced security
API/Webhooks
Extended audit
Sandbox
Custom integration options
```

## 113.1 Plan versioning

Never mutate historical pricing invisibly.

```text
Customer A subscribed to BUSINESS-EG v3 on 2026-08-01.
BUSINESS-EG v4 launches later.
```

Customer A can remain on v3 until explicit migration/renewal policy applies.

Store `plan_version_id`, not just a plan name.

---

# 114. Usage Limits and Metering

Even if the first pricing model stays simple, Bemo should be able to measure usage so commercial models can evolve safely.

Possible limits:

- Named users.
- Active users.
- Companies.
- Branches.
- Warehouses.
- Employees.
- Workers.
- API calls.
- Document storage.
- OCR pages later.
- AI usage later.
- Integrations.
- Automated jobs.
- Portal users.

Suggested counter:

```text
tenant_usage_counter
--------------------
tenant_id
metric_code
period
current_value
included_limit
last_calculated_at
```

UI:

```text
Users       7 / 10
Storage     2.4 GB / 10 GB
Warehouses  3 / 5
```

Thresholds can be:

```text
80%  → information
95%  → warning
100% → block creation only if commercial policy requires
```

Never block access to historical data merely because a limit is reached.

---

# 115. Tenant Lifecycle / SaaS Control Plane

Bemo needs a platform-level tenant lifecycle separate from ordinary company configuration.

Suggested lifecycle:

```text
LEAD
→ TRIAL_PROVISIONING
→ TRIAL_ACTIVE
→ TRIAL_EXPIRED
→ ACTIVE
→ PAYMENT_GRACE
→ SUSPENDED
→ CANCEL_PENDING
→ CANCELLED
→ ARCHIVED
```

The SaaS control plane should manage:

- Tenant.
- Subscription.
- Plan/version.
- Enabled modules.
- Usage.
- Billing.
- Trial.
- Provisioning.
- Customer-success status.
- Support metadata.
- Data retention state.
- Region.
- Industry pack.
- Country pack.

This can initially live in the same Spring Boot application, but its domain boundary should remain clear so it can later become a separate service if necessary.

---

# 116. Automated Tenant Provisioning

Creating a new customer should eventually become a workflow rather than manual SQL/setup.

Provisioning request:

```text
Company Name: Nile Foods
Country: Egypt
Industry: Food Distribution
Plan: Business
Language: Arabic
Currency: EGP
Trial: Yes
```

System performs:

```text
1. Create tenant/app.
2. Reserve tenant code.
3. Create subscription/trial.
4. Enable modules.
5. Apply country pack.
6. Apply industry pack.
7. Create default organization.
8. Create fiscal year.
9. Create default currency.
10. Install translations.
11. Install roles/permissions.
12. Install workflow templates.
13. Install dashboards/KPIs.
14. Create tenant admin.
15. Send activation.
16. Create onboarding checklist.
17. Record provisioning audit.
```

Provisioning states:

```text
REQUESTED
VALIDATING
DATABASE_SETUP
PACK_INSTALL
ADMIN_CREATE
READY
FAILED
```

On failure, platform operators need step name, localized error code, correlation ID, retry control and cleanup/rollback guidance.

---

# 117. Trial Management

A 14-day trial should be a product capability, not a manual spreadsheet reminder.

Fields:

```text
trial_started_at
trial_ends_at
trial_plan_version_id
trial_pack_id
trial_extension_count
trial_conversion_status
```

States:

```text
NOT_STARTED
ACTIVE
ENDING_SOON
EXPIRED
CONVERTED
```

Possible lifecycle messages:

```text
Day 0  → Welcome + onboarding
Day 1  → Help create first master data
Day 3  → Help complete first real workflow
Day 7  → Usage/setup summary
Day 11 → Trial ends in 3 days
Day 13 → Final reminder
Day 14 → Trial expires
```

Trial expiration should not delete data. A reasonable policy is:

- writes stop after expiry.
- read-only grace may continue for a configurable period.
- exports remain available according policy.
- data remains retained for a defined period.
- conversion action is clear.

Example:

```text
Your trial ended on 23 Aug 2026.
Your data is safe until 22 Sep 2026.

[Choose a Plan]
[Export My Data]
```

---

# 118. Resettable Sales Demo Environment

Sales demos should not rely on a random development database containing inconsistent or stale data.

A demo environment should have:

- Stable demo company.
- Realistic transactions.
- Multiple demo roles/personas.
- Story-based datasets.
- Easy controlled reset.
- No real customer data.
- Data that demonstrates both normal flows and meaningful exceptions.

Example Food Distribution demo data:

```text
Customers: 25
Suppliers: 10
Items: 80
Warehouses: 2
POs: 12
GRNs: 9
Supplier invoices: 8
Sales orders: 30
Customer invoices: 28
Receipts: 22
Inventory lots: 100+
```

Include intentionally useful exceptions:

- One overdue customer.
- One low-stock item.
- One expiring lot.
- One late supplier.
- One price variance.
- One pending approval.

That gives the presenter something intelligent to demonstrate.

A reset action must validate `tenant.is_demo = true` before any destructive work.

---

# 119. Guided Demo Story Engine

A strong demo follows a business process, not random screens.

Optional presenter mode:

```text
Scenario: Sell EGP 50,000 to Al Noor Market

1. Open customer.
2. Show credit position.
3. Create sales order.
4. Check inventory.
5. Confirm order.
6. Create delivery.
7. Create invoice.
8. Record EGP 20,000 payment.
9. Show remaining receivable.
10. Show inventory impact.
11. Show margin.
12. Show accounting entries.
```

Presenter sidebar:

```text
Demo Story
6 / 12 completed

✓ Customer
✓ Credit
✓ Sales Order
✓ Availability
✓ Delivery
✓ Invoice
→ Record Payment
```

This can be enabled only for demo tenants or sales users.

---

# 120. Guided Customer Onboarding

A new ERP customer should not arrive at a complex dashboard with no direction.

Create a role-aware onboarding center:

```text
Your setup is 63% complete.

✓ Company information
✓ Fiscal year
✓ Currency
✓ Chart of accounts
✓ First warehouse
○ Import suppliers
○ Import customers
○ Import opening stock
○ Create users
○ Configure approvals
○ Test first transaction
```

Dependencies must be explicit.

Example:

```text
Company
↓
Fiscal Configuration
↓
Master Data
├── Customers
├── Suppliers
├── Items
└── Employees
↓
Opening Balances
↓
Workflows
↓
Users
↓
Go-Live Validation
```

Step states:

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
SKIPPED
BLOCKED
```

The industry pack provides relevant steps so a Workforce tenant is not asked to configure manufacturing.

---

# 121. Go-Live Readiness Center

Before a tenant becomes operational, run readiness checks based on enabled modules and pack.

Example:

```text
Go-Live Readiness: 86%

BLOCKERS
❌ Fiscal period not opened
❌ Bank opening balance missing

WARNINGS
⚠ 12 items have no reorder policy
⚠ 3 suppliers missing tax numbers

READY
✓ Users configured
✓ Roles configured
✓ Warehouses configured
✓ Document numbering configured
✓ Backups configured
```

The readiness framework should allow each module/pack to contribute checks.

---

# 122. Starter Mode vs Advanced Mode

Small customers can be overwhelmed by enterprise-level configuration even if those controls are useful.

Introduce UX complexity levels:

```text
Experience Mode
○ Simple
○ Standard
○ Advanced
```

### Simple

Show essential fields and actions.

### Advanced

Expose accounting dimensions, cost centers, budget links, audit details, tax/rate information and deeper workflow controls.

Example PO simple mode:

```text
Supplier
Date
Items
Quantity
Price
```

Advanced mode can also show:

```text
Department
Project
Budget
Currency rate
Contract
Delivery terms
Tax configuration
Cost center
Approval route
```

Prefer metadata/visibility rules over separate duplicate components.

---

# 123. In-Product Module Upgrade Center

If modularity is a key commercial advantage, tenant admins should understand optional modules without being pressured during everyday operations.

Admin screen:

```text
Modules & Plan
```

Example:

```text
Inventory                 ACTIVE
Sales                     ACTIVE
Finance                   ACTIVE

Warehouse Management      AVAILABLE
Fixed Assets              AVAILABLE
Manufacturing             AVAILABLE
Projects                  AVAILABLE
```

Click WMS:

```text
Advanced Warehouse Management

Adds:
✓ bins and locations
✓ directed putaway
✓ picking
✓ cycle counts
✓ barcode workflows

Your existing inventory data is compatible.

[Start Add-On Trial]
[Request Demo]
```

Keep upgrade discovery primarily in admin/commercial surfaces, not as intrusive popups.

---

# 124. Module Add-On Trial

A tenant can trial an add-on without changing the full plan immediately.

Example:

```text
WMS add-on
Trial: 7 days
```

During the trial:

- Uses the real tenant data.
- New actions remain fully audited.
- Existing inventory records are preserved.
- At expiry, WMS can become read-only/disabled by policy.

Before enabling, preview impact:

```text
Impact:
- 3 new menus
- 2 new permission groups
- new warehouse-location structures
- no change to existing historical accounting
```

---

# 125. Module Entitlement, Feature Flag and Permission Separation

Keep these concepts separate:

```text
MODULE ENTITLEMENT
"What the customer purchased."

FEATURE FLAG
"What code/release is safely enabled."

USER PERMISSION
"What this person is allowed to do."
```

Example:

```text
Tenant purchased WMS       = yes
WMS release feature flag   = yes
User has wms.pick          = no
```

Result: the module exists but the user cannot perform picking.

Do not collapse commercial entitlement, release management and authorization into one boolean mechanism.

# 126. Product Analytics Platform

The first customers should teach the product team what is valuable. Bemo therefore needs privacy-conscious product analytics in addition to business audit logs.

Track allowlisted events such as:

```text
screen.opened
feature.used
workflow.started
workflow.completed
export.created
import.started
import.failed
onboarding.step_completed
report.saved
module.trial_started
```

Example:

```json
{
  "event": "workflow.completed",
  "tenantId": "...",
  "userId": "...",
  "module": "PROCUREMENT",
  "workflow": "PO_TO_GRN",
  "durationSeconds": 180
}
```

Do not put sensitive business values into generic usage analytics by default.

Avoid:

```text
salary
national ID
bank account
password/token
invoice attachment contents
```

Product analytics should answer:

- Which modules are actually used?
- Which screens are opened but abandoned?
- Which workflows take too long?
- Which onboarding step causes the most drop-off?
- Which features correlate with trial activation?
- Which reports are most frequently used?
- Which error codes occur most?
- Which add-on trials convert?
- Which active customers have declining usage?

Product analytics must be non-blocking: if analytics storage is unavailable, the ERP transaction must still complete.

---

# 127. Customer Health Score

Create a deterministic health score to identify successful, struggling or churn-risk customers without paid AI.

Possible dimensions:

```text
Activation          20%
Usage               25%
Workflow Adoption   20%
Data Quality        10%
Operational Health  10%
Support               5%
Commercial          10%
```

Example:

```text
Nile Foods
Health: 82 / 100

Activation             100
Usage                    88
Workflow adoption        72
Data quality              90
Operational health        95
Support                   80
Commercial                70
```

Positive signals:

- Multiple weekly active users.
- Core workflows completed.
- More than one department using the ERP.
- Scheduled reports configured.
- Approval workflows used.
- Integrations healthy.
- Month close completed.

Negative signals:

- Admin is the only active user.
- No core transactions for many days.
- Repeated failed imports.
- Critical setup incomplete.
- Many unresolved high-priority tickets.
- Trial near expiry without activation milestones.
- Usage sharply declining.

Every health score must show reasons. Never display an unexplained magic number.

---

# 128. Activation Milestones by Industry

Trial/product success should not be measured only by login.

Food Distribution example:

```text
✓ created warehouse
✓ imported 20+ items
✓ created customer
✓ created supplier
✓ created first purchase
✓ received stock
✓ created first sale
✓ viewed margin / stock result
```

Workforce example:

```text
✓ created contractor
✓ imported workers
✓ recorded attendance
✓ reviewed exceptions
✓ generated settlement preview
```

Manufacturing example:

```text
✓ created BOM
✓ created production order
✓ issued material
✓ recorded production
✓ received finished goods
```

Pack metadata should define milestones and their business meaning.

---

# 129. In-App Feedback Center

Add a simple `Help & Feedback` entry.

Actions:

- Report a problem.
- Suggest a feature.
- Ask a question.
- Rate this page/workflow.
- Send general feedback.

Automatically attach safe context:

```text
tenant ID
user ID
route
application version
browser
correlation ID
feature flags
module
```

Do not automatically attach form field values or sensitive business data.

User-submitted screenshot/attachment can be optional later.

---

# 130. Feature Request Management

Early customer requests should be normalized instead of living only in chats or memory.

Suggested model:

```text
feature_request
---------------
tenant_id
user_id
module_code
title
description
business_problem
frequency
business_impact
requested_at
status
linked_roadmap_item
```

Statuses:

```text
NEW
REVIEWING
PLANNED
IN_PROGRESS
RELEASED
DECLINED
DUPLICATE
```

Capture the **business problem**, not only the requested UI.

Example:

Weak request:

> Add a button to group unpaid invoices.

Better captured problem:

> Finance spends 30 minutes every morning manually identifying suppliers whose invoices should be paid this week.

The second description leaves room for a better solution such as a payment workbench or recommendation rule.

---

# 131. Support Center

A commercial ERP SaaS requires structured support.

Customer-facing area:

```text
Support
├── Tickets
├── Knowledge Base
├── System Status
└── Contact
```

Ticket fields:

```text
priority
category
module
screen
business_impact
description
attachments
status
assigned_team
sla_due_at
```

Statuses:

```text
NEW
TRIAGED
IN_PROGRESS
WAITING_CUSTOMER
RESOLVED
CLOSED
```

Business-impact guidance:

```text
LOW      question / cosmetic issue
MEDIUM   workaround exists
HIGH     important workflow blocked
CRITICAL business operation stopped
```

Require an impact description for Critical priority.

---

# 132. Context-Aware Knowledge Base and Learning

Help should know the current screen/module.

On Purchase Orders:

```text
? Help
```

can show:

- How to create a PO.
- How approval works.
- Why a PO can be blocked.
- How budget availability is calculated.
- Meaning of each status.
- Common errors.

Industry packs can add sector-specific help.

Tours should be:

- Role-specific.
- Pack-specific.
- Dismissible.
- Reopenable.
- Versioned.
- Non-blocking.

Example procurement tour:

```text
1. Requisition
2. RFQ
3. Purchase Order
4. Receipt
5. Invoice
6. Payment
```

---

# 133. Free Business Tools / ROI Calculators

The idea of free calculators can be implemented as deterministic public tools and also reused inside Bemo.

## Profit Calculator

Inputs:

```text
Purchase Price
Selling Price
Quantity
Direct Expenses
Fixed Expenses
```

Outputs:

```text
Revenue
COGS
Gross Profit
Net Profit
Gross Margin %
Net Margin %
Break-even Quantity
```

## Inventory Holding Cost Calculator

Inputs:

```text
Average stock value
Annual carrying %
Warehouse cost
Insurance
Expiry/shrinkage estimate
```

## Payroll Cost Calculator

Inputs:

```text
Headcount
Daily/monthly wage
Overtime
Employer costs
Average absence
```

## Cash Conversion Cycle Calculator

Inputs:

```text
Inventory Days
Receivable Days
Payable Days
```

## ERP ROI Calculator

Inputs:

```text
Employees doing manual administration
Hours/week
Average hourly cost
Monthly stock loss estimate
Late collection cost
Error/rework estimate
```

Calculate an **estimated** administrative opportunity and show all assumptions.

Possible public endpoints:

```text
POST /api/public/calculators/profit
POST /api/public/calculators/holding-cost
POST /api/public/calculators/erp-roi
```

Rate-limit public endpoints.

Anonymous calculation should not require contact details. Lead capture can be optional when a visitor requests saving/emailing the result.

---

# 134. Minimal CRM for Selling Bemo

Bemo does not need to become a giant CRM immediately, but the SaaS operator should have a small internal sales pipeline.

Suggested internal module:

```text
BEMO_SALES_CRM
```

Entities:

```text
lead
company_prospect
contact
opportunity
activity
demo_booking
trial
quote
subscription
```

Pipeline:

```text
NEW_LEAD
→ CONTACTED
→ QUALIFIED
→ DEMO_SCHEDULED
→ DEMO_DONE
→ TRIAL
→ PROPOSAL
→ WON
→ LOST
```

For lost opportunities capture reason:

```text
PRICE
MISSING_FEATURE
NO_BUDGET
NO_DECISION
COMPETITOR
TOO_COMPLEX
IMPLEMENTATION_CONCERN
OTHER
```

This gives a real answer to:

> Why did prospects reject the product?

---

# 135. Lead Source Attribution

Suggested sources:

```text
DIRECT_SALES
WHATSAPP
FACEBOOK
LINKEDIN
GOOGLE
REFERRAL
PARTNER
WEBSITE
FREE_TOOL
EVENT
OTHER
```

Track:

```text
Lead → Demo → Trial → Paid
```

KPIs later:

- Lead-to-demo rate.
- Demo-to-trial rate.
- Trial-to-paid conversion.
- Conversion by vertical.
- Conversion by acquisition source.

At the early stage, lead acquisition cost can remain manual if advertising spend is not integrated.

---

# 136. Sales Activity and Follow-Up

CRM activities:

```text
Call
WhatsApp
Meeting
Demo
Follow-up
Email
Note
```

Opportunity timeline:

```text
Aug 10  Call
Aug 11  Demo booked
Aug 13  Demo completed
Aug 13  Trial created
Aug 17  Follow-up due
```

Every active opportunity should have a clear next action and owner so prospects are not forgotten.

---

# 137. Trial-to-Paid Conversion Workflow

Connect CRM, trial and provisioning.

```text
Opportunity
→ Create Trial
→ Provision Tenant
→ Track Activation
→ Create Quote
→ Accept
→ Convert Existing Trial Tenant
→ Activate Subscription
```

Do not create a new tenant by default during conversion. Preserve trial tenant data unless the customer explicitly requests a clean production setup.

Conversion should:

- Remove/replace trial expiration.
- Apply purchased plan version.
- Apply purchased modules.
- Preserve IDs.
- Preserve audit history.
- Preserve configuration.
- Record commercial event.

If sample data exists, give a clear cleanup choice before go-live.

---

# 138. Quote / Proposal Builder

Commercial quote fields:

```text
Customer
Industry
Plan
Modules/Add-ons
Users
Implementation Fee
Recurring Fee
Billing Cycle
Annual Discount
Support Level
Validity Date
Terms Version
```

Price version and discount source must be recorded.

Non-standard discounts can use the existing approval engine.

Example:

```text
0–10% discount    Sales Manager
10–20%            Commercial Director
>20%              Executive Approval
```

Later generate a branded proposal document.

---

# 139. Subscription Billing Engine

Bemo should have a subscription-billing domain even if payment collection is initially manual.

Suggested entity:

```text
subscription
------------
tenant_id
plan_version_id
billing_cycle
start_date
renewal_date
status
currency
base_amount
discount
tax
payment_method
auto_renew
```

Billing cycles:

```text
MONTHLY
QUARTERLY
ANNUAL
CUSTOM
```

Subscription invoices:

```text
DRAFT
ISSUED
PAID
PARTIALLY_PAID
OVERDUE
VOID
```

Commercial billing must be logically separate from a customer's own ERP receivables unless the architecture deliberately reuses shared accounting components with strict tenant/platform boundaries.

---

# 140. Upgrade / Downgrade / Proration

Upgrade example:

```text
Starter EGP 500/month
→ Business EGP 1,500/month
on day 15
```

Possible policies:

- Immediate prorated charge.
- Upgrade effective next billing date.
- Manual commercial adjustment.

Downgrade requires safety analysis.

Example:

```text
Manufacturing currently contains:
- 42 BOMs
- 17 open production orders
- 5 quality inspections

Downgrade cannot complete until open production orders are closed.
Historical records will remain read-only.
```

Never destroy historical data on downgrade.

---

# 141. Commercial Grace / Suspension Policy

Suggested lifecycle:

```text
ACTIVE
→ PAYMENT_DUE
→ GRACE
→ RESTRICTED
→ SUSPENDED
```

Example policy:

```text
Due date      invoice due
+3 days       reminder
+7 days       grace warning
+14 days      restrict new postings
+30 days      suspend non-admin access
```

This should be configurable by commercial policy.

Even when suspended:

- Data remains retained.
- Authorized admin can see subscription/billing.
- Data export policy remains available.
- No automatic database deletion occurs.

---

# 142. Promotions and Founding-Customer Pricing

Early customers may receive special commercial terms.

Represent them cleanly:

```text
Promotion: FOUNDING10
10 early customers
30% discount
12 months
```

Suggested entities:

```text
promotion
promotion_redemption
subscription_discount
```

Do not alter the standard plan definition for one customer's special deal.

---

# 143. Renewal Management

For annual customers, generate internal/customer reminders according to policy:

```text
90 days
60 days
30 days
7 days
```

Internal renewal view:

```text
Renewal: 30 days
Health: 85
Usage: High
Open critical tickets: 0
Expansion opportunity: WMS
```

A renewal should not depend on a salesperson remembering an Excel date.

---

# 144. In-Product Expansion Recommendations Without AI

Use deterministic rules and show them only to tenant admins or account owners.

Example:

```text
IF tenant uses INVENTORY
AND warehouse_count >= 3
AND monthly_movements > 3000
AND WMS not enabled
THEN recommend WMS
```

Another:

```text
IF high asset-purchase volume
AND FIXED_ASSETS not enabled
THEN recommend Fixed Assets
```

Another:

```text
IF overdue receivables are frequent
AND advanced AR not enabled
THEN recommend Collections Workbench
```

Recommendations should explain the observed condition and be dismissible. Do not turn operational pages into advertising surfaces.

---

# 145. Customer Data Import Packs by Vertical

Migration quality can determine whether a trial succeeds.

Food Distribution templates:

```text
customers.xlsx
suppliers.xlsx
items.xlsx
opening_stock_by_batch.xlsx
customer_opening_balances.xlsx
supplier_opening_balances.xlsx
price_lists.xlsx
```

Workforce templates:

```text
contractors.xlsx
workers.xlsx
worker_categories.xlsx
advances.xlsx
opening_settlements.xlsx
```

Manufacturing templates:

```text
items.xlsx
bom.xlsx
work_centers.xlsx
machines.xlsx
opening_stock.xlsx
```

Each template should include:

- Instructions sheet.
- Required/optional indicator.
- Example row.
- Validation lists.
- Data type.
- Maximum length.
- Related master/reference.
- Error explanations.

---

# 146. Migration Project Center

For assisted customers, create an implementation workspace.

```text
Migration Project: Nile Foods
```

Stages:

```text
DISCOVERY
MAPPING
CLEANING
DRY_RUN
CUSTOMER_VALIDATION
FINAL_LOAD
RECONCILIATION
SIGNED_OFF
```

Dataset card:

```text
Customers
Rows: 1,250
Valid: 1,223
Warnings: 20
Errors: 7
Approved: yes
```

Financial reconciliation example:

```text
Opening AR source total     1,280,450
Bemo imported AR total      1,280,450
Difference                          0
```

Financial opening balances should require reconciliation/sign-off before go-live.

---

# 147. Vertical Workflow Templates

Industry packs should install workflow templates.

Food Distribution:

```text
Sales Order
→ Credit Check
→ Stock Allocation
→ Picking
→ Delivery
→ Invoice
→ Receipt
```

Manufacturing:

```text
Production Plan
→ Material Requirement
→ Production Order
→ Material Issue
→ Production
→ Quality
→ Finished Receipt
```

Workforce:

```text
Labor Request
→ Approval
→ Worker Assignment
→ Attendance
→ Settlement
→ Invoice
→ Payment
```

Reuse the generic state-machine, approval and task infrastructure rather than creating pack-specific workflow engines.

---

# 148. Industry KPI Template Engine

Suggested definition:

```text
kpi_definition
--------------
code
module
industry_pack
name_key
description_key
calculation_type
calculation_service
target_direction
warning_threshold
critical_threshold
unit
drilldown_route
```

Food Distribution KPIs:

- Expiring stock value.
- Fill rate.
- Route margin.
- Customer DSO.
- Stockout count.

Manufacturing:

- Scrap percentage.
- Schedule adherence.
- Material variance.
- Labor efficiency.
- Machine downtime.

Workforce:

- Fulfillment rate.
- Absence.
- Cost per worker-day.
- Settlement variance.

---

# 149. Sector Dashboard Templates

Default dashboard should look relevant immediately after provisioning.

Food Distribution owner:

```text
Today's Sales
Gross Margin
Collections
Overdue AR
Inventory Value
Expiring Stock
Low Stock
Open POs
```

Factory manager:

```text
Plan vs Actual
Production Delays
Scrap
Material Shortage
Quality Holds
Machine Downtime
Labor Attendance
```

Workforce manager:

```text
Requested Workers
Assigned Workers
Attendance
Absence
Unresolved Attendance
Upcoming Settlement
Advances
Contractor Score
```

Users can personalize later, but the pack provides a strong starting point.

---

# 150. Business Outcome / ROI Evidence Center

Marketing claims should map to measurable product evidence.

Possible business-outcome measures:

- Inventory discrepancies identified/resolved.
- Expiring stock identified before expiry.
- Overdue receivables collected.
- Duplicate supplier invoices blocked.
- Approval turnaround reduced.
- Bulk attendance exceptions resolved.
- Budget overruns prevented/flagged.
- Reconciliation backlog reduced.

Be careful with causal claims.

Good:

> Bemo identified EGP 82,000 of inventory expiring within 30 days.

Not automatically valid:

> Bemo saved EGP 82,000.

The second claim requires evidence that the loss was actually avoided.

During onboarding, tenant admin can optionally enter baseline process times so a later ROI report can show transparent estimates with assumptions.

# 151. SaaS Administration Command Center

Super Admin/platform operators need a view across customers without default access to their business records.

Cards:

```text
Active Tenants
Active Trials
Trials Ending This Week
Suspended Tenants
Failed Provisioning
Integration Failures
Backup Failures
Critical Support Tickets
Upcoming Renewals
```

Tenant table:

```text
Code
Name
Industry
Plan
Status
Users
Modules
Health
Last Activity
Renewal
```

Tenant 360 should combine:

```text
Commercial
- plan
- subscription
- billing
- renewal

Product
- modules
- active users
- activation
- health
- application version

Operations
- integrations
- scheduled jobs
- backup status
- recent technical errors

Support
- open tickets
- SLA
- last incident

Implementation
- onboarding
- migration
- training
```

Business data access remains behind separate support-access controls.

---

# 152. Support Access / Impersonation Governance

Do not build a hidden unrestricted `login as customer` function.

Use explicit support-access sessions:

```text
Support requests access
→ reason
→ scope
→ duration
→ approval according policy
→ step-up authentication
→ time-limited support session
→ visible banner
→ full audit
```

Example banner:

```text
Support Access Active
Engineer: support-user
Expires: 15:30
Reason: Ticket SUP-221
```

Scope examples:

```text
READ_ONLY
CONFIGURATION
MODULE_PROCUREMENT
MODULE_INVENTORY
```

Sensitive operations can remain prohibited even when support access is active.

Platform roles should distinguish:

```text
TENANT_METADATA_VIEW
BILLING_VIEW
SUPPORT_TICKET_VIEW
SUPPORT_ACCESS_REQUEST
TENANT_DATA_ACCESS
SECURITY_ADMIN
```

A commercial employee who changes a plan does not automatically need payroll or accounting visibility.

---

# 153. Country Pack Architecture

Country requirements and industry requirements are separate layers.

Example:

```text
Country Pack: Egypt
```

can provide:

- Arabic locale defaults.
- EGP defaults.
- tax terminology.
- ETA integration configuration.
- national-ID validation rules.
- phone format defaults.
- invoice fields.
- cheque amount-to-Arabic-words.
- weekend defaults.
- bank statement parsers.

Future:

```text
UAE
Saudi Arabia
Jordan
```

Suggested tenant regional settings:

```text
country
timezone
base_locale
secondary_locale
functional_currency
date_format
number_format
first_day_of_week
weekend_days
tax_country
```

Do not embed Egyptian-specific behavior into generic finance services unless it is truly a country rule.

---

# 154. Marketplace / Extension Architecture

Long-term modularity benefits from a controlled integration/extension catalog.

Examples:

```text
ETA Connector
ZKTeco Connector
Frankfurter FX Connector
WooCommerce Connector
Shopify Connector
WhatsApp Notification Connector
Bank Statement Parser
```

Metadata:

```text
extension_definition
extension_version
required_modules
required_permissions
configuration_schema
health_check_type
status
```

Do not allow arbitrary third-party executable code inside the backend initially. Start with first-party connectors under a common framework.

Integration Center card:

```text
ZKTeco
Connected
Last sync: 10 min ago

Frankfurter
Healthy
Last rate refresh: 2h ago

ETA
Not Configured
```

Every connector should support:

- Configure.
- Test connection.
- Sync now where applicable.
- Last success.
- Last failure.
- Error inbox.
- Health status.
- Disable.
- Permission/audit visibility.

---

# 155. Industry-Specific Integration Recommendations

Industry packs can recommend connectors without automatically enabling them.

Food Distribution:

- barcode/scanner integration.
- e-commerce later.
- mobile route sales later.

Manufacturing:

- biometrics.
- barcode/scanner.
- machine/IoT data later.

Export:

- ETA.
- FX feed.
- shipping/freight systems later.

A pack can show:

```text
Recommended Integration
ZKTeco Attendance
Reason: Workforce module enabled and 150+ employees configured.
```

---

# 156. Public Landing-Page Configuration API

If sector-specific landing pages are created later, a public non-sensitive catalog can drive them.

Example:

```text
GET /api/public/industry-packs/food-distribution
```

Returns only approved public content:

- Marketing name.
- Business outcomes.
- Module highlights.
- Supported country.
- Demo availability.
- Trial availability.
- Free tools/calculators.

This can power:

```text
/food-distribution
/manufacturing
/export
```

without exposing tenant data or internal feature flags.

---

# 157. Trial Signup Flow

Potential self-service flow:

```text
Industry Landing Page
→ Start Trial
→ Name
→ Company
→ Mobile
→ Email
→ Industry
→ Approximate Company Size
→ Verify / Set Password
→ Provision Tenant
→ Onboarding
```

Request the minimum information needed to create value. Do not put a 25-question implementation survey before the user sees the application.

Abuse protections can include:

- Rate limiting.
- Email/phone verification where appropriate.
- CAPTCHA only if abuse becomes material.
- Trial limits.
- Duplicate-account detection.
- External-integration restrictions.

---

# 158. WhatsApp-First Commercial Workflow

For Egypt/MENA direct sales, customer/prospect records can store:

```text
preferred_channel = WHATSAPP
whatsapp_opt_in = true
```

Early-stage functionality can simply:

- Copy approved message template.
- Open a WhatsApp conversation link.
- Record follow-up activity manually.

Later, integrate an approved WhatsApp Business provider if there is real demand.

Do not build unsolicited mass-messaging or scraping functions.

---

# 159. Customer Communication Preferences

Separate categories:

```text
Operational Alerts
Billing
Product Updates
Training
Marketing
Security Notices
```

Security/service-critical messages may be mandatory. Marketing consent should remain separate from service notifications.

---

# 160. Release Notes / What's New

Add an in-product release center.

Example:

```text
Version 1.8

NEW
- Supplier Performance Dashboard
- Bank Reconciliation

IMPROVED
- Faster Inventory Search

ACTION REQUIRED
- Review new permission: bank.reconcile
```

Filter release notes by enabled modules/industry pack so customers do not see irrelevant features.

A one-time adoption card can say:

```text
Bank Reconciliation is now available.
You already have two bank accounts configured.
[Configure Statement Import]
```

Keep it dismissible and non-intrusive.

---

# 161. Customer Data Portability / Exit Package

A serious SaaS ERP should make data portability part of customer trust.

Admin action:

```text
Export My Organization Data
```

Possible package:

```text
masters/
transactions/
documents/
audit/
configuration/
README.md
```

Formats:

- CSV/XLSX for business records.
- JSON for configuration/metadata.
- Original uploaded documents.

Sensitive export requires step-up authentication and audit.

---

# 162. Account Cancellation Workflow

Cancellation should be controlled:

```text
Request Cancellation
→ Reason
→ Effective Date
→ Outstanding Billing
→ Export Options
→ Retention Notice
→ Confirmation
```

Reasons:

```text
PRICE
MISSING_FEATURE
BUSINESS_CLOSED
COMPETITOR
IMPLEMENTATION_FAILED
LOW_USAGE
OTHER
```

This produces useful product feedback while giving customers a predictable exit.

Do not immediately delete data on cancellation or missed payment.

---

# 163. Data Retention After Cancellation

Lifecycle:

```text
ACTIVE
→ CANCELLED
→ RETENTION
→ SCHEDULED_DELETION
→ DELETED
```

Before permanent deletion:

- Notify authorized contacts.
- Allow export.
- Respect legal/business retention.
- Ensure backups follow deletion policy.
- Create deletion audit evidence.

---

# 164. Sandbox Environment

Business/Enterprise customers may need:

```text
Production
Sandbox
```

Sandbox use cases:

- Test imports.
- Test new approval rules.
- Train staff.
- Test module upgrades.
- Test integrations.
- Test configuration changes.

Sandbox external integrations must default to safe mode:

- No real payments.
- No ETA production submissions.
- No production webhooks.
- No real notification recipients unless explicitly configured.

---

# 165. Tenant Configuration Transport

Allow controlled movement of safe configuration between sandbox and production.

Eligible configuration:

- Roles/permission bundles.
- Approval definitions.
- Custom fields.
- Report definitions.
- Dashboard layouts/templates.
- Automation rules.
- Selected settings.

Never transfer:

- Passwords.
- Tokens.
- API secrets.
- Bank credentials.
- Production transactions.

Always preview diff before applying.

---

# 166. Industry Template Version Control

Every reusable template should have:

```text
template_id
version
source_pack
release_notes
created_at
```

Tenant install record:

```text
tenant_template_install
```

This lets support say:

> Tenant uses Food Distribution Pack 1.3 and Sales Workflow Template 2.1.

That is much easier to reproduce than undocumented manual setup.

---

# 167. Per-Industry Default Roles

Food Distribution example:

```text
Owner
Sales Manager
Sales Representative
Warehouse Manager
Procurement Manager
Accountant
Cashier
```

Manufacturing:

```text
Factory Manager
Production Planner
Production Supervisor
Quality Manager
Warehouse Manager
Maintenance Manager
Accountant
```

Workforce:

```text
Workforce Manager
Attendance Reviewer
Contractor Accountant
Site Supervisor
```

Roles should be reusable permission bundles and remain tenant-customizable.

---

# 168. Role-Based Demo Personas

Demo tenant should contain accounts such as:

```text
owner.demo
sales.demo
warehouse.demo
finance.demo
```

During demo:

> Now we switch to Warehouse Manager.

Show the same process with different menus/actions. This demonstrates permissions and workflow value more effectively than using only an all-powerful Admin account.

---

# 169. Process-Oriented Navigation

In addition to normal module menus, starter users can use visual business flows.

Sales:

```text
Quotation → Sales Order → Delivery → Invoice → Receipt
```

Procurement:

```text
Request → RFQ → PO → Receive → Invoice → Pay
```

Workforce:

```text
Request → Assign → Attend → Settle → Pay
```

Show counts at each stage:

```text
Sales Orders   8 waiting
Deliveries     3 today
Invoices       5 unposted
Collections    9 overdue
```

This aligns the product with business outcomes rather than database entities.

---

# 170. Process Home Pages

Each major module should evolve from a table-only page into a process home with:

```text
Needs Attention
Upcoming
Recent Activity
KPIs
Create / Start
```

Procurement example:

Needs Attention:

- Late POs.
- Unmatched supplier invoices.
- Pending approvals.
- Expiring supplier documents.

Upcoming:

- Expected deliveries today/tomorrow.
- Invoices due for payment.

This is both easier for SMB users and more advanced than a flat document list.

---

# 171. Owner "What Do I Need To Do Today?" Home

For SMB owners/managers, make the default experience action-oriented.

```text
Today

Sales
3 orders need delivery

Cash
EGP 84,000 expected today

Customers
2 overdue accounts need follow-up

Inventory
4 items may run out this week

Purchasing
2 POs are late

Approvals
3 items waiting for you
```

This is one of the strongest non-AI intelligence features Bemo can add.

---

# 172. Owner Snapshot

Simple periods:

```text
Today
This Week
This Month
```

Metrics:

- Sales.
- Collections.
- Purchases.
- Expenses.
- Gross margin.
- Cash.
- Receivables.
- Payables.
- Inventory value.
- Payroll/workforce cost.

Every number must drill to source and disclose its calculation basis.

Do not show accounting profit if the tenant has not enabled/configured complete COGS/accounting; label operational margin clearly instead.

---

# 173. Business Health Summary

A deterministic owner summary can show:

```text
Cash          Green
Receivables   Yellow
Inventory     Yellow
Suppliers     Green
Payroll       Green
Operations    Red
```

Explain each state.

Example:

> Inventory is Yellow because eight priority items are below reorder level and EGP 23,000 of stock expires within 30 days.

Avoid a mysterious overall score without the underlying reasons.

---

# 174. Plan Recommendation Wizard

Sales website/CRM can ask:

```text
How many users?
How many branches?
Need accounting?
Need payroll?
Need manufacturing?
Use contractor labor?
Need multiple companies?
```

Rule engine returns:

```text
Recommended: Business + Workforce
```

and explains why.

This can be deterministic and generated from the same plan/module catalog used by the backend.

---

# 175. Implementation Package Catalog

Recurring SaaS and one-time services should be separate.

Examples:

```text
Self Setup
Assisted Setup
Migration Package
Full Implementation
Custom Integration
Training Package
```

Store service order separately from recurring subscription.

This prevents implementation effort from being hidden inside monthly pricing.

---

# 176. Data Quality Score

Imported Excel data often creates implementation problems.

Show master-data quality:

```text
Customers    94%
Suppliers    87%
Items        91%
Employees    99%
```

Flag:

- Duplicate tax IDs.
- Missing UoM.
- Invalid categories.
- Missing payment terms.
- Missing item cost.
- Invalid phone/email.
- Active biometric employee without device/fingerprint ID.

Provide a fix work queue.

---

# 177. Industry Data Quality Rules

Food Distribution:

- Batch-managed item without shelf life.
- Perishable item without expiry policy.
- Saleable item without selling UoM.

Manufacturing:

- Produced item without BOM.
- BOM component without source policy.
- Machine without maintenance policy.

Workforce:

- Active worker without category.
- Biometric worker without identifier.
- Contractor without settlement rule.

---

# 178. Adoption-Safe Defaults and Progressive Configuration

A starter customer should not answer 100 settings before first use.

Industry defaults can set sensible values such as:

```text
Negative inventory: Block
Automatic numbering: On
Reorder method: Min/Max
```

Advanced questions appear only when relevant.

Examples:

- No multi-company → hide consolidation setup.
- No foreign currency → no revaluation setup.
- No manufacturing module → no work-center setup.
- No payroll → no salary-policy setup.

Before go-live, show all important defaults and allow review.

---

# 179. Configuration Recommendations Without AI

Rules can identify incomplete configuration.

Examples:

```text
You have 3 warehouses but warehouse-level replenishment is not configured.
```

```text
You sell on credit but 82% of active customers have no credit limit.
```

```text
72% of active inventory value has no reorder policy.
```

Actions:

```text
[Configure]
[Ignore]
[Remind Later]
```

Store ignored recommendations so users are not repeatedly nagged.

---

# 180. Configuration Validation Before Major Operations

Before first payroll:

```text
✓ payroll cycle
✓ employee pay definitions
✓ attendance period
❌ payroll clearing account missing
```

Before inventory accounting go-live:

```text
❌ valuation method not configured
```

Before ETA submission:

```text
❌ certificate missing
❌ taxpayer identity incomplete
```

Configuration errors should be detected before users attempt a final posting.

---

# 181. Customer Success Playbooks

Rule-based internal playbooks can create tasks.

Example:

```text
Trial has 5 days left
AND first sale not completed
→ customer success task:
"Help customer complete first sales workflow."
```

Another:

```text
Active customer
AND no login for 14 days
→ customer success check-in
```

Another:

```text
5+ failed imports in 7 days
→ implementation/support task
```

---

# 182. Churn Risk Rules

Simple first version:

```text
No active user 14 days           +30
No core transaction 30 days      +30
3+ unresolved high tickets       +20
Renewal <30 days                  +10
Usage decline >50%                +15
Payment overdue                   +25
```

Risk bands:

```text
0–29   Low
30–59  Medium
60+    High
```

Always show contributing reasons.

---

# 183. First-10-Customers Learning Dashboard

Internal dashboard:

```text
Customer
Industry
Why Bought
Primary Workflow
Top Features Used
Unused Purchased Modules
Top Problems
Feature Requests
Support Count
Health
Renewal Risk
```

After ten customers, summarize:

- Most common reason to buy.
- Most common missing capability.
- Most common onboarding blocker.
- Most valuable feature/module.
- Most unused module.
- Most common support problem.
- Most common lost-deal reason.

This is the system version of the principle that the first ten customers should shape the product.

---

# 184. Vertical Fit Score for Sales Qualification

When a prospect describes requirements, calculate fit from the capability catalog.

Example:

```text
Food Distribution Fit: 92%

Supported
✓ batch/expiry
✓ credit sales
✓ multi-warehouse
✓ procurement
✓ accounting

Needs configuration
△ route-sales settlement

Not currently supported
✕ offline van sales
```

This prevents overselling and makes missing requirements explicit before the contract.

---

# 185. Capability Catalog

Create canonical capabilities:

```text
inventory.lotTracking
inventory.expiry
sales.creditControl
procurement.rfq
finance.bankReconciliation
workforce.contractorSettlement
```

Each capability has:

```text
status = GA | BETA | PLANNED | NOT_SUPPORTED
module
industry relevance
minimum version
permission
help/documentation reference
```

The catalog can later power:

- Sales fit analysis.
- Plan comparison.
- Release notes.
- Public feature pages.
- Onboarding.

A roadmap item must never be presented publicly as `GA` merely because it exists in this README.

---

# 186. Product Edition / Stability Levels

Feature lifecycle:

```text
EXPERIMENTAL
ALPHA
BETA
GENERAL_AVAILABILITY
DEPRECATED
RETIRED
```

Rules:

- Experimental only on internal/demo/sandbox unless explicitly approved.
- Beta offered to selected design partners.
- GA requires migrations, tests, documentation and support readiness.
- Deprecated features have migration path and deadline.

---

# 187. Design Partner Program

Before major vertical features become GA, use 2–5 real design partners.

Track:

- Use cases.
- Weekly feedback.
- Migration complexity.
- Workflow fit.
- Performance.
- Support load.
- Acceptance criteria.

Lifecycle:

```text
DESIGN_PARTNER
→ BETA
→ GA
```

---

# 188. Vertical Acceptance Criteria

A pack should not be marketed as complete until its primary end-to-end processes are tested.

Food Distribution example:

```text
[ ] customer → order → delivery → invoice → receipt
[ ] supplier → PO → receipt → invoice → payment
[ ] batch + expiry
[ ] FEFO
[ ] customer credit control
[ ] aging
[ ] stock valuation
[ ] margin
[ ] finance posting
```

Citrus/Export:

```text
[ ] supplier/farm → receiving batch
[ ] quality/sorting
[ ] production/packing
[ ] lot traceability
[ ] finished inventory
[ ] export/customer order
[ ] shipment/container
[ ] cost/margin
[ ] labor cost
```

---

# 189. Sales Demo Acceptance Tests

Every official demo story should have automated backend/integration coverage for its critical business flow.

Distribution story example:

```text
create customer
create item
receive stock
create sales order
deliver
invoice
receipt
verify inventory
verify AR
verify GL
verify margin source
```

Every release should run a demo smoke suite for the supported verticals.

A sales demo must never depend on a workflow that is not tested.

---

# 190. Commercial Demo Script Standard

Each vertical should have a 5–10 minute script.

### Minute 0–1 — Business problem

Example:

> You have two warehouses and customers buy on credit.

### Minute 1–5 — One transaction flow

Demonstrate the complete business process.

### Minute 5–7 — Exception

Show:

- low stock.
- overdue customer.
- mismatch.

### Minute 7–9 — Management result

Show:

- margin.
- cash/receivable.
- inventory impact.
- Action Center.

### Minute 9–10 — Modular growth

Explain:

> When you later need WMS/Accounting/Payroll, enable the module without replacing the system.

---

# 191. Trial Activation UX

If a trial has no data, dashboard should not show meaningless zeros only.

Show:

```text
Start Here

1. Add or import products
2. Add a customer
3. Record first purchase
4. Make first sale
5. See inventory and margin
```

Offer:

```text
[Use Sample Data]
[Start Empty]
[Import My Data]
```

Sample data must be clearly marked and safely removable.

---

# 192. Sample Data Isolation

If a trial uses sample records, tag them using a safe batch marker so they can be removed before go-live.

Example:

```text
demo_data_batch_id
```

Before conversion:

```text
Sample data is still present.

○ Keep it
○ Remove it
○ Create a clean production tenant and migrate configuration
```

Never mix demo and real records invisibly.

---

# 193. Role-Based Success Checklists

Owner:

- view dashboard.
- review cash/receivables.
- approve transaction.

Warehouse:

- receive stock.
- find stock.
- transfer/count.

Finance:

- opening balances.
- invoice.
- receipt/payment.
- reconciliation.

Users see only relevant onboarding steps.

---

# 194. Usage-Based Product Recommendations

Example:

A user manually exports the same report every Monday four times.

Rule suggests:

> You frequently export this report. Would you like to schedule it weekly?

Another:

A user repeatedly filters `Overdue` customers and saves no view.

Suggest:

> Save this as "My Overdue Customers"?

These small deterministic suggestions make the product feel intelligent without an LLM.

---

# 195. Mobile Priority by Role

Do not try to make every configuration screen mobile-first immediately.

Prioritize high-value mobile roles:

### Owner

- KPIs.
- approvals.
- alerts.

### Sales

- customers.
- orders.
- collections later.

### Warehouse

- scan/receive/pick/count.

### Supervisor

- workforce/attendance.

Large accounting setup can remain desktop-oriented.

---

# 196. Offline Strategy by Workflow

Do not promise a generic offline ERP.

Classify workflows:

```text
Online required
- financial posting
- final approval
- global master changes

Potential offline later
- warehouse scan capture
- field-sales draft
- attendance capture
```

Offline sync requires explicit conflict resolution and idempotency.

---

# 197. Customer/Supplier Portals as Retention Features

Customer portal:

- Orders.
- Invoices.
- Statements.
- Receipts/payment status.
- Delivery status.
- Documents.

Supplier portal:

- RFQ.
- PO.
- delivery instructions.
- invoice upload/status.
- payment status.
- compliance documents.

Industry packs can specialize portal capabilities without creating separate portal codebases.

---

# 198. SaaS Security Baseline for Growth

Before scaling customer count materially, ensure:

- MFA / step-up authentication.
- Tenant isolation tests.
- Session controls.
- Support-access governance.
- Encrypted secrets.
- Rate limiting.
- Audit export.
- Backup verification.
- Least privilege.
- Dependency/vulnerability scanning.
- Incident response process.

The existing Bemo security foundation is strong; SaaS commercialization adds operational governance requirements.

---

# 199. Trial / Demo Security Restrictions

Trial/demo can restrict:

- Real payment execution.
- Production ETA submissions.
- Arbitrary external webhooks.
- Public API access by default.
- Bulk outbound messaging.
- Very large imports.
- Destructive mass operations.

Read-only FX and similar low-risk integrations can remain available.

---

# 200. SaaS Product Packaging — Final Layer Model

Use four layers:

## Layer 1 — Platform

Always included:

- Authentication.
- Users.
- Permissions.
- Organization.
- i18n.
- Audit.
- notifications.
- imports/exports.
- base reporting.

## Layer 2 — Business Modules

Examples:

- Sales.
- Inventory.
- Procurement.
- Finance.
- HR.
- Workforce.
- Manufacturing.

## Layer 3 — Vertical Pack

Examples:

- Food Distribution.
- Citrus Export.
- Contractor Workforce.
- Small Manufacturing.

## Layer 4 — Add-ons

Examples:

- WMS.
- Fixed Assets.
- Projects.
- BI.
- API.
- advanced integrations.
- Intelligence.
- AI later.

A commercial plan is just a controlled bundle of these capabilities.

# 201. Recommended Starter Commercial Offer

Keep public pricing simple even if internal entitlement architecture is flexible.

A sensible initial structure can be represented internally as:

```text
Starter
Business
Enterprise
```

### Starter

For small firms:

- Core platform.
- Customers.
- Suppliers.
- Sales.
- Basic inventory.
- Basic purchasing.
- Basic reports.

### Business

Adds:

- Finance.
- AR/AP depth.
- Advanced inventory.
- Approval workflows.
- Budgets.
- Scheduled reports.
- Higher limits.

### Enterprise

Adds/permits:

- Multi-company.
- Advanced security.
- API/webhooks.
- sandbox.
- custom integrations.
- extended audit.
- negotiated add-ons.

Actual prices remain data/configuration, never constants in source code.

---

# 202. First Vertical Recommendation for Bemo

Based on the current codebase, Bemo has unusually strong foundations in:

- Attendance.
- Contractor workforce.
- Procurement.
- Inventory operations.
- Factory-oriented workflows.
- Manufacturing/quality beginnings.

That makes the following initial commercial directions especially plausible:

```text
1. Citrus / agricultural packing and export operations
2. Contractor-workforce-heavy factories / operations
3. Food distribution after AR + expiry/WMS depth is completed
```

This is a product-positioning recommendation, not a requirement to rename the general platform.

A useful internal fit matrix:

| Area | Food Distribution | Citrus Export | Contractor Workforce | Manufacturing |
|---|---:|---:|---:|---:|
| Existing fit | High | Very High | Very High | Medium/High |
| Missing core work | AR/expiry/WMS | export/trace docs | Low/Medium | MRP/capacity |
| Demo strength today | Good | Very Good | Very Good | Good |
| Implementation complexity | Medium | Medium | Low/Medium | High |
| Cross-sell potential | High | High | Medium | High |

Update this matrix from real discovery calls rather than assumptions.

---

# 203. First-10-Customer Implementation Discipline

For every early customer:

1. Record the vertical.
2. Record the top three pains.
3. Record what system/Excel/manual process they use now.
4. Define one end-to-end workflow to go live first.
5. Configure the selected industry pack.
6. Import minimum viable data.
7. Train one to three internal champions.
8. Go live on one workflow.
9. Measure usage, errors and support.
10. Expand only when the first workflow is stable.

Do not enable 15 modules just because they exist.

---

# 204. Land-and-Expand Architecture

Bemo should support a customer starting with:

```text
Sales + Inventory
```

then later adding:

```text
Procurement
Finance
Payroll
WMS
```

Historical data should naturally support expansion, but accounting-impacting modules need explicit cutover rules.

Example:

```text
Inventory GL effective date: 2026-10-01
```

Operational inventory before this date remains historical operational data; accounting opening balances are introduced at cutover according to reconciliation policy.

Every module must define:

- Prerequisites.
- Activation migration.
- Cutover/effective date.
- Opening data requirements.
- Historical behavior.
- Deactivation/read-only behavior.
- Accounting impact.

---

# 205. No-Custom-Code Rule for First-Line Requests

Before adding tenant-specific source code, check whether the requirement can be solved with:

1. Setting.
2. Custom field.
3. Workflow.
4. Report/view.
5. Terminology override.
6. Industry pack.
7. Integration/extension.

Only then consider custom code.

This rule is critical if Bemo is to remain SaaS instead of becoming a collection of customer forks.

---

# 206. Tenant Customization Inventory

For every tenant, provide support/admin visibility into:

```text
Custom Fields
Custom Workflows
Custom Reports
Custom Dashboards
Custom Integrations
Custom Document Templates
Custom Terminology
Pack Deviations
```

This allows support to understand configuration quickly and helps pack upgrades detect conflicts.

---

# 207. Configuration Deviation Registry

Show differences from pack defaults.

Example:

```text
PO approval threshold        customized
Negative stock policy        pack default
Payroll advance rule         customized
Customer credit policy       customized
```

Configuration snapshots used for support must exclude secrets and sensitive business transactions.

---

# 208. Suggested New Backend Domains

Keep commercial/product concerns separate from ERP business services.

Suggested packages:

```text
com.bemo.platform.catalog
com.bemo.platform.entitlement
com.bemo.platform.subscription
com.bemo.platform.provisioning
com.bemo.platform.trial
com.bemo.platform.industry
com.bemo.platform.productanalytics
com.bemo.platform.customersuccess
com.bemo.platform.support
com.bemo.platform.crm
```

Do not put subscription-plan conditions directly inside Payroll or Procurement business logic.

Those modules should ask an entitlement service whether the capability is available.

---

# 209. Suggested New Frontend Areas

Platform operator:

```text
/platform/tenants
/platform/plans
/platform/modules
/platform/trials
/platform/provisioning
/platform/support
/platform/health
/platform/crm
```

Tenant admin:

```text
/admin/subscription
/admin/modules
/admin/onboarding
/admin/support
/admin/data-export
```

These need separate permissions from ERP business roles.

---

# 210. Suggested Platform Events

Use the common event framework proposed earlier.

```text
TENANT_CREATED
TENANT_PROVISIONING_STARTED
TENANT_READY
TRIAL_STARTED
TRIAL_ENDING
TRIAL_CONVERTED
TRIAL_EXPIRED
SUBSCRIPTION_STARTED
SUBSCRIPTION_CHANGED
MODULE_ENABLED
MODULE_DISABLED
ONBOARDING_STEP_COMPLETED
CUSTOMER_HEALTH_CHANGED
SUPPORT_TICKET_CREATED
RENEWAL_UPCOMING
```

Events should drive notifications/tasks without creating hidden side effects inside controllers.

---

# 211. Suggested Scheduled Jobs

```text
TrialLifecycleJob
SubscriptionRenewalJob
BillingReminderJob
CustomerHealthCalculationJob
ActivationMilestoneJob
UsageAggregationJob
ProductRecommendationJob
PackUpdateCheckJob
RetentionCleanupJob
```

All jobs must be:

- Idempotent.
- Monitored.
- Retry-safe.
- Audited when consequential.
- Timezone-aware where dates are customer-facing.

---

# 212. Suggested New Permissions

Platform roles:

```text
platform.tenants.read
platform.tenants.manage
platform.subscriptions.read
platform.subscriptions.manage
platform.billing.read
platform.billing.manage
platform.support.read
platform.support.manage
platform.support.access.request
platform.productAnalytics.read
platform.crm.read
platform.crm.manage
```

Tenant-side:

```text
subscription.read
subscription.manage
modules.read
modules.manage
onboarding.manage
support.create
support.read
```

Commercial admin, support, security and customer-business permissions must not be implicitly equivalent.

---

# 213. Control Plane Data Isolation Rule

Platform tables may contain references to many tenants and therefore do not use the same semantics as tenant-owned ERP tables.

Required tests:

- Tenant endpoint can only see its own subscription.
- Tenant A cannot change Tenant B modules.
- Platform sales role can read commercial metadata but cannot read tenant payroll/finance data.
- Support access is explicit and expires.
- Platform super admin business-data access is governed and audited.

This boundary deserves dedicated integration tests.

---

# 214. Entitlement Check Standard

Recommended service API:

```text
entitlementService.requireFeature("inventory.valuation")
```

or a reusable annotation/interceptor where appropriate.

Stable error:

```text
FEATURE_NOT_ENTITLED
```

Safe response context can include:

```text
featureCode
moduleCode
```

Do not leak internal prices through generic authorization errors.

---

# 215. Commercial Access State Standard

Trial/subscription expiration must be centralized.

Do not add date checks in dozens of services.

Create a commercial-access resolver that determines whether current tenant can:

```text
READ
CREATE
UPDATE
POST
EXPORT
ADMINISTER
```

according to:

- Trial state.
- Subscription state.
- Grace policy.
- Module entitlement.

Stable error examples:

```text
SUBSCRIPTION_READ_ONLY
TRIAL_EXPIRED
SUBSCRIPTION_SUSPENDED
```

Backend remains authoritative.

---

# 216. Read-Only Grace Mode

A consistent read-only state is useful for:

- Expired trial.
- Commercial grace.
- Module downgrade.
- Certain suspensions.

Frontend:

- Forms disabled.
- Clear banner.
- Historical data visible.
- Export allowed according policy.

Backend rejects writes with stable machine codes.

---

# 217. Background Jobs Under Suspension

Define behavior per job.

Example when tenant is suspended:

Continue:

- Backups.
- Security/audit retention.
- retention timers.

Pause or restrict:

- Recommendations.
- outbound webhooks.
- scheduled report emails.
- external posting integrations.

Inbound events need safe handling and must not silently mutate forbidden business state.

---

# 218. Module Dependency and Pack Validation Tests

CI should detect:

```text
A requires B
B requires A
```

unless deliberately represented as one combined module.

Pack installation tests must prove:

- Required modules exist.
- Required permissions exist.
- Menu routes exist.
- Translations exist.
- Default workflows are valid.
- Default dashboards only reference enabled capabilities.
- Upgrade remains idempotent.

---

# 219. Plan / Entitlement Regression Tests

Examples:

```text
Starter tenant cannot access Finance API.
Business tenant can access Finance when authorized.
WMS cannot enable without Inventory.
Downgrade preserves historical data read-only.
Expired trial blocks postings but allows permitted exports.
Module entitlement never bypasses user permission checks.
```

Commercial correctness needs the same test discipline as accounting correctness.

---

# 220. Product Analytics Privacy Tests

Analytics payloads should use an allowlist.

Allowed examples:

```text
event
route
module
result
duration
error_code
```

Rejected/sensitive field patterns:

```text
password
token
salary
nationalId
bankAccount
```

A unit/CI check should fail if known sensitive fields are accidentally added to generic product-event DTOs.

---

# 221. Demo Data Isolation Test

A destructive demo reset endpoint must fail unless:

```text
tenant.is_demo = true
```

Also require high-privilege authorization and explicit confirmation.

Audit:

```text
DEMO_RESET
actor
template_version
timestamp
```

This is a hard safety requirement.

---

# 222. Trial Conversion Regression Test

Test:

```text
Trial → Paid Subscription
```

Expected:

- Same tenant ID.
- Same business records.
- Same users.
- Same audit history.
- New entitlement snapshot.
- Trial restrictions removed.
- Billing/subscription state active.

---

# 223. Customer Export Regression Test

Export validation should compare record counts/totals.

For financial exports, include reconciliation summaries.

Example:

```text
Customer Invoice Count   1,202
Exported Invoice Count   1,202
Difference                   0
```

Document-export manifests should list missing/corrupt attachments rather than silently omit them.

---

# 224. Advanced Customer Experience Acceptance Criteria

Before calling Bemo fully SaaS-productized, the roadmap should converge on:

```text
[ ] tenant provisioning
[ ] module entitlements
[ ] plan versioning
[ ] trial lifecycle
[ ] guided onboarding
[ ] demo reset/templates
[ ] industry packs
[ ] product usage analytics
[ ] customer health
[ ] support center
[ ] subscription/billing lifecycle
[ ] commercial audit
[ ] data export/cancellation
[ ] safe support access
```

Not all are required for the first paying customer, but they are important scale milestones.

---

# 225. Product/Growth Implementation Phases

## Phase G0 — Before Many Customers

Build only the essentials:

1. Canonical module/feature catalog.
2. Tenant module entitlements.
3. Industry-pack metadata framework.
4. One selected vertical pack.
5. Trial lifecycle fields.
6. Resettable demo template.
7. Guided onboarding checklist.
8. Structured prospect/trial records.
9. Product events for key workflows.
10. Data-driven plan definitions.

## Phase G1 — First 3 Customers

Focus on:

- One vertical end-to-end flow.
- Import templates.
- Onboarding.
- Support.
- Customer feedback.
- Demo reliability.
- Data quality.
- Business-outcome dashboard.

Do not build complex billing/marketplace yet.

## Phase G2 — Customers 4–10

Add:

- Customer health.
- Activation metrics.
- Feature request/lost-reason tracking.
- Plan/entitlement admin.
- Quote/subscription records.
- Customer-success playbooks.
- Release notes.
- Add-on trial framework.

## Phase G3 — Customers 10–30

Add when justified:

- Proper subscription billing.
- Automated provisioning.
- Support SLA.
- sandbox.
- configuration transport.
- stronger analytics.
- partner/reseller if demand exists.

## Phase G4 — Customers 30–100

- Self-service signup/conversion.
- Payment gateway.
- Integration catalog.
- Automated renewal.
- Public status page.
- Cancellation/export automation.
- Regional price books.
- Second/third vertical.

## Phase G5 — Scale

- Separate control plane if needed.
- Regional deployment/data residency.
- Enterprise SSO.
- Mature partner ecosystem.
- Advanced per-tenant observability.

---

# 226. Recommended First Vertical Document Structure

Once a vertical is chosen, create a dedicated file such as:

```text
docs/verticals/FOOD_DISTRIBUTION.md
```

or:

```text
docs/verticals/CITRUS_EXPORT.md
```

It should include:

1. Ideal customer profile.
2. Company size range.
3. Existing/manual alternatives.
4. Top business pains.
5. Business outcomes.
6. Required workflows.
7. Master data.
8. Reports.
9. KPIs.
10. Business documents.
11. Integrations.
12. Demo scenario.
13. Trial activation milestones.
14. Import templates.
15. Known gaps.
16. Explicit out-of-scope items.
17. Acceptance tests.
18. Plan/module mapping.

This prevents a vertical pack from becoming only a label.

---

# 227. Example Food Distribution MVP Definition

## Masters

- Customers.
- Customer groups.
- Routes/territories.
- Sales reps.
- Credit terms.
- Credit limits.
- Items/categories.
- Units.
- Warehouses.
- Suppliers.
- Price lists.

## Procurement

```text
PO → GRN → Supplier Invoice → Payment
```

## Inventory

- Batch.
- Expiry.
- Valuation.
- Transfer.
- Adjustment.
- Reorder.
- Low stock.
- Expiry risk.

## Sales

```text
Sales Order
→ Reservation
→ Delivery
→ Invoice
→ Receipt
```

## AR

- Customer balance.
- Aging.
- Receipt allocation.
- Credit control.

## Finance

- GL.
- Journals.
- Bank/cash.
- Fiscal periods.

## Smart Rules

- Below reorder.
- Potential stockout.
- Expiry risk.
- Overdue customer.
- Credit limit exceeded.
- Late PO.
- Supplier price variance.
- Unusual discount.
- Low-margin sale.

---

# 228. Example Citrus / Packing / Export MVP Definition

## Masters

- Farms/suppliers.
- varieties.
- grades.
- packing specifications.
- customers.
- warehouses/cold stores.
- production lines.
- worker categories.

## Receiving

```text
Supplier/Farm
→ Receiving Lot
→ Weight
→ Quality
→ Accepted / Rejected
```

## Processing

```text
Raw Lot
→ Washing / Sorting
→ Grade
→ Waste
→ Packing
→ Finished Lot
```

## Traceability

A finished carton should trace to:

- Packing run.
- Source lot.
- supplier/farm.
- receipt.
- quality results.

## Workforce

- Daily labor.
- biometric/manual attendance.
- cost by production line/run.
- contractor settlement.

## Export Later

- Customer order.
- packing list.
- container.
- shipment.
- export documents.
- invoice.
- collection.

## KPIs

- Yield.
- Waste.
- Rejected incoming %.
- Packed kg/hour.
- Labor cost/ton.
- Cost/carton.
- Margin/container.
- Quality claims.
- Supplier quality.

---

# 229. Example Contractor Workforce MVP Definition

## Masters

- Contractor.
- worker.
- category.
- site.
- department.
- wage rules.

## Flow

```text
Labor Request
→ Approval
→ Contractor Fulfillment
→ Assignment
→ Attendance
→ Exceptions
→ Advances / Deductions
→ Settlement
→ Contractor Invoice
→ Payment
```

## Smart Controls

- Contractor fill rate.
- Worker identity duplication.
- Attendance anomaly.
- Wage-rate deviation.
- Settlement mismatch.
- Contractor reliability.

This is likely one of the quickest Bemo verticals to reach strong maturity because much of the core lifecycle already exists.

---

# 230. Recommended Repository Documentation Structure

```text
docs/
  product/
    SAAS_CONTROL_PLANE.md
    MODULE_ENTITLEMENTS.md
    TRIALS.md
    SUBSCRIPTIONS.md
    PRODUCT_ANALYTICS.md
  verticals/
    FOOD_DISTRIBUTION.md
    CITRUS_EXPORT.md
    CONTRACTOR_WORKFORCE.md
    SMALL_MANUFACTURING.md
  onboarding/
    IMPLEMENTATION_GUIDE.md
    MIGRATION_GUIDE.md
  demos/
    FOOD_DISTRIBUTION_DEMO.md
    WORKFORCE_DEMO.md
```

This master README remains the long-term vision, but implementation-specific documents should be split out once engineering begins.

# 231. Recommended Next 12 Major Product Epics

A practical combined order for the current Bemo codebase is:

## Epic 1 — Inventory Valuation + AR Foundation

**Implementation status: DONE 2026-08-09.** P0.1 delivered FIFO/weighted-average valuation and Inventory GL. V164/V165 complete the AR foundation with customer invoices/receipts, retry-safe locked allocations and advances, aging, credit limits/holds enforced on order confirmation and invoice issue, partner-ledger balances, and basic overdue collection tasks. Evidence: backend 343/69 and frontend 241/39 with H2/error/i18n/hardcoded/build gates green.

Why: distribution/manufacturing customers need trustworthy inventory value, customer balances and margin before advanced forecasting is meaningful.

Deliver:

- FIFO/weighted-average valuation.
- inventory GL.
- customer invoices/receipts.
- aging.
- credit limits.
- basic collections.

## Epic 2 — Module Entitlement Catalog

**Implementation status: DONE 2026-08-09.** The former scattered defaults/prefix map is replaced by one `EntitlementCatalog` covering module ownership, effective defaults, dependencies and API prefixes. Existing tenant rows remain the durable entitlement store; V166 adds required change reason. Backend guards direct API access, rejects missing dependencies and active dependents, preserves historical data on downgrade, and audits before/after/reason. Super Admin settings manage versioned entitlements. V167 supplies bilingual UI/errors. Regression: backend 347/70, frontend 244/40, all gates green.

Why: modular commercial packaging becomes dangerous if implemented later as scattered menu checks.

Deliver:

- module registry.
- feature registry.
- tenant entitlements.
- backend enforcement.
- menu/route integration.
- audit.
- dependency tests.

## Epic 3 — First Industry Pack

**Implementation status: DONE 2026-08-10.** `CONTRACTOR_WORKFORCE_EG` is the first configuration-driven pack. V168/V169 provide global metadata, recorded tenant version, required entitlements, terminology/dashboard/KPI/role/import-template defaults and prerequisite-aware onboarding. Install and upgrade are idempotent/audited; optional steps can be skipped; upgrades preserve customized JSON. Super Admin settings expose install, go-live progress, customization and non-destructive update. Regression: backend 352/71, frontend 247/41, all gates green.

Choose one vertical.

Deliver:

- pack metadata/version.
- terminology.
- roles.
- settings.
- dashboards.
- KPIs.
- onboarding.
- import templates.
- demo scenario.

## Epic 4 — Trial + Demo Template

**Implementation status: DONE 2026-08-10.** V170 records exact trial start/end and paid conversion on the existing tenant row, defines versioned demo templates, and isolates template-owned samples. Expired trials remain readable while a central backend policy blocks ERP writes. Start, conversion and demo reset are operation-idempotent and audited; non-demo reset is rejected, reset cannot touch business aggregates, and conversion preserves tenant ID/data. V171 and the Super Admin settings tab provide bilingual lifecycle, template, sample and safe-reset controls. Regression: backend 361/73, frontend 250/42, error parity 314/314, all gates green.

Deliver:

- trial lifecycle.
- demo tenant flag.
- versioned seed/template.
- safe reset.
- sample-data handling.
- trial conversion.

## Epic 5 — Guided Onboarding + Data Quality

**Implementation status: DONE 2026-08-10.** V172 records immutable assessments under a pack lock. The evaluator reuses only the selected vertical's dependency-aware steps, auto-completes from real tenant contractor/category/worker/import/settlement evidence, requires successful import state, preserves explicit optional skips, and produces setup progress, data-quality score, blockers and go-live readiness. V173 and the Admin/Super Admin settings workspace expose localized steps and direct fix routes. Regression: backend 365/74, frontend 253/43, error parity 316/316, all gates green.

Deliver:

- checklists.
- dependencies.
- imports.
- setup progress.
- data quality score.
- go-live readiness.

## Epic 6 — Smart Action Center 2.0

**Implementation status: DONE 2026-08-10.** V174 extends the existing tenant/recipient notification stream rather than creating a second inbox. Cards carry exception key, localized business impact, reason, recommendation, optional amount/currency, role targets and a validated internal action. Backend ranking combines severity, unread state and decisive role relevance; the frontend renders the order and direct action. Legacy sends, ownership checks and audit remain intact. V175 provides bilingual card labels. Regression: backend 368/74, frontend 255/44, all gates green.

Deliver:

- exception-first cards.
- business impact.
- reason.
- recommendation.
- direct action links.
- role-based prioritization.

## Epic 7 — Supplier / Contractor Scores + Risk Rules

**Implementation status: DONE 2026-08-10.** V176/V177 provide tenant-versioned risk bands, immutable retry-safe score evidence, transparent supplier/contractor formulas, audit and a role-aware scorecard workspace with direct remediation. Regression: backend 372/75, frontend 258/45, all gates green.

Leverage existing data for visible intelligence without AI.

## Epic 8 — Bank Reconciliation + Cash Position

**Implementation status: DONE (delivered as ADV-P0.3 on 2026-08-09).** V158/V159 cover idempotent statement import, exact/partial/manual/fee matching, balanced fee journals, reversal, closed-period/locking controls and currency-separated cash position. Delivery regression: backend 327/67, frontend 231/37; it remains covered by the current 372/75 and 258/45 regressions.

High-value SMB finance capability.

## Epic 9 — Product Analytics + Activation

**Implementation status: DONE 2026-08-10.** V178/V179 implement privacy-allowlisted tenant events, durable aggregates, six activation milestones, non-rollback safe capture, raw retention, tenant summaries and explicit Super Admin platform analytics. Web navigation strips query/fragment data before capture. Regression: backend 377/76, frontend 261/47, all gates green.

Learn from actual customer behavior.

## Epic 10 — Subscription / Plan Control Plane

**Implementation status: DONE 2026-08-10.** V180/V181 implement data-driven plans, canonical feature membership, explicit user limits, versioned tenant lifecycle, retry-safe immutable change history, dependency-safe entitlement synchronization and downgrade/cancellation controls without deleting ERP data. The real user-create flow enforces active status and plan limits. Super Admin UI manages definitions, usage and history. Regression: backend 385/77, frontend 264/48, all gates green.

Move commercial management out of spreadsheets/manual database changes.

## Epic 11 — Support + Customer Health

**Implementation status: DONE 2026-08-10.** V182/V183 implement tenant/idempotent support tickets, critical-impact validation, deterministic SLAs, versioned state transitions with immutable updates, privacy-limited feedback and immutable customer-health evidence. The score is a transparent 100-point composition across seven dimensions and every reason has an action route. Global Help and Admin queue/health UI are regression verified at backend 390/78 and frontend 268/49.

Retention becomes an explicit product/process capability.

## Epic 12 — Second Vertical Pack

**Implementation status: DONE 2026-08-10.** V184 makes pack defaults, roles, KPIs, import templates and onboarding steps data-driven instead of contractor constants, then seeds `FOOD_DISTRIBUTION_EG` with dependency-closed modules, FEFO/credit/expiry defaults, four roles, nine KPIs, four templates and nine go-live steps. V185 localizes it. Generic UI and non-destructive customization/upgrade behavior are verified at backend 391/78 and frontend 269/49.

Only after the first vertical has real usage evidence and repeatable sales/onboarding.

---

# 232. Immediate Engineering Stories

These can start before building a large SaaS control plane.

```text
STORY-GROWTH-001  Canonical module/feature registry
STORY-GROWTH-002  Tenant module entitlements
STORY-GROWTH-003  Industry pack metadata/version
STORY-GROWTH-004  Trial dates/state
STORY-GROWTH-005  Generic onboarding checklist
STORY-GROWTH-006  Demo tenant flag and safe reset guard
STORY-GROWTH-007  Product event sink with privacy allowlist
STORY-GROWTH-008  Activation milestone engine
STORY-GROWTH-009  Platform tenant summary/list
STORY-GROWTH-010  Data-driven plan definitions
```

Each story must include:

- Backend contract.
- migrations.
- frontend.
- permissions.
- menu/access-catalog effects.
- i18n.
- audit.
- tests.
- tenant-isolation tests.

---

# 233. Acceptance Criteria — Modular SaaS

```text
Given tenant A has Inventory but not Finance
When user calls a Finance endpoint
Then backend returns a stable entitlement denial.

Given tenant B has Finance entitlement
And user has Finance permission
When user calls Finance endpoint
Then operation succeeds.

Given tenant has WMS without Inventory
When WMS activation is attempted
Then activation is rejected with dependency explanation.

Given a module is downgraded
Then historical business data is preserved.

Given a commercial entitlement is changed
Then immutable audit contains actor, reason, before and after.

Given a route/menu is hidden
Then direct API access remains protected by backend checks.
```

---

# 234. Acceptance Criteria — Industry Pack

```text
Given new tenant chooses FOOD_DISTRIBUTION_EG
Then required modules/defaults are installed.

And industry roles exist.
And pack translations exist.
And default KPIs exist.
And default dashboard exists.
And onboarding steps are created.
And pack version is recorded.

When pack install is retried
Then result remains idempotent.

When customer customized a workflow
And pack upgrade changes its template
Then customer workflow is not silently overwritten.
```

---

# 235. Acceptance Criteria — Trial and Demo

```text
Given 14-day trial
Then start/end are recorded consistently.

Given active trial
Then entitled writes work.

Given expired trial
Then commercial read-only policy applies.

Given paid conversion
Then tenant ID and business records remain unchanged.

Given demo reset request
When tenant is not marked demo
Then reset is rejected.

Given demo reset succeeds
Then template version and actor are audited.
```

---

# 236. Acceptance Criteria — Onboarding

```text
Given selected vertical
Then irrelevant setup steps are absent.

Given prerequisite incomplete
Then dependent step is BLOCKED.

Given successful import
Then related milestone can automatically complete.

Given optional step skipped
Then status is SKIPPED, not falsely COMPLETED.

Given all go-live blockers resolved
Then readiness becomes READY.
```

---

# 237. Acceptance Criteria — Product Analytics

```text
Product event failure never rolls back an ERP transaction.

Sensitive fields are not accepted in generic analytics payload.

Tenant-visible analytics only returns current tenant.

Platform analytics requires explicit permission.

Usage events can be retained/aggregated according policy.
```

---

# 238. What Not to Build Yet

The commercial ideas should not trigger overengineering before customer evidence exists.

Do **not** prioritize these now:

- Complex affiliate network.
- 20 pricing plans.
- 20 vertical packs.
- Huge marketing automation platform.
- Full Salesforce-class CRM.
- Advanced usage-based invoicing.
- Third-party plugin execution marketplace.
- Mobile app for every role.
- Global multi-region deployment.
- Paid generative-AI dependency.
- Complex pricing experiments.

Build the architecture seams and implement only when real demand appears.

---

# 239. Combined Master Roadmap Tracks

The Bemo roadmap should now be managed in six parallel tracks.

## Track A — ERP Core Maturity

- Inventory valuation.
- AR/collections.
- bank reconciliation.
- fixed assets.
- advanced procurement.
- manufacturing/MRP.
- payroll/time.
- multi-company.
- projects.
- contracts.

## Track B — No-AI Intelligence

- Smart Action Center.
- scoring.
- risk rules.
- anomaly detection.
- forecasts.
- recommendation framework.
- explainability.
- simulations.
- exception-first workbenches.

## Track C — SaaS Productization

- module entitlements.
- plan catalog.
- trials.
- provisioning.
- subscriptions.
- usage limits.
- support-access governance.
- cancellation/export.

## Track D — Verticalization

- industry packs.
- terminology.
- workflows.
- KPIs.
- dashboards.
- import packs.
- demo stories.
- sector help.

## Track E — Growth / Customer Success

- activation milestones.
- product analytics.
- customer health.
- feedback.
- CRM.
- support.
- renewal.
- expansion signals.

## Track F — Optional AI

Only when customer value clearly exceeds model/API operating cost.

---

# 240. Final Product Principles from the Commercial Strategy

## Principle 1 — Niche Outside, Platform Inside

Externally:

> ERP for citrus packing/export companies.

Internally:

```text
Bemo Platform
+ Egypt Country Pack
+ Citrus Export Pack
+ Purchased Modules
```

Focused marketing does not require a forked product.

## Principle 2 — Sell Outcomes, Build Evidence

If marketing says:

> Know your stock, sales, profit and customer debt from one place.

Bemo must support that claim through:

- traceable calculations.
- drill-down.
- correct valuation/accounting basis.
- data quality.
- alerts.
- dashboards.
- reports.

## Principle 3 — Simple First, Powerful Underneath

SMB users can see:

```text
Sell
Buy
Stock
Collect
Pay
Reports
```

while Bemo internally enforces:

- state machines.
- audit.
- tenant security.
- valuation.
- fiscal periods.
- approvals.
- accounting controls.

## Principle 4 — Modular Growth Without Replacement

A customer should start small and add modules without migrating to another ERP.

## Principle 5 — Trial Must Show Value Quickly

A 14-day trial must not require 13 days of setup.

Target:

> First meaningful business outcome during the first session.

## Principle 6 — The First Customers Are Product Research

The platform should help answer:

- Why did customers buy?
- Why did prospects reject?
- Which workflows are actually used?
- Which feature creates support burden?
- Which feature drives retention?
- What repeated configuration should become a pack/default?

## Principle 7 — Intelligence Does Not Require Paid AI

Use deterministic rules, scoring, forecasting, exception management, recommendations and explainability first.

Optional generative AI remains a future add-on, BYOK/local-provider friendly, and never a dependency for core ERP correctness.

## Principle 8 — Do Not Wait for the Entire ERP

The strongest near-term product strategy is:

```text
Choose one vertical
→ perfect one workflow
→ create a reliable demo
→ make imports/onboarding excellent
→ get real customers
→ measure usage/problems
→ improve the vertical
→ then expand modules/verticals
```

This turns the marketing ideas into a sustainable product architecture instead of treating them as a separate sales document.
