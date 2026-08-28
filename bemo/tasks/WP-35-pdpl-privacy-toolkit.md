# WP-35 — Egypt PDPL Privacy Toolkit (Law 151/2020)
**Priority:** 🟡 · **Owner:** Backend dev C · **Depends on:** — · **Effort:** ~5 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17D

## Business goal
Egypt's Personal Data Protection Law: data subjects can request export or erasure of their personal data; controller must respond. Provide tenant tooling: subject-request register, one-click PII export (JSON+attachments manifest), scoped erasure/anonymization preserving financial evidence, consent registry, retention policies.

## Backend steps
1. Tables: `privacy_requests` (subject_type EMPLOYEE|PATIENT|PARTY, subject ref, kind EXPORT|ERASE|CONSENT_WITHDRAW, status RECEIVED|IN_PROGRESS|COMPLETED|REJECTED, legal_note, due_at = received+30d, decided_by) · `consent_registry` (subject ref, purpose key, granted_at, withdrawn_at NULL) · `retention_policies` (entity_key, months, action ANONYMIZE|DELETE, active).
2. Export job: gather subject PII across known entities (employees, patients, parties, users) into a JSON bundle + attachments list; deliver as download to admin only; audit the export.
3. Erase job v1 rules: anonymize identity fields (name→`<anonymized>`, national_id/phone nulled) while KEEPING financial rows (invoices, payroll, settlements — legal retention overrides); refuses hard-delete where FKs are financial evidence (`PRIVACY_ERASE_RESTRICTED` with list of retained entity types).
4. Retention runner: dry-run report first (counts per policy), execute only on explicit confirm.
5. Codes `PRIVACY_*` (~7).

## Frontend steps
1. Settings → Privacy tab: requests register with due-date badges (overdue red), new-request wizard, export-download button, erase flow with mandatory legal-note + typed confirmation showing what will be kept vs removed; consent registry viewer; retention policy editor + dry-run preview modal.
2. Keys `settings.privacy*` (~18).

## Acceptance Criteria (QA sign-off)
- [ ] AC-1 Export bundle for a fixture employee contains their PII from every covered table AND zero other subjects' data (leak test). — **NOT MET**: export bundle exceeds scaffold (no leak-tested gatherer).
- [ ] AC-2 Erase anonymizes identity columns but leaves invoice/payroll amounts and dates intact; GL trial balance unchanged post-erase (finance-invariant test). — **PARTIAL**: anonymize stub present; finance-invariant test absent.
- [ ] AC-3 Overdue request (due_at passed) flagged on register; completion requires decision note; every action audited. — **NOT MET**: `auditService` injected but never called in `PrivacyService`; overdue flag/due-badge flow not verified end-to-end.
- [ ] AC-4 Retention dry-run shows exact counts matching manual SQL count for two policies; execute without confirm impossible. — **NOT MET**: dry-run/confirm gate not implemented beyond scaffold.
- [ ] AC-5 Consent withdrawal stops WP-31 sends for that subject within one scheduler tick (integration with outbound log). — **PARTIAL**: `ConsentRegistry` exists but is NOT wired into WhatsApp sends (WP-31 same gap), so withdrawal cannot stop sends.
