# AGENT SESSION SUMMARY — Complete
- Replaced all hardcoded user-facing messages in FE (dashboard, settings, report-review, audit-logs, fiscal-periods, login, app-shell, payroll) with `i18n.t()` translation keys
- Added all new keys to `DEFAULT_FALLBACKS` in `i18n.service.ts` (ar-EG + en-US), removed duplicates
- Backend: added `translate()`, `translateOrDefault()`, `isSupported()` to `TranslationService.java`
- Backend: `ApiExceptionHandler.java` now locale-aware using `Accept-Language` header
- Backend: created Liquibase v39 migration (`error.*` translation keys) + CSV seed
- Remaining FE files to update (lower priority): minor feature templates, `api-error.ts` KNOWN_MESSAGES
- Fixed pre-existing duplicate `review.fullyReviewedTag` keys; FE production build passes clean

# HR platform handoff

This repository intentionally contains two applications:

- `be/`: Spring Boot backend. Before changing it, read `be/skills/hr-backend/SKILL.md` completely.
- `fe/`: Angular frontend. Before changing it, read `fe/skills/hr-frontend/SKILL.md` completely.

For changes spanning both applications, define the backend API contract first, then update the typed frontend model and data-access layer. Keep business calculations in the backend; the frontend may format results but must not reimplement attendance or payroll rules.

Current phase: the end-to-end MVP is implemented and verified on PostgreSQL. It includes SaaS app-scoped JWT authentication, per-user theme/density/locale preferences, database-backed Arabic/English translations, multi-role authorization, dynamic attendance categories and schedules, custom report ranges and pay-cycle presets, biometric imports, attendance review, approval/reopen, dashboards, Excel exports, epoch-millisecond API dates, and structured tracing. Tenant-owned entities must keep `@TenantId`; mutable aggregates keep `created_at`/`updated_at`, while immutable evidence uses semantic creation/import timestamps. Read both local skills before extending it.

## Menu Registration & Permission Synchronization Protocol

Whenever creating or adding a new feature/module with sidebar menu items, enforce the following 4-part synchronization protocol to guarantee instant menu visibility across all user roles and sessions:

1. **Frontend Visibility (`app-shell.component.ts`)**:
   - Register items in `items` array with appropriate `workspace` group key.
   - Update `visible(item)` and `AuthService.hasMenuAccess(menuId)` so that admin roles (`SUPER_ADMIN`, `ADMIN`) and new feature menu IDs (e.g. `workforce-*`) are explicitly returned as `true`, overriding obsolete local storage session arrays.
2. **Database Translation Keys & Fallbacks (`i18n.service.ts` & CSV)**:
   - Add the workspace section key (`workspace.<name>`) and nav label keys to `DEFAULT_FALLBACKS` in `i18n.service.ts` (both `ar-EG` and `en-US`).
   - Add translation rows to the Liquibase translation CSV (e.g. `workspace.workforce`).
3. **Database User Schema Migration (`v37` Liquibase)**:
   - Add a Liquibase changeset executing SQL update on `app_users.allowed_menus` to append the new menu IDs to existing user rows in PostgreSQL.
   - Update default fallback strings in `AppUser.java` and `AuthService.java` for new user creation.
4. **User Management UI (`users.page.ts`)**:
   - Add the new menu IDs to `menuOptions` in `users.page.ts` for explicit admin toggle control.

