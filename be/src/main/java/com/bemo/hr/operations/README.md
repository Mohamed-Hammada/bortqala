# Operations / العمليات

## English

This package owns inventory items, signed stock movements, signed partner ledger entries, and employee advances. A transaction may affect stock, a partner ledger, or both atomically. Positive and negative values are recorded as immutable movements; balances are derived, never overwritten.

Employee advances are accepted only when the employee's category explicitly allows them.

`GET /api/v1/operations/export.xlsx` exports localized, typed and formatted sheets for stock balances, movements, party balances and advances using the authenticated user's Excel preferences.

## العربية

هذه الحزمة مسؤولة عن أصناف المخزن، وحركات الكمية الموجبة والسالبة، ودفاتر الموردين والعملاء، وسُلف الموظفين. يمكن للعملية الواحدة أن تؤثر على المخزون والحساب المالي معًا داخل معاملة واحدة. الأرصدة ناتجة من جمع الحركات ولا يتم تعديلها مباشرة.

لا تُقبل سلفة لموظف إلا إذا كانت فئته تسمح بالسُلف صراحةً.

يصدر المسار `GET /api/v1/operations/export.xlsx` جداول Excel منسقة ومترجمة للأرصدة والحركات وحسابات الأطراف والسُلف حسب تفضيلات المستخدم.
