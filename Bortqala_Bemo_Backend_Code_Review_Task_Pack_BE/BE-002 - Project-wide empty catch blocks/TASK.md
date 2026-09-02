# BE-002 — Project-wide empty catch blocks

**Priority:** P1

## Objective

Review all 28 empty catch blocks identified in the review, including the 12 occurrences in medical/*.

For every occurrence, determine whether the exception is genuinely expected. If it is safe to ignore, document why and keep the handling narrow. Otherwise log useful context and/or propagate/translate the failure. Prefer specific exception types over Exception. Do not log secrets or sensitive payloads. Add regression tests where behavior changes.

Locations:
- shared/idempotency/infrastructure/IdempotencyHeaderFilter.java:138
- attendance/infrastructure/SpreadsheetBiometricFileReader.java:302,327,343
- platform/application/SearchService.java:64,79,91,103,116,129
- project/executive/application/ProjectExecutiveDashboardService.java:169
- workforce/WorkforceExcelImportService.java:438
- medical/application/HospitalOpsService.java:74,108,391,413
- medical/application/PharmacyService.java:68
- medical/application/InsuranceService.java:59,231
- medical/application/MedicalLabService.java:50,83,154,216
- medical/application/DentalSpecialtyService.java:96
- fleet/application/FleetService.java:416,466
- migration/application/DataMigrationService.java:107
- organization/application/IntercompanyService.java:193

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
