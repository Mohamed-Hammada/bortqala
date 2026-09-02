# BE-004 — Audit raw findAll() usage

**Priority:** P2

## Objective

Review the 127 raw .findAll() calls across the backend. For each occurrence, decide whether the table is guaranteed to remain small/reference-sized.

For potentially growing business tables, replace unbounded loading with filtered queries, pagination, projections, streaming/batching or another bounded strategy. Preserve ordering and business semantics. Document genuinely safe reference-table uses where useful. Add tests for high-volume paths where material.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
