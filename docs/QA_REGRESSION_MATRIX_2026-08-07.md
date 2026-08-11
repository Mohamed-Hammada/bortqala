# QA Regression Matrix — 2026-08-07 remediation

Run after overlaying this ZIP on `fm_bemo_consolidated`.

## Critical access/features
- SUPER_ADMIN: open every route listed in BUG-001.
- Set at least one tenant feature (for example `finance.enabled`) to `false`; log in as SUPER_ADMIN and verify the menu remains visible and the backend endpoint remains accessible.
- Repeat the same disabled-feature test as ADMIN/normal user and verify the feature is hidden/blocked according to tenant configuration.
- Restricted user: verify unauthorized routes are hidden/forbidden according to role.
- Explicitly disable a tenant feature and verify its menu item is hidden/blocked.
- `/workforce/attendance`, `/workforce/dashboard`, contractors, settlements.
- `/trade/procurement` PO → GRN → invoice → payment happy path.

## Workforce
- Create contractor for each accounting model; force a 4xx/5xx and verify modal retains input + error.
- Create category at 350 EGP / 8 hours / half-month.
- Create worker and verify 350/8 inheritance, then override rate and save.
- Verify Workforce categories do not claim Employee/Both scope.

## HR
- Create employee with `QA-EMP-0807`; reload and verify exact code remains `QA-EMP-0807`.
- Duplicate the same code and verify conflict.
- Create employee without code and verify generated category-prefixed sequence.

## Attendance report
- Resolve one row, wait for success notification, hard reload, verify decision remains.
- Repeat each decision type and check audit trail.
- Exercise bulk preview/count/confirmation.

## Reports/dashboard
- Click first-half preset: verify form is populated and no report is created until Create is pressed.
- Create half-month report; dashboard wording must say monthly report is absent rather than claiming no report exists.

## Inventory
- Verify column labels are “رمز الوحدة” and “اسم وحدة القياس”.
- Create movement without reference; list must show `MOV-*`, never `—`.
- NOTE: distinct PO/GRN/supplier-invoice fields require the separate schema/API redesign documented in the manifest.

## Accessibility/i18n
- Blank Workforce category code/name: visible inline error and `aria-invalid=true`.
- Arabic locale: no `nav.settingsHint`, `Smart AI Recommendation`, `Review Progress`, `Unresolved`, or `All Rows` raw/mixed strings in the confirmed locations.
- Keyboard test modal focus, cancel/save, icon-only actions on high-use screens.

## Deployment
- `npm run build` from `fe/`.
- Run: `node scripts/verify-production-build.mjs dist`
- Serve the production browser output behind the repository nginx config.
- Hard-reload dashboard plus at least five lazy routes; no `@fs/`, dev-server URLs, or missing chunk errors.
