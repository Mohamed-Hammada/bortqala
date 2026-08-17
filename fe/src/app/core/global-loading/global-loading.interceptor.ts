import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { GlobalLoadingService } from './global-loading.service';

const ACTION_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

/**
 * Override the global loading dialog for a specific request.
 *
 * Default:
 * - POST / PUT / PATCH / DELETE => shown
 * - GET / HEAD / OPTIONS => hidden
 *
 * Examples:
 *   context: new HttpContext().set(GLOBAL_LOADING, true)  // force for a long GET/export
 *   context: new HttpContext().set(GLOBAL_LOADING, false) // skip for background mutation
 */
export const GLOBAL_LOADING = new HttpContextToken<boolean | null>(() => null);

export const globalLoadingInterceptor: HttpInterceptorFn = (request, next) => {
  const override = request.context.get(GLOBAL_LOADING);
  const shouldShow = override ?? ACTION_METHODS.has(request.method.toUpperCase());

  if (!shouldShow) {
    return next(request);
  }

  const loading = inject(GlobalLoadingService);
  loading.begin();

  return next(request).pipe(finalize(() => loading.end()));
};
