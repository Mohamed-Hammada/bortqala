# Procurement / المشتريات

**EN:** Tenant-scoped purchase orders, line items, goods receipts, supplier invoices, and supplier payments. Purchase-order totals are derived from validated lines. Supplier invoices expose paid and outstanding amounts derived from posted payments; partial and multiple payments are supported and overpayment or supplier mismatch is rejected.

**AR:** أوامر شراء وبنود وإيصالات بضائع وفواتير موردين ومدفوعات معزولة لكل شركة. تُحسب إجماليات أوامر الشراء من البنود المتحقق منها. وتعرض فواتير المورد المدفوع والمتبقي من المدفوعات المرحلة، مع دعم السداد الجزئي والمتعدد ومنع تجاوز الرصيد أو اختلاف المورد.

**EN:** `GET /api/v1/trade/procurement/export.xlsx` returns a real multi-sheet workbook with typed date and numeric cells, Arabic RTL or English headings, generation metadata, and the authenticated user.

**AR:** يعيد `GET /api/v1/trade/procurement/export.xlsx` ملف Excel حقيقياً متعدد الأوراق بخلايا تواريخ وأرقام قابلة للحساب، وعناوين عربية RTL أو إنجليزية، وبيانات وقت الإنشاء والمستخدم.

**EN:** Direct suppliers require an invoice number. Managed suppliers may use an internal document reference only when a reason is supplied. Invoices retain purchase-order, goods-receipt, supplier, and responsible-party IDs so later relationship changes do not rewrite history.

**AR:** يتطلب المورد المباشر رقم فاتورة. ويمكن للمورد المُدار استخدام مرجع مستند داخلي فقط مع تسجيل السبب. تحتفظ الفاتورة بمعرفات أمر الشراء وإذن الاستلام والمورد والمسؤول حتى لا تغيّر تعديلات العلاقة اللاحقة المستندات التاريخية.

**EN:** Purchase-order lines reference controlled inventory items. Goods receipts derive accepted quantity from delivered minus rejected and quality-deducted quantities, block over-receipt, keep partially received orders open, and post only accepted quantity to inventory with GRN traceability.

**AR:** ترتبط بنود أمر الشراء بأصناف المخزون المعتمدة. يحسب إذن الاستلام الكمية المقبولة من المستلم بعد طرح المرفوض والخصم النوعي، ويمنع تجاوز الكمية المتبقية، ويبقي الأمر مفتوحاً عند الاستلام الجزئي، ويرحل الكمية المقبولة فقط إلى المخزون مع مرجع إذن الاستلام.

**EN:** Supplier-payment notes are part of the V51 production schema and are preserved with each posted payment.

**AR:** تحفظ ملاحظات دفعة المورد ضمن مخطط الإنتاج V51 مع كل دفعة مرحلة.

**EN:** V53 adds tenant-level procurement document sequences and unique PO/GRN numbers. Administrators choose automatic locked numeric numbering, which continues after the highest existing company document number, or unique manual numbering. Draft orders can edit date, supplier, item, quantity and price; item name/category/unit always come from inventory master data. Draft, issued and partially received orders remain available when creating a receipt.

**AR:** يضيف V53 تسلسلاً لمستندات المشتريات لكل شركة مع منع تكرار أرقام أوامر الشراء وأذون الاستلام. يختار المدير بين الترقيم التلقائي المقفل أو الترقيم اليدوي غير المكرر. ويمكن تعديل تاريخ المسودة والمورد والصنف والكمية والسعر، بينما يأتي اسم الصنف وتصنيفه ووحدته دائماً من دليل المخزون. وتظل المسودات والأوامر الصادرة والمستلمة جزئياً متاحة عند إنشاء إذن الاستلام.

**EN:** V57 adds validated transaction currency to purchase orders and supplier invoices, defaulting from the supplier while permitting an active configured currency override. Supplier-without-invoice transactions now persist a null supplier invoice number with a separate mandatory internal reference and reason. Receipt quantities retain delivered/original, rejected, quality-deducted, and accepted values.

**AR:** يضيف V57 عملة عملية متحققاً منها لأوامر الشراء وفواتير الموردين، وتبدأ بعملة المورد مع السماح باختيار عملة نشطة أخرى. وتحفظ معاملة المورد بدون فاتورة رقم فاتورة فارغاً فعلياً مع مرجع داخلي وسبب إلزاميين. كما يحتفظ الاستلام بالكميات الأصلية والمرفوضة والمخصومة للجودة والمقبولة.

**EN:** Supplier payments are idempotent by operation ID. The backend rejects cross-supplier invoices, closed invoices, non-positive amounts, and amounts above the live outstanding balance. These checks remain authoritative even when the API is called outside the UI.

**AR:** دفعات الموردين محمية من التكرار بمعرّف العملية. ويرفض الخادم الفاتورة التابعة لمورد آخر، والفاتورة المغلقة، والمبلغ غير الموجب، وأي مبلغ يتجاوز الرصيد المتبقي الفعلي، حتى عند استدعاء الواجهة البرمجية مباشرةً.
# Exchange-rate snapshots / لقطات سعر الصرف

**EN:** Purchase orders and supplier invoices store transaction/base currencies, rate, rate date, source, manual override reason, and base-currency totals. Supplier ledger postings use the frozen base amount.

**AR:** تحفظ أوامر الشراء وفواتير المورد عملة العملية والأساس والسعر وتاريخه ومصدره وسبب التعديل اليدوي والإجمالي بالعملة الأساسية، وتستخدم قيود المورد القيمة الأساسية المجمدة.

**EN:** Once a purchase order is first saved, its document date, transaction currency, exchange rate, source, and override reason are immutable. Draft editing still permits supplier, terms, item, quantity, and price changes, and recomputes the base total with the frozen rate.

**AR:** بعد أول حفظ لأمر الشراء يثبت تاريخ المستند وعملة العملية وسعر الصرف ومصدره وسبب التعديل. يظل تعديل المورد والشروط والصنف والكمية والسعر متاحاً للمسودة، ويعاد حساب الإجمالي الأساسي بالسعر المثبت.
