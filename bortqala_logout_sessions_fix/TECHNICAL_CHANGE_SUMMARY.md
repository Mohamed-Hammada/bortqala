# Technical Change Summary

## Current browser

`AuthService.logoutCurrentBrowser()`:
- calls the existing `/api/v1/auth/logout`;
- emits a `bemo-erp-logout-event` localStorage event with the current `userId`;
- clears the local session;
- every other tab listens for that event and only clears itself when its current `userId` matches.

The app shell observes the authenticated user signal and redirects a tab to `/login` when a remote-tab logout clears that tab's session.

## All devices

`POST /api/v1/auth/sessions/revoke-all` is authenticated and calls `AuthService.revokeOwnSessions(username)`.

The backend:
- resolves the currently authenticated user in the current tenant;
- calls `user.bumpTokenVersion()`;
- calls `refreshTokenService.revokeAllForUser(...)`;
- records an audit event;
- clears the current refresh cookie.

The existing JWT authentication converter checks the `tv` claim against the database token version, so old access JWTs for this user are rejected on later protected requests.

## Why the endpoint path is `/auth/sessions/revoke-all`

The current frontend interceptor considers `/api/v1/auth/logout` a public path using substring matching. Naming the all-device endpoint under `/auth/sessions/...` avoids accidentally classifying it as the public logout endpoint and ensures the bearer token is attached.
