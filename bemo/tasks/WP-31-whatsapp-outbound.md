# WP-31 — WhatsApp Business Outbound Templates (HR/AR events)
**Priority:** 🟠 · **Owner:** Backend dev D + FE · **Depends on:** — · **Effort:** ~5 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §13.5/§17C

## Business goal
Notify EMPLOYEES/CUSTOMERS on WhatsApp: payslip ready, loan installment due, leave approved, invoice overdue. CRM already receives inbound WhatsApp conversations (V314–V316) — this adds the outbound template side. BeOn-style gateway replaced by official Cloud API.

## Design decisions
- Provider interface `WhatsAppSender` behind `hr.whatsapp.provider=NONE|CLOUD_API` (token/phone-id via env). Template registration happens in Meta console (ops task documented in README); app references template NAMES via properties `hr.whatsapp.template.payslip` etc.
- Consent-first: send only to numbers with consent flag; every send logged.

## Backend steps
1. Table `whatsapp_outbound_log` (recipient_party/employee ref, template_key, params JSONB, status QUEUED|SENT|DELIVERED|FAILED|NO_CONSENT, provider_message_id, error NULL, sent_at).
2. Event hooks: payroll PAID (per employee), advance installment due (scheduler daily), leave approved, invoice overdue (AR aging trigger ≥X days) → enqueue with bilingual template params; worker sends + updates status from provider status webhook.
3. Consent management: employee/party phone record gains `whatsapp_consent bool` + source; NO_CONSENT logged, never sent.
4. Codes: `WA_*` (~5). Rate-limit + retry with backoff on 429/5xx.

## Frontend steps
1. Settings → WhatsApp: provider status card (configured? token health), template-name mapping fields, test-send dialog to own number.
2. Employees/parties: consent toggle with audit; per-event toggle matrix (which events active) — persists via settings save-all pattern.
3. Log viewer page (filter status/template/date) + resend-failed action.
4. Keys `settings.whatsapp*` (~14).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Provider NONE: events queue as NO_CONSENT-style skipped with reason, zero external calls (mock server asserts).
- [ ] AC-2 Payroll marked PAID for 3 employees → exactly 3 QUEUED → SENT with provider ids; status webhook updates DELIVERED; failure path records error and retry respects backoff (fake clock).
- [ ] AC-3 Employee without consent is never sent (log proves NO_CONSENT) even when event fires.
- [ ] AC-4 Template params render correct Arabic employee name/amount in payload snapshot test; test-send delivers to configured number in staging.
- [ ] AC-5 Duplicate event (payroll re-mark) does not double-send (dedupe key employee+event+period).
