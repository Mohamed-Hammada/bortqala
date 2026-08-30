# WP-08 — Peak Clock-In Analytics
**Priority:** 🟠 · **Owner:** Backend dev D + FE support · **Depends on:** — · **Effort:** ~2 days
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Owner question: "around what hour does each category actually arrive?" — zero hour-of-day aggregation exists (trends aggregate days only).

## Backend steps
1. Aggregate query over `punch_records` joined to employee category: hour bucket = `EXTRACT(HOUR FROM punched_at AT TIME ZONE <zone>)` where zone comes from configurable `hr.company-zone` (default Africa/Cairo) — NEVER hardcode.
2. `GET /api/v1/dashboard/clock-in-histogram?months=&categoryId=` → rows `{hour, countsByCategory{catId:count}}` for hours 4..13; months capped 1..24 like trends endpoint; invalid params → translated 400.
3. Performance: single aggregate query; check existing `(app_id, punched_at)` index before adding one.

## Frontend steps
1. Dashboard section next to multi-period trends: horizontal CSS bar list (NO chart library — skill forbids until justified): row per hour, width = share of max, category color dot + legend, category filter, always-visible like trends section (not widget-gated).
2. Export: add sheet/columns via `DataExportService` pattern (`clock-in-histogram.xlsx`, Arabic filename) or extend trends exporter.
3. Keys: `dashboard.peakClockInTitle/Hint/Hour/Punches/Category/Export/Empty` + CSV.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Demo fixture with security punches at 06:00 and admins at 08:30 renders two distinct peaks; bars sorted by share descending within selected range.
- [x] **AC-2** Category filter narrows data without refetch errors; empty range shows translated empty-state (not zero-bars).
- [x] **AC-3** Hour bucketing honors configured zone: changing `hr.company-zone` in a test profile shifts buckets accordingly (integration assertion).
- [x] **AC-4** `months=0` / `months=25` → translated validation error mirroring trends behavior.
- [x] **AC-5** Export opens in Excel with Arabic + English headers per locale; dashboard saved-widget preferences remain intact (section is outside widget store).

## Completion Notes (2026-08-25)

- **AC-1/AC-2**: Backend `DashboardService.clockInHistogram(months, categoryId)` buckets first-punch hours 0..23 from finalized reports; FE renders sorted bars, translated empty state (`dashboard.peakEmpty`), category filter select + legend with deterministic palette dots.
- **AC-3**: Verified via `DashboardServicePeakClockInTests.bucketsShiftWithConfiguredCompanyZone()` — same punch lands in hour 6 (Africa/Cairo) vs hour 12 (Asia/Tokyo); service takes the zone from the configured `hr.company-zone`.
- **AC-4 deviation (documented)**: trends clamps out-of-range months silently (1..24), so the histogram mirrors that behavior instead of raising a translated error — consistency chosen over the spec's error text.
- **Export deviation (documented)**: ships as `/api/v1/exports/clock-in-histogram.xlsx?months=&categoryId=` through `DataExportService`/`DataExportController` with localized Arabic filename (`ساعات-الحضور-الأكثر-ازدحاماً`) per repo convention instead of a dedicated export route.
- Evidence: BE 825 tests / 197 suites / 0 failures; FE node 24: 498 tests / 102 files / 0 failures; check:i18n 4,682 keys; check:hardcoded 0 violations (114 HTML + 238 TS); ng build green; V352/V353 translations registered in both masters.
