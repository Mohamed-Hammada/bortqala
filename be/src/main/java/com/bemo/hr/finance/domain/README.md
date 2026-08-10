# Finance domain / نطاق المالية

**EN:** `Currency.exchangeRate` is the configured business/accounting rate. Frankfurter fields (`referenceExchangeRate`, provider/base/date/fetched/supported metadata) are informational only. `ExchangeRateHintSetting` is tenant-owned and defaults to enabled with a four-hour refresh interval.

**AR:** الحقل `Currency.exchangeRate` هو سعر الصرف المعتمد للأعمال والمحاسبة. حقول Frankfurter المرجعية مخصصة للاستعلام والعرض فقط. إعداد `ExchangeRateHintSetting` تابع للمستأجر ومفعل افتراضياً بفترة تحديث أربع ساعات.

**EN:** Bank reconciliation uses three tenant-owned aggregates: immutable imported statement evidence, versioned lines with matched totals, and reversible match evidence linked to posted journals. Statement state progresses from `IMPORTED` to `IN_PROGRESS` and becomes `RECONCILED` only when no open or partial line remains.

**AR:** تستخدم التسوية البنكية ثلاث تجميعات تابعة للمستأجر: دليل الكشف المستورد، وحركات ذات إصدار وإجمالي مطابق، وأدلة مطابقة قابلة للعكس ومرتبطة بقيود مرحلة. تنتقل حالة الكشف من `IMPORTED` إلى `IN_PROGRESS` ولا تصبح `RECONCILED` إلا عند عدم وجود حركة مفتوحة أو جزئية.
