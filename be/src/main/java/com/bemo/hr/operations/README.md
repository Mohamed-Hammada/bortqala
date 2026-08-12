# Operations / العمليات

## English

This package owns inventory items, signed stock movements, signed partner ledger entries, and employee advances. A transaction may affect stock, a partner ledger, or both atomically. Positive and negative values are recorded as immutable movements; balances are derived, never overwritten.

Employee advances are accepted only when the employee's category explicitly allows them.

`GET /api/v1/operations/export.xlsx` exports localized, typed and formatted sheets for stock balances, movements, party balances and advances using the authenticated user's Excel preferences.

Inventory adjustments are ADMIN-only, require an explicit approval acknowledgement and reason, remain append-only, and cannot create a negative balance. Existing negative balances are corrected with positive adjustment documents rather than overwritten.

Procurement resolves item names, categories, and fixed units of measure from the inventory master through this service; client-supplied descriptive fields are never trusted as master data.

Every stock movement may carry separate business document references: purchase-order number, goods-receipt number, delivery-note number, supplier-invoice number, adjustment voucher number, an external reference, the warehouse, and attachment metadata (name, content type, size). The required references depend on the operation type: supplier receipts require the receipt and purchase-order numbers, sale/delivery movements require the delivery-note number, and adjustments require a voucher number. An invoice number is only accepted with a business party and is rejected when the same party already recorded it. Movement quantities stay positive; the operation type decides whether they increase or decrease the balance.

Demo employees, punches, parties, inventory movements, ledger entries, and advances are opt-in development fixtures. Production startup never creates them.

Inventory valuation is recorded separately from immutable quantity evidence. Each tenant selects FIFO or weighted-average costing, may enable controlled backdated posting, and maps inventory, receipt-offset, COGS, and variance accounts. Every new quantity movement receives an immutable cost explanation; FIFO layers and item locks prevent concurrent double consumption. When GL posting is enabled, the service validates the fiscal period and creates a balanced posted journal. Revaluation is idempotent by `operationId`, audited, and posts only the value difference. `GET /api/v1/operations/valuation/report` exposes item totals and movement drill-down, and the operations workbook includes valuation and movement-cost sheets.

Items also carry non-negative reorder points and suggested order quantities. `GET /api/v1/operations/reorder-alerts` derives the replenishment queue from current signed balances. Cycle counts use an idempotent `operationId`; `POST /api/v1/operations/cycle-counts` locks the item, records immutable system/count evidence, posts only the variance as a valued `CYCLE_COUNT` movement, and writes an audit event. Replays return the original count without posting another movement.

## العربية — تقييم المخزون

يُسجَّل تقييم المخزون منفصلاً عن دليل الكمية غير القابل للتعديل. تختار كل شركة طريقة الوارد أولاً أو المتوسط المرجح، وتربط حسابات المخزون والاستلام وتكلفة المبيعات والفروق. تحصل كل حركة كمية جديدة على أثر تكلفة وشرح ثابت، وتمنع أقفال الصنف وطبقات FIFO استهلاك التكلفة مرتين عند التزامن. عند تفعيل الترحيل المالي يتحقق النظام من الفترة المالية وينشئ قيداً متوازناً ومرحلاً. إعادة التقييم متكررة بأمان بواسطة `operationId` وتسجل في التدقيق ولا ترحل إلا فرق القيمة. يعرض المسار `GET /api/v1/operations/valuation/report` إجماليات الأصناف وتفاصيل الحركات، ويضيف تصدير العمليات ورقتي التقييم وتكاليف الحركات.

## العربية

هذه الحزمة مسؤولة عن أصناف المخزن، وحركات الكمية الموجبة والسالبة، ودفاتر الموردين والعملاء، وسُلف الموظفين. يمكن للعملية الواحدة أن تؤثر على المخزون والحساب المالي معًا داخل معاملة واحدة. الأرصدة ناتجة من جمع الحركات ولا يتم تعديلها مباشرة.

لا تُقبل سلفة لموظف إلا إذا كانت فئته تسمح بالسُلف صراحةً.

يصدر المسار `GET /api/v1/operations/export.xlsx` جداول Excel منسقة ومترجمة للأرصدة والحركات وحسابات الأطراف والسُلف حسب تفضيلات المستخدم.

تسويات المخزون متاحة للمدير فقط وتتطلب إقرار اعتماد وسبباً صريحاً، وتبقى كسجلات إضافية غير قابلة لمحو التاريخ، ولا يمكنها إنشاء رصيد سالب. تُصحح الأرصدة السالبة القديمة بمستندات تسوية موجبة.

تستمد المشتريات اسم الصنف وتصنيفه ووحدة قياسه الثابتة من دليل المخزون عبر هذه الخدمة، ولا تعتمد على البيانات الوصفية المرسلة من الواجهة كبيانات رئيسية.

يمكن أن تحمل كل حركة مخزون مراجع مستندات أعمال منفصلة: رقم أمر الشراء، ورقم إذن الاستلام، ورقم إذن التسليم، ورقم فاتورة المورد، ورقم سند التسوية، ومرجعًا خارجيًا، والمخزن، وبيانات المرفق (الاسم والنوع والحجم). تعتمد الحقول المطلوبة على نوع الحركة: استلام المورد يتطلب رقم الإذن ورقم أمر الشراء، وحركات البيع/التسليم تتطلب رقم إذن التسليم، والتسويات تتطلب رقم سند. لا يُقبل رقم فاتورة إلا مع جهة أعمال، ويُرفض إذا سبق تسجيله للجهة نفسها. تبقى كميات الحركات موجبة، ونوع الحركة هو من يقرر ما إذا كانت تزيد الرصيد أم تنقصه.

الموظفون والبصمات والأطراف وحركات المخزون والقيود والسلف النموذجية بيانات تطوير اختيارية، ولا ينشئها تشغيل الإنتاج.

**EN:** The employee advance ledger remains the financial source used by payroll. Scheduled employee advances and manual repayments from the workforce module mirror their signed entries here in the same backend transaction.

**AR:** يظل دفتر سلف الموظفين المصدر المالي الذي تستخدمه الرواتب. وتعكس سلف الموظفين المجدولة وعمليات سدادها اليدوي قيودها الموقعة هنا داخل معاملة الخادم نفسها.
