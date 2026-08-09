import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { LoginResponse, MenuAccessMode } from './auth.models';
import { ThemeService } from '../theme.service';
import { I18nService } from '../i18n.service';

const STORAGE_KEY = 'bemo-erp-session';

function userSession(mode: MenuAccessMode, allowedMenus: string[] | undefined): string {
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
      roles: ['VIEWER'],
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

describe('AuthService', () => {
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

  it('should be created', () => {
    expect(TestBed.inject(AuthService)).toBeTruthy();
  });

  it('should login and set session', () => {
    const service = TestBed.inject(AuthService);
    service.login('TEST', 'admin', 'password').subscribe(response => {
      expect(response.accessToken).toBe('test-token');
      expect(service.authenticated()).toBe(true);
    });

    const req = http.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 10000,
      mustChangePassword: false,
      app: { id: 'app1', code: 'TEST', name: 'Test App' },
      user: { id: 'user1', username: 'admin', displayName: 'Admin', roles: ['ADMIN'], active: true, version: 1 },
      preferences: {},
    });
  });

  it('should demo-login and set session', () => {
    const service = TestBed.inject(AuthService);
    service.demoLogin('demo-secret').subscribe(response => {
      expect(response.accessToken).toBe('test-token');
      expect(service.authenticated()).toBe(true);
    });

    const req = http.expectOne('/api/v1/auth/demo-login');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.body).toEqual({ secret: 'demo-secret' });
    req.flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 10000,
      mustChangePassword: false,
      app: { id: 'app1', code: 'TEST', name: 'Test App' },
      user: { id: 'user1', username: 'demo_superadmin', displayName: 'Demo Super Admin', roles: ['SUPER_ADMIN'], active: true, version: 1 },
      preferences: {},
    });
  });

  it('hasMenuAccess uses activeFeatures', () => {
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('payroll')).toBe(false);
  });

  it('hasMenuAccess fails closed for SELECTED users with empty allowedMenus', () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', []));
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('reports')).toBe(false);
    expect(service.hasMenuAccess('employees')).toBe(false);
  });

  it('hasMenuAccess grants SELECTED users only their allowed menus', () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', ['reports', 'employees']));
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('reports')).toBe(true);
    expect(service.hasMenuAccess('employees')).toBe(true);
    expect(service.hasMenuAccess('parties')).toBe(false);
  });

  it('hasMenuAccess grants ALL-mode users every menu even with empty allowedMenus', () => {
    localStorage.setItem(STORAGE_KEY, userSession('ALL', []));
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('reports')).toBe(true);
    expect(service.hasMenuAccess('employees')).toBe(true);
  });

  it('hasMenuAccess treats missing allowedMenus as no access for non-admin users', () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', undefined));
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('reports')).toBe(false);
  });

  it('hasMenuAccess still honors feature toggles before the mode check', () => {
    localStorage.setItem(STORAGE_KEY, userSession('SELECTED', ['payroll']));
    const service = TestBed.inject(AuthService);
    expect(service.hasMenuAccess('payroll')).toBe(false);
    expect(service.hasMenuAccess('reports')).toBe(false);
  });

  it('tryRefresh resolves false without a stored session and skips the refresh call', async () => {
    const service = TestBed.inject(AuthService);

    await expect(service.tryRefresh()).resolves.toBe(false);
    expect(service.authenticated()).toBe(false);
  });

  it('tryRefresh restores an expired stored session with a new token', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const service = TestBed.inject(AuthService);
    expect(service.authenticated()).toBe(false);
    expect(service.sessionRestorable()).toBe(true);

    const refresh = service.tryRefresh();
    const req = http.expectOne('/api/v1/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    const newExpiry = Date.now() + 3_600_000;
    req.flush({ accessToken: 'new-token', tokenType: 'Bearer', expiresAt: newExpiry });

    await expect(refresh).resolves.toBe(true);
    expect(service.authenticated()).toBe(true);
    expect(service.token()).toBe('new-token');

    const persisted = JSON.parse(localStorage.getItem(STORAGE_KEY)!) as { expiresAt: number };
    expect(persisted.expiresAt).toBe(newExpiry);
  });

  it('tryRefresh is single-flight for concurrent callers', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const service = TestBed.inject(AuthService);

    const first = service.tryRefresh();
    const second = service.tryRefresh();

    http.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'new-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 3_600_000,
    });

    await expect(first).resolves.toBe(true);
    await expect(second).resolves.toBe(true);
    expect(service.token()).toBe('new-token');
  });

  it('tryRefresh resolves false when the refresh request fails', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const service = TestBed.inject(AuthService);

    const refresh = service.tryRefresh();
    http.expectOne('/api/v1/auth/refresh').error(new ProgressEvent('error'), { status: 401, statusText: 'Unauthorized' });

    await expect(refresh).resolves.toBe(false);
    expect(service.token()).toBe('');
    expect(service.authenticated()).toBe(false);
  });

  it('tryRefresh clears the single-flight slot so a later call can retry', async () => {
    localStorage.setItem(STORAGE_KEY, storedSession(Date.now() - 1000));
    const service = TestBed.inject(AuthService);

    const first = service.tryRefresh();
    http.expectOne('/api/v1/auth/refresh').error(new ProgressEvent('error'), { status: 401, statusText: 'Unauthorized' });
    await first;

    const retry = service.tryRefresh();
    http.expectOne('/api/v1/auth/refresh').flush({
      accessToken: 'retried-token',
      tokenType: 'Bearer',
      expiresAt: Date.now() + 3_600_000,
    });

    await expect(retry).resolves.toBe(true);
    expect(service.token()).toBe('retried-token');
  });

  it('expireSession clears both memory and persisted session', () => {
    localStorage.setItem(STORAGE_KEY, storedSession());
    const service = TestBed.inject(AuthService);
    expect(service.authenticated()).toBe(false);
    expect(service.sessionRestorable()).toBe(true);

    service.expireSession();

    expect(service.authenticated()).toBe(false);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(service.sessionRestorable()).toBe(false);
  });

  describe('logout scope', () => {
    it('logout() defaults to current-browser scope', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);
      expect(service.authenticated()).toBe(false);
      expect(service.sessionRestorable()).toBe(true);

      service.logout();

      const req = http.expectOne('/api/v1/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      req.flush(null);

      expect(service.sessionRestorable()).toBe(false);
      const broadcast = JSON.parse(localStorage.getItem('bemo-erp-logout-event')!) as { userId: string; scope: string };
      expect(broadcast.userId).toBe('user1');
      expect(broadcast.scope).toBe('CURRENT_BROWSER');
    });

    it('logoutCurrentBrowser posts, broadcasts, and clears the session', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);

      service.logoutCurrentBrowser();

      http.expectOne('/api/v1/auth/logout').flush(null);
      expect(service.sessionRestorable()).toBe(false);
      expect(service.user()).toBeNull();
    });

    it('logoutAllDevices revokes every session and clears the local one', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);

      let done = false;
      service.logoutAllDevices().subscribe({ next: () => { done = true; } });

      const req = http.expectOne('/api/v1/auth/sessions/revoke-all');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      req.flush(null);

      expect(done).toBe(true);
      expect(service.sessionRestorable()).toBe(false);
      const broadcast = JSON.parse(localStorage.getItem('bemo-erp-logout-event')!) as { scope: string };
      expect(broadcast.scope).toBe('ALL_DEVICES');
    });

    it('logoutAllDevices does not clear the session when the revoke call fails', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);

      let failed = false;
      service.logoutAllDevices().subscribe({ error: () => { failed = true; } });

      http.expectOne('/api/v1/auth/sessions/revoke-all')
        .error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

      expect(failed).toBe(true);
      expect(service.sessionRestorable()).toBe(true);
      expect(localStorage.getItem('bemo-erp-logout-event')).toBeNull();
    });
  });

  describe('cross-tab logout sync', () => {
    it('clears the session when another tab logs out the same user', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);
      expect(service.sessionRestorable()).toBe(true);

      window.dispatchEvent(new StorageEvent('storage', {
        key: 'bemo-erp-logout-event',
        newValue: JSON.stringify({ userId: 'user1', scope: 'CURRENT_BROWSER', occurredAt: Date.now() }),
      }));

      expect(service.sessionRestorable()).toBe(false);
      expect(service.user()).toBeNull();
    });

    it('ignores logout events for a different user', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);

      window.dispatchEvent(new StorageEvent('storage', {
        key: 'bemo-erp-logout-event',
        newValue: JSON.stringify({ userId: 'someone-else', scope: 'CURRENT_BROWSER', occurredAt: Date.now() }),
      }));

      expect(service.sessionRestorable()).toBe(true);
    });

    it('ignores unrelated storage events and malformed payloads', () => {
      localStorage.setItem(STORAGE_KEY, storedSession());
      const service = TestBed.inject(AuthService);

      window.dispatchEvent(new StorageEvent('storage', { key: 'unrelated-key', newValue: 'x' }));
      window.dispatchEvent(new StorageEvent('storage', { key: 'bemo-erp-logout-event', newValue: '{not-json' }));

      expect(service.sessionRestorable()).toBe(true);
    });
  });
});
