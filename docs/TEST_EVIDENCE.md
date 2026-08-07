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
| Frontend (Angular + Vitest) | **150 tests / 28 files / 0 failures** | `npx ng test --watch=false` | count ≥ 150 AND failures = 0 |

## Evidence log

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
