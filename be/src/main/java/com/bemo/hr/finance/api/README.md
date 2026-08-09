# Finance API / واجهة المالية

**EN:** Fiscal-period listing is read-only and returns an empty array when a year has no configured periods. It never creates periods as a side effect; authorized users must call the explicit year-generation endpoint.

The treasury API also exposes tenant-scoped Frankfurter exchange-rate hint settings and manual refresh endpoints. Online values are reference metadata only and are never used to overwrite the configured accounting exchange rate.

**AR:** قراءة الفترات المالية عملية بلا آثار جانبية وترجع قائمة فارغة إذا لم تكن السنة مهيأة. لا يتم إنشاء الفترات تلقائياً، بل يستخدم المستخدم المخول مسار إنشاء السنة بصورة صريحة.

كما توفر واجهة الخزينة إعدادات أسعار Frankfurter المرجعية ومساراً للتحديث اليدوي لكل مستأجر. هذه القيم للعرض والاسترشاد فقط ولا تستبدل سعر الصرف المحاسبي المحفوظ.
