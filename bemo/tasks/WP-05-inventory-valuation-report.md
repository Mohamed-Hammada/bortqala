# WP-05 — Inventory Valuation Report Surface (B-5 residual)
**Priority:** 🟠 · **Owner:** Backend dev C · **Depends on:** — · **Effort:** 1–2 days
⚠️ **VERIFY FIRST — do not build what exists.** FIFO/weighted-average valuation + GL posting ALREADY EXIST in operations inventory control (backend skill states it explicitly). Your job is the *report surface*, if genuinely missing.

**Read first:** `_GLOBAL-RULES.md`

## Step 0 (mandatory, half day)
Grep `be/src/main/java/com/bemo/hr/operations` + `inventory` for `valuation|FIFO|weightedAverage|costLayer|asOf`. Write findings at the top of your PR description listing which endpoints already exist. Only implement gaps below that are actually absent.

## Likely gap → Backend steps
1. `GET /api/v1/operations/valuation-report?itemId=&warehouseId=&asOf=` returning per item: on-hand qty, unit cost (method-aware), inventory value, and WHICH method produced it (evidence, not a toggle). If layer tables lack as-of semantics, add repo query summing layers consumed ≤ asOf date. Epoch-millis for dates.
2. Permission: reuse inventory read authority; explicit `@PreAuthorize`.
3. If a report total must reconcile to GL: expose `glInventoryAccountBalance` in the response so FE can show reconciliation delta.

## Frontend steps
1. Operations workbench "Valuation" card/table: columns item, warehouse, qty, unit cost, value, method badge; as-of picker; Excel export button reusing exporter pattern with Arabic filename.
2. Keys: `operations.valuation*` (~8).

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** PR description lists pre-existing valuation endpoints found (or none) with file paths — reviewer can verify no duplication.
- [x] **AC-2** Report totals for a demo tenant reconcile to the GL inventory-control account within displayed tolerance; delta shown on screen when nonzero.
- [x] **AC-3** FIFO vs WA items each display the correct method badge matching their configuration.
- [x] **AC-4** As-of a past date, values reflect historical layers (fixture test proves an old receipt doesn't leak into current cost).
- [x] **AC-5** Export downloads localized headers; unknown `itemId` → clean 404 translated; all DoD gates pass.

## Step 0 findings (verify-first)
Already existed — NOT rebuilt:
- `GET /api/v1/operations/valuation/report` (`OperationsController.valuationReport`) + full FE table (`operations.page.html` valuation section) — current values only.
- `GET/PUT /valuation/settings`, `POST /valuation/revaluations`, `GET /valuation/movements/{movementId}`.
- FIFO/WA engine with cost layers + GL posting (`InventoryValuationService.valueMovement`, `postMovementJournal`).
- `POST /api/v1/operations/inventory/valuation/calculate|reconciliation` (`InventoryValuationSnapshotService`).

Genuine gap closed here: the GET report had no as-of/warehouse/item filters, no per-row method badge, and no GL reconciliation delta.

## Evidence (2026-08-24, V349)
- `GET /valuation/report?asOf=<epochMs>&warehouseId=&itemId=` (explicit `@PreAuthorize` SUPER_ADMIN/ADMIN/INVENTORY_MANAGER/FINANCE_MANAGER/AUDITOR). New repo queries: `balanceAsOf/balanceByWarehouse/balanceAsOfAndWarehouse` (StockMovementRepository), `inventoryValueAsOf/valuedQuantityAsOf` (costs), `revaluationValueAsOf`. Unknown `itemId` → existing translated `OPS_ITEM_NOT_FOUND` 404.
- Response gained `glInventoryAccountBalance` (net debit−credit of POSTED journal lines for `policy.inventoryAccountId`, TrialBalance-style) + `inventoryVarianceFromGl`; rows carry `valuationMethod` badge. Warehouse-filtered value approximated by global avg unit cost × warehouse qty (movement costs are not warehouse-stamped — documented).
- Deviation: method is tenant-wide policy (not per-item config), so every row shows the configured policy's method.
- FE: as-of date input + warehouse select + apply/export buttons; reconciliation banner (variance highlighted when ≠0); per-row method badge column; `loadValuation(filters)` + `exportValuation()` store methods → `/api/v1/exports/inventory-valuation.xlsx` (Arabic filename تقييم-المخزون). V349 CSV 16 keys (export.sheet/columns + operations.valuation.asOf/warehouseFilter/allWarehouses/methodBadge/glBalance/variance/export/itemFilter).
- Tests: BE 4 new (`asOfReportExcludesLaterReceiptsFromCost`, `reportReconcilesAgainstPostedGlInventoryAccount`, `unknownItemFilterReturnsClean404`, `warehouseFilterApproximatesValueWithGlobalAverageUnitCost`). FE 2 new store specs.
- Gates: BE **805 tests / 196 suites / 0 failures**, error codes PASS, catalog **13,980 PASS** (renamed colliding `export.column.value`→`export.column.valuationValue`); floor ≥805. FE **480 tests / 100 files**, check:i18n **4,657**, hardcoded **0**, build green; floor ≥480.
