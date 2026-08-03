import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and set session', () => {
    service.login('TEST', 'admin', 'password').subscribe(response => {
      expect(response.accessToken).toBe('test-token');
      expect(service.authenticated()).toBe(true);
      
    });

    const req = httpTestingController.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({
      accessToken: 'test-token',
      expiresAt: Date.now() + 10000,
      mustChangePassword: false,
      app: { id: 'app1' },
      user: { id: 'user1', roles: ['ADMIN'] },
      preferences: {}
    });
  });

  it('hasMenuAccess uses activeFeatures', () => {
    // Basic test
    expect(service.hasMenuAccess('payroll')).toBe(false);
  });
});
