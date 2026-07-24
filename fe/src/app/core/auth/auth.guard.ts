import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { RoleCode } from './auth.models';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.authenticated()) return true;
  const router = inject(Router);
  if (authService.token()) {
    authService.expireSession();
    return router.createUrlTree(['/login'], { queryParams: { reason: 'session-expired' } });
  }
  return router.createUrlTree(['/login']);
};

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const roles = (route.data['roles'] as RoleCode[] | undefined) ?? [];
  return authService.hasAnyRole(roles) ? true : inject(Router).createUrlTree(['/dashboard']);
};
