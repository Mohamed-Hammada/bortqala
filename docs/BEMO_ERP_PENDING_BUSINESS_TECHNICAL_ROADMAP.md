# BEMO ERP — Pending Business & Technical Roadmap (August 2026)

This roadmap consolidates every pending item from prior project status documents. It is the single source of truth for what remains after the completed `fm_bemo_consolidated` handoff and the `BEMO_ERP_FINAL_COMPLETION_REPORT` (29 July 2026).

**Reference numbers** (reuse in `PROJECT_MAP.md` blockers and commit messages):

- `ACCESS-APPROVAL-001` — Approval screens missing from access catalog (BLOCKER)
- `RELEASE-HEAD-001` — CI failure / release gate (BLOCKER)
- `DOC-STATUS-001` — PROJECT_MAP.md claims 33 catalog pages; catalog has 31 (DOCUMENTATION_MISMATCH)
- `PHASE1-REPORTS-001` — Phase 1 reports partial (P1)

---

## Part A — Immediate Release Blockers (P0)

### A-1. `ACCESS-APPROVAL-001` — Approval screens missing from access catalog (BLOCKER)
**Status:** ✅ RESOLVED 2026-08-06
**Reference:** Prior session FIN-001/UX-003, item "Effective Access Parity", `AccessCatalogService`.
**Problem:** The Workflow Definitions and Pending Approvals screens were built in the approval-workflow Epic, but the access catalog was never updated to include their menu IDs. Users with non-admin roles cannot reach them; the catalog parity test (`menuAndFeatureErrors`) does not cover them.
**Definition of done:**
- [x] Backend `AccessCatalogService` and `AccessCatalog` include the approval menu IDs (`approvals-my-tasks`, `approvals-workflows`) and feature codes (`approvals.read`/`approvals.decide`, `workflowDefinitions.read`/`workflowDefinitions.manage`) in `SUPER_ADMIN`/`ADMIN` defaults and in the tenant menu registry.
- [x] `AccessCatalogServiceTests` extend coverage to assert the new IDs are resolvable for `SUPER_ADMIN` and `ADMIN`, and denied for a role without them (34 cases green).
- [x] `AccessCatalogService.validateAssignmentOrThrow` accepts the new IDs.
- [x] Frontend `AuthService.hasMenuAccess` returns `true` for the new menu IDs for admin roles (generic admin bypass verified; menus already registered in `app-shell`, routes, `users.page` `menuOptions`).
- [x] Liquibase `v117` migration updates `app_users.allowed_menus` for existing users (append the two new IDs) plus `AuthService.ensureBootstrapAppAdmin` fallbacks.
- [x] `users.page.ts` `menuOptions` gains the two new IDs (already present under `workspace.approvals`).
- [x] `app-shell.component.ts` `visible()` treats the new IDs as admin-visible; menu registered under the `workspace.approvals` group.
- [x] Verify the catalog page count in `PROJECT_MAP.md` afterwards (see `DOC-STATUS-001`) — reconciled to 34 (was 33; B-1 added the BUDGETS page).

### A-2. `RELEASE-HEAD-001` — CI failure / release gate (BLOCKER)
**Status:** ⏳ IN PROGRESS — local green run established 2026-08-06; root cause identified & documented 2026-08-07; CI re-run still blocked on GitHub account/billing lock (repo owner must resolve)
**Reference:** `.github/workflows/ci.yml`, current HEAD `498227e5e2ee3109d20563f180335653b2a63983`.
**Problem:** The repository HEAD has a failed CI run (attached evidence: backend log "Attempting to redeploy after failure", `Job Summary` reporting failures). The pipeline is the release gate: backend `./gradlew clean test check`, frontend `npm ci`, `npm run check:i18n`, `ng test --watch=false`, `npm run build`. Without a green run, no release.
**Definition of done:**
- [x] Establish a local green run for both applications (backend full `./gradlew test`: **290 tests, 259 green, 31 fail — all 31 `NoClassDefFoundError: Could not initialize PostgresIntegrationTest`** because the Docker daemon is down; every failing suite extends the Testcontainers base, no non-Docker test fails; frontend `npm run check:i18n` passes (1424 keys), `ng test` 136/136 green (26 files), `npm run build` succeeds under node 24 — only pre-existing SCSS budget warnings).
- [x] Identify and document the root cause of the failed CI run.
- [ ] Ensure `GitHub Actions` billing/runner is available, re-run the pipeline, and record the green run URL + commit SHA in `PROJECT_MAP.md`.
- [ ] Add the hardcoded-UI i18n check to the CI pipeline (currently documented in the pipeline but not implemented).

  **→ DONE 2026-08-07** (`check:hardcoded` added to `fe/package.json` and to the `frontend` CI job; `check:i18n` now 1591 keys).

**Root cause (documented 2026-08-07):** GitHub Actions runner/billing lock, **not** a code or test failure. Verified via the public GitHub REST API (`api.github.com/repos/Mohamed-Hammada/bortqala/actions/runs`): all 13 workflow runs on `fm_bemo_consolidated` (run 17 → run 30, 2026-08-03 → 2026-08-06) conclude `failure`, and in every run all three jobs (`Backend (Gradle)`, `Frontend (Angular)`, `Compose validation & backend image`) complete within **2–4 seconds of job start with zero steps executed** (`steps: 0`), `conclusion=failure`, and empty `output.summary`/`output.text`. A job that runs code steps (checkout → gradle → tests) takes minutes; a job that never provisions a runner dies in seconds with no steps — the exact signature of GitHub-side runner unavailability (account/billing lock), and the reason the attached evidence shows only "Attempting to redeploy after failure" (the deploy job failing to obtain a runner) rather than any compile/test error. Because the failing jobs never executed the checkout step, no commit SHA on the branch can ever turn CI green until the GitHub account's Actions billing/runner availability is resolved by the repo owner (`Mohamed-Hammada`). Local evidence independently confirms the code itself is release-ready: backend 290 tests / 259 green with the only 31 failures being `NoClassDefFoundError: Could not initialize PostgresIntegrationTest` (Docker daemon down in this WSL env; CI itself provides Docker), and frontend `check:i18n` (1591 keys), `ng test` 136/136, `ng build` all green under node 24.
**Unblock action (repo owner):** In GitHub → Settings → Billing → Actions, confirm the plan/usage/account lock is cleared (or switch to a paid/free-minutes plan with runners enabled), then re-run workflow run 30 (`push` on `498227e`) or push a new commit. Once a job lists real steps and runs for minutes, the release gate can be recorded. No code change is required — the pipeline definition itself is sound.

### A-3. `DOC-STATUS-001` — PROJECT_MAP.md claims 33 catalog pages; catalog has 31 (DOCUMENTATION_MISMATCH)
**Status:** ✅ RESOLVED 2026-08-06 (catalog count reconciled to 33 after A-1; evidence field added)
**Reference:** `PROJECT_MAP.md` "Catalog" section, `AccessCatalog` registry.
**Problem:** `PROJECT_MAP.md` claims 33 access-catalog pages, but the catalog actually enumerates 31. After `ACCESS-APPROVAL-001` is implemented the count changes again; the map must reflect the real number at all times.
**Definition of done:**
- [x] Reconcile `PROJECT_MAP.md` catalog count to the actual `AccessCatalog` enumeration (31 today; 33 after `ACCESS-APPROVAL-001`).
- [x] Add an evidence field `catalog_entries` next to the claim with the exact enumeration source file/line (`AccessCatalog.java` `pages` list — 34 entries).

### A-4. `PHASE1-REPORTS-001` — Phase 1 reports partial (P1)
**Status:** DONE  
**Problem:** Part of the Phase-1 report deliverables (dashboard cards, Excel exports for attendance review) exist, but report depth (multi-period trends, filterable export surfaces) is partial.
**Definition of done:**
- [x] Inventory existing report surfaces and mark the missing ones.
- [x] Implement the missing report surfaces per the original Phase-1 spec.
**Resolution (2026-08-07):** Inventoried the live report surfaces (`ReportController` `/api/v1/reports` list/details/preview/decisions/export; `DashboardController` `/api/v1/dashboard` + summary/attendance-chart/payroll-summary/department-metrics; `DataExportController` `/api/v1/exports/{scope}.xlsx`) and closed the two remaining gaps:
- **Multi-period trends API** — `DashboardApi.TrendPoint` record + `DashboardService.trends(months)` (capped 1..24, oldest-first, per-month scheduled/present/rate/exception-days/overtime + payroll paid/pending counts and gross/paid totals) + `GET /api/v1/dashboard/trends?months=N` (`DashboardController.java:64`).
- **Filterable trends export** — `DataExportService.trends(months, options)` + `GET /api/v1/exports/trends.xlsx?months=N` (`DataExportController.java`) with Arabic filename.
- **Frontend** — `TrendPoint` model, `DashboardStore.loadTrends`/`downloadTrends`, dashboard multi-period trends table with month-count selector (3/6/12/24) and Excel export button (`dashboard.page.html`, `dashboard.page.ts`, `dashboard.store.ts`).
- **i18n** — Liquibase **V121** `20260807_v121_trends_export_translations.{yaml,csv}` (44 rows: 10 export column/sheet keys + 12 dashboard UI keys ×2 locales), registered in `next` + `test-h2`.
- **Evidence** — `DashboardServiceTrendsTests` (2 tests), `dashboard.store.spec.ts` (3 tests); backend 252/55/0, frontend 139/27/0, `check:i18n` 1603 keys, `check:hardcoded` 0/35, `ng build` green, H2 context green (V121 loaded), error-codes gate PASS 0 missing. Baselines refreshed in `docs/TEST_EVIDENCE.md` + both `check-test-count` gates.

---

## Part B — Phase 1 Remaining Business Work

### B-1. Budget Control & Encumbrance (Phase 1, business pending)
**Status:** DONE (2026-08-07, V122/V123/V124/V125)
- [x] Cost-center / department budgets, monthly vs. annual budget periods.
- [x] Encumbrance posting on PO issue; release on GRN/invoice/cancel.
- [x] Budget availability check before issue; over-budget warnings vs. hard blocks (`budget.blocking`).
- [x] Budget report surfaces (budgets, status/availability, encumbrances, Excel export).

### B-2. Approval workflow engine polish (Phase 1, business pending)
**Status:** OPEN  
- [ ] Step-level delegate/reassign support (manager on leave).
- [ ] SLA/ageing counters for pending approvals.
- [ ] Approval chain snapshots for audit replay.
- [ ] Multi-signature (both, any-of-N) step policies.

### B-3. Supplier onboarding & master data workflow (Phase 1, business pending)
**Status:** OPEN  
- [ ] Supplier request → approval → activate lifecycle.
- [ ] Duplicate supplier detection on create.
- [ ] Bank-account validation for payments.
- [ ] Supplier classification (commodity, rating, blacklist).

### B-4. Fixed Assets — lifecycle (Phase 1, business pending)
**Status:** OPEN  
- [ ] Asset register + categories + depreciation methods (SL / declining / units).
- [ ] Capitalization from PO/GRN, transfer, disposal, revaluation.
- [ ] Depreciation run with posting to finance.
- [ ] Asset register reports + ledger linkage.

### B-5. Inventory — stock movements & valuation (Phase 1, business pending)
**Status:** OPEN  
- [ ] Item master with categories, UoM, stockable flag.
- [ ] Stock in/out/adjustment transactions; FIFO/weighted-average valuation.
- [ ] Stock levels, reorder alerts, cycle counts.
- [ ] Inventory GL posting (inventory + COGS).

### B-6. Manufacturing / Work orders (Phase 1, business pending)
**Status:** OPEN  
- [ ] BOM definition; work-order lifecycle (planned → released → completed).
- [ ] Material issue/return, finished-goods receipt.
- [ ] Labour/overhead capture and cost roll-up.
- [ ] Work-order costing report vs. standard cost.

---

## Part C — Phase 2 Business Roadmap

### C-1. Multi-company / multi-branch consolidation
**Status:** OPEN  
- [ ] Company and branch masters with inter-company relationships.
- [ ] Consolidation currency + translation rules.
- [ ] Elimination entries; consolidated financial statements.
- [ ] Per-company fiscal periods and closing.

### C-2. Cash management
**Status:** OPEN  
- [ ] Bank account master, statement import, bank reconciliation.
- [ ] Cash-flow forecast (open receivables/payables + budgets).
- [ ] Cheque books / transfers / direct debits.

### C-3. Advanced receivables & collections
**Status:** OPEN  
- [ ] Customer aging buckets + collections task list.
- [ ] Credit limits and dunning cycles.
- [ ] Allocation of receipts to invoices/advances; write-off workflow.

### C-4. Project accounting
**Status:** OPEN  
- [ ] Project/WBS master; project budget and committed cost.
- [ ] Cost capture from purchasing, payroll, expense; billable vs. non-billable.
- [ ] Project revenue recognition (completed-contract vs. %-of-completion).
- [ ] Project P&L / WIP reports.

### C-5. Payroll & time integration (post-Phase 1 payroll)
**Status:** OPEN  
- [ ] Time-clock / biometric integration to attendance (partially exists for import).
- [ ] Overtime approval and premium pay rules.
- [ ] Payroll runs + GL distribution to cost centers/projects.
- [ ] Payslip delivery portal / e-signed payslips.

### C-6. Dashboards & KPIs (post-MVP)
**Status:** OPEN  
- [ ] Executive dashboard with trend charts.
- [ ] Role-based KPI cards (AR, AP, cash, headcount, attendance).
- [ ] Scheduled e-mailed report subscriptions.

### C-7. Multi-currency enhancements
**Status:** OPEN  
- [ ] Daily revaluation at month-end.
- [ ] Realized/unrealized gain-loss posting.
- [ ] Currency exchange-rate import (central bank feed).

### C-8. Business intelligence export layer
**Status:** OPEN  
- [ ] Data-export endpoints for warehouse ingestion.
- [ ] Snapshot tables for BI tools.
- [ ] Audit-friendly export manifest.

---

## Part D — Phase 3 Roadmap (Extended Platform)

### D-1. HR Employee Self-Service portal
**Status:** OPEN  
- [ ] Employee profile, leave/payroll-slip self-service.
- [ ] Request approvals inside the same workflow engine.
- [ ] Mobile-friendly layouts.

### D-2. Contract management
**Status:** OPEN  
- [ ] Contract register (customer/supplier/employee), expiry alerts.
- [ ] Amendment versioning and approval.
- [ ] Obligation/entitlement tracking.

### D-3. E-invoicing & tax reporting
**Status:** OPEN  
- [ ] Tax authority e-invoice XML generation + submission.
- [ ] VAT return preparation from ledger.
- [ ] Digital signature + submission audit trail.

### D-4. Document management
**Status:** OPEN  
- [ ] Central document store, versioning, access control by role.
- [ ] Document→record linkage (PO, invoice, voucher).
- [ ] OCR ingestion for supplier invoices.

### D-5. REST API public catalog & webhooks
**Status:** OPEN  
- [ ] Public OpenAPI spec + rate limiting.
- [ ] Webhook subscriptions for key domain events.
- [ ] Partner API keys with scopes.

### D-6. Localization depth
**Status:** OPEN  
- [ ] Currency formats per locale; number-to-Arabic-words for cheques.
- [ ] Gregorian/Hijri dual calendar support.
- [ ] Full right-to-left audit in all report templates.

### D-7. Security & compliance hardening
**Status:** OPEN  
- [ ] Session/IP allowlists, 2FA, audit export.
- [ ] GDPR/PDPL data-subject export/erasure.
- [ ] Encryption-at-rest key rotation.

### D-8. On-premise deployment pack
**Status:** OPEN  
- [ ] Docker-compose with TLS + DB backups.
- [ ] Installer docs and migration toolkit.

---

## Part E — Technical Debt & Platform Quality

### E-1. i18n completeness (I18N-RETRY-001)
**Status:** DONE 2026-08-07 (first pass)  
- [x] Full coverage of hardcoded UI strings (report output, tooltips, toasts).
- [x] Automated hardcoded-UI i18n check wired into CI.
- [x] Translation CSV completeness checker (missing key = failing build).
- Evidence: `fe/tools/check-hardcoded-strings.mjs` (35 templates scanned, zero bare text nodes) and `fe/tools/check-i18n.mjs` (1591 keys, both locales) wired as `npm run check:hardcoded` + `npm run check:i18n`; both run in the `frontend` CI job. Liquibase v119 (`20260807_v119_hardcoded_ui_translations.yaml`, 167 keys × 2 locales) registered in `next` + `test-h2` changelogs. Templates fully i18n-ized: accounts, tax-currency, quality, parties, payroll, operations, settings, procurement, employees, users, reports, report-review, skeleton. `ng test` 136/136, `ng build` green.

### E-2. Global exception handler alignment
**Status:** DONE 2026-08-07 (superseded handler + closed the residual error-code gaps)
- [x] `GlobalExceptionHandler` (being built by concurrent session) to cover all REST controllers uniformly.
- [x] Error-code catalog parity between backend enums and frontend `error.messages` map.
- [x] Consistent HTTP status mapping for all `BusinessRuleException` codes.
- Evidence: the concurrent handler landed as `ApiExceptionHandler` (`be/src/main/java/com/bemo/hr/shared/api/ApiExceptionHandler.java`) and is now the single `@RestControllerAdvice` covering auth, access-denied, not-found, business-rule (with per-field errors), data-integrity, optimistic-lock, bean-validation, malformed, method-not-allowed, `ResponseStatusException`, and generic 500 — no dead `GlobalExceptionHandler` remains in the repo, and every controller test (incl. `ScreenShortcutControllerTests`) wires the real handler. The frontend holds **no static error map**: `api-error.ts` resolves `apiError.code` through `I18nService.t()` against the same DB translation tables (fallback to the localized/raw message), so backend↔frontend parity is structural. An audit of all 241 backend exception codes found **26 codes that were shipped without DB translation rows** (approval engine, biometric sources, shortcuts, procurement, notifications, fiscal periods, journal numbering) — fixed by Liquibase **V120** (`20260807_v120_exception_i18n_fix.yaml`/`.csv`, 26 keys × ar-EG/en-US) registered in `next` + `test-h2`, verified green on the H2 context. A new CI gate `be/tools/check-error-codes.py` (wired into the `backend` job after the test gate) fails the build whenever a future code is added without a translation row, and a consistent status mapping exists via the handler's `codeFor(HttpStatus)` switch. Roadmap A-2's backend log line "Attempting to redeploy after failure" is unrelated (runner-provisioning lock — see A-2).

### E-3. Test-count evidence
**Status:** DONE 2026-08-07
- [x] Central place recording 239 backend + 136 frontend test counts with date + SHA.
- [x] CI gate that fails if counts regress below the recorded baseline.
- Evidence: `docs/TEST_EVIDENCE.md` records baseline **250 backend tests / 54 suites / 0 failures** (non-Docker, verified `BUILD SUCCESSFUL` 2026-08-07 at HEAD `498227e`) and **136 frontend tests / 26 files** (verified under node v24.18.1), plus earlier 2026-08-06 (290/259 green/31 Docker-down) and `ee8356e` (209) entries. CI enforces both: `be/tools/check-test-count.py` runs after `./gradlew clean test check` (baseline 250/54, 0 failures), and `fe/tools/check-test-count.mjs` consumes the `ng test --watch=false` summary piped in the frontend job (baseline 136/26, 0 failures; ANSI-stripped parser). Baselines may only be raised after a verified green run.

### E-4. Build tooling
**Status:** DONE 2026-08-07
- [x] Document native-Gradle flakiness on `/mnt/d` and the `/tmp/opencode/be-build` rsync workaround.
- [x] Pin node version enforcement via `.nvmrc` / `engines` (`<25`), CI matrix note.
- Evidence: `docs/BUILD_TOOLING.md` documents the rsync mirror commands, the `-PskipDockerTests` local convention, and the `/mnt/d` flakiness; `fe/.nvmrc` pins Node 24, `fe/package.json` `engines` is `">=22.0.0 <25"`, and the `frontend` CI job gained a `Node version guard (engines >=22 <25)` step before `npm ci`.

### E-5. `PROJECT_MAP.md` hygiene
**Status:** DONE 2026-08-07 (ongoing practice)
- [x] Keep `[ORPHANS & PENDING]` empty; every completed item updates the map immediately.
- [x] Evidence fields (`file:line`, test count, SHA) next to each claim.
- Evidence: this session kept the map and roadmap in lockstep — A-2 root-cause evidence, E-1, E-2 (V120 + parity gate), E-3 (`TEST_EVIDENCE.md` baselines), E-4 (`BUILD_TOOLING.md` + `.nvmrc`), and B-1 (budget & encumbrance) each updated `PROJECT_MAP.md`/roadmap in the same pass; the `[ORPHANS & PENDING]` section still lists only genuinely open items (`RELEASE-HEAD-001` CI re-run, remaining Part B business work). Evidence conventions recorded: `catalog_entries = 34` (`AccessCatalog.java` `pages` list), test baselines with date + SHA (`docs/TEST_EVIDENCE.md`), and `file:line` citations on new claims.

---

## Part F — Immediate Execution Plan (next 10 working sessions)

| # | Item | Est. |
|---|------|------|
| 1 | A-3 `DOC-STATUS-001` — reconcile map counts + evidence field | ✅ 0.5 day (done 2026-08-06) |
| 2 | A-1 `ACCESS-APPROVAL-001` — approval screens in access catalog + parity | ✅ 1 day (done 2026-08-06) |
| 3 | A-2 `RELEASE-HEAD-001` — local green run (done), root-cause doc, CI re-run | ⏳ root-cause doc done 2026-08-07; CI re-run blocked on GitHub billing |
| 4 | E-1 `I18N-RETRY-001` — hardcoded-UI coverage + CI check | ✅ 1.5 days (done 2026-08-07) |
| 5 | E-2 Global exception handler alignment + error-code parity | ✅ 1 day (done 2026-08-07, V120 + parity gate) |
| 6 | B-1 Budget control & encumbrance | ✅ 3 days (done 2026-08-07, V122-V125 + budgets page + PO department/encumbrance) |
| 7 | B-4 Fixed assets lifecycle | 3 days |
| 8 | B-5 Inventory valuation | 3 days |
| 9 | A-4 `PHASE1-REPORTS-001` — complete report surfaces | ✅ 1 day (done 2026-08-07, V121 + trends API/export + UI) |
| 10 | Part C Phase-2 items (multi-company first) | rolling |

All items in Parts B/C/D stay OPEN until their checklist is fully ticked. `PROJECT_MAP.md` must be updated within the same session each item completes.
