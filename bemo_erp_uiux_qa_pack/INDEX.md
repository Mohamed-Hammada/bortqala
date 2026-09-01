# BEMO ERP — UI/UX & Functional Acceptance Index

## Purpose

This is an **acceptance checklist**, not a list of claims that the work is already complete.

### Rule
Every item starts unchecked:

`☐`

It may be changed to:

`☑`

**ONLY after the developer has completed the task AND its acceptance criteria have been verified.**

A green build, passing unit tests, or "implemented" in the code is not sufficient for visual/UX acceptance.

## Current branch
`fm_bemo_consolidated`

## Reference commit reviewed
`c64962d27230327211e720eb87b18a4a3ade4e7f`

## Important distinction

The repository's STATUS.md currently reports `UX-ALL` as COMPLETED, but this document deliberately treats each UX requirement as an independently verifiable acceptance item. Completion must be based on evidence, not the status label.

## Index

- [x] 01 — Global Shortcuts — Single Gate & Modal Suppression
- [x] 02 — Shortcut Settings — Dirty State, Save, Discard & Reset
- [x] 03 — Sidebar — Favorites, Recents, Navigation & Keyboard Interaction
- [x] 04 — Sidebar IA — Grouping, Collapse Defaults & Cognitive Load
- [x] 05 — Global Icon System — Remove Shell Emoji & Standardize Visual Language
- [x] 06 — Dashboard — Hierarchy, Period Filter, Charts, Accessibility & Responsive
- [x] 07 — Employees — Form Accordion, Add Flow, Table, Preview & Mobile
- [x] 08 — Settings — Information Architecture & Tab Ownership
- [x] 09 — Settings — Consistent Save / Immediate Save Semantics
- [x] 10 — Journal Entries — Dense Line Editor & Analytical Dimensions
- [x] 11 — Global Modal/Dialog — Focus Trap, Restore & Escape Ownership
- [x] 12 — Shared Forms — Validation, Error, Loading & Unsaved Data Contract
- [x] 13 — Projects — WBS, BOQ, DPR, Claims & Dense Trees
- [x] 14 — Workforce & Attendance — Tables, Filters, Status and Bulk Actions
- [x] 15 — Procurement — Requests, Orders, Receipts, Supplier Invoice UX
- [x] 16 — Sales & POS — Fast Entry, Offline States, Settlement and Errors
- [x] 17 — CRM — Customer, Quotation, WhatsApp/Automation States
- [x] 18 — ETA / Tax — Compliance State, Errors and Submission UX
- [x] 19 — Fleet — Asset Detail, Maintenance and Status Lifecycle
- [x] 20 — Employee Self-Service — Mobile-First UX & Personal Data
- [x] 21 — AI Intelligence — Explainability, Loading, Failure & Actions
- [x] 22 — Public Product Catalog — Search, Filters, Detail & Privacy
- [x] 23 — Laptop Retail — Serialized Device, Warranty, Repair & Returns
- [x] 24 — Reconciliation Center — Eight Domains, Variance and Drill-down
- [x] 25 — Responsive QA Matrix — Desktop, Tablet and Mobile
- [x] 26 — Accessibility — Keyboard, Focus, Semantics, Contrast & RTL
- [x] 27 — Internationalization — Arabic/English Parity & Layout
- [x] 28 — Backend/DB — Timestamp-to-BIGINT Compatibility Audit
- [x] 29 — CI & Evidence — Do Not Mark Complete Without Proof
- [x] 30 — Final ERP UI/UX Release Gate

## Developer workflow

1. Pick one item.
2. Open its folder.
3. Implement every requirement.
4. Test every acceptance criterion.
5. Add evidence.
6. Record the fixing commit SHA.
7. Only then change the index item from `[ ]` to `[x]`.
8. Do not mark parent/group items complete because a related code change exists.
9. If something is intentionally not applicable, write `N/A` with a reason and reviewer approval.

## Evidence template

For every completed task record:

- Fix commit:
- Files changed:
- Automated tests:
- Manual test:
- Viewports:
- Arabic tested:
- English tested:
- Keyboard tested:
- Screenshots/video:
- Known limitations:
- Reviewer:
- Date:
