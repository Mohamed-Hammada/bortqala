# Bemo ERP — Claude Code Project Instructions

## Purpose
Bemo ERP is a multi-tenant business operations platform. Treat source code, tests, Liquibase migrations, and exercised API/UI flows as authoritative over historical notes.

## Repository Map
- `be/` — Spring Boot backend, JPA/domain logic, security, Liquibase.
- `fe/` — Angular 22 standalone frontend.
- `desktop/` — Tauri desktop distribution.
- `license-app/` — desktop license activation service.
- `device-hub/` / `be/modules/device-hub/` — hardware/vendor adapter integration where present.
- `docs/` — technical guides, verification evidence, remediation tracking.
- `.agents/skills/` — existing agent skills mirrored into `.claude/skills/`.
- `.claude/` — Claude-specific skills/configuration.

## Stack
- Backend: Spring Boot 4.1, Java 21 toolchain with Java 17-compatible bytecode, PostgreSQL + H2, Liquibase.
- Frontend: Angular 22, TypeScript 6, Signals, SCSS, Arabic RTL (`ar-EG`) and English (`en-US`).
- Frontend package manager: npm 11; Node 24.
- Testing: JUnit/Spring tests, Testcontainers for PostgreSQL integration, Angular/Vitest tooling.

## Core Rules
1. Inspect before editing. Identify the owning module, controller/service/store, entity/table, permissions, translations, and tests.
2. Prefer the smallest change that solves the task. Preserve existing contracts and backward compatibility unless the task explicitly changes them.
3. Database changes must use Liquibase and include appropriate translations/data where required. Do not edit generated/build artifacts.
4. Security is enforced at both API and UI layers. Respect tenant/app/role/permission/branch/cost-center scoping.
5. Do not hardcode user-facing strings; use the existing i18n catalog.
6. For workflow/state changes, validate current state, concurrency/versioning, idempotency, authorization, and allowed actions.
7. For exports, preserve formula-injection protections and existing formats.
8. Verify touched behavior with focused tests first, then relevant broader checks.

## Context Efficiency
- Do NOT read the whole repository for a focused task.
- Load only the `.claude/rules/*.md` file(s) relevant to the task (backend, frontend, database, erp-domain, qa, security, menu-registration) plus the matching module skill (`be/skills/`, `fe/skills/`) for implementation detail — rules are short checklists, skills hold the detailed conventions.
- Start with `docs/` and module-local documentation only when relevant.
- Use `PROJECT_MAP.md` for high-level orientation, not as proof of current behavior.
- `AGENTS.md` is a historical session log only. Do not read it for routine tasks and do not treat it as a source of truth; any instruction telling you to read it in full is stale.
- For a module task, read its local source/tests/docs first and expand only when dependencies are identified.
- Avoid copying large files into prompts when a short targeted excerpt is enough.

## Verification Commands
Backend:
```powershell
cd be
.\gradlew.bat test -PskipDockerTests
python tools/check-error-codes.py
python tools/check-translation-catalog.py
python tools/check-authorization-contract.py
```

Frontend:
```powershell
cd fe
npm run check:i18n
npm run check:hardcoded
npm run test -- --watch=false
npm run build
```
Frontend tests require Node 24 exactly (`fe/.nvmrc`, `engines: ">=24.0.0 <25"`). On any other Node major (e.g. 26), Vitest's jsdom loses `localStorage`, which cascades into ~250+ false failures in `i18n.service`/`auth.service`-dependent specs — a real, previously-hit failure mode, not a regression. Run `nvm use 24` (or source `$NVM_DIR/nvm.sh` first in non-login shells) before `npm run test`. `check:i18n`, `check:hardcoded`, and `npm run build` are unaffected by Node version.

Full integration/CI validation should include PostgreSQL/Testcontainers where the affected path requires it.

## Important Current Evidence Sources
- `PROJECT_MAP.md`
- `docs/TECHNICAL_GUIDE_CHECKLIST.md`
- `docs/TEST_EVIDENCE.md`
- canonical remediation tracker referenced by `README.md`
- module-local skills under `be/skills/`, `fe/skills/`, `.claude/skills/`
- `docs/status/` and `docs/history/` hold point-in-time status trackers and one-off audits; treat them as historical, not current, unless the tracker they reference is still open

## Task Workflow
1. Clarify the requested outcome from the task itself; do not ask unnecessary questions.
2. Locate affected feature/module.
3. Inspect implementation + tests + migration/security/i18n contracts.
4. Make focused changes.
5. Run targeted verification.
6. Report changed files, tests run, failures/blockers, and any unverified assumptions.

## Never
- Do not claim an item is verified solely because a historical note says so.
- Do not weaken authorization to make a test pass.
- Do not remove tests or validation just to get green builds.
- Do not expose secrets from `.env*`, credentials, private keys, or production configuration.
