# HR platform handoff

This repository intentionally contains two applications:

- `be/`: Spring Boot backend. Before changing it, read `be/skills/hr-backend/SKILL.md` completely.
- `fe/`: Angular frontend. Before changing it, read `fe/skills/hr-frontend/SKILL.md` completely.

For changes spanning both applications, define the backend API contract first, then update the typed frontend model and data-access layer. Keep business calculations in the backend; the frontend may format results but must not reimplement attendance or payroll rules.

Current phase: the end-to-end MVP is implemented and verified on PostgreSQL. It includes SaaS app-scoped JWT authentication, per-user theme/density/locale preferences, database-backed Arabic/English translations, multi-role authorization, dynamic attendance categories and schedules, custom report ranges and pay-cycle presets, biometric imports, attendance review, approval/reopen, dashboards, Excel exports, epoch-millisecond API dates, and structured tracing. Tenant-owned entities must keep `@TenantId`; mutable aggregates keep `created_at`/`updated_at`, while immutable evidence uses semantic creation/import timestamps. Read both local skills before extending it.
