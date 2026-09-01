import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { DashboardPage } from './dashboard.page';
import { DashboardStore } from './dashboard.store';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';

describe('DashboardPage - Hierarchy, Period Filter, Charts & Accessibility', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let component: DashboardPage;
  let store: DashboardStore;
  let router: Router;

  const mockPref = signal({
    dashboardWidgetIds: [
      'summary',
      'report',
      'attendance-chart',
      'insights',
      'units',
      'departments',
      'categories',
      'imports',
    ],
    dashboardAnimationsEnabled: true,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        {
          provide: HttpClient,
          useValue: {
            get: () => of([]),
            post: () => of({}),
            put: () => of({}),
          },
        },
        DashboardStore,
        {
          provide: AuthService,
          useValue: {
            user: signal({ id: 'u1', username: 'admin', roles: ['ADMIN'] }),
            preferences: mockPref,
            refreshPreferences: () => of({}),
            updateDashboardPreferences: vi.fn().mockReturnValue(of({})),
            canCustomizeDashboard: () => true,
            hasPermission: () => true,
          },
        },
        {
          provide: I18nService,
          useValue: {
            t: (k: string) => k,
            locale: signal('en-US'),
            dir: signal('ltr'),
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(DashboardPage);
    component = fixture.componentInstance;
    store = component.store;
    vi.spyOn(store, 'loadAll').mockResolvedValue(undefined);
    store.loading.set(false);
    store.chartLoading.set(false);
    component.draftWidgetIds.set([
      'summary',
      'report',
      'attendance-chart',
      'insights',
      'units',
      'departments',
      'categories',
      'imports',
    ]);
  });

  it('renders above-the-fold hierarchy with summary cards and hero section', () => {
    store.data.set({
      year: 2026,
      month: 9,
      updatedAt: '2026-09-01T10:00:00Z',
      activeEmployees: 42,
      activeCategories: 3,
      reportStatus: 'APPROVED',
      reportId: 'rep-1',
      unresolvedCount: 0,
      scheduledEmployeeDays: 800,
      presentEmployeeDays: 760,
      attendanceRate: 95,
      lateEmployeeDays: 10,
      singlePunchDays: 2,
      overtimeMinutes: 120,
      unmatchedIdentities: 0,
      importedPunches: 1500,
      totalStockMovements: 0,
      totalInventoryItems: 0,
      lowStockCount: 0,
      negativeStockCount: 0,
      totalPartnerEntries: 0,
      activePartiesCount: 0,
      categories: [],
      recentImports: [],
    });
    fixture.detectChanges();

    const summaryCards = fixture.nativeElement.querySelectorAll('.summary-card');
    expect(summaryCards.length).toBeGreaterThanOrEqual(4);

    const reportState = fixture.nativeElement.querySelector('.report-state');
    expect(reportState).toBeTruthy();
  });

  it('period change updates year/month and navigates with query params deterministically', () => {
    component.changePeriod('2026', '10');

    expect(component.year()).toBe(2026);
    expect(component.month()).toBe(10);
    expect(router.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: { year: 2026, month: 10 },
      queryParamsHandling: 'merge',
    }));
  });

  it('provides a screen-reader accessible fallback table for the attendance chart', () => {
    store.loading.set(false);
    store.chartLoading.set(false);
    store.data.set({
      year: 2026,
      month: 9,
      updatedAt: '2026-09-01T10:00:00Z',
      activeEmployees: 42,
      activeCategories: 3,
      reportStatus: 'APPROVED',
      reportId: 'rep-1',
      unresolvedCount: 0,
      scheduledEmployeeDays: 800,
      presentEmployeeDays: 760,
      attendanceRate: 95,
      lateEmployeeDays: 10,
      singlePunchDays: 2,
      overtimeMinutes: 120,
      unmatchedIdentities: 0,
      importedPunches: 1500,
      totalStockMovements: 0,
      totalInventoryItems: 0,
      lowStockCount: 0,
      negativeStockCount: 0,
      totalPartnerEntries: 0,
      activePartiesCount: 0,
      categories: [],
      recentImports: [],
    });
    store.chartData.set([
      { label: '2026-09-01', present: 40, absent: 2, late: 1, exception: 0 },
      { label: '2026-09-02', present: 41, absent: 1, late: 0, exception: 0 },
    ]);
    fixture.detectChanges();

    const chartSection = fixture.nativeElement.querySelector('.chart-section');
    expect(chartSection).toBeTruthy();
    const srTable = fixture.nativeElement.querySelector('.chart-section table.sr-only');
    expect(srTable).toBeTruthy();
    const rows = srTable.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
  });

  it('toggles motion preferences dynamically', async () => {
    component.animationsEnabled.set(true);
    await component.toggleAnimations();
    expect(component.animationsEnabled()).toBe(false);

    await component.toggleAnimations();
    expect(component.animationsEnabled()).toBe(true);
  });
});
