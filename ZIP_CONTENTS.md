# App-scoped translations update

This package contains only the files added or modified for app-scoped translations and Super Admin translation management, applied on top of:

- Branch: `fm_bemo_consolidated`
- Base commit: `54d757a700106ec513be998e8a068e0b1264b6d9`
- Base subject: `update`
- Package date: `2026-08-09`

## Included behavior

- Keep platform translations with `app_id = NULL` as the default.
- Allow one translation override per application, locale, and key.
- Return the application override first and fall back to the NULL-app default.
- Let `SUPER_ADMIN` users manage defaults and application overrides from the Arabic menu `الإعدادات ← إدارة الترجمات`.
- Restore the default by deleting only the selected application's override.
- Invalidate frontend translation caches when the app/session scope changes.
- Preserve the latest logout synchronization and Frankfurter exchange-rate changes.
- Register the new Liquibase changes after the upstream V148–V151 migrations as V152 and V153.
- Include Arabic business documentation and 25 QA scenarios.

## Verification performed

- Angular compiler (`ngc --noEmit`): passed.
- TypeScript spec compilation: passed.
- i18n key check: passed, 1846 keys in both `ar-EG` and `en-US`.
- Hardcoded UI string check: passed.
- Backend error-code translation gate: passed, 266/266.
- YAML parsing, CSV ID/key uniqueness, merge-marker scan, and `git diff --check`: passed.

The full Angular test runner could not start because the environment has Node 24.14.0 while Angular 22.0.8 requires Node 24.15.0 or newer. Backend Gradle tests could not download Gradle 9.3.1 because outbound access to `services.gradle.org` is unavailable. The source and spec type-checks passed.

No deleted files, `.git`, `node_modules`, build outputs, or unrelated upstream files are included.
