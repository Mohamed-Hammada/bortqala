# Security / الأمان والصلاحيات

**EN:** Username/password authentication, BCrypt hashes, stateless HS256 JWTs, configurable trusted CORS origins, user administration, and multiple roles per user. Production requires `HR_JWT_SECRET`.

**EN:** Each SaaS application stores an administrator-controlled session timeout between 5 minutes and 7 days. Login uses it to calculate the expiry of newly issued JWTs. Protected `401` responses are handled by the frontend as session expiry.

**AR:** تحفظ كل شركة مدة جلسة يحددها مدير النظام بين 5 دقائق و7 أيام، ويستخدمها الدخول لحساب انتهاء JWT الجديدة. تتعامل الواجهة مع `401` للطلبات المحمية كانتهاء للجلسة.

**AR:** دخول باسم مستخدم وكلمة مرور مشفرة بـ BCrypt، وJWT بلا جلسة، وOrigins موثوقة قابلة للإعداد، وإدارة مستخدمين مع عدة أدوار لكل مستخدم. الإنتاج يتطلب `HR_JWT_SECRET`.

**EN:** Per-user navigation preferences persist favorite and recently used menu IDs, section visibility, and the recent-item limit through `/api/v1/auth/preferences/navigation`, so the same settings follow the user across devices without changing permissions.

**AR:** تُحفظ تفضيلات التنقل لكل مستخدم، بما فيها المفضلة والعناصر المستخدمة حديثاً وإظهار الأقسام والحد الأقصى للعناصر، عبر `/api/v1/auth/preferences/navigation` لتتزامن بين الأجهزة دون تغيير الصلاحيات.

**EN:** User category assignment is persisted on `app_users.category_id`; the assignment selector combines active employee and worker categories and rejects inactive or unknown IDs at the API boundary.

**AR:** يُحفظ تعيين فئة المستخدم في `app_users.category_id`، ويجمع المحدد فئات الموظفين والعمال النشطة ويرفض الخادم أي معرف غير معروف أو غير نشط.
**EN:** Tenant application settings include the procurement numbering policy. Automatic mode is the default; administrators may switch to validated unique manual PO/GRN numbers.

## Dashboard policy / سياسة لوحة المتابعة

**EN:** Dashboard layouts and motion preferences are persisted per user. An admin can enable or disable layout customization for each user, while only a Super Admin can enable or disable customization for Admin accounts at tenant level. Motion remains an independent accessibility preference.

**AR:** تُحفظ عناصر لوحة المتابعة وترتيبها وإعداد الحركة لكل مستخدم. يستطيع المدير تفعيل أو إيقاف تخصيص التخطيط لكل مستخدم، بينما يتحكم المدير الشامل وحده في إتاحة التخصيص لحسابات المديرين على مستوى الشركة. ويظل إعداد الحركة خياراً مستقلاً لسهولة الاستخدام.

**AR:** تشمل إعدادات الشركة سياسة ترقيم مستندات المشتريات. الوضع التلقائي هو الافتراضي، ويمكن للمدير التحويل إلى أرقام يدوية غير مكررة ومتحقق منها لأوامر الشراء وأذون الاستلام.
