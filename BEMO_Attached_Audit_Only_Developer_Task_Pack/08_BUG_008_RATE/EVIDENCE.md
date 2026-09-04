# Evidence — BUG-008 — Worker daily rate does not inherit from category

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; implemented in a prior workforce session, verified this session)

Files/components changed:
- `fe/src/app/features/workforce/pages/workers/workers.component.ts` —
  - `onCategoryChange(categoryId)` (lines 94-99): on category select, sets `form.defaultDailyRate = category.defaultDailyRate` (inherited rate, e.g. 350) and `form.standardDailyHours = category.standardDailyHours` — no more hardcoded 200 default.
  - `openNew()` (lines 78-79): when a default/first category exists, seeds the worker's rate from it.
  - Validate `rate >= 0` and `hours > 0` (lines 114-115) with localized `workforce.ui.workers.rateHoursError`.
- `fe/src/app/features/workforce/pages/workers/workers.component.html` —
  - Category select options show each category's rate (line 64); after selection the rate/hours inputs are populated.
  - Explicit override hint `workforce.ui.workers.categoryInheritance` (line 81): "New workers inherit the daily rate and work hours from the selected category; they can be edited afterward." (ar: تورث اليومية وساعات العمل من الفئة ويمكن تعديلهما).
  - Rate is a separate editable labeled input (line 84) — the override is clearly visible and editable.
- `be/src/main/java/com/bemo/hr/workforce/WorkforceSettlementService.java` — settlement computes gross from `worker.getDefaultDailyRate()` (line 173, effective per-day entries at 175) → saved effective rate drives cost/settlement.

Automated tests:
- `workers.component.spec.ts` (inheritance + validation).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:i18n` 5,884 keys PASS; `check:hardcoded` PASS.

Manual verification:
- Selecting a category with rate 350 populates the worker form's daily rate to 350 (not 200); hours inherit too.
- The inherited value is editable; the override is clearly labelled by the inheritance hint.
- Saving persists the effective rate and it is used in worker settlement/labour cost.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [x] Tab  [x] Enter  [ ] Shift+Tab  [ ] Space  [ ] Escape (select + inputs)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
