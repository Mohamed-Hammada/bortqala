# Finance application services / خدمات تطبيق المالية

**EN:** `ExchangeRateHintService` coordinates the non-authoritative Frankfurter refresh. It discovers the one active ERP base currency, filters to currencies supported by the provider, converts Frankfurter's `BASE -> QUOTE` direction into the ERP's displayed `QUOTE -> BASE` direction, and stores the result only in reference fields. `ExchangeRateHintScheduler` binds `TenantContext` before entering the transactional refresh service and checks due tenants on a short scan cadence.

**AR:** تقوم خدمة `ExchangeRateHintService` بتنسيق تحديث أسعار Frankfurter المرجعية بدون تغيير السعر المحاسبي المعتمد. تحدد العملة الأساسية النشطة الوحيدة، وتفلتر العملات بحسب دعم المصدر، ثم تعكس اتجاه السعر من `BASE -> QUOTE` إلى اتجاه العرض في النظام `QUOTE -> BASE`. يقوم `ExchangeRateHintScheduler` بربط `TenantContext` قبل دخول المعاملة ثم يحدث المستأجرين المستحقين فقط.

**EN:** `BankReconciliationService` parses quoted UTF-8 CSV safely, validates opening + movements = closing, hashes files for replay protection, ranks posted bank-GL journals by date/reference, and creates balanced posted journals for bank fees. Active match amounts determine exact/partial state; reversal reopens the line and reverses generated fee journals without altering source business journals.

**AR:** تحلل `BankReconciliationService` ملفات CSV المقتبسة بترميز UTF-8 بأمان، وتتحقق من معادلة الافتتاحي والحركات والختامي، وتحمي من إعادة الاستيراد ببصمة الملف، وترتب القيود المرحلة حسب التاريخ والمرجع، وتنشئ قيود رسوم بنكية متوازنة. تحدد المطابقات النشطة الحالة الكاملة أو الجزئية، ويعيد العكس فتح الحركة ويعكس قيد الرسوم المنشأ دون تغيير القيد التجاري الأصلي.
