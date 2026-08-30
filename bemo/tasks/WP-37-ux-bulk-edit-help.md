# WP-37 — UX Pack B: Inline Bulk Edit + Contextual Help
**Priority:** 🟡 · **Owner:** Frontend dev F · **Depends on:** — · **Effort:** ~4 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17E/F

## Business goal
Excel-habit users: select N grid rows → change one field (category, status, branch…) for all at once with preview and undo-safe confirm; plus per-screen help tooltips linking bilingual micro-guides.

## Steps
1. Reusable `bulk-edit` directive/component: checkbox column opt-in per grid; floating action bar "N selected · Change <field>" for fields the page declares editable-bulk; confirm dialog shows before/after counts per value (e.g., 12 rows: Admin→Security 7, Viewer→5) then calls page's bulk endpoint.
2. Backend pattern: pages expose `POST …/bulk-update {ids[], field, value}` only where safe (employees category/status first); row-level permission re-check server-side; partial failure returns per-id results (`bulk_results` envelope) — UI shows succeeded/failed breakdown. Idempotent by nature (set-based).
3. Contextual help: `help` icon component reading `help.<pageKey>` translation key (long-form text from DB translations) + link "more" to docs anchor; add keys progressively (start: reports, payroll, procurement).
4. Keys ~14 `common.bulkEdit*`, `help.*`.

## Tests
Bulk endpoint permission matrix; partial-failure envelope rendering; help drawer content loads from i18n service.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Selecting 10 employees → change category shows exact before/after distribution; cancel changes nothing (no request fired).
- [x] AC-2 Bulk call with one forbidden id: succeeds 9, fails 1 — user sees which and why; no silent partial state.
- [x] AC-3 Server re-authorizes EVERY id (lower-privileged caller cannot smuggle a forbidden id inside ids[] — security test).
- [x] AC-4 Help icon renders Arabic guide on ar locale from DB; missing key falls back gracefully without raw key text.
- [x] AC-5 Grids without bulk declaration show no checkboxes (zero regression on untouched pages).

## Deliverables Summary
- **Backend Services**: Bulk update handlers, partial failure envelopes, per-ID authorization verification, and DB translation help guides.
- **Frontend Architecture**: `BulkEditComponent` (`fe/src/app/shared/ui/bulk-edit/`), floating bulk action bar, before/after distribution preview dialog, and contextual help tooltips.

