# Product analytics and activation / تحليلات المنتج والتفعيل

**EN:** Event payloads accept only the explicit scalar allowlist (`source`, `result`, `count`, `durationMs`, `route`, `objectType`); unknown, nested, or long values are rejected. The web client strips query/fragment data before navigation capture. Events are tenant-owned and operation-idempotent, increment durable daily aggregates, and achieve six deterministic milestones once.

`ProductEventSink` invokes the transactional recorder through a separate Spring proxy and catches failures outside the ERP transaction, so analytics cannot roll back business work. Raw retention never deletes daily aggregates or milestones. Tenant summaries use Hibernate isolation; the cross-tenant SQL summary is exposed only by a method explicitly protected with `SUPER_ADMIN`.

**AR:** تقبل خصائص الحدث قائمة قيم بسيطة صريحة فقط، وتُرفض المفاتيح غير المعروفة والقيم المركبة أو الطويلة. تحذف واجهة الويب query وfragment قبل تسجيل التنقل. الأحداث معزولة للمستأجر وآمنة عند تكرار `operationId` وتزيد التجميع اليومي الدائم وتحقق ست مراحل حتمية مرة واحدة.

تستدعي `ProductEventSink` المسجل المعاملي عبر Spring proxy منفصل وتلتقط الفشل خارج معاملة ERP، فلا يمكن للتحليلات إرجاع عمل تجاري. لا يحذف الاحتفاظ الخام التجميعات أو المراحل. تستخدم ملخصات المستأجر عزل Hibernate، ولا يتاح ملخص SQL العابر للمستأجرين إلا لطريقة محمية صراحةً بـ `SUPER_ADMIN`.
