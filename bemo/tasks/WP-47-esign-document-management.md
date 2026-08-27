# WP-47 — eSign Workflows + Document Management (GED)
**Priority:** 🟢 · **Owner:** Backend dev H + FE · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20

## Business goal
Two linked capabilities: (1) route a document (offer, contract, consent) for sequential approvals with typed-drawn/hash signature capture and audit evidence; (2) organize all attachments company-wide into folders/tags with search — replacing flat attachment lists.

## Design decisions
- v1 signature = structured evidence (signer identity + timestamp + content SHA-256 + intent click), NOT certificate-based; Egypt Trust token integration stays an interface stub (`SignatureProvider=INTERNAL|EGYPT_TRUST` future).
- Files stored via existing attachment storage util; GED adds metadata layer over ALL existing attachment rows (backfill script maps known trios to folders by module).

## Backend steps
1. GED: `doc_folders` (parent tree, name per locale optional) · `doc_tags` (app-scoped) + link table; unified `GET /api/v1/documents?folder=&tag=&q=&kind=` searching name+extracted-text-later across attachments registry; move/rename/tag endpoints audited.
2. Signature packets: `signature_packets` (title, file ref or generated doc snapshot, status DRAFT|ROUTING|COMPLETED|REJECTED) + `signature_steps` (order seq, signer user/party, role label, status PENDING|SIGNED|DECLINED, signed_at, ip, content_sha256, method CLICK_TO_ACCEPT|DRAWN_DATA_URL) — routing advances only in order; any DECLINE aborts with reason; completed packet stores final manifest JSON (court-friendly evidence).
3. Notification to next signer via NotificationCenter/email hook.
4. Codes `DOC_*`, `SIGN_*` (~10).

## Frontend steps
1. `features/documents/`: explorer tree + tag chips + global search page; drag-free move via dialog.
2. Sign flows: create packet wizard (file upload or pick generated doc → add ordered signers); "Awaiting my signature" inbox item → sign dialog showing doc preview + accept checkbox + optional drawn signature canvas (store dataURL) ; status tracker visualization of steps.
3. Keys ~24.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Sequential enforcement: step-2 signer cannot sign before step-1 (API rejects out-of-order); decline by step-1 halts packet as REJECTED with reason recorded.
- [ ] AC-2 Manifest integrity: recomputed SHA-256 of stored file equals hash captured at each signature (tamper test modifying byte fails verification view).
- [ ] AC-3 Every step logs ip+utc timestamp+method; manifest export downloads as JSON evidence bundle.
- [ ] AC-4 GED backfill: existing operations attachment rows appear under mapped folders after migration; none duplicated (count reconciliation).
- [ ] AC-5 Search finds by tag and filename prefix within ≤300ms on 1k-doc fixture; cross-tenant zero leakage.
