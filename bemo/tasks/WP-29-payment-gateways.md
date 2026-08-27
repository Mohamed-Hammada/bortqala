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
- [ ] AC-1 Link created for 5,000 invoice → webhook PAID posts exactly one receipt + one ledger credit; replayed webhook (same txn id) is a no-op.
- [ ] AC-2 Tampered webhook signature → 401 `WEBHOOK_SIGNATURE_INVALID`, zero state change (security test).
- [ ] AC-3 Expired link page shows expired state and rejects webhook payment as late (provider-refund note logged).
- [ ] AC-4 Public page leaks nothing beyond {companyName, description, amount} (payload snapshot test).
- [ ] AC-5 Gateway NONE hides all actions; secrets never in repo (grep gate); poller catches a webhook-simulated miss within configured interval exactly once.
