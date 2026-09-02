# Evidence — BUG-020 — Workforce category form/table rule fields inconsistent

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/src/app/features/workforce/pages/categories/categories.component.html` — creation dialog explicitly exposes every business-relevant rule field: code (`codeRequired *`), name (`nameRequired *`), description, default daily rate (`dailyRateRequired *`), standard daily hours (`hoursRequired *`), scope (`scopeRequired *` with WORKER/BOTH options), and default settlement cycle (`cycle *` with HALF_MONTH/MONTHLY options).
- `fe/src/app/features/workforce/pages/categories/categories.component.ts` — `getCycleLabel` (HALF_MONTH/MONTHLY/THIRTY_DAYS/HALF_MONTHLY), `getScopeLabel` (WORKER/EMPLOYEE/BOTH), `getStatusLabel` render the same configured values shown in the table.
- The table (code/name/description/dailyRate/hours/cycle/scope/status) reflects the exact configured values.
- DB translations: `workforce.ui.categories.codeRequired/nameRequired/dailyRateRequired/hoursRequired/scopeRequired/cycle/dailyRate/hours` ar-EG + en-US present.

Automated tests:
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures.
- Gates: `check:i18n` 5,884 keys PASS; `check:hardcoded` PASS; `check:translation-catalog.py` 17,888 rows PASS.

Manual verification:
- Creating a category exposes the daily-rate, hours, scope (incl. BOTH), and settlement-cycle (incl. HALF_MONTH = 15-day) options explicitly — no longer "appeared without explicit selection."
- Defaults (rate 0, hours 8, HALF_MONTH, WORKER) are visible in the form and match the table.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A to this consistency fix)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
