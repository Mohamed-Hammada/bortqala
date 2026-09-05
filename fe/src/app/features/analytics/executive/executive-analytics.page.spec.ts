import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExecutiveAnalyticsPage } from './executive-analytics.page';
import {
  ComparativeTrends,
  ExecutiveKpiSnapshot,
  ExecutiveOverview,
  KpiDefinition,
  OwnerCockpitResponse,
} from './executive-analytics.models';

describe('ExecutiveAnalyticsPage', () => {
  let httpMock: HttpTestingController;

  const mockCockpit: OwnerCockpitResponse = {
    period: '2026-09',
    timestamp: 123456789,
    kpiSummary: {
      todaySales: 150000,
      todayCollections: 95000,
      netLiquidity: 1850000,
      cashInHand: 350000,
      bankBalances: 1710000,
      totalRevenue: 2800000,
      totalCogs: 1680000,
      grossMarginAmount: 1120000,
      grossMarginPercent: 40.0,
      totalOpex: 480000,
      operatingProfit: 640000,
      netProfit: 580000,
      netMarginPercent: 20.7,
      payrollDisbursed: 210000,
      payrollPending: 45000,
      activeHeadcount: 42,
      manufacturingWipCount: 3,
      manufacturingWipValuation: 240000,
      projectBudgetTotal: 3500000,
      projectActualCost: 2900000,
      projectCostVariance: 600000,
      lowStockCount: 4,
      deadStockCount: 2,
      totalReceivables: 850000,
      overdueReceivables: 120000,
      totalPayables: 450000,
      overduePayables: 60000,
    },
    targets: {
      id: 'tgt-1',
      periodKey: '2026-Q3',
      targetRevenue: 2500000,
      targetGrossMarginPercent: 38.0,
      targetMaxOpex: 500000,
      targetMinLiquidity: 1500000,
      targetMaxOverdueAr: 150000,
      notes: 'Standard Q3',
      updatedAt: 123456789,
    },
    arAging: {
      current: { labelKey: 'executive.bucketCurrent', amount: 500000, invoiceCount: 15, percentOfTotal: 58.8 },
      days30To60: { labelKey: 'executive.bucket30to60', amount: 200000, invoiceCount: 6, percentOfTotal: 23.5 },
      days60To90: { labelKey: 'executive.bucket60to90', amount: 90000, invoiceCount: 3, percentOfTotal: 10.6 },
      daysOver90: { labelKey: 'executive.bucketOver90', amount: 60000, invoiceCount: 2, percentOfTotal: 7.1 },
      total: 850000,
      totalOverdue: 350000,
    },
    apAging: {
      current: { labelKey: 'executive.bucketCurrent', amount: 300000, invoiceCount: 8, percentOfTotal: 66.7 },
      days30To60: { labelKey: 'executive.bucket30to60', amount: 90000, invoiceCount: 3, percentOfTotal: 20.0 },
      days60To90: { labelKey: 'executive.bucket60to90', amount: 40000, invoiceCount: 2, percentOfTotal: 8.9 },
      daysOver90: { labelKey: 'executive.bucketOver90', amount: 20000, invoiceCount: 1, percentOfTotal: 4.4 },
      total: 450000,
      totalOverdue: 150000,
    },
    branchLeaderboard: [
      {
        branchId: 'br-main',
        branchCode: 'CAI',
        branchName: 'Cairo HQ',
        isMainBranch: true,
        revenue: 1800000,
        cogs: 1080000,
        grossProfit: 720000,
        grossMarginPercent: 40.0,
        opex: 300000,
        netProfit: 420000,
        headcount: 28,
        cashAndBank: 1200000,
      },
    ],
    topCustomers: [
      {
        customerId: 'cust-1',
        customerName: 'Al-Ahram Trading',
        totalInvoiced: 650000,
        totalCollected: 500000,
        outstandingBalance: 150000,
        invoiceCount: 8,
      },
    ],
    topProducts: [
      {
        itemId: 'it-1',
        itemCode: 'IT-001',
        itemName: 'Industrial Pump A1',
        quantitySold: 45,
        revenue: 450000,
        cogs: 270000,
        marginPercent: 40.0,
      },
    ],
    expenseBreakdown: [
      {
        category: 'SALARIES',
        nameKey: 'expenses.salaries',
        amount: 210000,
        percentOfTotal: 43.8,
      },
    ],
    lowStockAlerts: [
      {
        itemId: 'it-2',
        itemCode: 'IT-002',
        itemName: 'Bearing 6204',
        currentStock: 12,
        reorderPoint: 25,
        reorderQuantity: 50,
        isDeadStock: false,
        estimatedValue: 2400,
      },
    ],
    deadStockAlerts: [],
    manufacturingWip: [
      {
        orderId: 'mo-1',
        orderNumber: 'WO-2026-001',
        itemName: 'Assembly Line X',
        targetQuantity: 100,
        actualOutputQuantity: 65,
        materialCost: 180000,
        startDate: '2026-09-01',
        status: 'IN_PROGRESS',
      },
    ],
    projectBudgetControl: [
      {
        projectId: 'p-1',
        code: 'PRJ-101',
        name: 'New Factory Setup',
        contractValue: 5000000,
        budgetAmount: 4200000,
        actualCost: 3800000,
        costVariance: 400000,
        status: 'ACTIVE',
      },
    ],
  };

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
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
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

    const reqCockpit = httpMock.expectOne(req => req.url === '/api/v1/analytics/executive/cockpit');
    expect(reqCockpit.request.method).toBe('GET');
    reqCockpit.flush(mockCockpit);

    const reqBranches = httpMock.expectOne('/api/v1/organization/branches');
    expect(reqBranches.request.method).toBe('GET');
    reqBranches.flush([
      { id: 'br-main', code: 'CAI', name: 'Cairo HQ' },
    ]);

    await Promise.resolve();

    return { fixture, component };
  }

  it('should create and load cockpit and branches on init', async () => {
    const { component } = await setupComponent();
    expect(component).toBeTruthy();
    expect(component.activeTab()).toBe('cockpit');
    expect(component.cockpitData()?.kpiSummary.totalRevenue).toBe(2800000);
    expect(component.cockpitData()?.branchLeaderboard.length).toBe(1);
    expect(component.branches().length).toBe(1);
  });

  it('should switch tabs and load overview on demand', async () => {
    const { fixture, component } = await setupComponent();
    expect(component.activeTab()).toBe('cockpit');

    component.setTab('overview');
    expect(component.activeTab()).toBe('overview');

    const reqOverview = httpMock.expectOne('/api/v1/analytics/executive/overview');
    reqOverview.flush(mockOverview);

    const reqTrends = httpMock.expectOne('/api/v1/analytics/executive/trends?months=6');
    reqTrends.flush(mockTrends);

    const reqRegistry = httpMock.expectOne('/api/v1/analytics/executive/kpi-registry');
    reqRegistry.flush(mockRegistry);

    const reqSnapshots = httpMock.expectOne('/api/v1/analytics/executive/snapshots');
    reqSnapshots.flush(mockSnapshots);

    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.overview()?.totalRevenue).toBe(1500000);
  });

  it('should change period preset and reload cockpit', async () => {
    const { component } = await setupComponent();

    component.onPeriodPresetChange('THIS_QUARTER');

    const reqCockpit = httpMock.expectOne(req => req.url === '/api/v1/analytics/executive/cockpit');
    expect(reqCockpit.request.params.get('period')).toContain('-Q');
    reqCockpit.flush({ ...mockCockpit, period: '2026-Q3' });

    await Promise.resolve();
    expect(component.cockpitData()?.period).toBe('2026-Q3');
  });

  it('should open target modal and submit targets', async () => {
    const { component } = await setupComponent();

    component.openTargetModal();
    expect(component.targetModalOpen()).toBe(true);

    component.targetForm.patchValue({
      periodKey: '2026-Q4',
      targetRevenue: 3000000,
      targetGrossMarginPercent: 42.0,
      targetMaxOpex: 550000,
      targetMinLiquidity: 1800000,
      targetMaxOverdueAr: 100000,
      notes: 'Q4 Target Stretch',
    });

    const submitPromise = component.submitTarget();

    const postReq = httpMock.expectOne('/api/v1/analytics/executive/targets');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.targetRevenue).toBe(3000000);
    postReq.flush({
      id: 'tgt-2',
      periodKey: '2026-Q4',
      targetRevenue: 3000000,
      targetGrossMarginPercent: 42.0,
      targetMaxOpex: 550000,
      targetMinLiquidity: 1800000,
      targetMaxOverdueAr: 100000,
      notes: 'Q4 Target Stretch',
      updatedAt: 123456789,
    });

    await Promise.resolve();

    const reloadReq = httpMock.expectOne(req => req.url === '/api/v1/analytics/executive/cockpit');
    reloadReq.flush(mockCockpit);

    await submitPromise;
    expect(component.targetModalOpen()).toBe(false);
  });

  it('should trigger excel export', async () => {
    const { component } = await setupComponent();

    const exportPromise = component.exportCockpitExcel();

    const exportReq = httpMock.expectOne(req => req.url === '/api/v1/analytics/executive/cockpit/export.xlsx');
    expect(exportReq.request.method).toBe('GET');
    exportReq.flush(new Blob(['test-excel-content']));

    await exportPromise;
    expect(component.exportingExcel()).toBe(false);
  });
});

