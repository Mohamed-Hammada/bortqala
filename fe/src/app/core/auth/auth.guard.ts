import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, UrlTree } from '@angular/router';
import { RoleCode } from './auth.models';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (!authService.authenticated()) {
    const router = inject(Router);
    if (authService.sessionRestorable()) {
      authService.expireSession();
      return router.createUrlTree(['/login'], { queryParams: { reason: 'session-expired' } });
    }
    return router.createUrlTree(['/login']);
  }
  if (authService.mustChangePassword()) {
    return inject(Router).createUrlTree(['/change-password']);
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
