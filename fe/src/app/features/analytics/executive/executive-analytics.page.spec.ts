import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExecutiveAnalyticsPage } from './executive-analytics.page';
import {
  ComparativeTrends,
  ExecutiveKpiSnapshot,
  ExecutiveOverview,
  KpiDefinition,
} from './executive-analytics.models';

describe('ExecutiveAnalyticsPage', () => {
  let httpMock: HttpTestingController;

  const mockOverview: ExecutiveOverview = {
    period: '2026-08',
    timestamp: 123456,
    totalRevenue: 1500000,
    totalOpex: 900000,
    grossProfit: 600000,
    netMarginPercent: 40.0,
    operatingCashFlow: 510000,
    salesBookings: 300000,
    posGross: 120000,
    openReceivables: 330000,
    inventoryValuation: 450000,
    projectPortfolioValue: 2000000,
    projectCostVariance: 300000,
    activeHeadcount: 50,
    payrollDisbursed: 625000,
    attendanceRatePercent: 96.5,
    etaTaxCompliancePercent: 98.4,
    moduleSummaries: [
      {
        category: 'FINANCIAL',
        moduleName: 'General Ledger',
        kpis: [
          {
            key: 'NET_PROFIT_MARGIN',
            nameEn: 'Net Profit Margin',
            nameAr: 'هامش صافي الربح',
            category: 'FINANCIAL',
            actualValue: 40.0,
            targetValue: 25.0,
            variancePercent: 15.0,
            trendDirection: 'UP',
            unit: 'PERCENT',
            reconciliationStatus: 'RECONCILED',
            drilldownUrl: '/finance/accounts',
          },
        ],
      },
    ],
  };

  const mockTrends: ComparativeTrends = {
    months: 6,
    trendPoints: [
      {
        period: '2026-03',
        revenue: 1200000,
        opex: 800000,
        netProfit: 400000,
        marginPercent: 33.3,
        salesBookings: 450000,
        inventoryValue: 600000,
        payrollDisbursed: 320000,
        projectEarnedValue: 850000,
      },
    ],
  };

  const mockRegistry: KpiDefinition[] = [
    {
      key: 'NET_PROFIT_MARGIN',
      nameEn: 'Net Profit Margin',
      nameAr: 'هامش صافي الربح',
      category: 'FINANCIAL',
      grain: 'MONTHLY',
      unit: 'PERCENT',
      formulaEn: 'Net Profit / Revenue * 100',
      formulaAr: 'صافي الربح / الإيرادات * 100',
      sourceModule: 'Finance',
      requiredPermission: 'P_FINANCE_READ',
    },
  ];

  const mockSnapshots: ExecutiveKpiSnapshot[] = [
    {
      id: 'snp-1',
      snapshotDate: 123456,
      periodKey: '2026-Q3',
      category: 'FINANCIAL',
      kpiKey: 'NET_PROFIT_MARGIN',
      targetValue: 25.0,
      actualValue: 40.0,
      varianceValue: 15.0,
      variancePercent: 60.0,
      trendDirection: 'UP',
      reconciliationStatus: 'RECONCILED',
      drilldownUrl: '/finance/accounts',
      metadataJson: '{}',
      createdAt: 123456,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExecutiveAnalyticsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  async function setupComponent() {
    const fixture = TestBed.createComponent(ExecutiveAnalyticsPage);
    const component = fixture.componentInstance;

    fixture.detectChanges();

    const reqOverview = httpMock.expectOne('/api/v1/analytics/executive/overview');
    expect(reqOverview.request.method).toBe('GET');
    reqOverview.flush(mockOverview);

    const reqTrends = httpMock.expectOne('/api/v1/analytics/executive/trends?months=6');
    expect(reqTrends.request.method).toBe('GET');
    reqTrends.flush(mockTrends);

    const reqRegistry = httpMock.expectOne('/api/v1/analytics/executive/kpi-registry');
    expect(reqRegistry.request.method).toBe('GET');
    reqRegistry.flush(mockRegistry);

    const reqSnapshots = httpMock.expectOne('/api/v1/analytics/executive/snapshots');
    expect(reqSnapshots.request.method).toBe('GET');
    reqSnapshots.flush(mockSnapshots);

    await Promise.resolve();

    return { fixture, component };
  }

  it('should create and load overview, trends, registry and snapshots on init', async () => {
    const { component } = await setupComponent();
    expect(component).toBeTruthy();
    expect(component.overview()?.totalRevenue).toBe(1500000);
    expect(component.trends()?.trendPoints.length).toBe(1);
    expect(component.registry().length).toBe(1);
    expect(component.snapshots().length).toBe(1);
  });

  it('should switch tabs properly', async () => {
    const { component } = await setupComponent();
    component.activeTab.set('trends');
    expect(component.activeTab()).toBe('trends');

    component.activeTab.set('registry');
    expect(component.activeTab()).toBe('registry');

    component.activeTab.set('snapshots');
    expect(component.activeTab()).toBe('snapshots');
  });

  it('should change months for trends', async () => {
    const { component } = await setupComponent();

    const changePromise = component.changeMonths(12);

    const reqTrends12 = httpMock.expectOne('/api/v1/analytics/executive/trends?months=12');
    expect(reqTrends12.request.method).toBe('GET');
    reqTrends12.flush({ ...mockTrends, months: 12 });

    await changePromise;
    expect(component.selectedMonths()).toBe(12);
  });

  it('should open modal and submit snapshot', async () => {
    const { component } = await setupComponent();

    component.openSnapshotModal();
    expect(component.snapshotModalOpen()).toBe(true);

    component.snapshotForm.patchValue({
      periodKey: '2026-Q3',
      category: 'FINANCIAL',
      kpiKey: 'NET_PROFIT_MARGIN',
      actualValue: 40.0,
      trendDirection: 'UP',
      reconciliationStatus: 'RECONCILED',
    });

    const submitPromise = component.submitSnapshot();

    const postReq = httpMock.expectOne('/api/v1/analytics/executive/snapshots');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(mockSnapshots[0]);

    await Promise.resolve();

    const reqSnapshots = httpMock.expectOne('/api/v1/analytics/executive/snapshots');
    reqSnapshots.flush(mockSnapshots);

    await submitPromise;
    expect(component.snapshotModalOpen()).toBe(false);
  });
});
