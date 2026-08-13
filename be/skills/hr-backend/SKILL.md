---
name: hr-backend
description: Continue and review the Bemo Spring Boot backend in be/. Use for employees, attendance, reports, business parties, inventory, signed ledgers, advances, Excel APIs, persistence, migrations, security, tests, or any Java backend change in this repository.
---

# Continue the HR backend

Build the backend as a modular monolith on Spring Boot 4.1, Java 21, Gradle, PostgreSQL, JPA, Liquibase, Bean Validation, and Actuator. Preserve calculation evidence and manual decisions so an approved report is reproducible.

## Start every task

1. Inspect `build.gradle`, `src/main/resources`, and the affected feature package.
2. Read the root `AGENTS.md`; read the frontend skill when changing an API consumed by `fe`.
3. State the business rule and acceptance cases before coding.
4. Implement one vertical slice, then run `./gradlew.bat test` on Windows or `./gradlew test` elsewhere.
5. Update **Current state** only when the handoff facts change.
6. Update the bilingual `README.md` in every affected Java package; a package change is incomplete while its Arabic/English contract is stale.

## Module boundaries

Use one top-level package per capability under `com.bemo.hr`:

- `employee`: employees, employment status, and attendance category assignment.
- `attendance`: device imports, identity matching, punches, and daily calculations.
- `calendar`: seasonal schedules, workdays, weekends, and confirmed holidays.
- `reporting`: review periods, exceptions, decisions, approval, and export.
- `payroll`: daily/monthly or half-month pay-cycle output; do not execute payments.
- `parties`: suppliers, processing/export customers, sorting traders, farms, and other configurable business contacts.
- `operations`: inventory items, immutable signed stock/money movements, derived balances, processing loss metadata, and category-controlled employee advances.
- `shared`: error envelope, clock, identifiers, and audit primitives only.

Inside a feature, add only the layers the slice needs: `api`, `application`, `domain`, `infrastructure`. Do not create empty layers. Keep domain calculations free of Spring and persistence. Use interfaces for repository/device/export boundaries and variable policies; do not create an interface for every class.

## Coding rules

- Use constructor injection and `final` fields. Name injected fields after their type in camelCase.
- Use records for API DTOs and immutable value objects. Never expose JPA entities from controllers.
- Use `Instant` for audit/import timestamps, `LocalDate` for work dates, and `LocalTime` for schedules. Convert using configurable `hr.company-zone` (default `Africa/Cairo`); never hard-code a zone in business logic.
- Serialize every `Instant` and `LocalDate` API value as an epoch-millisecond JSON number. Accept numeric epoch milliseconds only. Keep `LocalTime` as `HH:mm`.
- Every tenant-owned entity carries `@TenantId appId`. Bind `TenantContext` from the JWT before opening a transaction; never use native SQL without an explicit `app_id` predicate.
- Mutable aggregates have `created_at` and `updated_at`. Immutable punch/import evidence uses its semantic `imported_at`, `punched_at`, or parent-batch timestamp and must not imply mutability with a fake `updated_at`.
- Store raw punch imports immutably. Corrections create audited decisions and never rewrite source punches.
- Make report approval idempotent. Approved snapshots change only through an explicit reopen workflow.
- Return RFC 9457 problem details. Validate requests at the API boundary and invariants in the domain.
- Apply schema changes through versioned Liquibase YAML. Never rely on `ddl-auto` outside tests.
- Keep transactions in application services. Avoid bidirectional JPA relationships and uncontrolled lazy loading.
- Test policy combinations with parameterized unit tests and API/persistence edges with focused integration tests.

## Business invariants

- Employment is configurable, not hard-coded as roles. Initial examples: daily workers paid every 15 days, and 30-day cycles for 8-hour administrators/secretarial staff, 12-hour security, and 10-hour accountants.
- A category owns expected daily minutes, seasonal start-time rules, workweek, grace/rounding/overtime rules, pay cycle, and one-punch/no-punch policies.
- A category also owns employee-advance eligibility. Employee identifiers use the category code as a prefix and a locked per-category sequence when no suffix is supplied.
- Summer and winter schedules are effective-dated configuration; never infer seasons inside code.
- Match device users by stable device id when possible. Name matching is a review aid, never an irreversible automatic merge.
- Daily results retain first/last punch, worked and expected minutes, lateness, early leave, overtime, status, rule version, and warnings.
- A single punch proves non-blocking presence only for categories with `singlePunchCounts=true`; otherwise it remains a blocking `SINGLE_PUNCH` exception.
- No punches produce an exception. HR records `deduct`, `normal day`, `approved leave`, or a configured reason with actor, time, and note.
- If all active members of a category lack a qualifying punch on a workday, propose a holiday; never decide automatically. Persist confirmation so it is not asked again.
- Reports accept a user-selected range of up to 366 days plus an explicit calendar-month, 15-day, or 30-day pay cycle. Reject overlapping reports for the same pay cycle. Monthly/half-month presets remain convenience options; approval freezes inputs and decisions.
- Daily workers use half-month periods (1-15, 16-end). Monthly reports may aggregate both periods.

## API and export rules

- Prefix REST endpoints with `/api/v1`; use plural resources, pagination, filtering, and stable sorting.
- Return calculated facts and decision metadata together so the UI can explain statuses.
- Generate Excel on the backend from an approved or selected report snapshot. Include filters, generation time, report version, and configuration version.
- Localize every exported title/header/status to the authenticated user's locale, apply their chosen native Excel table style, preserve typed cells, and use a feature plus timestamp filename.
- Model biometric uploads as import batches with checksum, device, actor, status, row counts, and row errors. Re-importing a checksum must be safe.
- Enforce upload guards at the service boundary: max file bytes and max rows are configurable via `hr.workforce-import.max-file-bytes` / `hr.workforce-import.max-rows` (defaults 20 MB / 20 000, env `HR_WORKFORCE_IMPORT_*`); preview endpoints must be bounded (`hr.workforce-import.preview-limit`, default 100). All reverse/validate/preview work must stay bounded to the target batch's own rows, never scan unrelated data.
- Throw `BusinessRuleException` with a stable machine key (e.g. `WORKFORCE_IMPORT_*`, `EXCEL_*`); do not hard-code Arabic message strings. `ApiExceptionHandler` resolves the key through the DB translation tables (falling back to the constructor message), so new keys must ship with a Liquibase V+ translation CSV/loadData changeset for ar-EG and en-US. Static messages always get a code; only messages that embed runtime values (string concatenation such as balances, ids, counts) stay single-arg so their dynamic fallback survives — never give a concatenated message a translation key, because a DB row would replace the dynamic value.
- `NotFoundException` supports an optional `(message, code)` constructor; the handler resolves the code the same way as business rules. Every backend exception code must exist exactly once per locale (`uq_translations_key_locale`); a new key colliding with an existing CSV row must be re-used or skipped, never inserted twice.

## Current state

- Runtime: Spring Boot `4.1.0`, Gradle `9.3.1`, a Java 21 toolchain with Java 17 bytecode compatibility (`options.release = 17`), and Temurin Java 21 Docker images. PostgreSQL is used for production and H2 only for dev/tests. Avoid Java 21+ collection APIs (`getFirst()`/`getLast()`) in main and test sources.
- Persistence: Liquibase creates SaaS apps/users/preferences, global translation rows, categories/schedules, employees/code sequences, holidays, immutable imports/punches/errors, reports, business parties, inventory, signed ledgers, advances, daily snapshots, and proposals. PostgreSQL 18.4 startup and Hibernate schema validation were exercised locally. V89 adds `version` to `workforce_import_batches`; V90 adds `device_id` to `punch_records` plus a unique `(app_id, device_id, device_user_id, punched_at)` index; V91 loads ar-EG/en-US rows for every backend exception code.
- Concurrency: workforce import commit takes a pessimistic write lock (`findByIdForUpdate`) on the batch so a repeated `operationId` from two concurrent requests yields exactly one applied import and one replay; punch dedup relies on the V90 unique index so concurrent device syncs store each device user/time pair once. PostgreSQL concurrency is covered by Testcontainers-only suites (`@PostgresIntegrationTest`, `@RepeatedTest`) that cannot run without Docker.
- Security: login requires app code, username, and password. HS256 JWTs carry `appId`/`appCode`; tenant discrimination is automatic for JPA entities. Users hold one or more of `ADMIN`, `HR_MANAGER`, `HR_REVIEWER`, `VIEWER`. Each app has an ADMIN-controlled 5–10,080 minute timeout used for newly issued tokens.
- Observability: every response contains client and server correlation headers. Logstash JSON records correlation ids, IP, browser device id, JWT user id/name/roles, method/path/status/duration, and bounded user-agent without tokens, bodies, passwords, or queries.
- APIs: category/schedule and employee CRUD, biometric CSV/XLS/XLSX and ZKTeco MDB/ACCDB imports with unmatched identities, dashboard, preset discovery, custom-range reports, business parties, inventory/ledger/advance operations, user preferences, public translation bundles, multi-user management, and localized Excel exports.
- Calculation: effective schedule minutes, configurable workdays/grace/one-punch policy, manual versus biometric modes, first/last punch, worked/late/early/overtime, persisted decisions, confirmed holidays, and frozen approval snapshots.
- DEMO bootstrap: reference categories, seasonal rules, employees, attendance edge cases, business parties, inventory movements, partner balances, and an eligible advance are created idempotently; existing codes are never overwritten.

- Inventory control now includes FIFO/weighted-average valuation and GL posting, per-item reorder thresholds, a derived replenishment queue, and idempotent cycle counts that preserve system/count evidence and post valued variance movements.
- Warehouse inventory now receives accepted procurement quantities into the selected warehouse and supports tenant-unique, auditable draft/ship/receive/cancel transfers with positive lines, stock validation, idempotent transitions, and source/target balance movement. Inventory, lot/serial, production-material, and quality endpoints use the real domain roles; the obsolete nonexistent `OPERATIONS_MANAGER` authority is not used.
- Next safe extensions must be selected from verified source gaps and failing acceptance tests; roadmap/README status labels are historical hints only and are not implementation evidence.

Do not add payment execution or a vendor-specific device SDK until the user selects those integrations. Do not invent packing/carton or payroll formulas from partially legible notes; signed ledgers preserve the known facts while later policies remain configurable. Design adapters so integrations do not change calculation rules.
