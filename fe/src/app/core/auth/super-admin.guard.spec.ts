import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';
import { superAdminGuard } from './auth.guard';

describe('superAdminGuard', () => {
  function run(isSuperAdmin: boolean) {
    const forbidden = { forbidden: true };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isSuperAdmin: () => isSuperAdmin } },
        { provide: Router, useValue: { createUrlTree: () => forbidden } },
      ],
    });

    const route = {} as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => superAdminGuard(route, state));
  }

  afterEach(() => TestBed.resetTestingModule());

  it('allows SUPER_ADMIN', () => {
    expect(run(true)).toBeTrue();
  });

  it('rejects a normal tenant ADMIN', () => {
    expect(run(false) as unknown).toEqual({ forbidden: true });
  });
});
