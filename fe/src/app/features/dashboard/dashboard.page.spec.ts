import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { DashboardPage } from './dashboard.page';
import { DashboardStore } from './dashboard.store';
import { I18nService } from '../../core/i18n.service';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationService } from '../../core/notification.service';

type DashboardFixture = Record<string, unknown>;

describe('DashboardPage WP-08 peak clock-in analytics', () => {
  let httpMock: HttpTestingController;
  let page: DashboardPage;
  let store: DashboardStore;

  const HISTOGRAM_URL = '/api/v1/dashboard/clock-in-histogram';

  function dashboardPayload(categories: DashboardFixture[] = []): DashboardFixture {
    return {
      year: 2026, month: 8, reportId: null, reportStatus: null, updatedAt: 0,
      attendanceRate: 0, scheduledEmployeeDays: 0, presentEmployeeDays: 0,
      importedPunches: 0, singlePunchDays: 0, unresolvedCount: 0,
      unmatchedIdentities: 0, totalStockMovements: 0, totalInventoryItems: 0,
      lowStockCount: 0, negativeStockCount: 0, totalPartnerEntries: 0,
      categories, recentImports: [],
    };
  }

  const tick = () => new Promise<void>((resolve) => setTimeout(resolve, 0));

  /** Flushes every outstanding histogram request so no stragglers remain. */
  function drainPeak(payload: unknown[] = []): void {
    let matched = httpMock.match((req) => req.url === HISTOGRAM_URL);
    while (matched.length > 0) {
      matched.forEach((req) => req.flush(payload));
      matched = httpMock.match((req) => req.url === HISTOGRAM_URL);
    }
  }

  function flushInitialRequests(dashboard: DashboardFixture = dashboardPayload()): void {
    httpMock.expectOne((req) => req.url === '/api/v1/dashboard').flush(dashboard);
    httpMock.expectOne((req) => req.url === '/api/v1/dashboard/attendance-chart').flush([]);
    httpMock.expectOne((req) => req.url === '/api/v1/dashboard/payroll-summary').flush({});
    httpMock.expectOne((req) => req.url === '/api/v1/dashboard/department-metrics').flush([]);
    httpMock.expectOne((req) => req.url === '/api/v1/dashboard/trends').flush([]);
    httpMock.expectOne((req) => req.url.includes('/api/v1/imports/attendance/months')).flush([]);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: {
              subscribe: (fn: (params: Record<string, string>) => void) => fn({}),
            },
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: () => 'ar-EG',
          },
        },
        {
          provide: AuthService,
          useValue: {
            refreshPreferences: () => of({}),
            preferences: () => ({ dashboardWidgetIds: [], dashboardAnimationsEnabled: true }),
            appSettings: () => of({}),
            canCustomizeDashboard: () => false,
          },
        },
        { provide: NotificationService, useValue: { error: () => undefined, success: () => undefined } },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DashboardPage);
    page = fixture.componentInstance;
    store = page.store;
    fixture.detectChanges();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    httpMock.verify();
  });

  it('legend lists each histogram category once with a deterministic color dot', async () => {
    const categories = [
      { categoryId: 'SECURITY', categoryName: 'أمن', employeeCount: 4 },
      { categoryId: 'ADMIN', categoryName: 'إدارة', employeeCount: 3 },
    ];
    flushInitialRequests(dashboardPayload(categories));
    void store.loadClockInHistogram(6);
    await tick();
    drainPeak([
      { hour: 6, countsByCategory: { SECURITY: 5, ADMIN: 1 } },
      { hour: 8, countsByCategory: { ADMIN: 3 } },
    ]);
    await tick();

    const legend = page.peakLegend();
    expect(legend.map((item) => item.categoryId)).toEqual(['SECURITY', 'ADMIN']);
    expect(new Set(legend.map((item) => item.color)).size).toBe(2);
    expect(page.categoryColor('SECURITY')).toBe(page.categoryColor('SECURITY'));
  });

  it('category filter refetches the histogram with the selected category id', async () => {
    flushInitialRequests();
    await tick();
    drainPeak([{ hour: 6, countsByCategory: { SECURITY: 5 } }]);
    await tick();

    page.changePeakCategory('SECURITY');
    await tick();
    const requests = httpMock.match(
      (req) => req.url === HISTOGRAM_URL
        && req.params.get('months') === '6'
        && req.params.get('categoryId') === 'SECURITY');
    expect(requests.length).toBeGreaterThan(0);
    requests[0].flush([{ hour: 6, countsByCategory: { SECURITY: 2 } }]);
    drainPeak();
    await tick();

    expect(store.clockInBuckets()[0].countsByCategory['SECURITY']).toBe(2);
  });

  it('changing months keeps no category filter when none selected', async () => {
    flushInitialRequests();
    await tick();
    drainPeak();
    await tick();

    page.changePeakMonths('12');
    await tick();
    const requests = httpMock.match(
      (req) => req.url === HISTOGRAM_URL && req.params.get('months') === '12');
    expect(requests.length).toBeGreaterThan(0);
    for (const request of requests) {
      expect(request.request.params.get('categoryId')).toBeNull();
      request.flush([]);
    }
    drainPeak();
    await tick();
  });

  it('export hits the clock-in-histogram xlsx scope with current filters', async () => {
    flushInitialRequests();
    await tick();
    drainPeak([{ hour: 7, countsByCategory: { ADMIN: 4 } }]);
    await tick();

    page.changePeakCategory('ADMIN');
    await tick();
    const requests = httpMock.match(
      (req) => req.url === HISTOGRAM_URL
        && req.params.get('months') === '6'
        && req.params.get('categoryId') === 'ADMIN');
    expect(requests.length).toBeGreaterThan(0);
    requests[0].flush([{ hour: 7, countsByCategory: { ADMIN: 4 } }]);
    drainPeak();
    await tick();

    const promise = page.exportPeak();
    const exportReq = httpMock.expectOne(
      (req) => req.url === '/api/v1/exports/clock-in-histogram.xlsx' && req.params.get('categoryId') === 'ADMIN');
    expect(exportReq.request.responseType).toBe('blob');
    exportReq.flush(new Blob(['xlsx']));
    await promise;
    expect(page.peakExporting()).toBe(false);
  });
});
