import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { ProductionPage } from './production.page';

describe('ProductionPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProductionPage,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key, locale: () => 'en-US' } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads manufacturing boms, orders, work centers, routings and quality on init', async () => {
    const page = TestBed.inject(ProductionPage);

    http.expectOne('/api/v1/manufacturing/boms').flush([
      { id: 'bom-1', bomCode: 'BOM-101', finishedGoodName: 'Steel Frame', yieldQuantity: 1, active: true },
    ]);
    http.expectOne('/api/v1/manufacturing/orders').flush([
      { id: 'ord-1', orderNumber: 'MO-1001', bomId: 'bom-1', targetQuantity: 10, startDate: 1000, status: 'PLANNED', createdAt: 1000, updatedAt: 1000 },
    ]);
    http.expectOne('/api/v1/manufacturing/work-centers').flush([
      { id: 'wc-1', code: 'WC-01', name: 'Cutting Station', hourlyRate: 60, capacityHoursPerDay: 8, active: true },
    ]);
    http.expectOne('/api/v1/manufacturing/routings').flush([
      { id: 'rt-1', routingCode: 'RT-101', name: 'Standard Cutting', itemId: 'bom-1', active: true },
    ]);
    http.expectOne('/api/v1/manufacturing/quality').flush([
      { id: 'qc-1', inspectionNumber: 'QC-101', productionOrderId: 'ord-1', inspectorId: 'QA', result: 'PASSED', createdAt: 1000 },
    ]);

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    expect(page.boms().length).toBe(1);
    expect(page.orders().length).toBe(1);
    expect(page.workCenters().length).toBe(1);
    expect(page.routings().length).toBe(1);
    expect(page.inspections().length).toBe(1);
  });

  it('switches tabs and opens new item drawer', async () => {
    const page = TestBed.inject(ProductionPage);
    flushInit(http);

    page.activeTab.set('workCenters');
    page.openNew();
    expect(page.drawerOpen()).toBe(true);
    expect(page.workCenterForm.controls.hourlyRate.value).toBe(50);
  });

  function flushInit(mockHttp: HttpTestingController) {
    mockHttp.expectOne('/api/v1/manufacturing/boms').flush([]);
    mockHttp.expectOne('/api/v1/manufacturing/orders').flush([]);
    mockHttp.expectOne('/api/v1/manufacturing/work-centers').flush([]);
    mockHttp.expectOne('/api/v1/manufacturing/routings').flush([]);
    mockHttp.expectOne('/api/v1/manufacturing/quality').flush([]);
  }
});
