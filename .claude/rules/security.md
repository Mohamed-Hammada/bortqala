# Security & Tenancy Rules

Cross-cutting invariants for authorization and multi-tenancy. This is the high-level checklist; for backend implementation detail (annotations, `TenantContext`, role list, JWT claims) see `be/skills/hr-backend/SKILL.md`.

## Invariants

1. **Tenant isolation.** Every tenant-owned entity is scoped (`@TenantId`/`appId`). Never write a native query or bulk operation without an explicit tenant predicate. A request must never be able to read or mutate another tenant's data.
2. **Authorization is a backend concern.** `@PreAuthorize`/permission checks and role/permission boundaries are enforced in the backend. UI guards, `visible()` checks, and route guards exist for UX only — never rely on them as the security boundary, and never remove or weaken a backend check to make a UI flow easier.
3. **Role/permission boundaries.** Respect the existing role/permission model (`RoleCode`, `AccessCatalog`, PBAC policies where present). Adding a feature that needs new access must add the permission through the existing catalog, not a bespoke check.
4. **Branch/cost-center scoping.** Where a module has branch or cost-center scoping, apply it consistently across queries, exports, and reports — a scoped user must never see or export cross-branch/cross-cost-center data they aren't entitled to.
5. **API and UI enforcement together.** Every authorization change must be reflected in both the API (source of truth) and the UI (guards/visibility) — see `.claude/rules/menu-registration.md` when the change affects sidebar/menu visibility.
6. **Object ownership.** Where a resource has an owner/actor, verify ownership or an explicit override permission before allowing mutation.
7. **Never bypass backend authorization** to unblock a test, a demo, or a UI flow. If a check seems wrong, fix the check's logic — do not delete or short-circuit it.

## When touching auth/permission code

- Trace the change through controller → `@PreAuthorize`/service-level check → tenant predicate, not just the UI.
- Add/extend authorization tests for the touched endpoint (positive and negative — allowed role succeeds, disallowed role is rejected).
- Run `python tools/check-authorization-contract.py` (see `CLAUDE.md` verification commands).
