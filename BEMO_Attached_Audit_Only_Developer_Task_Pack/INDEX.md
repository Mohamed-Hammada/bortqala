# BEMO ERP — ATTACHED AUDIT ONLY — ACCEPTANCE INDEX

Source: the attached Senior QA Engineering End-to-End Browser Audit Report dated September 1, 2026.

IMPORTANT: This package uses ONLY that attachment. It does not merge the GitHub review, other audits, or other documents.

Completion rule: every item starts `[ ]`. Change to `[x]` ONLY after implementation AND acceptance verification. A green build, passing tests, or 'implemented' is not enough by itself.

## INDEX

- [ ] 01 — BUG-001 — Super Admin authorization vs sidebar visibility
- [ ] 02 — BUG-002 — Contractor creation fails silently
- [ ] 03 — BUG-003 — Workforce attendance/dashboard disabled
- [ ] 04 — BUG-004 — Procurement exposed but disabled
- [ ] 05 — BUG-005 — Manual attendance decision does not persist
- [ ] 06 — BUG-006 — Employee code duplicated on create
- [x] 07 — BUG-007 — BOTH workforce category is not unified
- [ ] 08 — BUG-008 — Worker daily rate does not inherit from category
- [ ] 09 — BUG-009 — Dev-server/lazy-loaded chunk deployment risk
- [x] 10 — BUG-010 — Inventory movement lacks distinct document references
- [x] 11 — BUG-011 — Duplicate inventory unit headers
- [x] 12 — BUG-012 — Save feedback is too generic
- [x] 13 — BUG-013 — Required-field validation lacks visible explanation
- [x] 14 — BUG-014 — Raw translation key visible
- [x] 15 — BUG-015 — Mixed Arabic/English terminology
- [x] 16 — BUG-016 — Report shortcut action is ambiguous
- [x] 17 — BUG-017 — Dashboard report-cycle wording is misleading
- [x] 18 — BUG-018 — Missing accessibility names
- [x] 19 — BUG-019 — Contractor failure lacks recovery guidance
- [x] 20 — BUG-020 — Workforce category form/table rule fields inconsistent
- [ ] 21 — BUG-021 — User-role configuration is cognitively overloaded
- [ ] 22 — BUG-022 — Role metadata disagrees with live access
- [ ] 23 — PAGE — Dashboard
- [ ] 24 — PAGE — Employees
- [ ] 25 — PAGE — Imports / Device Integrations
- [ ] 26 — PAGE — Parties / Partner Risk / Operations
- [ ] 27 — PAGE — Reports
- [ ] 28 — PAGE — Performance / KPI
- [ ] 29 — PAGE — Smart Import
- [ ] 30 — PAGE — Commercial / POS / CRM / Payroll
- [ ] 31 — GLOBAL — Accessibility / Responsive
- [ ] 32 — FINAL — Attached Audit Acceptance Gate

## Evidence required for every completed item
- Fix commit SHA
- Files/components changed
- Automated test result
- Manual verification result
- Arabic/RTL where applicable
- English/LTR where applicable
- Responsive verification where applicable
- Screenshots/video where useful
- Known limitations/N/A reason
- QA reviewer and date
