# Trade — Commercial Umbrella (التجارة)

**EN:** Umbrella package for commercial trade capabilities. Children own their domains and carry their own bilingual READMEs:

- `trade/procurement/` — purchase orders, goods receipts, supplier invoices & payments, three-way matching, supplier returns, purchase-request linkage.
- `trade/sales/` — sales quotations, invoicing, customer credit profiles & receivables.
- `trade/parties/` — trading party extensions beyond HR business parties (commercial roles, relationships).
- `trade/pos/` — retail POS engine and cashier shift reconciliation (V311–V313).

**AR:** حزمة جامعة للقدرات التجارية، وتحمل الوحدات الفرعية نطاقاتها وملفات تعريف ثنائية اللغة خاصة بها: المشتريات (أوامر الشراء والاستلام وفواتير ومدفوعات الموردين والمطابقة الثلاثية والمرتجعات)، والمبيعات (عروض الأسعار والفواتير وملفات ائتمان العملاء والتحصيل)، والأطراف التجارية، ونقاط البيع وتسوية ورديات الكاشير.

- Cross-child rules: money math stays backend-owned; every financial mutation posts partner-ledger/journal evidence; document numbers come from `shared/numbering`.
- New sub-capabilities (e.g., purchase requests, van sales) should be added as children here, not as top-level packages.
