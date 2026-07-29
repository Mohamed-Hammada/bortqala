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
