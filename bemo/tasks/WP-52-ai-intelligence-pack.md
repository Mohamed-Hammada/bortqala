# WP-52 — AI Intelligence Pack (forecast / anomaly / NL-Q&A) — PHASE-GATED
**Priority:** 🟢 · **Owner:** Backend A + dedicated spike time · **Depends on:** WP-40 (data access patterns), real data volume · **Effort:** 3 phases × ~1 wk
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17A

## Business goal
Decision-support layer: cash-flow forecast, inventory demand forecast, expense anomaly flags, collections risk scoring, and ask-questions-in-Arabic over tenant data. Ship as ASSISTANT EVIDENCE, never auto-actions.

## Phase gating (do not skip)
- **Phase 0 gate:** ≥6 months real transaction data in a staging tenant; otherwise stop and report — forecasts on thin data are worse than none.

### Phase 1 — Deterministic "AI-lite" (no ML deps, ship value fast)
1. Cash-flow forecast: linear/seasonal-naive projection from 12-month inflow/outflow buckets + known upcoming (loan installments, payroll dates) → `GET /api/v1/analytics/cashflow-forecast?months=3` with per-bucket confidence band (min/max of history).
2. Expense anomaly: z-score vs same-vendor 6-month rolling mean; flag >2.5σ into NotificationCenter (`ANOMALY_EXPENSE_*`) with evidence numbers.
3. Demand hint: per item monthly-avg consumption × lead-time buffer → suggested reorder quantity alongside existing reorder-point alerts.

### Phase 2 — Collections scoring
4. Customer payment-behavior score (avg days-late trend, dispute count) → A/B/C bands on party financial position card; collection-priority queue for AR team.

### Phase 3 — NL Q&A over curated datasets
5. Reuse WP-41 dataset registry: LLM adapter (config provider, env key) translates Arabic/English question → dataset query JSON → VALIDATED against descriptor whitelist (reject anything outside) → executes → answers with table + cited filters. Hard rules: read-only datasets only, row caps, full audit of every question+generated query, feature flag per tenant, OFF by default.

## Frontend steps
Forecast card on finance dashboard with band chart (CSS), anomaly feed integration, party score badges, chat-style Q&A panel (flag-gated).

## Acceptance Criteria (QA sign-off)
- [ ] AC-P1 Forecast on synthetic seasonality fixture tracks trend within stated band; band widens correctly as history shrinks (<3 months → feature returns NOT_ENOUGH_DATA translated).
- [ ] AC-P2 Anomaly test: vendor invoice 3× mean flagged once (dedupe by vendor+month); normal variance never flagged across 100-row fuzz.
- [ ] AC-P3 Score bands match hand-computed fixture; explanation text lists the exact factors used (explainability requirement).
- [ ] AC-P4 NL-QA: question about sales maps ONLY to whitelisted dataset fields (hostile prompt asking for other tenants' data returns refusal + nothing executed — security suite); every Q&A pair audited; flag-off = endpoint 404.
- [ ] AC-P5 Zero writes anywhere in pack (read-only proof: DB diff empty after full exercise).
