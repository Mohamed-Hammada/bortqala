import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, UrlTree } from '@angular/router';
import { permissionGuard } from './permission.guard';
import { AuthService } from './auth.service';

describe('permissionGuard', () => {
  let isAuthenticated: boolean;
  let allowedPerms: Set<string>;
  let redirectedUrl: string | null;

  beforeEach(() => {
    isAuthenticated = true;
    allowedPerms = new Set<string>();
    redirectedUrl = null;

    const mockAuth = {
      authenticated: () => isAuthenticated,
      hasAnyPermission: (perms: string[]) => perms.some((p) => allowedPerms.has(p)),
      hasPermission: (perm: string) => allowedPerms.has(perm),
    };

    const mockRouter = {
      parseUrl: (url: string) => {
        redirectedUrl = url;
        return { toString: () => url } as UrlTree;
      },
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuth },
        { provide: Router, useValue: mockRouter },
      ],
    });
  });

  it('redirects to login when unauthenticated', () => {
    isAuthenticated = false;

    const route = { data: { requiredPermission: 'finance:journal:post' } } as unknown as ActivatedRouteSnapshot;

    TestBed.runInInjectionContext(() => {
      const result = permissionGuard(route, {} as any);
      expect(redirectedUrl).toBe('/login');
    });
  });

  it('allows access when user has the required permission', () => {
    allowedPerms.add('finance:journal:post');

    const route = { data: { requiredPermission: 'finance:journal:post' } } as unknown as ActivatedRouteSnapshot;

    TestBed.runInInjectionContext(() => {
      const result = permissionGuard(route, {} as any);
      expect(result).toBe(true);
    });
  });

  it('redirects to dashboard when user lacks required permission', () => {
    const route = { data: { requiredPermission: 'finance:journal:post' } } as unknown as ActivatedRouteSnapshot;

    TestBed.runInInjectionContext(() => {
      const result = permissionGuard(route, {} as any);
      expect(redirectedUrl).toBe('/dashboard');
    });
  });
});
