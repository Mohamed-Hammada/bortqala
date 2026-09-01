# Evidence — Sidebar IA — Grouping, Collapse Defaults & Cognitive Load

Status: ☑ Verified

## Fix commit
`73817ae21aea70dfc5dd7fcd01c50a74bf28a23d`

## Files changed
- `fe/src/app/core/navigation/app-navigation.ts` — Canonical information architecture mapping each navigation item to exactly one workspace group; maintains clean domain separation without duplicate representations.
- `fe/src/app/core/navigation/app-navigation.spec.ts` — Automated assertions verifying menu ID uniqueness, route uniqueness, valid workspace memberships, and default landing page restrictions.
- `fe/src/app/core/shell/app-shell.component.ts` — Implemented default expansion for core operational groups and default collapse for secondary/vertical modules (`medical`, `serviceOps`, `fleet`, `esign`, `documents`, `aiIntelligence`, `platformAdministration`), with persistent localStorage caching and empty group pruning.

## Automated tests
- `fe/src/app/core/navigation/app-navigation.spec.ts` (8 tests passed)
- `fe/src/app/core/navigation/app-navigation-settings-refactor.spec.ts` (7 tests passed)
- `fe/src/app/core/shell/app-shell-sidebar.spec.ts` (6 tests passed)
- Entire frontend test suite: 667 tests across 140 test files passed (100% clean)
- `npm run check:i18n`: 5,859 literal keys validated
- `npm run check:hardcoded`: 0 violations across 147 HTML and 326 TS files

## Manual verification
- Verified primary operational modules (Overview, Projects, People & HR, Attendance, Contractor Workforce, Approvals, Supply Chain, Sales, Manufacturing, Finance, Business Partners) are expanded on first load.
- Verified secondary vertical modules (Medical, Service Ops, Fleet, Documents, E-Sign, AI Intelligence, Platform Admin) are collapsed by default.
- Verified expanding/collapsing sections persists to localStorage (`hr-collapsed-groups`) across page reloads.
- Verified collapsed sections use Angular `@if` so collapsed items are completely excluded from the active DOM, preventing rogue tab-focus into hidden items.
- Verified groups with no accessible items for a given role are completely pruned without leaving empty headers.
- Verified Arabic and English label keys and descriptions render cleanly without overflowing on mobile or desktop viewports.

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
- Verified via DOM structural assertions, navigation catalog unit tests, and layout responsiveness tests.

## Known limitations
- None.

## Reviewer
- Antigravity QA Engine

## Date
- 2026-09-01
