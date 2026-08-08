import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ReportsPage } from './reports.page';
import { I18nService } from '../../core/i18n.service';
import { ReportsStore } from './reports.store';
import { PeriodOption } from './reports.models';

describe('ReportsPage REM-006 period presets', () => {
  let httpMock: HttpTestingController;
  let page: ReportsPage;

  const period: PeriodOption = {
    year: 2026,
    month: 8,
    kind: 'SECOND_HALF',
    start: 1786406400000,
    end: 1787356799999,
  };

  beforeEach(async () => {
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: () => undefined,
    });
    await TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: {
            locale: () => 'ar-EG',
            t: (key: string, params?: Record<string, string>) => params ? `${key}:${params['period']}` : key,
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(ReportsPage);
    page = fixture.componentInstance;

    flushInitialLoads();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  function flushInitialLoads() {
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/reports').flush([]);
    httpMock.expectOne((req) => req.method === 'GET' && req.url.includes('/available-periods')).flush([period]);
    httpMock.expectOne((req) => req.method === 'GET' && req.url === '/api/v1/admin/app-settings').flush({});
  }

  it('fills the period form on preset click and does not create a report', () => {
    page.create(period);

    expect(page.periodForm.getRawValue().periodStart).toBeDefined();
    expect(page.periodForm.getRawValue().payCycle).toBe('HALF_MONTHLY');
    const created = httpMock.match((req) => req.method === 'POST' && req.url.includes('/api/v1/reports'));
    expect(created).toHaveLength(0);
  });

  it('preset card is a non-destructive fill-only shortcut with an action aria label', () => {
    const name = page.periodName(period);
    const aria = page.i18n.t('reports.usePeriodAria', { period: name });
    expect(aria).toContain(name);
    expect(aria).toContain('reports.usePeriodAria');
  });
});
