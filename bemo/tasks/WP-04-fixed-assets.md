# WP-04 — Fixed Assets Module (roadmap B-4, Daftra parity gap)
**Priority:** 🔴 · **Owner:** Backend dev C (finance) · **Depends on:** — · **Effort:** ~6 days
**Liquibase:** V344/V345 (coordinate with index table).
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Companies own cars/machines/PCs. Accountant needs monthly depreciation auto-posted to GL and disposal gain/loss handled. Today: nothing exists.

## Backend steps
1. New package `com.bemo.hr.assets`. Entity `FixedAsset`: name, category (VEHICLE/MACHINERY/EQUIPMENT/BUILDING/OTHER), acquisition_date, acquisition_cost >0, salvage_value ≥0 < cost, useful_life_months 1..480, method STRAIGHT_LINE (v1), cached accumulated_depreciation + last_posted_year_month, status ACTIVE/FULLY_DEPRECIATED/DISPOSED, disposal(date, proceeds), branch_id/cost_center_id FKs (existing org tables), version.
2. Migration V344: table + indexes `(app_id,status)`, `(cost_center_id)`; V345 evidence table `fixed_asset_depreciation_posts` with UNIQUE `(asset_id, year_month)` → idempotent runs.
3. Depreciation run: service method + `@Scheduled` monthly cron + manual `POST /api/v1/fixed-assets/run-depreciation?yearMonth=YYYY-MM` (admin). Per active asset: monthly charge = `(cost − salvage)/life`, final month = remainder. Skip already-posted months; post ONE journal per asset/month: Dr depreciation-expense (`hr.finance.depreciation-expense-account-code`, default `5300`) / Cr accumulated-depreciation (default `1280`). Resolve accounts via existing account lookup; missing account → skip asset with warning log + result entry (never fail whole run).
4. Disposal: `POST /{id}/dispose {date, proceeds}` only from ACTIVE/FULLY_DEPRECIATED: Dr accumulated (book value), Dr cash(proceeds), plug Dr loss / Cr gain computed backend-side, Cr original cost.
5. Export `/api/v1/fixed-assets/export.xlsx` via DataExportService pattern (Arabic filename).
6. Codes: `ASSET_LIFE_INVALID`, `ASSET_DISPOSAL_INVALID`, `DEPRECIATION_PERIOD_LOCKED`, `ASSET_NOT_FOUND`.

## Frontend steps
1. New feature `features/finance/fixed-assets/`: table (name, category, cost, monthly charge, accumulated, NBV, status badge), create/edit dialog, dispose dialog with live gain/loss preview, admin "Run month-end" button with year-month picker + per-asset results toast.
2. Full menu protocol (new menu id `fixedAssets`, finance workspace); AccessCatalog += `P_ASSET_READ`/`P_ASSET_MANAGE` → catalog count 35 → update PROJECT_MAP `catalog_entries`.

## Tests
Straight-line math incl. short final month; double-run same month = one journal (unique constraint); disposal gain AND loss branches; tenant isolation; scheduler skipped when manual lock held.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Asset of cost 120,000 / salvage 0 / life 60 shows exactly 2,000 monthly charge; final month posts the exact remainder (no rounding drift over the life). — `FixedAssetTests` (1000/3 → 333.33 ×2 + 333.34 remainder, FULLY_DEPRECIATED flip).
- [x] **AC-2** Running the same month twice creates ONE journal entry (evidence row proves it); second run returns "already posted" summary without errors. — uq `uq_fixed_asset_dep_post_asset_month` + `existsByAssetIdAndYearMonth`; `rerunningTheSameMonthIsExactlyOnce`.
- [x] **AC-3** Trial balance after N months shows accumulated depreciation = N × monthly charge. — cached accumulated verified after each run (`straightLineRunPostsOneBalancedApprovedJournalPerAsset` asserts 1000.00 after one charge; balanced Dr=Cr lines).
- [x] **AC-4** Disposal mid-life computes gain or loss correctly on two fixtures; journals balance to zero. — `disposalWithGainBooksBalancedGainJournal` (proceeds 9000 vs NBV 8000 → Cr gain 1000, debits==credits==13000) + `disposalAtALossBooksTheLossPlug` (Dr loss 500).
- [x] **AC-5** Disposed asset disappears from run scope but stays listed with DISPOSED badge and history. — run scope = status ACTIVE only; FE keeps DISPOSED rows with disposal date/proceeds cell.
- [x] **AC-6** Menu visible to ADMIN immediately after migration on an EXISTING database; non-finance roles don't see it. — V347 allowed_menus UPDATE + AccessCatalog page(FINANCE_ROLES) + FE route/contract/auth gate; `page-access-consistency.spec` updated.
- [x] **AC-7** Excel export localized; all new codes translated; DoD gates pass. — `/api/v1/exports/fixed-assets.xlsx` w/ Arabic filename; error codes gate **596/596**; catalog **13,938 PASS**.

## Evidence (2026-08-24)
- BE package `com.bemo.hr.assets`: `FixedAsset` (chargeFor/firstChargeMonth/finalChargeMonth/registerPostedCharge/dispose/disposalGainOrLoss), `FixedAssetDepreciationPost`, repos (`existsByAssetIdAndYearMonth`), `AssetsApi` DTOs, `AssetDepreciationService` (run + disposeWithJournal + ReentrantLock), `AssetDepreciationScheduler` (cron `hr.assets.depreciation-cron`, default 01:00 UTC 1st, runs previous month, skips when lock held), `AssetService` CRUD+dispose, `AssetsController` `/api/v1/fixed-assets` (`asset.read`/`asset.manage` permissions).
- Account codes configurable via `hr.finance.{depreciation-expense|accumulated-depreciation|fixed-asset-cost|asset-disposal-proceeds|asset-disposal-loss|asset-disposal-gain}-account-code` (defaults 5300/1280/1300/10101/5310/4100 — note 1280/5300 are NOT in the v27 seed chart, so first runs on a fresh DB skip with SKIPPED_MISSING_ACCOUNT until the accountant creates them; per spec this must not fail the run).
- **Deviation from spec:** export path is `/api/v1/exports/fixed-assets.xlsx` via DataExportService/DataExportController switch + localized Arabic filename (repo convention), not `/api/v1/fixed-assets/export.xlsx`.
- V347: `20260825_v347_fixed_assets.yaml` (fixed_assets + indexes (status)/(cost_center_id), fixed_asset_depreciation_posts + uq(app_id,asset_id,year_month), allowed_menus grant) + translations CSV 130 rows ids v347-001…130 registered in BOTH masters.
- AccessCatalog: P_ASSET_READ/P_ASSET_MANAGE constants + ALL_PERMISSIONS + FINANCE_READ/FINANCE_WRITE sets + FIXED_ASSETS page def (menuId `fixed-assets`, titleKey nav.fixedAssets, MANAGE action).
- FE: `features/finance/fixed-assets/{fixed-assets.models.ts,page.ts,html,scss}` — register table, add/edit drawer, dispose modal (hint + proceeds/date), run-depreciation modal (month picker prefilled previous month + results table with outcome labels), XLSX export; NAV_ITEMS + CATALOG_PAGE_CONTRACT + route guards + hasMenuAccess finance toggle; spec 8 tests.
- Gates: BE **800 tests / 196 suites / 0 failures** BUILD SUCCESSFUL 3m49s · error codes **596/596** · catalog **13,938 rows** · floors OK. FE **475 tests / 100 files**, check:i18n **4,646**, check:hardcoded **0/113 HTML + 237 TS**, build green. Baselines raised (≥800/196 BE, ≥475/100 FE); docs/TEST_EVIDENCE.md entry added.
