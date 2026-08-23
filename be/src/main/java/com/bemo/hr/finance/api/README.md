# Finance API / واجهة المالية

**EN:** Fiscal-period listing is read-only and returns an empty array when a year has no configured periods. It never creates periods as a side effect; authorized users must call the explicit year-generation endpoint.

The treasury API also exposes tenant-scoped Frankfurter exchange-rate hint settings and manual refresh endpoints. Online values are reference metadata only and are never used to overwrite the configured accounting exchange rate.

**AR:** قراءة الفترات المالية عملية بلا آثار جانبية وترجع قائمة فارغة إذا لم تكن السنة مهيأة. لا يتم إنشاء الفترات تلقائياً، بل يستخدم المستخدم المخول مسار إنشاء السنة بصورة صريحة.

كما توفر واجهة الخزينة إعدادات أسعار Frankfurter المرجعية ومساراً للتحديث اليدوي لكل مستأجر. هذه القيم للعرض والاسترشاد فقط ولا تستبدل سعر الصرف المحاسبي المحفوظ.

**EN:** `/api/v1/finance/reports/cash-flow` returns a direct-method cash flow statement derived only from posted GL evidence: operating/investing/financing classification by counter-account, exact opening-to-closing reconciliation (`reconciled` flag), and an equal-length comparative previous period. It never presents net income as cash flow.

**AR:** يعيد المسار `/api/v1/finance/reports/cash-flow` قائمة تدفقات نقدية بالطريقة المباشرة مشتقة من الأدلة المرحلة فقط: تصنيف تشغيلي/استثماري/تمويلي بحسب الأطراف المقابلة، ومطابقة تامة من الافتتاحي إلى الختامي (علم `reconciled`)، وفترة مقارنة مماثلة المدة. ولا يُعرض صافي الدخل أبداً كتدفق نقدي.

## Bank reconciliation / التسوية البنكية

**EN:** `/api/v1/finance/bank-reconciliation` imports balance-validated CSV statements idempotently, exposes statement workbenches with posted-journal suggestions, supports partial/manual/fee matching and controlled reversal, and reports the latest cash position. Matching writes require an open fiscal period and lock the statement aggregate.

**AR:** تستورد واجهة `/api/v1/finance/bank-reconciliation` كشوف CSV بعد التحقق من الأرصدة ومنع التكرار، وتعرض شاشة تسوية باقتراحات من القيود المرحلة، وتدعم المطابقة الجزئية واليدوية والرسوم والعكس المضبوط، كما تعرض أحدث موقف نقدي. تتطلب عمليات المطابقة فترة مالية مفتوحة وتقفل تجميعة الكشف أثناء التعديل.
