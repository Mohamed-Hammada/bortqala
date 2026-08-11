# Audit (التدقيق)

**EN:** Immutable audit trail for every sensitive action across the platform.
Records the app/tenant scope, actor, target entity, action code, affected
record id and a free-form details snapshot. Read via the audit log API; written
through `AuditService.record(...)` from every command path.

**AR:** سجل تدقيق ثابت لكل إجراء حساس عبر المنصة. يسجل نطاق التطبيق/الشركة
والمنفّذ والكيان المستهدف ورمز الإجراء ومعرف السجل ولقطة تفاصيل حرة. يُقرأ عبر
واجهة سجلات التدقيق، وتُكتب عبر `AuditService.record(...)` من كل مسار أمر.

## Key types
- `domain/AuditLog.java`: the immutable aggregate (semantic timestamps, no mutable
  `updated_at`).
- `infrastructure/AuditLogRepository.java`: tenant-scoped persistence.
- `application/AuditService.java`: `record(code, entity, entityId, actor, details)`.
- `api/AuditLogController.java` + `AuditLogApi.java`: read endpoints.

## New in V100
User create/update now audit the full role list on create and an added/removed
role diff on update (see `AuthService.accessChangeDetails`), and the Add/Edit
User screens surface the same change comparison before saving.
