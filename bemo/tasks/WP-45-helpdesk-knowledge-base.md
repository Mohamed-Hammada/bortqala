# WP-45 — Helpdesk Tickets + Knowledge Base
**Priority:** 🟢 · **Owner:** Full-stack F · **Depends on:** — · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Odoo gaps

## Business goal
Internal IT/support (or customer-facing support teams) need tickets with SLA timers and a searchable bilingual knowledge base to deflect repeats.

## Backend steps
1. Tables: `helpdesk_categories` (name translations, sla_first_response_hours default 8, sla_resolution_hours default 48) · `tickets` (ticket_no seq, requester user/party, category FK, title, description, priority LOW|NORMAL|HIGH|URGENT, status NEW|OPEN|WAITING_CUSTOMER|RESOLVED|CLOSED, assignee NULL, first_response_at NULL, resolved_at NULL, SLA breach flags derived) · `ticket_messages` (ticket FK, author, body, internal bool, attachments trio) — thread model.
2. SLA engine: on create stamp due timestamps from category+priority multiplier; cron flags breached (`sla_breach_first/solution` booleans cached); notifications to assignee on assign/SLA-warn (≤2h left).
3. KB: `kb_articles` (slug, title_key? no — real columns title_ar/en, body_ar/en markdown-lite, tags text[], published bool, views counter, helpful up/down counts); search endpoint ILIKE over both languages + tag filter; link-from-ticket action creates article draft prefilled.
4. Codes `TICKET_*`, `KB_*`.

## Frontend steps
1. `features/helpdesk/`: inbox (filters status/assignee/SLA-breached), ticket detail = message thread + internal-note toggle + status/assignee controls; my-assignments view.
2. `features/kb/`: article list/search page + editor (title/body both locales required to publish) + article view with helpful voting; help-center public-ish route behind login.
3. Keys ~26.

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 URGENT ticket SLA stamps use priority multiplier correctly (category base × matrix fixture); breach flags flip via cron within window and revert never. — **PARTIAL**: SLA due-timestamp stamps + multiplier exist; breach cron + "revert never" unverified.
- [x] AC-2 First response timestamp set by FIRST non-internal agent message only (internal notes don't count — rule test). — **MET** (non-internal first-response rule test).
- [x] AC-3 Customer-visible thread hides internal notes strictly (permission+filter test both layers). — **MET** (internal-note filter + permission tests).
- [ ] AC-4 Arabic search hits Arabic-body articles ranking exact-title matches first; publishing requires BOTH locale bodies (validation test). — **PARTIAL**: `kb.search` ILIKE/ranked + bilingual-publish validation exist (v392/v395 fixed kb.search ar); ranking-order test unverified.
- [x] AC-5 "Create article from ticket" prefills body with sanitized thread (no internal notes leaked — string scan test). — **MET** (sanitized prefill + string-scan test).
