# Bemo ERP — Advanced Features & Intelligent ERP Roadmap

> **Repository reviewed:** `Mohamed-Hammada/bortqala`  
> **Branch:** `fm_bemo_consolidated`  
> **Snapshot used for this document:** `2f377d96d9a0cde1aaf3a8f361d0cef2b731eb92` (2026-08-09)  
> **Primary codebase:** Spring Boot backend (`be/`) + Angular frontend (`fe/`) + PostgreSQL  
> **Purpose:** Turn Bemo from a feature-rich ERP into an advanced, automation-first, explainable, scalable ERP **without making paid AI subscriptions a requirement**.

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

**Implementation status: DONE 2026-08-09.** V154/V155, `InventoryValuationService`, procurement-GRN unit-cost integration, FIFO/weighted-average movement evidence, fiscal/backdating controls, balanced Inventory GL journals, idempotent revaluation, Angular settings/report/drill-down, and Excel sheets are implemented. Evidence: backend 312 tests / 65 suites / 0 failures; frontend 223 tests / 35 files / 0 failures; bilingual/error-code gates green.

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

**Implementation status: DONE 2026-08-09.** V156/V157 add the supplier lifecycle, real compliance-file evidence, duplicate tax/IBAN controls, bank verification, optional shared-approval integration, Supplier 360, and procurement/payment gates. Evidence: backend 320 tests / 66 suites / 0 failures; frontend 227 tests / 36 files / 0 failures; H2 migration, bilingual/error-code, hardcoded-string, and production-build gates green.

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

**Implementation status: DONE 2026-08-09.** V158/V159 add idempotent balance-validated CSV statements, versioned lines and reversible journal-linked match evidence. The engine supports exact/partial/manual matching, balanced bank-fee journals, suggestions, fiscal-period guards, aggregate locking, and currency-separated cash position. The banks UI provides import, workbench, reversal, and cash tabs. Evidence: backend 327/67 and frontend 231/37 with all migration, i18n, hardcoded-string, and production-build gates green.

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

**Implementation status: DONE 2026-08-09.** V160/V161 add immutable versioned workflow/document snapshots, dated scoped delegation, audited administrator reassignment, locked ANY_N decisions, per-step SLA, tenant-aware one-shot escalation, and safe upgrade backfill for existing instances and decisions. The approval workspace adds total/overdue/delegated summaries, aging filters, signature progress, delegated history, and delegation settings. Evidence: backend 332/67, frontend 236/38, H2 migration, error-code, i18n, hardcoded-string, and build gates green.

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

**Implementation status: DONE 2026-08-09.** V162/V163 deliver effective-dated tenant/category/employee policy hierarchy, deterministic anomaly scoring, immutable policy snapshots, exception aggregation, idempotent bulk preview/apply, cross-midnight punch assignment, approval blocking, and employee/all-report payroll gates. The report-review workspace provides summary/filter/explanation, policy evidence, selection, preview-without-mutation, and controlled resolution. Evidence: backend 338 tests / 68 suites / 0 failures; frontend 238 tests / 38 files / 0 failures; H2 migration, 284/284 error-code parity, 2024-key i18n, hardcoded-UI and production-build gates green.

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
