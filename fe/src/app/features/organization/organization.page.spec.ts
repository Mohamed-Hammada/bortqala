import { TestBed } from '@angular/core/testing';
import { OrganizationPage } from './organization.page';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  BranchControlSummary,
  ConsolidatedGroupReport,
  ConsolidatedOrganizationSummary,
  IntercompanyTransaction,
  OrganizationHierarchy,
  StockTransferDiscrepancyItem,
  StockTransferItem,
} from './organization.models';

describe('OrganizationPage', () => {
  let httpMock: HttpTestingController;

  const mockHierarchy: OrganizationHierarchy = {
    companies: [{ id: 'c1', code: 'CMP-1', name: 'Main Co', active: true, createdAt: 1, updatedAt: 1 }],
    branches: [{ id: 'b1', companyId: 'c1', code: 'BR-1', name: 'Cairo Branch', active: true, createdAt: 1, updatedAt: 1 }],
    warehouses: [{ id: 'w1', branchId: 'b1', code: 'WH-1', name: 'Main WH', active: true, createdAt: 1, updatedAt: 1 }],
    departments: [{ id: 'd1', companyId: 'c1', code: 'DEP-1', name: 'Finance', active: true, createdAt: 1, updatedAt: 1 }],
  };

  const mockConsolidation: ConsolidatedOrganizationSummary = {
    totalRevenue: 1500000,
    totalExpenses: 900000,
    eliminatedTransfers: 100000,
    consolidatedNetMargin: 600000,
    activeBranches: 1,
    totalHeadcount: 25,
    branchMetrics: [
      {
        branchId: 'b1',
        branchCode: 'BR-1',
        branchName: 'Cairo Branch',
        companyId: 'c1',
        companyName: 'Main Co',
        revenue: 1500000,
        expenses: 900000,
        netProfit: 600000,
        marginPercent: 40,
        inventoryValue: 300000,
        headcount: 25,
        activeProjects: 3,
      },
    ],
  };

  const mockTxs: IntercompanyTransaction[] = [
    {
      id: 'tx-1',
      transactionNumber: 'IC-2026-001',
      fromCompanyId: 'c1',
      fromCompanyName: 'Main Co',
      toCompanyId: 'c2',
      toCompanyName: 'Sister Co',
      transactionType: 'INVENTORY_TRANSFER',
      amount: 50000,
      currency: 'EGP',
      status: 'PENDING_APPROVAL',
      createdAt: 1,
      updatedAt: 1,
    },
  ];

  const mockControlSummary: BranchControlSummary = {
    branchId: 'b1',
    branchCode: 'BR-1',
    branchName: 'Cairo Branch',
    companyId: 'c1',
    companyName: 'Main Co',
    isMainBranch: true,
    warehousesCount: 2,
    cashboxesCount: 1,
    bankAccountsCount: 2,
    posTerminalsCount: 4,
    employeesCount: 15,
    inventoryValuation: 250000,
    activeUsersCount: 8,
  };

  const mockTransfers: StockTransferItem[] = [
    {
      id: 'st-1',
      transferNumber: 'TR-2026-001',
      sourceWarehouseId: 'w1',
      sourceWarehouseName: 'Main WH',
      sourceBranchName: 'Cairo Branch',
      targetWarehouseId: 'w2',
      targetWarehouseName: 'Alex WH',
      targetBranchName: 'Alex Branch',
      transferDate: '2026-09-01',
      status: 'DRAFT',
      version: 1,
      lines: [
        {
          id: 'stl-1',
          itemId: 'item-1',
          itemCode: 'ITM-001',
          itemName: 'Widget A',
          quantity: 100,
        },
      ],
    },
  ];

  const mockDiscrepancies: StockTransferDiscrepancyItem[] = [
    {
      id: 'disc-1',
      transferId: 'st-1',
      transferLineId: 'stl-1',
      itemId: 'item-1',
      itemCode: 'ITM-001',
      itemName: 'Widget A',
      shippedQuantity: 100,
      receivedQuantity: 95,
      damagedQuantity: 3,
      lostQuantity: 2,
      discrepancyType: 'DAMAGED',
      resolutionStatus: 'PENDING',
      reportedBy: 'user-1',
      reportedAt: 1725400000000,
    },
  ];

  const mockGroupReport: ConsolidatedGroupReport = {
    period: '2026-Q1',
    totalRevenue: 2500000,
    totalExpenses: 1800000,
    netIncome: 700000,
    totalAssets: 4000000,
    totalLiabilities: 1500000,
    totalEquity: 2500000,
    intercompanyEliminationsCount: 1,
    intercompanyEliminationsTotal: 150000,
    plLines: [
      {
        accountCode: '4101',
        accountName: 'Sales Revenue',
        category: 'REVENUE',
        amount: 2500000,
        eliminationAmount: 150000,
        consolidatedAmount: 2350000,
      },
    ],
    balanceSheetLines: [
      {
        accountCode: '1101',
        accountName: 'Cash & Banks',
        category: 'ASSET',
        amount: 1200000,
        eliminationAmount: 0,
        consolidatedAmount: 1200000,
      },
    ],
    branchComparison: [
      {
        branchId: 'b1',
        branchCode: 'BR-1',
        branchName: 'Cairo Branch',
        companyName: 'Main Co',
        revenue: 1500000,
        expenses: 900000,
        netProfit: 600000,
        inventoryValuation: 300000,
        headcount: 15,
      },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function flushLoad(
    hierarchy = mockHierarchy,
    consolidation = mockConsolidation,
    txs = mockTxs,
    permitted = mockHierarchy.branches
  ) {
    const reqHierarchy = httpMock.expectOne('/api/v1/organization');
    expect(reqHierarchy.request.method).toBe('GET');
    reqHierarchy.flush(hierarchy);

    const reqConsolidation = httpMock.expectOne('/api/v1/organization/consolidation/summary');
    expect(reqConsolidation.request.method).toBe('GET');
    reqConsolidation.flush(consolidation);

    const reqIntercompany = httpMock.expectOne('/api/v1/organization/intercompany');
    expect(reqIntercompany.request.method).toBe('GET');
    reqIntercompany.flush(txs);

    const reqPermitted = httpMock.expectOne('/api/v1/organization/branches/permitted');
    expect(reqPermitted.request.method).toBe('GET');
    reqPermitted.flush(permitted);
  }

  async function flushMicrotasks() {
    for (let i = 0; i < 10; i++) {
      await Promise.resolve();
    }
  }

  async function setupComponent() {
    const fixture = TestBed.createComponent(OrganizationPage);
    const component = fixture.componentInstance;

    flushLoad();
    await flushMicrotasks();

    return { fixture, component };
  }

  it('should create and load organization hierarchy, consolidation and intercompany data', async () => {
    const { component } = await setupComponent();
    expect(component).toBeTruthy();
    expect(component.companies().length).toBe(1);
    expect(component.branches().length).toBe(1);
    expect(component.consolidationSummary()?.totalRevenue).toBe(1500000);
    expect(component.intercompanyTransactions().length).toBe(1);
  });

  it('should switch tabs properly and trigger loads', async () => {
    const { component } = await setupComponent();

    // Switch to consolidation tab triggers loadGroupReport
    component.switchTab('consolidation');
    expect(component.activeTab()).toBe('consolidation');

    const repReq = httpMock.expectOne((req) => req.url.startsWith('/api/v1/organization/consolidation/group-report'));
    expect(repReq.request.method).toBe('GET');
    repReq.flush(mockGroupReport);
    await flushMicrotasks();
    expect(component.groupReport()?.netIncome).toBe(700000);

    // Switch to transfers tab triggers loadTransfers
    component.switchTab('transfers');
    expect(component.activeTab()).toBe('transfers');

    const trReq = httpMock.expectOne('/api/v1/operations/transfers');
    expect(trReq.request.method).toBe('GET');
    trReq.flush(mockTransfers);

    const discReq = httpMock.expectOne('/api/v1/operations/transfers/discrepancies');
    expect(discReq.request.method).toBe('GET');
    discReq.flush(mockDiscrepancies);

    await flushMicrotasks();
    expect(component.transfers().length).toBe(1);
    expect(component.discrepancies().length).toBe(1);
  });

  it('should view branch 360 control summary', async () => {
    const { component } = await setupComponent();

    const summaryPromise = component.viewBranchSummary(mockHierarchy.branches[0].id);
    expect(component.summaryModalOpen()).toBe(true);

    const sumReq = httpMock.expectOne('/api/v1/organization/branches/b1/control-summary');
    expect(sumReq.request.method).toBe('GET');
    sumReq.flush(mockControlSummary);

    await summaryPromise;
    expect(component.selectedBranchSummary()?.branchCode).toBe('BR-1');
    expect(component.selectedBranchSummary()?.inventoryValuation).toBe(250000);

    component.closeSummaryModal();
    expect(component.summaryModalOpen()).toBe(false);
  });

  it('should dispatch a stock transfer with carrier and vehicle details', async () => {
    const { component } = await setupComponent();

    component.openDispatchModal(mockTransfers[0]);
    expect(component.dispatchModalOpen()).toBe(true);

    component.dispatchForm.patchValue({
      carrierName: 'Fast Express',
      driverName: 'Ahmed Ali',
      driverPhone: '01012345678',
      vehiclePlate: 'ABC-1234',
      waybillNumber: 'WB-998877',
      notes: 'Fragile equipment',
    });

    const dispatchPromise = component.submitDispatch();

    const dispReq = httpMock.expectOne('/api/v1/operations/transfers/st-1/dispatch');
    expect(dispReq.request.method).toBe('POST');
    expect(dispReq.request.body).toEqual({
      carrierName: 'Fast Express',
      driverName: 'Ahmed Ali',
      driverPhone: '01012345678',
      vehiclePlate: 'ABC-1234',
      waybillNumber: 'WB-998877',
      notes: 'Fragile equipment',
    });
    dispReq.flush({ ...mockTransfers[0], status: 'SHIPPED' });

    await flushMicrotasks();

    // Auto-reloads transfers
    const trReq = httpMock.expectOne('/api/v1/operations/transfers');
    trReq.flush([{ ...mockTransfers[0], status: 'SHIPPED' }]);
    const discReq = httpMock.expectOne('/api/v1/operations/transfers/discrepancies');
    discReq.flush([]);

    await dispatchPromise;
    expect(component.dispatchModalOpen()).toBe(false);
  });

  it('should receive and inspect a stock transfer with discrepancies', async () => {
    const { component } = await setupComponent();

    const shippedTransfer: StockTransferItem = {
      ...mockTransfers[0],
      status: 'SHIPPED',
    };

    component.openInspectModal(shippedTransfer);
    expect(component.inspectModalOpen()).toBe(true);
    expect(component.inspectionLines().length).toBe(1);

    component.updateInspectionLine(0, 'receivedQuantity', 95);
    component.updateInspectionLine(0, 'damagedQuantity', 3);
    component.updateInspectionLine(0, 'lostQuantity', 2);
    component.updateInspectionLine(0, 'discrepancyReason', 'Damaged in transit');
    component.inspectNotes.setValue('Inspected by Cairo dock master');

    const receivePromise = component.submitReceiveInspection();

    const recReq = httpMock.expectOne('/api/v1/operations/transfers/st-1/receive-inspection');
    expect(recReq.request.method).toBe('POST');
    expect(recReq.request.body).toEqual({
      notes: 'Inspected by Cairo dock master',
      inspectionLines: [
        {
          lineId: 'stl-1',
          receivedQuantity: 95,
          damagedQuantity: 3,
          lostQuantity: 2,
          discrepancyReason: 'Damaged in transit',
          discrepancyNotes: '',
        },
      ],
    });
    recReq.flush({ ...shippedTransfer, status: 'RECEIVED', hasDiscrepancy: true });

    await flushMicrotasks();

    // Auto-reloads transfers
    const trReq = httpMock.expectOne('/api/v1/operations/transfers');
    trReq.flush([{ ...shippedTransfer, status: 'RECEIVED', hasDiscrepancy: true }]);
    const discReq = httpMock.expectOne('/api/v1/operations/transfers/discrepancies');
    discReq.flush(mockDiscrepancies);

    await receivePromise;
    expect(component.inspectModalOpen()).toBe(false);
  });

  it('should resolve a transfer discrepancy', async () => {
    const { component } = await setupComponent();

    component.openResolveModal(mockDiscrepancies[0]);
    expect(component.resolveModalOpen()).toBe(true);

    component.resolveForm.patchValue({
      resolutionStatus: 'CLAIMED',
      resolutionNotes: 'Insurance claim filed with carrier',
    });

    const resolvePromise = component.submitResolve();

    const resReq = httpMock.expectOne('/api/v1/operations/transfers/discrepancies/disc-1/resolve');
    expect(resReq.request.method).toBe('POST');
    expect(resReq.request.body).toEqual({
      resolutionStatus: 'CLAIMED',
      resolutionNotes: 'Insurance claim filed with carrier',
    });
    resReq.flush({
      ...mockDiscrepancies[0],
      resolutionStatus: 'CLAIMED',
      resolutionNotes: 'Insurance claim filed with carrier',
    });

    await flushMicrotasks();

    // Auto-reloads transfers
    const trReq = httpMock.expectOne('/api/v1/operations/transfers');
    trReq.flush(mockTransfers);
    const discReq = httpMock.expectOne('/api/v1/operations/transfers/discrepancies');
    discReq.flush([{ ...mockDiscrepancies[0], resolutionStatus: 'CLAIMED' }]);

    await resolvePromise;
    expect(component.resolveModalOpen()).toBe(false);
  });

  it('should load consolidated group report with filters and export excel', async () => {
    const { component } = await setupComponent();

    component.selectedCompanyFilter.set('c1');
    component.selectedBranchFilter.set('b1');
    component.selectedPeriod.set('2026-Q2');

    const repPromise = component.loadGroupReport();

    const repReq = httpMock.expectOne(
      (req) =>
        req.url.includes('/api/v1/organization/consolidation/group-report') &&
        req.url.includes('companyId=c1') &&
        req.url.includes('branchId=b1') &&
        req.url.includes('period=2026-Q2')
    );
    expect(repReq.request.method).toBe('GET');
    repReq.flush(mockGroupReport);

    await repPromise;
    expect(component.groupReport()?.totalRevenue).toBe(2500000);

    // Test Excel export
    const exportPromise = component.exportGroupReport();
    const expReq = httpMock.expectOne(
      (req) =>
        req.url.includes('/api/v1/organization/consolidation/group-report/export') &&
        req.url.includes('companyId=c1') &&
        req.url.includes('branchId=b1') &&
        req.url.includes('period=2026-Q2')
    );
    expect(expReq.request.method).toBe('GET');
    expReq.flush(new Blob(['test-xlsx-content'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));

    await exportPromise;
  });

  it('should approve an intercompany transaction and reload', async () => {
    const { component } = await setupComponent();

    const approvePromise = component.approveIntercompany('tx-1');

    const approveReq = httpMock.expectOne('/api/v1/organization/intercompany/tx-1/approve');
    expect(approveReq.request.method).toBe('POST');
    approveReq.flush({ ...mockTxs[0], status: 'APPROVED' });

    await flushMicrotasks();

    // Reload calls triggered by approve
    flushLoad(mockHierarchy, mockConsolidation, [{ ...mockTxs[0], status: 'APPROVED' }]);

    await approvePromise;
  });

  it('should settle an intercompany transaction and reload', async () => {
    const { component } = await setupComponent();

    const settlePromise = component.settleIntercompany('tx-1');

    const settleReq = httpMock.expectOne('/api/v1/organization/intercompany/tx-1/settle');
    expect(settleReq.request.method).toBe('POST');
    settleReq.flush({ ...mockTxs[0], status: 'SETTLED' });

    await flushMicrotasks();

    // Reload calls triggered by settle
    flushLoad(mockHierarchy, mockConsolidation, [{ ...mockTxs[0], status: 'SETTLED' }]);

    await settlePromise;
  });

  it('should submit elimination and reload', async () => {
    const { component } = await setupComponent();

    component.openEliminationModal();
    expect(component.eliminationModalOpen()).toBe(true);

    component.eliminationForm.controls.period.setValue('2026-Q3');
    const elimPromise = component.submitElimination();

    const elimReq = httpMock.expectOne('/api/v1/organization/intercompany/eliminate');
    expect(elimReq.request.method).toBe('POST');
    expect(elimReq.request.body).toEqual({ period: '2026-Q3' });
    elimReq.flush({ period: '2026-Q3', eliminatedCount: 1, eliminatedTotalAmount: 50000 });

    await flushMicrotasks();

    expect(component.eliminationModalOpen()).toBe(false);

    // Reload calls
    flushLoad(mockHierarchy, mockConsolidation, [{ ...mockTxs[0], status: 'ELIMINATED', eliminatedInPeriod: '2026-Q3' }]);

    await elimPromise;
  });
});


