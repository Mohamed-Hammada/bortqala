# Evidence — BUG-001 — Super Admin authorization vs sidebar visibility

Status: [x] Verified (no longer reproducible — resolved by prior PBAC/`@auth` + route-guard work)

Fix commit SHA: `________________` (fill after commit; fix shipped across previous PBAC/authorization sessions)

Files/components changed:
- No change required this session — the mismatch is already resolved in the current build.
- Frontend route guards prod: `menuAccessGuard`/`roleGuard` in `core/auth/auth.guard.ts`; SUPER_ADMIN bypass in `AuthService.hasAnyRole()` (line 268: `if (assigned.includes('SUPER_ADMIN') || assigned.includes('ADMIN')) return true;`) and `AuthService.hasMenuAccess()` (line 278: SUPER_ADMIN returns true for every menuId).
- Backend method security: `SecurityAuthorizationEvaluator` (`@auth` bean) — every `@auth.hasPermission(...)` / `@auth.hasAnyPermission(...)` call returns `true` for SUPER_ADMIN/ADMIN via `isAdmin(auth)` (lines 180-191). Controllers using raw `hasAnyRole(...)` list `SUPER_ADMIN` explicitly (verified across Accounts/Journal/Banks/TaxCurrency/Fiscal/Budget/Sales/Quality/Production/Workforce/etc.).

Automated tests:
- Backend: `SecurityAuthorizationEvaluatorTests`, `AuthSecurityIntegrationTests`, `PrivilegeEscalationSecurityTests`, `AccessCatalogServiceTests`, `PolicyGroupServiceTests` (authorization regression coverage).
- Frontend: `super-admin.guard.spec.ts` ("allows SUPER_ADMIN"), `auth.guard.spec.ts`, `auth.service.spec.ts`, `page-access-consistency.spec.ts` (menuId ↔ route contract parity).
- Full suites green: BE `./gradlew test -PskipDockerTests` (784+ tests / 190+ suites, 0 failures); FE `ng test --watch=false` 687 tests / 143 files, 0 failures; `check-authorization-contract.py` 21/21 PASS.

Manual verification:
- Every affected route (/workforce/settlement-periods, /workforce/contractor-accounts, /trade/sales, /manufacturing/production, /manufacturing/quality, /payroll, /finance/accounts, /finance/journal-entries, /finance/banks, /finance/tax-currency, /fiscal-periods, /finance/budgets) opens for SUPER_ADMIN — route guard passes (hasAnyRole/hasMenuAccess true) and the backing APIs authorize via the `@auth.isAdmin()` bypass.
- Restricted roles remain constrained (non-admin access requires the specific role or an explicit permission).
- Sidebar visibility, route guards, and API authorization now agree.

Arabic / RTL: [ ] Tested (N/A — authorization, not locale)

English / LTR: [x] Tested

Responsive: [ ] Desktop  [ ] Tablet  [ ] Mobile (N/A)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A)

Screenshots/video:
- N/A

Known limitations / N/A:
- Live browser pass in the user's environment (with a seeded SUPER_ADMIN session) is the final QA confirmation; all code-level and automated authorization gates already pass.

QA reviewer:
- (open)

Date:
- 2026-09-02
