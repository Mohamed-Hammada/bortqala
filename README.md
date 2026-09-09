# Bemo ERP

Bemo ERP is a multi-tenant operations platform covering HR and attendance, contractor workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications, and support.

The repository contains these active applications:

- `be/` — Spring Boot backend and Liquibase database catalog.
- `fe/` — Angular web application.
- `desktop/` — Tauri desktop distribution that packages the web/backend stack with a local runtime and PostgreSQL.
- `license-app/` — license activation service used by the desktop distribution.

Implementation claims in historical notes are not authoritative. Verify behavior from source, automated tests, migrations, and exercised API/UI flows. The canonical current remediation tracker is `docs/BORTQALA_CURRENT_CODE_REVIEW_REMAINING_WORK_2026-08-13.md`; `docs/BORTQALA_REMAINING_WORK_CHECKLIST.md` is retained only as a superseded historical checklist because its overlapping IDs describe older work. Current architecture and operational evidence also live in `PROJECT_MAP.md`, `docs/TECHNICAL_GUIDE_CHECKLIST.md`, and `docs/TEST_EVIDENCE.md`. The project is not release-ready while the canonical tracker contains open or blocked P0 gates.

Current remediation status (updated 2026-09-08): all implementation items through `O2C-001` plus `FIN-UI-001`, `SEC-001`, and `UI-001` are verified complete. The fiscal-period page is now the consolidated Finance Reports & Close workbench, with matching backend, route, catalog, and shell permissions. `PAY-001` enforces `DRAFT → CALCULATED → REVIEWED → APPROVED → POSTED → PAID`, row locks, expected versions, role-scoped transitions, SoD guards, and frozen snapshots; its PostgreSQL concurrent-payment proof (`PayrollPaymentConcurrencyTests`, 10/10 against a real `postgres:17-alpine` container) executed and passed once Docker became available in the working environment — the canonical tracker's last open P0 gate is now closed. See `docs/PRODUCTION_READINESS_AUDIT_2026-09-08.md` for the full evidence trail.

P2 status: Java builds intentionally use the Java 21 toolchain with Java 17-compatible bytecode; frontend builds are standardized on Node 24; the Nginx frontend boundary applies a restrictive CSP without `unsafe-eval`. Partial manufacturing issue/receipt is explicitly out of the current all-or-nothing production-order scope.

**Known gap (found 2026-09-06, not yet fixed):** AR aging (`GET /api/v1/parties/reports/aging`) accepts an optional `asOfDate`, but `PartyFinancialPositionService.getFinancialPosition()` always buckets against `System.currentTimeMillis()` regardless of the parameter — the as-of date has no effect on aging buckets. Do not rely on it for a historical aging cutoff until fixed. See `docs/DOCUMENTATION_IMPLEMENTATION_RECONCILIATION.md` §9.5.

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
