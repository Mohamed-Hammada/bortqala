import { RoleCode } from '../../core/auth/auth.models';

/**
 * Wire models for the role-to-page access guidance feature. These mirror the
 * backend `/api/v1/access/catalog`, `/api/v1/access/preview` and
 * `/api/v1/users/access/validate` contracts. The frontend never hardcodes a
 * role-to-page mapping; the catalog loaded from the backend is the single source
 * of truth and the backend stays authoritative before saving.
 */

export type AccessSensitivity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AccessRoleKind = 'READ_ONLY' | 'OPERATIONAL' | 'APPROVAL' | 'FINANCE' | 'ADMINISTRATION';

export type AccessLevel =
  | 'NONE'
  | 'VIEW'
  | 'CREATE'
  | 'EDIT'
  | 'MANAGE'
  | 'REVIEW'
  | 'APPROVE'
  | 'POST'
  | 'REVERSE'
  | 'RESTRICTED'
  | 'HIDDEN'
  | 'MODULE_UNAVAILABLE';

export interface AccessRole {
  code: RoleCode;
  nameKey: string;
  descriptionKey: string;
  sensitivity: AccessSensitivity;
  kind: AccessRoleKind;
  permissions: string[];
  dependencies: string[];
  sensitiveReasonKey: string | null;
}

export interface AccessAction {
  code: string;
  permission: string;
  sensitive: boolean;
}

export interface AccessPage {
  code: string;
  module: string;
  route: string;
  menuId: string;
  titleKey: string;
  viewPermissions: string[];
  /** Route-guard role matrix from the backend catalog (includes ADMIN/SUPER_ADMIN). */
  roles: string[];
  /** Tenant feature key gating this page's menu; null means not feature-gated. */
  requiredFeature: string | null;
  actions: AccessAction[];
}

export interface AccessConflictRule {
  code: string;
  permissions: string[];
  severity: 'WARNING' | 'BLOCK';
  reasonKey: string;
}

export interface AccessNeed {
  code: string;
  labelKey: string;
  permissions: string[];
}

export interface AccessCatalog {
  roles: AccessRole[];
  pages: AccessPage[];
  conflictRules: AccessConflictRule[];
  needs: AccessNeed[];
  sensitivePermissions: string[];
}

export interface EffectivePageAccess {
  pageCode: string;
  access: AccessLevel;
  grantedByRoles: string[];
  grantedActions: string[];
  missingPermissions: string[];
}

export interface AccessWarning {
  code: string;
  messageKey: string;
  permissions: string[];
  blocking: boolean;
}

export interface AccessConflict {
  code: string;
  reasonKey: string;
  roles: string[];
  permissions: string[];
  severity: 'WARNING' | 'BLOCK';
}

export interface AccessPreview {
  pages: EffectivePageAccess[];
  warnings: AccessWarning[];
  conflicts: AccessConflict[];
  sensitivePermissions: string[];
}

export interface AccessValidateError {
  code:
    | 'ACCESS_UNKNOWN_MENU'
    | 'ACCESS_MENU_ROLE_MISMATCH'
    | 'ACCESS_FEATURE_DISABLED'
    | 'ACCESS_ACK_REASON_REQUIRED';
  messageKey: string;
  menuId: string | null;
  pageCode: string | null;
}

export interface AccessValidateResult {
  valid: boolean;
  conflicts: AccessConflict[];
  warnings: AccessWarning[];
  errors: AccessValidateError[];
  sensitivePermissions: string[];
}

export const ACCESS_LEVEL_PRECEDENCE: AccessLevel[] = [
  'REVERSE',
  'POST',
  'APPROVE',
  'REVIEW',
  'MANAGE',
  'EDIT',
  'CREATE',
  'VIEW',
];

/** WP-10: server-side menu catalog entry (fallback = USER_MENU_OPTIONS constant). */
export interface MenuOption {
  id: string;
  labelKey: string;
  groupKey: string;
  verticalTags: string[];
  enabled: boolean;
}

/** WP-10: vertical job role template. */
export interface RoleTemplate {
  code: string;
  nameKey: string;
  vertical: string;
  menuIds: string[];
  permissionPrefixes: string[];
  suggestedPolicyGroupIds: string[];
  sortOrder: number;
}
