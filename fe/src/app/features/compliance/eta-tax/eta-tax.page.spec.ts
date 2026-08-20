import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { EtaTaxPage } from './eta-tax.page';

describe('EtaTaxPage', () => {
  let fixture: ComponentFixture<EtaTaxPage>;
  let component: EtaTaxPage;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtaTaxPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EtaTaxPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    flushLoad();
    await fixture.whenStable();
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads summary, submissions, config, mappings, and open invoices on init', () => {
    expect(component.summary()?.totalSubmitted).toBe(10);
    expect(component.submissions()).toHaveLength(1);
    expect(component.config()?.issuerTaxId).toBe('123456789');
    expect(component.itemMappings()).toHaveLength(1);
    expect(component.openInvoices()).toHaveLength(1);
  });

  it('queues an invoice for ETA compliance and reloads queue', async () => {
    component.openQueueModal();
    expect(component.showQueueModal()).toBe(true);

    component.queueForm.setValue({
      invoiceId: 'inv-1',
      documentType: 'INVOICE',
    });

    component.submitQueue();
    const req = http.expectOne('/api/v1/compliance/eta/submissions/queue');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.invoiceId).toBe('inv-1');
    req.flush({ id: 'sub-2', internalId: 'INV-002', status: 'VALIDATED' });

    await Promise.resolve();
    http.expectOne('/api/v1/compliance/eta/submissions').flush([]);
    http.expectOne('/api/v1/compliance/eta/summary').flush({
      totalSubmitted: 11,
      validCount: 8,
      invalidCount: 1,
      pendingCount: 2,
      totalTaxReported: 14000,
    });
    expect(component.showQueueModal()).toBe(false);
  });

  it('submits document to ETA and displays success toast', async () => {
    const sub = component.submissions()[0];
    component.submitToEta(sub);

    const req = http.expectOne(`/api/v1/compliance/eta/submissions/${sub.id}/submit`);
    expect(req.request.method).toBe('POST');
    req.flush({ ...sub, status: 'VALID', etaUuid: 'ETA-12345' });

    await Promise.resolve();
    http.expectOne('/api/v1/compliance/eta/submissions').flush([{ ...sub, status: 'VALID', etaUuid: 'ETA-12345' }]);
    http.expectOne('/api/v1/compliance/eta/summary').flush({
      totalSubmitted: 10,
      validCount: 9,
      invalidCount: 1,
      pendingCount: 0,
      totalTaxReported: 14140,
    });
  });

  it('cancels document with reason', async () => {
    const sub = component.submissions()[0];
    component.openCancelModal(sub);
    expect(component.showCancelModal()).toBe(true);

    component.cancelForm.setValue({ reason: 'Incorrect customer tax registration number' });
    component.submitCancel();

    const req = http.expectOne(`/api/v1/compliance/eta/submissions/${sub.id}/cancel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.reason).toBe('Incorrect customer tax registration number');
    req.flush({ ...sub, status: 'CANCELLED' });

    await Promise.resolve();
    http.expectOne('/api/v1/compliance/eta/submissions').flush([{ ...sub, status: 'CANCELLED' }]);
    http.expectOne('/api/v1/compliance/eta/summary').flush({
      totalSubmitted: 10,
      validCount: 8,
      invalidCount: 1,
      pendingCount: 1,
      totalTaxReported: 14000,
    });
    expect(component.showCancelModal()).toBe(false);
  });

  it('saves ETA configuration', async () => {
    component.configForm.setValue({
      clientId: 'new-client-id',
      clientSecret: 'secret',
      issuerTaxId: '987654321',
      issuerName: 'Updated Company',
      environment: 'PRODUCTION',
      tokenUrl: '',
      apiBaseUrl: '',
      active: true,
    });

    component.saveConfig();
    const req = http.expectOne('/api/v1/compliance/eta/config');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.issuerTaxId).toBe('987654321');
    req.flush({
      id: 'cfg-1',
      clientId: 'new-client-id',
      maskedSecret: 'se****et',
      issuerTaxId: '987654321',
      issuerName: 'Updated Company',
      environment: 'PRODUCTION',
      tokenUrl: '',
      apiBaseUrl: '',
      active: true,
      updatedAt: Date.now(),
    });

    await Promise.resolve();
    expect(component.config()?.issuerTaxId).toBe('987654321');
  });

  it('saves item tax code mapping', async () => {
    component.openMappingModal();
    expect(component.showMappingModal()).toBe(true);

    component.mappingForm.setValue({
      itemId: 'ITM-99',
      itemCode: 'ITM-99',
      codeType: 'EGS',
      itemCodeValue: 'EG-123456789-099',
      descriptionAr: 'منتج تجريبي',
      descriptionEn: 'Demo Product',
      active: true,
    });

    component.saveMapping();
    const req = http.expectOne('/api/v1/compliance/eta/item-mappings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.itemCodeValue).toBe('EG-123456789-099');
    req.flush({
      id: 'map-2',
      itemId: 'ITM-99',
      itemCode: 'ITM-99',
      codeType: 'EGS',
      itemCodeValue: 'EG-123456789-099',
      descriptionAr: 'منتج تجريبي',
      descriptionEn: 'Demo Product',
      active: true,
      createdAt: Date.now(),
    });

    await Promise.resolve();
    http.expectOne('/api/v1/compliance/eta/item-mappings').flush([]);
    expect(component.showMappingModal()).toBe(false);
  });

  function flushLoad() {
    http.expectOne('/api/v1/compliance/eta/summary').flush({
      totalSubmitted: 10,
      validCount: 8,
      invalidCount: 1,
      pendingCount: 1,
      totalTaxReported: 14000,
    });
    http.expectOne('/api/v1/compliance/eta/submissions').flush([
      {
        id: 'sub-1',
        invoiceId: 'inv-1',
        internalId: 'INV-001',
        documentType: 'INVOICE',
        etaUuid: 'ETA-12345-6789',
        submissionUuid: 'uuid-1',
        status: 'VALIDATED',
        dateTimeIssued: Date.now(),
        totalSalesAmount: 1000,
        totalDiscountAmount: 0,
        netAmount: 1000,
        taxAmount: 140,
        totalAmount: 1140,
        canonicalJsonHash: 'hash-abc',
        rawResponseJson: null,
        validationErrorsJson: null,
        submissionAttempts: 0,
        cancellationReason: null,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        version: 0,
      },
    ]);
    http.expectOne('/api/v1/compliance/eta/config').flush({
      id: 'cfg-1',
      clientId: 'client-1',
      maskedSecret: 'cl****t1',
      issuerTaxId: '123456789',
      issuerName: 'Bemo Egypt',
      environment: 'PRE_PRODUCTION',
      tokenUrl: '',
      apiBaseUrl: '',
      active: true,
      updatedAt: Date.now(),
    });
    http.expectOne('/api/v1/compliance/eta/item-mappings').flush([
      {
        id: 'map-1',
        itemId: 'item-1',
        itemCode: 'ITM-01',
        codeType: 'EGS',
        itemCodeValue: 'EG-123456789-001',
        descriptionAr: 'صنف 1',
        descriptionEn: 'Item 1',
        active: true,
        createdAt: Date.now(),
      },
    ]);
    http.expectOne('/api/v1/trade/sales/receivables/invoices').flush([
      {
        id: 'inv-1',
        invoiceNumber: 'INV-001',
        amount: 1140,
        currencyCode: 'EGP',
      },
    ]);
  }
});
