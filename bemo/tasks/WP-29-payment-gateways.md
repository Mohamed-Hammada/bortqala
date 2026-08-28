# WP-29 — Payment Gateways & Customer Payment Page (Egypt: Fawry/Paymob/InstaPay)
**Priority:** 🟠 · **Owner:** Backend dev D · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17B

## Business goal
Collect receivables faster: generate a payment link (Fawry/Paymob/InstaPay) for an invoice or party balance; customer opens a PUBLIC tokenized page, pays, webhook confirms → ledger + receipt automatically. No login needed by customer.

## Design decisions
- Provider interface `PaymentGatewayClient` with v1 adapter `PaymobClient` (most common EG aggregator) behind property `hr.payments.gateway=NONE|PAYMOB`; NONE = feature off. Secrets via env only.
- Webhook is the source of truth; polling fallback job for missed webhooks. Idempotent on provider transaction id.

## Backend steps
1. Tables: `payment_links` (id, app_id, kind INVOICE|PARTY_BALANCE|CUSTOM, ref ids, amount, currency EGP, token UUID (URL-safe), status PENDING|PAID|EXPIRED|CANCELLED, gateway_ref NULL, expires_at) · `gateway_transactions` (link FK, provider_txn_id UNIQUE, raw_payload JSONB, amount, paid_at).
2. Endpoints: admin `POST /api/v1/finance/payment-links` (+WhatsApp/copy string builder), list/revoke; public (no auth, rate-limited): `GET /p/{token}` minimal page payload (amount, company name, line description — no PII beyond what's needed), `POST /p/{token}/webhook` verifying HMAC signature per provider spec.
3. On confirmed payment: create customer receipt via existing sales/treasury posting + partner-ledger credit; mark link PAID exactly once (unique txn id guard); notify AR user via NotificationCenter.
4. Codes: `PAYLINK_*`, `WEBHOOK_SIGNATURE_INVALID`.

## Frontend steps
1. Sales/parties screens: "🔗 Payment link" action on invoice row and party statement → dialog showing link + WhatsApp share button + expiry picker; links table with statuses.
2. Public page `features/public/pay/:token` OUTSIDE auth guard: clean Arabic-first card, amount, pay button (redirect to gateway checkout), success/failure states translated.
3. Keys `finance.paylink*` (~16).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Link created for 5,000 invoice → webhook PAID posts exactly one receipt + one ledger credit; replayed webhook (same txn id) is a no-op. — **PARTIAL**: link create+PAID flow implemented; idempotent replay covered by `handleWebhook_idempotent_duplicateTxn_noop` unit test, but provider adapter is `NoOpGatewayClient` — no live webhook path (external provider).
- [x] AC-2 Tampered webhook signature → 401 `WEBHOOK_SIGNATURE_INVALID`, zero state change (security test). — **MET**: `PaymentLinkService.verifySignature` (HMAC-SHA256 over `providerTxnId` behind `hr.payments.webhook-secret`, skipped when blank); missing/tampered signature → `WEBHOOK_SIGNATURE_INVALID` 401 before any state change. `handleWebhook_tamperedSignature_rejectedBeforeAnyStateChange` + `handleWebhook_validSignatureAndSecret_processesWebhook` tests green; V407 translation rows added.
- [ ] AC-3 Expired link page shows expired state and rejects webhook payment as late (provider-refund note logged). — **PARTIAL**: expiration flag exists; late-reject + refund-note path unverified.
- [x] AC-4 Public page leaks nothing beyond {companyName, description, amount} (payload snapshot test). — **MET** (public `/p/{token}` minimal payload + snapshot test).
- [ ] AC-5 Gateway NONE hides all actions; secrets never in repo (grep gate); poller catches a webhook-simulated miss within configured interval exactly once. — **PARTIAL**: NONE-off + env-only secrets hold; poller fallback job NOT MET.
