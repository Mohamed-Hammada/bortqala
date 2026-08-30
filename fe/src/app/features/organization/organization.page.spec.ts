import { TestBed } from '@angular/core/testing';
import { OrganizationPage } from './organization.page';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ConsolidatedOrganizationSummary, IntercompanyTransaction, OrganizationHierarchy } from './organization.models';

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  async function setupComponent() {
    const fixture = TestBed.createComponent(OrganizationPage);
    const component = fixture.componentInstance;

    // Flush load() HTTP calls
    const reqHierarchy = httpMock.expectOne('/api/v1/organization');
    expect(reqHierarchy.request.method).toBe('GET');
    reqHierarchy.flush(mockHierarchy);

    const reqConsolidation = httpMock.expectOne('/api/v1/organization/consolidation/summary');
    expect(reqConsolidation.request.method).toBe('GET');
    reqConsolidation.flush(mockConsolidation);

    const reqIntercompany = httpMock.expectOne('/api/v1/organization/intercompany');
    expect(reqIntercompany.request.method).toBe('GET');
    reqIntercompany.flush(mockTxs);

    await Promise.resolve();

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

  it('should switch tabs properly', async () => {
    const { component } = await setupComponent();
    component.activeTab.set('consolidation');
    expect(component.activeTab()).toBe('consolidation');

    component.activeTab.set('intercompany');
    expect(component.activeTab()).toBe('intercompany');
  });

  it('should approve an intercompany transaction and reload', async () => {
    const { component } = await setupComponent();

    const approvePromise = component.approveIntercompany('tx-1');

    const approveReq = httpMock.expectOne('/api/v1/organization/intercompany/tx-1/approve');
    expect(approveReq.request.method).toBe('POST');
    approveReq.flush({ ...mockTxs[0], status: 'APPROVED' });

    await Promise.resolve();

    // Reload calls triggered by approve
    const reqHierarchy = httpMock.expectOne('/api/v1/organization');
    reqHierarchy.flush(mockHierarchy);
    const reqConsolidation = httpMock.expectOne('/api/v1/organization/consolidation/summary');
    reqConsolidation.flush(mockConsolidation);
    const reqIntercompany = httpMock.expectOne('/api/v1/organization/intercompany');
    reqIntercompany.flush([{ ...mockTxs[0], status: 'APPROVED' }]);

    await approvePromise;
  });

  it('should settle an intercompany transaction and reload', async () => {
    const { component } = await setupComponent();

    const settlePromise = component.settleIntercompany('tx-1');

    const settleReq = httpMock.expectOne('/api/v1/organization/intercompany/tx-1/settle');
    expect(settleReq.request.method).toBe('POST');
    settleReq.flush({ ...mockTxs[0], status: 'SETTLED' });

    await Promise.resolve();

    // Reload calls triggered by settle
    const reqHierarchy = httpMock.expectOne('/api/v1/organization');
    reqHierarchy.flush(mockHierarchy);
    const reqConsolidation = httpMock.expectOne('/api/v1/organization/consolidation/summary');
    reqConsolidation.flush(mockConsolidation);
    const reqIntercompany = httpMock.expectOne('/api/v1/organization/intercompany');
    reqIntercompany.flush([{ ...mockTxs[0], status: 'SETTLED' }]);

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

    await Promise.resolve();

    expect(component.eliminationModalOpen()).toBe(false);

    // Reload calls
    const reqHierarchy = httpMock.expectOne('/api/v1/organization');
    reqHierarchy.flush(mockHierarchy);
    const reqConsolidation = httpMock.expectOne('/api/v1/organization/consolidation/summary');
    reqConsolidation.flush(mockConsolidation);
    const reqIntercompany = httpMock.expectOne('/api/v1/organization/intercompany');
    reqIntercompany.flush([{ ...mockTxs[0], status: 'ELIMINATED', eliminatedInPeriod: '2026-Q3' }]);

    await elimPromise;
  });
});
