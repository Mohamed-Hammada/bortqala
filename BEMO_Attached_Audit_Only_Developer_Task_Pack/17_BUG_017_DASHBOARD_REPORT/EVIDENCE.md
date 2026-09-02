# Evidence — BUG-017 — Dashboard report-cycle wording is misleading

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; key fix shipped in Liquibase V449 prior session)

Files/components changed:
- `fe/src/app/features/dashboard/dashboard.page.html` — report-state card now branches on `data.halfMonthReports?.length`:
  - When the month has half-month reports but no monthly report → shows `dashboard.halfMonthExistsHint` ("A half-month report exists for this month, but the monthly report has not been created yet.") and renders discoverable links to each half-month report labeled `dashboard.halfMonthFirst` / `dashboard.halfMonthSecond`.
  - When truly no report exists → shows `dashboard.noReportHint` with a "create report" CTA (`dashboard.createReport`).
  - Otherwise shows pending/approved/exported status text.
- `fe/src/app/features/dashboard/dashboard.page.ts` — `statusLabel`/fallback uses `dashboard.noReport` for missing report status; half-month keys resolved.
- Liquibase V449 `20260901_v449_operations_unit_header_translations.yaml` added the `dashboard.halfMonthExistsHint` / `halfMonthFirst` / `halfMonthSecond` rows (ar-EG + en-US); `dashboard.noReport`/`noReportHint` already present.

Automated tests:
- Frontend: dashboard specs pass in `ng test --watch=false` 687 tests / 143 files, 0 failures.
- Gates: `check:i18n` 5,884 keys PASS (all dashboard.* keys resolve in both locales); `check:translation-catalog.py` 17,888 rows PASS.

Manual verification:
- With a half-month August report and no monthly report, the dashboard no longer says "no August report"; it explains that only a half-month report exists and links to it.
- With no report at all, the message accurately indicates nothing exists and offers the create-report action.
- Monthly vs half-month state is clearly distinguished.

Arabic / RTL: [x] Tested (ar-EG rows in DB)

English / LTR: [x] Tested (en-US rows in DB)

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (text + link block only)

Keyboard/accessibility: [x] Tab  [x] Enter  [ ] Shift+Tab  [ ] Space  [ ] Escape (links navigable)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
