# Bemo ERP

BEMO ERP is a multi-tenant business platform covering HR/attendance, workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications and support.

## Documentation accuracy

The previous implementation checklist incorrectly presented all roadmap items as completed.

The corrected code-verified checklist is the primary handoff document:

[`BEMO_ERP_IMPLEMENTATION_READMES_fm_bemo_consolidated/IMPLEMENTATION_CHECKLIST.md`](BEMO_ERP_IMPLEMENTATION_READMES_fm_bemo_consolidated/IMPLEMENTATION_CHECKLIST.md)

Audit baseline:
- developer-fix checkpoint: `a102a92a9127ab13f862048a2586a933efa50912`
- reviewed current HEAD: `8595cfa1b600f2bb4ac39fa52d32debcca5cb2ce`

A checked item means **verified end-to-end**, not merely “class/controller/migration exists.”

Current high-level state:
- Payroll — PARTIAL
- Procurement — PARTIAL
- Sales/O2C — PARTIAL
- Inventory — PARTIAL
- Manufacturing/Quality — PARTIAL
- Finance close/reconciliation — PARTIAL
- Workforce — SOURCE PRESENT / VERIFY
- Web Push — SOURCE PRESENT / RUNTIME VERIFY
- About — PARTIAL
- Support/Feedback — SOURCE PRESENT / VERIFY
- Repository hygiene — PARTIAL

See:
- [`docs/implementation/IMPLEMENTATION_STATUS.md`](docs/implementation/IMPLEMENTATION_STATUS.md)
- [`docs/implementation/REMAINING_WORK.md`](docs/implementation/REMAINING_WORK.md)
- [`docs/README.md`](docs/README.md)

## Verification commands

```powershell
cd be
.\gradlew.bat clean test

cd ..\fe
npm run check:i18n
npm run build
```

Compilation/test success alone does not make a business workflow complete; exercise the full acceptance path and update the corrected checklist only when evidence supports `[x] VERIFIED`.
