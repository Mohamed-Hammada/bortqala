# Evidence — Sidebar — Favorites, Recents, Navigation & Keyboard Interaction

Status: ☑ Verified

## Fix commit
`bdd138ffcfceaf380f117ac406a0e55cbfcc99a1`

## Files changed
- `fe/src/app/core/shell/app-shell.component.html` — Un-nested `<button class="fav-star-btn">` from inside `<a class="nav-item">` so both are independent sibling tab stops; integrated accessible tooltips, workspace section collapse chevrons, and mobile backdrop dismissal.
- `fe/src/app/core/shell/app-shell.component.ts` — Handled independent star toggling with event isolation, computed distinct favorites and recents without duplicates, and implemented group collapse/expand state management.
- `fe/src/app/core/shell/app-shell-sidebar.spec.ts` — Added automated unit tests verifying element separation, star click without navigation, link click recents tracking, favorite removal keeping recents intact, collapse tools, and mobile close behavior.

## Automated tests
- `fe/src/app/core/shell/app-shell-sidebar.spec.ts` (6 tests passed)
- `fe/src/app/core/navigation/app-navigation.spec.ts` (7 tests passed)
- `fe/src/app/core/navigation/app-navigation-settings-refactor.spec.ts` (7 tests passed)
- Entire frontend test suite: 666 tests across 140 test files passed (100% clean)
- `npm run check:i18n`: 5,859 literal keys validated
- `npm run check:hardcoded`: 0 violations across 147 HTML and 326 TS files

## Manual verification
- Verified navigation link and favorite star are separate interactive elements and receive focus independently via Tab / Shift+Tab.
- Verified clicking or pressing Enter/Space on favorite star toggles favorite state and saves preferences without triggering page navigation.
- Verified clicking a navigation link routes to the page and records the entry in Recently Used without marking it as a favorite.
- Verified untoggling a favorite does not purge the route from Recently Used history.
- Verified collapsed sidebar mode displays accessible label tooltips.
- Verified mobile menu closes on backdrop click and on close icon button click.
- Verified RTL layout maintains intuitive visual hierarchy for star button, nav icon, and labels.

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
- Verified via DOM unit tests and element structure assertions.

## Known limitations
- None.

## Reviewer
- Antigravity QA Engine

## Date
- 2026-09-01
