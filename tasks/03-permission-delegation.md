# TASK 03 — Permission Delegation
## Goal
ADMIN may grant only permissions that the ADMIN itself possesses.
`GrantablePermissions ⊆ EffectivePermissions`
## Acceptance Criteria
- [ ] ADMIN can grant owned permissions.
- [ ] ADMIN cannot grant unowned permissions.
- [ ] Cross-domain permissions are rejected.
- [ ] Forged API payloads are rejected.
- [ ] UI shows only grantable permissions.
- [ ] Permission changes are audited.
- [ ] Negative tests exist.
