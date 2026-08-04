---
name: hr-frontend
description: Continue and review the Bemo Angular frontend in fe/. Use for Arabic/English HR screens, attendance review, parties, inventory/ledgers/advances, category settings, imports, localized Excel actions, typed API integration, tests, or any Angular UI change in this repository.
---

# Continue the HR frontend

Build a calm, table-first Arabic RTL application on Angular 22 standalone APIs, strict TypeScript, signals, SCSS, and lazy feature routes. Optimize for HR users moving from Excel: visible context, bulk actions, predictable tables, and explicit review states.

## Start every task

1. Inspect `package.json`, `src/app/app.routes.ts`, global styles, and the affected feature.
2. Read the root `AGENTS.md`; read the backend skill before inventing or changing an API.
3. Define loading, empty, error, stale, permission, and success states before the happy path.
4. Implement one lazy feature slice, then run `npm test -- --watch=false` and `npm run build`.
5. Update **Current state** only when the handoff facts change.
6. Keep a bilingual Arabic/English `README.md` in every affected feature/core package and update it with the code.

## Structure

Use feature-first folders without excessive splitting:

```text
src/app/
  core/                 # shell, interceptors, environment services
  shared/ui/            # presentational components reused by real features
  features/<feature>/
    pages/              # routed smart components
    ui/                 # feature-only presentation
    data-access/        # API client and signal state
    models/             # typed API and view models
    <feature>.routes.ts
```

Do not create Angular `NgModule`s. Do not create a shared abstraction until two real consumers need it. A small feature may keep page, state, and models together; split only when responsibility becomes unclear.

## Angular rules

- Use standalone components, `ChangeDetectionStrategy.OnPush`, `inject`, signals/computed, typed reactive forms, and built-in `@if`/`@for` flow.
- Keep HTTP calls in `data-access`; components consume typed state and emit intent. Never use `any` or duplicate backend calculations.
- Use interfaces for API/table models and discriminated unions for statuses and decisions.
- Lazy-load feature routes. Put period and filters in the URL when they define the viewed report.
- Cancel obsolete requests when filters change. Expose loading, empty, error, stale, and success states.
- Provide accessible names, keyboard focus, semantic tables, sticky headers, visible focus, and touch targets of at least 40px.
- Confirm destructive or bulk decisions and show affected counts. Optimistically update only retry-safe actions.
- Test services/state and high-value workflows; assert behavior rather than implementation details.

## Product and visual rules

- Default to Arabic RTL (`lang="ar"`, `dir="rtl"`) while keeping numbers and times readable.
- Apply the cinematic design kit selectively: El Messiri-style heading hierarchy, Tajawal-style body text, warm ivory surfaces, deep ink, restrained gold, soft ambient depth, and slow purposeful motion.
- This is an operations dashboard. Do not add scroll films, video, constant animation, glassmorphism, oversized hero areas, or effects that reduce table density.
- The first viewport must show selected period, report state, unresolved count, employees, punch coverage, and next action.
- Use spreadsheet-familiar tables with sticky identity columns when useful, visible totals, filters, column control, and export scope (`all data` or `current report`).
- Separate system facts from HR decisions. Show punch/rule evidence beside editable decisions and audit details.
- Use color plus text or shape for status; never color alone. Respect `prefers-reduced-motion`.
- On small screens, move secondary columns to a detail drawer instead of squeezing attendance tables.

## Primary workflow

1. Select year and months; only periods without an approved report can start a report.
2. Review category coverage and typical arrival-time distribution.
3. Resolve holiday proposals where all active category members are absent; confirmed holidays must not be asked again.
4. Resolve no-punch and single-punch exceptions with row or bulk decisions.
5. Review daily expected versus worked time and warnings.
6. Approve the frozen report, then export all data or a selected report to Excel.

Show unresolved counts per section and prevent approval while blocking exceptions remain.

## API conventions

- Use relative `/api/v1/...` URLs through an Angular dev proxy; never hard-code hosts in features.
- Keep contract types in feature `models`; map wire values to display strings at the view edge.
- Treat backend statuses as closed unions and fail visibly on unknown values.
- Send and receive `Instant` and work-date values only as epoch-millisecond `number`s through `core/date.ts`; schedule clock times remain `HH:mm`.
- Load UI copy from `/api/v1/i18n/{locale}` through `I18nService`. New static copy needs `ar-EG` and `en-US` database rows and must react to locale/RTL changes.
- Register the initial translation load with `provideAppInitializer`; never allow the first rendered frame to expose raw keys such as `parties.eyebrow`. Run `npm run check:i18n` for every copy change.
- Apply the authenticated user's theme, density, and locale preferences through the core services. Do not store a second feature-local preference source.

## Current state

- Runtime: Angular `22.0.8`, strict standalone TypeScript, SCSS, Vitest, and Node `24.18.0` LTS.
- Shell: complete Arabic RTL role-aware navigation with an ivory/ink/gold operations design, accessible labels, responsive layout, loading/error/empty states, and no component framework.
- Authentication: SaaS app code plus username/password, HttpOnly-cookie refresh with the access token held **in memory only** (local storage/session storage persist session metadata — `StoredSession` without `accessToken`, rehydrated as an empty token + single-flight `tryRefresh()` on the first 401), forced-password-change flow, route/action role checks, Bearer interceptor with single-flight refresh-on-401 retry, protected-401 expiry redirect and translated login notice, per-request correlation id, stable browser device id, ADMIN-controlled session timeout, and `workforceRoleGuard` on all workforce routes. Domain role codes (`RoleCode`) cover the 14 backend roles and mirror backend `@PreAuthorize` sets.
- Features: dashboard, dynamic categories/rules and advance eligibility, generated category-prefixed employee codes, employee/device mapping, biometric imports, custom report ranges, translated review, business parties, inventory/signed ledgers/advances, multi-role users, top-corner settings, light/dark/system theme, database i18n initialized before rendering, SVG icons, pagination, ESC drawers, and localized Excel exports.
- Contract: typed feature models call relative `/api/v1` URLs through `proxy.conf.json`; attendance calculations remain exclusively in the backend.
- Verification: `npm run check:i18n` validates all literal frontend keys in both locales (last documented run: 1098 DB keys, ar-EG + en-US), 27 Vitest tests across 10 files pass, and the production build succeeds. API and workbook QA verified PostgreSQL data, JWT flows, Arabic/English exports and typed/formatted Excel tables. Note: no Node toolchain is available locally in the current dev environment, so `check:i18n`, `ng test`, and `npm run build` must run in CI (`.github/workflows/ci.yml` frontend job).
- Next safe extensions: replace prompt-based decisions with an accessible review drawer, add bulk decisions and table filters/column preferences, add server-backed pagination for large data sets, and add end-to-end regression tests.

Do not add a component framework, global state library, chart library, or icon package until a concrete feature justifies it. Prefer Angular/platform capabilities and product-specific SCSS first.
