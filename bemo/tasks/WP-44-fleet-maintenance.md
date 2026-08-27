# WP-44 — Fleet & Equipment Maintenance
**Priority:** 🟢 · **Owner:** Backend dev C · **Depends on:** WP-04 (assets) recommended · **Effort:** ~7 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Odoo gaps

## Business goal
Companies running vans/trucks/machines need: vehicle registry linked to fixed assets, fuel logs with efficiency tracking, maintenance schedules with due alerts, driver assignment, license/insurance renewals (reuse contract-expiry alerting pattern).

## Backend steps
1. Tables: `vehicles` (plate, type, asset_id FK nullable → WP-04, default_driver_employee_id, active) · `fuel_logs` (vehicle FK, date, liters >0, odometer, cost, station_text) — derived km/liter between consecutive odo entries (backend-computed) · `maintenance_schedules` (vehicle or equipment ref, kind OIL|TIRES|INSPECTION|CUSTOM, interval_km|interval_days, last_done_odometer/date, next_due derived) · `maintenance_records` (done_at, odometer, cost, vendor party nullable) · `vehicle_documents` (license/insurance expiry trio reuse).
2. Due engine: daily job flags schedules due within X days/km → NotificationCenter cards per fleet manager; document expiry reuses existing expiry-alert service.
3. Cost report: per-vehicle monthly fuel+maintenance cost; per-km cost where odometer data allows.
4. Codes `FLEET_*` (~8).

## Frontend steps
1. Feature `features/fleet/`: vehicles table + detail tabs (logs, schedule, docs); fuel entry quick form; due-soon dashboard card; renewal alerts list.
2. Keys ~22.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Efficiency: consecutive logs 100km apart with 10L → 10.0 km/L displayed; missing odometer rows skipped gracefully in trend.
- [ ] AC-2 Schedule due by km fires when latest maintenance odometer + interval ≤ current max odometer (fixture math), and by days independently; done-record resets both baselines.
- [ ] AC-3 License expiry alert appears exactly once per document per lead-window config (dedupe key test).
- [ ] AC-4 Link to WP-04 asset shows depreciation NBV on vehicle page when linked; unlink safe.
- [ ] AC-5 Cost report totals tie to sum of child records within 0.01 for month fixture.
