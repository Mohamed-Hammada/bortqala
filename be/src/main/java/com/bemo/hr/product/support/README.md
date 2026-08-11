# Support and customer health / الدعم وصحة العميل

**EN:** Tenant users create operation-idempotent tickets with priority, module, screen, business impact and description. Critical tickets require a meaningful impact and receive a four-hour SLA; all priorities have deterministic SLA deadlines. Admin transitions follow an explicit state machine with optimistic locking, immutable comments and audit evidence. Feedback accepts a strict type/rating contract and stores only safe route context stripped of query and fragment values.

The customer-health calculation is deterministic and snapshots seven explained dimensions: activation, usage, workflow adoption, data quality, operational health, support load and commercial state. The weights total 100. Every dimension returns positive/action-required status and a safe remediation route, so no unexplained score is shown. Calculation is tenant-locked, operation-idempotent and audited.

**AR:** ينشئ مستخدمو المستأجر تذاكر آمنة عند تكرار العملية تتضمن الأولوية والوحدة والشاشة وأثر العمل والوصف. تتطلب التذاكر الحرجة أثراً مفصلاً وتحصل على مهلة استجابة أربع ساعات، ولكل أولوية موعد حتمي. تتبع انتقالات الإدارة آلة حالات صريحة مع قفل الإصدار وتعليقات ثابتة ودليل تدقيق. تقبل الملاحظات أنواعاً وتقييماً مضبوطين ولا تحفظ إلا مساراً آمناً بعد حذف query وfragment.

يحسب مؤشر صحة العميل نتيجة حتمية ويحفظ سبعة أبعاد مشروحة: التفعيل والاستخدام وتبني المسارات وجودة البيانات والصحة التشغيلية وعبء الدعم والحالة التجارية. مجموع الأوزان 100، ويعيد كل بُعد حالة إيجابية أو إجراء مطلوب ومسار إصلاح آمن، لذلك لا توجد درجة غامضة. الحساب مقفول على المستأجر وآمن عند التكرار ومسجل في التدقيق.
