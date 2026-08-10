# Reporting API / واجهة التقارير

**EN:** Period creation, evidence review, decisions, holiday confirmation, approval/reopen, and Excel download.

**AR:** إنشاء الفترات ومراجعة الدليل والقرارات وتأكيد الإجازة والاعتماد/إعادة الفتح وتنزيل Excel.
# Day-anomaly API / واجهة شذوذ الأيام

**EN:** Report APIs expose detect, decide, reverse, and reopen operations. Decision operation IDs are idempotent and mutations are permission-checked server-side.

**AR:** توفر واجهات التقارير اكتشاف الحالة واتخاذ القرار وعكسه وإعادة فتحه، مع معرف عملية غير مكرر وصلاحيات مطبقة في الخادم.

**EN:** `/api/v1/attendance/policies` manages policy creation/listing. Report-scoped exception endpoints detect, query the workbench, preview a bulk resolution, and apply it with a retry-safe operation ID.

**AR:** تدير `/api/v1/attendance/policies` إنشاء السياسات وعرضها، بينما تكشف واجهات الاستثناءات داخل التقرير الاكتشاف ومنضدة المراجعة والمعاينة الجماعية والتنفيذ الآمن عند إعادة المحاولة.
