# Reporting domain / نطاق التقارير

**EN:** Report lifecycle, frozen daily evidence, HR decisions, holiday proposals, and the Spring-free daily calculator. `singlePunchCounts=true` produces non-blocking presence from one punch; otherwise one punch is a blocking exception.

**AR:** دورة التقرير ودليل الأيام المجمد وقرارات HR واقتراحات الإجازات والحاسبة اليومية المستقلة عن Spring. البصمة الواحدة تكون حضورًا غير معطل فقط عندما تسمح سياسة الفئة بذلك، وإلا تظل استثناءً يجب مراجعته.
# Day anomalies / شذوذ الأيام

**EN:** `DayAnomaly` owns the Open, Deferred, Resolved, Reversed lifecycle. `DayAnomalyResultSnapshot` retains the previous attendance decision for safe audited reversal.

**AR:** يدير `DayAnomaly` دورة مفتوحة ومؤجلة ومعالجة ومعكوسة، ويحفظ `DayAnomalyResultSnapshot` قرار الحضور السابق لتراجع آمن ومسجل.

**EN:** `AttendancePolicy` defines effective hierarchy and thresholds. `AttendanceException` retains score, type, policy version/JSON snapshot, payroll impact, resolution, actor, and operation ID. Cross-midnight attribution stays in the Spring-free calculator.

**AR:** يحدد `AttendancePolicy` ترتيب السياسات وحدودها حسب تاريخ السريان، ويحفظ `AttendanceException` الدرجة والنوع ونسخة السياسة وتأثير الرواتب والقرار والمنفذ ومعرف العملية. ويبقى إسناد الوردية العابرة لمنتصف الليل داخل الحاسبة المستقلة عن Spring.
