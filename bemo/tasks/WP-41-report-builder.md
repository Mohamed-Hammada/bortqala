# WP-41 — Custom Report Builder (drag-drop dataset explorer)
**Priority:** 🟡 · **Owner:** Full-stack B (senior-ish) · **Depends on:** — · **Effort:** ~10 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17G

## Business goal
"Show me sales per branch per month with returns" — today every question = new dev work. Give power users a safe query builder over curated datasets, saved as reusable reports.

## Design decisions
- SAFETY FIRST: no raw SQL. Backend exposes curated DATASETS (`sales_lines`, `attendance_days`, `journal_lines`, `stock_movements`) each with whitelisted dimensions/measures and JOIN paths defined in code. Builder composes {dataset, dimensions[], measures[], filters[], sort[], limit≤10k} → backend translates to parameterized JPA criteria/projection.
- All tenant filters auto-applied; permission per dataset required.

## Backend steps
1. Dataset registry: `ReportDataset` descriptors (code, entity path, allowed fields w/ types, measure aggregates SUM/COUNT/AVG, default sort) as code config — versioned so saved reports reference dataset versions.
2. Engine: validate request against descriptor (unknown field → `RB_FIELD_UNKNOWN`), build typed projection, execute paged, return columns metadata + rows (epoch-millis dates).
3. Saved reports: `saved_reports` (name, definition JSONB + dataset_version, owner, shared_role nullable); run endpoint revalidates against CURRENT descriptor (version drift → warn flag).
4. Export: reuse exporter for result grids.

## Frontend steps
1. Feature `features/reports/builder`: left panel datasets+fields tree, middle canvas (drag to dimensions/measures buckets), live preview table (paged), filter rows builder (field/op/value typed controls), save dialog (name/share), my-reports list.
2. No chart in v1 (tables first per user base); export button prominent.
3. Keys ~24 `reportBuilder.*`.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Sales-lines dataset: dimension=branch+month, measure=SUM(net) matches manual SQL fixture exactly for 3 branches × 4 months.
- [x] AC-2 Injection attempts in filter values are parameterized (engine test with hostile strings asserts no SQL error/leak).
- [x] AC-3 Unknown/disallowed field rejected translated listing allowed fields; limit>10k clamped with notice.
- [x] AC-4 Saved report runs identically after save (round-trip); dataset version drift shows non-blocking warning banner.
- [x] AC-5 Datasets respect permissions: attendance dataset hidden from role without HR read; cross-tenant impossible (context test).

## Deliverables Summary
- **Database Schema**: `saved_reports` (Liquibase `v379`, `v380`).
- **Backend Architecture**: Package `com.bemo.hr.reportbuilder` (`ReportDataset`, `ReportBuilderService`, `ReportBuilderController`, dataset whitelist projection engine, parameterized criteria builder, saved reports repository).
- **Frontend Architecture**: `ReportBuilderPage` (`fe/src/app/features/report-builder/report-builder.page.ts`), dataset field picker, dimensions/measures selector, filter builder, live data grid, and saved report runner.

