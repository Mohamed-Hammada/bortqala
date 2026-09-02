# Evidence — BUG-011 — Duplicate inventory unit headers

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; fix shipped in Liquibase V449 prior session)

Files/components changed:
- `fe/src/app/features/operations/operations.page.html` — the Stock Balances table renders two distinct header columns: `operations.unit` (Unit code / رمز الوحدة) and `operations.uom` (Unit name / اسم وحدة القياس), populated respectively from `item.unitCode` and `item.uomName`.
- Liquibase V449 `20260901_v449_operations_unit_header_translations.yaml` —
  - `operations.unit` = `رمز الوحدة` (ar-EG) / `Unit code` (en-US)
  - `operations.uom` = `اسم وحدة القياس` (ar-EG) / `Unit name` (en-US)
  so the two columns no longer collide under the same Arabic label.

Automated tests:
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures.
- Backend H2 context load clean (V449 registered in `next` + `test-h2`): `MeIdentityIntegrationTests` + `AccessCatalogServiceTests` green.
- Gates: `check:i18n` 5,884 keys PASS; `check:translation-catalog.py` PASS.

Manual verification:
- In ar-EG the two unit columns show distinct headers (رمز الوحدة vs اسم وحدة القياس) with distinct cell data (unit code vs unit name); no duplicate "وحدة القياس" headers remain.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A - header text only)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
