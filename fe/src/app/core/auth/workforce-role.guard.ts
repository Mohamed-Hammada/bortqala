import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { RoleCode } from './auth.models';
import { AuthService } from './auth.service';
import { roleGuardDecision } from './auth.guard';

export const WORKFORCE_BASE_ROLES: readonly RoleCode[] = [
  'WORKFORCE_MANAGER',
  'WORKFORCE_REVIEWER',
  'WORKFORCE_FINANCE',
];

export const WORKFORCE_IMPORT_ROLES: readonly RoleCode[] = [
  'WORKFORCE_MANAGER',
  'WORKFORCE_REVIEWER',
];

export const WORKFORCE_ACCOUNT_ROLES: readonly RoleCode[] = [
  'WORKFORCE_MANAGER',
  'WORKFORCE_FINANCE',
];

export const WORKFORCE_ROLES: readonly RoleCode[] = WORKFORCE_BASE_ROLES;

export const workforceRoleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const roles = (route.data['roles'] as RoleCode[] | undefined) ?? [...WORKFORCE_ROLES];
  return roleGuardDecision(authService.hasAnyRole(roles), inject(Router));
};
