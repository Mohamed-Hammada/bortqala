# WP-32 — Scheduled Report Delivery (email/WhatsApp cron)
**Priority:** 🟡 · **Owner:** Backend dev A · **Depends on:** WP-31 optional (WhatsApp channel) · **Effort:** ~4 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17C

## Business goal
Owner receives "Daily cash position" and "Weekly AR aging" as Excel/PDF on WhatsApp/email automatically — no one remembers to click export.

## Backend steps
1. Table `report_schedules` (id, app_id, name, report_kind enum matching existing exporters (attendance|payroll|ar_aging|cashflow|trends|custom…), params JSONB (range/preset/filters), channel EMAIL|WHATSAPP, recipients text[] or role-based resolver, cadence DAILY|WEEKLY|MONTHLY + time-of-day, active, last_run_at, last_status).
2. Runner: scheduled scan per tenant timezone → for each due schedule call the EXISTING exporter service to produce bytes → send via email sender (`hr.mail.*` SMTP props) or WP-31 WhatsApp doc-send if available; record status/error.
3. Guard rails: max rows guard reuse; failure keeps schedule active with consecutive-failure counter → auto-disable after N=5 with admin notification.
4. Codes `SCHED_*` (~5).

## Frontend steps
1. Reports page gains "⏰ Scheduled delivery" section: list + create dialog (report picker = existing report types, filter recap, channel, recipients chips, cadence builder) + run-now button (executes immediately for testing) + last-run status column with error tooltip.
2. Keys `reports.schedule*` (~16).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Daily schedule fires once per day per TZ change rule (fake clock test across DST-less Cairo); weekly honors weekday.
- [ ] AC-2 Run-now returns the same bytes the manual export endpoint produces (byte-equal assertion on fixture).
- [ ] AC-3 Email path attaches localized xlsx (headers in recipient locale preference); WhatsApp path skipped gracefully when provider NONE (status SKIPPED_CHANNEL).
- [ ] AC-4 5 consecutive failures auto-disable + single admin notification (not 5); re-enable works.
- [ ] AC-5 Deleted/changed report filters fail safe with clear error rather than empty file.
