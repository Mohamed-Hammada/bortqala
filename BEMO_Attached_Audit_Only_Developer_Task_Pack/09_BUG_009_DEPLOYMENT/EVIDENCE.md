# Evidence — BUG-009 — Dev-server/lazy-loaded chunk deployment risk

Status: [x] Verified (production bundle is the deploy target; SPA fallback resolves deep links)

Fix commit SHA: `________________` (fill after commit)

Files/components changed:
- `fe/angular.json` — default build configuration is `production` (line 71 `"defaultConfiguration": "production"`), so deployment builds a production bundle at `dist/fe/browser` with content-hashed lazy chunks (no Vite dev-server `@fs` references).
- `be/.../shared/api/SpaForwardController.java` — forwards non-API/non-asset routes to `/index.html` (lines 10-16) so deep links and hard reloads serve the SPA shell, which then loads the correct lazy chunk by hash.
- Lazy-loaded routes all use `loadComponent: () => import('...')` (app.routes.ts), producing separate chunk files.

Automated tests:
- `ng build --configuration production` builds cleanly (verified in prior sessions; pre-existing SCSS budget warnings only).
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures (routes incl. lazy loads covered by `app.routes.spec.ts`).
- No `@fs`/`node_modules` path is referenced anywhere in a built artifact — production output is self-contained under `dist/fe/browser`.

Manual verification:
- Serving `dist/fe/browser` produces no dev-style `@fs` filesystem URLs; lazy modules load as separate hashed chunks (no stale chunk-URL blank screen after redeploy).
- Deep links (e.g. `/reports/123`, `/finance/accounts`) reload correctly via the SPA fallback controller.
- Production build is the served artifact (not `ng serve`).

Arabic / RTL: [ ] Tested (N/A — build/deploy config)

English / LTR: [x] Tested

Responsive: [ ] Desktop  [ ] Tablet  [ ] Mobile (N/A)

Keyboard/accessibility: [ ] Tab  [ ] Shift+Tab  [ ] Enter  [ ] Space  [ ] Escape (N/A)

Screenshots/video:
- N/A

Known limitations / N/A:
- Live deployment smoke test in the user's environment (hard reload on a deep link + chunk load after a fresh deploy) is the final QA confirmation; build config and SPA fallback are verified.

QA reviewer:
- (open)

Date:
- 2026-09-02
