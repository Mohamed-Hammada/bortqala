import '@angular/compiler';
import { HttpContext, HttpEvent, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  GLOBAL_LOADING,
  globalLoadingInterceptor,
} from './global-loading.interceptor';
import { GlobalLoadingService } from './global-loading.service';

describe('globalLoadingInterceptor', () => {
  const loading = {
    begin: vi.fn(),
    end: vi.fn(),
  };

  beforeEach(() => {
    loading.begin.mockReset();
    loading.end.mockReset();

    TestBed.configureTestingModule({
      providers: [{ provide: GlobalLoadingService, useValue: loading }],
    });
  });

  function execute(
    request: HttpRequest<unknown>,
    response: Observable<HttpEvent<unknown>> = of(new HttpResponse({ status: 200 })),
  ): void {
    TestBed.runInInjectionContext(() => {
      globalLoadingInterceptor(request, () => response).subscribe({
        error: () => undefined,
      });
    });
  }

  it('tracks mutating requests by default', () => {
    execute(new HttpRequest('POST', '/api/v1/example', {}));

    expect(loading.begin).toHaveBeenCalledTimes(1);
    expect(loading.end).toHaveBeenCalledTimes(1);
  });

  it('does not track GET requests by default', () => {
    execute(new HttpRequest('GET', '/api/v1/system/status'));

    expect(loading.begin).not.toHaveBeenCalled();
    expect(loading.end).not.toHaveBeenCalled();
  });

  it('clears the loader when the backend request fails', () => {
    execute(
      new HttpRequest('DELETE', '/api/v1/example/1'),
      throwError(() => new Error('backend failed')),
    );

    expect(loading.begin).toHaveBeenCalledTimes(1);
    expect(loading.end).toHaveBeenCalledTimes(1);
  });

  it('can force the loader for a long-running GET', () => {
    const context = new HttpContext().set(GLOBAL_LOADING, true);
    execute(new HttpRequest('GET', '/api/v1/reports/export', { context }));

    expect(loading.begin).toHaveBeenCalledTimes(1);
    expect(loading.end).toHaveBeenCalledTimes(1);
  });

  it('can skip the loader for a background mutation', () => {
    const context = new HttpContext().set(GLOBAL_LOADING, false);
    execute(new HttpRequest('POST', '/api/v1/background/sync', {}, { context }));

    expect(loading.begin).not.toHaveBeenCalled();
    expect(loading.end).not.toHaveBeenCalled();
  });
});
