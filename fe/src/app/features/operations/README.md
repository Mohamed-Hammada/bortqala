# Operations / العمليات

## English

This feature provides a table-first workspace for inventory balances, signed stock and money movements, partner balances, and employee advances. Quantity and amount may be positive or negative. The backend remains the source of truth and enforces advance eligibility.

All grids use the shared 5/10/50/100 paginator (default 5), drawers close with Escape, and the export action downloads the localized multi-sheet workbook from the backend.

The movement form records separate business document references (purchase order, goods receipt, delivery note, invoice, voucher, external reference, warehouse) plus an optional attachment. The form marks and enforces the references required by the selected operation type before submit, and the movements table resolves them to a single primary document number with the external reference, warehouse, and attachment shown underneath. Client-side attachment validation mirrors the backend limits (max 5 MB; images, PDF and Excel only).

The valuation workspace shows on-hand quantity, weighted/FIFO unit cost, inventory value, opening quantity gaps, and a movement-cost drill-down. Admins configure the valuation method and posting accounts, record idempotent revaluations, and supply receipt unit cost. Calculations, fiscal-period checks, locking, and journals remain backend-owned; the page only formats the returned evidence.

The same workbench exposes backend-derived reorder alerts and immutable cycle-count history. Item creation captures reorder thresholds and suggested replenishment quantities. Recording a physical count sends a one-use operation ID; the backend owns the system quantity, variance, movement, valuation, and audit calculations.

## العربية — تقييم المخزون

تعرض مساحة التقييم الكمية المتاحة وتكلفة الوحدة بطريقة المتوسط المرجح أو FIFO وقيمة المخزون وفروق الكميات الافتتاحية، مع تفاصيل تكلفة كل حركة وشرحها وقيد اليومية المرتبط. يضبط المدير طريقة التقييم وحسابات الترحيل ويسجل إعادة تقييم آمنة من التكرار ويدخل تكلفة الاستلام. تبقى الحسابات والتحقق من الفترة والأقفال والقيود مسؤولية الخادم، وتكتفي الواجهة بعرض الدليل وتنسيقه.

## العربية

توفر هذه الشاشة مساحة عمل تعتمد على الجداول لأرصدة المخزون، والحركات الكمية والمالية الموجبة والسالبة، وأرصدة الأطراف، وسُلف الموظفين. الخادم هو مصدر الحقيقة ويتحقق من أهلية فئة الموظف للسلفة.

كل الجداول تبدأ بخمسة صفوف مع خيارات 5/10/50/100، وتغلق النوافذ الجانبية بمفتاح Escape، ويحمّل زر التصدير ملف Excel متعدد الأوراق باللغة المختارة.

يُسجّل نموذج الحركة مراجع مستندات أعمال منفصلة (أمر الشراء، وإذن الاستلام، وإذن التسليم، والفاتورة، والسند، والمرجع الخارجي، والمخزن) مع مرفق اختياري. يحدد النموذج الحقول المطلوبة حسب نوع الحركة ويلزم بها قبل الحفظ، ويعرض جدول الحركات رقم المستند الأساسي مع المرجع الخارجي والمخزن والمرفق تحته. يطابق التحقق من المرفق في الواجهة حدود الخادم (بحد أقصى 5 ميجابايت؛ الصور وPDF وExcel فقط).
