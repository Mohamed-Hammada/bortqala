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

## Access rule
Effective access = tenant/module scope + role/permission + menu visibility +
backend `@PreAuthorize`. Menus only control what appears; actions come from roles.

## V100 translations
All UI copy for this feature (role cards, access levels, sensitivity kinds,
warnings, conflict reasons, guided-mode needs and the three new backend error
codes `ACCESS_UNKNOWN_ROLE`, `ACCESS_SELF_ROLE_MODIFICATION`,
`ACCESS_CONFLICT_BLOCKED`) ships in the V100 Liquibase bilingual CSV.
