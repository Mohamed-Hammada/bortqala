# Frontend / الواجهة

## English

Angular 22 standalone application for the HR attendance workflow. It uses strict TypeScript, signals, typed reactive forms, lazy routes, SCSS, Arabic RTL defaults, and a feature-first structure without NgModules or a UI framework.

```powershell
npm install
npm start
npm test -- --watch=false
npm run build
```

The development server proxies `/api` and `/actuator` to `http://localhost:8080`. Authentication stores the access session in browser storage, attaches the JWT, generates a fresh `X-Correlation-Id` for each request, and retains a stable random `X-Device-Id` for that browser installation. Backend authorization remains authoritative.

Login requires app code, username, and password. The top-corner settings page persists light/dark/system theme, table density, and Arabic/English locale per user. UI bundles are loaded from the database-backed i18n API; API dates are epoch-millisecond numbers handled through `core/date.ts`.

The translation bundle is loaded by Angular's application initializer before the first route renders, preventing raw database keys from flashing on screen. `npm run check:i18n` rejects literal UI keys missing from either locale. Table pages use a default of 5 rows with 5/10/50/100 choices, and dialogs/drawers close with Escape.

The operations feature manages inventory, signed stock/money transactions, party balances and eligible employee advances. Export actions use the backend's locale-aware formatted workbooks. In the desktop build, Tauri supplies the generated first-login credentials through its restricted localhost bridge; the normal browser development hint remains `DEMO / admin / Admin@12345`.

On an expired protected session, the interceptor clears browser authentication and redirects to `/login?reason=session-expired`; the login page shows the database-translated expiry notice. Administrators can also set the application session lifetime from the settings page. The new value affects subsequently issued tokens.

Read [skills/hr-frontend/SKILL.md](skills/hr-frontend/SKILL.md) before making changes.

## العربية

واجهة Angular 22 مستقلة لإدارة الحضور. تستخدم TypeScript الصارم وsignals ونماذج typed ومسارات lazy وSCSS، والاتجاه الافتراضي عربي RTL، بدون NgModules أو مكتبة واجهات ثقيلة.

أوامر التثبيت والتشغيل والاختبار والبناء موجودة أعلاه. أثناء التطوير تُحوّل الواجهة طلبات `/api` إلى الخادم على المنفذ 8080. ترسل JWT ورقم تتبع جديدًا لكل طلب ومعرف جهاز ثابتًا لهذا المتصفح، لكن الخادم يظل صاحب قرار الصلاحيات النهائي.

الدخول يتطلب كود التطبيق واسم المستخدم وكلمة المرور. إعدادات الركن العلوي تحفظ المظهر والكثافة واللغة لكل مستخدم، وتُحمّل الترجمة العربية/الإنجليزية من API قاعدة البيانات. كل تاريخ API رقم epoch milliseconds عبر `core/date.ts`.

يكتمل تحميل قاموس الترجمة قبل رسم أول مسار حتى لا تظهر مفاتيح مثل `parties.eyebrow`. يمنع فحص `npm run check:i18n` أي مفتاح غير موجود في إحدى اللغتين. تبدأ الجداول بخمسة صفوف مع خيارات 5/10/50/100 وتغلق الحوارات بمفتاح Escape.

تدير شاشة العمليات المخزون والحركات الكمية والمالية وحسابات الأطراف والسُلف المسموحة. في نسخة سطح المكتب تمرر Tauri بيانات أول دخول العشوائية إلى شاشة الدخول عبر جسر localhost المقيّد.

اقرأ ملف مهارة الواجهة قبل أي تعديل وحدّث README الخاص بكل feature أو core package تغيره.
