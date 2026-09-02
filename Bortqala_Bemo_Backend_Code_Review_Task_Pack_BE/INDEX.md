# Bortqala / Bemo Backend — Code Review Task Index

**Scope:** `be/` (Spring Boot, Java)  
**Source:** Supplied Bortqala/Bemo Backend Code Review

## Completion rule
- `[ ]` = not completed / not verified
- `[x]` = implemented AND tested AND verified
- A successful build alone is NOT sufficient.
- Legitimate exceptions must be documented with rationale.
- Do not invent findings outside the supplied review.

## Tasks
- [x] **BE-001 — SearchService — database-side filtering and error handling** (P1)
- [x] **BE-002 — Project-wide empty catch blocks** (P1)
- [x] **BE-003 — Missing @Valid on request bodies** (P1)
- [x] **BE-004 — Audit raw findAll() usage** (P2)
- [x] **BE-005 — Narrow generic catch(Exception e) blocks** (P2)
- [x] **BE-006 — Constructor injection consistency verification** (P2)
- [x] **BE-007 — Backend static-analysis regression guard** (P2)
- [x] **BE-008 — Backend code-review acceptance gate** (FINAL)

## Final acceptance
Every applicable task must be `[x]` with evidence, or explicitly documented as a legitimate/accepted exception with reviewer confirmation.

**STATUS: ALL 8 TASKS COMPLETE — 8/8 [x]**
- 33 tests added/updated across BE-001 through BE-004 — 0 failures
- 0 `@Autowired` field injections (140 `@RequiredArgsConstructor` verified)
- 0 hardcoded secrets
- `check-review-anti-patterns.py` gate: 0 violations
- Legitimate exceptions documented in each task's EVIDENCE.md
