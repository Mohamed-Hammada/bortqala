# Advanced approval engine / محرك الاعتمادات المتقدم

**EN:** A submission stores the workflow version, document summary, and an immutable copy of every applicable step. Decisions always read that copy, so later workflow edits cannot change active or historical evidence. `ANY_N` counts distinct decision makers and locks the instance while deciding to prevent concurrent threshold races.

**AR:** يحفظ طلب الاعتماد إصدار المسار وملخص المستند ونسخة ثابتة من كل خطوة مطبقة. تقرأ القرارات هذه النسخة دائماً، لذلك لا تغير تعديلات المسار اللاحقة الطلبات الجارية أو الأدلة التاريخية. تعد سياسة `ANY_N` أصحاب القرار المختلفين وتقفل الطلب أثناء القرار لمنع سباقات التزامن.

**EN:** Dated delegations may be global or document-type scoped. The decision audit records both the acting delegate and the original approver. Administrators can reassign a stuck snapshot step with a mandatory reason. Every step may define SLA hours; the tenant-aware scheduler marks overdue requests once and the task API exposes due time, aging, escalation level, and signature progress.

**AR:** يمكن أن يكون التفويض المؤرخ عاماً أو مقيداً بنوع مستند، ويسجل التدقيق المستخدم المنفذ وصاحب الصلاحية الأصلي معاً. يستطيع المسؤول إعادة تعيين خطوة متوقفة مع سبب إلزامي. ويمكن لكل خطوة تحديد ساعات مستوى الخدمة، ثم يعلّم المجدول الواعي بالمستأجر الطلب المتأخر مرة واحدة وتعرض واجهة المهام الموعد والعمر ومستوى التصعيد وتقدم التوقيعات.
