# BEMO ERP — Add/Edit User UX Redesign

Source reviewed directly from `Mohamed-Hammada/bortqala`, branch `fm_bemo_consolidated`, on 2026-08-10.

## Why this redesign

The current Add/Edit User modal exposes several permission concepts at the same time: role selection, guided needs, page lookup, grouped menu permissions, effective-access preview, special permissions, account status, validation, conflicts, and sensitive-access acknowledgments. This makes a routine “create a user” task require knowledge of the internal authorization model.

The redesign follows **simple by default, advanced on demand**:

1. **Account basics first** — display name, username, password, optional category, and active status.
2. **Human-readable roles second** — role name + business description only. Technical role codes, sensitivity/kind badges, page/action counts, guided-role duplication, and page-search tooling are removed from the normal flow.
3. **Role-driven menu access for new users** — until an admin manually changes a menu, allowed menus are derived from the backend access catalog and selected roles. This prevents the common UX trap of choosing a correct role but accidentally leaving an unrelated menu list.
4. **Advanced access is collapsed** — module/menu overrides and effective-access preview are still available when needed.
5. **Existing users are safe** — edit mode preserves the user’s existing `allowedMenus`; role changes do not silently rewrite a previously customized menu configuration.
6. **Security behavior is preserved** — backend `/api/v1/users/access/validate` remains authoritative before save, including conflicts, sensitive access, and acknowledgment reasons.
7. **No new translation keys** — the redesign reuses existing database-backed i18n keys, so it does not introduce a Liquibase translation migration.

## Files changed

- `fe/src/app/features/users/users.page.html`
- `fe/src/app/features/users/users.page.scss`
- `fe/src/app/features/users/users.page.ts`
- `fe/src/app/features/users/users.page.spec.ts`
- `fe/src/app/features/users/README.md`

No backend API or database schema changes are required.

## Apply

From the root of your local `bortqala` checkout:

```bash
# Make sure you are on the exact source branch/version first.
git checkout fm_bemo_consolidated
git status

python path/to/apply_user_page_redesign.py

git diff -- fe/src/app/features/users
```

The script performs a strict Git blob-SHA preflight against the files that were inspected. If those source files changed after this review, it stops rather than applying the redesign to an unknown version.

## Verify

```bash
cd fe
npm run check:hardcoded
npm run check:i18n
npm test -- --watch=false
npm run build
```

### QA flow

1. Open **Users** and click **Add User**.
2. Confirm the normal form only exposes account basics, role cards, and the two account-level permission switches.
3. Select `WORKFORCE_MANAGER` (or another non-admin role) and expand advanced menu access; matching menus should be selected automatically for a new user.
4. Manually change one menu, then change a role; the manual menu configuration should now be preserved.
5. Create an `ADMIN` user; runtime/admin override behavior should remain unchanged.
6. Edit an existing user with custom menus; opening/editing must not rewrite their saved menus automatically.
7. Exercise a role combination that produces a warning/conflict; save should still run backend validation and require an acknowledgment reason when the backend says it is required.
8. Test both Arabic RTL and English LTR, plus desktop and narrow/mobile dialog widths.

## GitHub write status

The connected GitHub integration could read the repository but returned HTTP 403 (`Resource not accessible by integration`) when attempting to create a safe feature branch. Therefore this package intentionally does **not** modify `fm_bemo_consolidated` remotely.
