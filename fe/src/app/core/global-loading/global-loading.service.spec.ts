import '@angular/compiler';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GlobalLoadingService } from './global-loading.service';

describe('GlobalLoadingService', () => {
  let service: GlobalLoadingService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = new GlobalLoadingService();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not flash for quick requests', () => {
    service.begin();
    vi.advanceTimersByTime(100);
    service.end();
    vi.advanceTimersByTime(100);

    expect(service.visible()).toBe(false);
  });

  it('shows after the delay and hides when the request finishes', () => {
    service.begin();
    vi.advanceTimersByTime(150);

    expect(service.visible()).toBe(true);

    service.end();

    expect(service.visible()).toBe(false);
  });

  it('stays visible while overlapping requests are still pending', () => {
    service.begin();
    service.begin();
    vi.advanceTimersByTime(150);

    expect(service.visible()).toBe(true);

    service.end();
    expect(service.visible()).toBe(true);

    service.end();
    expect(service.visible()).toBe(false);
  });
});
