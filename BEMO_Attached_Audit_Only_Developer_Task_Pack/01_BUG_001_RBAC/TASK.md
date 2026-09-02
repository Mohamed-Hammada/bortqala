# BUG-001 — Super Admin authorization vs sidebar visibility

Priority: **CRITICAL**

Fix the mismatch where Super Admin sees modules but receives /forbidden/403. Affected routes: /workforce/settlement-periods, /workforce/contractor-accounts, /trade/sales, /manufacturing/production, /manufacturing/quality, /payroll, /finance/accounts, /finance/journal-entries, /finance/banks, /finance/tax-currency, /fiscal-periods, /finance/budgets.

Acceptance:
- [ ] Every affected route opens as Super Admin.
- [ ] Restricted roles remain restricted.
- [ ] Sidebar visibility matches route/API authorization.
- [ ] Direct URL access matches sidebar behavior.
- [ ] Automated authorization regression tests added.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.