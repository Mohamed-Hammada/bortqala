# Inventory Analytics & Warehouse Master (تحليلات المخازن والباركود)

**EN:** Warehouse intelligence over the operations stock ledger (roadmap item 9, V286–V287). Provides aging buckets (slow-moving / dead-stock flags on `InventoryItem.isDeadStock`), reorder-point monitoring with shortage computation (`OperationsService` alerts where balance ≤ reorderPoint, severity split at ≤50%), low/negative-stock dashboard counters, barcode master data with aliases (`barcodeAliases`) and `GET /inventory/barcode-lookup` exact-or-alias resolution (`InventoryAnalyticsService.lookupBarcode`), and continuous FIFO/weighted-average valuation evidence consumed by reports.

**AR:** ذكاء المستودعات فوق حركات المخزون (البند 9، ترحيلات V286–V287): أعمار الأصناف وأعلام الراكد، مراقبة حدود إعادة الطلب وحساب العجز بدرجتي شدة، عدادات نقص وسالب المخزون للوحة القيادة، بيانات باركود رئيسية مع أسماء بديلة ونقطة بحث بالباركود أو البديل، وأدلة تقييم مستمر (فيفو/متوسط مرجح) تستهلكها التقارير.

- Key files: `application/InventoryAnalyticsService.java` (alerts, lookup, valuation views), `api/InventoryAnalyticsController.java` (`@PreAuthorize` explicit), `domain/InventoryItem` reorder/tracking config.
- Frontend consumers: operations workbench valuation/replenishment sections; camera scanning arrives with the Capacitor wrapper (WP-14).
- Rule: balances/variance/valuation math stays backend-owned; screens format only.
