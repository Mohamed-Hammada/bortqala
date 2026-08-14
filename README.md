# Bemo ERP

Bemo ERP is a multi-tenant operations platform covering HR and attendance, contractor workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications, and support.

The repository contains these active applications:

- `be/` — Spring Boot backend and Liquibase database catalog.
- `fe/` — Angular web application.
- `desktop/` — Tauri desktop distribution that packages the web/backend stack with a local runtime and PostgreSQL.
- `license-app/` — license activation service used by the desktop distribution.

Implementation claims in historical notes are not authoritative. Verify behavior from source, automated tests, migrations, and exercised API/UI flows. The canonical current remediation tracker is `docs/BORTQALA_CURRENT_CODE_REVIEW_REMAINING_WORK_2026-08-13.md`; `docs/BORTQALA_REMAINING_WORK_CHECKLIST.md` is retained only as a superseded historical checklist because its overlapping IDs describe older work. Current architecture and operational evidence also live in `PROJECT_MAP.md`, `docs/TECHNICAL_GUIDE_CHECKLIST.md`, and `docs/TEST_EVIDENCE.md`. The project is not release-ready while the canonical tracker contains open or blocked P0 gates.

Current remediation status: all implementation items through `O2C-001` plus `FIN-UI-001`, `SEC-001`, and `UI-001` are verified complete. The fiscal-period page is now the consolidated Finance Reports & Close workbench, with matching backend, route, catalog, and shell permissions. `PAY-001` enforces `DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID`, row locks, expected versions, role-scoped transitions, and frozen snapshots; its PostgreSQL concurrent-payment proof remains the only open P0 verification gate.

P2 status: Java builds intentionally use the Java 21 toolchain with Java 17-compatible bytecode; frontend builds are standardized on Node 24; business-sensitive AR aging/collections require an explicit as-of date; the Nginx frontend boundary applies a restrictive CSP without `unsafe-eval`. Partial manufacturing issue/receipt is explicitly out of the current all-or-nothing production-order scope.

## Local verification

```powershell
cd be
.\gradlew.bat test -PskipDockerTests
python tools/check-error-codes.py
python tools/check-translation-catalog.py
python tools/check-authorization-contract.py

cd ..\fe
npm run check:i18n
npm run check:hardcoded
npm run test -- --watch=false
npm run build
```

Generated dependency/build folders and desktop runtime bundles are ignored and can be recreated from their lockfiles and `desktop/scripts/prepare-resources.ps1`.
