# WP-40 — Public API + Webhooks + API Keys
**Priority:** 🟡 · **Owner:** Backend dev H · **Depends on:** — · **Effort:** ~7 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17G

## Business goal
Integrators/accountants want programmatic access (read invoices, create customers) and event pushes (invoice.paid, employee.created) instead of scraping. Daftra runs a whole developer portal on this — table stakes for "platform".

## Design decisions
- Scope v1: READ across core resources + CREATE for 2 low-risk resources (customer, journal note). Webhooks out: `invoice.paid`, `payment.recorded`, `employee.created`, `stock.low`.
- Auth: per-key (`bk_…`) with tenant binding + scoped permission set; keys NEVER grant more than their linked user role.

## Backend steps
1. Tables: `api_keys` (id, app_id, name, key_hash (only hash stored — full key shown once at creation), scopes text[], rate_limit_per_min default 120, active, last_used_at, created_by) · `webhook_endpoints` (app_id, url, secret, events[], active) · `webhook_deliveries` (endpoint FK, event, payload JSONB, status, attempts, last_error).
2. Key auth filter: `X-Api-Key` → resolve hash → bind TenantContext + authority scope map → per-minute bucket check (429 with Retry-After).
3. Event dispatcher: domain services publish to outbox → delivery worker POSTs signed payload (`X-Signature` HMAC of body+secret), retries backoff 5 tries then dead.
4. Docs: OpenAPI yaml generated from controllers (springdoc if present, else hand-maintained `/api/v1/docs` static page) — README explains.
5. Codes `APIKEY_*`, `WEBHOOK_*`.

## Frontend steps
1. Settings→Integrations: keys list (create-once reveal dialog with copy), scopes multiselect, revoke; webhook endpoints CRUD + recent deliveries viewer (status, response code, redrive button).
2. Keys ~14.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Key with `invoices:read` reads invoices but 403s creating customer without scope; wrong key → generic 401 (no enumeration). — **NOT MET**: no `X-Api-Key` filter exists; `ApiKeyService` is pure CRUD storing `scopes` but never enforcing them.
- [ ] AC-2 Rate limit trips at configured N/min returning 429+Retry-After; counter resets next minute (fake clock). — **NOT MET**: `rateLimitPerMin` stored but never enforced (only unrelated `LoginRateLimiter`).
- [ ] AC-3 invoice.paid delivery body matches published schema snapshot; invalid-signature receiver documented test shows rejection guidance; 5 failures → dead status visible in UI. — **NOT MET**: no webhook outbox/delivery worker, no HMAC signing, no backoff→dead.
- [ ] AC-4 Full key displayed exactly once; only hash persisted (DB inspection test); revocation effective immediately incl. in-flight? (documented: next request rejected). — **PARTIAL**: key_hash-only persistence + create-once reveal exist; revocation/immediate-effect path not verified (no filter to enforce it). Deliveries viewer + `POST .../redrive` endpoints exist (`PlatformController.java:109-118`).
- [ ] AC-5 Tenant isolation: key from app A cannot read app B even with same key string replayed (context binding test). — **NOT MET**: no key-auth context binding to test.
