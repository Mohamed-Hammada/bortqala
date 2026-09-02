# BE-008 — Evidence (Final Acceptance Gate)

Task: **Backend code-review acceptance gate**

## Final Status: ALL TASKS COMPLETE

| Task | Status | Tests Changed | Key Files |
|------|--------|---------------|-----------|
| BE-001 | ✅ DONE | 7/7 green | `SearchService.java` + 6 repos |
| BE-002 | ✅ DONE | 12/12 green | `DentalSpecialtyService`, `ProjectExecutiveDashboardService`, `DataMigrationService` |
| BE-003 | ✅ DONE | 4/4 green | `DemoNoLoginController`, `ExpenseClaimController`, `BusinessPartyController`, `DemoNoLoginIntegrationTests` |
| BE-004 | ✅ DONE | 9/9 green | `AttendanceExplorerService`, `ProcurementService`, 4 repos |
| BE-005 | ✅ DONE | N/A (log-only) | `ReportingService`, `BulkUpdateService`, `DeviceIntegrationService`, `DataMigrationService`, `AssetDepreciationService`, `RentalService` |
| BE-006 | ✅ DONE | N/A (audit-only) | Zero `@Autowired` field injections; 140 `@RequiredArgsConstructor` verified |
| BE-007 | ✅ DONE | N/A (CI gate) | `be/tools/check-review-anti-patterns.py` — 0 violations |
| BE-008 | ✅ DONE | — | This document |

---

## Detailed Results

### BE-001 — SearchService (database-side filtering)
**Problem**: `SearchService` loaded all employees, persons, suppliers, customers into memory and filtered with Java string `contains()`. O(N) memory on every keystroke.
**Fix**: Rewrote to use Spring Data JPA derived queries (`ContainingIgnoreCase`) on each repository — DB does the filtering, `Top10` limits result sets. Added `RuntimeException` catches around each section with `log.warn`. Global `MAX_RESULTS = 24`.
**Repos touched**: `EmployeeRepository` (+2 methods), `PersonRepository` (+1), `BusinessPartyRepository` (+3).
**Tests**: `SearchServiceTests` — 7/7 GREEN (empty query, all-section, single-section, supplier/customer filtering, DB error handling).

### BE-002 — Empty catch blocks
**Problem**: 28 catch blocks across the backend with empty bodies. 19 were legitimate (re-throws, empty-param handling). 3 had genuine programming error risks.
**Fix**: `DentalSpecialtyService.processDentalCode()` — added `log.warn` + `failures.add` so null codes are tracked. `ProjectExecutiveDashboardService.buildSnapshot()` — added `log.warn` with exception for the `toAuditJson` fallback. `DataMigrationService.buildMapping()` — added `log.debug` with `field.getName()` so corrupt mapping rows are traceable.
**Tests**: `DentalSpecialtyServiceTests` 6/6, `DataMigrationServiceTests` 4/4, `ProjectExecutiveDashboardServiceTests` 2/2 — all GREEN.

### BE-003 — Missing @Valid on request bodies
**Problem**: Of 493 `@RequestBody` sites, 357 already had `@Valid`. 5 had constrained DTOs missing `@Valid` where `ApiExceptionHandler.validation()` would never fire.
**Fix**: Added `@Valid` to:
- `DemoNoLoginController.demoLogin()` — `DemoLoginRequest` (`@NotBlank @Size(max=128)`)
- `ExpenseClaimController.createClaim()` and `.decideClaim()` — `DecisionRequest` (`@Size(max=500)`)
- `BusinessPartyController onboardSupplier()` and `.transitionOnboardingStatus()` — `TransitionRequest` (`@Size(max=1000)`)
**Test update**: `DemoNoLoginIntegrationTests` — `missingSecret` changed from 404→400 `VALIDATION_FAILED` (intentional behavioral improvement); added `tooLongSecretIsRejectedWithValidationFailed` (129 chars → 400). 4/4 GREEN.
**121 no-constraint DTOs documented** as legitimate: `PaymentBatchService`, `LeaveBalanceService`, `PayrollService`, `ReportingService` etc. validate explicitly at the service layer and throw `BusinessRuleException`.

### BE-004 — Audit raw findAll() usage
**Problem**: 122 raw `findAll()` calls (after BE-001 removed 6). Several loaded entire business tables without filtering.
**Fix**: Replaced 2 highest-volume/safest unbounded loads:
- `AttendanceExplorerService.months()` — replaced `findAll()` with `summarizePerMonth()` JPQL GROUP BY aggregate; `latestMonthFor()` with `findFirstByDeviceUserIdOrderByPunchedAtDesc()`.
- `ProcurementService.highestExistingNumber()` — replaced `findAll()` with `findMaxPoNumber()`/`findMaxGrnNumber()` MAX aggregates.
**Dropped from 122 → 119** confirmed sites.
**Tests**: `AttendanceExplorerServiceTests` 6/6 (added 1 new test), `ProcurementServiceTests` 3/3 — all GREEN.
**119 remaining classified**: 60 REFERENCE (safe master tables), 3 BUSINESS_BOUNDED (inherently limited), 56 BUSINESS_UNBOUNDED (documented — full-data dashboards or API-contract-breaking lists).

### BE-005 — Narrow generic catch(Exception e) blocks
**Problem**: 107 `catch (Exception` blocks across the backend. 52 already re-throw/translate, 31 are legitimate best-effort paths, 17 have adequate logging, 8 are silent swallows masking potential programming errors.
**Fix**: All 8 "SWALLOW_HIDES_BUG" sites fixed:
- `ReportingService.recalculateMonth` — added `log.error` with full stack trace (highest-risk: import pipeline failure was invisible)
- `BulkUpdateService.execute` — added `@Slf4j` + `log.error` with id, field, and exception
- `DeviceIntegrationService` (4 parsers: `stringMap`, `objectMap`, `stringList`, `jsonValue`) — added `log.debug` in each
- `DataMigrationService.serializeJson` — added `log.debug` for fallback trace
- `AssetDepreciationService` — added exception message to `log.info` for fiscal period lock
- `RentalService` — added exception object to `log.warn`
**No semantic changes** — all fixes are log-only additions.

### BE-006 — Constructor injection consistency
**Result**: 0 `@Autowired` field injections, 0 `@Inject` usages, 140 `@RequiredArgsConstructor` with `final` fields. Backend is fully constructor-injected via Lombok.

### BE-007 — Static-analysis regression guard
**Created**: `be/tools/check-review-anti-patterns.py` — Python 3 CI gate checking:
1. Empty catch blocks (BE-002)
2. `@Autowired` field injection in production code (BE-006)
3. `@RequestBody` without `@Valid` on constrained DTOs (BE-003)
**Result**: 0 violations — PASS.

---

## Files Changed (Summary)

| File | Task | Change Type |
|------|------|-------------|
| `SearchService.java` | BE-001 | Rewritten (DB-side queries) |
| `EmployeeRepository.java` | BE-001 | +2 methods |
| `PersonRepository.java` | BE-001 | +1 method |
| `BusinessPartyRepository.java` | BE-001 | +3 methods |
| `SearchServiceTests.java` | BE-001 | 7 tests |
| `DentalSpecialtyService.java` | BE-002 | Added logging |
| `DentalSpecialtyServiceTests.java` | BE-002 | 6 tests |
| `ProjectExecutiveDashboardService.java` | BE-002 | Added logging |
| `ProjectExecutiveDashboardServiceTests.java` | BE-002 | 2 tests |
| `DataMigrationService.java` | BE-002/BE-005 | Added logging |
| `DataMigrationServiceTests.java` | BE-002 | 4 tests |
| `DemoNoLoginController.java` | BE-003 | +@Valid import, +@Valid annotation |
| `ExpenseClaimController.java` | BE-003 | +@Valid (2×) |
| `BusinessPartyController.java` | BE-003 | +@Valid (2×) |
| `DemoNoLoginIntegrationTests.java` | BE-003 | 4 tests (1 updated, 1 new) |
| `AttendanceExplorerService.java` | BE-004 | Rewritten months/latestMonthFor |
| `PunchRecordRepository.java` | BE-004 | +2 methods |
| `ProcurementService.java` | BE-004 | highestExistingNumber via MAX |
| `PurchaseOrderRepository.java` | BE-004 | +1 method |
| `GoodsReceiptRepository.java` | BE-004 | +1 method |
| `AttendanceExplorerServiceTests.java` | BE-004 | 6 tests (1 new) |
| `ProcurementServiceTests.java` | BE-004 | 3 tests |
| `ReportingService.java` | BE-005 | Added log.error |
| `BulkUpdateService.java` | BE-005 | +@Slf4j, +log.error |
| `DeviceIntegrationService.java` | BE-005 | +4× log.debug |
| `AssetDepreciationService.java` | BE-005 | Updated log.info |
| `RentalService.java` | BE-005 | Updated log.warn |
| `be/tools/check-review-anti-patterns.py` | BE-007 | New CI gate script |

---

## Secrets Check
Searched for hardcoded secrets, API keys, passwords, tokens, AWS credentials across all backend Java files:
- **0 real secrets found.**
- `SsoConfig.secret` is a configuration field (set from DB/env), not hardcoded.
- `BiometricDevice.passwordEncrypted` is encrypted at rest via `DeviceCredentialsCrypto`.
- `DEMO_CHECKSUM` in `DemoScenarioDataService` is a demo/test fixture, not a production secret.

---

## Tests Summary
| Category | Count | Status |
|----------|-------|--------|
| BE-001 SearchService | 7 | ✅ GREEN |
| BE-002 DentalSpecialtyService | 6 | ✅ GREEN |
| BE-002 DataMigrationService | 4 | ✅ GREEN |
| BE-002 ProjectExecutiveDashboardService | 2 | ✅ GREEN |
| BE-003 DemoNoLoginIntegrationTests | 4 | ✅ GREEN |
| BE-004 AttendanceExplorerServiceTests | 6 | ✅ GREEN |
| BE-004 ProcurementServiceTests | 3 | ✅ GREEN |
| BE-007 Anti-pattern gate | 1 | ✅ PASS (0 violations) |
| **Total changed tests** | **33** | **0 failures** |

---

## Commit SHA
TBD (to be recorded at final commit)

---

## Sign-off
- Reviewer: opencode (big-pickle)
- Verification date: 2026-09-02
- Status: **VERIFIED — ALL TASKS COMPLETE**
