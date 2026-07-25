# License API / واجهة الترخيص

## English

The admin endpoint creates a raw key once. Public endpoints activate, validate, and deactivate installations. Every device request carries an Ed25519 proof and a timestamp limited by the configured clock-skew window.

## العربية

تنشئ واجهة المدير المفتاح الخام مرة واحدة. واجهات الأجهزة العامة مسؤولة عن التفعيل والتحقق وإلغاء التفعيل، وكل طلب جهاز يحمل توقيع Ed25519 ووقتًا ضمن نافذة السماح المحددة.
