# Menu Registration & Permission Synchronization Protocol

Use whenever creating or adding a new feature/module that needs a sidebar menu item.

Extracted from `AGENTS.md` (formerly under "HR platform handoff"), which is now historical-only. This is the current, authoritative version of the protocol.

## The two applications

This repository intentionally contains two applications:
- `be/` — Spring Boot backend. See `.claude/rules/backend.md` and `be/skills/hr-backend/SKILL.md`.
- `fe/` — Angular frontend. See `.claude/rules/frontend.md` and `fe/skills/hr-frontend/SKILL.md`.

For changes spanning both applications, define the backend API contract first, then update the typed frontend model and data-access layer. Keep business calculations in the backend; the frontend may format results but must not reimplement domain rules (e.g. attendance or payroll).

## 4-part synchronization protocol

Enforce all four parts together to guarantee instant menu visibility across all user roles and sessions. A menu item is not considered wired up until all four are done.

1. **Frontend visibility** (`app-shell.component.ts`, and the navigation contract in `fe/src/app/core/navigation/`):
   - Register the item in the `items` array with the appropriate `workspace` group key.
   - Update `visible(item)` and `AuthService.hasMenuAccess(menuId)` so that admin roles (`SUPER_ADMIN`, `ADMIN`) and the new feature's menu IDs (e.g. `workforce-*`) are explicitly returned as `true`, overriding obsolete local-storage session arrays.
2. **Database translation keys & fallbacks** (`i18n.service.ts` & Liquibase CSV):
   - Add the workspace section key (`workspace.<name>`) and nav label keys to `DEFAULT_FALLBACKS` in `i18n.service.ts` for both `ar-EG` and `en-US`.
   - Add translation rows to the Liquibase translation CSV (e.g. `workspace.workforce`) in both locales, following `.claude/rules/database.md`.
3. **User schema migration** (Liquibase):
   - Add a new Liquibase changeset (next available version per `.claude/rules/database.md` — the original protocol was established at `v37`; current changelog numbering is far past that, so use the next free version) executing an idempotent SQL update on `app_users.allowed_menus` to append the new menu IDs to existing user rows in PostgreSQL.
   - Update default fallback strings in `AppUser.java` and `AuthService.java` for new user creation.
4. **User management UI** (`users.page.ts`):
   - Add the new menu IDs to `menuOptions` (`USER_MENU_OPTIONS`) in `users.page.ts` for explicit admin toggle control, and to `AccessCatalog` if new permissions were introduced.

## Verification

Confirm the menu appears for the intended roles, that both locales render translated labels (no raw keys, no fallback-to-key), and that `npm run check:i18n` passes for the new keys.
