import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { RoleCode } from './auth.models';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const queryParams = { ...route.queryParams };
  if (!authService.authenticated()) {
    if (authService.sessionRestorable()) {
      const refreshed = await authService.tryRefresh();
      if (refreshed && authService.authenticated()) {
        if (authService.mustChangePassword()) {
          return router.createUrlTree(['/change-password']);
        }
        return true;
      }
      authService.expireSession();
      return router.createUrlTree(['/login'], { queryParams: { ...queryParams, reason: 'session-expired' } });
    }
    return router.createUrlTree(['/login'], { queryParams });
  }
  if (authService.mustChangePassword()) {
    return router.createUrlTree(['/change-password']);
  }
  return true;
};

export const mustChangePasswordGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  if (!authService.authenticated()) return router.createUrlTree(['/login']);
  if (authService.mustChangePassword()) return true;
  return router.createUrlTree(['/dashboard']);
};

export function roleGuardDecision(allowed: boolean, router: Router): boolean | UrlTree {
  return allowed ? true : router.createUrlTree(['/forbidden']);
}

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const roles = (route.data['roles'] as RoleCode[] | undefined) ?? [];
  return roleGuardDecision(authService.hasAnyRole(roles), inject(Router));
};

export const menuAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const menuId = route.data['menuId'] as string | undefined;
  const router = inject(Router);
  if (!menuId) return roleGuardDecision(false, router);
  return roleGuardDecision(authService.hasMenuAccess(menuId), router);
};
