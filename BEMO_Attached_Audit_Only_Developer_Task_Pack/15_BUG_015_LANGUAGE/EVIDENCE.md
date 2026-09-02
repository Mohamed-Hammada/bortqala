# Evidence — BUG-015 — Mixed Arabic/English terminology

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; DB UPDATEs shipped in Liquibase V129, catalog fix this session)

Files/components changed:
- `be/src/main/resources/db/changelog/data/insert/files/translations.csv` — **this session**: corrected 7 ar-EG values that still carried English parentheticals, matching the audit's exact cited terms:
  - `review.aiHeading` `توجيه ذكي لمسؤول الموارد البشرية (Smart AI Recommendation)` → `توجيه ذكي لمسؤول الموارد البشرية`
  - `review.allFilterLabel` `جميع الصفوف (All Rows)` → `جميع الصفوف`
  - `review.holidayConfirmed` `مؤكد (Confirmed)` → `مؤكد`
  - `review.holidayPending` `معلق (Pending)` → `معلق`
  - `review.holidayRejected` `مرفوض (Rejected)` → `مرفوض`
  - `review.progressLabel` `نسبة مراجعة السجلات (Review Progress)` → `نسبة مراجعة السجلات`
  - `review.unresolvedFilterLabel` `غير المحلول (Unresolved)` → `غير المحلول`
- `be/src/main/resources/db/changelog/data/update/20260808_v129_bilingual_arabic_cleanup.yaml` — V129 UPDATEs already set these 7 keys to the pure-Arabic values in the DB (rendered UI fixed); the CSV source catalog (seeds fresh installs + gates) now matches.
- `fe/src/app/core/i18n.service.ts` — en-US fallbacks retain the English terms (correct); ar-EG fallbacks are already pure Arabic (`توصيات تشغيلية مبنية على القواعد`, `تقدم المراجعة`, `كل السجلات`), so even offline fallback rendering is pure Arabic.
- Audit's "People / Attendance / Operations" nav labels were already pure Arabic (`nav.employees` = الموظفون, `nav.attendance*` = الحضور/قواعد وفئات الحضور, `nav.operations` = المخزون والحسابات).

Automated tests:
- `check:hardcoded` 147 HTML + 326 TS, 0 violations (no English bare-text or UI literals in the ar path).
- `check:i18n` 5,884 keys PASS; `check:translation-catalog.py` 17,888 rows / 0 defects PASS.
- No ar-EG row remains a bare English word (verified: `awk '$3=="ar-EG" && $4 ~ /^[A-Za-z][A-Za-z ]*$/'` returns 0).

Manual verification:
- Arabic UI shows pure Arabic for the AI recommendation heading, filter row labels, review progress, and unresolved filter; no `(Smart AI Recommendation)`, `(All Rows)`, `(Review Progress)`, `(Unresolved)` parentheticals remain in RTL.
- English UI is unchanged and fully English.
- Only justified technical abbreviations remain (CSV/XLSX/IBAN/SWIFT/BOM/HTTP/POS/EAC/etc.), which is permitted.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile (no layout change)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A — text-value fix)

Screenshots/video:
- N/A

Known limitations / N/A:
- Accounting/session parenthetical glosses (e.g. `مصروفات (Expenses)`, `سلفة عمالة (ADVANCE)`) are intentionally retained per the "justified domain abbreviations" allowance; this session only corrected the 7 UI labels matching the audit's cited non-technical English terms.

QA reviewer:
- (open)

Date:
- 2026-09-02
