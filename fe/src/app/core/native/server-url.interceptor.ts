import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { NativeBridgeService } from './native-bridge.service';

/** WP-14: inside the Android shell, relative /api paths must target the configured company server. */
export const serverUrlInterceptor: HttpInterceptorFn = (request, next) => {
  const native = inject(NativeBridgeService);
  const base = native.apiBase();
  if (!base || !request.url.startsWith('/')) return next(request);
  return next(request.clone({ url: `${base}${request.url}` }));
};
