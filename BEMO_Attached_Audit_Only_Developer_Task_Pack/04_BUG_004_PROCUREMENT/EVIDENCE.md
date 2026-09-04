# Evidence — BUG-004 — Procurement exposed but disabled

Status: [x] Verified (enabled end-to-end by default; no feature-disabled gate on the page)

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- No code change required this session — procurement is enabled end-to-end by default in the current build.
- Feature flag: `procurement.enabled` defaults to **true** (`shared/security/EntitlementCatalog.java:18`, `defaultEnabled=true` at 32-34). `TenantFeatureInterceptor` returns 403 `FEATURE_DISABLED` only when explicitly disabled.
- Frontend menu gate `auth.service.ts:286-288` gates menu `procurement` on `activeFeatures` containing `procurement.enabled` OR `purchasing.enabled`; the route is guarded by `menuAccessGuard` (which would block the page before it rendered if disabled).
- `features/trade/procurement/procurement.page.{ts,html}` contains NO feature-disabled gate/banner — only per-button save/validation `[disabled]` states. The New PO button (`procurement.page.html:12`) calls `openNewPo()` unconditionally when the PO tab is active.

Automated tests:
- Backend `ProcurementServiceTests`/`OperationsDocumentReferencesTests`/payment/3-way-match suites green; `./gradlew test -PskipDockerTests` passes.
- Frontend `procurement.page.spec.ts` (PO lifecycle, GRN partial receipt, invoice 3-way match resolve, payments) green; `ng test --watch=false` 687 tests / 143 files, 0 failures.

Manual verification:
- New PO (`openNewPo`, submitPo) with supplier selection (`onPoSupplierSelected`) + order lines (`addItemLine`).
- Lifecycle DRAFT→ISSUED→PARTIALLY_RECEIVED→RECEIVED (`issuePo`, `cancelPo`).
- Partial receipts via GRN (`openNewGrn`, `acceptedGrnQuantity`, `submitGrn`).
- Invoice matching (`performThreeWayMatch`, `resolveMatch`).
- Accounting posting (`ProcurementAccountingService` postSupplierInvoice/Payment/SettlementDiscount).
- Payments incl. settlement discount + installment plans.
- All backed by `ProcurementController` routes (`/orders`, `/goods-receipts`, `/invoices`, `/payments`, `/three-way-match`, `/resolve`) + `ProcurementService`.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [x] Enter  [ ] Space  [ ] Escape (wizard forms)

Screenshots/video:
- N/A

Known limitations / N/A:
- If a tenant explicitly disables `procurement.enabled`, the menu + route + API all agree to block it (no "controls rendered but disabled" state). Default is fully enabled.

QA reviewer:
- (open)

Date:
- 2026-09-02
