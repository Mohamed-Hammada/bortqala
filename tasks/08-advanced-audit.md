# TASK 08 — Advanced Audit
## Goal
Answer WHO did WHAT to WHICH RECORD, WHEN, under WHICH TENANT/DOMAIN, using WHICH DEVICE, with WHAT RESULT and WHAT CHANGED.
## Fields
actor, role, domain, action, entity type/id, timestamp, request/correlation ID, device, result, reason, old values, new values, metadata.
## Actions
CREATE, UPDATE, DELETE, APPROVE, REJECT, POST, REVERSE, CANCEL, PAY, RECEIVE, TRANSFER, ADJUST, IMPORT, EXPORT, LOGIN, LOGIN_FAILED, ROLE_ASSIGNED, ROLE_REMOVED, PERMISSION_GRANTED, PERMISSION_REVOKED, DEVICE_REGISTERED, DEVICE_REVOKED.
## Acceptance Criteria
- [ ] Critical operations audited.
- [ ] Exact actor/action/entity recorded.
- [ ] Sensitive old/new values recorded.
- [ ] Failed authorization audited.
- [ ] Role/permission/device changes audited.
- [ ] Secrets are redacted.
- [ ] Users cannot modify/delete audit history.
- [ ] Tenant isolation and indexed/paginated search exist.
