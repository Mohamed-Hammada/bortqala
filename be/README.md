# Backend / الخادم

## English

Spring Boot 4.1 modular monolith built with Java 26 and Gradle. PostgreSQL is the production database; Liquibase is the only schema authority. H2 is used only by the `dev` profile and tests.

The backend is tenant-aware. Authentication resolves an active application by `appCode`; the JWT contains `appId` and `appCode`, and Hibernate applies the tenant discriminator to application-owned entities. API `Instant` and `LocalDate` values are JSON epoch-millisecond numbers; `LocalTime` is `HH:mm`.

Each application stores an administrator-controlled session timeout between 5 and 10,080 minutes. It is used when issuing new JWTs, so changing it does not rewrite the expiry of an existing signed token. The admin-only settings endpoints are `GET` and `PUT /api/v1/admin/app-settings`.

Mutable aggregates use `created_at` and `updated_at`; immutable attendance evidence uses semantic timestamps. User preferences and Arabic/English translation bundles are persisted. Locales are canonicalized to `ar-EG` or `en-US`. The schema and migrations have been validated against local PostgreSQL 18.4.

The optional development bootstrap creates editable DEMO reference categories from the supplied paper notes. It never overwrites an existing category code. Supported pay cycles are calendar-month, 15-day, and 30-day.

The same idempotent demo mode covers two-punch, single-punch, late and full-category no-punch days; suppliers, processing/export customers and sorting traders; raw/packaging/sorted inventory; signed stock and financial movements; and an eligible employee advance. Categories decide whether advances are allowed. Employee codes are category-prefixed and can be automatically sequenced.

The operations endpoints under `/api/v1/operations` expose items, immutable signed transactions, employee advances, derived balances and a localized multi-sheet Excel export. Positive/negative quantity and amount semantics are recorded explicitly rather than mutating balances.

Run and test:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
.\gradlew.bat clean test
```

Production configuration:

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `HR_JWT_SECRET` | HS256 secret of at least 32 bytes; mandatory outside development |
| `HR_COMPANY_ZONE` | Company time zone; defaults to `Africa/Cairo` |
| `HR_CORS_ALLOWED_ORIGINS` | Comma-separated trusted frontend origins |
| `HR_BOOTSTRAP_APP_CODE`, `HR_BOOTSTRAP_APP_NAME` | Optional bootstrap SaaS application |
| `HR_BOOTSTRAP_ADMIN_USERNAME`, `HR_BOOTSTRAP_ADMIN_PASSWORD` | Optional one-time admin inside the bootstrap application |

Every request returns `X-Correlation-Id` and `X-Server-Correlation-Id`. Console output is Logstash JSON. Do not log tokens, passwords, bodies, or query strings.

Local PostgreSQL setup can use `scripts/CreatePostgresDatabase.java` to idempotently create `hr_platform` as UTF-8. For this workspace the verified connection is `jdbc:postgresql://localhost:5432/hr_platform` with the user-provided local credentials.

Read [skills/hr-backend/SKILL.md](skills/hr-backend/SKILL.md) before making changes.

## العربية

الخادم Modular Monolith باستخدام Spring Boot 4.1 وJava 26 وGradle. قاعدة الإنتاج PostgreSQL، وLiquibase هو المصدر الوحيد لتغييرات الجداول، بينما H2 للتطوير والاختبارات فقط.

كل مستخدم وبيانات تشغيلية مرتبطة بتطبيق SaaS. الدخول يبدأ بكود التطبيق، وتحمل JWT معرف التطبيق وكوده، ويطبّق Hibernate عزل المستأجر. قيم `Instant` و`LocalDate` في JSON أرقام epoch milliseconds، بينما `LocalTime` بصيغة `HH:mm`.

الجداول القابلة للتعديل تحمل `created_at` و`updated_at`، بينما دليل البصمة الثابت يستخدم توقيتًا دلاليًا للاستيراد أو البصمة. إعدادات المستخدم والترجمات العربية/الإنجليزية محفوظة في قاعدة البيانات، وتم التحقق على PostgreSQL 18.4.

بيانات DEMO تغطي حالات البصمة العادية والواحدة والتأخير والغياب الكامل، وجهات التعامل والمخزون والحركات المالية والسُلف. تحدد الفئة السماح بالسلفة، ويبدأ كود الموظف بكود الفئة مع sequence تلقائي عند تركه فارغًا. توفر `/api/v1/operations` الحركات والأرصدة وتصدير Excel مترجم متعدد الأوراق.

أوامر التشغيل والاختبار موجودة أعلاه. في الإنتاج يجب توفير سر JWT قوي وبيانات PostgreSQL وتحديد origins الموثوقة. كل طلب يرجع رقم تتبع العميل ورقمًا مستقلًا من الخادم، والسجلات بصيغة JSON ولا تحتوي token أو كلمة مرور أو body أو query.

اقرأ ملف مهارة الخادم قبل أي تعديل وحدّث README الخاص بكل package تغيره.
