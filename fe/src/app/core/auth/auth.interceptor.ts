import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, mergeMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { I18nService } from '../i18n.service';

const PUBLIC_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/demo-login',
  '/api/v1/auth/refresh',
  '/api/v1/auth/logout',
  '/api/v1/i18n/',
];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const i18n = inject(I18nService);
  const router = inject(Router);
  const token = authService.token();
  const deviceKey = 'bemo-erp-device-id';
  let deviceId = localStorage.getItem(deviceKey);
  if (!deviceId) {
    deviceId = createRequestId();
    localStorage.setItem(deviceKey, deviceId);
  }
  const headers: Record<string, string> = {
    'Accept-Language': i18n.locale(),
    'X-Correlation-Id': createRequestId(),
    'X-Device-Id': deviceId,
  };
  const publicRequest = PUBLIC_PATHS.some((path) => request.url.includes(path));
  if (token && !publicRequest) headers['Authorization'] = `Bearer ${token}`;
  const augmented = request.clone({ setHeaders: headers });
  return next(augmented).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !publicRequest &&
        (authService.sessionRestorable() || authService.user() !== null)
      ) {
        return from(authService.tryRefresh()).pipe(
          mergeMap((refreshed) => {
            if (refreshed && authService.token()) {
              const retryHeaders: Record<string, string> = {
                Authorization: `Bearer ${authService.token()}`,
              };
              return next(request.clone({ setHeaders: retryHeaders }));
            }
            authService.expireSession();
            void router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
            return throwError(() => error);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};

function createRequestId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();

  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0'));
  return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10).join('')}`;
}
