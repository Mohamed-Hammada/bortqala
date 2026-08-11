# Reporting application / تطبيق التقارير

**EN:** Transactional report orchestration, pay-cycle-aware monthly/half-month period discovery, company-zone-aware dashboard metrics, and the Excel-export boundary.

**AR:** تنسيق معاملات التقارير واكتشاف الفترات الشهرية ونصف الشهرية حسب دورة الاستحقاق ومؤشرات لوحة المتابعة حسب المنطقة الزمنية للشركة وحد تصدير Excel.

**EN:** `ReportingService` detects threshold breaches, applies one day-anomaly decision transactionally, preserves independently reviewed rows, and reverses only rows still owned by the anomaly marker.

**AR:** يكتشف `ReportingService` تجاوز النسبة ويطبّق قرار شذوذ اليوم داخل معاملة واحدة، ويحافظ على الصفوف التي روجعت مستقلاً، ويعكس فقط الصفوف التي ما زالت مرتبطة بعلامة الحالة.

**EN:** `AttendanceExceptionService` resolves policies deterministically, aggregates evidence idempotently, previews bulk commands without mutation, locks selected evidence during apply, updates report blockers, and exposes approval/payroll readiness guards.

**AR:** يختار `AttendanceExceptionService` السياسة بصورة حتمية، ويجمع الأدلة دون تكرار، ويعاين الأوامر الجماعية دون تعديل، ويقفل الأدلة المختارة أثناء التنفيذ، ويحدّث عوائق التقرير، ويوفر حراس جاهزية الاعتماد والرواتب.
