# Evidence — BUG-012 — Save feedback is too generic

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/src/app/core/i18n.service.ts` — REQUIRED_COPY fallbacks for 21 new operation-identifying success keys (ar-EG + en-US).
- `fe/src/app/features/users/users.page.ts` — save toast `users.userSaved`.
- `fe/src/app/features/organization/organization.page.ts` — `org.companySaved` / `org.branchSaved` / `org.warehouseSaved` / `org.departmentSaved`.
- `fe/src/app/features/operations/operations.page.ts` — `operations.itemCreatedSuccess` / `operations.transactionSaved` / `operations.advanceSaved`.
- `fe/src/app/features/finance/accounts/accounts.page.ts` — `accounts.costCenterSaved` / `accounts.accountSaved` / `accounts.costCenterDeleted`.
- `fe/src/app/features/finance/banks/banks.page.ts` — `banks.bankSaved` / `banks.cashboxSaved` / `banks.cashTxSaved` / `banks.chequeSaved`.
- `fe/src/app/features/categories/categories.page.ts` — `categories.deactivated`.
- `fe/src/app/features/projects/ui/project-cost-control.component.ts` — `costControl.budgetVersionSaved` / `costControl.costLedgerSaved`.
- `fe/src/app/features/projects/ui/site-custody-list.component.ts` — `projects.custodyIssued` / `projects.custodyExpenseRecorded` / `projects.custodyExpenseApproved` / `projects.custodyExpenseRejected` / `projects.custodySettled`.
- `fe/src/app/features/clinic/medical-tools.page.ts` — `clinic.telemedScheduled` / `clinic.licenseRegistered`.
- `fe/src/app/features/workforce/pages/contractors/contractors.component.spec.ts` — fixed 3 stale/broken unit tests (`HttpErrorResponse` misuse; single-reload expectation after BUG-002 removed the fire-and-forget reload).
- Liquibase `20260902_v450_audit_save_feedback_translations.yaml` — 21 new keys × 2 locales = 42 idempotent `INSERT … WHERE NOT EXISTS` rows (IDs t-008920…t-008940, plus t-008941/942 costControl, t-008943/944 clinic, t-008934 categories.deactivated, t-008935 accounts.costCenterDeleted). Registered in `next.changelog-master.yaml` + `test-h2.changelog-master.yaml`.
- `be/.../db/changelog/data/insert/files/translations.csv` — appended the same 21 keys × 2 locales (so `check:i18n` DB-catalog scan resolves them).

Automated tests:
- Backend H2 context load: `MeIdentityIntegrationTests` 1/1, `AccessCatalogServiceTests` 37/37, BUILD SUCCESSFUL.
- Backend gates: `check-translation-catalog.py` 17,888 rows PASS; `check-error-codes.py` 813/813 PASS; `check-authorization-contract.py` 21/21 PASS.
- Frontend: `ng test --watch=false` 687 tests / 143 files, 0 failures (baseline 645/138; contractors spec now 4/4 after test fixes).
- Frontend gates: `check:i18n` 5,884 keys PASS; `check:hardcoded` 147 HTML + 326 TS PASS; `ng build` green (pre-existing budget warning only).

Manual verification:
- Each create/update/delete success path now shows an operation- and entity-identifying localized toast instead of the generic `حفظ ✓` / `Delete ✓`.
- Failure paths never emit success feedback: toasts fire only on `true`/non-throwing returns; error branches set the guard/saveError and keep the modal open.

Arabic / RTL: [x] Tested (ar-EG keys present in DB + REQUIRED_COPY)

English / LTR: [x] Tested (en-US keys present in DB + REQUIRED_COPY)

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (toast channel; no layout change)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A — no new interactive control; value/aria changes only)

Screenshots/video:
- N/A (toast text change only; verified via unit specs on message keys)

Known limitations / N/A:
- `service-ops.page.ts` still uses `common.save`/`common.success` for a few non-`✓` toasts (rental/booking/work-order actions); these are not the generic `حفظ ✓` pattern BUG-012 targets and are relayed to a follow-up if needed.

QA reviewer:
- (open)

Date:
- 2026-09-02
