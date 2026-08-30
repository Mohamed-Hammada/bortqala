# Compliance — Egyptian ETA E-Invoicing (الامتثال — الفاتورة الإلكترونية)

**EN:** Egyptian Tax Authority e-invoicing compliance engine (roadmap item 22, migrations V308–V310). Prepares invoice documents for ETA submission per the Egyptian e-invoicing rules, tracks submission/validation state per document, and exposes admin endpoints under the compliance package (`EtaComplianceController`, explicit `@PreAuthorize`). UI lives at `fe/src/app/features/compliance/eta-tax` (models/service/page/spec).

**AR:** محرك الامتثال للفاتورة الإلكترونية لمصلحة الضرائب المصرية (البند 22 من خارطة الطريق، ترحيلات V308–V310). يجهّز مستندات الفواتير وفق قواعد مصلحة الضرائب، ويتتبع حالة الإرسال/التحقق لكل مستند، ويوفر نقاط نهاية إدارية (`EtaComplianceController` بصلاحيات صريحة). الواجهة: `fe/src/app/features/compliance/eta-tax`.

- Scope v1: document preparation + status tracking; live ETA submission credentials/network calls are environment-dependent and validated at deployment.
- Related: retail POS uses the separate ETA **e-receipt** regime — not implemented yet (see `missing-todo.md` §18).
- All exception codes are DB-translated (ar-EG/en-US) and resolved through `ApiExceptionHandler`.
