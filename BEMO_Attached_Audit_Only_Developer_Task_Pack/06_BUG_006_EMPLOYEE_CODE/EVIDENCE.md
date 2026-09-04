# Evidence — BUG-006 — Employee code duplicated on create

Status: [x] Verified (historical-data corruption; current create path round-trips unchanged)

Fix commit SHA: `________________` (fill after commit; fix + dedup shipped in prior REM-004/EmployeeCodeDedup sessions)

Files/components changed:
- `be/src/main/java/com/bemo/hr/employee/application/HrConfigurationService.java` — `standardizeEmployeeCode()` (lines 263-291): a **user-supplied** code is normalized (trim + uppercase) but never re-prefixed/appended (lines 266-280; doc comment "never prepend the category code again (BUG-006)"). Auto-generation (prefix + `%04d` when the code is blank) is the only place a category prefix is added. `createEmployee` (155) and `updateEmployee` (174) use it.
- `be/src/main/java/com/bemo/hr/employee/domain/Employee.java` — stores the passed code as-is (trim/uppercase), lines 74/90.
- `be/src/main/java/com/bemo/hr/bulkimport/application/EmployeeMasterImportHandler.java` — Excel `employeeCode` passed through unchanged (77-87).
- `fe/src/app/features/employees/employees.page.ts` — `employeeCode` plain control (303), edit pre-fills identical stored value (387), raw value forwarded on submit (417-428). Template input `employees.page.html` line 206.
- Uniqueness: `EmployeeRepository.existsByEmployeeCodeIgnoreCase` (+ `...AndIdNot`) used in `standardizeEmployeeCode` throwing `HRCFG_EMPLOYEE_CODE_EXISTS`; DB unique `uq_employees_app_employee_code` (V1) + Postgres case-insensitive `uq_employees_app_lower_employee_code` (V142).
- Historical cleanup: `EmployeeCodeDedupService` (`duplicatedCanonical` returns canonical for `X-X`; endpoint `POST /api/v1/employees/code-corrections`), Liquibase V131 one-off data remediation + `daily_results` sync.

Automated tests:
- `HrConfigurationEmployeeTests.rejectsDuplicateEmployeeCodeWithinTenantButAllowsSameCodeInAnotherTenant` (line 107) — create `EMP1` preserves `employeeCode() == "EMP1"`, second `EMP1` rejected (create-path round-trip regression).
- `EmployeeCodeDedupServiceTests` (5 tests) — exact-pattern detection, dry-run, apply+audit+sync, conflict suffix, idempotency.
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:i18n` PASS.

Manual verification:
- Enter `QA-EMP-0807` in the create form → stored as `QA-EMP-0807` (never `QA-EMP-0807-QA-EMP-0807`); edit form shows the same code; reports/selectors show the same code.
- Duplicate entering an existing code is rejected with a localized already-exists error.

Arabic / RTL: [x] Tested (error localized)

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A — value round-trip)

Screenshots/video:
- N/A

Known limitations / N/A:
- Any remaining `X-X` rows in an existing deployment are legacy data from the earlier bug, corrected by running the dedup service/v131 once (idempotent).

QA reviewer:
- (open)

Date:
- 2026-09-02
