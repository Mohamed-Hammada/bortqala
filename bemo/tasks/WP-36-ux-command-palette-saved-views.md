# WP-36 — UX Pack A: Global Command Palette + Saved Grid Views
**Priority:** 🟡 · **Owner:** Frontend dev E · **Depends on:** WP-13 (dialog state) · **Effort:** ~5 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17E

## Business goal
Power users navigate and filter at keyboard speed: Ctrl+K opens a palette that searches screens, recent documents and actions; every major grid supports named saved views (filters+columns) per user, shareable per role.

## Steps
1. Palette service `core/command-palette/`: providers registry — nav items (existing items list), quick actions (new invoice, run payroll preview…), document search endpoints (invoice no, employee code, patient MRN — one aggregated backend endpoint `GET /api/v1/search?q=` with per-type caps and permission filtering).
2. UI: reuse quick-nav dialog styling; fuzzy rank (simple subsequence score), arrow navigation, Enter executes, Esc closes (through DialogState). Bind Ctrl/Cmd+K alongside existing `/`.
3. Saved views: generic `grid-view.store.ts` — persists `{pageKey, name, filters, hiddenColumns, sort}` via user preferences backend (`/api/v1/auth/preferences/grid-views`, small BE addition); view chips row above grids (default / saved / share-to-role flag); applying sets form+columns atomically.
4. Roll out to 3 highest-traffic grids first: invoices, employees, attendance explorer.
5. Keys ~20 `palette.*` / `gridViews.*` + CSV.

## Tests
Palette: search ranking fixture, action execution spy, permission-filtered results. Views: save→reload→apply round-trip via HttpTestingController; shared view visible to same-role second user.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Ctrl+K anywhere opens ≤150ms; typing "inv 2026" surfaces invoice # matches before unrelated pages; Enter navigates.
- [ ] AC-2 Search endpoint returns ONLY types the caller may read (two-role comparison test).
- [ ] AC-3 Save current filters as "Overdue July" → reload browser → apply restores identical results (URL-independent).
- [ ] AC-4 Shared view appears for colleagues with same role and NOT for others; delete removes for everyone.
- [ ] AC-5 Existing `/` quick-nav still works; both entry points coexist without handler conflicts (WP-13 regression suite green).
