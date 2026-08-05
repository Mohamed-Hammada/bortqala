import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard, menuAccessGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { LoginResponse, MenuAccessMode } from './auth.models';
import { ThemeService } from '../theme.service';
import { I18nService } from '../i18n.service';

const STORAGE_KEY = 'bemo-erp-session';

function userSession(mode: MenuAccessMode, allowedMenus: string[] | undefined, roles: string[]): string {
  const session: LoginResponse = {
    accessToken: 'stored-token',
    tokenType: 'Bearer',
    expiresAt: Date.now() + 60_000,
    mustChangePassword: false,
    app: { id: 'app1', code: 'TEST', name: 'Test App' },
    user: {
      id: 'user2',
      username: 'viewer',
      displayName: 'Viewer',
      roles: roles as LoginResponse['user']['roles'],
      allowedMenus,
      menuAccessMode: mode,
      active: true,
      version: 1,
    },
    preferences: {} as LoginResponse['preferences'],
  };
  return JSON.stringify(session);
}

function storedSession(expiresAt = Date.now() + 60_000, mustChangePassword = false): string {
  const session: LoginResponse = {
    accessToken: 'stored-token',
    tokenType: 'Bearer',
    expiresAt,
    mustChangePassword,
    app: { id: 'app1', code: 'TEST', name: 'Test App' },
    user: {
      id: 'user1',
      username: 'admin',
      displayName: 'Admin',
      roles: ['ADMIN'],
      active: true,
      version: 1,
    },
    preferences: {} as LoginResponse['preferences'],
  };
  return JSON.stringify(session);
}

function runGuard() {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;
  return TestBed.runInInjectionContext(() => authGuard(route, state));
}

function treeUrl(decision: unknown): string | null {
  return decision instanceof UrlTree ? decision.toString() : null;
}

describe('authGuard', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: ThemeService, useValue: { apply: () => undefined } },
        { provide: I18nService, useValue: { use: () => Promise.resolve() } },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('allows navigation for an already authenticated session', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession());
    const decisionPromise = runGuard();
    http.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'fresh-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });

    await expect(decisionPromise).resolves.toBe(true);
  });

  it('redirects to login when no restorable session exists', async () => {
    const decision = await runGuard();

    expect(treeUrl(decision)).toBe('/login');
  });

  it('preserves query parameters when redirecting to login', async () => {
    const route = { queryParams: { my_secret: 'abc123' } } as unknown as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    const decision = await TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(treeUrl(decision)).toBe('/login?my_secret=abc123');
  });

  it('refreshes an expired session and allows navigation on success', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const decisionPromise = runGuard();
    http.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'fresh-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });

    await expect(decisionPromise).resolves.toBe(true);
  });

  it('redirects to session-expired login when the refresh fails', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const decisionPromise = runGuard();
    http.expectOne('/api/v1/auth/refresh').error(new ProgressEvent('error'), { status: 401, statusText: 'Unauthorized' });

    const decision = await decisionPromise;
    expect(treeUrl(decision)).toBe('/login?reason=session-expired');
  });

  it('redirects to change-password when the session requires it', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() + 60_000, true));
    const decisionPromise = runGuard();
    http.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'fresh-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 60_000,
    });

    const decision = await decisionPromise;
    expect(treeUrl(decision)).toBe('/change-password');
  });
});

describe('menuAccessGuard', () => {
  let http: HttpTestingController;

  function runMenuGuard(menuId?: string) {
    const route = { data: menuId === undefined ? {} : { menuId } } as unknown as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => menuAccessGuard(route, state));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: ThemeService, useValue: { apply: () => undefined } },
        { provide: I18nService, useValue: { use: () => Promise.resolve() } },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('allows navigation when the user has the required menu', async () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', ['reports', 'payroll'], ['VIEWER']));
    expect(await runMenuGuard('reports')).toBe(true);
  });

  it('denies navigation when the user lacks the menu', async () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', ['reports'], ['VIEWER']));
    const decision = await runMenuGuard('payroll');
    expect(treeUrl(decision)).toBe('/forbidden');
  });

  it('denies navigation for SELECTED users with no allowed menus', async () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', [], ['VIEWER']));
    const decision = await runMenuGuard('reports');
    expect(treeUrl(decision)).toBe('/forbidden');
  });

  it('allows navigation for ALL-mode users even without an explicit menu list', async () => {
    localStorage.setItem(STORAGE_KEY, userSession('ALL', [], ['VIEWER']));
    expect(await runMenuGuard('employees')).toBe(true);
  });

  it('fails closed when the route does not declare a menuId', async () => {
    localStorage.setItem(STORAGE_KEY, userSession('ALL', [], ['VIEWER']));
    const decision = await runMenuGuard();
    expect(treeUrl(decision)).toBe('/forbidden');
  });
});
