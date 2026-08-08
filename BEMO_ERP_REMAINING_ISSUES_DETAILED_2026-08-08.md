# Bemo ERP — Detailed Remaining Issues Only

**Report date:** 2026-08-08  
**Tested deployment:** `https://combinations-tissue-purpose-mate.trycloudflare.com`  
**Account used:** `مدير النظام الشامل (Super Admin)`  
**Purpose:** This report intentionally contains **only issues that remain after the latest retest**. Fixed issues are excluded.

---

## 1. Remaining-Issue Summary

| ID | Severity | Screen | Action that fails or needs improvement | Status |
|---|---|---|---|---|
| REM-001 | High | Attendance report review | Save `يوم عادي` HR decision | Not fixed |
| REM-002 | High | Workforce/employee categories | Use a category scoped `Both` in employee categories | Not fixed |
| REM-003 | High | Audit logs | Audit contractor, worker, and employee creation | Not fixed |
| REM-004 | Medium | Employees and dependent records | Correct the previously duplicated employee code | Migration required |
| REM-005 | Medium | Inventory movement | Record separate PO, receipt, invoice, and document references | Not fixed |
| REM-006 | Medium | Reports and periods | Understand what a period shortcut will do before clicking | Not fixed |
| REM-007 | Medium | Audit logs | Move between audit pages and see the correct range/page | Not fixed |
| REM-008 | Low/Medium UX | Settings shortcuts | Efficiently review and edit shortcut rows | Needs UI improvement |

### Release blockers

The following must be fixed before production acceptance:

1. REM-001 — attendance decisions are not persisted.
2. REM-002 — category scope behavior is inconsistent.
3. REM-003 — sensitive master-data creation is missing from the audit trail.

---

# REM-001 — Attendance Report HR Decision Does Not Persist

## Classification

| Field | Value |
|---|---|
| Severity | **High** |
| Priority | P1 — fix before production |
| Business area | Employee attendance and HR review |
| Screen name | `التقارير` → attendance report review |
| Direct route | `/reports/4c2c55df-3a5f-4da0-b4db-92b210d4a63e` |
| Affected action | `يوم عادي` → enter calculated minutes → `تأكيد` |
| Reproducibility | Reproduced consistently before and after the latest deployment |

## Where to find it

1. From the sidebar, open **متابعة والدوام**.
2. Open **التقارير**.
3. Open the report for **2026-08-01 to 2026-08-15**.
4. Locate any unresolved row showing `إدخال يدوي` and `مطلوب تأكيد الحضور يدويًا`.

## Exact test state

Before the action, the report showed:

- Review progress: `22% (2 / 9)`
- Pending decisions: `7`
- Unresolved filter count: `7`
- Seven rows with the `يوم عادي` action

## Exact reproduction steps

1. Open the report listed above.
2. Confirm that the first unresolved row belongs to `QA موظف اختبار شامل`.
3. In the `قرار HR` column, click **يوم عادي**.
4. The dialog/confirmation panel shows `الدقائق المحتسبة لليوم`.
5. Keep the value at **480 minutes**.
6. Click **تأكيد**.
7. Wait for the save request to complete.
8. Check the progress and unresolved counters.
9. Reload the browser page.
10. Check the same row and counters again.

## Actual result

- The confirmation UI closes, giving the impression that the decision was accepted.
- Review progress remains `22% (2 / 9)`.
- Pending decisions remain `7`.
- The unresolved filter remains `7`.
- The same row still displays `يوم عادي`, `خصم`, and `إجازة` as unresolved actions.
- After a hard reload, the decision is still absent.
- No clear error toast or inline error is displayed.
- No attendance-decision event appears in the reviewed audit records.

## Expected result

Immediately after saving:

- The row should display the selected HR decision.
- The calculated value should become 480 minutes.
- Review progress should increase from 2/9 to 3/9.
- Pending and unresolved counts should decrease from 7 to 6.
- The row should move out of the unresolved filter.
- A specific success toast should say that the employee/day decision was saved.

After reload:

- The same decision must still be present.
- The progress and counters must remain updated.
- An audit event must record the action.

## Business impact

- HR cannot complete attendance review.
- Attendance reports cannot reach an approved state safely.
- Payroll may use unresolved or incorrect attendance.
- Manual attendance corrections can be lost without warning.
- Users may repeat the action because the UI appears to accept it.
- There is no reliable evidence of who approved the day or what value was entered.

## Likely technical area to inspect

- Report-review component save handler
- HR decision API endpoint and payload
- Employee/report-row identifier mapping
- Handling of manually scheduled rows with zero fingerprints
- Observable/Promise subscription and error branch
- State refresh after save
- API transaction and audit-event publication
- Legacy employee code/reference handling for `QA-EMP-0807-QA-EMP-0807`

## Required UI behavior

While saving:

- Disable `تأكيد` to prevent duplicate submissions.
- Show a spinner or `جارٍ الحفظ` state.
- Keep the entered 480-minute value in memory.

On success:

- Close the confirmation UI.
- Refresh the row and summary counters from the authoritative server response.
- Show `تم حفظ قرار الحضور بنجاح`.

On failure:

- Keep the dialog open.
- Preserve the entered value.
- Display an Arabic error explaining that the decision was not saved.
- Provide a `إعادة المحاولة` action.
- Do not update the row optimistically unless the server confirms success.

## Acceptance criteria

- [ ] Saving `يوم عادي` returns a successful server response.
- [ ] The selected row shows the saved decision immediately.
- [ ] Review progress changes from 2/9 to 3/9.
- [ ] Unresolved count changes from 7 to 6.
- [ ] Reloading the report preserves the decision.
- [ ] The decision appears in the audit trail.
- [ ] A failed API request keeps the dialog open and shows an Arabic retry message.
- [ ] `تأكيد` cannot be clicked twice while saving.
- [ ] The same behavior works for `خصم` and `إجازة`.
- [ ] Bulk decisions and individual decisions update the same counters consistently.

---

# REM-002 — Category Scope “Both” Is Still Inconsistent

## Classification

| Field | Value |
|---|---|
| Severity | **High** |
| Priority | P1 — business-model decision required |
| Business area | Workforce and employee configuration |
| Screen 1 | `فئات العمال` |
| Screen 1 route | `/workforce/categories` |
| Screen 2 | `الفئات والقواعد` |
| Screen 2 route | `/categories` |
| Affected action | Create/use a category with scope `Both` |

## Existing test record

| Field | Value |
|---|---|
| Code | `QA-CAT-0807` |
| Name | `QA فئة عمال اختبار` |
| Scope | `Both` |
| Daily rate | 350 EGP |
| Standard hours | 8 |

## Where the category currently appears

The category appears in:

- `/workforce/categories`
- worker creation
- workforce attendance filters
- workforce dashboard filters
- user category selection

## Where it is missing

The same category does **not** appear in:

- `/categories` (`الفئات والقواعد`)
- employee category listing
- employee creation category list

Only the separate category `QA فئة موظفين اختبار` appears in the employee category screen.

## Reproduction steps

1. Open **العمالة والمقاولون** → **فئات العمال**.
2. Find `QA فئة عمال اختبار`.
3. Confirm that its scope is `Both`.
4. Open **متابعة والدوام** → **الفئات والقواعد**.
5. Search for the same code or name.
6. Open the employee creation form and inspect the category selector.

## Actual result

- The `Both` category is limited to workforce contexts.
- A separate employee category is required.
- The UI and data model suggest unification, but behavior remains separated.

## Expected result

One of these two models must be implemented consistently.

### Option A — Unified category model

- `Worker` scope appears only in workforce contexts.
- `Employee` scope appears only in employee contexts.
- `Both` scope appears in both contexts.
- Shared fields remain synchronized.
- Context-specific fields are clearly separated.

### Option B — Separate category models

- Remove the `Both` option.
- Rename the category types to make separation explicit.
- Do not expose workforce categories in user/employee selectors.
- Provide a controlled mapping if reporting needs cross-category grouping.

## Business impact

- Administrators must duplicate categories.
- Attendance schedules and settlement cycles can diverge.
- Daily-rate and employee salary rules may be applied to the wrong category.
- Reporting by category becomes unreliable.
- User-category permissions may point to a category that employee setup cannot use.
- Future changes require updates in multiple places.

## Likely technical area to inspect

- Category entity/schema and scope enum
- Employee-category repository queries
- Workforce-category repository queries
- API filters that exclude workforce-origin categories
- Frontend selectors and list endpoints
- User-category selector aggregation
- Migration strategy for existing duplicated categories

## Acceptance criteria for a unified model

- [ ] A newly created `Both` category appears in both category screens.
- [ ] The category appears in both employee and worker creation selectors.
- [ ] Editing shared values from one screen is reflected in the other.
- [ ] Employee-only and worker-only categories remain correctly filtered.
- [ ] Reports correctly group employees and workers using the shared category.
- [ ] User creation displays the same authoritative category record once.
- [ ] API and UI tests cover `WORKER`, `EMPLOYEE`, and `BOTH` scopes.

---

# REM-003 — Audit Trail Does Not Record New Contractor, Worker, and Employee Creates

## Classification

| Field | Value |
|---|---|
| Severity | **High** |
| Priority | P1 — compliance and traceability |
| Business area | Security, audit, master data |
| Screen name | `سجل التدقيق والتتبع` |
| Direct route | `/audit-logs` |
| Affected action | Review audit events after successful master-data creation |

## Successfully created records missing from reviewed audit data

| Entity | Code | Name |
|---|---|---|
| Contractor | `QA-CTR-RETEST-0808` | `QA مقاول إعادة اختبار` |
| Worker | `QA-WRK-RETEST-0808` | `QA عامل إعادة اختبار` |
| Employee | `QA-EMP-RETEST-0808` | `QA موظف إعادة اختبار` |

## Reproduction steps

1. Create a contractor from `/workforce/contractors`.
2. Confirm it appears in the contractor table.
3. Create a worker from `/workforce/workers`.
4. Confirm it appears in the worker table.
5. Create an employee from `/employees`.
6. Confirm it appears in the employee table.
7. Immediately open `/audit-logs`.
8. Review the newest events and the next page of older events.
9. Search visually for the record codes above and relevant entity/action types.

## Actual result

- All three records persist in their functional screens.
- No matching create events were found in the reviewed audit entries.
- The newest audit records are dominated by `USER TOKEN_REFRESH`.
- Multiple token-refresh events can occur within the same minute.
- Business operations become difficult to find among session noise.

## Expected result

Each successful sensitive mutation should create one business audit event.

### Contractor event example

- Entity type: `CONTRACTOR`
- Action: `CREATE`
- Code: `QA-CTR-RETEST-0808`
- Accounting model
- Settlement cycle
- Payment routing
- Actor and timestamp

### Worker event example

- Entity type: `WORKER`
- Action: `CREATE`
- Code: `QA-WRK-RETEST-0808`
- Contractor ID/code
- Category ID/code
- Daily rate and hours
- Actor and timestamp

### Employee event example

- Entity type: `EMPLOYEE`
- Action: `CREATE`
- Code: `QA-EMP-RETEST-0808`
- Category
- Employment type
- Salary
- Active period
- Actor and timestamp

## Business impact

- The system cannot reliably answer who created or changed important master data.
- Contractor financial setup changes may be untraceable.
- Employee and worker records can affect attendance, settlements, payroll, and journals without a complete audit chain.
- Compliance investigation becomes difficult.
- Token-refresh noise can hide relevant security or business events.

## Recommended fix

1. Publish an audit event after the business transaction commits successfully.
2. Use the same transaction/outbox strategy so the record and audit event cannot diverge.
3. Add create, update, activate/deactivate, and delete/archive events for each sensitive entity.
4. Move token-refresh events to a separate session/security log, or reduce them to a lower verbosity level.
5. Add filters for date, user, entity, action, and business code.
6. Allow searching inside entity code and change details.

## Acceptance criteria

- [ ] Contractor creation produces exactly one `CONTRACTOR CREATE` event.
- [ ] Worker creation produces exactly one `WORKER CREATE` event.
- [ ] Employee creation produces exactly one `EMPLOYEE CREATE` event.
- [ ] Events contain actor, tenant, timestamp, entity ID, code, and meaningful details.
- [ ] Failed creates do not produce successful audit events.
- [ ] Audit entries are not editable or deletable through normal application workflows.
- [ ] Token refreshes no longer dominate the business audit page.
- [ ] Entity/action/code filters return the new QA records.

---

# REM-004 — Existing Duplicated Employee Code Requires Migration

## Classification

| Field | Value |
|---|---|
| Severity | Medium — existing data integrity |
| Priority | P2, before imports/payroll production use |
| Screen name | `الموظفون` |
| Direct route | `/employees` |
| Related screens | Reports, advances, payroll, attendance imports |
| Affected record | `QA موظف اختبار شامل` |

## Current data

| Field | Value |
|---|---|
| Originally entered code | `QA-EMP-0807` |
| Current stored/displayed code | `QA-EMP-0807-QA-EMP-0807` |

## Current behavior

The creation bug is fixed for new employees. The new employee `QA-EMP-RETEST-0808` stored correctly. However, the old corrupted record remains unchanged.

The duplicated code is visible in:

- employee table;
- report rows;
- advance/beneficiary references;
- any downstream display using the employee code.

## Why this is not only cosmetic

Employee codes may be used for:

- biometric matching;
- imports and exports;
- payroll identifiers;
- report lookups;
- advance deductions;
- integration keys;
- audit/search references.

## Required migration approach

1. Identify all duplicated-code patterns safely.
2. Confirm the intended canonical code.
3. Check that the canonical code is not already assigned to another employee.
4. Update the employee code using a controlled migration.
5. Update or preserve all foreign-key references.
6. Rebuild any search index or cached display value.
7. Record the correction in the audit trail.
8. Provide a dry-run report before applying the migration in production.

## Acceptance criteria

- [ ] `QA موظف اختبار شامل` displays `QA-EMP-0807` once.
- [ ] The employee remains linked to the existing report and advance.
- [ ] No duplicate or orphan employee is created.
- [ ] Imports can match the corrected code.
- [ ] An audit event records the old and new code.
- [ ] A uniqueness constraint prevents future duplicate codes.

---

# REM-005 — Inventory Movement Lacks Separate Business Document References

## Classification

| Field | Value |
|---|---|
| Severity | Medium |
| Priority | P2 — required for procurement/accounting traceability |
| Screen name | `المخزون والحسابات` |
| Direct route | `/operations` |
| Dialog/action | `حركة جديدة` |

## Where to find it

1. Open **العمليات والمخزون**.
2. Open **المخزون والحسابات**.
3. Click **حركة جديدة**.

## Current form fields

The movement form contains:

- Item
- Supplier/customer
- Operation type
- Quantity
- Amount
- Waste percentage
- `رقم المرجع`
- Note
- Date/time

## Missing document identities

The form does not separately capture:

- purchase-request number;
- purchase-order number;
- goods-receipt/document number;
- supplier invoice number;
- supplier delivery-note number;
- internal voucher number;
- external reference;
- document attachment.

## Existing evidence

The supply receipt `QA-INV-0807` is visible in the inventory movement table, but its `نوع المستند` remains `—`.

## Actual result

All external and internal references must be compressed into one generic field or note. The resulting stock movement cannot be reliably matched to a procurement or finance document.

## Expected behavior by operation type

### Supply receipt

Required/available references should include:

- Supplier
- Purchase order
- Receipt document number
- Supplier delivery note
- Supplier invoice, when available
- Warehouse
- Attachment

### Sale/delivery

References should include:

- Sales order
- Delivery note
- Customer invoice
- Warehouse
- Attachment

### Adjustment

References should include:

- Adjustment voucher
- Reason code
- Approval reference
- Attachment/evidence

## Business impact

- Three-way matching is impossible or unreliable.
- Users cannot distinguish an invoice number from a receipt number.
- Duplicate supplier invoices are harder to detect.
- Stock and financial entries are harder to reconcile.
- Audit and dispute investigation lacks document evidence.

## Acceptance criteria

- [ ] Movement type controls which reference fields are visible and required.
- [ ] PO, receipt, invoice, voucher, and external references are stored separately.
- [ ] `نوع المستند` is populated in the movement table.
- [ ] Users can open linked procurement/finance documents from the movement.
- [ ] Duplicate supplier invoice numbers are validated per supplier.
- [ ] Attachments support safe type and size validation.
- [ ] Existing movements remain readable after schema changes.

---

# REM-006 — Report Period Shortcut Action Is Ambiguous

## Classification

| Field | Value |
|---|---|
| Severity | Medium UX / accidental side effect |
| Priority | P2 |
| Screen name | `التقارير والفترات` |
| Direct route | `/reports` |
| Affected control | Period shortcut cards, e.g. `أغسطس — من 16 إلى نهاية الشهر` |

## Where to find it

1. Open **متابعة والدوام**.
2. Open **التقارير**.
3. Scroll to `اختصارات جاهزة`.

## Current UI

The page contains:

- date-from field;
- date-to field;
- settlement-cycle selector;
- `معاينة` button;
- `إنشاء التقرير` button;
- date-period shortcut cards.

Each shortcut card is named only by its period and category cycle. It does not display an action verb.

## Actual usability problem

Before clicking, the user cannot know whether the shortcut will:

1. fill the date fields;
2. preview the period;
3. immediately create a persistent report.

In the earlier test, clicking a shortcut navigated directly to a newly created report. This is surprising because `معاينة` and `إنشاء التقرير` are already separate actions.

## Business impact

- Users may create reports accidentally.
- Overlap rules may block the intended future report.
- A report may be generated before dates and cycle are reviewed.
- Users may assume the card is a harmless form shortcut.

## Recommended design

Preferred behavior:

- Clicking a period shortcut fills the date and cycle fields only.
- The card label includes `استخدام هذه الفترة`.
- The user then chooses `معاينة` or `إنشاء التقرير`.

If immediate creation is required:

- Label the card `إنشاء تقرير لهذه الفترة`.
- Show a confirmation with dates and cycle.
- Disable the action when an overlapping report exists.

## Acceptance criteria

- [ ] Every shortcut has a clear action verb.
- [ ] A non-destructive shortcut does not create a report.
- [ ] Immediate creation requires explicit confirmation.
- [ ] Existing/overlapping periods are disabled with an explanation.
- [ ] Keyboard and screen-reader names explain the action.
- [ ] The final action shows a specific success message and opens the created report.

---

# REM-007 — Audit Pagination Range Does Not Reflect the Loaded Page

## Classification

| Field | Value |
|---|---|
| Severity | Medium UX / audit navigation |
| Priority | P2 |
| Screen name | `سجل التدقيق والتتبع` |
| Direct route | `/audit-logs` |
| Affected action | Click `التالي` to load older audit records |

## Reproduction steps

1. Open `/audit-logs`.
2. Confirm the first page shows newest token-refresh events.
3. Click `التالي`.
4. Confirm that the table content changes to older records.
5. Inspect the displayed row range and page indicator.

## Actual result

- Older content loads.
- The displayed range remains `1–25 / 99`.
- The page indicator does not reliably communicate which rows are shown.

## Expected result

For page 2 with page size 25:

- Range should display `26–50 / 99`.
- Page indicator should display `2 / 4`.
- Previous and next button states should match the current page.

## Business impact

- Auditors cannot confidently identify their position in the log.
- Users may review the same data twice or skip a page.
- Evidence screenshots/exports can contain a misleading range.

## Acceptance criteria

- [ ] Page 1 displays `1–25 / 99` and `1 / 4`.
- [ ] Page 2 displays `26–50 / 99` and `2 / 4`.
- [ ] Changing page size recalculates ranges correctly.
- [ ] Previous/next buttons update their disabled states.
- [ ] Refresh preserves or intentionally resets the page with clear behavior.

---

# REM-008 — Shortcut Settings Screen Is Too Dense for Efficient Editing

## Classification

| Field | Value |
|---|---|
| Severity | Low/Medium UX |
| Priority | P3 |
| Screen name | `إعداداتي` → `الاختصارات` |
| Direct route | `/settings` |
| Affected action | Review or edit many shortcut mappings |

## What works

- The configured shortcut `G → D` successfully opens the Dashboard.
- Each existing row has a target selector and enabled state.
- Selected target values are present.

## Remaining UI problem

The screen displays many shortcut rows simultaneously. Every row exposes a large target-page selector, enabled state, and removal control. The repeated long option lists create visual noise and make comparison difficult.

## Recommended layout

Use a compact table:

| Shortcut | Target screen | Status | Actions |
|---|---|---|---|
| `G → D` | Dashboard | Enabled | Edit / Delete |

Only show the full target selector after the user clicks **Edit**. Provide:

- search by shortcut or target page;
- conflict detection;
- duplicate-key warning;
- add-new-shortcut button;
- clear save/cancel state;
- readable empty state.

## Acceptance criteria

- [ ] Default view is compact and scannable.
- [ ] Editing one row does not expand every other row.
- [ ] Duplicate sequences are prevented.
- [ ] Invalid/incomplete shortcuts cannot be saved.
- [ ] The selected target is visible without opening the selector.
- [ ] Add, edit, delete, reset, and save actions provide specific feedback.
- [ ] Keyboard-only users can manage shortcuts.

---

## 2. Business Flows That Still Require Final Acceptance Testing

These are **not confirmed defects**, but they must be tested after the remaining blockers are fixed.

### Attendance-to-payroll

1. Record/import attendance.
2. Resolve every HR exception.
3. Approve the report.
4. Generate payroll.
5. Apply advance deduction.
6. Approve and pay payroll.
7. Post journal entries.

Current blocker: REM-001.

### Contractor-to-settlement

1. Contractor and workers.
2. Labor request.
3. Attendance.
4. Fifteen-day calculation.
5. Review and approve settlement.
6. Contractor account posting.
7. Payment and journal posting.

Creation and attendance now work, but the complete financial settlement still needs an end-to-end transaction.

### Purchase-to-pay

1. Purchase request.
2. Quotation.
3. Purchase order.
4. Goods receipt.
5. Supplier invoice.
6. Payment.
7. Stock, supplier ledger, and journal verification.

Current traceability limitation: REM-005.

---

## 3. Developer Fix Order

1. **REM-001:** attendance decision persistence and error handling.
2. **REM-003:** complete audit event publication and reduce token-refresh noise.
3. **REM-002:** finalize and implement the category scope model.
4. **REM-004:** migrate the already corrupted employee code.
5. **REM-005:** implement distinct business-document references.
6. **REM-006:** clarify report shortcut behavior.
7. **REM-007:** correct audit pagination state.
8. **REM-008:** simplify the shortcut settings layout.

---

## 4. Final Retest Checklist

- [ ] Resolve one attendance row and verify it after reload.
- [ ] Confirm the attendance decision is audited.
- [ ] Confirm report counters update atomically.
- [ ] Create a `Both` category and use it for both an employee and a worker.
- [ ] Create contractor, worker, and employee records and locate their audit events.
- [ ] Confirm token refresh events do not hide business actions.
- [ ] Correct the old duplicated employee code without breaking reports or advances.
- [ ] Record a procurement receipt with separate PO, receipt, and invoice numbers.
- [ ] Verify document type and linked documents in inventory movements.
- [ ] Confirm period shortcuts describe their action and cannot create accidentally.
- [ ] Navigate every audit page and verify range/page indicators.
- [ ] Add, edit, remove, and execute a custom shortcut using the improved settings UI.

---

## 5. Conclusion

Only a focused set of issues remains. The most serious is the attendance-review save failure because it can silently lose HR decisions and block payroll readiness. Category scope and audit completeness are also production blockers because they affect configuration integrity and accountability. The other items concern existing data cleanup, transaction traceability, and UI clarity.

Fixing REM-001 through REM-003 should be treated as the next release gate. After that, apply the data migration and UI/traceability improvements, then execute the three end-to-end business-flow tests listed above.
