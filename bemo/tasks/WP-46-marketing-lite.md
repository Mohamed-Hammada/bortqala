# WP-46 — Marketing Lite: Email/SMS Campaigns + Surveys
**Priority:** 🟢 · **Owner:** Full-stack B · **Depends on:** WP-31 (WhatsApp infra pattern), WP-40 optional · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Odoo marketing suite

## Business goal
Scoped v1 of a marketing suite: send announcement campaigns (email via SMTP, SMS via EG provider adapter, WhatsApp if WP-31 present) to segmented audiences, plus simple surveys/polls with results — no automation flows yet.

## Backend steps
1. Segments: saved audience queries over parties/employees/customers with whitelisted criteria (has email, tag, category, active, balance-range) → resolved recipient list preview with counts before send.
2. Tables: `campaigns` (name, channel EMAIL|SMS|WHATSAPP, subject/templatename, body_ar/en or template ref, segment snapshot JSONB, status DRAFT|SCHEDULED|SENDING|SENT|FAILED, scheduled_at) · `campaign_recipients` (campaign FK, target ref, status QUEUED|SENT|FAILED|BOUNCED, error) · `surveys` + `survey_questions` (type CHOICE|MULTI|RATING|TEXT, options JSONB) + `survey_responses` (respondent token anonymous option).
3. Sender worker reuses provider adapters; throttle rate per channel config; per-recipient localization by stored locale preference.
4. Consent: respects PDPL consent registry (WP-35) for marketing purpose — missing consent excluded with reason.
5. Codes `CAMPAIGN_*`, `SURVEY_*` (~8).

## Frontend steps
1. `features/marketing/`: campaign wizard (audience builder → compose bilingual → preview sample → schedule/send-now), results grid with failure reasons + export; survey builder (add questions dynamic form like WP-27 renderer) + public-ish response route by token + results charts as CSS bars.
2. Keys ~24.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Audience preview counts match resolver SQL for 3-segment fixture; recipients without consent appear in "excluded" list with reason, never sent (worker assertion). — **NOT MET**: consent registry not wired into campaign sends (same gap as WP-31/WP-35); excluded-list+reason path absent.
- [ ] AC-2 Throttled send of 250 recipients respects N/min config (fake clock batch test) and records per-recipient status. — **PARTIAL**: `campaign_recipients` per-recipient status exists; throttle-by-config + fake-clock batch test unverified.
- [ ] AC-3 Recipient with Arabic preference receives Arabic body variant (locale selection test); English gets English. — **NOT MET**: per-recipient locale selection path not verified (sender is NoOp-style).
- [ ] AC-4 Survey: one response per anonymous token (unique guard), results percentages sum 100 ± rounding note; TEXT answers exported CSV safely quoted. — **PARTIAL**: survey/survey_responses scaffold exists; token-unique guard/Csv-safe-export tests unverified.
- [ ] AC-5 Failed provider call marks campaign FAILED with actionable error; resume continues from last queued recipient (no dup sends). — **PARTIAL**: FAILED-state + error recorded; resume/continue-from-last unverified.
