# Bemo ERP — Browser / All-Devices Logout Fix

Target repository: `Mohamed-Hammada/bortqala`  
Target branch: `fm_bemo_consolidated`  
Inspected branch head: `da9374aaf5a94a09c5ef3d97a2f03241218fbf8e`

## What this fixes

The logout action now opens two explicit choices:

1. **Sign out from this browser**
   - Revokes the current refresh session.
   - Immediately signs the same user out of **all open tabs in this browser**.
   - The cross-tab message is scoped by `userId`, so it does not intentionally target another user.

2. **Sign out from all devices**
   - Calls a protected self-service endpoint: `POST /api/v1/auth/sessions/revoke-all`.
   - Revokes every active refresh token for the signed-in user.
   - Bumps that user's token version, invalidating that user's existing access JWTs on subsequent protected requests.
   - Clears the current refresh cookie only after the server operation succeeds.
   - Same-browser tabs are immediately synchronized; other devices are forced out when they next make a protected request/refresh.

No admin permission is required to revoke your **own** sessions, and no other user's sessions are touched.

## Files modified

- `fe/src/app/core/auth/auth.service.ts`
- `fe/src/app/core/shell/app-shell.component.ts`
- `fe/src/app/core/shell/app-shell.component.html`
- `fe/src/app/core/i18n.service.ts`
- `be/src/main/java/com/bemo/hr/shared/security/AuthController.java`
- `be/src/main/java/com/bemo/hr/shared/security/AuthService.java`

No files are deleted.

## Apply

Extract this ZIP anywhere. From the repository root, run:

```bash
python /path/to/apply_logout_fix.py --check --root .
python /path/to/apply_logout_fix.py --root .
```

`--check` validates every expected source fragment first and writes nothing. The actual apply also validates all files before writing, so it will not silently partially patch a changed branch.

## Recommended verification

Test at least these flows after rebuilding frontend and backend:

- Login as User A in Tab 1 and Tab 2 of the same browser. Choose **Sign out from this browser** in Tab 1. Both tabs should go to `/login`.
- Login as User A on Browser/Device 1 and Browser/Device 2. Choose **Sign out from this browser** on Device 1. Device 2 should stay signed in.
- Login as User A on two devices and User B separately. Choose **Sign out from all devices** as User A. User A should lose all sessions; User B must remain unaffected.
- While User A is open in two tabs, trigger **Sign out from all devices** in one tab. Both same-browser tabs should move to `/login` immediately.
- Simulate a failed network call for **Sign out from all devices**. The dialog should remain available and show an error rather than falsely claiming all remote sessions were revoked.
- Verify normal login, token refresh, password change, and admin “revoke sessions” behavior still work.

## Implementation notes

The frontend does **not** put access tokens into `localStorage`. The added cross-tab event stores only a logout event containing the user ID, scope, timestamp, and event ID.

The backend already had `RefreshTokenService.revokeAllForUser(...)` and token-version validation. This patch reuses those mechanisms for a self-service all-device logout instead of creating a second session store.
