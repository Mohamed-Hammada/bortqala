# Backend Rules

Use this rule set for Spring Boot/backend work.

See also: `be/skills/hr-backend/SKILL.md` is the authoritative, detailed backend architecture/business-invariant spec (package layout, coding conventions, current Liquibase version, concurrency evidence) — this file is a short checklist, not a substitute for it. See `.claude/rules/security.md` for tenancy/authorization invariants and `.claude/rules/menu-registration.md` when the change adds a sidebar menu item.

## Layering
Prefer:
Controller -> Service/Application -> Domain/Repository -> Infrastructure
Keep business rules in services/domain logic, not controllers.

## Security
See `.claude/rules/security.md` for the full checklist (tenant isolation, authorization, role/permission boundaries, branch/cost-center scoping). Never rely on frontend guards as the security boundary.

## Persistence
For schema changes:
- add Liquibase migration
- consider PostgreSQL and H2 test compatibility
- preserve indexes/constraints
- consider optimistic/pessimistic locking for concurrent mutations

## Mutating APIs
Check idempotency for retryable operations and preserve operation IDs/contracts where established.
For workflow transitions, return/maintain status + version + allowed actions where the existing feature uses the shared transition contract.

## Errors
Use stable machine-readable error codes and existing `ApiError` conventions. Localize through the existing translation mechanism rather than hardcoded UI messages.

## Testing
For touched services:
- focused unit tests
- integration tests when database/locking/migration behavior matters
- PostgreSQL/Testcontainers evidence for concurrency or PostgreSQL-specific behavior
