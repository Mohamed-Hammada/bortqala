# BE-001 — SearchService — database-side filtering and error handling

**Priority:** P1

## Objective

File: be/src/main/java/com/bemo/hr/platform/application/SearchService.java

Replace unbounded repository.findAll() + Java-side filtering for employees, parties, invoices, purchase orders, projects and journal entries.

Push filtering into database/repository queries, use case-insensitive matching as appropriate, and enforce a bounded result limit/page. Replace silent catch(Exception ignored) with appropriate logging and deliberate error handling. Preserve search semantics and add regression/performance-oriented tests. Verify empty, short, mixed-case and no-result searches.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
