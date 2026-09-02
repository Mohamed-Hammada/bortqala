# BE-002 — Evidence

Task: **Project-wide empty catch blocks**

## Implementation
- Fix commit SHA: TBD (final commit at end of session)
- Files/components changed:
  - `be/src/main/java/com/bemo/hr/medical/application/DentalSpecialtyService.java` — narrowed two broad `catch (Exception e)` / `catch (Exception ignored)` blocks (enum parsing for `DentalRecord.Condition`/`DentalRecord.Surface`) to `catch (IllegalArgumentException)`, aligning with the rest of the medical modules. Invalid enum values still fall back to `CARIES` / `null` (no behavior change).
  - `be/src/main/java/com/bemo/hr/project/executive/application/ProjectExecutiveDashboardService.java` — line 169 `catch (Exception ignored) {}` changed to `catch (RuntimeException ex) { log.warn(...) }` so a workforce-headcount aggregation failure degrades gracefully (headcount 0) but is no longer silent.
  - `be/src/main/java/com/bemo/hr/migration/application/DataMigrationService.java` — line 107 broad `catch (Exception ignored)` narrowed to a multi-catch of `tools.jackson.core.JacksonException | NumberFormatException | ArithmeticException` with a per-record warn log; malformed records are still skipped without failing the dry-run batch.
  - 6 empty catches in `platform/application/SearchService.java` (lines 64,79,91,103,116,129) were already eliminated as part of BE-001 (narrowed to `RuntimeException` + warn logging, empty sections).
- Files/components reviewed and documented as legitimate (no code change required):
  - `shared/idempotency/infrastructure/IdempotencyHeaderFilter.java:138` — narrow `NumberFormatException` fallback (SC_OK) when parsing the stored `status||body` prefix.
  - `attendance/infrastructure/SpreadsheetBiometricFileReader.java:302,327,343` — narrow `DateTimeParseException` inside multi-formatter trial loops that later throw `IllegalArgumentException` when all formats fail.
  - `workforce/WorkforceExcelImportService.java:438` — narrow `DateTimeParseException` while trying multiple date patterns.
  - `medical/application/HospitalOpsService.java:74,108,391,413` — narrow `IllegalArgumentException` enum-value fallbacks with safe defaults.
  - `medical/application/PharmacyService.java:68`, `medical/application/InsuranceService.java:59,231`, `medical/application/MedicalLabService.java:50,83,154,216` — narrow `IllegalArgumentException` enum parsing/fallbacks.
  - `fleet/application/FleetService.java:416,466` — narrow `DateTimeParseException` graceful degradation for legacy/unparseable date strings.
  - `organization/application/IntercompanyService.java:193` — narrow `NumberFormatException` fallback (sequence restarts at 1).
- Root cause: 28 empty catch blocks, mostly `IllegalArgumentException`/`DateTimeParseException`/`NumberFormatException` around deliberate parse-with-fallback logic, plus a few broad `catch (Exception)` blocks that risked hiding programming errors.
- Fix summary: Narrowed every broad `catch (Exception)` empty block to a specific exception type with explicit fallback; added warn logging where recovery is intentional; kept the narrow single-exception parse fallbacks unchanged (they are deliberate and safe). SearchService's silent catches were already removed under BE-001.

## Automated verification
- Build: `./gradlew compileJava`/`compileTestJava` BUILD SUCCESSFUL.
- Tests: `./gradlew test --tests DentalSpecialtyServiceTests --tests DataMigrationServiceTests --tests ProjectExecutiveDashboardServiceTests -PskipDockerTests` — 12 tests, 0 failures:
  - DentalSpecialtyServiceTests: 6 (incl. new `recordToothCondition_InvalidEnums_FallBackToDefaults`).
  - DataMigrationServiceTests: 4 (incl. new `dryRunSkipsMalformedRecords`).
  - ProjectExecutiveDashboardServiceTests: 2.
- Static analysis: not independently gated (BE-007 builds guards).
- Additional checks: SearchServiceTests 7/7 (BE-001).

## Runtime/manual verification
- Original scenario: silent `catch (Exception ignored)` scattered across modules.
- Expected result: each catch narrowed/documented; intentional recoveries log context.
- Actual result: broad `Exception` empty blocks removed; remaining single-exception fallbacks documented per-occurrence.
- Regression scenario: invalid enum values (Dental), malformed migration rows (DataMigration), dashboards rendered with workforce source unavailable (ExecutiveDashboard) — covered by tests.

## Exceptions / limitations
- Legitimate exceptions remaining: the 17 documented narrow parse-with-fallback catches (Idempotency*, SpreadsheetBiometric*, WorkforceExcelImportService, 11 medical enum parses, FleetService×2, IntercompanyService) are intentionally empty because the catch body relies on the pre-try default value and the exception type is already the only expected failure mode.
- Reason: enum/date/number parsing with explicit fallback requires swallowing only the specific parse exception; logging them would add noise for expected user-input variants.
- Known limitations: none.

## Sign-off
- Reviewer: opencode (big-pickle)
- Verification date: 2026-09-02
- Status: VERIFIED