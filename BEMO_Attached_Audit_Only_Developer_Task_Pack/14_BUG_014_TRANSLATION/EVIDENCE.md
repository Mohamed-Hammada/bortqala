# Evidence — BUG-014 — Raw translation key visible

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; the `nav.settingsHint` key row added in Liquibase V129 prior session)

Files/components changed:
- `fe/src/app/core/navigation/app-navigation.ts` — sidebar Settings nav items carry `descriptionKey: 'nav.settingsHint'` (lines 816, 838), rendered through `i18n.t(...)` so the DB value, not the raw key, is displayed.
- Translation rows for `nav.settingsHint`:
  - ar-EG `إعدادات النظام والتفضيلات` (`t-004695-ar`)
  - en-US `System settings and preferences` (`t-004695-en`)
  in `data/insert/files/translations.csv` + `i18n.service.ts` REQUIRED_COPY fallbacks (both locales).
- `check:i18n` gate is the automated "missing rendered translation key" detector: every `i18n.t` / `labelKey` / `descriptionKey` literal is validated to exist in both `ar-EG` and `en-US` before CI passes.

Automated tests:
- `check:i18n` 5,884 keys PASS (0 missing; no `nav.*` raw key resolvable to a literal).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures (i18n.service.spec asserts `nav.settingsHint` resolution).
- `check:hardcoded` PASS.

Manual verification:
- No sidebar item shows a literal `nav.settingsHint` (or any other raw key); each renders its Arabic/English value per current locale.

Arabic / RTL: [x] Tested (إعدادات النظام والتفضيلات)

English / LTR: [x] Tested (System settings and preferences)

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A — text-node fix)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
