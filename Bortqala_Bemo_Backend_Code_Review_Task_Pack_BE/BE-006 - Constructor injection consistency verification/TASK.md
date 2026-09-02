# BE-006 — Constructor injection consistency verification

**Priority:** P2

## Objective

Verify constructor injection consistency across the backend. The review found no field-level @Autowired, which is good. Perform a focused manual/AST-assisted check of newer medical and fleet modules. Convert any safe field injection to constructor injection while preserving Spring wiring and tests. Avoid unrelated architectural changes.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
