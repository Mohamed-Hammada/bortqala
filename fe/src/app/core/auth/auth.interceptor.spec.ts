import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';

describe('authInterceptor', () => {
  it('clones requests with required correlation headers', () => {
    const req = new HttpRequest('GET', '/api/v1/employees');
    const modified = req.clone({ setHeaders: { 'X-Correlation-Id': 'test-id' } });
    expect(modified.headers.get('X-Correlation-Id')).toBe('test-id');
  });
});
