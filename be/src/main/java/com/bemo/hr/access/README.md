# Access Guidance (دليل الصلاحيات)

**EN:** Canonical role-to-page access catalog plus effective-access preview and
authoritative assignment validation for the Add/Edit User screens. The catalog
is derived from the `@PreAuthorize` role matrices and frontend route guards, so
the UI never re-implements authorization mapping and the backend stays the
source of truth.

**AR:** دليل مركزي يربط الأدوار بالصفحات والإجراءات، مع معاينة للصلاحية الفعلية
وتحقق معتمد قبل حفظ أي مستخدم. يُشتق الدليل من مصفوفات `@PreAuthorize`
وحُرّاس المسارات في الواجهة، فلا تعيد الواجهة رسم خريطة الصلاحيات ويبقى
الخادم المرجع الوحيد.

## Key services
- `domain/AccessCatalog.java`: the single source of truth — role → permission and
  permission → page/action mappings, segregation-of-duties rules, sensitive
  permission set and guided-mode business needs.
- `domain/AccessDefs.java`: immutable catalog records (`AccessRoleDef`,
  `AccessPageDef`, `AccessActionDef`, `AccessConflictRuleDef`, `AccessNeedDef`).
- `domain/AccessEnums.java`: `AccessSensitivity`, `RoleKind`, `ConflictSeverity`
  and the ordered `AccessLevel` used by the preview.
- `application/AccessCatalogService.java`: catalog projection, effective-access
  preview, conflict evaluation and `validateAssignment(...)` (super-admin
  delegation, self-role modification guard, block/warn segregation rules).
- `api/AccessController.java`: `GET /access/catalog`, `POST /access/preview`,
  `POST /users/access/validate`.

## Policy-Based Access Control (PBAC) & Granular Matrix
- `domain/SecurityPermission.java`: 200+ fine-grained permission claims across all ERP modules (`trade`, `pos`, `crm`, `procurement`, `contracting`, `manufacturing`, `finance`, `inventory`, `hr`, `compliance`, `access`, `verticals`).
- `domain/SecurityPolicyGroup.java`: Tenant-scoped custom policy groups.
- `domain/PolicyGroupPermission.java`: Mapping of policy groups to granular permissions.
- `domain/UserPolicyAssignment.java`: Assignment of policy groups to users with optional branch (`scopeBranchId`) and cost center (`scopeCostCenterId`) scopes.
- `application/PolicyGroupService.java`: CRUD for policy groups, user assignments, and effective permission calculation.
- `application/SecurityAuthorizationEvaluator.java`: SpEL evaluation bean `@auth` supporting `@auth.hasPermission('...')`, `@auth.hasAnyPermission(...)`, `@auth.hasBranchAccess(...)`, and `@auth.hasCostCenterAccess(...)`.
- `api/AccessPolicyController.java`: Endpoints at `/api/v1/access/catalog/permissions`, `/api/v1/access/policy-groups`, `/api/v1/access/users/{userId}/policies`, and `/api/v1/access/me/permissions`.

## Access rule
Effective access = tenant/module scope + role/PBAC policies + branch/cost-center scope + backend `@PreAuthorize` / `@auth.hasPermission`. SUPER_ADMIN and ADMIN retain automatic wildcard access.

## V332-V334 migrations
Schema creation, seed of 200+ granular permissions, and bilingual translations for PBAC security catalog.
