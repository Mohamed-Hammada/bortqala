# GLOBAL RULES (apply to this and every task file)

## Repository map
`be/` Spring Boot 4.1 backend (Java 21, Gradle, PostgreSQL prod / H2 tests, Liquibase, JPA) · `fe/` Angular 22 (standalone, signals, SCSS, Vitest, **Node 24 only**) · `desktop/` Tauri · `license-app/` licensing · `be/modules/device-hub/` Python device gateway.

## Backend rules (from `be/skills/hr-backend/SKILL.md`)
1. Package per capability under `com.bemo.hr.<cap>`; only layers you need: `api/application/domain/infrastructure`. No empty layers.
2. Constructor injection, `final` fields. DTOs are records. NEVER expose JPA entities from controllers.
3. Tenant entities: `@TenantId private String appId;` + `@Version` when mutable.
4. JSON dates = epoch-millisecond numbers (`Instant`/`LocalDate`); times `"HH:mm"`.
5. Schema ONLY via versioned Liquibase YAML. **Next free number: V341** (V340 exists). Register in `next.changelog-master.yaml` AND `test-h2.changelog-master.yaml`.
6. Errors: `BusinessRuleException(msg, "STABLE_CODE", HttpStatus)` + translation CSV rows ar-EG/en-US (ids `v341-001-en/-ar`). Gate: `python be/tools/check-error-codes.py`.
7. `@Transactional` on application-service methods. No bidirectional JPA relations.
8. Tests: parameterized unit + focused integration. Run `./gradlew test -PskipDockerTests`.
9. Update bilingual `README.md` of every touched Java package.
10. Money/time math stays in the BACKEND. Frontend formats only.

## Frontend rules (from `fe/skills/hr-frontend/SKILL.md`)
1. Standalone components, OnPush, `inject()`, signals/computed, typed reactive forms, `@if/@for`. No NgModules. No `any`.
2. Folder shape: `features/<name>/{pages,ui,data-access,models}` + `<name>.routes.ts`, lazy route in `app.routes.ts`.
3. HTTP only in data-access services.
4. Zero hardcoded strings: `i18n.t('key', undefined, 'fallback')`; invented keys ALSO need Liquibase CSV rows both locales. Gates: `npm run check:i18n`, `npm run check:hardcoded`.
5. Epoch-millis through `core/date.ts`.
6. States before happy path: loading/empty/error/stale/permission/success.
7. Confirm destructive actions with affected counts; touch targets ≥40px; never color alone.

## New page/menu protocol (AGENTS.md 4-part)
Shell nav item + visible() gate → i18n fallbacks + CSV rows → SQL UPDATE on `app_users.allowed_menus` (idempotent) + defaults in `AppUser.java`/`AuthService.java` → `USER_MENU_OPTIONS` in `users.page.ts` (+ AccessCatalog if permissions added).

## Definition of Done (every task)
```
[ ] ./gradlew test -PskipDockerTests → 0 failures
[ ] python be/tools/check-error-codes.py && check-translation-catalog.py → green
[ ] fe: nvm use 24; npm run test -- --watch=false → 0 failures
[ ] npm run check:i18n && npm run check:hardcoded → green
[ ] npm run build → success (pre-existing SCSS budget warnings OK)
[ ] New exception codes translated ar+en; menus registered (if any); READMEs updated
```

## Git
Branch per task `feat/wp-XX-short-name`. Never stage unrelated dirty files (`start-backend-*.bat`). Blocked >1 day → STOP, write blocker in `docs/`, ask.
