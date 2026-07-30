# Security / الأمان والصلاحيات

**EN:** Username/password authentication, BCrypt hashes, stateless HS256 JWTs, configurable trusted CORS origins, user administration, and multiple roles per user. Production requires `HR_JWT_SECRET`.

**EN:** Each SaaS application stores an administrator-controlled session timeout between 5 minutes and 7 days. Login uses it to calculate the expiry of newly issued JWTs. Protected `401` responses are handled by the frontend as session expiry.

**AR:** تحفظ كل شركة مدة جلسة يحددها مدير النظام بين 5 دقائق و7 أيام، ويستخدمها الدخول لحساب انتهاء JWT الجديدة. تتعامل الواجهة مع `401` للطلبات المحمية كانتهاء للجلسة.

**AR:** دخول باسم مستخدم وكلمة مرور مشفرة بـ BCrypt، وJWT بلا جلسة، وOrigins موثوقة قابلة للإعداد، وإدارة مستخدمين مع عدة أدوار لكل مستخدم. الإنتاج يتطلب `HR_JWT_SECRET`.

## Mandatory bootstrap / التهيئة الإلزامية

**EN:** Every application start idempotently ensures one operational Admin and one Super Admin after Liquibase has installed the mandatory `ADMIN` and `SUPER_ADMIN` roles. Non-dev deployments must provide `HR_BOOTSTRAP_APP_CODE`, `HR_BOOTSTRAP_APP_NAME`, `HR_BOOTSTRAP_ADMIN_USERNAME`, `HR_BOOTSTRAP_ADMIN_PASSWORD`, `HR_BOOTSTRAP_SUPER_ADMIN_USERNAME`, and `HR_BOOTSTRAP_SUPER_ADMIN_PASSWORD`; startup fails when any value is missing. Demo records are not part of this bootstrap.

**AR:** يضمن كل تشغيل للتطبيق ـ بدون تكرار ـ وجود حساب مدير تشغيل وحساب مدير شامل بعد أن يضيف Liquibase دوري `ADMIN` و`SUPER_ADMIN` الإلزاميين. يجب على البيئات غير التطويرية توفير متغيرات `HR_BOOTSTRAP_*` الخاصة بالشركة والحسابين، ويتوقف التشغيل بوضوح إذا كانت أي قيمة مفقودة. البيانات التجريبية ليست جزءاً من هذه التهيئة.

**EN:** Per-user navigation preferences persist favorite and recently used menu IDs, section visibility, and the recent-item limit through `/api/v1/auth/preferences/navigation`, so the same settings follow the user across devices without changing permissions.

**AR:** تُحفظ تفضيلات التنقل لكل مستخدم، بما فيها المفضلة والعناصر المستخدمة حديثاً وإظهار الأقسام والحد الأقصى للعناصر، عبر `/api/v1/auth/preferences/navigation` لتتزامن بين الأجهزة دون تغيير الصلاحيات.

**EN:** User category assignment is persisted on `app_users.category_id`; the assignment selector combines active employee and worker categories and rejects inactive or unknown IDs at the API boundary.

**AR:** يُحفظ تعيين فئة المستخدم في `app_users.category_id`، ويجمع المحدد فئات الموظفين والعمال النشطة ويرفض الخادم أي معرف غير معروف أو غير نشط.
**EN:** Tenant application settings include the procurement numbering policy. Automatic mode is the default; administrators may switch to validated unique manual PO/GRN numbers.

**EN:** Tenant settings also store the configurable daily attendance anomaly threshold used to detect probable biometric-device outages.

**AR:** تحفظ إعدادات الشركة أيضاً نسبة شذوذ الحضور اليومية القابلة للضبط والمستخدمة لاكتشاف احتمالات تعطل أجهزة البصمة.

## Dashboard policy / سياسة لوحة المتابعة

**EN:** Dashboard layouts and motion preferences are persisted per user. An admin can enable or disable layout customization for each user, while only a Super Admin can enable or disable customization for Admin accounts at tenant level. Motion remains an independent accessibility preference.

**AR:** تُحفظ عناصر لوحة المتابعة وترتيبها وإعداد الحركة لكل مستخدم. يستطيع المدير تفعيل أو إيقاف تخصيص التخطيط لكل مستخدم، بينما يتحكم المدير الشامل وحده في إتاحة التخصيص لحسابات المديرين على مستوى الشركة. ويظل إعداد الحركة خياراً مستقلاً لسهولة الاستخدام.

**AR:** تشمل إعدادات الشركة سياسة ترقيم مستندات المشتريات. الوضع التلقائي هو الافتراضي، ويمكن للمدير التحويل إلى أرقام يدوية غير مكررة ومتحقق منها لأوامر الشراء وأذون الاستلام.

**EN:** Public health and PWA cache-generation endpoints are explicitly permitted without a JWT. Rotating the cache generation remains protected by `SUPER_ADMIN` authorization at the API layer.

**AR:** يُسمح بالوصول العام لمساري فحص الحالة وإصدار كاش PWA دون JWT، بينما يظل تغيير إصدار الكاش محمياً بصلاحية `SUPER_ADMIN` على مستوى الـ API.
