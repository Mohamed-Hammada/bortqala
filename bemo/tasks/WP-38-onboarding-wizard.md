# WP-38 — Tenant Onboarding Wizard (first-run checklist)
**Priority:** 🟡 · **Owner:** Full-stack F · **Depends on:** — · **Effort:** ~4 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17E

## Business goal
New tenant lands on empty ERP and flounders. A step-by-step first-run checklist (builds on existing `BusinessVerticalSetupComponent`) walks: pick vertical → create categories → import/add employees → connect device or manual mode → opening balances → invite users — each step showing done/pending with direct action links.

## Backend steps
1. Onboarding status endpoint `GET /api/v1/onboarding/status` (reuse `product/onboarding` module): computed checklist `{key, done, hintKey}` per tenant — done = real evidence queries (categories exist? employees>0? device integration row? any journal? >1 user?). No new state stored except dismissal.
2. `POST /api/v1/onboarding/dismiss` (stores dismissed_at per user; re-showable from Help menu).
3. Evidence queries must be cheap (EXISTS limits) and permission-aware.

## Frontend steps
1. Post-login (when !dismissed && !allDone): welcome overlay listing steps with progress ring; each row = title + one-line hint + primary button deep-linking (vertical setup opens settings tab; categories → categories page; etc.). Live re-check on return focus.
2. Re-entry point: Help menu "Setup checklist".
3. Keys ~16 `onboarding.*`.

## Tests
Status endpoint truth table over seeded DB states; overlay shows only when pending & not dismissed; deep-links route correctly.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Brand-new tenant sees 6-step checklist with all pending; completing steps live-updates ticks without reload (refetch on window focus).
- [ ] AC-2 Each action button lands the admin exactly where the work happens (route assertions).
- [ ] AC-3 Dismissal persists per-user; another admin still sees it; all-done tenants never see it again automatically.
- [ ] AC-4 Status endpoint answers ≤150ms on populated DB (EXISTS-only queries proven in test log).
