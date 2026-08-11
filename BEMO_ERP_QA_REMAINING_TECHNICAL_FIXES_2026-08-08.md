# Bemo ERP — Remaining QA Technical Fixes

**Review date:** 2026-08-08  
**Repository:** `Mohamed-Hammada/bortqala`  
**Branch reviewed:** `fm_bemo_consolidated`  
**QA baseline SHA provided:** `1d8ea2ec71f489b95bebe6f72a486754e9a1654a`  
**Current branch head reviewed:** `4e8c6c2a7c7ca04808c1e93b62af0aa408659418` (`fix qa`)  
**Source QA report:** `BEMO_ERP_REMAINING_ISSUES_DETAILED_2026-08-08(1).md`

## Executive Result

The QA report is **not fully resolved** at the current branch head.

### Code-level status

| QA ID | Result | Notes |
|---|---|---|
| REM-001 | **Appears fixed in code** | Decision save now blocks duplicate submission, keeps error UI open, verifies persistence using a fresh GET, shows success/error feedback, and records an audit event. Runtime retest is still recommended. |
| REM-002 | **Not fixed** | Employee categories and workforce categories are still separate backend models/repositories. There is no authoritative `EMPLOYEE / WORKER / BOTH` scope model. |
| REM-003 | **Partially fixed** | Contractor, worker, and employee CREATE auditing was added, but the broader audit acceptance criteria are incomplete: token-refresh noise, search/filtering, and full mutation coverage remain. |
| REM-004 | **Partially fixed / migration needs correction** | A data migration was added, but it has unsafe rollback behavior and a conflict-path inconsistency for `daily_results`. It also does not produce the required audit correction record/dry-run output. |
| REM-005 | **Partially fixed** | `documentType` and `reason` were added, but the design still has one generic `referenceCode`; separate PO/receipt/invoice/delivery/voucher references and attachment/link validation are still absent. |
| REM-006 | **Mostly fixed, UX acceptance incomplete** | Preset cards now fill the report form instead of creating a report immediately. However, each card still lacks a visible action verb such as `استخدام هذه الفترة`, so the strict UX acceptance criterion is not fully met. |
| REM-007 | **Appears fixed in code** | Audit pagination now tracks server `totalElements` and passes the actual current page to the shared pagination component, which computes the correct range. Runtime retest is recommended. |
| REM-008 | **Partially fixed** | A table layout, add/save/reset actions, duplicate prevention, and feedback exist, but every row still exposes the target selector and there is no search/filter workflow or explicit edit mode. |

---

# Remaining Technical Work

## REM-002 — Unify Category Scope Model

### Current technical problem

There are still two independent category models:

- Employee attendance category:
  - `be/src/main/java/com/bemo/hr/employee/domain/AttendanceCategory.java`
  - used by `HrConfigurationService`
  - repository: `AttendanceCategoryRepository`
- Workforce category:
  - `be/src/main/java/com/bemo/hr/workforce/WorkerCategory.java`
  - repository: `WorkerCategoryRepository`
  - service: `WorkerCategoryService`

`WorkerCategory` currently has fields such as code, name, daily rate, standard hours, settlement cycle, and status, but **no scope field**.

`HrConfigurationService.listCategories()` reads only from `AttendanceCategoryRepository`.

Therefore a workforce category cannot become an employee category merely by choosing `Both`; the two contexts do not share one authoritative category record.

### Required architecture

Use one canonical category identity with a scope enum:

```java
public enum CategoryScope {
    EMPLOYEE,
    WORKER,
    BOTH
}
```

Recommended structure:

1. Keep shared fields in a canonical category table/entity:
   - `id`
   - `app_id`
   - `code`
   - `name`
   - `scope`
   - `active`
   - common reporting fields

2. Store employee-specific settings separately if needed:
   - pay cycle
   - attendance mode
   - workday rules
   - schedule rules
   - advance eligibility

3. Store workforce-specific settings separately if needed:
   - default daily rate
   - standard daily hours
   - default settlement cycle

A practical schema is:

```text
categories
  id
  app_id
  code
  name
  scope
  active

employee_category_config
  category_id
  expected_daily_minutes
  pay_cycle
  attendance_mode
  ...

workforce_category_config
  category_id
  default_daily_rate
  standard_daily_hours
  default_settlement_cycle
  ...
```

### Backend changes

Update employee category queries so they return:

```text
scope IN (EMPLOYEE, BOTH)
```

Update workforce category queries so they return:

```text
scope IN (WORKER, BOTH)
```

Do not maintain two unrelated records for a `BOTH` category.

Update:

- employee creation category selector endpoint
- worker creation category selector endpoint
- attendance filters
- workforce dashboard filters
- user permission/category selector
- report grouping
- import validation

### Migration

Create a controlled migration that:

1. Maps existing `attendance_categories` and `worker_categories`.
2. Detects duplicate code/name pairs.
3. Produces a mapping table/report before changing foreign keys.
4. Creates canonical category IDs.
5. Updates:
   - employees
   - workers
   - schedules
   - attendance/report configuration
   - user-category permission references
6. Keeps an audit record of category merges.
7. Verifies no orphan references exist.

### Required tests

Add integration tests for:

- `EMPLOYEE` visible only to employee context.
- `WORKER` visible only to workforce context.
- `BOTH` visible in both.
- Editing shared fields is reflected everywhere.
- Employee and worker creation can both use the same `BOTH` ID.
- Reports group the shared category correctly.
- User category selector returns the canonical category once.

---

## REM-003 — Finish Audit Trail Coverage and Remove Session Noise

### What is already implemented

The current head adds audit writes for:

- employee CREATE in `HrConfigurationService`
- contractor CREATE/UPDATE in `ContractorService`
- worker CREATE/UPDATE in `WorkerService`
- attendance daily decision in `ReportingService`

This resolves an important part of the original defect.

### What is still missing

The QA acceptance criteria also require:

- token refresh events not dominating the business audit page
- searchable/filterable audit events
- reliable mutation coverage, not only selected CREATE/UPDATE paths
- meaningful business details
- failed transactions not producing successful business events

The current `/audit-logs` frontend remains a simple paginated table with no filter/search controls.

### Required backend work

Introduce a consistent audit publishing abstraction instead of adding manual string JSON in individual services.

Example:

```java
auditPublisher.businessEvent(
    AuditEvent.builder()
        .action(AuditAction.CREATE)
        .entityType(AuditEntityType.EMPLOYEE)
        .entityId(employee.getId())
        .businessCode(employee.getEmployeeCode())
        .actor(currentActor())
        .details(details)
        .build()
);
```

Prefer structured JSON serialization rather than manually concatenating JSON strings.

Ensure audit persistence is transactionally correct:

- If the audit row must roll back with the business transaction, save it in the same transaction.
- If audit must be guaranteed after commit, use an outbox or `@TransactionalEventListener(AFTER_COMMIT)` plus reliable delivery.

### Complete mutation coverage

At minimum add audit coverage for:

- employee CREATE / UPDATE / DEACTIVATE
- contractor CREATE / UPDATE / status change
- worker CREATE / UPDATE / status change
- category CREATE / UPDATE / DEACTIVATE
- attendance decision / reversal
- payroll approval/payment
- settlement approval/payment
- inventory adjustment/financial-impact operations

### Token refresh noise

Locate the authentication refresh flow that writes `USER TOKEN_REFRESH`.

Recommended options:

1. Do not store refresh events in the main business audit table.
2. Store them in a separate security/session log.
3. Or classify them as `SECURITY_SESSION` and exclude them from the default business audit query.

The normal audit screen should default to business events.

### Add backend audit filters

Extend the audit API with parameters similar to:

```text
GET /api/v1/audit-logs
  ?page=0
  &size=25
  &entityType=EMPLOYEE
  &action=CREATE
  &username=...
  &search=QA-EMP-RETEST-0808
  &from=...
  &to=...
```

Add appropriate DB indexes, especially:

```text
(app_id, occurred_at)
(app_id, entity_type, occurred_at)
(app_id, action, occurred_at)
(app_id, username, occurred_at)
```

### Frontend work

In:

`fe/src/app/features/audit-logs/`

add:

- entity type filter
- action filter
- username filter
- date range
- business code / details search
- clear filters
- loading/error states

### Required tests

- exactly one successful CREATE event per created employee/worker/contractor
- failed CREATE does not leave a successful audit event
- token refresh does not appear in default business audit results
- code search finds `QA-EMP-*`, `QA-WRK-*`, and `QA-CTR-*`
- update/deactivate actions are visible
- audit rows are immutable through normal APIs

---

## REM-004 — Correct Employee Code Migration Before Production

### Current migration

File:

`be/src/main/resources/db/changelog/data/update/20260808_v131_employee_code_deduplication.yaml`

The migration correctly recognizes patterns such as:

```text
QA-EMP-0807-QA-EMP-0807
```

and attempts to normalize them.

However, the migration should **not be accepted as production-safe yet**.

### Problem 1 — Unsafe rollback

The rollback currently applies this logic broadly:

```sql
employee_code = employee_code || '-' || employee_code
```

to rows matching a general hyphen pattern.

That can duplicate codes for employees that were never modified by this migration.

#### Required fix

Do not use a broad pattern-based rollback.

Before updating rows, create a correction mapping containing:

```text
employee_id
app_id
old_code
new_code
```

Then use that exact mapping for both forward update and rollback.

Example approach:

```sql
CREATE TABLE employee_code_correction_20260808 AS
SELECT id, app_id, employee_code AS old_code, ... AS new_code
FROM employees
WHERE <exact duplicated-code predicate>;
```

Then:

```sql
UPDATE employees e
SET employee_code = m.new_code
FROM employee_code_correction_20260808 m
WHERE e.id = m.employee_id;
```

Rollback must restore only:

```sql
m.old_code
```

for the exact recorded employee IDs.

### Problem 2 — Conflict path can desynchronize report snapshots

When the canonical code is already occupied, the employee row is changed to:

```text
<canonical>-<id suffix>
```

but `daily_results` is still normalized only to:

```text
<canonical>
```

This can make an employee display one code in `employees` and a different code in historical reports.

#### Required fix

Update snapshot tables by `employee_id` using the exact `new_code` from the correction mapping:

```sql
UPDATE daily_results d
SET employee_code = m.new_code
FROM employee_code_correction_20260808 m
WHERE d.employee_id = m.employee_id;
```

Do the same for every denormalized employee-code snapshot table.

### Problem 3 — No audit correction event

The QA acceptance requires the old and new code to be traceable.

After migration, write an immutable audit/migration record containing:

```text
employee_id
old_code
new_code
migration_id
timestamp
```

### Problem 4 — No dry-run report

Provide a pre-migration query/script that returns:

- employee ID
- tenant/app ID
- old code
- proposed new code
- conflict status
- affected downstream row counts

The production deployment should require review of this output before applying the write migration.

### Uniqueness protection

Verify a tenant-aware case-insensitive uniqueness rule exists.

If not, add one appropriate to the database, for example a unique index conceptually equivalent to:

```text
(app_id, lower(employee_code))
```

### Required tests

- normal duplicated code becomes canonical once
- conflict case generates deterministic unique code
- `daily_results` matches the employee’s actual post-migration code
- unrelated valid employee codes are untouched
- rollback restores only affected records
- advance/report relationships remain valid
- audit/migration log contains old/new code

---

## REM-005 — Implement Real Business Document References

### Current partial implementation

The current patch adds to transaction data:

```text
documentType
reason
```

and the UI now includes a document type selector.

However, the API still has only one generic:

```text
referenceCode
```

This does not meet the original requirement for separate business document identities.

### Recommended data model

Prefer a normalized child table rather than adding many nullable columns to `stock_movements`.

Example:

```text
stock_movement_documents
  id
  app_id
  stock_movement_id
  document_type
  reference_number
  linked_entity_type
  linked_entity_id
  issuer_party_id
  attachment_id
  created_at
```

Supported document types:

```text
PURCHASE_REQUEST
PURCHASE_ORDER
GOODS_RECEIPT
SUPPLIER_DELIVERY_NOTE
SUPPLIER_INVOICE
SALES_ORDER
DELIVERY_NOTE
CUSTOMER_INVOICE
ADJUSTMENT_VOUCHER
INTERNAL_VOUCHER
EXTERNAL_REFERENCE
```

### API change

Instead of:

```java
String referenceCode,
String documentType
```

support:

```java
List<DocumentReferenceRequest> documents
```

For example:

```java
public record DocumentReferenceRequest(
    String documentType,
    String referenceNumber,
    String linkedEntityId,
    String attachmentId
) {}
```

### Operation-specific validation

For `SUPPLY_RECEIPT`, allow/require as applicable:

- supplier
- purchase order
- goods receipt number
- supplier delivery note
- supplier invoice
- warehouse
- attachment

For sales/delivery:

- sales order
- delivery note
- customer invoice
- warehouse
- attachment

For adjustment:

- adjustment voucher
- reason code
- approval reference
- attachment/evidence

### Duplicate supplier invoice validation

Add a tenant-aware rule similar to:

```text
UNIQUE(app_id, supplier_id, supplier_invoice_number)
```

or enforce it in the normalized document-reference table for `SUPPLIER_INVOICE`.

### Frontend

Change the transaction form dynamically based on `operationType`.

Do not show one generic "reference" field as the only identity.

The movement table/details should display:

- document type
- document number
- linked PO/receipt/invoice
- clickable internal document link when available
- attachment indicator

### Migration

Existing movements should remain readable.

Convert the old generic `referenceCode` into:

```text
document_type = LEGACY_REFERENCE
reference_number = existing referenceCode
```

when no more precise type can be inferred.

---

## REM-006 — Finish Report Preset UX

### What is fixed

The current reports page uses preset cards only to populate:

- start date
- end date
- pay cycle

The user must still press Preview or Create.

This removes the dangerous automatic report-creation side effect.

### Remaining UX gap

Each card still visually shows only:

- period name
- cycle
- date range

The card itself does not contain a clear action verb.

### Required change

Rename the frontend method:

```ts
create(period)
```

to something explicit such as:

```ts
applyPreset(period)
```

Then add visible card text such as:

```text
استخدام هذه الفترة
```

and an accessible label such as:

```text
استخدام هذه الفترة: أغسطس — من 16 إلى نهاية الشهر
```

Example:

```html
<button
  type="button"
  (click)="applyPreset(period)"
  [attr.aria-label]="i18n.t('reports.usePresetAria', { period: periodName(period) })"
>
  <strong>{{ periodName(period) }}</strong>
  <small>{{ cycleLabel(period) }}</small>
  <span class="preset-action">{{ i18n.t('reports.useThisPeriod') }}</span>
</button>
```

Also ensure the global preset hint explicitly says that clicking a preset **only fills the form** and does not create a report.

### Tests

- clicking preset performs no POST request
- fields are populated
- Preview still requires explicit click
- Create still requires explicit click
- accessible name explains the action

---

## REM-008 — Complete Shortcut Settings Edit UX

### Current state

The current implementation already has several improvements:

- a table layout
- Add button
- Save button
- Reset button
- remove action
- duplicate shortcut prevention
- duplicate destination prevention
- enabled switch
- feedback and accessibility announcements

However, every row still renders the full destination `<select>` immediately.

There is also no search/filter for a long shortcut list.

### Required UI change

Add an explicit row edit mode.

State example:

```ts
readonly editingClientId = signal<string | null>(null);
readonly search = signal('');
```

Default row:

```text
G → D | Dashboard | Enabled | Edit | Delete
```

Only the edited row should show:

- key capture control
- destination selector
- enabled control
- save/cancel edit actions

All other rows remain static and compact.

### Search

Add a search box that filters by:

- shortcut key
- destination title
- page code

Example computed value:

```ts
readonly filteredDrafts = computed(() => {
  const q = this.search().trim().toLowerCase();
  if (!q) return this.drafts();

  return this.drafts().filter(d =>
    this.displayKey(d.secondKeyCode).toLowerCase().includes(q) ||
    d.pageCode.toLowerCase().includes(q) ||
    this.getDestinationTitle(d.pageCode).toLowerCase().includes(q)
  );
});
```

### Preserve current protections

Keep:

- duplicate key prevention
- duplicate destination prevention
- invalid key validation
- save/reset feedback
- keyboard support
- live-region announcements

### Add tests

- default table contains no open selector except edited row
- Edit opens only one row
- Cancel restores the original draft
- Delete gives explicit feedback
- Search filters by key and target name
- keyboard-only edit/save works
- selected target remains visible while not editing

---

# Code-Level Items That Do Not Need New Fix Work

## REM-001 — Attendance Decision Persistence

The current implementation has the expected defensive behavior:

- prevents a second submit while saving
- keeps the prompt open on failure
- shows the error
- refreshes the report after the PUT
- verifies that the saved decision is present in the fresh GET
- only closes the prompt after verification
- shows success feedback
- records the attendance decision in the audit service
- has frontend tests covering failure, duplicate submit protection, and fresh-GET persistence confirmation
- has backend tests for decision state and audit publishing

### Still required

Perform a runtime retest against the deployed backend/database because unit/frontend tests cannot prove the real deployment schema and transaction state are correct.

---

## REM-007 — Audit Pagination

The current implementation now:

- stores the server `totalElements`
- sends the requested 0-based page to the API
- updates pagination state after the response
- passes the current page + total count to the shared pagination component

The shared component computes:

```text
from = (page - 1) * pageSize + 1
to   = min(total, page * pageSize)
```

This should produce:

```text
Page 1: 1–25
Page 2: 26–50
```

for a page size of 25.

### Still required

Runtime click-through retest for:

- next
- previous
- page-size changes
- final partial page
- refresh/reset behavior

---

# Recommended Fix Order

1. **REM-002** — category model integrity / release blocker
2. **REM-003** — audit completeness / compliance
3. **REM-004** — make data migration safe before production
4. **REM-005** — procurement/accounting document traceability
5. **REM-008** — finish shortcut settings UX
6. **REM-006** — finish preset action wording/accessibility

Then run runtime regression tests for **REM-001** and **REM-007**.

---

# Final Retest Checklist

- [ ] Create a `BOTH` category and use the same category ID for an employee and a worker.
- [ ] Confirm `EMPLOYEE`, `WORKER`, and `BOTH` filtering is correct in every selector.
- [ ] Create/update/deactivate employee, contractor, and worker records and verify audit events.
- [ ] Confirm token refresh events do not dominate the default business audit page.
- [ ] Search audit by entity/action/business code.
- [ ] Run the employee-code migration dry-run and review every affected row.
- [ ] Apply migration and verify employee/report/advance references remain consistent.
- [ ] Verify migration rollback affects only migration-touched employee IDs.
- [ ] Record a supply receipt with distinct PO, receipt, delivery-note, and invoice references.
- [ ] Verify duplicate supplier invoice validation.
- [ ] Verify linked document type/number is visible from inventory movement.
- [ ] Click a report preset and confirm it only fills the form.
- [ ] Confirm every report preset has a clear visible/accessibility action label.
- [ ] Search/edit/delete shortcut rows using the compact edit-mode UI.
- [ ] Save `يوم عادي` attendance decision, reload, and verify it persists.
- [ ] Verify attendance decision audit event.
- [ ] Navigate audit page 1 → 2 → 3 and verify range/page indicators.
