# BE-007 — Backend static-analysis regression guard

**Priority:** P2

## Objective

Where compatible with the existing build, add lightweight automated guards against recurrence of the reviewed anti-patterns:
- empty catch blocks;
- broad catch(Exception) blocks for review;
- unbounded findAll() in business paths where feasible;
- request bodies missing @Valid when DTO validation is present.

Avoid false positives for legitimate/documented cases. Integrate with the existing developer/CI workflow without breaking valid code.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
