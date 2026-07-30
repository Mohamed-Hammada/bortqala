# Workforce & Contractors Module (إدارة العمالة والمقاولين)

This package implements the core business domain, data persistence, settlement calculations, labor requests, advance ledgers, manual attendance matrices, and REST APIs for contractors and daily/hourly workforce management.

## Key Services
- `ContractorService`: Manages contractor profiles, historical rates, and 4 calculation models (`worker_net_total`, `contractor_daily_rate`, `worker_cost_plus_fee`, `fixed_period_amount`).
- `WorkerService`: Manages daily worker entities, category defaults, rate versions, and contractor assignment history.
- `LaborRequestService`: Handles labor headcount requests, contractor fulfillments, and variance tracking.
- `WorkforceAttendanceService`: Saves matrix attendance entries (1, 0.5, 0, hours) and calculates daily effective earnings.
- `WorkforceSettlementService`: Calculates and locks 15-day / monthly settlements with audit snapshotting.
- `WorkforceAdvanceService`: Short/long term workforce advances with installment scheduling and maximum deduction thresholds.
- `WorkforceExcelImportService`: Multi-sheet Excel parsing and discrepancy diagnostics (e.g. 1635 vs 1550 day discrepancy detection).

## July 2026 completion / استكمال يوليو 2026

**EN:** Contractor, worker, and worker-category registers expose real `.xlsx` downloads with RTL Arabic sheets, typed numeric cells, filters, frozen headings, and Excel tables. V58 adds tenant-scoped advance deduction policies with global defaults and category/worker overrides; advance creation resolves worker, then category, then global policy while allowing an explicit advance-level override.

**AR:** توفر سجلات المقاولين والعمال وتصنيفات العمال تنزيلات `.xlsx` حقيقية بأوراق عربية RTL وخلايا رقمية وفلاتر وعناوين ثابتة وجداول Excel. ويضيف V58 سياسات خصم سلف لكل شركة بإعداد عام واستثناءات للفئة أو العامل؛ ويطبّق إنشاء السلفة أولوية العامل ثم الفئة ثم الإعداد العام مع السماح بتجاوز خاص بالسلفة.

**EN:** Settlement periods now follow Draft, Calculated, Reviewed, Approved, and Locked states. Each successful calculation records its version, actor, timestamp, totals, warnings, errors, and input fingerprint; a failed recalculation keeps the last valid result. Attendance, rate, advance, or policy changes mark the period stale before approval.

**AR:** تتبع فترات التسوية الآن حالات مسودة، محسوبة، مراجعة، معتمدة، ومقفلة. ويسجل كل احتساب ناجح الإصدار والمنفذ والوقت والإجماليات والتحذيرات والأخطاء وبصمة المدخلات؛ بينما يحافظ فشل إعادة الاحتساب على آخر نتيجة صحيحة. ويجعل تغيير الحضور أو الأسعار أو السلف أو السياسات الفترة بحاجة لإعادة احتساب قبل الاعتماد.

**EN:** Workforce imports persist the original workbook, checksum, mapping, row-level validation, and immutable before/after evidence. The workflow supports preview, duplicate detection, Arabic RTL error workbooks, idempotent commit, valid-row policy, and audited reversal without deleting import history.

**AR:** يحفظ استيراد العمال الملف الأصلي وبصمته ومطابقة الأعمدة والتحقق لكل صف ودليل ما قبل/بعد غير القابل للمحو. ويدعم المعاينة واكتشاف التكرار وملف أخطاء عربي RTL والتنفيذ غير المكرر وسياسة الصفوف الصحيحة والتراجع المسجل دون حذف تاريخ الاستيراد.

**EN:** Manual attendance now reloads persisted cells and returns transactional batch summaries with created, updated, unchanged, and row-level validation failures. Advance policies have effective dates and immutable versions; each advance and settlement stores the policy version snapshot used.

**AR:** يعيد الحضور اليدوي الآن تحميل الخلايا المحفوظة ويعيد ملخص حفظ جماعي داخل معاملة واحدة يشمل الجديد والمحدث وغير المتغير وأخطاء كل خلية. كما أصبحت سياسات السلف مؤرخة وذات إصدارات غير قابلة للمحو، وتحفظ كل سلفة وتسوية لقطة إصدار السياسة المستخدمة.

**EN:** Employee recipients now use the same advance, installment, policy, repayment, and audit workflow as workers and contractors. Employee and employee-category policy overrides are supported, eligible-category rules are enforced by the backend, and payroll deductions consume only due automatic installments within the configured percentage cap.

**AR:** أصبح الموظف مستفيداً مدعوماً في دورة السلف والأقساط والسياسات والسداد والتدقيق نفسها الخاصة بالعمال والمقاولين. تدعم السياسات استثناء الموظف وفئة الموظفين، ويتحقق الخادم من سماح الفئة بالسلف، ولا تخصم الرواتب إلا الأقساط الآلية المستحقة في حدود النسبة المضبوطة.
