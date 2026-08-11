# Workforce / العمالة والمقاولون

**EN:** Lazy workforce screens cover contractors, workers, categories, requests, half-month attendance, settlements, advances, accounts, and imports. Manual attendance filters by contractor/category/worker/status, supports selected workers and dates, previews the affected worker/day/cell counts, and applies attendance or overtime only after confirmation.

**AR:** شاشات عمالة مؤجلة التحميل للمقاولين والعمال والفئات والطلبات وحضور نصف الشهر والتسويات والسلف والحسابات والاستيراد. تدعم شاشة الحضور التصفية بالمقاول والفئة والعامل والحالة، وتحديد العمال والأيام، ومعاينة أعداد العمال والأيام والخلايا، وتطبيق الحضور أو الأوفر تايم بعد التأكيد فقط.

**EN:** The manual-attendance matrix remains visible independently of the calculation-rules banner. The workforce dashboard includes contractor/category distribution charts, request coverage, and advance exposure and honors the user's dashboard motion preference. Contractor, worker, and category pages download server-generated Excel workbooks. The advances screen manages global, category, and worker deduction policies.

**AR:** يبقى جدول الحضور اليدوي ظاهراً بصورة مستقلة عن بطاقة قواعد الاحتساب. وتحتوي لوحة القوى العاملة على رسوم توزيع حسب المقاول والفئة، وتغطية الطلبات، ومخاطر السلف، مع احترام خيار حركة لوحة المستخدم. وتنزّل صفحات المقاولين والعمال والفئات ملفات Excel ينشئها الخادم. كما تدير شاشة السلف السياسة العامة واستثناءات الفئة والعامل.

**EN:** Settlement screens expose calculation progress, version, actor, execution time, retained last-valid totals, stale-input warnings, affected workers, and guarded Review/Approve/Lock actions. The reports/import screen implements upload, mapping, validation, preview, commit, result, error download, and audited reversal; unimplemented report actions are not displayed.

**AR:** تعرض شاشات التسوية تقدم الاحتساب وإصداره ومنفذه ووقت التنفيذ وآخر إجماليات صحيحة وتحذير تغير المدخلات والعمال المتأثرين، مع إجراءات مراجعة واعتماد وقفل منضبطة. وتنفذ شاشة التقارير والاستيراد الرفع والمطابقة والتحقق والمعاينة والتنفيذ والنتيجة وتنزيل الأخطاء والتراجع المسجل، ولا تعرض إجراءات تقارير غير منفذة.

**EN:** Manual attendance loads saved entries, marks dirty/error cells, shows the server batch summary, and guards navigation and reload. The workforce dashboard shares contractor/category/location/status filters through URL parameters and supports chart/KPI drill-down. Advance screens show effective policy dates and versions before creation.

**AR:** يحمل الحضور اليدوي السجلات المحفوظة ويميز الخلايا المعدلة والخاطئة ويعرض ملخص الخادم ويحمي التنقل وإعادة التحميل. وتتشارك لوحة القوى العاملة فلاتر المقاول والفئة والموقع والحالة داخل الرابط وتدعم الانتقال من الرسم والبطاقة للتفاصيل، وتعرض شاشة السلف إصدار السياسة الفعلي قبل الصرف.

**EN:** The advances screen also supports active employees, employee-category and employee policy overrides, employee-specific labels in tables/exports, and direct entry from the employee directory.

**AR:** تدعم شاشة السلف أيضاً الموظفين النشطين واستثناءات سياسة فئة الموظفين والموظف، وتعرض أسماءهم ونوعهم بوضوح في الجدول والتصدير، ويمكن فتحها مباشرة من دليل الموظفين.

## 2026-08-10 — Dark-theme semantic palette migration

**EN:** Workforce page/component styles were migrated from fixed light palette values to BEMO semantic tokens. White/light cards, dark-only headings, neutral borders, muted text, success/warning/danger surfaces now resolve through `--surface`, `--surface-muted`, `--ink`, `--muted`, `--secondary-text`, `--line`, and semantic status tokens. This keeps the light appearance while making dark mode readable and consistent.

**AR:** تم تحويل ألوان صفحات ووحدات Workforce من ألوان Light ثابتة إلى متغيرات BEMO الدلالية. وبذلك أصبحت البطاقات والعناوين والجداول والنصوص الثانوية وحالات النجاح/التحذير/الخطأ تتغير بشكل صحيح مع Dark وLight بدلاً من ظهور خلفيات بيضاء أو نصوص داكنة غير مقروءة.

## 2026-08-10 — Settlement workflow contrast

**EN:** The settlement lifecycle strip now uses semantic BEMO theme surfaces and readable step chips instead of a hard-coded light bar.

**AR:** شريط دورة التسوية يستخدم الآن ألوان BEMO الدلالية ويظل مقروءاً في الوضع الداكن والفاتح.
