# BE-003 — Missing @Valid on request bodies

**Priority:** P1

## Objective

Sweep the approximately 111 @RequestBody parameters reported without @Valid.

Priority examples: CrmController, CostCenterController, PaymentBatchController, EssController, TranslationAdminController, OfflineLicensingController, DisasterRecoveryBackupController.

Add @Valid where DTO Bean Validation is intended. Where validation is genuinely not appropriate, document the reason and ensure equivalent explicit validation where required. Ensure validation errors use the existing global exception handling convention. Add tests for invalid and valid payloads, especially finance-adjacent endpoints. Do not weaken DTO constraints just to make endpoints pass.

## Acceptance Criteria

- [ ] Root cause investigated.
- [ ] Correct implementation completed.
- [ ] Relevant tests added/updated.
- [ ] Build/static analysis/tests pass.
- [ ] Runtime/manual verification completed where applicable.
- [ ] No unrelated regression identified.
- [ ] EVIDENCE.md completed.

Only mark complete after implementation AND verification.
