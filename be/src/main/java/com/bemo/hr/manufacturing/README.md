# Manufacturing — Production & Quality (التصنيع والجودة)

**EN:** Manufacturing execution (roadmap items 14–15, V296): routings with work centers, production BOMs converting raw materials to finished goods (e.g., oranges + carton 8-key → packed box) with per-unit cost roll-up, all-or-nothing production orders issuing materials and receiving output, WIP costing with variance closing, and QC inspections (`manufacturing/quality/`) with non-conformance handling. Partial issue/receipt is intentionally out of scope (documented product decision).

**AR:** تنفيذ التصنيع (البندان 14–15، ترحيل V296): مسارات الإنتاج ومراكز العمل، وقوائم المواد لإنتاج التام من الخام (برتقال + كرتونة 8 مقاس → كرتونة مغلفة) باحتساب تكلفة الوحدة، وأوامر إنتاج بصرف واستلام كليين مع تسوية انحرافات التشغيل تحت التشغيل، وفحوصات الجودة في `manufacturing/quality/` ومعالجة عدم المطابقة. الصرف/الاستلام الجزئي مستثنى عمداً بقرار منتج موثق.

- Children: `production/` (routings, work centers, BOM, orders, WIP) · `quality/` (inspections, NCR).
- Cost rule: BOM roll-up, variance and valuation math are backend-owned; screens display evidence.
- Integration: material issue consumes operations stock; finished goods receipt lands in warehouse balances and feeds inventory analytics.
