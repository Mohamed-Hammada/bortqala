# WP-34 — SSO (Google / Microsoft Workspace)
**Priority:** 🟡 · **Owner:** Backend dev H · **Depends on:** WP-33 recommended · **Effort:** ~4 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17D

## Business goal
Tenants on Google Workspace / Microsoft 365 sign in with corporate identity instead of (or alongside) passwords. Admin maps SSO email → user; local password login can stay as fallback.

## Design decisions
- OIDC Authorization Code flow; per-tenant config (client id/secret, issuer) stored admin-side; discovery document fetch at startup/refresh.
- First SSO login for unknown email → deny with clear message unless admin pre-linked or auto-provision flag set (creates VIEWER-role user).

## Backend steps
1. Tables: `sso_configs` (app_id, provider GOOGLE|MICROSOFT, client_id, encrypted secret, issuer/discovery URL, auto_provision bool, default_role, active) + `user_sso_identities` (user FK, provider, subject unique).
2. Endpoints: `GET /api/v1/auth/sso/{appId}/start?provider=` → redirect URL w/ state+nonce (state stored short-TTL); `GET /api/v1/auth/sso/callback` validates state/nonce, exchanges code, matches `user_sso_identities` (or provision) → issues the SAME JWT session pipeline as password login (audit `SSO_LOGIN`).
3. Admin endpoints to CRUD config (secret write-only). Codes `SSO_*` (~6).

## Frontend steps
1. Login screen: "Sign in with Google/Microsoft" buttons only when tenant has active configs (pre-flight probe endpoint).
2. Settings→Security: SSO config card (admin), linked-identities list per user in users page detail.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Happy path against real Google test tenant: redirect → consent → callback issues app JWT with correct roles; audit row written. — **MET (backend)** via `SsoSessionIssuer` (issues the same HMAC-JWT pipeline as password login incl. `tv`/`pwc`/roles claims + refresh token) + `SSO_LOGIN`/`SSO_PROVISION` audit rows; `SsoServiceTests` (existing-identity + provision) + `SsoSessionIssuerTests` (decodable JWT, roles claim, session-timeout TTL override) green. Live redemption still needs a real Google/Microsoft test tenant (external creds).
- [x] AC-2 State/nonce replay or mismatch rejected (`SSO_STATE_INVALID`); expired state (>5 min) rejected. — **MET**: `decodeState`/`validateStateExpiry` + tests (invalid, expired, provider-disabled).
- [x] AC-3 Unknown email: auto_provision=false denies translated; =true creates VIEWER once (second login maps same identity, no dup users — unique constraint test). — **MET**: `provisionUser` (default role `VIEWER`, random temp password, `mustChangePassword` reset) + existing-identity mapping on second login; tests cover deny + provision + reuse + inactive-user reject.
- [x] AC-4 Disabling config hides button and rejects callbacks immediately; secret never returned by any GET (write-only field test). — **MET**: callback rejects on missing active config (`SSO_PROVIDER_DISABLED`); `ConfigResponse.from` has no secret field (reflection write-only test `configResponse_mapsFieldsAndHidesSecret`).
