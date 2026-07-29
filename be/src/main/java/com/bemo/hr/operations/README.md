# Operations / العمليات

## English

This package owns inventory items, signed stock movements, signed partner ledger entries, and employee advances. A transaction may affect stock, a partner ledger, or both atomically. Positive and negative values are recorded as immutable movements; balances are derived, never overwritten.

Employee advances are accepted only when the employee's category explicitly allows them.

`GET /api/v1/operations/export.xlsx` exports localized, typed and formatted sheets for stock balances, movements, party balances and advances using the authenticated user's Excel preferences.

Inventory adjustments are ADMIN-only, require an explicit approval acknowledgement and reason, remain append-only, and cannot create a negative balance. Existing negative balances are corrected with positive adjustment documents rather than overwritten.

Procurement resolves item names, categories, and fixed units of measure from the inventory master through this service; client-supplied descriptive fields are never trusted as master data.

Demo employees, punches, parties, inventory movements, ledger entries, and advances are opt-in development fixtures. Production startup never creates them.

## العربية

هذه الحزمة مسؤولة عن أصناف المخزن، وحركات الكمية الموجبة والسالبة، ودفاتر الموردين والعملاء، وسُلف الموظفين. يمكن للعملية الواحدة أن تؤثر على المخزون والحساب المالي معًا داخل معاملة واحدة. الأرصدة ناتجة من جمع الحركات ولا يتم تعديلها مباشرة.

لا تُقبل سلفة لموظف إلا إذا كانت فئته تسمح بالسُلف صراحةً.

يصدر المسار `GET /api/v1/operations/export.xlsx` جداول Excel منسقة ومترجمة للأرصدة والحركات وحسابات الأطراف والسُلف حسب تفضيلات المستخدم.

تسويات المخزون متاحة للمدير فقط وتتطلب إقرار اعتماد وسبباً صريحاً، وتبقى كسجلات إضافية غير قابلة لمحو التاريخ، ولا يمكنها إنشاء رصيد سالب. تُصحح الأرصدة السالبة القديمة بمستندات تسوية موجبة.

تستمد المشتريات اسم الصنف وتصنيفه ووحدة قياسه الثابتة من دليل المخزون عبر هذه الخدمة، ولا تعتمد على البيانات الوصفية المرسلة من الواجهة كبيانات رئيسية.

الموظفون والبصمات والأطراف وحركات المخزون والقيود والسلف النموذجية بيانات تطوير اختيارية، ولا ينشئها تشغيل الإنتاج.
