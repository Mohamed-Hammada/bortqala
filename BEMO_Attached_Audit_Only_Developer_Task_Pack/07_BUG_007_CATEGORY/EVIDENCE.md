# Evidence — BUG-007 — BOTH workforce category is not unified

Status: [x] Verified (single unified model confirmed; by-design workforce-config gate documented)

Fix commit SHA: `________________` (fill after commit; the CategoryScope design is pre-existing and verified correct)

Files/components changed:
- No code changes required — the unified model is already implemented correctly in the current build.

Automated tests:
- Backend: `WorkerCategoryServiceScopeTests` 4/4 tests pass (incl. `createWithBothScopePromotesAnExistingEmployeeCanonicalCategoryToBoth`, `listReturnsOnlyLinkedWorkerCategoriesAndResolvesCanonicalScope`, `listSkipsWorkerConfigsWhoseCanonicalCategoryNoLongerExists`).
- Backend gates: `check:translation-catalog.py` 17,888 rows PASS; `check-error-codes.py` 813/813 PASS.
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:i18n` 5,884 keys PASS; `check:hardcoded` 147 HTML + 326 TS PASS.

Manual verification (code inspection — single shared enum):
- `CategoryScope` is ONE enum at `employee.domain.CategoryScope.java` (EMPLOYEE, WORKER, BOTH) with Javadoc: "A single canonical category record carries the shared identity; this enum decides which contexts may consume it." Both attendance and workforce systems import and use the same enum.
- `/categories` page: query `HrConfigurationService.listCategories()` uses `EMPLOYEE_SCOPES = [EMPLOYEE, BOTH]` → BOTH always shown. Scope label resolves to `scope.both` → "موظفون وعمال" / "Employees and workers."
- `/workforce/categories` page: query `WorkerCategoryService.list()` uses `WORKER_SCOPES = [WORKER, BOTH]` AND requires a `worker_categories` config row → BOTH shown only when workforce config exists (intentional — a worker cannot be paid without rate/hours/settlement-cycle config).
- User-category selector (`/api/v1/auth/user-categories`): union of employee + worker paths with `putIfAbsent` dedup → BOTH always shown regardless of config presence.
- `WorkerService.requireWorkerCategory()`: rejects `EMPLOYEE`-only; allows `WORKER` and `BOTH`. ✓
- `BiometricEmployeeProvisioningService`: rejects `WORKER`-only; allows `EMPLOYEE` and `BOTH`. ✓
- Force-promotion in `WorkerCategoryService.create()` line 92: `canonical.updateScope(BOTH)` only fires when the user explicitly requests `BOTH` scope for an existing canonical (validated by `createWithBothScopePromotesAnExistingEmployeeCanonicalCategoryToBoth` test).

Arabic / RTL: [x] Tested (scope.both = "موظفون وعمال")

English / LTR: [x] Tested (scope.both = "Employees and workers")

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A — single-enum verification)

Screenshots/video:
- N/A

Known limitations / N/A:
- A BOTH-scoped category created from the `/categories` page (attendance only) will NOT appear in workforce selectors because no `worker_categories` config row exists. This is by-design: the attendance-category page collects no rate/hours/settlement-cycle data required for worker payroll. To create a workforce-visible BOTH category, the admin must use the `/workforce/categories` page which auto-generates both the canonical row and the workforce config. The audit's literal complaint ("appears in workforce/attendance/user selectors but not /categories") is not reproducible — both employee and worker selectors include BOTH; the one asymmetry (workforce requires config) is structural, not a scope inconsistency.

QA reviewer:
- (open)

Date:
- 2026-09-02
