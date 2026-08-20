import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.authenticated()) {
    return router.parseUrl('/login');
  }

  const singlePerm = route.data?.['requiredPermission'] as string | undefined;
  const multiPerms = route.data?.['requiredPermissions'] as string[] | undefined;

  const permissionsToCheck: string[] = [];
  if (singlePerm) {
    permissionsToCheck.push(singlePerm);
  }
  if (multiPerms && Array.isArray(multiPerms)) {
    permissionsToCheck.push(...multiPerms);
  }

  if (permissionsToCheck.length === 0) {
    return true;
  }

  const hasAccess = authService.hasAnyPermission(permissionsToCheck);
  if (hasAccess) {
    return true;
  }

  return router.parseUrl('/dashboard');
};
