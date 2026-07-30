# Core / نواة الواجهة

**EN:** Authentication, JWT transport, guards, database-backed Arabic/English i18n, user theme/density application, the role-aware shell, and cross-feature error handling. `I18nService` starts through `provideAppInitializer`, so routes do not render raw translation keys while the initial bundle is loading.

**AR:** تسجيل الدخول وإرسال JWT والحراس وترجمة عربية/إنجليزية من قاعدة البيانات وتطبيق المظهر/الكثافة وقالب الصلاحيات ومعالجة الأخطاء المشتركة. يبدأ `I18nService` من `provideAppInitializer` حتى لا تُرسم المسارات قبل اكتمال قاموس اللغة ولا تظهر مفاتيح الترجمة الخام.

**EN:** The shell consumes server-backed navigation preferences for synchronized favorites and recently used sections; hiding either section never changes route authorization.

**AR:** يقرأ قالب التطبيق تفضيلات التنقل المحفوظة في الخادم لمزامنة المفضلة والمستخدمة حديثاً، وإخفاء أي قسم لا يغير صلاحيات المسارات.

**EN:** The Angular workspace exposes a Vitest unit-test target, so `npm test -- --watch=false` runs the committed specifications once in CI or local verification.

**AR:** تتضمن مساحة عمل Angular هدف اختبارات Vitest، ولذلك يشغّل الأمر `npm test -- --watch=false` الاختبارات المضافة مرة واحدة في التحقق المحلي أو CI.

**EN:** Every literal UI translation key is represented in the database migration catalog for both `ar-EG` and `en-US`; `npm run check:i18n` prevents either locale from drifting behind the typed frontend copy.

**AR:** كل مفتاح ترجمة حرفي في الواجهة مسجل في ترحيلات قاعدة البيانات لكل من `ar-EG` و`en-US`، ويمنع الأمر `npm run check:i18n` تأخر أي لغة عن نصوص الواجهة المعتمدة.

**EN:** The shell exposes both expand-all and collapse-all workspace actions and standardizes Enter-to-submit for ordinary form inputs; native Tab navigation, multiline entry and select interaction remain intact.

**AR:** يوفر قالب التطبيق زري فتح وغلق كل مجموعات القوائم ويوحد إرسال النماذج بزر Enter من حقول الإدخال العادية، مع الحفاظ على تنقل Tab وإدخال النص متعدد الأسطر وتفاعل القوائم.

**EN:** The permission-aware shortcut layer provides `Ctrl+K` quick navigation, `?` help, `G`-then-letter menu chords, arrow/Enter result navigation, and descriptive menu/action tooltips. Shortcuts are ignored while users type in form controls.

**AR:** توفر طبقة الاختصارات المراعية للصلاحيات انتقالاً سريعاً عبر `Ctrl+K`، ودليل الاختصارات عبر `?`، وانتقالاً للقوائم بحرف `G` ثم حرف الوجهة، مع أسهم وEnter للنتائج وتلميحات واضحة. ولا تعمل اختصارات الحروف أثناء الكتابة داخل الحقول.

## Visible tooltips / التلميحات المرئية

**EN:** Shell navigation, quick actions, shortcuts, menu controls, and dashboard actions use the shared styled tooltip overlay instead of relying on the browser's native `title` popup.

**AR:** تستخدم عناصر تنقل التطبيق والإجراءات السريعة والاختصارات وأزرار القوائم واللوحة تلميح التطبيق المرئي المشترك بدلاً من الاعتماد على نافذة `title` الافتراضية للمتصفح.

## PWA lifecycle / دورة تطبيق PWA

**EN:** Production builds generate an Angular service worker and installable Bemo ERP manifest. Startup and 30-second checks call `/api/v1/system/status`; when its durable cache generation changes, only Cache Storage and the old service-worker registration are cleared before a one-time reload. Authentication and user preferences are preserved.

**AR:** تنشئ نسخة الإنتاج Service Worker وملف تثبيت خاصاً بـ Bemo ERP. يستدعي بدء التطبيق والفحص كل 30 ثانية `/api/v1/system/status`، وعند تغير إصدار الكاش المحفوظ في الخادم يتم مسح Cache Storage وتسجيل Service Worker القديم فقط ثم إعادة التحميل مرة واحدة، دون حذف جلسة المستخدم أو تفضيلاته.
