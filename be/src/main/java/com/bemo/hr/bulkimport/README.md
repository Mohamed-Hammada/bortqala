# Bulk Smart Import (الاستيراد الذكي بالجملة)

**EN:** Generic Excel import engine for business data (employees, items with barcodes/UoM, stock counts, shift rosters, customers/suppliers…). Defines a workflow catalog (`SmartImportCatalog`) with sheet/column metadata in English and Arabic, validates uploaded rows (`SmartImportValidator` — required fields, duplicates by natural keys like ItemCode/Barcode, numeric/date coercion) and returns bounded row-level errors before commit. Upload guards are configurable via `hr.workforce-import.max-file-bytes` / `max-rows` / `preview-limit` (defaults 20 MB / 20 000 / 100); preview must stay bounded to the batch's own rows.

**AR:** محرّك استيراد إكسل عام لبيانات الأعمال (الموظفين، الأصناف والباركود ووحدات القياس، الجرد، ورديات الشهر، العملاء والموردون). يعرّف كتالوج القوالب (`SmartImportCatalog`) بأعمدته ثنائية اللغة، ويتحقق من الصفوف (`SmartImportValidator`: الإلزامي، التكرار بالمفاتيح الطبيعية مثل كود الصنف/الباركود، التنسيقات) ويعيد أخطاء صفوف محدودة قبل الحفظ. حدود الرفع قابلة للضبط عبر `hr.workforce-import.*` والمعاينة محدودة بصفوف الدفعة نفسها فقط.

- Layers: `api` (upload/validate endpoints) · `application` (catalog + validator + batch orchestration) · `domain` (row models).
- Concurrency: repeated `operationId` on the same batch yields exactly one applied import (pessimistic lock, see workforce import commit tests).
- Templates: every structured-import screen offers a downloadable template file.
