# Payroll capability / مخرجات الأجور

**EN:** Produces payroll-ready attendance totals for monthly and half-month cycles. It never executes payments.

**AR:** تنتج إجماليات حضور جاهزة للأجور للدورات الشهرية ونصف الشهرية، ولا تنفذ أي دفع مالي.

**EN:** Payroll resolves employee advance deductions through the shared installment engine, posts the employee-ledger settlement and scheduled-advance allocation transactionally, and reverses both when a salary payment is reversed.

**AR:** تحتسب الرواتب خصم سلف الموظفين من محرك الأقساط الموحد، وتسجل تسوية دفتر الموظف وتوزيعها على السلف المجدولة داخل معاملة واحدة، وتعكس الجانبين عند التراجع عن صرف الراتب.

**EN:** Payment recording and approval/posting transitions require an approved attendance report and no open payroll-blocking exception for the affected employee or report.

**EN:** Calculation becomes deterministic when a period is calculated, reviewed, approved, posted, or paid. The service captures one tenant-scoped immutable `PayrollInputSnapshot` per employee/period before persisting results. Salary, attendance minutes, manual adjustments, advance deduction, and the effective policy version/divisor/overtime multiplier are frozen; replay reuses the same snapshot. Effective-dated policies are managed at `/api/v1/payroll/calculation-policies`.

**AR:** يصبح احتساب الفترة حتمياً عند الانتقال إلى الاحتساب أو المراجعة أو الاعتماد أو الترحيل أو الصرف. تحفظ الخدمة لقطة `PayrollInputSnapshot` ثابتة ومعزولة للمستأجر لكل موظف وفترة قبل تثبيت النتائج، وتشمل الراتب ودقائق الحضور والتعديلات وخصم السلفة وإصدار سياسة الرواتب وقيم المقسوم ومعامل الإضافي. أي إعادة تنفيذ تستخدم اللقطة نفسها، وتدار السياسات المؤرخة عبر `/api/v1/payroll/calculation-policies`.

**AR:** يتطلب تسجيل الصرف وانتقالات الاعتماد/الترحيل تقرير حضور معتمداً وألا توجد استثناءات حضور مفتوحة تمنع راتب الموظف أو التقرير.
