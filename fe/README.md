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

On an expired protected session, the interceptor clears browser authentication and redirects to `/login?reason=session-expired`; the login page shows the database-translated expiry notice. Administrators can also set the application session lifetime from the settings page. The new value affects subsequently issued tokens.

Read [skills/hr-frontend/SKILL.md](skills/hr-frontend/SKILL.md) before making changes.

## العربية

واجهة Angular 22 مستقلة لإدارة الحضور. تستخدم TypeScript الصارم وsignals ونماذج typed ومسارات lazy وSCSS، والاتجاه الافتراضي عربي RTL، بدون NgModules أو مكتبة واجهات ثقيلة.

أوامر التثبيت والتشغيل والاختبار والبناء موجودة أعلاه. أثناء التطوير تُحوّل الواجهة طلبات `/api` إلى الخادم على المنفذ 8080. ترسل JWT ورقم تتبع جديدًا لكل طلب ومعرف جهاز ثابتًا لهذا المتصفح، لكن الخادم يظل صاحب قرار الصلاحيات النهائي.

الدخول يتطلب كود التطبيق واسم المستخدم وكلمة المرور. إعدادات الركن العلوي تحفظ المظهر والكثافة واللغة لكل مستخدم، وتُحمّل الترجمة العربية/الإنجليزية من API قاعدة البيانات. كل تاريخ API رقم epoch milliseconds عبر `core/date.ts`.

اقرأ ملف مهارة الواجهة قبل أي تعديل وحدّث README الخاص بكل feature أو core package تغيره.
