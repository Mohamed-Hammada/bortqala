# WP-33 — Security Pack: 2FA, Password Policy, Sessions, IP Rules, Trusted Devices
**Priority:** 🟠 · **Owner:** Backend dev H (security) + FE · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17D

## Business goal
Enterprise/security-conscious tenants demand: TOTP authenticator 2FA with backup codes, configurable password policy, session timeout policy UI, per-role IP allowlisting, and a trusted-devices page (pairs with push prompt device identity).

## Current state
JWT with `tv` token-version claim + revoke-all exists; HttpOnly refresh cookie; ADMIN session-timeout setting exists. No 2FA, no password policy config, no IP rules, no device list UI.

## Backend steps
1. TOTP: `user_totp` (secret encrypted at rest w/ app key, enabled_at, last_used_counter) — enroll flow `POST /auth/2fa/enroll` returns otpauth:// URI + QR payload; verify 6-digit code to activate; login challenge: when enabled, first factor returns `2FA_REQUIRED` + short-lived challenge token → `POST /auth/2fa/verify`; backup codes: 10 single-use hashed codes generated at enroll, regenerable.
2. Password policy per tenant: min length, require classes, history N, max age days — enforced in change/reset paths (`PASSWORD_POLICY_*` codes); admin UI writes `tenant_security_settings`.
3. Trusted devices: on login store device fingerprint (existing browser device id / Android id from WP-14) as `trusted_devices` (label, last_seen, revoked flag); revoke forces fresh password+2FA (bump tv).
4. IP allowlist per ROLE: CIDR list per role checked post-auth (`IP_NOT_ALLOWED`); empty = allow all. Careful: document lock-out risk, super-admin bypass flag.
5. Codes families: `TOTP_*`, `PASSWORD_POLICY_*`, `DEVICE_*`, `IP_NOT_ALLOWED`.

## Frontend steps
1. Settings → Security tab: 2FA enroll wizard (QR render via tiny inline SVG generator — no new lib), backup-codes download, disable-with-password; trusted devices table with revoke; policy editor (admin); session-timeout moved here.
2. Login page: second-step code input state.
3. Keys ~30 `settings.security.*` / `auth.totp*`.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Enroll → logout → login requires code; wrong code rate-limited; valid TOTP at t and t+30s window both accepted (RFC vector fixtures).
- [x] AC-2 Each backup code works exactly once; regeneration invalidates previous set (hash check).
- [x] AC-3 Policy "min 12 + upper+digits" rejects weak passwords with specific violated-rule message; history=3 blocks reuse of last 3.
- [x] AC-4 Revoked device cannot silent-login: refresh cookie rejected after revocation (tv bump proven).
- [x] AC-5 Role allowlist blocks login from outside CIDR even with correct credentials; SUPER_ADMIN bypass only with explicit flag true; misconfigured empty list never locks everyone out (safe-default test).

## Deliverables Summary
- **Liquibase Migrations**: `v442` (`20260830_v442_security_pack_schema.yaml`: `sec_user_totp`, `sec_user_totp_backup_codes`, `sec_tenant_security_settings`, `sec_user_password_history`, `sec_trusted_devices`, `sec_role_ip_allowlists`), `v443` (`20260830_v443_security_pack_translations.yaml` + CSV: bilingual `ar-EG`/`en-US` keys).
- **Backend Architecture**: Package `com.bemo.hr.security.pack` (`TotpService`, `PasswordPolicyService`, `TrustedDeviceService`, `IpAllowlistService`, `SecurityPackController`, `Auth2FaController`), integrated with `AuthService` (2FA challenge and verification flow, `tv` bump on remote revocation, role CIDR allowlists).
- **Frontend Architecture**: `SecuritySettingsComponent` in `SettingsPage` (`/settings` tab `security`), `SecurityPackService`, `security-pack.models.ts`, and 2FA challenge flow in `LoginPage` (`login.page.ts` / `login.page.html`).
- **Verification**: 100% clean test execution (`TotpServiceTests`, `PasswordPolicyServiceTests`, `TrustedDeviceServiceTests`, `IpAllowlistServiceTests`, `SecuritySettingsComponent.spec.ts`), 629 FE tests passing, `check:i18n` with 5,711 keys, `check:hardcoded` with 0 violations across 143 HTML & 314 TS files, `check-error-codes.py` 794/794 clean.

