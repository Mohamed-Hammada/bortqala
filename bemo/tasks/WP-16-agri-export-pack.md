# WP-16 — Agri-Export Documentation Pack
**Priority:** 🟢 · **Owner:** Full-stack dev B · **Depends on:** — · **Effort:** ~2 weeks
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §18 row 1

## Business goal
The owner's core business: export packhouse produce. Today trade/inventory cover sales, but export paperwork (COO, packing list, phytosanitary application, ACID numbers) and compliance registers (pesticide/MRL) are manual. CargoX/Nafeza APIs = future; v1 captures numbers manually.

## Backend steps
1. `export_shipments` (V-number coordinator): customer_party FK, contract_ref, container_no, booking_no, acid_no, port_of_loading/discharge, etb_date/eta_date, status PREPARING|BOOKED|SHIPPED|SETTLED, lines → inventory lot references (qty per lot).
2. Doc generators: printable COO + packing list + phytosanitary application sheet from shipment+lots (server-side HTML templates → existing Excel exporter for xlsx v1; PDF later).
3. Compliance register `lot_treatment_logs`: lot FK, chemical, dose, treatment_date, pre_harvest_interval_days → derived earliest_safe_pickup = treatment_date + PHI; endpoint flags violations when pickup < safe date (`MRL_INTERVAL_VIOLATED` warning list).
4. Proceeds tracker: expected_fx_amount vs realized entries (manual v1) + days-outstanding aging on shipment.
5. Gate behind `agri.enabled` entitlement; menus `exportShipments` under trade workspace (full A.4).

## Frontend steps
1. `features/trade/export-shipments`: list + detail tabs (shipment, lots, treatments, docs, proceeds); doc-preview buttons opening print view; violation badges on lots.
2. Keys `trade.export*` family (~20).

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** Shipment with 3 lots generates COO + packing list whose quantities sum exactly to lot quantities (no manual re-entry anywhere). — **MET**: `ExportShipmentDocService.render()` reads quantities directly from the shipment's persisted lines (`ExportShipmentLine.getQuantity()`) — there is no re-entry path, so printed quantities always equal lot quantities. `ExportShipmentDocServiceTests` proves a 3-lot shipment (1000/500/750 KG) renders a COO whose quantity column contains exactly those values and a total row summing to 2250. Generated bilingual xlsx via `ExcelExportSupport` (sheet COO / packing list / phyto).
- [x] **AC-2** Treatment with PHI 14 days on lot picked after 10 days appears in violation list; compliant lot does not. — **MET** (compliance/pesticides endpoints).
- [x] **AC-3** ACID/booking numbers persist and print onto documents; status transitions PREPARING→BOOKED→SHIPPED→SETTLED enforced (invalid jumps rejected). — **MET**: fields persist; `ExportShipmentStatus.canTransitionTo` strict-linear + `ExportShipment.transitionTo` throws invalid jumps (`ExportShipmentStatus.java:18-20`); FE only offers next step. ACID/booking/container/ports/dates all print into the meta block of every generated doc.
- [x] **AC-4** Proceeds aging shows days outstanding per shipment and totals per customer; settled shipments drop out. — **MET**: `ExportShipmentService.getAging()` + `daysOutstanding()` UTC (`Application\ExportShipmentService.java:255-291`).
- [x] **AC-5** Feature invisible when `agri.enabled=false` (entitlement test); docs print Arabic+English correctly. — **MET**: `EntitlementCatalog.agri.enabled` + `TenantFeatureInterceptor` + `AccessCatalog FEATURE_AGRI` gates. Bilingual doc-print now shipped: `ExportShipmentDocService` renders the same generator in ar-EG (right-to-left sheet, Arabic headers) and en-US (LTR, English headers) from the DB-backed translation catalog; `ExportShipmentDocServiceTests` asserts RTL for ar, LTR for en, and bilingual column headers. Endpoints `GET /api/v1/trade/export-shipments/{id}/docs/{coo|packing-list|phytosanitary}.xlsx` (localized Content-Disposition filename); FE DOCS tab (`export.tabDocs`) with download buttons for all three docs (3 FE specs).
