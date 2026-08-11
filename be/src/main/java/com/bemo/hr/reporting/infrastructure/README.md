# Reporting infrastructure / بنية التقارير

**EN:** Repository boundaries for reports, daily snapshots, and holiday proposals.

**AR:** مستودعات التقارير والنتائج اليومية المجمدة واقتراحات الإجازات.
# Day-anomaly persistence / تخزين شذوذ الأيام

**EN:** Tenant-filtered repositories persist anomaly headers and immutable affected-result snapshots with report/category/date and operation uniqueness.

**AR:** تحفظ المستودعات المعزولة حسب الشركة رأس حالة الشذوذ ولقطات النتائج المتأثرة، مع تفرد التقرير والفئة والتاريخ ومعرف العملية.

**EN:** Advanced-attendance repositories stay tenant-filtered; exception writes use pessimistic selection and a unique report/result/type evidence key, while payroll lookups use the report/employee/status/blocking index.

**AR:** تبقى مستودعات الحضور المتقدم معزولة حسب الشركة؛ وتقفل كتابة الاستثناءات الصفوف المختارة وتمنع تكرار دليل التقرير/النتيجة/النوع، بينما تستخدم بوابة الرواتب فهرس التقرير والموظف والحالة والحظر.
