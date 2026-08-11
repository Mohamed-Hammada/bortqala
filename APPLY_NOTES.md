# Bortqala cumulative patch — final known pending fixes

Base reviewed branch:

```text
fm_bemo_consolidated
```

Base reviewed SHA:

```text
4ea88988a8fe2ec1c1132e2a30583c7b717e34ff
```

This ZIP supersedes the earlier `bortqala_patch_4ea88988.zip`.

## Included fixes

### Existing fixes carried forward

- Users/access role-page consistency.
- Admin and Super Admin allowed-menu count consistency.
- Localized Users CSV export.
- Employee required/optional field presentation.
- Employee empty-state localization.
- Related Users tests.

### Final pending fixes added

1. **Procurement operation-specific busy states**
   - `savingPo`
   - `savingGrn`
   - `savingInvoice`
   - `savingPayment`
   - `resolvingMatch`
   - `submitting` is retained only as a read-only aggregate computed state.
   - Each form now guards and disables only its own operation.

2. **Three-way-match hardcoded UI messages removed**
   - `procurement.matchResolutionNotesRequired`
   - `procurement.matchResolvedSuccess`
   - `procurement.resolvingMatch`
   - New V126 Arabic/English translation migration included.

3. **Hardcoded UI checker now scans TypeScript**
   - Keeps HTML scanning.
   - Scans `.ts` source files, excluding specs and declarations.
   - Detects literal text passed directly to notification success/error/warning/info calls.
   - Detects hardcoded `window.alert` / `window.confirm` messages.
   - Detects visible third-argument `i18n.t(..., ..., fallback)` fallback strings.

4. **Procurement unit tests updated**
   - Tests independent operation states.
   - Tests aggregate busy state including match resolution.
   - Existing invoice validation tests updated for the new state model.

## Apply

Extract this ZIP over the repository root so the included repository-relative files replace/add their targets.

Then run one of:

### Windows PowerShell

```powershell
.\APPLY_PENDING_FIXES.ps1
```

### Linux / WSL / macOS

```bash
./APPLY_PENDING_FIXES.sh
```

### Any platform with Python

```bash
python APPLY_PENDING_FIXES.py
```

The auto-patcher modifies only these large existing files:

```text
fe/src/app/features/trade/procurement/procurement.page.ts
fe/src/app/features/trade/procurement/procurement.page.html
be/src/test/resources/db/changelog/releases/test-h2.changelog-master.yaml
```

All other changed files are already present in the ZIP at their final repository paths.

## Files added/replaced by extraction

```text
fe/src/app/features/employees/employees.page.html
fe/src/app/features/users/users.page.ts
fe/src/app/features/users/users.page.spec.ts
fe/src/app/features/trade/procurement/procurement.page.spec.ts
fe/tools/check-hardcoded-strings.mjs
be/src/main/resources/db/changelog/releases/next.changelog-master.yaml
be/src/main/resources/db/changelog/data/insert/20260807_v126_procurement_hardening_translations.yaml
be/src/main/resources/db/changelog/data/insert/files/20260807_v126_procurement_hardening_translations.csv
```

## Files modified by the auto-patcher

```text
fe/src/app/features/trade/procurement/procurement.page.ts
fe/src/app/features/trade/procurement/procurement.page.html
be/src/test/resources/db/changelog/releases/test-h2.changelog-master.yaml
```

## Files to delete

None.

See `DELETE_FILES.txt`.

## Recommended local verification

### Frontend

```bash
cd fe
npm ci
npm run check:i18n
npm run check:hardcoded-ui
npm test -- --watch=false
npm run build
```

### Backend

```bash
cd be
./gradlew clean test
```

Also run your PostgreSQL/Liquibase integration tests if they are separate from the normal Gradle test task.

## Important

This package contains source fixes, migrations, and tests. It has not been used to claim a successful full application build in this environment. Your local test/build run remains the final verification.
