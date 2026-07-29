# Authentication / تسجيل الدخول

**EN:** Stores the short-lived access session locally, attaches the Bearer token, and enforces route roles. Backend authorization remains authoritative.

**EN:** A protected API `401` is treated as session expiry: the local session is cleared and the user is redirected to login with a translated expiry notice. A `401` from the public login request remains an invalid-credentials error and never triggers the expiry redirect.

**AR:** عند رجوع `401` من API محمي تُحذف الجلسة المحلية ويُنقل المستخدم إلى الدخول مع تنبيه مترجم بانتهاء الجلسة. أما `401` من طلب الدخول العام فيظل خطأ بيانات دخول ولا يسبب تحويل انتهاء الجلسة.

**AR:** تحفظ جلسة الوصول محليًا وترسل Bearer token وتتحقق من أدوار المسارات، مع بقاء قرار الأمان النهائي للـ backend.

**EN:** Every API call also receives a fresh cryptographically random client correlation id and the browser installation's stable device id. The server returns its own independent correlation id. Unit tests lock this behavior down.

**EN:** The typed tenant settings contract includes the attendance day-anomaly percentage so administrators can configure biometric-outage detection without bypassing backend validation.

**AR:** يتضمن عقد إعدادات الشركة نسبة شذوذ يوم الحضور حتى يستطيع المدير ضبط اكتشاف تعطل البصمة مع بقاء التحقق النهائي في الخادم.

**AR:** كل API call يرسل رقم تتبع جديدًا ورقم جهاز ثابتًا لهذا المتصفح، والخادم يرجع رقم تتبع مستقلًا خاصًا به.
