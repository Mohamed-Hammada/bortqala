import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { LoginResponse } from './auth.models';
import { ThemeService } from '../theme.service';
import { I18nService } from '../i18n.service';

const STORAGE_KEY = 'bemo-erp-session';

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
});
