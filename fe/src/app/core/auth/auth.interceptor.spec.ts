import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let expireSession: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    localStorage.clear();
    expireSession = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => 'test-jwt', expireSession } },
        { provide: Router, useValue: { navigate } },
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTestingController.verify());

  it('adds JWT, a stable device id, and a new correlation id for every request', () => {
    httpClient.get('/api/first').subscribe();
    const first = httpTestingController.expectOne('/api/first');
    const firstCorrelationId = first.request.headers.get('X-Correlation-Id');
    const deviceId = first.request.headers.get('X-Device-Id');
    expect(first.request.headers.get('Authorization')).toBe('Bearer test-jwt');
    expect(firstCorrelationId).toMatch(/^[0-9a-f-]{36}$/);
    expect(deviceId).toMatch(/^[0-9a-f-]{36}$/);
    first.flush({});

    httpClient.get('/api/second').subscribe();
    const second = httpTestingController.expectOne('/api/second');
    expect(second.request.headers.get('X-Device-Id')).toBe(deviceId);
    expect(second.request.headers.get('X-Correlation-Id')).not.toBe(firstCorrelationId);
    second.flush({});
  });

  it('clears the expired session and redirects protected 401 responses to login', () => {
    httpClient.get('/api/v1/dashboard').subscribe({ error: () => undefined });

    httpTestingController
      .expectOne('/api/v1/dashboard')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(expireSession).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'session-expired' },
    });
  });

  it('keeps invalid login credentials on the login form without an expiry redirect', () => {
    httpClient
      .post('/api/v1/auth/login', { username: 'admin' })
      .subscribe({ error: () => undefined });

    const login = httpTestingController.expectOne('/api/v1/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(expireSession).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });
});
