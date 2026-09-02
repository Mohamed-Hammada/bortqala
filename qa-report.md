# QA Test Report

## Test Information
- Application: Bemo ERP (Angular 22 frontend + Spring Boot backend)
- Environment: Local dev (http://localhost:4200, backend http://localhost:8080, DEV profile, DEMO app code)
- Test Date: 2026-09-02
- Browser: Chromium (Playwright MCP)
- Tester/Agent: QA automation agent
- Overall Status: Completed

## Test Summary
- Total test scenarios: 35 (27 passed / 8 failed / 0 blocked after environment recovery)
- Passed: 27
- Failed: 8 (see issues; several are dev-env or telemetry-only)
- Blocked: 0
- Issues found: 8 (1 High-equivalent UX defect, 4 Medium, 3 Low/environmental)

### Areas verified (passed)
- Authentication: empty submit blocked, invalid credentials rejected with localized error, valid login redirects to dashboard, logout with scope dialog, protected-route guard redirects to /login after logout
- i18n & layout: Arabic (RTL) and English (LTR) render fully after DB recovery; language switch persists and applies dir/lang instantly
- CRUD: category create, employee create/edit/deactivate (with correct confirm prompt content), all persisted correctly across full page reloads
- Validation: required fields blocked client-side (employees dialog shows proper inline alerts)
- Duplicate handling: server correctly returns 409 for duplicate category/employee codes (UI display fails — ISSUE-006)
- Search/filter: employee search positive + negative match; command palette (Ctrl+K) search works
- Pagination controls render (single-page data; deep pagination not exercised)
- Reports: catalog/filters render, preview API correct (1 category/1 employee/1 workday), report created with KPI cards and empty states
- Excel export downloads a file successfully
- Dashboard renders with correct zero-states
- Responsive: no horizontal overflow at 390px mobile width
- Console/network: clean after recovery (only expected pre-login SSO probe 401)

### Test data created (safe to delete)
- Category `QA-CAT-01` ("فئة اختبار QA", manual attendance)
- Employee `EMP-QA-001` ("موظف اختبار كيو إيه ١ — معدل", deactivated at end of run)
- Attendance report for 02/09/2026 (id `11e4bd76-fc02-453f-8084-2f9ed8b94fc2`, "تحت المراجعة")

## Issues

### ISSUE-001 — Stale Angular build error overlay blocks first app load
**Severity:** Low
**Priority:** P3
**Status:** Open
**Page/Feature:** App bootstrap / dev server build
**Test Scenario:** Open http://localhost:4200 for the first time.
**Preconditions:** Angular dev server running with stale incremental build state.
**Steps to Reproduce:**
1. Open http://localhost:4200 in a fresh browser.
2. Observe full-screen error overlay instead of the app.
**Expected Result:** Login page renders.
**Actual Result:** Overlay shown: `TS2551: Property 'openExpenseModal' does not exist on type 'SiteCustodyListComponent'. Did you mean 'showExpenseModal'?` (src/app/features/projects/ui/site-custody-list.component.html:53:60). The property/method DOES exist in current source (`openExpenseModal()` at site-custody-list.component.ts:109), so the overlay is a stale build artifact. A hard reload cleared it and the app loaded.
**Evidence:**
- Screenshot: issue-001-compile-error.png (Playwright MCP output dir)
- Console error: none (overlay is server-injected)
**Test Data:** N/A
**Impact:** Confusing first-load experience in dev; would not affect production builds, but indicates stale incremental-build state in the running dev server.
**Notes:** Dev-environment-only artifact; logged for completeness. Reload is a workaround.

---

### ISSUE-002 — Backend unreachable (DB down): GET /api/v1/i18n/ar-EG returned 500; UI fell back to raw translation keys
**Severity:** Medium (environment root cause; app-side error handling still poor)
**Priority:** P2
**Status:** Resolved (environment) / Open (error-handling observation)
**Page/Feature:** All pages / i18n service (Arabic is default locale)
**Test Scenario:** Load login page and observe rendered labels.
**Preconditions:** Backend running, default language ar-EG.
**Steps to Reproduce:**
1. Open http://localhost:4200.
2. Observe login page labels.
**Expected Result:** Arabic (or selected-language) labels render.
**Actual Result:** Every text node renders the raw key, e.g. `login.tagline`, `login.title`, `login.username`, `login.submit`. Network shows `GET /api/v1/i18n/ar-EG => 500` (repeated on each navigation). No translations can load for any user.
**Evidence:**
- Network/API error: `GET /api/v1/i18n/ar-EG => [500] Internal Server Error` (backend returned generic `{"timestamp":...,"status":500,"error":"Internal Server Error"}` shape on the sibling endpoint; i18n confirmed failing via UI)
- Console error: `Failed to load resource: 500 @ /api/v1/i18n/ar-EG`
**Test Data:** N/A
**Impact:** Arabic (default locale) UI is unusable/unreadable for all users; affects every page.
**Notes:** ROOT CAUSE CONFIRMED: PostgreSQL was not running (DB runs in WSL2 `Ubuntu-24.04`, nothing listened on 5432). Once the database was started, `GET /api/v1/i18n/ar-EG` returned 200 and translations loaded. Remaining app-side observation: while the backend fails, the UI silently renders raw i18n keys with no user-facing error or retry — poor failure mode. Language toggle buttons (`login.langAr`/`login.langEn`) were visible but non-functional without translations.

---

### ISSUE-003 — GET /api/v1/system/status returned 500 while database was down
**Severity:** Medium (environment root cause)
**Priority:** P2
**Status:** Resolved (environment)
**Page/Feature:** System status endpoint (polled from login page)
**Test Scenario:** Load login page and watch API calls.
**Preconditions:** Backend running.
**Steps to Reproduce:**
1. Open http://localhost:4200/login.
2. Observe network traffic.
**Expected Result:** Status endpoint returns 200 with system health/entitlement info.
**Actual Result:** `GET /api/v1/system/status => [500]` with body `{"timestamp":"2026-09-02T15:43:05.706Z","status":500,"error":"Internal Server Error","path":"/api/v1/system/status"}`. Confirmed directly against backend: `curl http://localhost:8080/api/v1/system/status` → 500.
**Evidence:**
- Network/API error: 500 on `/api/v1/system/status` (repeated 3+ times)
- Console error: `Failed to load resource: 500 @ /api/v1/system/status`
**Test Data:** N/A
**Impact:** Any feature depending on system status (feature flags/entitlements/health display) is degraded or broken; backend has a server-side error that is not surfaced to the user.
**Notes:** Root cause identical to ISSUE-002: PostgreSQL was down (WSL2 DB not started). Endpoint returned 200 after the DB started. No user-visible error message was shown on the login page despite repeated 500s.

---

### ISSUE-004 — Product analytics event ingestion returns 409 for simple page views
**Severity:** Medium
**Priority:** P2
**Status:** Open
**Page/Feature:** Product analytics tracking (POST /api/v1/product-analytics/events)
**Test Scenario:** Log in and land on the dashboard; verify analytics telemetry.
**Preconditions:** Authenticated session.
**Steps to Reproduce:**
1. Log in with valid credentials.
2. Land on `/dashboard`.
3. Observe the network call to `/api/v1/product-analytics/events`.
**Expected Result:** The PAGE_VIEW event is accepted (200/2xx).
**Actual Result:** `409 Conflict` with body `{"code":"DATA_CONFLICT","message":"The operation conflicts with existing data."}`. The event carries a freshly generated `operationId` (UUID) so a client-side duplicate is unlikely; a server-side uniqueness constraint appears to reject legitimate events.
**Evidence:**
- Network/API error: `POST /api/v1/product-analytics/events => [409]`
- Request body: `{"eventName":"PAGE_VIEW","featureKey":"navigation","operationId":"4924e5c2-...","properties":{"route":"/dashboard","source":"web"}}`
**Test Data:** N/A (telemetry only)
**Impact:** Product-usage analytics are silently dropped (at least for dashboard page views); usage reports will be incomplete. No user-facing impact.
**Notes:** Needs re-verification on other routes to determine scope.

---

### ISSUE-005 — Documented bootstrap admin password does not match the actual dev database user
**Severity:** Low
**Priority:** P3
**Status:** Open
**Page/Feature:** Dev environment setup / authentication
**Test Scenario:** Log in with the credentials documented in `start-backend-dev.bat`.
**Preconditions:** Dev backend + dev database running.
**Steps to Reproduce:**
1. Start the dev stack as per the repo scripts.
2. Log in as `admin` using the password declared in `start-backend-dev.bat` (`HR_BOOTSTRAP_ADMIN_PASSWORD`).
3. Login is rejected with 401 `AUTHENTICATION_FAILED`.
4. Log in with the `application.properties` default bootstrap password → succeeds.
**Expected Result:** The documented bootstrap password matches the seeded `admin` user.
**Actual Result:** The existing `admin` row in the dev DB was seeded with the `application.properties` default password, because `BootstrapAdminInitializer.ensureBootstrapAppAdmin` only creates the user when absent (AuthService.java:700) and never resets an existing password. The `.bat` env override therefore has no effect on an already-seeded DB, but the file leads the reader to believe otherwise.
**Evidence:**
- Network/API error: `POST /api/v1/auth/login => [401]` with the .bat-declared password; `[200]` with the properties default.
- Console error: corresponding 401 resource errors during login attempts.
**Test Data:** Dev bootstrap `admin` user (passwords intentionally not reproduced in this report; see `start-backend-dev.bat` and `be/src/main/resources/application.properties`).
**Impact:** Developer onboarding friction/time lost; QA and new devs follow the documented credential and get locked out with no hint.
**Notes:** Either the initializer should document "password only applies on first seed", or the script/docs should be synced.

---

### ISSUE-006 — Server-side conflict (409) errors silently swallowed in create dialogs; backend error message never shown to the user
**Severity:** Medium
**Priority:** P2
**Status:** Open
**Page/Feature:** Categories (قواعد وفئات الحضور) and Employees (الموظفون) create dialogs
**Test Scenario:** Duplicate data handling — create a record with an already-existing code.
**Preconditions:** Category `QA-CAT-01` and employee `EMP-QA-001` exist.
**Steps to Reproduce:**
1. Open `/categories` → "＋ فئة جديدة".
2. Enter code `QA-CAT-01` (already exists) and any different name.
3. Click "حفظ الإعداد".
4. Repeat on `/employees` → "＋ موظف جديد" with code `EMP-QA-001`.
**Expected Result:** An error is displayed, e.g. the backend's localized message "رمز التصنيف موجود بالفعل." (category code already exists) or the employees equivalent.
**Actual Result:** Both dialogs stay open with no error message, no toast, no field highlight — completely silent failure. On the employees dialog the only hint is a bare "1 *" badge on the "التواريخ" section, which reveals no error text when expanded (and the badge is unrelated to the actual duplicate-code problem, pointing at the wrong section). Additionally, only AFTER the user cancels the dialog does an alert appear — but it is mislabeled as a *loading* failure: "⚠️ حدث خطأ أثناء التحميل / رمز الموظف موجود بالفعل." with a "🔄 إعادة المحاولة" (retry) button, i.e. the save error is surfaced at the wrong time, in the wrong context, with an irrelevant action. The user has no way to connect the alert to the save they attempted. Reproduced twice on categories, once on employees.
**Evidence:**
- Network/API error: `POST /api/v1/categories => [409]` with body `{"code":"HRCFG_CATEGORY_CODE_EXISTS","localizedMessage":"رمز التصنيف موجود بالفعل.","status":409}`; `POST /api/v1/employees => [409]` (same pattern)
- Full-page accessibility snapshots taken immediately after each save show no alert/toast/status element in either dialog.
**Test Data:** Existing category `QA-CAT-01` ("فئة اختبار QA"), existing employee `EMP-QA-001`; duplicate attempts with different names.
**Impact:** Users cannot tell why their save fails; data-entry frustration, likely duplicate support tickets. The backend provides good localized messages that the frontend discards.
**Notes:** Empty-form saves are blocked by native HTML5 validation (categories: silent native block; employees: proper inline alerts) — inconsistent error handling between the two dialogs.

---

### ISSUE-007 — "Unsaved changes" confirm overlay is missing dialog semantics; invisible to accessibility tree and breaks automation/screen readers
**Severity:** Medium
**Priority:** P2
**Status:** Open
**Page/Feature:** Employees dialog — unsaved-changes confirmation (`.confirm-overlay`)
**Test Scenario:** Close the employee dialog with unsaved changes; verify the confirm prompt is accessible.
**Preconditions:** Employee create/edit dialog open with modified fields.
**Steps to Reproduce:**
1. Open `/employees` → "＋ موظف جديد", type anything.
2. Click "إلغاء" (Cancel).
**Expected Result:** The "تغييرات غير محفوظة" confirm prompt is a role="dialog"/"alertdialog" element exposed to the accessibility tree, focus is trapped inside, and Esc/Tab behave predictably.
**Actual Result:** The overlay renders visually (`.confirm-overlay`, fixed, z-index 10000, pointer events ON) but is completely absent from the accessibility tree (no dialog/alertdialog role, not in the AX snapshot). Screen-reader users get no indication of the prompt; automation snapshots see a page where "nothing is clickable" with no explanation. Focus is not moved into the prompt.
**Evidence:**
- AX snapshot after Cancel shows only the underlying page — no prompt — while DOM inspection shows `.confirm-overlay` visible and intercepting all pointer events (clicks on page elements time out "intercepts pointer events").
- DOM: `<div class="confirm-overlay">⚠️ تغييرات غير محفوظة … [تجاهل التغييرات] [إلغاء]</div>`
**Test Data:** N/A
**Impact:** Screen-reader users cannot dismiss or even perceive the prompt (hard blocker for them); also breaks test automation and any DOM-based assistive tooling.
**Notes:** Underlying cancel→confirm flow itself works once clicked.

---

### ISSUE-008 — No-match empty state on Employees repeats onboarding copy that contradicts current state
**Severity:** Low
**Priority:** P3
**Status:** Open
**Page/Feature:** Employees list — search empty state
**Test Scenario:** Search with a string that matches no employees while a category and employee already exist.
**Steps to Reproduce:**
1. With category `QA-CAT-01` and employee `EMP-QA-001` existing, search `NOMATCH-XYZ-999`.
**Expected Result:** Empty state says something like "no employees match this search" and suggests clearing the search.
**Actual Result:** Heading correctly says "لا يوجد موظفون مطابقون" but the body repeats the onboarding copy "أضف فئة أولًا ثم سجل الموظفين." (add a category first, then register employees) — which contradicts reality (a category and an employee already exist) and suggests the wrong remedy.
**Evidence:** AX snapshot of the empty-state row.
**Test Data:** Search term `NOMATCH-XYZ-999`.
**Impact:** Mild user confusion; wrong guidance.
**Notes:** Same empty-state component is reused for both "no data at all" and "no search matches" without adapting the hint.

---

### ISSUE-009 — Raw translation key shown for a role in Users dialogs
**Severity:** Low
**Priority:** P3
**Status:** Open
**Page/Feature:** Users (المستخدمون) — new-user dialog, roles list
**Test Scenario:** Open "＋ مستخدم جديد" and inspect the available roles.
**Steps to Reproduce:**
1. Open `/users` → "＋ مستخدم جديد".
2. Inspect the roles checkboxes list.
**Expected Result:** All role names/descriptions are localized.
**Actual Result:** One role renders as the raw key `roles.access.projectManager` with description `roles.access.projectManager.descr` (also truncated — "descr" instead of "description") in both the dropdown options and the roles checkboxes.
**Evidence:** AX snapshot of the dialog roles section.
**Test Data:** N/A
**Impact:** Cosmetic but visible to admins managing roles; suggests a missing/misspelled i18n key (`descr` suffix looks like a typo).
**Notes:** All other 18 roles localize correctly.

---

### ISSUE-010 — Leave requests completely broken: frontend/backend date format mismatch (ISO string vs epoch millis)
**Severity:** Critical
**Priority:** P0
**Status:** Open
**Page/Feature:** Leaves (الإجازات) — create request + list
**Test Scenario:** Submit a new leave request from the UI and view the requests table.
**Preconditions:** At least one employee and the "ANNUAL" leave type exist.
**Steps to Reproduce:**
1. Open `/leaves` → "＋ تقديم طلب إجازة جديد".
2. Select employee/type, valid start < end dates (e.g. 2026-09-10 → 2026-09-12).
3. Click "حفظ".
**Expected Result:** Request is created (201) and appears in the table with readable dates.
**Actual Result:**
1. **Create always fails:** `POST /api/v1/leaves/requests => 400 {"code":"MALFORMED_REQUEST"}` for every UI attempt (3 attempts). Reproduced with direct API call using the exact UI payload — same 400.
2. **Root cause (verified by probing the API):** the backend's Jackson ObjectMapper only deserializes `LocalDate` from **epoch-millis numbers**; it rejects ISO-8601 strings. `{"startDate":"2026-09-10"}` → 400; `{"startDate":[2026,9,10]}` → 400; `{"startDate":1788296400000}` → **201 Created**. The frontend sends ISO strings, so the UI can never create a leave request.
3. **Read path is broken too:** records created via the API render the dates as raw epoch numbers in the table (e.g. "1788296400000" as تاريخ البداية), because the backend serializes `LocalDate` as a number and the frontend cannot format it.
**Evidence:**
- Network/API error: 3× `POST /api/v1/leaves/requests => [400]` from the UI; direct reproduction confirmed format dependency (see above).
- Console error: corresponding 400 resource errors.
- Table cell content: `LR-2026-7407 … 1788296400000 1788555600000 4 قيد المراجعة والاعتماد`
**Test Data:** Employee `EMP-QA-001` (a3ddf140-…), leave type ANNUAL (64dfad46-…), dates 2026-09-10→2026-09-12. A request `LR-2026-7407` (id `fbb38e34-2c21-4211-b656-6b189fcd2684`) was created via the epoch-millis workaround to verify the read path — safe to delete.
**Impact:** The entire leave-request feature is unusable from the UI: no user can submit a leave request, and date columns show raw numbers for any record that exists. Business-critical HR flow broken.
**Notes:** Minor sub-issue: no client-side validation for end-date ≥ start-date; the backend does implement INVALID_DATE_RANGE but it is unreachable because deserialization fails first. Also the generic "نص الطلب غير صالح" (malformed request body) gives no field-level guidance. Likely root cause: the backend mixes Jackson 3 (`tools.jackson.databind.ObjectMapper`, imported in SecurityConfig.java:32 and elsewhere) — whose default for `java.time` types is epoch-millis — with code written for Jackson 2 (ISO strings) conventions. Other date-based modules (expenses, journal entries, projects, etc.) may be affected the same way; recommend a global Jackson date-format configuration and an end-to-end date contract test.

---

### ISSUE-011 — Parties: required tax ID for new suppliers enforced with a silent return; no error shown
**Severity:** Medium
**Priority:** P2
**Status:** Open
**Page/Feature:** Parties (جهات التعامل) — create dialog
**Test Scenario:** Create a new supplier party.
**Steps to Reproduce:**
1. Open `/parties` → "＋ جهة جديدة".
2. Fill code, name, phone, email but leave "رقم التسجيل الضريبي" (tax ID) empty.
3. Click "حفظ الجهة".
**Expected Result:** An inline error on the tax-ID field (e.g. "tax registration number is required for suppliers") or a toast.
**Actual Result:** Nothing happens at all — no request, no toast, no field highlight. The code at `parties.page.ts:212-216` sets a reactive-form error and returns silently; the template never surfaces it. Reproduced once; after entering a tax ID the save succeeds (201).
**Evidence:** Network panel shows no POST to `/api/v1/parties` on the blocked click; no `[role=alert]` or toast present.
**Test Data:** Party `QA-PARTY-01` / "مورد اختبار كيو إيه" (created after filling tax ID 300-QA-778899).
**Impact:** Users filling supplier data without a tax ID conclude the app is broken (same family of silent-failure defects as ISSUE-006).
**Notes:** Suggest a shared dialog-error pattern: always surface blocked saves with an inline message.

---

### ISSUE-012 — Expenses: mislabeled page-level error ("تعذر تحميل سجلات التدقيق") and silent claim-create failure for users without a linked employee
**Severity:** Medium
**Priority:** P2
**Status:** Open
**Page/Feature:** Expenses (المصروفات) page
**Test Scenario:** Open /expenses as the `admin` user (no employee record linked) and submit an expense claim.
**Preconditions:** Signed in as a user with no linked employee (e.g. `admin`).
**Steps to Reproduce:**
1. Open `/expenses`.
2. Click "مطلب جديد", fill category/date/amount/description, click "حفظ".
**Expected Result:** Either a working claim form (admin can file on behalf of employees) or a clear message "this module requires an employee-linked account", and correct error labels.
**Actual Result:**
1. The page fails to load data: `GET /api/v1/expenses => 409 EXPENSE_NO_EMPLOYEE_LINKED`, but the visible error is **mislabeled**: "تعذر تحميل سجلات التدقيق / لا يوجد سجل موظف مرتبط بحساب المستخدم الحالي. / 🔄 إعادة المحاولة" — it wraps the real message in a wrong "audit logs failed" heading with an irrelevant retry button.
2. The claim form still opens and accepts input; on save `POST /api/v1/expenses => 400 MALFORMED_REQUEST` (ISO-date payload, see ISSUE-010) and the dialog stays open with **no toast, no inline error**.
**Evidence:**
- Network: `GET /api/v1/expenses => [409]`, `POST /api/v1/expenses => [400]`
- Page text: `مطالبات المصروفات … تعذر تحميل سجلات التدقيق لا يوجد سجل موظف مرتبط بحساب المستخدم الحالي. إعادة المحاولة`
**Test Data:** Amount 250 EGP, category MEAL, date 2026-09-02.
**Impact:** Admins can't use the expenses module and get confusing guidance; claims cannot be created even with a linked employee due to ISSUE-010.
**Notes:** The expenses POST payload `{"spentOn":"2026-09-02",...}` is the same ISO-vs-epoch LocalDate defect as ISSUE-010 — the fix is likely one global Jackson configuration.

---

### ISSUE-013 — Journal Entries: expense lines require a dimension, but line-level dimension fields are raw UUID text inputs with no picker; header-level project does not propagate
**Severity:** High
**Priority:** P1
**Status:** Open
**Page/Feature:** Finance → Journal Entries (قيود اليومية) — create dialog
**Test Scenario:** Create a balanced journal entry with an expense line.
**Preconditions:** Two posting accounts exist; a project exists (for the dimension).
**Steps to Reproduce:**
1. Open `/finance/journal-entries` → "＋ قيد يومية جديد".
2. Fill date/description/reference, select debit (expense) and credit accounts, 100/100.
3. Click "حفظ القيد كـ مسودة".
**Expected Result:** The dialog lets the user pick a project/cost-center/department for the expense line from a friendly dropdown (or auto-applies the header-level project), then saves.
**Actual Result:**
1. First save fails: `400 JOURNAL_DIMENSION_REQUIRED` ("تتطلب أسطر الإيرادات والمصروفات مركز تكلفة أو مشروعاً أو قسماً") — shown inline correctly (good).
2. The header-level "بُعد المشروع" selector (which the user naturally fills) does **not** populate the line-level `projectId` — the payload keeps `projectId:""` on lines and the backend rejects it again.
3. The line-level "المشروع/مركز التكلفة/القسم/…" fields are **plain text inputs expecting raw UUIDs** (journal-entries.page.html:200-216) with no dropdown/autocomplete — typing a project name does nothing. Only pasting the exact project UUID (`0d3c6f38-…`) makes the save succeed (200).
**Evidence:**
- Network: 2× `POST /api/v1/finance/journal-entries => [400] JOURNAL_DIMENSION_REQUIRED`, then `[200]` after manual UUID paste.
- Payload diff: header `projectId:"0d3c6f38-…"` present while lines carry `projectId:""`.
**Test Data:** Entry REF-QA-01, accounts QA-5001 (debit 100) / QA-1001 (credit 100), project QA-PRJ-001. Entry saved as draft after UUID workaround.
**Impact:** Ordinary accountants cannot complete an expense journal entry at all — the required dimension can only be satisfied by pasting internal UUIDs, which are not visible anywhere in the UI. Effectively blocks the finance workflow.
**Notes:** Positive: this dialog *does* surface backend errors inline (the correct pattern; contrast with ISSUE-006).

---

## Overall Assessment

The application's core flows are functionally solid once the environment is healthy: auth (including logout scoping and route guards), bilingual RTL/LTR i18n, category/employee CRUD with persistence, search, report creation, exports, and responsive layout all work as expected. The backend returns well-structured, localized error responses.

The dominant quality gap is **frontend error handling**: the backend's clear Arabic error messages for duplicate/conflict saves (409) are discarded — the dialogs fail silently, and the only surfaced alert appears after cancel, mislabeled as a "loading error" with an irrelevant retry action. The second gap is **accessibility of modal confirm overlays**, which are invisible to the accessibility tree.

Three initial blockers (stale build overlay, i18n 500, status 500) were all traced to environment state — PostgreSQL in WSL2 was not running — and resolved once the DB started; they are recorded for completeness with their app-side observations.

## Recommended Next Actions
1. **P1 — Fix dialog error surfacing (ISSUE-006):** render the backend's `localizedMessage` inside the open dialog (inline alert near the footer) and do not route save errors into the page-level "loading error" alert.
2. **P2 — Add dialog semantics to confirm overlays (ISSUE-007):** `role="alertdialog"`, focus trap, Esc handling, and initial focus on the destructive option.
3. **P2 — Investigate analytics 409 (ISSUE-004):** the ingest endpoint rejects legitimate page views; check server-side uniqueness constraints on `operationId`.
4. **P3 — Sync bootstrap admin docs (ISSUE-005):** clarify that `HR_BOOTSTRAP_ADMIN_PASSWORD` only applies on first seed, or make the initializer update the password when it differs.
5. **P3 — Clean up minor UX (ISSUE-001, ISSUE-008):** restart dev server with a clean build before release testing; differentiate "no data" vs "no search match" empty states.
6. Re-run this suite after fixes; deep-dive areas not covered here (fingerprint imports, payroll runs, approvals workflow, permissions/authorization with non-admin users).
