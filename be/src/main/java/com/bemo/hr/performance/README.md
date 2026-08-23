# Performance Appraisals & KPI Cycles (تقييم الأداء)

**EN:** KPI-based performance appraisal engine (roadmap item 20, V303–V305): define KPI definitions with weight and target, run evaluation cycles per period, score employees per KPI (manager entry, optional self-assessment), compute weighted totals backend-side, and expose cycle dashboards. Results are immutable evidence once the cycle closes; reopening requires an explicit admin action recorded in audit.

**AR:** محرك تقييم أداء مبني على مؤشرات الأداء (البند 20، ترحيلات V303–V305): تعريف المؤشرات بالوزن والمستهدف، ودورات تقييم لكل فترة، وإدخال درجات الموظفين على كل مؤشر (إدخال المدير وتقييم ذاتي اختياري)، وحساب الإجماليات الموزونة في الخادم، ولوحات متابعة الدورات. النتائج دليل غير قابل للتعديل بعد إغلاق الدورة، وإعادة الفتح تحتاج صلاحية إدارة ويُسجَّل في سجل التدقيق.

- Layers: standard api/application/domain/infrastructure under `com.bemo.hr.performance`.
- Frontend: `fe/src/app/features/performance` (models/service/page/spec).
- Weighted totals must never be duplicated in Angular — endpoints return computed scores.
- Translations: V304–V305 carry ar-EG/en-US rows for UI copy and exception codes.
