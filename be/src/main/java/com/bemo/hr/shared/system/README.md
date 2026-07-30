# System status / حالة النظام

**EN:** `GET /api/v1/system/status` is public and reports backend availability, build version, server time, and the durable client-cache version. The response reads PostgreSQL, so a successful response also proves the application can reach its database. `POST /api/v1/admin/system/cache-version` is restricted to `SUPER_ADMIN`; it rotates the cache version and records the actor, time, and optional reason so clients can clear stale PWA caches safely.

**AR:** المسار العام `GET /api/v1/system/status` يعرض حالة الخادم وإصدار البناء ووقت الخادم وإصدار كاش الواجهة المحفوظ في PostgreSQL، ولذلك فإن نجاحه يؤكد أيضاً اتصال التطبيق بقاعدة البيانات. المسار `POST /api/v1/admin/system/cache-version` متاح فقط لـ `SUPER_ADMIN` ويغيّر إصدار الكاش مع تسجيل المنفذ والوقت والسبب الاختياري، لكي تمسح تطبيقات PWA الملفات القديمة بصورة آمنة.
