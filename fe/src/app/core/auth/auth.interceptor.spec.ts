import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { I18nService } from '../i18n.service';

describe('authInterceptor', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            token: vi.fn(() => 'jwt-token'),
            sessionRestorable: vi.fn(() => false),
            user: vi.fn(() => null),
          },
        },
        {
          provide: I18nService,
          useValue: { locale: vi.fn(() => 'ar-EG') },
        },
      ],
    });
  });

  function run(request: HttpRequest<unknown>): HttpRequest<unknown> | null {
    let sent: HttpRequest<unknown> | null = null;
    TestBed.runInInjectionContext(() =>
      authInterceptor(request, (next: HttpRequest<unknown>) => {
        sent = next;
        return of(new HttpResponse({ status: 200 }));
      }).subscribe(),
    );
    return sent;
  }

  it('clones requests with required correlation headers', () => {
    const req = new HttpRequest('GET', '/api/v1/employees');
    const sent = run(req);
    expect(sent?.headers.get('X-Correlation-Id')).toBeTruthy();
    expect(sent?.headers.get('X-Device-Id')).toBeTruthy();
  });

  it('sends the active app locale as Accept-Language so backend errors are localized', () => {
    const req = new HttpRequest('GET', '/api/v1/employees');
    const sent = run(req);
    expect(sent?.headers.get('Accept-Language')).toBe('ar-EG');
  });

  it('attaches the JWT to translation bundles so the backend can resolve the application override', () => {
    const req = new HttpRequest('GET', '/api/v1/i18n/ar-EG');
    const sent = run(req);
    expect(sent?.headers.get('Authorization')).toBe('Bearer jwt-token');
  });
});
