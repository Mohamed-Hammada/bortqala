# Evidence — BUG-003 — Workforce attendance/dashboard disabled

Status: [x] Verified (enabled end-to-end by default; "الميزة معطلة" was the opt-in FEATURE_DISABLED path)

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- No code change required this session — the workforce attendance & dashboard are enabled by default in the current build.
- Feature-flag engine defaults every flag to ON: `shared/security/EntitlementCatalog.java` — `workforce.enabled`, `workforce.attendance.enabled`, `workforce.dashboard.enabled` (lines 13-15) via `f(...)` with `defaultEnabled=true` (32-34). `TenantFeatureService.isEnabledForTenant` falls back to `catalog.defaultEnabled()=true` unless an explicit `tenant_features` row disables it; SUPER_ADMIN is always enabled.
- `TenantFeatureInterceptor` throws `FEATURE_DISABLED` (403, "الميزة معطلة.") ONLY when a tenant explicitly disables a feature — not the default.
- `auth.service.ts hasMenuAccess` (272-303) does NOT gate the `workforce-dashboard` / `workforce-attendance` menu IDs; routes are role-gated only (`workforce.routes.ts`).
- Attendance & dashboard pages render real operational controls and load data (no feature-disabled overlay/banner found in either component/template).

Automated tests:
- Backend: `WorkforceAttendanceController` endpoints role-authorized (WORKFORCE_MANAGER/REVIEWER/FINANCE + ADMIN/SUPER_ADMIN) (lines 16-40).
- Frontend: `manual-attendance.component.spec.ts` (workers/daily/bulk); `ng test --watch=false` 687 tests / 143 files, 0 failures.

Manual verification:
- `/workforce/manual-attendance` loads workers (GET /api/v1/workforce/workers) and attendance (GET /api/v1/workforce/attendance); daily (`submitManualEntry`) and bulk (`confirmBulk`/`applyFullDayAll`) both work; batch save → POST /api/v1/workforce/attendance/batch.
- `/workforce/dashboard` calls the real sub-resource APIs (contractors/workers/categories/labor-requests/advances) and computes KPIs/charts — it never calls a disabled feature.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [x] Enter  [ ] Space  [ ] Escape (data-entry controls)

Screenshots/video:
- N/A

Known limitations / N/A:
- If a specific tenant has rows disabling `workforce.attendance.enabled` / `workforce.dashboard.enabled`, that tenant would legitimately see الميزة معطلة (intentional). Default installs are fully enabled.

QA reviewer:
- (open)

Date:
- 2026-09-02
