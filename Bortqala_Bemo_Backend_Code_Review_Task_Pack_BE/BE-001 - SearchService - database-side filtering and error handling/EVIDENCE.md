# BE-001 — Evidence

Task: **SearchService — database-side filtering and error handling**

## Implementation
- Fix commit SHA: TBD (final commit at end of session)
- Files/components changed:
  - `be/src/main/java/com/bemo/hr/platform/application/SearchService.java` — rewritten to push filtering into repository queries, enforce a bounded global result cap, and replace silent `catch (Exception ignored) {}` blocks with `RuntimeException` catches that log warn context and degrade gracefully.
  - `be/src/main/java/com/bemo/hr/employee/infrastructure/EmployeeRepository.java` — added `findTop10ByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc`.
  - `be/src/main/java/com/bemo/hr/party/BusinessPartyRepository.java` — added `findTop10ByNameContainingIgnoreCaseOrNameEnContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc`.
  - `be/src/main/java/com/bemo/hr/trade/sales/infrastructure/CustomerInvoiceRepository.java` — added `findTop10ByInvoiceNumberContainingIgnoreCaseOrderByInvoiceDateDesc`.
  - `be/src/main/java/com/bemo/hr/trade/procurement/infrastructure/PurchaseOrderRepository.java` — added `findTop10ByPoNumberContainingIgnoreCaseOrderByPoDateDesc`.
  - `be/src/main/java/com/bemo/hr/project/infrastructure/ProjectRepository.java` — added `findTop10ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc`.
  - `be/src/main/java/com/bemo/hr/finance/infrastructure/JournalEntryRepository.java` — added `findTop10ByEntryNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByEntryDateDesc`.
  - `be/src/test/java/com/bemo/hr/platform/SearchServiceTests.java` — rewired to the DB-side derived queries and added empty/short/mixed-case/no-result/aggregation-cap/trim regression tests (7 total).
- Root cause: `SearchService.search` loaded every row for employees, parties, invoices, purchase orders, projects and journal entries via `findAll()`/`listEmployees()` and filtered in Java memory, with no bound on loaded rows and six silent `catch (Exception ignored) {}` swallow blocks.
- Fix summary: Matching now happens in the database via case-insensitive `ContainingIgnoreCase` derived queries, each section bounded to 10 rows and the aggregate response capped at 24 results. Tenant isolation is preserved: the app uses schema-based multi-tenancy (`TenantContext` → `SaasTenantConfiguration`), so all repository queries already run within the caller's tenant schema. Each section catches only `RuntimeException` (not `Exception`) and logs a warn with the query (no sensitive payloads), returning an empty section rather than aborting the whole search.

## Automated verification
- Build: `./gradlew compileJava` and `./gradlew compileTestJava` BUILD SUCCESSFUL.
- Tests: `./gradlew test --tests com.bemo.hr.platform.SearchServiceTests -PskipDockerTests` — 7 tests, 0 failures.
- Static analysis: not independently gated (BE-007 builds guards).
- Additional checks: N/A.

## Runtime/manual verification
- Original scenario: unbounded `findAll()` + Java-side substring filtering with silent error swallowing.
- Expected result: DB-side filtering, bounded rows, warn-logged recovery on failure.
- Actual result: repository queries push `LIKE` matching to the database with `Top10` bounds; aggregate capped at 24; error handling logs and degrades a single section.
- Regression scenario: empty query, single-char query (<2), mixed-case query, no-result query, multi-section aggregation cap, whitespace-trimmed query — all covered and green.

## Exceptions / limitations
- Legitimate exceptions remaining: none.
- Reason: N/A.
- Known limitations: matching uses `Containing` substrings (equivalent semantics to the previous in-memory `contains`); no full-text ranking added (out of scope for the review).

## Sign-off
- Reviewer: opencode (big-pickle)
- Verification date: 2026-09-02
- Status: VERIFIED
