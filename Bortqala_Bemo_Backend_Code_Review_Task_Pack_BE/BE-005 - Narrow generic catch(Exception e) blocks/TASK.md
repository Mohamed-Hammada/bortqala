# BE-005 — Narrow generic catch(Exception e) blocks

**Priority:** P2

## Objective

Review the 49 generic catch(Exception e) blocks reported separately from empty catches.

Identify expected failure modes, narrow catches to specific exception types where practical, preserve global exception handling, and do not hide programming errors such as NullPointerException behind generic recovery. Log useful context when recovery is intentional and propagate failures when the caller must know. Add regression tests for changed error paths.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
