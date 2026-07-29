# Procurement / المشتريات

**EN:** A centered-dialog, tabbed workflow for purchase orders, receipts, invoices, and payments. Invoice rows show outstanding balances and keep partially paid invoices available for subsequent payments.

**AR:** سير عمل مبوب بنوافذ وسط الصفحة لأوامر الشراء والاستلام والفواتير والمدفوعات. تعرض الفواتير الرصيد المتبقي وتبقي الفاتورة المدفوعة جزئياً متاحة لدفعات لاحقة.

**EN:** The export action downloads the backend-generated `.xlsx` workbook instead of a CSV file labeled as Excel.

**AR:** ينزّل زر التصدير ملف `.xlsx` الذي ينشئه الخادم بدلاً من ملف CSV يحمل تسمية Excel.

**EN:** Invoice entry supports both supplier invoice numbers and the controlled managed-supplier fallback of internal reference plus missing-invoice reason, and can link the receipt as well as the purchase order.

**AR:** يدعم إدخال الفاتورة رقم فاتورة المورد أو بديل المورد المُدار المنضبط بمرجع داخلي وسبب عدم وجود الفاتورة، ويمكن ربط إذن الاستلام إلى جانب أمر الشراء.

**EN:** Order lines select live inventory items. Receipt entry separates delivered, rejected, quality-deducted, and accepted quantities and captures warehouse location, lot, and quality reason; partial orders remain selectable for later receipts.

**AR:** تختار بنود الأمر أصناف المخزون الفعلية. ويفصل إدخال الاستلام بين الكمية المستلمة والمرفوضة والمخصومة نوعياً والمقبولة، مع الموقع والتشغيلة وسبب الجودة، وتظل الأوامر المستلمة جزئياً متاحة لاستلام لاحق.

**EN:** Procurement forms support native Tab order, Enter submission and Escape cancellation. Order dates accept past, current or future dates. Suppliers are selected from active supplier master data, units are fixed from the selected inventory item, draft lines remain editable, and receipt entry immediately loads the selected order lines. Number fields follow the tenant's automatic/manual numbering setting.

**AR:** تدعم نماذج المشتريات ترتيب Tab الطبيعي والإرسال بزر Enter والإلغاء بزر Escape. يقبل تاريخ الأمر تاريخاً سابقاً أو حالياً أو مستقبلياً. ويُختار المورد من الموردين النشطين، وتثبت الوحدة من الصنف المحدد، وتظل بنود المسودة قابلة للتعديل، ويحمّل إذن الاستلام بنود الأمر المختار فوراً. وتتبع حقول الأرقام إعداد الشركة للترقيم التلقائي أو اليدوي.

**EN:** Line and document totals are evaluated from the current editable values, so changing quantity or price immediately enables saving with the correct amount. Purchase orders and invoices expose transaction currency, invoice availability is explicit, and the no-invoice path never manufactures a supplier invoice number. Financial help tooltips explain original, discount, net, and installment values.

**AR:** تُحسب إجماليات البنود والمستند من القيم القابلة للتعديل الحالية، لذلك يؤدي تغيير الكمية أو السعر إلى تحديث المبلغ وتمكين الحفظ فوراً. وتعرض أوامر الشراء والفواتير عملة العملية، ويُحدد وجود فاتورة المورد صراحةً، ولا ينشئ مسار بدون فاتورة رقماً وهمياً. وتشرح التلميحات المالية المبلغ الأصلي والخصم والصافي والأقساط.

**EN:** The payment dialog limits invoice choices to open invoices for the selected supplier and clears an incompatible invoice when the supplier changes. Each option shows invoice number, date, currency, total, and outstanding balance, and the amount cannot exceed that balance.

**AR:** تقصر نافذة الدفع قائمة الفواتير على الفواتير المفتوحة للمورد المختار، وتلغي اختيار الفاتورة غير المتوافقة عند تغيير المورد. ويعرض كل خيار رقم الفاتورة وتاريخها وعملتها وإجماليها ورصيدها المتبقي، ولا تسمح النافذة بتجاوز هذا الرصيد.
# Currency snapshot UX / تجربة لقطة العملة

**EN:** PO and invoice dialogs show the exchange rate and totals in transaction and base currencies, require a reason for manual overrides, and display the frozen values returned by the server.

**AR:** تعرض نوافذ أمر الشراء والفاتورة سعر الصرف والإجماليات بعملة العملية والعملة الأساسية، وتطلب سبباً للتعديل اليدوي وتحفظ القيم المجمدة التي يعيدها الخادم.

**EN:** On an existing draft purchase order, the exchange snapshot fields are visibly locked while supplier, terms, item, quantity, and unit price remain editable.

**AR:** عند تعديل أمر شراء محفوظ تظهر حقول لقطة سعر الصرف مقفلة بوضوح، بينما تظل حقول المورد والشروط والصنف والكمية وسعر الوحدة قابلة للتعديل.
