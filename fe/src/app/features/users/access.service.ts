import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import {
  ACCESS_LEVEL_PRECEDENCE,
  AccessCatalog,
  AccessConflict,
  AccessLevel,
  AccessPage,
  AccessPreview,
  AccessRole,
  AccessValidateResult,
  EffectivePageAccess,
} from './access.models';

/**
 * Data-access layer for the role-to-page access guidance. Loads the canonical
 * catalog once and recomputes the effective-access preview locally for fast
 * feedback. The backend endpoints remain authoritative: validation always runs
 * server-side before a user is saved.
 */
@Injectable({ providedIn: 'root' })
export class AccessService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly catalog = signal<AccessCatalog | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadCatalog(): Promise<AccessCatalog | null> {
    if (this.catalog()) return this.catalog();
    this.loading.set(true);
    this.error.set(null);
    try {
      const catalog = await firstValueFrom(this.http.get<AccessCatalog>('/api/v1/access/catalog'));
      this.catalog.set(catalog);
      return catalog;
    } catch (e) {
      this.error.set('access.catalogError');
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async reloadCatalog(): Promise<AccessCatalog | null> {
    this.catalog.set(null);
    return this.loadCatalog();
  }

  roles(): AccessRole[] {
    return this.catalog()?.roles ?? [];
  }

  pages(): AccessPage[] {
    return this.catalog()?.pages ?? [];
  }

  rolePermissions(roleCodes: string[]): Set<string> {
    const catalog = this.catalog();
    if (!catalog) return new Set();
    const granted = new Set<string>();
    for (const code of roleCodes) {
      const role = catalog.roles.find((item) => item.code === code);
      if (role) role.permissions.forEach((permission) => granted.add(permission));
    }
    return granted;
  }

  /**
   * Local preview mirroring the backend algorithm. Fast feedback only; the
   * backend preview/validation is authoritative before saving.
   */
  preview(roleCodes: string[], menuCodes: string[]): AccessPreview {
    const catalog = this.catalog();
    const pages: EffectivePageAccess[] = [];
    if (!catalog) {
      return { pages: [], warnings: [], conflicts: [], sensitivePermissions: [] };
    }
    const granted = this.rolePermissions(roleCodes);
    const menus = new Set(menuCodes ?? []);
    for (const page of catalog.pages) {
      pages.push(this.effectivePage(page, roleCodes, granted, menus));
    }
    const warnings = this.sensitiveWarnings(granted, catalog.sensitivePermissions);
    const conflicts = this.evaluateConflicts(granted, roleCodes);
    return {
      pages,
      warnings,
      conflicts,
      sensitivePermissions: [...granted].filter((p) => catalog.sensitivePermissions.includes(p)).sort(),
    };
  }

  private effectivePage(
    page: AccessPage,
    roleCodes: string[],
    granted: Set<string>,
    menus: Set<string>,
  ): EffectivePageAccess {
    const viewPermission = page.viewPermissions[0];
    const viewGranted = granted.has(viewPermission);
    const grantedActions = page.actions.filter((a) => granted.has(a.permission)).map((a) => a.code);
    const catalog = this.catalog();
    const grantingRoles = roleCodes
      .filter((role) => catalog?.roles.find((r) => r.code === role)?.permissions.includes(viewPermission))
      .sort();

    const featureUnavailable =
      page.requiredFeature !== null && page.requiredFeature !== undefined &&
      !this.activeFeatures().includes(page.requiredFeature);
    const adminSelected = roleCodes.some((role) => role === 'ADMIN' || role === 'SUPER_ADMIN');
    const routeRoleDenied =
      page.roles.length > 0 && !page.roles.some((role) => roleCodes.includes(role));
    const missingPermissions = [
      ...(viewGranted ? [] : [viewPermission]),
      ...page.actions.filter((a) => !granted.has(a.permission)).map((a) => a.permission),
      ...(routeRoleDenied ? page.roles : []),
    ];
    let access: AccessLevel;
    if (featureUnavailable) {
      access = 'MODULE_UNAVAILABLE';
    } else if (adminSelected) {
      access = 'REVIEW';
    } else if (!menus.has(page.menuId)) {
      access = 'HIDDEN';
    } else if (routeRoleDenied) {
      access = 'RESTRICTED';
    } else if (!viewGranted) {
      access = 'RESTRICTED';
    } else {
      access = this.deriveLevel(grantedActions);
    }
    return {
      pageCode: page.code,
      access,
      grantedByRoles: grantingRoles,
      grantedActions: [...grantedActions].sort(),
      missingPermissions: [...missingPermissions].sort(),
    };
  }

  private activeFeatures(): string[] {
    return this.auth.user()?.activeFeatures ?? [];
  }

  private deriveLevel(grantedActions: string[]): AccessLevel {
    for (const level of ACCESS_LEVEL_PRECEDENCE) {
      if (grantedActions.includes(level)) return level;
    }
    return 'VIEW';
  }

  private sensitiveWarnings(granted: Set<string>, sensitive: string[]): AccessPreview['warnings'] {
    return [...granted]
      .filter((permission) => sensitive.includes(permission))
      .sort()
      .map((permission) => ({
        code: permission,
        messageKey: `access.warnings.${permission.replaceAll('.', '-')}`,
        permissions: [permission],
        blocking: false,
      }));
  }

  private evaluateConflicts(granted: Set<string>, roleCodes: string[]): AccessConflict[] {
    const catalog = this.catalog();
    if (!catalog) return [];
    const conflicts: AccessConflict[] = [];
    for (const rule of catalog.conflictRules) {
      if (rule.permissions.every((permission) => granted.has(permission))) {
        const affectedRoles = [
          ...new Set(
            rule.permissions.flatMap((permission) =>
              catalog.roles
                .filter((role) => role.permissions.includes(permission) && roleCodes.includes(role.code))
                .map((role) => role.code),
            ),
          ),
        ].sort();
        conflicts.push({ code: rule.code, reasonKey: rule.reasonKey, roles: affectedRoles, permissions: rule.permissions, severity: rule.severity });
      }
    }
    return conflicts;
  }

  /**
   * Greedy minimal-role suggestion covering a set of business permissions.
   * Super-admin and admin roles are never suggested.
   */
  suggestRoles(permissions: string[]): string[] {
    const catalog = this.catalog();
    if (!catalog || permissions.length === 0) return [];
    const uncovered = new Set(permissions);
    const chosen: string[] = [];
    while (uncovered.size > 0) {
      let best: AccessRole | null = null;
      let bestCover = 0;
      let bestSize = Number.POSITIVE_INFINITY;
      for (const role of catalog.roles) {
        if (role.code === 'ADMIN' || role.code === 'SUPER_ADMIN') continue;
        const cover = role.permissions.filter((permission) => uncovered.has(permission)).length;
        if (cover === 0) continue;
        if (cover > bestCover || (cover === bestCover && role.permissions.length < bestSize)) {
          bestCover = cover;
          bestSize = role.permissions.length;
          best = role;
        }
      }
      if (!best) break;
      chosen.push(best.code);
      best.permissions.forEach((permission) => uncovered.delete(permission));
    }
    return chosen;
  }

  /**
   * Broader role suggestions: roles whose permission set strictly contains the
   * suggested role's permission set (used for the "optional broader role" hint).
   */
  broaderRoles(roleCodes: string[]): string[] {
    const catalog = this.catalog();
    if (!catalog) return [];
    const suggested = roleCodes
      .map((code) => catalog.roles.find((role) => role.code === code))
      .filter((role): role is AccessRole => !!role);
    if (suggested.length === 0) return [];
    const suggestedPermissions = new Set(suggested.flatMap((role) => role.permissions));
    return catalog.roles
      .filter((role) => {
        if (role.code === 'ADMIN' || role.code === 'SUPER_ADMIN') return false;
        if (roleCodes.includes(role.code)) return false;
        return role.permissions.length > suggestedPermissions.size &&
          [...suggestedPermissions].every((permission) => role.permissions.includes(permission));
      })
      .map((role) => role.code)
      .sort();
  }

  /** Roles that grant a specific permission (for the find-by-page search). */
  rolesGranting(permission: string): string[] {
    const catalog = this.catalog();
    if (!catalog) return [];
    return catalog.roles
      .filter((role) => role.permissions.includes(permission))
      .map((role) => role.code)
      .sort();
  }

  previewRemote(roleCodes: string[], menuCodes: string[]): Promise<AccessPreview> {
    return firstValueFrom(this.http.post<AccessPreview>('/api/v1/access/preview', { roleCodes, menuCodes }));
  }

  validate(
    roleCodes: string[],
    menuCodes: string[],
    targetUserId: string | null,
    reason?: string,
  ): Promise<AccessValidateResult> {
    return firstValueFrom(
      this.http.post<AccessValidateResult>('/api/v1/users/access/validate', {
        roleCodes,
        menuCodes,
        targetUserId,
        reason,
      }),
    );
  }
}
