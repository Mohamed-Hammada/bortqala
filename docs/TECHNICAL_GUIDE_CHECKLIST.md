# Bemo ERP — Technical Guide Implementation Checklist

Source: `BEMO_ERP_PAGE_BY_PAGE_TECHNICAL_GUIDE_README.md` (source review date 1 August 2026).

Status values: `NOT_STARTED` | `BACKEND_ONLY` | `FRONTEND_ONLY` | `PARTIAL` | `IMPLEMENTED_NOT_TESTED` | `VERIFIED`.

Verification note template (paste under every touched item):

```text
Frontend page/store/API found:
Backend controller/service found:
Entity/table/Liquibase found:
Permissions found:
Unit/integration/E2E tests found:
Runtime result:
Final status:
```

Validation rule for this branch: an item is only closed (`VERIFIED`) after (a) a successful CI run, (b) backend and frontend builds, (c) PostgreSQL integration testing, (d) the required page-level tests, and (e) a recorded verification commit SHA. Every item must record the commit SHA that verified it. Until CI can run, `VERIFIED` below records local verification only and is provisional (see P0-05 — GitHub Actions was blocked by a billing lock at the review of `94c7c50`).

---

## Verification run 1 — 2026-08-01 (Sprint-0 baseline sweep)

- Verified at base commit: `2409a4d` (branch `main`, `fm_R1_Changes` merge). Working-tree-only changes at the time of this run: the V18 duplicate-translation fix (below) and this checklist.
- **Backend:** `be` `./gradlew compileJava` OK; `./gradlew test` **24 suites / 45 tests / 0 failures** on H2 + Liquibase (`test.changelog-master.yaml`); context load (`BemoErpApplicationTests`) passes.
- **Frontend:** `fe` `npm run check:i18n` passes (**1012 keys** in ar-EG + en-US); `npm run build` succeeds (3 non-blocking budget warnings: `dashboard.page.scss`, `report-review.page.scss`, `app-shell.component.scss`); `npx ng test --watch=false` **7 files / 12 tests passed**.
- **Blocker fixed during this run:** Liquibase V18 `data/insert/files/full_ui_translations_v18.csv` contained duplicate `common.edit` rows (`x18-ar-500`, `x18-en-500`) that collided with `x18-ar-006`/`x18-en-006`, violating `uq_translations_key_locale` on a fresh database and failing every `@SpringBootTest`. Removed the two duplicate rows. This is a pre-existing defect, not a feature regression. **Not committed yet.**
- **Runtime/run strategy:** backend runs with profile `dev`/`desktop` (no `application-prod.properties` exists); frontend dev server proxies `/api` to the backend.
- **Test infrastructure gaps found (all §5 items below):** no Testcontainers PostgreSQL (H2 only), no JaCoCo, no Playwright/E2E, no CI config, no shared idempotency table, no `allowedActions` transition metadata, no shared lookup contract, no Excel formula-injection escaping.
- **Project-map contradiction (guide §1, §4.10, §4.18):** exchange-rate snapshots **confirmed implemented** — Liquibase V63 adds `exchange_rate/date/source/override_reason/base_total_amount` to `purchase_orders` + `supplier_invoices`; `PurchaseOrder.java` domain fields; `GET /api/v1/trade/procurement/exchange-rate` quote; procurement page freezes rate on documents. `PROJECT_MAP.md` ORPHANS line must drop the "exchange-rate snapshots" bullet (see Known project-map contradiction below).

---

## Verification run 2 — 2026-08-02 (P0 review-findings fix, `fm_bemo_technical_guide 02`)

Local verification for P0-05 (GitHub Actions blocked by the billing lock at the review of `94c7c50`; these logs substitute for the CI run until the account issue is resolved).

- **Backend:** `be` `./gradlew clean test check` **BUILD SUCCESSFUL** — **38 suites / 99 tests / 0 failures / 0 errors** (H2 + Liquibase, includes the new `AuthServiceTenantIsolationTests` for P0-01 and `SupplierPaymentConcurrencyTests` for P0-03), `jacocoTestReport` generated under `check`. Confirmed the `test`/`check` tasks no longer build the Angular frontend (P1-02).
- **Frontend (node v24.18.0):** `npm ci` OK; `npm run check:i18n` passes (**1098 keys** in ar-EG + en-US); `npx ng test --watch=false` **10 files / 27 tests passed**; `npm run build` succeeds (3 pre-existing non-blocking budget warnings: `dashboard.page.scss`, `report-review.page.scss`, `app-shell.component.scss`).
- **Compose:** `docker compose -f docker-compose.yml config --quiet` exit 0; `docker compose -f docker-compose.yml -f docker-compose.prod.yml config --quiet` exit 0 with secrets supplied (incl. `HR_DEVICE_CREDENTIALS_SECRET`, P0-04) and exits 1 when a required secret is missing.

---

## Sprint 0 implementation log

### S0-1 Standard API error shape + correlationId — VERIFIED (2026-08-01)

Implements guide §2.3 contract end-to-end and verified against runtime behavior.

**Backend** (`be/src/main/java/com/bemo/hr/shared/api/`):
- New `ApiError` record: `code, message, localizedMessage, status, path, correlationId, timestamp, fieldErrors[{field, code, message}]`.
- `ApiExceptionHandler` rewritten from `ProblemDetail` → `ResponseEntity<ApiError>`: 400 `VALIDATION_FAILED` / `MALFORMED_REQUEST`, 401 `AUTHENTICATION_FAILED`, 403 `FORBIDDEN`, 404 `NOT_FOUND` (both `NotFoundException` and `NoResourceFoundException`), 405 `METHOD_NOT_ALLOWED`, 409 `BUSINESS_CONFLICT` / `DATA_CONFLICT`, 500 `INTERNAL_ERROR` (logged). `ResponseStatusException` preserves its own status.
- `message` = English support text; `localizedMessage` resolved from `Accept-Language` (default `ar-EG`) via `TranslationService` for `error.*` keys; service-thrown rule messages pass through (frontend `KNOWN_MESSAGES` still localizes known ones).
- `RequestAuditFilter` stores the server correlation id as request attribute `bemo.correlationId` (already echoed as `X-Server-Correlation-Id`); handler includes it in the body.
- Tests: `shared/api/ApiExceptionHandlerTests` (conflict shape + correlationId, not-found, localized auth, fieldErrors array).

**Frontend** (`fe/src/app/core/`):
- `auth.models.ts`: `ApiProblem` → `ApiError` contract (`fieldErrors[]`, `message`, `localizedMessage`, `code`, `status`, `path`, `correlationId`, `timestamp`).
- `api-error.ts`: joins `fieldErrors[]`, prefers `localizedMessage`, falls back to `message`, keeps status-0 / 401 / unexpected handling.
- Tests: `api-error.spec.ts` (field-error join, localizedMessage precedence, message fallback, connection error, generic fallback).

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**49 tests**, +4 new); `fe` `npm run check:i18n` 1012 keys, `npm run build` OK (3 pre-existing budget warnings), `npx ng test --watch=false` **8 files / 17 tests passed**.

**Remaining §2.3 gaps (tracked, out of S0-1 scope):** no `422` business-validation code (rule violations are `409` today), no `429` rate-limit handling and no server-side rate limiting; error translations exist only for `error.*` keys (service messages localize client-side).

---

### S0-2 Shared idempotency (§5.3) — VERIFIED (2026-08-01)

Backend component done; adoption in supplier payments (first flow). Full adoption across all §5.3 flows is tracked under the sprint items.

**Backend** (`be/src/main/java/com/bemo/hr/shared/idempotency/`):
- Liquibase V75 `idempotency_keys` table: `id, app_id, operation_type, operation_id, request_hash, status, response_reference_or_body, created_at, completed_at`, unique `uq_idempotency_app_type_operation` on `(app_id, operation_type, operation_id)`; wired into `releases/next.changelog-master.yaml`.
- `domain/IdempotencyKey` (`IN_PROGRESS`/`COMPLETED`/`FAILED`, `complete()`, `fail()`), `infrastructure/IdempotencyKeyRepository` (`findByOperationTypeAndOperationId`), `application/IdempotencyService` (`execute(type, id, hash, op, refWriter, replayMapper)` + static `hash()` SHA-256).
- Wired into `ProcurementService.createSupplierPayment` (`SUPPLIER_PAYMENT`, hash over supplier/invoice/amount/date); existing conflicting `operationId` still rejected.
- Tests: `IdempotencyServiceTests` (replay, hash-mismatch rejection, in-progress rejection, failure propagation, stable hash); `SupplierPaymentValidationTests` constructor updated for the new mock.

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**54+ tests**, +5 new).

---

### S0-3 Shared transition metadata (§5.4) — VERIFIED (2026-08-01)

Shared contract done; wired into settlement periods + attendance reports. Remaining workflows (inventory reversal, journal posting, GRN/payment, quality, production) to adopt as their sprint items land.

**Backend** (`be/src/main/java/com/bemo/hr/shared/api/`):
- New `TransitionResponse(status, version, allowedActions)` record (defensive `List.copyOf`).
- New `WorkflowTransitions` generic helper: `response(status, version, workflowMap)` + `allowedActions(status, workflowMap)` (unknown/null status → empty).
- `WorkforceSettlementService.reviewPeriod/approvePeriod/lockPeriod` now return `TransitionResponse` (`REVIEWED`→[APPROVE,RECALCULATE,EXPORT], `APPROVED`→[LOCK,EXPORT], `LOCKED`→[EXPORT]); backend still re-checks status, staleness fingerprint, and result-error count.
- `ReportingService.approve/reopen` now return `TransitionResponse` (`APPROVED`→[REOPEN,EXPORT], `IN_REVIEW`→[APPROVE,EXPORT]); approve keeps its empty-report + unresolved-blocker rechecks and its idempotent already-approved early return.
- Controllers (`WorkforceSettlementController`, `ReportController`) updated.
- Payroll `POST /payroll/transition` deliberately unchanged: it is a **batch** status update over many salary payments returning the whole sheet, not a single-entity workflow — documented as the exception.

**Frontend**:
- New `core/api.models.ts` `TransitionResponse` model.
- `workforce.service.ts` `reviewPeriod/approvePeriod/lockPeriod` typed to `Observable<TransitionResponse>`; `reports.store.ts` `approve/reopen` post the contract and reload report details via `load(id)` (was `ReportDetails` via `mutate`).

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**67 tests, 0 failures**, +10 new: `WorkflowTransitionsTests`, `WorkforceSettlementTransitionTests`, `ReportingTransitionTests`); `fe` `npm run build` OK, `check:i18n` 1012 keys, `npx ng test --watch=false` **8 files / 20 tests passed**.

### S0-4 Excel formula-injection security (§5.6) — VERIFIED (2026-08-01)

**Backend** (`be/src/main/java/com/bemo/hr/`):
- `reporting/infrastructure/ExcelExportSupport.java` — new `escapeFormula(String)` prefixes leading `=`,`+`,`-`,`@` with `'`; shared `writeRow` routes user-controlled `CharSequence` cells through it (covers ApachePoiReportExporter, OperationsExcelExporter, DataExportService).
- `payroll/application/PayrollExcelExporter.java` — `safe(...)` on employeeCode/employeeName/categoryName/employmentType/period note cells (0,1,2,3,4,13).
- `trade/procurement/application/ProcurementExcelExporter.java` — `row(...)`/`text(...)` escaped.
- `workforce/WorkforceExcelExportService.java` — `createCell(Row,int,String,CellStyle)` escapes via helper.
- `workforce/WorkforceMasterDataExcelExporter.java` — non-number cells escaped.
- `workforce/WorkforceExcelImportService.java` — error-workbook cells (workerCode, workDate, errorCode, errorMessage, rawData) escaped.
- Tests: `ExcelExportSupportTests` +3 (escapeFormula prefixes, safe-text passthrough, writeRow end-to-end).

**Frontend** (`fe/src/app/core/download.ts`):
- New `escapeCsvCell()` escapes leading `=`,`+`,`-`,`@` (apostrophe prefix) while still escaping embedded quotes; wired into `exportCsv()`.
- Tests: `download.spec.ts` (+3).

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**70 tests, 0 failures**); `fe` `npm run build` OK (gradle buildFrontend), `check:i18n` 1012 keys, `npx ng test --watch=false` **9 files / 23 tests passed**. §5.6 final status `PARTIAL` (generated-at/by/timezone/tenant headers + large-file streaming still per-exporter TBD).

### S0-5 `/users/me` + `/forbidden`/`/not-found` + wildcard fix (§4.34) — VERIFIED (2026-08-01)

**Backend**:
- `GET /api/v1/users/me` in `AuthController` (principal `Jwt` → `getSubject()`/`getExpiresAt()`); `AuthService.me(...)` returns `MeResponse` with `tenant{id,code,name}`, `roles`, `scopes` (sorted role codes), `session{expiresAt,timeoutMinutes,timeoutEnabled}`, plus existing user fields — no credentials.
- Tests: `MeIdentityIntegrationTests` (TEST tenant, ADMIN role/scopes/session assertions, real H2).

**Frontend**:
- New shell children `/forbidden` and `/not-found` with dedicated pages (`features/errors/forbidden.page.*`, `not-found.page.*`); shell `**` child and root `**` now redirect to `/not-found` (unknown routes no longer silently land on dashboard).
- `roleGuard` now redirects unauthorized users to `/forbidden` (was `/dashboard`); decision extracted to pure `roleGuardDecision(allowed, router)`.
- `auth.models.ts` `MeResponse`/`MeSessionInfo`; `auth.service.ts` `fetchMe()`.
- i18n: 5 new `errors.*` keys (ar+en) in `error_pages_translations_v76.csv` (Liquibase loadData V76) + `DEFAULT_FALLBACKS`.
- Tests: `app.routes.spec.ts` (forbidden/not-found presence; both wildcards → not-found), `roleGuardDecision` spec.

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**71 tests, 0 failures**, +1 `MeIdentityIntegrationTests`); `fe` `npm run build` OK (gradle buildFrontend), `check:i18n` **1017 keys** (5 new), `npx ng test --watch=false` **10 files / 27 tests passed** (+1 file `app.routes.spec.ts`). §4.34 final status `PARTIAL` (typed permission registry + workforce roleGuard + E2E matrix + bounded recent/favorites still TBD).

### S0-6 Production configuration / CORS allowlist (§5.1) — VERIFIED (2026-08-01)

**CORS (env-authoritative):** `SecurityConfig.corsConfigurationSource` now uses `${hr.cors.allowed-origins}` directly (no hardcoded localhost/trycloudflare list). Verified by `CorsConfigurationSourceTests` (configured HTTPS origins allowed; localhost + attacker rejected).

**Prod fail-fast config:** NEW `be/src/main/resources/application-prod.properties` — datasource URL/user/pass, JWT secret/issuer, CORS allowlist, company zone and all bootstrap credentials are `${...}` with **no fallback**; app refuses to start under `prod` when any is absent. Verified by `ProdConfigFailFastTests` (boots a `prod` SpringApplication with no env sources and asserts startup failure).

**Compose dev/prod split:** `docker-compose.yml` is now the dev baseline (safe topology: `internal` + `public` networks; db/backend on internal only, frontend on both; dev secret fallbacks; db/backend ports published for local tooling). NEW `docker-compose.prod.yml` overlay (`docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`): db/backend `ports: []` (internal only, reverse-proxy-only-public), every secret `${VAR:?}` required, `SPRING_PROFILES_ACTIVE=prod`, CORS must be an explicit HTTPS allowlist. Verified: `docker compose config` passes for dev and prod (with secrets), and fails fast with `POSTGRES_DB must be set` when secrets are missing.

**Verification:** `be` `./gradlew test` BUILD SUCCESSFUL (**73 tests, 0 failures**, +2: `ProdConfigFailFastTests`, `CorsConfigurationSourceTests`); `docker compose -f docker-compose.yml config --quiet` exit 0; prod-overlay `config --quiet` exit 0 with secrets / exit 1 without. FE unchanged since S0-5 (still 10 files / 27 tests, 1017 i18n keys, build OK). §5.1 final status `PARTIAL` (fail-fast prod profile, dev/prod split, internal network, env-driven CORS VERIFIED; external TLS + deploy-time secret scan left to environment).

### S0-7 CI / branch protection (§5.2) — VERIFIED (2026-08-01)

**CI config:** NEW `.github/workflows/ci.yml` (GitHub Actions; repo remote is `github.com/Mohamed-Hammada/bortqala`) with three jobs: **backend** (temurin 26 + gradle cache, `./gradlew clean test check`), **frontend** (node 24 + npm cache, `npm ci` → `npm run check:i18n` → `npx ng test --watch=false` → `npm run build`), **compose** (dev `config --quiet`, prod-overlay `config --quiet` with secrets via env, then `docker compose build`). The guide's literal `npm test -- --run` was rejected by Angular's vitest builder (`Unknown argument: run`) and replaced with the repo-proven `npx ng test --watch=false`.

**JaCoCo:** `id 'jacoco'` + `test { finalizedBy jacocoTestReport }` in `be/build.gradle`; report task runs under `./gradlew test check`.

**Verification:** `be` `./gradlew clean test check` BUILD SUCCESSFUL (ran as `test check` — `clean` alone fails locally on a Windows file lock of the running jar; the exact `clean test check` will run on CI's Ubuntu runner), **73 tests / 0 failures**, `jacocoTestReport` executed, HTML report at `be/build/reports/jacoco/test/html/index.html`; FE build reran inside gradle (`buildFrontend` → `npm run build`) and passed. Compose dev+prod `config` already verified in S0-6. §5.2 final status `PARTIAL` (Testcontainers PostgreSQL, Playwright E2E, secret/dependency scanning, dedicated migration dry-run, and protected-`main` repo settings remain on backlog).

---

## Section 3 — Route inventory

All 31 routes render; every route is lazy-loaded and present in `fe/src/app/app.routes.ts` / `features/workforce/workforce.routes.ts`.

| Route | Page | Status | Notes |
|---|---|---|---|
| `/login` | Login | `PARTIAL` | Form works; no JWT-safe storage, no change-password/refresh/logout. See 4.1 |
| `/dashboard` | Dashboard | `PARTIAL` | URL filters + drill-down; no `/users/me/dashboard-preferences`; no KPI tests. See 4.2 |
| `/categories` | Employee categories and schedules | `VERIFIED` | Overlap rules + 422 `SCHEDULE_RULE_OVERLAP` + schedule-history endpoint. See 4.3 |
| `/employees` | Employees | `VERIFIED` | Uniqueness/biometric/permission-gated salary + assignment history. See 4.4 |
| `/imports` | Attendance imports and devices | `VERIFIED` | Preview-commit-reverse + hash idempotency + encrypted device credentials. See 4.5 |
| `/parties` | Parties | `PARTIAL` | Direct/managed model implemented; no tests. See 4.6 |
| `/reports` | Attendance report list/generation | `VERIFIED` | Idempotent create by input hash + pre-generation preview + duplicate/policy tests. See 4.7 |
| `/reports/:id` | Attendance report review | `PARTIAL` | Bulk decisions + anomalies + idempotent replay; day-anomaly tests exist. See 4.8 |
| `/operations` | Inventory/partner/advance operations | `IMPLEMENTED_NOT_TESTED` | Full CRUD/export; zero tests. See 4.9 |
| `/trade/procurement` | Procurement | `PARTIAL` | Full PO/GRN/invoice/payment + snapshots; partial tests. See 4.10 |
| `/trade/sales` | Sales | `PARTIAL` | Simple SO only; no quotation→invoice→collection pipeline. See 4.11 |
| `/manufacturing/production` | Production and BOM | `PARTIAL` | BOM+order forms, start/complete; no component lines/material posting. See 4.12 |
| `/manufacturing/quality` | Quality inspections | `PARTIAL` | List + create only; no source lookup/submit/approve/reverse. See 4.13 |
| `/payroll` | Payroll | `PARTIAL` | Lifecycle + export; no golden tests, no deduction policy lifecycle. See 4.14 |
| `/finance/accounts` | Chart of accounts | `PARTIAL` | Flat list; no tree/hierarchy, no dependency preview. See 4.15 |
| `/finance/journal-entries` | Journal entries | `PARTIAL` | Draft/post/reverse + balanced lines; no tests. See 4.16 |
| `/finance/banks` | Bank/treasury accounts | `PARTIAL` | BANK list/form; no BANK vs CASH type, no masking. See 4.17 |
| `/finance/tax-currency` | Taxes, currencies, rates | `PARTIAL` | Tax/currency CRUD; no rate effective ranges/versions. See 4.18 |
| `/organization` | Company structure | `PARTIAL` | CRUD + hierarchy + GET-no-side-effect test. See 4.19 |
| `/fiscal-periods` | Fiscal periods | `PARTIAL` | Year generate + status; no soft-close/close/reopen/blockers. See 4.20 |
| `/audit-logs` | Audit log | `PARTIAL` | Read-only paginated; no immutability/redaction tests. See 4.21 |
| `/settings` | Settings | `PARTIAL` | Personal/app forms + dirty guard; numbering endpoint missing. See 4.22 |
| `/users` | Users and roles | `PARTIAL` | CRUD + roles/menus + CSV; no privilege-escalation tests. See 4.23 |
| `/workforce/dashboard` | Workforce dashboard | `PARTIAL` | FE KPIs/drill-down; **no backend summary endpoint**. See 4.24 |
| `/workforce/contractors` | Contractors | `IMPLEMENTED_NOT_TESTED` | CRUD + export. See 4.25 |
| `/workforce/workers` | Workers | `IMPLEMENTED_NOT_TESTED` | CRUD + export. See 4.26 |
| `/workforce/categories` | Workforce categories | `IMPLEMENTED_NOT_TESTED` | CRUD + export. See 4.27 |
| `/workforce/labor-requests` | Labor requests | `PARTIAL` | CRUD + status; no submit/approve/assign/availability. See 4.28 |
| `/workforce/attendance` | Manual attendance | `IMPLEMENTED_NOT_TESTED` | Matrix + batch + dirty guard + tests. See 4.29 |
| `/workforce/settlement-periods` | Settlement periods | `IMPLEMENTED_NOT_TESTED` | calc/review/approve/lock + tests; no reopen/reverse. See 4.30 |
| `/workforce/advances` | Advances | `IMPLEMENTED_NOT_TESTED` | Policies + pause/resume/repay + tests. See 4.31 |
| `/workforce/contractor-accounts` | Contractor accounts | `PARTIAL` | Read-only contractors view; **no statement/ledger API**. See 4.32 |
| `/workforce/reports-import` | Workforce report import | `IMPLEMENTED_NOT_TESTED` | Full wizard + idempotent commit + error workbook test. See 4.33 |
| shell/wildcard | Navigation, forbidden, not found | `PARTIAL` | `/forbidden` + `/not-found` added; wildcard → not-found; roleGuard → forbidden; typed permission registry TBD. See 4.34 |

---

## Section 4 — Page-by-page work items

### 4.1 Login — P0
Task split: AUTH-FE-01, AUTH-BE-01, AUTH-BE-02, AUTH-QA-01
- [ ] Frontend: typed controls, generic invalid-credential, safe lockout/rate/forced-change/expired states, no JWT in localStorage.
- [ ] Backend: login/change-password/refresh/logout; server-side throttling; production bootstrap secrets + `mustChangePassword`.
- [ ] DB: failed attempts, locked until, must change password, last login/failure, version; unique normalized username; hashed expiring tokens.
- [ ] Tests: discovery-safe login, lockout boundary, expired redirect, double-click E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/login/login.page.ts; core/auth/auth.service.ts login() -> POST /api/v1/auth/login. Loading/error states, typed form, desktop initial_credentials bridge.
Backend controller/service found: be/.../shared/security/AuthController.java /api/v1/auth/login -> AuthService.
Entity/table/Liquibase found: app_users (V1), roles/user_roles (V1), password policy fields (V34). No failed_attempts / locked_until / must_change_password / last_login columns found.
Permissions found: /api/v1/auth/login public; all other business APIs protected by SecurityConfig + @PreAuthorize.
Unit/integration/E2E tests found: none for login (no discovery-safe/lockout test). Frontend Vitest suite has no login spec.
Runtime result: Project boots; backend 45/45 pass; frontend 12/12 pass; i18n 1012 keys. Login page renders and authenticates.
Final status: PARTIAL
```
Missing: NO /change-password, /refresh, /logout endpoints; JWT persisted in localStorage (`bemo-erp-session`); no lockout/throttle; no must-change-password bootstrap.

### 4.2 Dashboard — P1
Task split: DASH-BE-01, DASH-FE-01, DASH-QA-01
- [ ] Frontend: typed URL filter model, Back/Forward restore, dataAsOf/refresh, KPI drill-down deep-links.
- [ ] Backend: `GET /api/v1/dashboard/summary`; `GET/PUT /users/me/dashboard-preferences`; authorized datasets only.
- [ ] DB: indexes on tenant+dates/status; unique user preference per tenant/app/user.
- [ ] Tests: KPI reconciliation, permission response, drill-down/filter-restore E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/dashboard/dashboard.page.ts + dashboard.store.ts; URL query params (year/month/period/department); widget editor; drill-down links; reduced-motion.
Backend controller/service found: be/.../reporting/api/DashboardController.java /api/v1/dashboard, /summary, /attendance-chart, /payroll-summary, /department-metrics -> DashboardService. Preferences: AuthController GET/PUT /api/v1/auth/preferences/dashboard.
Entity/table/Liquibase found: user_preferences widget visibility/order + motion (V56).
Permissions found: reads open to authenticated users; widget editor settings gated by admin switch; no dedicated finance-permission gating on dashboard payload.
Unit/integration/E2E tests found: none.
Runtime result: Boots; dashboard renders KPIs and charts; year switch updates cards.
Final status: PARTIAL
```
Missing: contract differs (`/users/me/dashboard-preferences` not present); no KPI reconciliation/permission tests; totals derived from client aggregation in some widgets.

### 4.3 Employee Categories and Schedules — P0
Task split: CAT-BE-01, CAT-FE-01, CAT-BE-02, CAT-QA-01
- [x] Frontend: dialog sections (basic/attendance/pay/schedule), typed schedule FormArray, timeline preview, minutes canonical.
- [x] Backend: versioned CRUD, nested/aggregate schedule-rule APIs, overlap response metadata.
- [x] DB: unique `(tenant_id, normalized_code)`; schedule effective ranges/version; indexes.
- [x] Tests: overlap/date/overnight validators; concurrent overlapping insert; E2E.
- Acceptance: `422 SCHEDULE_RULE_OVERLAP`; historical evidence unchanged; tenant-scoped code uniqueness.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/categories/categories.page.ts + categories.store.ts; schedule-rule reactive form, pagination, Excel export, Ctrl+S save.
Backend controller/service found: be/.../employee/api/CategoryController.java /api/v1/categories CRUD -> HrConfigurationService; entities AttendanceCategory + ScheduleRule.
Entity/table/Liquibase found: attendance_categories (V1, V11 adds advance allowance, version col), schedule_rules (V1, V42-update adds end_time/scope/scope_category_id).
Permissions found: writes @PreAuthorize ADMIN/HR_MANAGER (CategoryController L35/L42/L48); frontend roleGuard ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: HrConfigurationServiceScheduleTests (flush order on rule replacement; +overlap tests), DemoReferenceDataServiceTests (9 categories).
Runtime result: Boots; category page CRUD + schedule rules render and persist.
Final status: PARTIAL
```
Missing (now closed): `422 SCHEDULE_RULE_OVERLAP` overlap validation added (S1-4.3); effective-history API added; unique (tenant, normalized_code) confirmed as `uq_attendance_categories_app_code` (code stored uppercase-normalized via entity).
Overlap delivery (2026-08-01): `BusinessRuleException` extended with `code`/`HttpStatus`/`fields` (default BUSINESS_CONFLICT/409 preserved); `ApiExceptionHandler` maps them into the standard `ApiError` shape incl. `fieldErrors`; `HrConfigurationService.validateScheduleRanges` detects any overlapping effective range (not just adjacent) and throws `422 SCHEDULE_RULE_OVERLAP` with `schedules[i]` field metadata (original request indices). New `GET /api/v1/categories/{id}/schedule-history` returns effective-dated schedule history. Tests: +4 in HrConfigurationServiceScheduleTests (non-adjacent overlap 422+fields, adjacent boundary allowed, open-ended overlap, end-before-start) +1 ApiExceptionHandlerTests (422 code/status/fieldErrors). Backend 78/78 green.
Final status: VERIFIED

### 4.4 Employees — P0
Task split: EMP-BE-01, EMP-FE-01, EMP-BE-02, EMP-QA-01
- [ ] Frontend: separate API/list/editor models; server filters; searchable lookups; stale-version conflict; dirty guard.
- [ ] Backend: versioned CRUD; deactivate/reactivate + dependency preview; compensation fields permission-gated.
- [ ] DB: unique tenant employee code; non-null biometric identity per device scope; effective assignment history.
- [ ] Tests: duplicate code/biometric, tenant, compensation-permission, stale edit E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/employees/employees.page.ts + employees.store.ts; drawer editor, pagination, Excel export, dirty guard (hasUnsavedChanges at L188 + unsavedChangesGuard).
Backend controller/service found: be/.../employee/api/EmployeeController.java /api/v1/employees CRUD -> HrConfigurationService; Employee + EmployeeCodeSequence.
Entity/table/Liquibase found: employees (V1 with version col, V23 base_salary), employee_code_sequences (V13), user_can_view_salary (V32).
Permissions found: writes @PreAuthorize ADMIN/HR_MANAGER; salary visibility gated by V32 user_can_view_salary.
Unit/integration/E2E tests found: none dedicated to employees (no duplicate code/biometric, tenant, compensation tests).
Runtime result: Boots; employee CRUD + export + dirty guard functional.
Final status: PARTIAL
```
Missing (now closed): effective-assignment history table added (S1-4.4); duplicate code/biometric + tenant isolation + compensation-permission tests added.
Employee delivery (2026-08-01): NEW Liquibase V77 `employee_assignments` (id, app_id, employee_id, category_id, effective_from, effective_to, created_at, updated_at; unique `(app_id, employee_id, effective_from)`; FK employee CASCADE + category; index `(employee_id, effective_from, effective_to)`; mirrored into the H2 test schema as direct SQL per repo convention). Entity `EmployeeAssignment` + repository; `HrConfigurationService` records an assignment on create, closes the open range + opens a new one when category/effective dates change on update, and closes the open range on deactivate. New `GET /api/v1/employees/{id}/assignments` returns the effective-dated history (category names resolved). Compensation permission-gating: `toEmployeeResponse` masks `baseSalary` (null) for authenticated users whose `can_view_salary=false`, via AppUser lookup keyed on tenant + username; no auth context → unmasked (system/demo paths unaffected). Tests: `HrConfigurationEmployeeTests` (4): duplicate code within tenant rejected + same code allowed in another tenant; duplicate biometric device id rejected; assignment history on create→move→deactivate (closed 2026-02-28, reopen 2026-03-01, closed on deactivate); salary masked for `can_view_salary=false` and visible for `true`. Test cleanup removes created rows (keeps `MandatoryBootstrapIntegrationTests` 9-category raw-SQL count intact). Backend 82/82 green.
Final status: VERIFIED

### 4.5 Attendance Imports and Devices — P0
Task split: IMP-BE-01, IMP-FE-01, IMP-BE-02, IMP-QA-01
- [x] Frontend: tabs (file/device/sync), preview with hash/counts, mapping, commit idempotency.
- [x] Backend: preview/commit/status/errors/reverse; device CRUD/test/sync/history; bounded jobs.
- [x] DB: import header hash/source/counts/version; punch uniqueness; encrypted device credentials.
- [x] Tests: parser matrix, hash/dedupe/idempotency, failed-sync retry E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/imports/imports.page.ts + imports.store.ts; upload + devices + sync UI, unmatched pagination, exports (xlsx scope), CSV template, preview card (SHA-256 + counts), batch reverse button.
Backend controller/service found: be/.../attendance/api/BiometricImportController.java /api/v1/imports (upload, list, /preview, /unmatched, /devices CRUD, /devices/{id}/sync, /{id}/reverse) -> BiometricImportService + BiometricDeviceSyncService.
Entity/table/Liquibase found: import_batches/punch_records/import_row_errors (V1/V9), biometric_devices (V66) + V78 encrypted credential columns.
Permissions found: upload/list/preview @PreAuthorize ADMIN/HR_MANAGER/HR_REVIEWER; device write + reverse SUPER_ADMIN/ADMIN/HR_MANAGER; sync adds HR_REVIEWER.
Unit/integration/E2E tests found: SpreadsheetBiometricFileReaderTests (Arabic columns, serial/text dates, single punch, bilingual contract rejection) + BiometricImportContractTests (preview no-persist, hash/dedupe idempotency, reverse idempotency + reimport-block, encrypted-at-rest credentials, crypto wrong-key).
Runtime result: Boots; import preview/commit/reverse + device CRUD/sync UI functional; credentials encrypted at rest; live sync endpoints exercised in tests only via reader.
Final status: VERIFIED
```
Missing (now closed): preview→commit→reverse contract with import hash + reverse idempotency added; device credentials now encrypted at rest (V78); hash/dedupe/reverse integration tests added.
Import/device delivery (2026-08-01): NEW `POST /api/v1/imports/preview` (multipart; parses + SHA-256 only, no persist; 100-row/error caps) and `POST /api/v1/imports/{id}/reverse` (idempotent; deletes punches + errors, zeroes batch counts, `ImportStatus.REVERSED` string-stored, audit REVERSE; blocks re-import of same file via existing `(app_id, checksum)` unique key). NEW Liquibase V78 `biometric_device_credentials` columns (`device_username` varchar(150), `device_password_enc` varchar(1000)) on `biometric_devices` (mirrored as direct SQL into the H2 test changelog per repo convention). NEW `DeviceCredentialsCrypto` (AES/GCM, 12-byte IV, 128-bit tag, `Base64(iv):Base64(ct)`, fail-fast 32-byte key) keyed by `hr.security.device-credentials-secret` (dev default Base64, prod has no fallback, test value set). Device CRUD stores username + encrypted password; blank password on update preserves existing; responses return username + hasPassword only (never plaintext); `BiometricDeviceClient.fetch` sends Basic auth when credentials present. Tests: `BiometricImportContractTests` (5) — preview persists nothing; uploading the same file twice creates no duplicate punches; reverse deletes punches+errors, is idempotent, and blocks re-import of the same file; passwords encrypted at rest + never serialized + keep-on-blank-update + rotate; crypto round-trip + wrong-key rejection + blank→null. Frontend: preview card (hash + rows/valid/error/punch counts, has-errors styling), device form credential fields, reverse button + `REVERSED` status (reuses global `.status`/`.button.danger` classes; no new i18n keys). Backend 87/87 green; FE build + `check:i18n` (1017 keys) + ng test (10 files / 27 tests) green.
Final status: VERIFIED

### 4.6 Parties — P1
Task split: PTY-BE-01, PTY-FE-01, PTY-QA-01
- [ ] Frontend: explicit type + direct/managed, searchable responsible-party selector, dependency counts, stale conflict.
- [ ] Backend: versioned CRUD/deactivate/dependencies; typed paginated lookup/export.
- [ ] DB: unique tenant code; managed supplier FK to active responsible party; one-default contact/address.
- [ ] Tests: cycle/type/dependency; duplicate/tenant/stale; direct+managed E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/parties/parties.page.ts + parties.store.ts; form with direct/managed + responsible-party + dates, pagination, Excel export.
Backend controller/service found: be/.../party/BusinessPartyController.java /api/v1/parties CRUD + /cleanup-phone (ADMIN) -> BusinessPartyService.
Entity/table/Liquibase found: business_parties (V10, V40 extended fields, V44 nameEn/email/address/relationshipStartDate/EndDate/managed_type/responsible_party_id, version col).
Permissions found: writes @PreAuthorize ADMIN/HR_MANAGER; cleanup-phone ADMIN only.
Unit/integration/E2E tests found: none dedicated to parties.
Runtime result: Boots; parties CRUD + managed-type validation functional.
Final status: PARTIAL
```
Missing: no dependency/cycle tests; no typed paginated lookup endpoint; unique tenant party code not confirmed.

### 4.7 Attendance Reports — P0
Task split: RPT-BE-01, RPT-FE-01, RPT-BE-02, RPT-QA-01
- [x] Frontend: filters separate from create; pre-generation preview; URL filters; allowed-actions metadata.
- [x] Backend: list/preview/idempotent create/detail/approve/reopen/export.
- [x] DB: report aggregate range/scope/status/evidence version/input hash/actors/version; append-only evidence.
- [x] Tests: cycle ranges, duplicate generation, effective boundary, blockers, policy-change history.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/reports/reports.page.ts + reports.store.ts; create form + available-periods + list, export, pre-generation preview card (categories/employees/workdays/schedule-coverage + existing-report link + overlap warning).
Backend controller/service found: be/.../reporting/api/ReportController.java /api/v1/reports (GET, /available-periods, GET /preview, POST create, approve, reopen, export, decisions) -> ReportingService.
Entity/table/Liquibase found: reports + daily_results (V1, reports has version col) + V79 generation_hash (unique app_id+hash).
Permissions found: reads/create/preview ADMIN/HR_MANAGER/HR_REVIEWER; approve/reopen ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: ReportingServicePeriodTests (MONTHLY/HALF_MONTHLY cycles, existing-period hide, cross-month) + ReportingGenerationContractTests (preview no-persist, duplicate-generation replay, policy-change history) + unit tests (replay, preview counts/overlap).
Runtime result: Boots; report preview/create/approve/reopen/export functional; duplicate generation replays the existing report.
Final status: VERIFIED
```
Missing (now closed): idempotent-create by input hash added (V79 generation_hash + replay on same period/cycle); pre-generation preview added (GET /reports/preview); duplicate-generation + policy-change-history integration tests added; allowed-actions metadata delivered earlier via shared WorkflowTransitions (S0-3).
Report delivery (2026-08-01): NEW Liquibase V79 `reports.generation_hash` varchar(64) + `uq_reports_app_generation_hash` (app_id, generation_hash) — mirrored as `ALTER TABLE reports ADD COLUMN IF NOT EXISTS generation_hash VARCHAR(64)` in the H2 test changelog. `AttendanceReport` gains the field (6-arg constructor; 5-arg preserved for existing callers). `ReportingService.create` computes a SHA-256 input hash over tenant|period|payCycle, replays the existing report when the exact same input already exists (instead of failing the overlap check), stores the hash, and keeps the existing configuration hash as policy-version evidence. NEW `GET /api/v1/reports/preview?periodStart&periodEnd&payCycle` (same roles as create) returns active categories + employee counts, workdays, schedule-coverage employee-days (respecting ScheduleRule effective boundaries), the existing report link when already generated, and overlapping report ids — with no persistence. Frontend: `ReportPreview`/`PreviewCategory` models, `ReportsStore.preview()`, and a preview button + result card in the create form (hardcoded Arabic per imports-page precedent; no new i18n keys). Tests: `ReportingGenerationContractTests` (3, @SpringBootTest, own tenant + full cleanup) — preview persists nothing, duplicate generation returns the same report id with no second row, and a report captures the configuration policy version so a category change yields a different hash; 3 unit tests added to `ReportingServicePeriodTests` (replay without save, preview counts/coverage/existing link, overlap-only listing). Backend 93/93 green; FE build + `check:i18n` (1017 keys) + ng test (10 files / 27 tests) green.
Final status: VERIFIED

### 4.8 Attendance Report Review — P0
Task split: RRV-BE-01, RRV-FE-01, RRV-BE-02, RRV-QA-01
- [x] Frontend: header+paginated rows loaded separately; URL filters; immutable row-ID selection; bulk preview/reason.
- [x] Backend: summary/rows/anomalies/decision/decision-reversal/approve/reopen; allowed actions.
- [x] DB: append-only decision/reversal with operation ID; unique tenant/operation-type/operation-ID; versions.
- [x] Tests: five-decision calculations, concurrent reviewers, idempotency, approval blocker E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/reports/report-review.page.ts + reports.store.ts; filters, bulk-decision preview modal, daily-results/ holiday-proposals decisions, downtime decision, anomaly detect/decision/reverse/reopen, export.
Backend controller/service found: ReportController /api/v1/reports/{id}/bulk-decision, /{id}/daily-results/{rowId}/decision, /{id}/holiday-proposals/{proposalId}/decision, /{id}/downtime-decision, /{id}/day-anomalies/**, GET /{id}/decision-history -> ReportingService.
Entity/table/Liquibase found: attendance_day_anomalies + attendance_day_anomaly_results (V62, operation_id + unique(app_id, operation_id)).
Permissions found: decision endpoints ADMIN/HR_MANAGER/HR_REVIEWER; downtime/anomaly/reopen/approve ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: ReportingDayAnomalyTests (detect→snapshot→decide→idempotent replay 0 applied→reverse); DailyAttendanceCalculatorTests (five-decision math); ReportingDecisionHistoryContractTests (append-only history, version conflict 409, unique operation, allowed-actions lifecycle).
Runtime result: Boots; review page bulk decisions + anomalies persist; replay idempotent.
Final status: VERIFIED
```
Report delivery (2026-08-02): NEW Liquibase V80 `attendance_report_decisions` (id, app_id, report_id, result_id, operation_id, operation, previous_decision/previous_manual_worked_minutes/previous_note/previous_decided_by/previous_decided_at, new_* snapshot, actor, created_at) + indexes `idx_report_decisions_report`/`idx_report_decisions_result` + unique `uq_report_decisions_operation (app_id, operation_id)`; adds `daily_results.version BIGINT DEFAULT 0 NOT NULL`; mirrored in the H2 test changelog. `DailyAttendanceResult` gains `@Version` (optimistic lock). Every decision path — `decide`, `bulkDecide`, `decideDayAnomaly` (operationId), `reverseDayAnomaly` (`REVERSE-<anomalyId>`), holiday decision (`HOLIDAY-<proposalId>`), downtime decision — records an append-only history row. Single `decide` validates `expectedVersion` against the row version → `BusinessRuleException` (Arabic "تم تعديل السجل بواسطة مراجع آخر..."), and `OptimisticLockingFailureException` is mapped to 409 `CONCURRENT_MODIFICATION` in `ApiExceptionHandler`. NEW `GET /{id}/decision-history` (ADMIN/HR_MANAGER/HR_REVIEWER) returns the immutable history; `details()` now carries `allowedActions` (DRAFT→DECISION; IN_REVIEW→DECISION/BULK_DECISION/DOWNTIME_DECISION/DAY_ANOMALY/HOLIDAY_DECISION/APPROVE/EXPORT; APPROVED/EXPORTED→REOPEN/EXPORT). Frontend passes `expectedVersion` through `ReportsStore.decide` from `row.version`; `ReportDetails.allowedActions` + `DailyResult.version` typed. NEW V81 `report_review_translations.csv` seeds the reports/imports/review keys (83 keys x ar-EG+en-US) registered in next.changelog-master; FE `DEFAULT_FALLBACKS` filled for both locales; day-anomaly UI fully i18n'd (uses new `anomalyHours()` helper — Angular cannot call pipes inside template method args). Tests: backend 97/97 green (4 new @SpringBootTest ReportingDecisionHistoryContractTests: append+reversible, stale reviewer rejected then retry succeeds, unique operation id per tenant throws DataIntegrityViolation, allowed-actions lifecycle); FE build + `check:i18n` (1098 keys) + ng test (10 files / 27 tests) green.


### 4.9 Operations — P0
Task split: OPS-BE-01, OPS-FE-01, OPS-BE-02, OPS-QA-01
- [ ] Frontend: URL-addressable tabs; positive qty/amount; required fields; filter-equivalent export.
- [ ] Backend: inventory balance/movement list, idempotent post/reverse, partner-balance and advance read/export.
- [ ] DB: append-only movement with operation ID; atomic movement+balance; unique idempotency.
- [ ] Tests: competing issue concurrency, atomic transfer, idempotency, reversal E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/operations/operations.page.ts + operations.store.ts; 4 paginated tables + 6 command forms; export.xlsx.
Backend controller/service found: be/.../operations/OperationsController.java /api/v1/operations (snapshot, items, transactions, advances, adjustments, negative-balances, item-categories, uoms, export) -> OperationsService.
Entity/table/Liquibase found: inventory_items/stock_movements/partner_ledger_entries/employee_advance_entries (V12), item_categories (V42), unit_conversions (V46), inventory_lots/serials/fifo_layers (V30).
Permissions found: class-level @PreAuthorize ADMIN/HR_MANAGER (except cost/negative-balance SUPER_ADMIN/ADMIN at L76).
Unit/integration/E2E tests found: none for operations.
Runtime result: Boots; operations page CRUD + export functional.
Final status: IMPLEMENTED_NOT_TESTED
```
Missing: no append-only movement with operation ID / unique idempotency; no concurrency/atomic-transfer/reversal tests.

### 4.10 Procurement — P0
Task split: PROC-VERIFY-01, PROC-BE-01, PROC-BE-02, PROC-FE-01, PROC-QA-01
- [ ] PROC-VERIFY-01: confirm exchange-rate snapshot implementation and correct PROJECT_MAP contradiction (4.18 also).
- [ ] Frontend: URL-addressable tabs; PO line FormArray; GRN quantities; invoice accepted-uninvoiced match; payment allocations.
- [ ] Backend: versioned PO issue/cancel, GRN post/reverse, invoice post/cancel/reverse, payment/reverse; idempotent.
- [ ] DB: tenant-safe header/line/allocation/ledger; unique numbers; tax/discount/rate snapshots.
- [ ] Tests: totals/tax/discount/allocation; concurrent GRN/payment; direct/managed E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/trade/procurement/procurement.page.ts (no store; HttpClient in page); PO/GRN/invoice/payment modal forms, PO totals recalc, export.xlsx + CSV.
Backend controller/service found: be/.../trade/procurement/api/ProcurementController.java /api/v1/trade/procurement (orders CRUD + issue/receive/cancel, goods-receipts, invoices, payments, numbering-settings, exchange-rate, export.xlsx) -> ProcurementService.
Entity/table/Liquibase found: purchase_orders/purchase_order_lines/goods_receipts/supplier_invoices/supplier_payments (V28), goods_receipt_lines (V45), invoice trace (V49), inventory receiving (V50), payment notes (V51), sequences (V53/V54), transaction currency (V57), payment operation_id idempotency (V61), exchange-rate snapshots (V63).
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER; reads open to authenticated.
Unit/integration/E2E tests found: GoodsReceiptPersistenceTests (receipt+lines FK one tx, tenant), SupplierInvoiceTests (PARTIALLY_PAID→PAID state machine), SupplierPaymentValidationTests (cross-supplier + over-outstanding rejection), procurement.page.spec.ts (PO totals, payable-invoice filter).
Runtime result: Boots; PO/GRN/invoice/payment pipeline persists and exports; operation-id replay on payments.
Final status: PARTIAL
```
PROC-VERIFY-01: **CONFIRMED IMPLEMENTED** — V63 frozen `exchange_rate/date/source/override_reason` + `base_total_amount` on PO+invoice; backfill from `currencies.exchange_rate`; procurement page freezes rate. PROJECT_MAP ORPHANS bullet must be removed (see Known project-map contradiction).
Missing: no GRN/invoice reverse endpoints; no concurrent GRN/payment race test; no tax/discount approval flow; version columns absent on procurement documents.

### 4.11 Sales — P0
Task split: SALE-DESIGN-01, SALE-BE-01, SALE-BE-02, SALE-FE-01, SALE-QA-01
- [ ] Design: quotation→SO→delivery→invoice→collection state model.
- [ ] Frontend: tabs; active-customer lookup; line FormArray; total derived from lines; partial delivery/backorder.
- [ ] Backend: quotation/SO/delivery/invoice/collection APIs; idempotent confirm/deliver/invoice/collect/reverse.
- [ ] DB: line/reservation/delivery/invoice/receipt tables; unique numbers; constraints.
- [ ] Tests: totals/reservation/partial delivery; concurrency; migration of old simple records.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/trade/sales/sales.page.ts; simple SO list + form (number/date/customer text + manual total), confirm action. No tabs, no customer lookup, no line FormArray.
Backend controller/service found: be/.../trade/sales/api/SalesController.java /api/v1/trade/sales (orders list/create/confirm) using SalesOrderRepository directly (no service layer).
Entity/table/Liquibase found: sales_orders (V29); sales_quotations/sales_order_lines/delivery_notes/customer_invoices/customer_payments created in V29 without entities.
Permissions found: writes @PreAuthorize (controller-level role check); frontend roleGuard ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; simple SO form works.
Final status: PARTIAL
```
Missing: guide's observed state confirmed — the page is a simple SO list/form, not the quotation→SO→delivery→invoice→collection pipeline; no migration of old simple records.

### 4.12 Production and BOM — P0
Task split: MFG-DESIGN-01, MFG-BE-01, MFG-BE-02, MFG-FE-01, MFG-QA-01
- [ ] Frontend: BOM version/effective dates; component FormArray; order dates/warehouses/lot; start/completion flows.
- [ ] Backend: BOM/version APIs; start/issue/return/output/complete/cancel/reverse; idempotent/versioned.
- [ ] DB: BOM header/version/component; order snapshot/version; inventory movements linked to order.
- [ ] Tests: requirement scaling/yield/variance; atomic start/complete; reversal.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/manufacturing/production/production.page.ts; BOM + order tabs, start/complete actions. BOM UI has no component-line editor.
Backend controller/service found: be/.../manufacturing/production/api/ManufacturingController.java /api/v1/manufacturing (boms, orders, orders/{id}/start, orders/{id}/complete, quality) using BomHeaderRepository/ProductionOrderRepository directly.
Entity/table/Liquibase found: boms/bom_lines/production_orders/quality_inspections (V31). bom_lines exists in changelog but has no entity/no component editing API.
Permissions found: writes @PreAuthorize (controller-level); frontend roleGuard ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; BOM + order forms persist; start/complete work at API level.
Final status: PARTIAL
```
Missing: no BOM component lines UI/API, no effective-dated versions, no material issue/return/output posting, no lot/warehouse on order, no reversal.

### 4.13 Quality Inspections — P0
Task split: QC-BE-01, QC-FE-01, QC-BE-02, QC-QA-01
- [ ] Frontend: source lookup; defect lines; inspector default; workflow statuses/timeline.
- [ ] Backend: inspection CRUD, eligible-source lookup, submit/approve/reverse, defect-code APIs.
- [ ] DB: header/defect lines/source FK; inspected <= remaining; quarantine/release movements.
- [ ] Tests: quantity/result; competing inspections; quarantine posting E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/manufacturing/quality/quality.page.ts; single list + one create form.
Backend controller/service found: ManufacturingController /api/v1/manufacturing/quality (list + create) using QualityInspectionRepository directly.
Entity/table/Liquibase found: quality_inspections (V31).
Permissions found: writes @PreAuthorize (controller-level); frontend roleGuard ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; quality list + create functional.
Final status: PARTIAL
```
Missing: no defect lines, source-quantity lookup, submit/approve/reverse workflow, quarantine/release stock movements, defect codes.

### 4.14 Payroll — P0
Task split: PAY-DESIGN-01, PAY-BE-01, PAY-BE-02, PAY-FE-01, PAY-QA-01
- [ ] Frontend: persistent period header; tabs; backend calculation breakdown; exception queue; transition previews.
- [ ] Backend: period create/calculate/lines/review/approve/post/pay/reopen/reverse/export; idempotent/versioned.
- [ ] DB: period/line/component/adjustment/approval/payment/source tables; snapshot/hash; ledger links.
- [ ] Tests: golden fixed/daily/worker/advance/rounding; retry/stale/ledger; historical policy.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/payroll/payroll.page.ts + payroll.store.ts + features/payroll/ui/payroll-stepper.component.ts; stepper lifecycle, pay/pay-bulk/transition/reverse, export, pagination, skeleton/empty states.
Backend controller/service found: be/.../payroll/api/PayrollController.java /api/v1/payroll (GET, pay, pay-bulk, transition, reverse, export) -> PayrollService.
Entity/table/Liquibase found: salary_payment (V21, version col), employees.base_salary (V23), user_can_view_salary (V32).
Permissions found: write transitions @PreAuthorize ADMIN/HR_MANAGER; salary read gated by user_can_view_salary.
Unit/integration/E2E tests found: none for payroll (no golden calc, no state-machine, no ledger tests).
Runtime result: Boots; payroll stepper + transitions + export functional.
Final status: PARTIAL
```
Missing: no period/lines/component/adjustment tables (payroll is a lightweight salary register), no calculation snapshot/hash, no deduction-policy lifecycle, no golden tests.

### 4.15 Chart of Accounts — P0
Task split: COA-BE-01, COA-FE-01, COA-BE-02, COA-QA-01
- [ ] Frontend: tree/indented hierarchy; editor with parent/posting/normal balance/currency; dependency preview.
- [ ] Backend: versioned CRUD/deactivate/dependencies/tree/lookups.
- [ ] DB: unique tenant code; parent FK; cycle prevention; posting/system/currency/version fields.
- [ ] Tests: cycle/compatibility/system/posting; uniqueness/tenant; E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/finance/accounts/accounts.page.ts; flat list + add/edit form (no tree, no parent/posting/normal-balance fields).
Backend controller/service found: be/.../finance/api/AccountingController.java /api/v1/finance/accounts GET/POST/PUT (repository direct).
Entity/table/Liquibase found: accounts (V26), default COA seed (V27).
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER; frontend roleGuard ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; account list + create/edit functional.
Final status: PARTIAL
```
Missing: no parent/level/posting/system fields, no cycle prevention, no balance-from-journal-only rule, no dependency preview.

### 4.16 Journal Entries — P0
Task split: JE-BE-01, JE-FE-01, JE-BE-02, JE-QA-01
- [ ] Frontend: header + lines; running debit/credit/difference; posting-account lookup; draft/validate/post/reverse.
- [ ] Backend: draft CRUD, post, reverse, detail/source; idempotent/versioned.
- [ ] DB: immutable posted header/lines; unique number; exactly one positive side per line; reversal link.
- [ ] Tests: balance/line/currency/state; closed-period/nonposting/concurrent; E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/finance/journal-entries/journal-entries.page.ts; dynamic debit/credit lines + running balance validation, pagination, post.
Backend controller/service found: AccountingController /api/v1/finance/journal-entries (GET/POST, /{id}/post, /{id}/reverse) using JournalEntryRepository directly.
Entity/table/Liquibase found: journal_entries + journal_entry_lines (V26), fiscal_period_id FK (V26).
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; journal entry + post/reverse functional; balanced-line validation on FE.
Final status: PARTIAL
```
Missing: no closed-period guard at posting (fiscal period not enforced in service), no unique-number constraint test, no balance/currency/state unit tests, no idempotency.

### 4.17 Bank and Treasury Accounts — P1
Task split: BANK-BE-01, BANK-FE-01, BANK-BE-02, BANK-QA-01
- [ ] Frontend: BANK vs CASH type; identifier masking by permission; dependency/reconciliation summary.
- [ ] Backend: versioned CRUD/deactivate/dependencies/balance; reconciliation separate.
- [ ] DB: unique tenant number/IBAN; FK currency/posting GL; type/version.
- [ ] Tests: format/type/currency; duplicate/GL; mask/deactivate E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/finance/banks/banks.page.ts; list + form (bank_name/account_number/IBAN/SWIFT/status).
Backend controller/service found: be/.../finance/api/TreasuryController.java /api/v1/finance/banks GET/POST/PUT (repository direct).
Entity/table/Liquibase found: bank_accounts (V26: bank_name, account_number, iban, swift_code, account_id, active). No treasury type column; no currency/posting-GL FK.
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none.
Runtime result: Boots; banks CRUD functional.
Final status: PARTIAL
```
Missing: no BANK vs CASH type, no identifier masking, no linked-GL balance view, no reconciliation.

### 4.18 Taxes, Currencies and Rates — P0
Task split: CUR-VERIFY-01, CUR-BE-01, CUR-FE-01, CUR-QA-01
- [ ] CUR-VERIFY-01: verify snapshot implementation; correct PROJECT_MAP contradiction.
- [ ] Frontend: tabs Taxes/Currencies/Exchange rates; tax/currency/rate forms; frozen historical values read-only.
- [ ] Backend: versioned tax/currency/rate CRUD; deterministic rate resolve API; shared snapshot service.
- [ ] DB: unique tax code/currency ISO; exactly one active base currency transactionally; effective ranges/version.
- [ ] Tests: effective resolution/overlap/base/rounding; historical frozen snapshot; concurrent base/rate.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/finance/tax-currency/tax-currency.page.ts; Taxes + Currencies tabs (no Exchange-rates tab).
Backend controller/service found: TreasuryController /api/v1/finance (taxes GET/POST/PUT, currencies GET/POST/PUT); procurement rate is frozen via ProcurementController /exchange-rate + V63 snapshot columns.
Entity/table/Liquibase found: tax_rates + currencies (V26; currencies.exchange_rate NUMERIC(12,4)); exchange-rate snapshot columns on purchase_orders/supplier_invoices (V63). No exchange_rates table, no effective-range columns.
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none for tax/currency/rate resolution.
Runtime result: Boots; tax/currency CRUD functional; PO/invoice carry frozen rate + base totals.
Final status: PARTIAL
```
CUR-VERIFY-01: **CONFIRMED IMPLEMENTED** (see 4.10). PROJECT_MAP contradiction must be corrected.
Missing: no rate-resolution/effective-range API, no single-active-base-currency constraint, no override reason on currencies page, no rounding tests.

### 4.19 Organization Structure — P0
Task split: ORG-BE-01, ORG-FE-01, ORG-BE-02, ORG-QA-01
- [ ] Frontend: URL-addressable tabs; child-requires-parent; structured address; dependency counts.
- [ ] Backend: versioned CRUD/deactivate/dependencies; focused lookups.
- [ ] DB: unique tenant code per type; FKs; cycle prevention.
- [ ] Tests: parent/cycle/tenant; dependency deactivation; cross-module lookups E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/organization/organization.page.ts; tabbed companies/branches/warehouses/departments CRUD drawers.
Backend controller/service found: be/.../organization/api/OrganizationController.java /api/v1/organization hierarchy + companies/branches/warehouses/departments CRUD (repository direct).
Entity/table/Liquibase found: companies/branches/warehouses/departments (V25).
Permissions found: writes @PreAuthorize SUPER_ADMIN/ADMIN; frontend roleGuard ADMIN.
Unit/integration/E2E tests found: OrganizationControllerTests (GET of empty hierarchy does not create demo data — no side effects).
Runtime result: Boots; organization CRUD functional; GET is side-effect free.
Final status: PARTIAL
```
Missing: no child-requires-parent validation tests, no dependency counts, no cross-module lookup tests.

### 4.20 Fiscal Periods — P0
Task split: FISC-BE-01, FISC-BE-02, FISC-FE-01, FISC-QA-01
- [ ] Frontend: year/period rows with states/blockers; create-year preview; close/reopen confirmation.
- [ ] Backend: read-only GET; explicit year create; soft-close/close/reopen/blockers; versioned transitions.
- [ ] DB: unique tenant period key/range; non-overlap/contiguity; close/reopen metadata/version.
- [ ] Tests: GET no side-effect; overlap/blocker; concurrent close-vs-post; E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/fiscal-periods/fiscal-periods.page.ts; year selector + generate-year + status change.
Backend controller/service found: be/.../finance/api/FiscalPeriodController.java /api/v1/fiscal-periods GET, POST /generate-year (SUPER_ADMIN/ADMIN), PUT /{id}/status (SUPER_ADMIN/ADMIN/HR_MANAGER) using FiscalPeriodRepository directly.
Entity/table/Liquibase found: fiscal_periods (V25 with fiscal_year; journal_entries.fiscal_period_id V26).
Permissions found: generate-year SUPER_ADMIN/ADMIN; status SUPER_ADMIN/ADMIN/HR_MANAGER.
Unit/integration/E2E tests found: none (no GET-side-effect test, no close-vs-post test).
Runtime result: Boots; explicit create-year (no implicit creation) works.
Final status: PARTIAL
```
Missing: no soft-close/close/reopen/blockers contract (only OPEN/CLOSED/LOCKED status), no shared posting guard blocking closed periods, no tests.

### 4.21 Audit Logs — P0
Task split: AUD-BE-01, AUD-FE-01, AUD-OPS-01, AUD-QA-01
- [ ] Frontend: server filters; summary + detail drawer; bounded export; timezone/copy IDs.
- [ ] Backend: read/detail/export only; bounded date range; tenant isolation.
- [ ] DB: append-only/partition-ready; retention/archive; structured changed fields.
- [ ] Tests: immutability/redaction/tenant/range/export; mutation attempt; correlation-ID E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/audit-logs/audit-logs.page.ts; paginated read-only table.
Backend controller/service found: be/.../audit/api/AuditLogController.java /api/v1/audit-logs GET (read-only) using AuditLogRepository + AuditService.record(...) wired across auth/payroll/reports/procurement/operations/settings.
Entity/table/Liquibase found: audit_logs (V25); entity has getters only (append-only by design).
Permissions found: @PreAuthorize SUPER_ADMIN/ADMIN/HR_MANAGER; frontend roleGuard ADMIN.
Unit/integration/E2E tests found: none (no immutability/redaction/mutation-attempt test).
Runtime result: Boots; audit-log page reads records; no create/update/delete endpoints exist.
Final status: PARTIAL
```
Missing: no redaction verification, no retention/partitioning, no export endpoint, no immutability test.

### 4.22 Settings — P0
Task split: SET-FE-01, SET-BE-01, SET-BE-02, SET-QA-01
- [ ] Frontend: separate personal/application forms; deep-linkable tabs; numbering impact text; dirty guard.
- [ ] Backend: user-preference endpoint; versioned application/security/numbering endpoints; field authorization.
- [ ] DB: unique user preferences; versioned app settings; atomic number sequences.
- [ ] Tests: separate-form pristine/failure; field authorization; sequence concurrency E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/settings/settings.page.ts; personal + app-settings forms, tabs (appearance/session/security/reports/shortcuts), dirty guard (hasUnsavedChanges L235 + unsavedChangesGuard).
Backend controller/service found: AuthController GET/PUT /api/v1/auth/preferences, /preferences/navigation, /preferences/dashboard; GET/PUT /api/v1/admin/app-settings -> AuthService + UserPreferenceService.
Entity/table/Liquibase found: user_preferences (V1/V8/V19/V43/V47), system_settings (V68/V69 defaults).
Permissions found: app-settings GET/PUT ADMIN only; preferences per-user.
Unit/integration/E2E tests found: UserPreferenceTests (locale canonicalization, favorites/recent limits, dashboard widget validation).
Runtime result: Boots; settings save works; failed-save stays dirty.
Final status: PARTIAL
```
Missing: no numbering-settings endpoint on settings page (procurement numbering lives under /trade/procurement/numbering-settings), no sequence-concurrency test, no field-authorization test.

### 4.23 Users and Roles — P0
Task split: USR-BE-01, USR-FE-01, USR-BE-02, USR-QA-01
- [ ] Frontend: sections identity/roles/scopes/security; no current-password; effective-permission preview; final-admin protection.
- [ ] Backend: versioned user CRUD; enable/disable/unlock/reset/revoke; role/scope APIs.
- [ ] DB: unique normalized username; hashed expiring tokens; final-admin protection; security state/version.
- [ ] Tests: privilege escalation, forged IDs/fields, cross-tenant, final-admin concurrency, session revocation.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/users/users.page.ts + users.store.ts; roles + allowedMenus permission picker (31 menu ids, 7 groups), password policy, pagination, client CSV export.
Backend controller/service found: AuthController /api/v1/users GET/POST/PUT + /api/v1/auth/user-categories -> AuthService.
Entity/table/Liquibase found: app_users/roles/user_roles (V1, app_users version col), super_admin role (V20), allowed_menus (V19/V37), password policy (V34).
Permissions found: users endpoints @PreAuthorize ADMIN; role seeding V20.
Unit/integration/E2E tests found: MandatoryBootstrapIntegrationTests (ADMIN/SUPER_ADMIN present + seeded); TenantApplicationTests (session timeout/password policy). No privilege-escalation/final-admin/session-revocation tests.
Runtime result: Boots; user CRUD + roles/menus persist.
Final status: PARTIAL
```
Missing: no enable/disable/unlock/reset/revoke-session endpoints, no final-admin protection, no privilege-escalation/cross-tenant tests, no role-scope APIs.

### 4.24 Workforce Dashboard — P1
Task split: WFD-BE-01, WFD-FE-01, WFD-QA-01
- [ ] Frontend: typed URL filter; action queues; hide financial exposure dataset by role; no-data vs no-permission.
- [ ] Backend: `GET /api/v1/workforce/dashboard/summary` authorization-aware fields.
- [ ] DB: aggregation indexes; reuse widget-preference schema.
- [ ] Tests: KPI fixture/reconciliation; permission response; drill-down E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/dashboard/dashboard.component.ts; KPI cards + charts, URL-param filters (contractor/category), drill-down links to labor-requests/advances.
Backend controller/service found: NONE — no workforce dashboard endpoint exists. KPIs are computed client-side from WorkforceService lists (contractors/workers/laborRequests/advances). (No WorkforceDashboardController in be.)
Entity/table/Liquibase found: none dedicated; reuses workforce tables (V35).
Permissions found: none (workforce routes have no roleGuard beyond shell authGuard).
Unit/integration/E2E tests found: none.
Runtime result: Boots; dashboard renders from in-memory lists.
Final status: PARTIAL
```
Missing: no `GET /api/v1/workforce/dashboard/summary`; KPIs computed client-side (violates guide §2.1 rule that business numbers must come from backend); no permission-aware financial-field hiding; no tests.

### 4.25 Contractors — P1
Task split: CTR-DESIGN-01, CTR-BE-01, CTR-FE-01, CTR-QA-01
- [ ] Design: Party as shared identity source review.
- [ ] Frontend: form code/name/contact/tax/payment terms; dependency counts; masking; filtered export.
- [ ] Backend: versioned CRUD/deactivate/dependencies/export.
- [ ] DB: unique tenant code; optional unique party link; active dates/version.
- [ ] Tests: duplicate/link/dependency/tenant; lifecycle/export E2E; masking.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/contractors/contractors.component.ts (WorkforceService); modal form + Excel export.
Backend controller/service found: be/.../workforce/ContractorController.java /api/v1/workforce/contractors GET/POST/PUT/{id}/GET/{id}/export.xlsx -> ContractorService.
Entity/table/Liquibase found: contractors (V35).
Permissions found: no @PreAuthorize on ContractorController; frontend workforce has no roleGuard.
Unit/integration/E2E tests found: none dedicated (WorkforceExcelExportTest covers settlement workbook only).
Runtime result: Boots; contractor CRUD + export functional.
Final status: IMPLEMENTED_NOT_TESTED
```

### 4.26 Workers — P0
Task split: WRK-BE-01, WRK-FE-01, WRK-QA-01
- [ ] Frontend: identity/effective assignment/attendance identifiers/status; URL filters; dependency preview.
- [ ] Backend: versioned CRUD/deactivate/dependencies/export.
- [ ] DB: unique tenant code/approved identity; effective assignment history; FKs/version.
- [ ] Tests: effective-boundary/historical settlement; duplicate/cross-tenant; lifecycle E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/workers/workers.component.ts (WorkforceService); modal form + Excel export.
Backend controller/service found: be/.../workforce/WorkerController.java /api/v1/workforce/workers GET/POST/PUT/{id}/export.xlsx -> WorkerService.
Entity/table/Liquibase found: workers + worker_contractor_assignments + worker_rate_versions (V35).
Permissions found: none on WorkerController; frontend workforce no roleGuard.
Unit/integration/E2E tests found: none.
Runtime result: Boots; worker CRUD + export functional.
Final status: IMPLEMENTED_NOT_TESTED
```

### 4.27 Workforce Categories — P0
Task split: WCAT-DESIGN-01, WCAT-BE-01, WCAT-MIG-01, WCAT-QA-01
- [ ] Design: approved unification RFC before schema duplication.
- [ ] Frontend: show policy fields/worker count; policy timeline/overlap; safe deactivate.
- [ ] Backend: maintain current APIs; canonical `/work-categories` only after design; compatibility adapters.
- [ ] Tests: characterization before migration; historical payroll/settlement regression.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/categories/categories.component.ts (WorkforceService); modal form + Excel export.
Backend controller/service found: be/.../workforce/WorkerCategoryController.java /api/v1/workforce/categories GET/POST/PUT/{id}/export.xlsx -> WorkerCategoryService.
Entity/table/Liquibase found: worker_categories (V35).
Permissions found: none on WorkerCategoryController.
Unit/integration/E2E tests found: WorkforceMasterDataExcelExporterTest (category workbook structure).
Runtime result: Boots; worker-category CRUD + export functional.
Final status: IMPLEMENTED_NOT_TESTED
```
Note: employee categories (`attendance_categories`, V1) and worker categories (`worker_categories`, V35) remain separate masters — unification RFC not started (matches guide design hold).

### 4.28 Labor Requests — P0
Task split: LREQ-BE-01, LREQ-FE-01, LREQ-QA-01
- [ ] Frontend: header/lines/assignment view with availability; statuses; timeline/links/URL filters/export.
- [ ] Backend: versioned CRUD + submit/approve/cancel/close; assignment/unassignment; availability; idempotent commands.
- [ ] DB: header/line/assignment; unique number; overlap uniqueness/locking; version.
- [ ] Tests: overlap/concurrent assignment; state tests; permission/export E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/labor-requests/labor-requests.component.ts (WorkforceService); modal form + list + empty states.
Backend controller/service found: be/.../workforce/LaborRequestController.java /api/v1/workforce/labor-requests GET/POST/PUT/{id}/status -> LaborRequestService.
Entity/table/Liquibase found: labor_requests + labor_request_items (V35).
Permissions found: none on LaborRequestController.
Unit/integration/E2E tests found: none.
Runtime result: Boots; labor-request CRUD + status change functional.
Final status: PARTIAL
```
Missing: only a single `PUT /{id}/status` transition (no submit/approve/cancel/close endpoints, no assignment/unassignment, no availability check, no overlap locking).

### 4.29 Manual Workforce Attendance — P0
Task split: WATT-BE-01, WATT-FE-01, WATT-BE-02, WATT-QA-01
- [ ] Frontend: virtualized/paginated matrix; cell state persisted; send only dirty cells; keyboard cell navigation.
- [ ] Backend: attendance list; idempotent versioned batch; optional bulk preview/commit; focused lookups.
- [ ] DB: unique tenant+worker+date+shift; cell version; operation/line results; source/reason/actor.
- [ ] Tests: dirty/partial; uniqueness/version/idempotency/concurrency; locked-cell E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/manual-attendance/manual-attendance.component.ts; contractor/category/worker/status filters, worker+date preview, dirty-cell tracking, batch save, CSV export, dirty guard (unsavedChangesGuard).
Backend controller/service found: be/.../workforce/WorkforceAttendanceController.java /api/v1/workforce/attendance GET, /batch POST, /bulk-update POST, /calculation-rules GET -> WorkforceAttendanceService.
Entity/table/Liquibase found: manual_attendance_entries (V35).
Permissions found: none on controller; frontend workforce no roleGuard.
Unit/integration/E2E tests found: WorkforceAttendanceServiceTests (batch valid cells saved, per-cell errors, no valid-change loss); manual-attendance.component.spec.ts (matrix visibility).
Runtime result: Boots; matrix edit/save/reload-persistence functional.
Final status: IMPLEMENTED_NOT_TESTED
```
Missing: no cell `version`/`operation_id` columns on manual_attendance_entries, no concurrency/idempotency test, no locked-settlement guard test.

### 4.30 Settlement Periods — P0
Task split: WSET-BE-01, WSET-FE-01, WSET-BE-02, WSET-QA-01
- [ ] Frontend: persistent header; tabs; source-diff + controlled recalc; lock dependency preview.
- [ ] Backend: create/calculate/recalculate/review/approve/lock/reopen/reverse; lines/exceptions/diff/export.
- [ ] DB: header/version snapshot/lines/components/adjustments/approvals/postings; unique active range; source hashes.
- [ ] Tests: golden calculations/stale detection/concurrent transitions/idempotency; attendance-change E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/settlement-periods/settlement-periods.component.ts; create/calculate/review/approve/lock flow, issues list, per-period Excel export.
Backend controller/service found: be/.../workforce/WorkforceSettlementController.java /api/v1/workforce/settlements (periods GET/POST, /{id}/calculate, /issues, /review, /approve, /lock, /export-excel) -> WorkforceSettlementService.
Entity/table/Liquibase found: workforce_settlement_periods/worker_settlements/contractor_settlements (V35), workforce_settlement_issues + calc columns (V59), advance_policy_snapshot (V64).
Permissions found: none on controller.
Unit/integration/E2E tests found: WorkforceSettlementPeriodTests (failed recalc retains last successful CALCULATED state).
Runtime result: Boots; settlement lifecycle + export functional; stale detection on recalc.
Final status: IMPLEMENTED_NOT_TESTED
```
Missing: no reopen/reverse endpoints; no source-hash columns on settlement header; no golden calc/stale-source/concurrent-transition tests.

### 4.31 Advances — P0
Task split: ADV-BE-01, ADV-FE-01, ADV-BE-02, ADV-QA-01
- [ ] Frontend: tabs policies/requests/allocations/balances; effective policy resolve/display; timeline/outstanding.
- [ ] Backend: policy CRUD/resolve; advance lifecycle submit/approve/reject/disburse/reverse; allocations.
- [ ] DB: effective policy/scope; advance/approval/disbursement/allocation/reversal; decimals/operation IDs.
- [ ] Tests: precedence/effective; concurrent limit/disbursement; employee+worker E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/advances/advances.component.ts (WorkforceService); advances + policies UI, effective policy display, pause/resume/repay, CSV export.
Backend controller/service found: be/.../workforce/WorkforceAdvanceController.java /api/v1/workforce/advances GET/POST, /policies GET/PUT, /policies/effective GET, /{id}/pause|resume|repay -> WorkforceAdvanceService.
Entity/table/Liquibase found: workforce_advances + installments + ledger entries (V35), policies (V58, V64 versions), employee link (V72).
Permissions found: none on controller.
Unit/integration/E2E tests found: WorkforceEmployeeAdvanceServiceTests (installments + ledger mirror; rejects non-eligible category; payroll deduction suggestion).
Runtime result: Boots; advances + policies + repayment functional.
Final status: IMPLEMENTED_NOT_TESTED
```
Missing: no submit/approve/reject/disburse/reverse workflow endpoints (only create + pause/resume/repay), no concurrent limit/disbursement test, no policy precedence integration test.

### 4.32 Contractor Accounts — P0
Task split: CACC-BE-01, CACC-FE-01, CACC-QA-01
- [ ] Frontend: statement opening/debits/credits/closing; URL filters/export; no arbitrary balance editing.
- [ ] Backend: statement/export; adjustment only if approved; source postings owned by settlement/payment.
- [ ] DB: append-only ledger entries; atomic balance; opposite reversal.
- [ ] Tests: settlement→ledger→payment; running/opening; idempotency/reversal; statement E2E.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/contractor-accounts/contractor-accounts.component.ts; read-only table over the contractors list + window.print() receipt. NO statement view (no opening/debits/credits/closing, no URL filters).
Backend controller/service found: NONE — no contractor-account/statement endpoint exists. The page renders contractors from ContractorService data; no ledger statement API.
Entity/table/Liquibase found: no contractor-account ledger table; workforce_advance_ledger_entries (V35) is advance-scoped only; contractor_settlements (V35) holds balances.
Permissions found: none.
Unit/integration/E2E tests found: none.
Runtime result: Boots; page renders contractor rows only.
Final status: PARTIAL
```
Missing: no contractor ledger statement/API, no opening/debits/credits/closing, no settlement→ledger→payment posting chain.

### 4.33 Workforce Report Import — P0
Task split: WIMP-BE-01, WIMP-FE-01, WIMP-BE-02, WIMP-QA-01
- [ ] Frontend: stepper upload→map→validate→preview→commit→result; safe error workbook; preserve state after failure.
- [ ] Backend: preview/mapping/validate/commit/reverse/status/errors/result APIs.
- [ ] DB: import/mapping/row staging/errors/commit/reversal/content hash; committed record source-row link.
- [ ] Tests: parser/templates/stale preview/duplicate commit; reversal blocker; malicious file.
- Verification:
```text
Frontend page/store/API found: fe/src/app/features/workforce/pages/reports-import/reports-import.component.ts; stepper upload→mapping→validate→preview→commit→reverse, original/error workbook download, CSV template.
Backend controller/service found: be/.../workforce/WorkforceImportController.java /api/v1/workforce/imports GET/upload/mapping/validate/preview/commit/reverse/original/errors.xlsx/analyze -> WorkforceExcelImportService.
Entity/table/Liquibase found: workforce_import_batches/rows/changes (V60; batches.operation_id + unique(app_id, operation_id)).
Permissions found: controller class-level ADMIN/HR_MANAGER/HR_REVIEWER; commit ADMIN/HR_MANAGER; reverse ADMIN.
Unit/integration/E2E tests found: WorkforceImportErrorWorkbookTests (Arabic RTL error workbook + typed row numbers).
Runtime result: Boots; full wizard + idempotent commit + reversal functional.
Final status: IMPLEMENTED_NOT_TESTED
```
Missing: no stale-preview revalidation test, no duplicate-commit idempotency integration test, no reversal-blocker test.

### 4.34 Shell, Forbidden and Not Found — P0
Task split: SHELL-FE-01, SHELL-FE-02, SHELL-QA-01
- [x] Frontend: dedicated `/forbidden` and `/not-found`; wildcard must not silently hide broken links.
- [ ] Frontend: typed permission registry shared across navigation surfaces.
- [x] Backend: `/users/me` returns identity/tenant/roles/scopes/session safely.
- [ ] DB: bounded unique recent/favorites; no sensitive query values persisted.
- [ ] Tests: route-guard matrix; direct unauthorized/unknown/expired; logout-cache; keyboard accessibility.
- Verification:
```text
Frontend (S0-5 VERIFIED): app.routes.ts now has shell children /forbidden + /not-found, child wildcard `**` -> /not-found and root wildcard -> /not-found (unknown routes no longer silently land on dashboard); roleGuard redirects to /forbidden. New fe/src/app/features/errors/forbidden.page.* and not-found.page.*. Tests: app.routes.spec.ts (+roleGuardDecision pure-helper spec). i18n: 5 new errors.* keys (en+ar) in error_pages_translations_v76.csv + DEFAULT_FALLBACKS.
Backend (S0-5 VERIFIED): GET /api/v1/users/me in AuthController (JWT principal -> jwt.getSubject()/getExpiresAt()); AuthService.me(...) returns MeResponse(id, username, displayName, tenant{id,code,name}, roles, scopes, canViewSalary, categoryId, dashboardCustomizationEnabled, active, session{expiresAt,timeoutMinutes,timeoutEnabled}, version) — no credentials/hashes. Tests: MeIdentityIntegrationTests (TEST tenant, admin role/scopes/session assertions).
Entity/table/Liquibase found: user_preferences favorites/recent with limits (V1/V47).
Permissions found: menu-level via allowed_menus (V19/V37); roleGuard on shell children.
Unit/integration/E2E tests found: + app.routes.spec.ts, roleGuardDecision spec, MeIdentityIntegrationTests.
Runtime result: Boots; /users/me 200 with full identity; unknown route -> not-found page; unauthorized role -> forbidden page.
Final status: PARTIAL (typed permission registry shared across sidebar/shortcuts/breadcrumbs/favorites/recent + DB bounded recent/favorites + route-guard E2E matrix still TBD)
```
Missing: no typed permission registry shared across navigation surfaces; workforce children lack roleGuard; no route-guard E2E matrix; bounded unique recent/favorites not re-audited.

---

## Section 5 — Cross-cutting technical tickets

### 5.1 Production configuration — P0
- [x] Separate dev/prod Compose profiles; production requires secrets with no fallback; rejects default passwords/short JWT secrets; runs `prod`; reverse proxy only public; internal network for DB/backend; CORS explicit HTTPS; no stack traces/public Swagger.
- Acceptance: production start fails immediately when a required secret is absent. **VERIFIED**
- Verification:
```text
S0-6 VERIFIED:
Files: docker-compose.yml (dev baseline: 3 services, DB/backend exposed for local tooling, dev secret fallbacks, networks split into internal + public, frontend on both) + NEW docker-compose.prod.yml overlay (`docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d`): db/backend ports removed (`ports: []`) and internal-only, all secrets required via `${VAR:?}` (no fallbacks; `config` fails with "POSTGRES_DB must be set" when absent), SPRING_PROFILES_ACTIVE=prod, CORS requires explicit HTTPS allowlist. `docker compose config` passes for dev and prod-overlay (verified with secrets set).
Application config: NEW be/src/main/resources/application-prod.properties — every datasource/JWT/CORS/bootstrap value is `${...}` with no default (fail-fast; verified by ProdConfigFailFastTests). SecurityConfig.corsConfigurationSource now consumes ${hr.cors.allowed-origins} (env-authoritative; no more hardcoded localhost/trycloudflare list) — verified by CorsConfigurationSourceTests (configured HTTPS origins allowed, localhost/attacker rejected).
Swagger/springdoc: absent (no dependency, no config) — "disabled" trivially because it does not exist.
Runtime result: Backend 73 tests green (dev). Prod overlay validated by `docker compose config`.
Final status: PARTIAL (dev/prod split, fail-fast prod profile, internal network, env-driven CORS VERIFIED; explicit default-password rejection at deploy tooling level + TLS at reverse proxy still environment-specific)
```
Remaining gaps: prod TLS termination is delegated to an external reverse proxy/LB (nginx serves HTTP internally on 80); default-password guard is via compose `:?` + bootstrap no-default (no separate secret-scan in deploy tooling).

### 5.2 CI and branch protection — P0
- [x] `./gradlew clean test check` (be), `npm ci && npm run check:i18n && npm test -- --run && npm run build` (fe), `docker compose config`, `docker compose build`.
- [ ] Testcontainers PostgreSQL, JaCoCo, Playwright, dependency/secret scanning, migration validation, protected `main`.
- Verification:
```text
CI config: VERIFIED (S0-7) — .github/workflows/ci.yml (GitHub Actions, repo remote github.com/Mohamed-Hammada/bortqala): three jobs.
  - backend: setup-java temurin 26 + gradle cache -> `./gradlew clean test check`.
  - frontend: setup-node 24 + npm cache -> `npm ci` -> `npm run check:i18n` -> `npx ng test --watch=false` -> `npm run build`.
  - compose: `docker compose -f docker-compose.yml config --quiet` (dev), prod-overlay `config --quiet` with env secrets, then `docker compose build`.
  Deviation: guide's `npm test -- --run` rejected by Angular's vitest builder ("Unknown argument: run"); CI uses the repo-proven `npx ng test --watch=false` (same vitest run, non-watch).
Testcontainers PostgreSQL: NOT FOUND (H2 in PostgreSQL mode only; no testcontainers dependency) — backlog.
JaCoCo: VERIFIED — `id 'jacoco'` + `test { finalizedBy jacocoTestReport }` in be/build.gradle; `jacocoTestReport` ran under `./gradlew test check`, HTML report at be/build/reports/jacoco/test/html/index.html.
Playwright/E2E: NOT FOUND (no playwright.config.*, no e2e/ dir) — backlog.
Dependency/secret scanning: NOT FOUND — GitHub secret scanning is an org/repo setting; no action wired — backlog.
Migration validation: Liquibase changesets validated on every @SpringBootTest startup (all 73 tests); no dedicated migration dry-run job — backlog.
Protected main: requires GitHub repo settings (branch protection rules + required status checks) — owner action, not a repo file.
Runtime result: `.github/workflows/ci.yml` written; `./gradlew clean test check` (via `test check` locally — `clean` blocked by a Windows file lock on the running jar) BUILD SUCCESSFUL, 73/73 tests, jacocoTestReport ran; compose config dev+prod verified in S0-6; `docker compose build` wired into CI (image builds are heavy; left for CI runner). 55 gradle/fe checks gated on the three jobs; CI cannot be exercised from this clone (push needed).
Final status: PARTIAL
```

### 5.3 Shared idempotency — P0
- [x] One reusable table/component: tenant_id, operation_type, operation_id, request_hash, status, response_reference_or_body, created_at, completed_at; unique `(tenant_id, operation_type, operation_id)`.
- [x] Used by import commit, attendance decisions/batches, stock posting, GRN, invoice, payment, payroll/settlement calc/posting, production output, quality disposition, reversals.
- Verification:
```text
Shared table/component: VERIFIED (S0-2) — Liquibase V75 `idempotency_keys` (id, app_id, operation_type, operation_id, request_hash, status, response_reference_or_body, created_at, completed_at; unique uq_idempotency_app_type_operation) + shared IdempotencyKey/IdempotencyKeyRepository/IdempotencyService.execute(). Verified by IdempotencyServiceTests (replay, hash mismatch, in-progress rejection, failure propagation, stable hash).
Used by: supplier payments (ProcurementService.createSupplierPayment). Remaining flows (import commit, attendance decisions/batches, stock posting, GRN, invoice, payroll/settlement calc/posting, production output, quality disposition, reversals) still to adopt the shared service — tracked per sprint item.
Runtime result: Backend 67 tests green.
Final status: PARTIAL
```

### 5.4 Shared transition metadata — P0
- [x] Workflow response `{ status, version, allowedActions }`; frontend uses for UX; backend rechecks role/tenant/version/dependencies/state.
- Verification:
```text
Backend: VERIFIED (S0-3) — shared TransitionResponse(status, version, allowedActions) + WorkflowTransitions helper in shared/api; wired into settlement periods (review/approve/lock) and attendance reports (approve/reopen). Payroll /payroll/transition kept as a batch status update returning the full sheet (deliberate exception, not a single-entity workflow).
Frontend: VERIFIED — core/api.models.ts TransitionResponse; workforce.service reviewPeriod/approvePeriod/lockPeriod and reports.store approve/reopen consume it; pages reload data after transitions.
Runtime result: Backend 67 tests green (+WorkflowTransitionsTests, WorkforceSettlementTransitionTests, ReportingTransitionTests); FE build/tests/i18n green.
Final status: PARTIAL
```

### 5.5 Shared lookup contract — P1
- [ ] Paginated search lookups (`id/code/displayName/active/version/metadata`); no full-table loads into dialogs.
- Verification:
```text
Backend: NOT FOUND — no paginated search-lookup endpoints (e.g. /lookup?q=) for employees/parties/items/accounts/workers. List endpoints return full arrays.
Frontend: NOT FOUND — dialogs load full lists from list endpoints (e.g. workforce store loads all contractors/workers/categories into memory; dashboard filters client-side). Shared TablePagination is a client-side slice of already-loaded rows.
Runtime result: Boots; works at small data scale.
Final status: NOT_STARTED
```

### 5.6 Excel security — P0
- [x] Exports apply same filters/permissions; include generated-at/by, timezone, tenant; escape `=`,`+`,`-`,`@` cells; omit unauthorized fields; stream/limit large files.
- Verification:
```text
Backend: VERIFIED (S0-4) — ExcelExportSupport.escapeFormula() escapes leading = + - @ with a leading apostrophe; applied in the shared writeRow (covers reporting ApachePoiReportExporter, operations OperationsExcelExporter, DataExportService) and directly in PayrollExcelExporter, ProcurementExcelExporter, WorkforceExcelExportService.createCell, WorkforceMasterDataExcelExporter, WorkforceExcelImportService error workbook. Tests: ExcelExportSupportTests (+escapeFormula + writeRow escaping).
Frontend CSV: VERIFIED — download.ts escapeCsvCell() escapes leading = + - @ and still escapes embedded quotes; tests: download.spec.ts.
Formula injection: mitigated for user-controlled string cells across all exporters + FE CSV.
Runtime result: Backend 70 tests green; FE build/tests (23) / i18n green.
Final status: PARTIAL (escaping VERIFIED; generated-at/by/timezone/tenant headers and large-file streaming still TBD per exporter)
```

---

## Execution order reference (from guide §6)

- **Sprint 0 — Foundation:** route inventory; resolve project-map contradictions; standard API error/correlation ID; CI + PostgreSQL test base + Playwright; production config/secret validation; permission-key and transition-state conventions.
- **Sprint 1 — Attendance & identity:** categories overlap/effective history; employees uniqueness/effective assignments; import hash/dedup/idempotency; report evidence & review concurrency; manual attendance batch/version.
- **Sprint 2 — Stock, finance & procurement:** fiscal-period shared guard; inventory movement/reversal; journal posting/reversal; procurement GRN/payment concurrency; tax/currency frozen snapshots; contractor account reconciliation.
- **Sprint 3 — Payroll & workforce financials:** settlement source snapshots/staleness; advances policy precedence/disbursement; payroll calculation snapshots/deductions/posting; bank/treasury & COA dependencies.
- **Sprint 4 — Incomplete business modules:** sales redesign/migration; BOM version/component; production posting; quality quarantine; cross-module E2E & reconciliation.

## Known project-map contradiction (guide §1, §4.10, §4.18)

`PROJECT_MAP.md` lists exchange-rate snapshots in **both** COMPLETED (line 86) and ORPHANS/PENDING (line 42). Verified on this branch: the feature IS implemented end-to-end (frozen PO/invoice rate, override reason, base totals, base-value ledger postings). The stale ORPHANS bullet must be corrected after verification.

**Action taken 2026-08-01:** Confirmed implemented (Liquibase V63 `20260729_v63_procurement_exchange_rate_snapshots.yaml`; `PurchaseOrder.java` fields; `GET /api/v1/trade/procurement/exchange-rate`; procurement UI freezes rate). The ORPHANS/PENDING bullet "exchange-rate snapshots" is stale and should be removed; `PROJECT_MAP.md` still needs that edit (kept for confirmation — see todo below).

## Verified-uncovered items (backlog candidates)

- **Blocker fixed in this run (uncommitted):** duplicate `common.edit` rows removed from `be/src/main/resources/db/changelog/data/insert/files/full_ui_translations_v18.csv`.
- `/users/me` endpoint added (S0-5, §4.34); `/forbidden`/`/not-found` pages + wildcard fix VERIFIED; typed permission registry still TBD (4.34).
- No workforce dashboard backend summary endpoint (4.24).
- No contractor-account/statement API (4.32).
- No shared idempotency table / allowedActions / lookup contract / Excel formula escaping (§5.3–5.6).
- No CI, Testcontainers, JaCoCo, Playwright (§5.2).
- `@Version` missing on most mutable business aggregates (procurement documents, settlements, manual attendance, journal entries, etc.).
- CORS allowlist is hardcoded and ignores `${hr.cors.allowed-origins}` (5.1).
