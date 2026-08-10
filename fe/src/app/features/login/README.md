# Login / تسجيل الدخول

**EN:** SaaS app-code plus username/password login that receives a JWT session. The saved user locale is fully loaded and applied before routing into the role-aware shell.

Expired protected sessions arrive with `reason=session-expired` and display the database-backed translated notice. Invalid username/password errors remain separate.

تظهر الجلسة المنتهية برسالة مترجمة من قاعدة البيانات، وتبقى رسالة خطأ اسم المستخدم أو كلمة المرور مستقلة عنها.

يستخدم الدخول كود التطبيق واسم المستخدم وكلمة المرور، ويحمّل لغة المستخدم المحفوظة ويطبقها بالكامل قبل الانتقال إلى الواجهة.

**AR:** دخول باسم المستخدم وكلمة المرور للحصول على جلسة JWT ثم الانتقال إلى الواجهة المناسبة للصلاحيات.

**EN:** Login uses the Bemo ERP product identity and shared installable-app logo while tenant company names remain independent.

**AR:** تستخدم شاشة الدخول هوية Bemo ERP وشعار التطبيق القابل للتثبيت، مع بقاء اسم كل شركة مستقلاً عن اسم المنتج.

**EN:** A demo-only no-login SUPER_ADMIN link (`?my_secret=...`) is exchanged against the public `/api/v1/auth/demo-login` endpoint and routed straight to the dashboard. The secret is stripped from the URL on success and on failure; backend-rejected links show the database-backed translated notice.

**AR:** يدعم رابط الدخول التجريبي للمدير الشامل بدون كلمة مرور (`?my_secret=...`) التبادل مع نقطة `/api/v1/auth/demo-login` العامة ثم الانتقال مباشرة إلى اللوحة. يُحذف السر من عنوان الصفحة عند النجاح والفشل، وتعرض الروابط المرفوضة رسالة مترجمة من قاعدة البيانات.

## 2026-08-10 RTL technical-field direction

**EN:** App code, username, and password are explicitly LTR technical values while labels continue to follow the UI locale. The password reveal action is placed on the opposite edge from the LTR text with reserved input padding, preventing the reveal label from overlapping password dots.

**AR:** أصبحت قيم كود التطبيق واسم المستخدم وكلمة المرور LTR بشكل صريح مع بقاء عناوين الحقول حسب اتجاه اللغة. كما تم فصل زر إظهار كلمة المرور عن نقاط كلمة المرور لمنع التداخل الظاهر في RTL.
