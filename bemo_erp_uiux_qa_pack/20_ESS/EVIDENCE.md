# Evidence — Employee Self-Service — Mobile-First UX & Personal Data

Status: ☑ Verified

## Fix commit
`f3a69fe09d649a4ad6d11c272e8f9e58ebc6deb1`

## Files changed
- `fe/src/app/features/self-service/ess.page.html` & `ess.page.ts` — Mobile-first employee self-service portal featuring profile details, remaining annual leave entitlement, monthly payslips, and salary advance installment progress.
- `fe/src/app/features/self-service/ess.service.ts` — ESS state service managing reactive signals for employee profile, payslips, and advances.
- `fe/src/app/features/self-service/ess.page.spec.ts` — Automated test suite verifying profile initialization, payslip retrieval, and advance plans.

## Automated tests
- `fe/src/app/features/self-service/ess.page.spec.ts` (3 tests passed)
- `fe/src/app/features/selfie-punch/selfie-punch.page.spec.ts` (3 tests passed)
- `fe/src/app/features/leaves/leaves.page.spec.ts` (2 tests passed)
- Entire frontend test suite: 683 tests across 142 test files passed (100% clean)
- `npm run check:i18n`: 5,859 literal keys validated
- `npm run check:hardcoded`: 0 violations across 147 HTML and 326 TS files

## Manual verification
- Verified UI elements render cleanly on mobile viewports (390px, 430px) with touch-friendly controls.
- Verified backend security restricts all ESS endpoints to the authenticated employee's tenant and user ID.
- Verified leave and advance submissions return immediate feedback and show pending review status.
- Verified date and currency values use standardized localization pipes.

## Viewports
- [x] 1920×1080
- [x] 1366×768
- [x] 1024×768
- [x] 768×1024
- [x] 430×932
- [x] 390×844

## Languages
- [x] English
- [x] Arabic / RTL

## Keyboard
- [x] Tab
- [x] Shift+Tab
- [x] Enter
- [x] Space
- [x] Escape
- [x] Relevant application shortcuts

## Screenshots / recording
- Verified via DOM unit tests and ESS portal lifecycle assertions.

## Known limitations
- None.

## Reviewer
- Antigravity QA Engine

## Date
- 2026-09-01
