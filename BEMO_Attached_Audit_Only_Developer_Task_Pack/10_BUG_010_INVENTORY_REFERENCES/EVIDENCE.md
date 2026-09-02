# Evidence — BUG-010 — Inventory movement lacks distinct document references

Status: [x] Verified

Fix commit SHA: `________________` (fill after commit; feature shipped in prior REM-005 session)

Files/components changed:
- `fe/src/app/features/operations/operations.models.ts` — `StockMovement` / `TransactionPayload` carry distinct `purchaseOrderNo`, `receiptNo`, `deliveryNoteNo`, `invoiceNo`, `voucherNo`, `externalRef`, `warehouse`, `attachmentName`, `attachmentContentType`, `attachmentSize`.
- `fe/src/app/features/operations/operations.page.ts` — `requiredReferences()` returns type-specific required keys (`SUPPLY_RECEIPT`→PO+receipt, `PROCESSING_INTAKE`→receipt, `PROCESSING_DELIVERY`/`EXPORT_SALE`/`SORTING_SALE`→deliveryNote, `ADJUSTMENT`→voucher); `primaryReference()` resolves a primary reference with precedence; `documentTypeLabel()`; `onAttachmentSelected()` (≤5 MB, images/PDF/Excel).
- `fe/src/app/features/operations/operations.page.html` — movements table shows a document-type column and the resolved primary reference (+ externalRef, warehouse, 📎 attachment as secondary lines); the linked-documents form section (`operations.documents` / `operations.referencesHint`) renders each reference field with a required `*` flag per type and an attachment chip.

Automated tests:
- `operations.page.spec.ts` covers full payload sans attachment, per-type required-ref blocking, primary-reference precedence, attachment rejection.
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures; `check:i18n` 5,884 keys PASS; `check:hardcoded` PASS.

Manual verification:
- Distinct references (PO, receipt, delivery note, invoice, voucher, external) are stored and shown in list/detail views.
- Requiredness depends on movement type (per `requiredReferences`).
- Attachment linkage shown and cleared.

Arabic / RTL: [x] Tested (keys localized)

English / LTR: [x] Tested

Responsive: [ ] Desktop  [ ] Tablet  [ ] Mobile (N/A - unchanged layout)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [x] Enter  [ ] Space  [ ] Escape (input fields)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
