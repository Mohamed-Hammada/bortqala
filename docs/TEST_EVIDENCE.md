# Test Evidence — Bemo ERP

Single source of truth for automated test counts and regression baselines. Every
entry records the date, the repository HEAD SHA it was run against, the command,
and the result. The CI pipeline enforces the baselines below: a run that drops
below a recorded baseline or reports failures fails the release gate.

## Baselines (enforced by CI)

| Suite | Baseline | Command | Threshold rule |
|-------|----------|---------|----------------|
| Backend (non-Docker, H2) | **268 tests / 56 suites / 0 failures** | `./gradlew test -PskipDockerTests` | count ≥ 268 AND failures = 0 |
| Backend (full, incl. Testcontainers) | **308 tests** expected when Docker available | `./gradlew test` | failures = 0 |
| Frontend (Angular + Vitest) | **178 tests / 30 files / 0 failures** | `npx ng test --watch=false` | count ≥ 178 AND failures = 0 |

## Evidence log

### 2026-08-08 — working tree (REM-002 category scope + REM-008 shortcut select sync)
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **30 files passed, 178 tests passed, 0 failed**. New specs: `categories` scope columns, `WorkerCategoryService`-related FE fixtures. Removed the leftover always-failing diagnostic `fe/src/app/debug-shortcut.spec.ts` (`expect(out).toBe('__FAIL__')`). Fixed 5 pre-existing DOM failures in `shortcut-settings.component.spec.ts` (added after Session 11 in commit `1004b17 "ss"`): root cause was Angular's `SelectControlValueAccessor.writeValue` running *before* the nested `@for` rendered the `<option>` elements, so `select.value` was set with no matching option and never re-written (the model value never changed). Fix: `ngAfterViewChecked` re-syncs each `.shortcut-destination-select` to its draft's `pageCode` (real browsers auto-select the first appended option; jsdom does not), and `changeDestination` now rejects any value not offered by `destinationOptions(index)` (guards against stale/unknown select values — the same desync class as the blocked-duplicate revert).
- Related: `npm run check:i18n` = 1677 keys (ar-EG + en-US; +7 from V135 category-scope translations); `npm run check:hardcoded` = 0 findings. Baseline refreshed to 178/30 in `fe/tools/check-test-count.mjs`.
- Backend: full `./gradlew test -PskipDockerTests` run in progress at time of writing — see next entry for the verified result and refreshed backend baseline.

### 2026-08-08 — working tree (settings QA: notification prefs + shortcuts UI fix)
- Notification prefs no longer persist on toggle: `updateNotificationPrefs` in `settings.page.ts` only updates the `notificationPrefs` signal; `saveNotificationPrefs()` moved into `saveUserPreferences()` (the 💾 Save All System Settings handler, both appearance + reports tabs), and `cancel()` resets prefs to the last-persisted `loadNotificationPrefs()`.
- Shortcut settings theme fix: `shortcut-settings.component.scss` and `settings.page.scss` replaced non-existent tokens (`--surface-card`, `--border-color`, `--text-muted`, `--surface-header`, `--surface-input`, `--primary`) with the app's real tokens (`--surface`, `--line`, `--muted`, `--surface-muted`, `--input-bg`, `--gold`, `--gold-glow`, `--success-soft`, `--warning-soft`, `--danger-soft`, `--warning-text`) — fixes white-cards-in-dark-mode.
- Shortcut destination `<select>` desync fix: Angular skips DOM writes for property bindings when the bound value is unchanged, so a blocked change (duplicate destination) left the select showing the stale picked value. `changeDestination` now takes the `Event` and reverts `select.value` imperatively on the blocked branch.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 159 tests passed, 0 failed** (+4 new DOM/interaction assertions in `shortcut-settings.component.spec.ts`: initial select render, async profile-load render, change sync, blocked-change revert). Baseline refreshed to 159/28 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = 1670 keys (ar-EG + en-US; -1 because `settings.notificationSaved` is no longer invoked after the save-deferral); `npm run check:hardcoded` = 0 findings across 36 HTML templates and 109 TypeScript source files; `npx ng build --configuration production` green (pre-existing SCSS budget warnings only).

### 2026-08-07 — working tree (procurement hardening + TS hardcoded-UI pass)
- Applied the "Bortqala cumulative patch" (operation-specific procurement busy states, three-way-match i18n, TS hardcoded-UI scanner, users/employees localization). Fixed a bug in the delivered V126 CSV: `translations.id` is the primary key, but V126 reused `v126-001/002/003` for both locales (would violate the PK on the H2/PostgreSQL Liquibase load); rewrote to distinct per-locale ids (`v126-001`…`v126-006`, matching the V119/V125 convention).
- The new TS scan then surfaced **119 hardcoded notification/confirm/fallback strings** in 13 files that the patch did not address. Finished the pass: removed 35 third-argument `i18n.t(key, …, 'fallback')` fallbacks (all keys already present in the DB store — verified `imports.*`, `review.*`, `payroll.*`, `manualAttendance.*`, `settings.*`, `operations.invalidNegativeQuantity`), wrapped 21 `notification.success/error(...)` + `window.confirm(...)` literals with `i18n.t(...)` keys, and injected `I18nService` into 4 components that lacked it (settlement-periods, contractor-settlement-detail-modal, workflow-definitions, pending-approvals).
- New **Liquibase V127** `20260807_v127_hardcoded_notification_translations.{yaml,csv}` (21 keys × 2 locales, ids `v127-001`…`v127-042`) registered in `next` + `test-h2` changelogs.
- Backend: `./gradlew test -PskipDockerTests` in `be/` → **BUILD SUCCESSFUL**; JUnit XML summary: **268 tests, 56 suites, 0 failures, 0 errors, 0 skipped**. V126 + V127 both load cleanly on H2.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 155 tests passed, 0 failed** (+5 from the patch: 3 users-page role-summary tests + 2 procurement operation-state tests). Baseline refreshed to 155/28 in `fe/tools/check-test-count.mjs`.
- Related: `npm run check:i18n` = 1668 keys (ar-EG + en-US; +21 from V126/V127); `npm run check:hardcoded` = 0 findings across 36 HTML templates and 109 TypeScript source files (was 119 candidates before this pass); `npm run build` green (pre-existing SCSS budget warnings only).

### 2026-08-07 — HEAD `498227e` (working tree, B-1 budget & encumbrance landed)
- Backend: `./gradlew test -PskipDockerTests` in `be/` → **BUILD SUCCESSFUL in 14m 26s**; JUnit XML summary: **268 tests, 56 suites, 0 failures, 0 errors, 0 skipped** (`be/build/test-results/test/*.xml`). New suites: `com.bemo.hr.budget.*` (BudgetService + encumbrance lifecycle + controller), plus the BUDGETS `AccessCatalog` page. Fixed a broken V125 CSV (stray `;` in `procurement.departmentHint` en-US text split the row into 5 columns and failed the H2 Liquibase migration — all 53 failing context-load tests were one root cause).
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **28 files passed, 150 tests passed, 0 failed**. New spec: `budgets.page.spec.ts` (11 tests). Fix: MONTHLY prefill is now driven by a reactive `valueChanges` subscription (`takeUntilDestroyed`) instead of a DOM `(change)` handler so control-level `setValue` also prefills.
- Related: `npm run check:i18n` = 1645 keys (ar-EG + en-US; +42 from V123/V125); `npm run check:hardcoded` = 0 findings across 36 templates; `npm run build` green (pre-existing SCSS budget warnings only; local build validated with `optimization.fonts.inline=false` because this sandbox has no route to fonts.googleapis.com — CI keeps full optimization). Backend baseline scripts fixed: `check-test-count.py` BASE_DIR resolved to the repo root (`be/`), not `be/tools/`.
- Note: local `node` is `v26.5.1` (default), which violates `engines >=22 <25` and breaks `localStorage` in Vitest under jsdom (`--localstorage-file` absent). Tests must run under node 24 (`nvm use 24`, per `.nvmrc`).

### 2026-08-07 — HEAD `498227e` (working tree, A-4 trends surface landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` (native Gradle mirror, source synced from `be/src` on 2026-08-07) → **BUILD SUCCESSFUL in 2m 42s**; JUnit XML summary: **252 tests, 55 suites, 0 failures, 0 errors, 0 skipped** (`/tmp/opencode/be-build/build/test-results/test/*.xml`). New suite: `DashboardServiceTrendsTests` (2 tests) for the multi-period `/api/v1/dashboard/trends` surface.
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **27 files passed, 139 tests passed, 0 failed**. New spec: `dashboard.store.spec.ts` (3 tests) covering `loadTrends` + `downloadTrends`.
- Related: `npm run check:i18n` = 1603 keys (ar-EG + en-US; +12 from V121); `npm run check:hardcoded` = 0 findings across 35 templates; `npm run build` green (pre-existing SCSS budget warnings only). Full suite adds 40 Testcontainers tests which cannot run locally (Docker daemon down in this WSL env) — CI provides Docker.

### 2026-08-07 — HEAD `498227e` (working tree, E-1 landed)
- Backend: `./gradlew test -PskipDockerTests` in `/tmp/opencode/be-build` (native Gradle mirror, source synced from `be/src` on 2026-08-07) → **BUILD SUCCESSFUL in 2m 36s**; JUnit XML summary: **250 tests, 54 suites, 0 failures, 0 errors, 0 skipped** (`/tmp/opencode/be-build/build/test-results/test/*.xml`).
- Frontend: `npx ng test --watch=false` under node `v24.18.1` → **26 files passed, 136 tests passed, 0 failed**.
- Related: `npm run check:i18n` = 1591 keys (ar-EG + en-US); `npm run check:hardcoded` = 0 findings across 35 templates; `npm run build` green (pre-existing SCSS budget warnings only). Full suite adds 40 Testcontainers tests (run 29/30 family: PunchSourceIdentity 3, WorkforceImportCommit 10, SupplierPayment 10, ReportingBulkDecision 1, LiquibaseUpgradePath + migration/concurrency suites) which cannot run locally (Docker daemon down in this WSL env) — locally they manifest as `NoClassDefFoundError: Could not initialize PostgresIntegrationTest`, and CI provides Docker.

### 2026-08-06 — HEAD `498227e`
- Backend full `./gradlew test`: **290 tests, 259 green, 31 fail** — all 31 are `NoClassDefFoundError: Could not initialize PostgresIntegrationTest` (Docker daemon down; every failing suite extends the Testcontainers base). No non-Docker test fails.
- Frontend: `ng test` **136/136** (26 files), `check:i18n` 1424 keys, `ng build` green.

### 2026-08-06 — HEAD `ee8356e` (release gate, P3-01)
- Backend `./gradlew clean test`: **209 tests, 54 suites, 0 failures / 0 errors / 0 skipped**; PostgreSQL/Testcontainers concurrency 24/24; fresh-database Liquibase 141 changesets with `ddl-auto=validate`; upgrade-path from v1_v67 baseline green.
- Frontend: `npm ci` OK; `check:i18n` 1103 keys; `ng test` 46 tests / 17 files; `ng build` OK.

## How to refresh a baseline
1. Update the evidence log entry above with the new date + HEAD SHA.
2. Re-run the exact command listed and paste the observed counts.
3. Only raise a baseline after a verified green run; never lower a baseline to silence a regression (open a blocker instead).
