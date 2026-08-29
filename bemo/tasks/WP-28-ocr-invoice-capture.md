# WP-28 — OCR Supplier-Invoice Capture (photo → draft GRN)
**Priority:** 🟠 · **Owner:** Backend A + FE · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §13.2 ⭐ / §17A

## Business goal
Photograph a paper supplier invoice → system extracts supplier, date, lines (item/qty/price) into an editable draft goods-receipt. Biggest time-saver vs manual entry; top competitor gap.

## Design decisions
- Pluggable extractor interface `InvoiceExtractor` in `shared` or `procurement`: v1 adapter = external vision API via configurable provider (`hr.ocr.provider=NONE|OPENAI_VISION|CUSTOM_URL`, key via env — NEVER committed); NONE returns explicit "OCR not configured" so feature ships safely behind flag.
- Human-in-the-loop ALWAYS: extraction produces a DRAFT with per-field confidence; user edits before anything commits. No auto-posting.

## Backend steps
1. Tables: `ocr_capture_jobs` (id, app_id, uploaded_by, image attachment trio, status UPLOADED|PROCESSING|REVIEW|CONVERTED|FAILED, extracted JSONB payload, confidence summary, error_code NULL).
2. Endpoints: `POST /api/v1/procurement/ocr-capture` (upload ≤5MB image) → job; worker (job primitive in shared) calls provider, normalizes to `{supplierName?, invoiceNo?, date?, lines[{name,qty,unitPrice}]}`; `GET /{id}` poll; `POST /{id}/convert {partyId, warehouseId, fieldMap…}` → creates DRAFT GRN via existing service (never posts).
3. Fuzzy match supplier name → existing parties suggestions list in payload.
4. Codes: `OCR_NOT_CONFIGURED`, `OCR_PROVIDER_FAILED`, `OCR_IMAGE_INVALID`.

## Frontend steps
1. Procurement page "📷 Scan invoice" button → upload dialog → review screen: side-by-side image + editable extracted fields with confidence badges (high ≥0.85 green, else amber); supplier picker pre-suggested; Convert→draft GRN reuses existing GRN form prefilled.
2. Keys `procurement.ocr*` (~14). Feature hidden when provider NONE (or visible-but-disabled with hint — choose hidden).

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** With provider NONE: upload returns translated not-configured (`OCR_NOT_CONFIGURED` 503) and UI never shows broken state.
- [x] **AC-2** Golden-image fixture (printed invoice) extracts supplier/no/date/lines through mock provider; confidence summary extracted.
- [x] **AC-3** Nothing hits inventory/partner-ledger until user converts AND the created record is a DRAFT requiring normal confirm flow (ledger-zero assertion).
- [x] **AC-4** Provider failure surfaces `OCR_PROVIDER_FAILED` with retry; job row keeps image for reprocess.
- [x] **AC-5** API key only ever from env/config; oversize/bad-type uploads rejected like REM-005 rules (`OCR_IMAGE_INVALID`).
