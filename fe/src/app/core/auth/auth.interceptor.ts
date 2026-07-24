import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.token();
  const deviceKey = 'hr-platform-device-id';
  let deviceId = localStorage.getItem(deviceKey);
  if (!deviceId) {
    deviceId = createRequestId();
    localStorage.setItem(deviceKey, deviceId);
  }
  const headers: Record<string, string> = {
    'X-Correlation-Id': createRequestId(),
    'X-Device-Id': deviceId,
  };
  const publicRequest =
    request.url.includes('/api/v1/auth/login') || request.url.includes('/api/v1/i18n/');
  if (token && !publicRequest) headers['Authorization'] = `Bearer ${token}`;
  return next(request.clone({ setHeaders: headers })).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !publicRequest &&
        authService.token()
      ) {
        authService.expireSession();
        void router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
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
