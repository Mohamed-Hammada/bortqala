# Bemo License Service / خدمة تراخيص Bemo

## English

Independent Spring Boot 4 + Gradle service for issuing perpetual, year-based, or fixed-expiry licenses. A raw license key is returned only when created; the database stores its SHA-256 hash. Activations are limited and bound to an installation id, a hashed device fingerprint, and an Ed25519 device public key. Activation takes a database write lock on the licence before counting seats, and a deactivated installation can safely reactivate without violating its unique database key.

The server signs certificates with its Ed25519 private key. Only the public key belongs in the desktop application. Validation and deactivation requests must be signed by the installation private key, so another device cannot release or reuse the activation.

Required production environment variables: `LICENSE_DB_URL`, `LICENSE_DB_USERNAME`, `LICENSE_DB_PASSWORD`, `LICENSE_ADMIN_KEY`, `LICENSE_SIGNING_PRIVATE_KEY`, and `LICENSE_SIGNING_PUBLIC_KEY`. Generate the signing pair once with `LicenseSigningKeyTool`; store the private key in a secrets manager and back it up securely.

Endpoints: admin creation at `POST /api/v1/licenses`; public activation, validation, and deactivation under `/public/v1/activations`. Only the Tauri desktop client uses them. The ordinary web application has no licence dependency and is controlled through active/disabled user accounts. The desktop contacts this service when the customer enters the key for first activation; later startups verify the pinned signed certificate and Windows-bound fingerprint locally. Settings can release the device, and the NSIS pre-uninstall hook calls the same signed deactivation flow.

## العربية

خدمة مستقلة مبنية بـ Spring Boot 4 وGradle لإصدار ترخيص دائم أو لعدد سنوات أو حتى تاريخ محدد. يظهر مفتاح الترخيص الخام مرة واحدة عند الإنشاء، بينما تُحفظ بصمته SHA-256 فقط. التفعيل محدود العدد ومرتبط بمعرّف التثبيت وبصمة جهاز مُجزّأة ومفتاح Ed25519 عام خاص بالجهاز.

يوقّع الخادم شهادة الترخيص بمفتاحه الخاص، ولا يوضع داخل تطبيق سطح المكتب إلا المفتاح العام. يوقّع الجهاز طلبات التحقق وإلغاء التفعيل بمفتاح التثبيت الخاص، لذلك لا يستطيع جهاز آخر تحرير المفتاح أو انتحال التثبيت.

متغيرات الإنتاج المطلوبة هي: `LICENSE_DB_URL` و`LICENSE_DB_USERNAME` و`LICENSE_DB_PASSWORD` و`LICENSE_ADMIN_KEY` ومفتاحا التوقيع. يجب حفظ المفتاح الخاص في مدير أسرار مع نسخة احتياطية آمنة.

عند إزالة التطبيق يجب إرسال طلب إلغاء التفعيل. إذا كان الجهاز غير متصل، يجب تحذير المستخدم أن المفتاح لن يصبح متاحًا لجهاز آخر قبل نجاح الاتصال بالخدمة.

تستخدم Tauri لسطح المكتب هذه الخدمة فقط، ولا يعتمد عليها تطبيق الويب. يتصل تطبيق سطح المكتب عند إدخال المفتاح لأول تفعيل، ثم يتحقق محليًا في التشغيلات اللاحقة من الشهادة الموقعة وبصمة Windows. يتم التحكم في الويب عبر تفعيل حسابات المستخدمين أو تعطيلها مؤقتًا أو بصفة دائمة.
