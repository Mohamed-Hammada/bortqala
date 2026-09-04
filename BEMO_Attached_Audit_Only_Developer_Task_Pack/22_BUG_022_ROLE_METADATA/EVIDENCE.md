# Evidence — BUG-022 — Role metadata disagrees with live access

Status: [x] Verified (role description now accurately matches effective access; root cause was BUG-001 incomplete bypass)

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- Shared root cause with BUG-001. No separate code change needed: SUPER_ADMIN full-access bypass is now enforced in all three layers.
- `fe/src/app/core/auth/auth.service.ts` — `hasAnyRole()` (line 268) and `hasMenuAccess()` (line 278) return true for SUPER_ADMIN/ADMIN, so the sidebar menu matches the description.
- Route guards (`menuAccessGuard`, super-admin guard) agree with the menu.
- Backend `SecurityAuthorizationEvaluator.isAdmin()` bypasses `@auth.hasPermission` for SUPER_ADMIN/ADMIN; raw `hasAnyRole` controllers explicitly include SUPER_ADMIN.
- Role metadata `descriptionKey` = `role.superAdminHint` (`users.page.ts:163`).
- Description text (`translations.csv` t-006885): ar-EG "تحكم كامل في المنصة: المستخدمون والإعدادات وجميع الوحدات والتدقيق." / en-US "Full platform control: users, settings, all modules, audit."

Automated tests:
- `SecurityAuthorizationEvaluatorTests`, `AuthSecurityIntegrationTests`, `PrivilegeEscalationSecurityTests`, `super-admin.guard.spec.ts`, `page-access-consistency.spec.ts` — all assert SUPER_ADMIN can access every declared module.
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures.

Manual verification:
- As SUPER_ADMIN, sidebar shows all modules (hasMenuAccess bypass), every route passes the guard, and every API call is authorized — matching the "full platform control" description. No module is denied despite the description claiming full control.

Arabic / RTL: [x] Tested

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A)

Screenshots/video:
- N/A

Known limitations / N/A:
- None.

QA reviewer:
- (open)

Date:
- 2026-09-02
