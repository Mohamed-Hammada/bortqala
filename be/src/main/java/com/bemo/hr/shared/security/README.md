# Security / الأمان والصلاحيات

**EN:** Username/password authentication, BCrypt hashes, stateless HS256 JWTs, configurable trusted CORS origins, user administration, and multiple roles per user. Production requires `HR_JWT_SECRET`.

**EN:** Each SaaS application stores an administrator-controlled session timeout between 5 minutes and 7 days. Login uses it to calculate the expiry of newly issued JWTs. Protected `401` responses are handled by the frontend as session expiry.

**AR:** تحفظ كل شركة مدة جلسة يحددها مدير النظام بين 5 دقائق و7 أيام، ويستخدمها الدخول لحساب انتهاء JWT الجديدة. تتعامل الواجهة مع `401` للطلبات المحمية كانتهاء للجلسة.

**AR:** دخول باسم مستخدم وكلمة مرور مشفرة بـ BCrypt، وJWT بلا جلسة، وOrigins موثوقة قابلة للإعداد، وإدارة مستخدمين مع عدة أدوار لكل مستخدم. الإنتاج يتطلب `HR_JWT_SECRET`.
