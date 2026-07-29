# Liquibase changelog / سجل تغييرات قاعدة البيانات

**EN:** `db.changelog-master.yaml` is the only schema entry point. Never edit a changeset already applied to a shared database; add a new versioned YAML file with explicit tenant predicates for tenant-owned data. V4 canonicalizes user locales, adds the 30-day translation, and seeds editable DEMO reference categories idempotently.

**AR:** ملف `db.changelog-master.yaml` هو نقطة الدخول الوحيدة لبنية قاعدة البيانات. لا تعدّل changeset طُبّق على قاعدة مشتركة؛ أضف ملف YAML جديدًا مرقمًا، واستخدم شرط `app_id` صريحًا لبيانات الشركات. الإصدار V4 يوحّد صيغة لغة المستخدم، ويضيف ترجمة دورة 30 يوم، ويضيف فئات DEMO المرجعية القابلة للتعديل بدون تكرار.

V5 adds the Arabic and English database translation for the session-expired login notification.

يضيف V5 ترجمة إشعار انتهاء الجلسة بالعربية والإنجليزية إلى قاعدة البيانات.

V6 adds the per-application session timeout and its Arabic/English settings translations. V7 adds the translated invalid-credentials message used separately from session expiry.

يضيف V6 مدة جلسة قابلة للتحكم لكل تطبيق وترجمات إعداداتها بالعربية والإنجليزية. ويضيف V7 رسالة بيانات الدخول غير الصحيحة المترجمة، مستقلة عن رسالة انتهاء الجلسة.

**EN:** V47 persists per-user sidebar visibility, favorites, recent history, and the configurable recent-item limit. V48-V50 add user category assignment, supplier invoice traceability, and inventory-linked quality-aware goods receiving. V51 aligns supplier-payment notes with the persisted entity so PostgreSQL schema validation succeeds. V52 completes every literal frontend translation in both Arabic and English. The H2 test changelog now mirrors all production migrations through V52.

**AR:** يحفظ V47 إعدادات إظهار أقسام القائمة والمفضلة والسجل الحديث والحد الأقصى للعناصر لكل مستخدم. كما يتبع سجل H2 الاختباري جميع ترحيلات الإنتاج حتى V47 للتحقق من كل حقول الكيانات الحالية.

**AR:** يستكمل V52 جميع مفاتيح الترجمة الحرفية المستخدمة في الواجهة باللغتين العربية والإنجليزية، ويتبع سجل H2 الاختباري الآن جميع ترحيلات الإنتاج حتى V52.

**EN:** V53 adds configurable tenant procurement numbering, locked sequence storage that continues after existing document numbers, unique PO/GRN constraints, and the new Arabic/English navigation, settings and import-status copy.

**EN:** V54 seeds and reconciles each PostgreSQL tenant's purchase-order and goods-receipt counters from its highest existing document number before automatic numbering is used.

**EN:** V55 adds the Arabic/English database translation catalog for permission-aware quick navigation, shortcut help, menu chords, and tooltip copy.

**EN:** V56 adds tenant and per-user dashboard customization permissions, persisted widget layouts, the independent motion preference, and Arabic/English dashboard copy.

**AR:** يضيف V56 صلاحيات تخصيص لوحة المتابعة على مستوى الشركة والمستخدم، وحفظ ترتيب العناصر، وخيار الحركة المستقل، ونصوص اللوحة بالعربية والإنجليزية.

**AR:** يضيف V53 ترقيم مشتريات قابلاً للضبط لكل شركة، وتخزيناً آمناً للتسلسل، وقيود عدم تكرار أرقام أوامر الشراء وأذون الاستلام، ونصوص التنقل والإعدادات وحالة الاستيراد بالعربية والإنجليزية.

**EN:** V57 adds procurement transaction currency and separates a nullable supplier invoice number from the mandatory internal reference used for no-invoice transactions. V58 adds tenant-owned global/category/worker advance deduction policies and seeds a safe global default for every tenant.

**AR:** يضيف V57 عملة عملية المشتريات ويفصل رقم فاتورة المورد القابل للفراغ عن المرجع الداخلي الإلزامي لمعاملات بدون فاتورة. ويضيف V58 سياسات خصم سلف مملوكة للشركة على مستوى عام أو فئة أو عامل، مع إعداد عام آمن لكل شركة.

**EN:** V59 stores settlement calculation versions, execution summaries, failure details, input fingerprints, and versioned worker issues. V60 adds durable import batches, row validation evidence, original files, duplicate checksums, idempotent operation IDs, and reversible change evidence. V61 makes every supplier payment operation ID mandatory and tenant-unique.

**AR:** يحفظ V59 إصدارات احتساب التسويات وملخص التنفيذ وتفاصيل الفشل وبصمة المدخلات ومشكلات العمال حسب الإصدار. ويضيف V60 دفعات استيراد دائمة وأدلة تحقق الصفوف والملفات الأصلية وبصمات منع التكرار ومعرفات العمليات وأدلة التغيير القابلة للعكس. ويجعل V61 معرف كل عملية دفع للمورد إلزامياً وفريداً داخل الشركة.

**EN:** V62 adds persisted attendance day anomalies and the tenant threshold. V63 freezes procurement exchange-rate snapshots and base totals. V64 versions advance policies by effective dates and stores the applied policy snapshot on advances and settlements.

**AR:** يضيف V62 حالات شذوذ الحضور اليومية المحفوظة ونسبة الشركة. ويثبت V63 لقطات سعر الصرف وإجماليات العملة الأساسية للمشتريات. ويضيف V64 إصدارات مؤرخة لسياسات السلف ويحفظ لقطة السياسة المستخدمة مع السلف والتسويات.

**EN:** V65 adds the complete Arabic/English database translation catalog for the responsive user dialog, including password visibility, menu selection, permission counts, and module group labels.

**EN:** V66 adds tenant-owned live biometric IP/API devices, manual and scheduled synchronization state, and durable sync feedback. V67 removes the known invalid legacy supplier phone and adds audited compensating inventory reversals for the two invalid negative inbound QA movements; the original evidence rows remain untouched.

## Folder convention for V68+

- `releases/20260729_v1_v67.changelog-master.yaml`: ordered baseline release after the approved development-database rebuild.
- `schema/create/`: new tables, sequences, indexes, and constraints.
- `schema/update/`: alterations to existing schema.
- `data/insert/`: production reference-data and translation inserts.
- `data/update/`: audited backfills and corrective updates.
- `data/delete/`: narrowly approved deletes with explicit evidence/rollback policy.
- `releases/`: dependency-ordered release masters that include files across the operation folders.
- `src/test/resources/db/changelog/test-data/`: test fixtures only; never included by production.

The production `db.changelog-master.yaml` includes only ordered release masters. The development database was explicitly approved for recreation when the historical files were physically categorized, so the categorized paths are now canonical.

**AR:** يضيف V65 كتالوج ترجمات قاعدة البيانات الكامل بالعربية والإنجليزية لنافذة المستخدم المتجاوبة، بما يشمل إظهار كلمة المرور واختيار القوائم وعدد الصلاحيات وتسميات مجموعات الوحدات.
