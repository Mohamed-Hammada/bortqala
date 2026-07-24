# Bemo HR attendance platform

An executable, configurable HR attendance system for teams moving from Excel. The repository contains two applications only: `be/` for the API and `fe/` for the browser UI.

## English

### What is implemented

- Dynamic job/attendance categories with monthly, 15-day, or 30-day pay cycles, expected minutes, workdays, one-punch policy, and `BIOMETRIC`, `MANUAL`, or `HYBRID` modes.
- Effective-dated summer/winter or custom schedule rules, including start time, grace period, and expected-minute override.
- Employees with optional biometric device identity, so daily/manual workers are not falsely treated as missing punches.
- CSV, TXT, XLS, and XLSX biometric imports with checksums, immutable raw evidence, row errors, and unmatched identities.
- Daily calculations and a review workflow for no punch, single punch, manual attendance, leave, deductions, and proposed/confirmed category holidays.
- Monthly and half-month reports with blocking-exception checks, approval/reopen, frozen snapshots, dashboards, and Excel exports.
- Username/password JWT authentication, multiple roles per user, backend authorization, and role-aware Angular navigation.
- Administrators can configure the SaaS application's session lifetime from 5 minutes to 7 days. The selected timeout is applied to newly issued JWTs; expired protected requests are cleared and redirected to login with a dedicated translated notice.
- SaaS application tenancy: login requires an application code, JWTs carry the application identity, and tenant business data is isolated by `app_id`.
- API dates use Unix epoch milliseconds (`long`/TypeScript `number`) for both `Instant` and `LocalDate`; schedule times remain `HH:mm`.
- Per-user light/dark/system theme, table density, and Arabic/English locale preferences stored in PostgreSQL.
- Database-backed translation bundles, SVG navigation icons, and user-selected report ranges with explicit pay-cycle scope and overlap protection.
- On login, the authenticated user's saved locale is loaded before navigation. DEMO also receives editable reference categories from the paper notes: security, accounting, administration, secretarial, daily, cleaning, operation, export, and sorting workers.
- Structured JSON request logs with independent client/server correlation IDs, browser device ID, authenticated user, IP, roles, status, and duration.

### Run locally

Requirements: Java 26, Node 24.18+, npm 11+, and optionally Docker for PostgreSQL.

For the self-contained development database:

```powershell
cd be
$env:JAVA_HOME='C:\Users\wolfn\scoop\apps\openjdk26\current'
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'

cd ..\fe
npm install
npm start
```

Open `http://localhost:4200`. The development-only credentials are application `DEMO`, user `admin`, password `Admin@12345`. Never use this password or the development JWT secret in production.

For PostgreSQL, run `docker compose up -d` inside `be/`, set the environment described in [be/README.md](be/README.md), and start without the `dev` profile.

### Verify

```powershell
cd be
.\gradlew.bat clean test

cd ..\fe
npm test -- --watch=false
npm run build
```

Start with [docs/business-requirements.md](docs/business-requirements.md), [be/skills/hr-backend/SKILL.md](be/skills/hr-backend/SKILL.md), and [fe/skills/hr-frontend/SKILL.md](fe/skills/hr-frontend/SKILL.md) before extending the system.

## العربية

### ما تم تنفيذه

- فئات عمل وحضور ديناميكية بدورة شهرية أو نصف شهرية، وساعات متوقعة، وأيام عمل، وسياسة البصمة الواحدة، وأنماط بصمة أو يدوي أو مختلط.
- قواعد جداول بتاريخ سريان للصيف والشتاء أو أي موسم آخر، مع وقت البداية والسماح وإمكانية تغيير عدد الدقائق المطلوبة.
- موظفون برقم بصمة اختياري حتى لا يُعتبر العامل اليومي أو اليدوي غائبًا بسبب عدم وجود بصمة.
- استيراد CSV وTXT وXLS وXLSX مع منع التكرار، وحفظ المصدر، وأخطاء الصفوف، والهويات غير المربوطة.
- حساب يومي ومراجعة لحالات عدم البصمة والبصمة الواحدة والحضور اليدوي والإجازة والخصم واقتراح إجازة الفئة كلها.
- تقارير شهرية ونصف شهرية، ومنع الاعتماد عند وجود استثناءات، وإعادة الفتح، ولقطات ثابتة، ولوحة متابعة، وتصدير Excel.
- دخول باسم مستخدم وكلمة مرور وJWT، وأكثر من دور للمستخدم، وصلاحيات مؤكدة في الخادم والواجهة.
- دعم SaaS بعزل بيانات كل شركة من خلال `app_id`، والدخول بكود التطبيق مع تضمين هوية التطبيق داخل JWT.
- كل تاريخ بين الواجهة والخادم يُرسل كرقم Unix epoch milliseconds، بينما وقت الجدول يبقى بصيغة `HH:mm`.
- إعدادات مظهر وكثافة جداول ولغة محفوظة لكل مستخدم، وترجمات عربية/إنجليزية من قاعدة البيانات.
- نطاق تقرير يحدده المستخدم مع اختيار دورة الاستحقاق ومنع التداخل، مع بقاء اختصارات الشهر ونصف الشهر.
- سجلات JSON قابلة للتتبع تحتوي أرقام تتبع مستقلة للواجهة والخادم، ومعرف الجهاز، والمستخدم، وIP، والأدوار، والحالة، والمدة.

### التشغيل المحلي

المطلوب Java 26 وNode 24.18 أو أحدث وnpm 11 أو أحدث، وDocker اختياري لتشغيل PostgreSQL. استخدم أوامر التشغيل الإنجليزية أعلاه، ثم افتح `http://localhost:4200`. بيانات التطوير فقط هي التطبيق `DEMO` والمستخدم `admin` وكلمة المرور `Admin@12345`، ويجب عدم استخدامها في الإنتاج.

قبل استكمال أي تطوير اقرأ مستند متطلبات العمل وملفي مهارة الـbackend والـfrontend المشار إليهما أعلاه؛ فهما نقطة التسليم لأي مطور أو agent جديد.
