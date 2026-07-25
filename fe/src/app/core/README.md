# Core / نواة الواجهة

**EN:** Authentication, JWT transport, guards, database-backed Arabic/English i18n, user theme/density application, the role-aware shell, and cross-feature error handling. `I18nService` starts through `provideAppInitializer`, so routes do not render raw translation keys while the initial bundle is loading.

**AR:** تسجيل الدخول وإرسال JWT والحراس وترجمة عربية/إنجليزية من قاعدة البيانات وتطبيق المظهر/الكثافة وقالب الصلاحيات ومعالجة الأخطاء المشتركة. يبدأ `I18nService` من `provideAppInitializer` حتى لا تُرسم المسارات قبل اكتمال قاموس اللغة ولا تظهر مفاتيح الترجمة الخام.
