# Attendance Browser (مستعرض الحضور)

**EN:** Day-level attendance explorer across employees and categories: filters by date range, category, status (`DailyStatus` union — PRESENT, LATE, SINGLE_PUNCH, NO_PUNCH, MANUAL_ENTRY, …), and review state; renders punch evidence (first/last, punchCount, worked vs expected minutes) beside decisions so HR sees facts and decisions separately. Feeds drill-down from dashboards and the report-review flow.

**AR:** مستعرض الحضور اليومي عبر الموظفين والفئات: تصفية بالفترة والفئة والحالة وأ حالة المراجعة، ويعرض أدلة البصمة (أول/آخر بصمة، عددها، الدقائق العاملة مقابل المتوقعة) بجوار القرارات ليفصل HR بين الحقيقة والقرار. يغذي التنقل من لوحات القيادة وتدفق مراجعة التقارير.

- Data: `AttendanceExplorerApi` punchCount/firstPunch/lastPunch fields; statuses are closed unions — fail visibly on unknown values.
- Bulk decisions themselves live in the report-review feature; this browser is read/inspect-first.
