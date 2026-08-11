import { TestBed } from '@angular/core/testing';
import { WorkforceService } from './data-access/workforce.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('WorkforceService', () => {
  let service: WorkforceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(WorkforceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should post contractor settlement to finance', () => {
    const payload = { operationId: 'op-123', expectedVersion: 1, reason: 'Testing posting' };
    service.postSettlementToFinance('settl-1', payload).subscribe(res => {
      expect(res.id).toBe('settl-1');
      expect(res.status).toBe('POSTED');
    });

    const req = httpMock.expectOne('/api/v1/workforce/settlements/contractor-settlements/settl-1/post');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 'settl-1', status: 'POSTED' });
  });

  it('should link invoice to contractor settlement', () => {
    const payload = { invoiceNumber: 'INV-999', invoiceDate: 1700000000000, invoiceAmount: 5000 };
    service.linkSettlementInvoice('settl-1', payload).subscribe(res => {
      expect(res.invoiceNumber).toBe('INV-999');
    });

    const req = httpMock.expectOne('/api/v1/workforce/settlements/contractor-settlements/settl-1/link-invoice');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 'settl-1', invoiceNumber: 'INV-999' });
  });

  it('should record payment against contractor settlement', () => {
    const payload = { operationId: 'op-456', amount: 2500, paymentDate: 1700000000000, paymentReference: 'REF-123' };
    service.recordSettlementPayment('settl-1', payload).subscribe(res => {
      expect(res.paidAmount).toBe(2500);
      expect(res.status).toBe('PAID');
    });

    const req = httpMock.expectOne('/api/v1/workforce/settlements/contractor-settlements/settl-1/mark-paid');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 'settl-1', paidAmount: 2500, status: 'PAID' });
  });
});
