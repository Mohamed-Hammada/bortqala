import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { SalesOrder, SalesPage } from './sales.page';

describe('SalesPage accounts receivable and quotations', () => {
  let fixture: ComponentFixture<SalesPage>;
  let component: SalesPage;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalesPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SalesPage);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    flushLoad();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('loads aging invoices receipts and collection work in one workbench', () => {
    expect(component.aging()?.total).toBe(500);
    expect(component.invoices()).toHaveLength(1);
    expect(component.openInvoiceCount()).toBe(1);
    expect(component.collections()).toHaveLength(1);
    expect(component.quotations()).toHaveLength(1);
  });

  it('creates a sales quotation and reloads backend data', async () => {
    component.openQuoteModal();
    expect(component.quoteDrawerOpen()).toBe(true);

    component.quoteForm.setValue({
      customerId: 'CUST-01',
      quoteDate: '2026-08-19',
      validUntil: '2026-09-19',
      termsAndConditions: 'Net 30 days',
    });
    component.quoteLines.set([
      { itemId: 'item-1', quantity: 5, unitPrice: 100, discountAmount: 0, taxAmount: 70, notes: 'Quote line 1' }
    ]);

    const promise = component.submitQuote();
    const req = http.expectOne('/api/v1/trade/sales/quotations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.customerId).toBe('CUST-01');
    req.flush({ id: 'quo-1', quotationNumber: 'QUO-2026-001' });

    await Promise.resolve();
    await Promise.resolve();
    flushLoad();
    await promise;
  });

  it('creates an invoice with epoch dates and reloads backend calculations', async () => {
    component.invoiceForm.setValue({
      invoiceNumber: 'INV-2',
      customerId: 'customer-1',
      invoiceDate: '2026-08-09',
      dueDate: '2026-09-08',
      currencyCode: 'EGP',
      amount: 250,
    });
    const promise = component.createInvoice();
    const request = http.expectOne('/api/v1/trade/sales/receivables/invoices');
    expect(request.request.body.amount).toBe(250);
    expect(request.request.body.invoiceDate).toBe(new Date('2026-08-09').getTime());
    request.flush({});
    await Promise.resolve();
    await Promise.resolve();
    flushLoad();
    await promise;
  });

  it('records one optional invoice allocation with a retry-safe operation id', async () => {
    component.receiptForm.setValue({
      receiptNumber: 'RC-2',
      customerId: 'customer-1',
      receiptDate: '2026-08-09',
      currencyCode: 'EGP',
      amount: 200,
      invoiceId: 'invoice-1',
      allocationAmount: 150,
    });
    const promise = component.recordReceipt();
    const request = http.expectOne('/api/v1/trade/sales/receivables/receipts');
    expect(request.request.body.operationId).toBeTruthy();
    expect(request.request.body.allocations).toEqual([{ invoiceId: 'invoice-1', amount: 150 }]);
    request.flush({});
    await Promise.resolve();
    await Promise.resolve();
    flushLoad();
    await promise;
  });

  it('creates an order with typed lines warehouse and server-derived pricing inputs', async () => {
    component.soForm.setValue({
      soNumber: 'SO-2',
      soDate: '2026-08-09',
      customerId: 'customer-1',
      totalAmount: 999,
      warehouseId: 'wh-1',
      currencyCode: 'EGP',
    });
    component.orderLines.set([{ itemId: 'item-1', itemName: 'Item', quantity: 2, unitPrice: 100, discountRate: 10 }]);
    const promise = component.submitSo();
    const request = http.expectOne('/api/v1/trade/sales/orders');
    expect(request.request.body.warehouseId).toBe('wh-1');
    expect(request.request.body.lines).toEqual([{ itemId: 'item-1', itemName: 'Item', quantity: 2, unitPrice: 100, discountRate: 10 }]);
    request.flush({});
    await Promise.resolve();
    await Promise.resolve();
    flushLoad();
    await promise;
  });

  it('executes delivery with an operation id and loads its downstream references', async () => {
    const so = {
      id: 'so-1',
      soNumber: 'SO-1',
      soDate: 1,
      customerId: 'customer-1',
      status: 'CONFIRMED' as const,
      totalAmount: 100,
      warehouseId: 'wh-1',
      currencyCode: 'EGP',
      lines: [],
      createdAt: 1,
      updatedAt: 1,
    };
    const promise = component.deliverOrder(so);
    const request = http.expectOne('/api/v1/trade/sales/orders/so-1/deliveries');
    expect(request.request.body.operationId).toBeTruthy();
    request.flush({});
    await Promise.resolve();
    await Promise.resolve();
    flushLoad([{ ...so, status: 'DELIVERED' }]);
    await new Promise(resolve => setTimeout(resolve, 0));
    http.expectOne('/api/v1/trade/sales/orders/so-1/deliveries').flush([
      { id: 'd1', deliveryNumber: 'DN-1', salesOrderId: 'so-1', deliveryDate: 1, warehouseId: 'wh-1', invoiceId: 'i1', invoiceNumber: 'INV-1', status: 'DELIVERED', lines: [] },
    ]);
    http.expectOne('/api/v1/trade/sales/orders/so-1/returns').flush([]);
    await promise;
    expect(component.deliveriesByOrder()['so-1'][0].invoiceNumber).toBe('INV-1');
  });

  it('submits a partial return against the selected delivery line', async () => {
    const so = {
      id: 'so-1',
      soNumber: 'SO-1',
      soDate: 1,
      customerId: 'customer-1',
      status: 'DELIVERED' as const,
      totalAmount: 100,
      warehouseId: 'wh-1',
      currencyCode: 'EGP',
      lines: [],
      createdAt: 1,
      updatedAt: 1,
    };
    component.fulfillmentOrder.set(so);
    component.returnForm.setValue({ deliveryId: 'd1', deliveryLineId: 'dl1', quantity: 1, reason: 'Damaged' });
    const promise = component.receiveReturn();
    const request = http.expectOne('/api/v1/trade/sales/orders/so-1/returns');
    expect(request.request.body.operationId).toBeTruthy();
    expect(request.request.body.lines).toEqual([{ deliveryLineId: 'dl1', quantity: 1, disposition: 'AVAILABLE' }]);
    request.flush({});
    await Promise.resolve();
    await Promise.resolve();
    flushLoad([so]);
    await new Promise(resolve => setTimeout(resolve, 0));
    http.expectOne('/api/v1/trade/sales/orders/so-1/deliveries').flush([]);
    http.expectOne('/api/v1/trade/sales/orders/so-1/returns').flush([
      { id: 'r1', returnNumber: 'RET-1', deliveryId: 'd1', creditNoteId: 'c1', creditNoteNumber: 'CN-1', status: 'REFUNDED' },
    ]);
    await promise;
    expect(component.returnsByOrder()['so-1'][0].creditNoteNumber).toBe('CN-1');
  });

  it('loads the commission statement with payroll-sent metadata', async () => {
    component.statementRepId.set('emp-1');
    component.statementPeriod.set('2026-08');
    const promise = component.loadStatement();
    const request = http.expectOne(r => r.url === '/api/v1/sales/targets/commissions' && r.params.get('repId') === 'emp-1' && r.params.get('period') === '2026-08');
    expect(request.request.method).toBe('GET');
    request.flush({
      repId: 'emp-1',
      period: '2026-08',
      entries: [{ ruleId: 'r1', ruleName: 'Std 5%', basisAmount: 2000, percent: 5, commissionAmount: 100 }],
      totalCommission: 100,
      payrollSent: true,
      payrollSentAt: 1724600000000,
    });
    await promise;
    const stmt = component.commissionStatement();
    expect(stmt?.payrollSent).toBe(true);
    expect(stmt?.totalCommission).toBe(100);
  });

  it('downloads the commission statement export as an xlsx blob', async () => {
    component.statementRepId.set('emp-1');
    component.statementPeriod.set('2026-08');
    const promise = component.exportStatement();
    const request = http.expectOne(r => r.url === '/api/v1/sales/targets/commissions/export.xlsx' && r.params.get('repId') === 'emp-1' && r.params.get('period') === '2026-08');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['PK\u0003\u0004'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
    await promise;
  });

  it('sends the statement to payroll once and blocks a second click', async () => {
    component.statementRepId.set('emp-1');
    component.statementPeriod.set('2026-08');
    component.commissionStatement.set({
      repId: 'emp-1',
      period: '2026-08',
      entries: [],
      totalCommission: 0,
      payrollSent: false,
      payrollSentAt: null,
    });
    const promise = component.sendToPayroll();
    const request = http.expectOne('/api/v1/sales/targets/commissions/send-to-payroll');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ repId: 'emp-1', period: '2026-08' });
    request.flush({ repId: 'emp-1', period: '2026-08', totalCommission: 0, alreadySent: false, sentAt: 1724600000000 });
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne(r => r.url === '/api/v1/sales/targets/commissions' && r.params.get('repId') === 'emp-1' && r.params.get('period') === '2026-08').flush({
      repId: 'emp-1',
      period: '2026-08',
      entries: [],
      totalCommission: 0,
      payrollSent: true,
      payrollSentAt: 1724600000000,
    });
    await promise;
    expect(component.sendingToPayroll()).toBe(false);

    component.commissionStatement.set({
      repId: 'emp-1',
      period: '2026-08',
      entries: [],
      totalCommission: 0,
      payrollSent: true,
      payrollSentAt: 1724600000000,
    });
    component.activeMainTab.set('COMMISSIONS');
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button[disabled]');
    expect(button).toBeTruthy();
  });

  function flushLoad(orders: SalesOrder[] = []) {
    http.expectOne('/api/v1/trade/sales/orders').flush(orders);
    http.expectOne('/api/v1/trade/sales/quotations').flush([
      {
        id: 'quo-1',
        quotationNumber: 'QUO-2026-001',
        customerId: 'CUST-01',
        customerName: 'شركة النيل',
        quoteDate: '2026-08-19',
        validUntil: '2026-09-19',
        subtotal: 1000,
        discountAmount: 0,
        taxAmount: 140,
        totalAmount: 1140,
        status: 'DRAFT',
        lines: [],
        createdAt: Date.now(),
        updatedAt: Date.now(),
        version: 0,
      }
    ]);
    http.expectOne('/api/v1/trade/sales/receivables/invoices').flush([
      { id: 'invoice-1', invoiceNumber: 'INV-1', customerId: 'customer-1', invoiceDate: 1, dueDate: 2, currencyCode: 'EGP', amount: 500, outstandingAmount: 500, status: 'OPEN', version: 0 },
    ]);
    http.expectOne('/api/v1/trade/sales/receivables/receipts').flush([]);
    http.expectOne(r => r.url === '/api/v1/trade/sales/receivables/aging' && r.params.has('asOf')).flush({ asOf: 1, current: 0, days1To30: 500, days31To60: 0, days61To90: 0, over90: 0, total: 500 });
    http.expectOne(r => r.url === '/api/v1/trade/sales/receivables/collections' && r.params.has('asOf')).flush([
      { id: 'task-1', invoiceNumber: 'INV-1', customerId: 'customer-1', outstandingAmount: 500, dueDate: 2, daysOverdue: 10, status: 'OPEN', nextActionDate: 0, version: 0 },
    ]);
  }
});
