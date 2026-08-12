# Bemo ERP

Bemo ERP is a multi-tenant operations platform covering HR and attendance, contractor workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications, and support.

The repository contains these active applications:

- `be/` — Spring Boot backend and Liquibase database catalog.
- `fe/` — Angular web application.
- `desktop/` — Tauri desktop distribution that packages the web/backend stack with a local runtime and PostgreSQL.
- `license-app/` — license activation service used by the desktop distribution.

Implementation claims in historical notes are not authoritative. Verify behavior from source, automated tests, migrations, and exercised API/UI flows. Current architecture and operational evidence live in `PROJECT_MAP.md`, `docs/TECHNICAL_GUIDE_CHECKLIST.md`, and `docs/TEST_EVIDENCE.md`.

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
