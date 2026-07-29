# Procurement tests / اختبارات المشتريات

**EN:** Verifies supplier invoices remain partially paid until posted payments reach the net payable amount.

**AR:** يتحقق من بقاء فاتورة المورد مدفوعة جزئياً حتى تصل المدفوعات المرحلة إلى صافي المبلغ المستحق.
## Goods receipt persistence

`GoodsReceiptPersistenceTests` verifies that a receipt and its item lines are inserted in one transaction with a valid parent foreign key. This protects the GRN workflow from the historical conflict caused by inserting child rows before linking them to the receipt.

يتحقق اختبار `GoodsReceiptPersistenceTests` من حفظ إذن الاستلام وبنوده داخل معاملة واحدة مع ربط صحيح بالمستند الأب، لمنع تعارض قاعدة البيانات الذي كان يعطل إنشاء الإذن.
