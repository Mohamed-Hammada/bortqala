# TASK 01 — SUPER_ADMIN / ADMIN Hierarchy
## Goal
Enforce `SUPER_ADMIN → ADMIN → USER`.
## Requirements
- Only SUPER_ADMIN can create/promote ADMIN.
- ADMIN can create normal users.
- ADMIN cannot create/promote ADMIN.
- No lower role can assign SUPER_ADMIN.
- Enforcement must be server-side.
## Acceptance Criteria
- [ ] SUPER_ADMIN → ADMIN succeeds.
- [ ] ADMIN → ADMIN fails.
- [ ] ADMIN → USER succeeds.
- [ ] Forged role payloads fail.
- [ ] Cross-tenant role changes fail.
- [ ] Role mutations are audited.
- [ ] Unit, integration, negative and E2E tests exist.
