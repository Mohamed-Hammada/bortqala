# WP-06 — Generated-Report-Periods Registry (hide finalized months)
**Priority:** 🟠 · **Owner:** Junior full-stack (starter task) · **Depends on:** — · **Effort:** ~2 days
**Read first:** `_GLOBAL-RULES.md`

## Business goal
HR picks July 2026 to generate a report — system must gray out months that already have an APPROVED report so nobody regenerates/conflicts. Today the picker shows every month.

## Backend steps
1. PREFER QUERY over new table: repo query deriving from existing approved reports — `SELECT DISTINCT period boundaries WHERE status = APPROVED` in `reporting` package.
2. Endpoint `GET /api/v1/reports/generated-periods?year=YYYY` → `[{from: epochMillis, to: epochMillis, type}]`. Validate year sane range; unknown/empty year → empty list, never error.
3. Only introduce a dedicated registry table IF the distinct query exceeds ~200ms on realistic data — measure first, document result in PR.

## Frontend steps
1. Reports page period picker fetches generated periods for displayed year; those month chips render DISABLED with tooltip `reports.alreadyGenerated` ("تم إنشاء تقرير معتمد لهذه الفترة") + inline link "عرض التقرير الحالي" navigating to the existing report filtered by its range.
2. Refetch when year changes; loading state on chips while fetching.
3. Keys: `reports.alreadyGenerated`, `reports.viewExisting` (+ CSV rows).

## Tests
BE endpoint returns expected ranges from fixture approved reports and ignores non-approved ones; FE spec asserts chip disabled + tooltip text + navigation emitted on link click.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Generate + approve July report → picker shows July disabled with translated tooltip; August still enabled. ✅ (DOM-tested: chip disabled + `reports.alreadyGenerated` title/note; sibling chips stay enabled)
- [x] **AC-2** DRAFT/unapproved reports do NOT disable their months (only APPROVED counts). ✅ (backend filters status IN (APPROVED, EXPORTED); unit-tested)
- [x] **AC-3** Tooltip's view-link opens the exact existing report (range matches). ✅ (`/generated-periods` returns reportId; view-link routerLinks to `/reports/{id}` — spec asserts href)
- [x] **AC-4** Year switch refetches; years with zero reports show all enabled with no error. ✅ (store token-guarded refetch on changeYear; empty/null/out-of-range year → empty list never errors)
- [x] **AC-5** No new table unless PR proves query timing >200ms (reviewer checks measurement note). ✅ (pure derived query `findByPeriodStartBetweenAndStatusIn` — no registry table)
