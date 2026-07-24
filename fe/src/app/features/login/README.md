# Login / تسجيل الدخول

**EN:** SaaS app-code plus username/password login that receives a JWT session. The saved user locale is fully loaded and applied before routing into the role-aware shell.

Expired protected sessions arrive with `reason=session-expired` and display the database-backed translated notice. Invalid username/password errors remain separate.

تظهر الجلسة المنتهية برسالة مترجمة من قاعدة البيانات، وتبقى رسالة خطأ اسم المستخدم أو كلمة المرور مستقلة عنها.

يستخدم الدخول كود التطبيق واسم المستخدم وكلمة المرور، ويحمّل لغة المستخدم المحفوظة ويطبقها بالكامل قبل الانتقال إلى الواجهة.

**AR:** دخول باسم المستخدم وكلمة المرور للحصول على جلسة JWT ثم الانتقال إلى الواجهة المناسبة للصلاحيات.
