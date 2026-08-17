import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardStore } from './dashboard.store';
import { I18nService } from '../../core/i18n.service';

describe('DashboardStore', () => {
  let store: DashboardStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardStore,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'ar-EG' } },
      ],
    });
    store = TestBed.inject(DashboardStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(store).toBeTruthy();
  });

  it('loads multi-period trends into the trends signal', async () => {
    const payload = [
      { label: '2026-07', year: 2026, month: 7, scheduledEmployeeDays: 2, presentEmployeeDays: 1,
        attendanceRate: 50, exceptionDays: 1, overtimeMinutes: 30, paidCount: 1, pendingCount: 1,
        totalGross: 8000, totalPaid: 4500 },
    ];
    const promise = store.loadTrends(6, 2026, 7);
    const request = httpMock.expectOne('/api/v1/dashboard/trends?months=6&year=2026&month=7');
    request.flush(payload);
    await promise;
    expect(store.trends().length).toBe(1);
    expect(store.trends()[0].attendanceRate).toBe(50);
    expect(store.trendsLoading()).toBe(false);
  });

  it('downloads the multi-period trends workbook', async () => {
    const promise = store.downloadTrends(12, 2026, 7);
    const request = httpMock.expectOne('/api/v1/exports/trends.xlsx?months=12&year=2026&month=7');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['xlsx'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
    const blob = await promise;
    expect(blob.size).toBe(4);
  });
});
