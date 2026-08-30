import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LaptopRetailService } from './laptop-retail.service';

describe('LaptopRetailService', () => {
  let service: LaptopRetailService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), LaptopRetailService],
    });
    service = TestBed.inject(LaptopRetailService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listDevices sends GET with status and brand params', () => {
    service.listDevices({ status: 'IN_STOCK', brand: 'Lenovo' }).subscribe((devices) => {
      expect(devices.length).toBe(1);
      expect(devices[0].serialNumber).toBe('SN-123');
    });

    const req = httpMock.expectOne((r) =>
      r.url === '/api/v1/retail/laptops/devices' &&
      r.params.get('status') === 'IN_STOCK' &&
      r.params.get('brand') === 'Lenovo'
    );
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 'd1',
      serialNumber: 'SN-123',
      brand: 'Lenovo',
      model: 'ThinkPad T14',
      cpu: 'i7',
      ramGb: 16,
      storageGb: 512,
      storageType: 'SSD',
      conditionGrade: 'NEW',
      purchasePrice: 25000,
      sellingPrice: 32000,
      margin: 7000,
      status: 'IN_STOCK',
      isWarrantyActive: false,
    }]);
  });

  it('sellDevice posts customer and warranty info', () => {
    service.sellDevice('d1', {
      customerId: 'c1',
      customerName: 'Mohamed',
      warrantyMonths: 12,
      finalSellingPrice: 31000,
    }).subscribe((device) => {
      expect(device.status).toBe('SOLD');
      expect(device.customerName).toBe('Mohamed');
    });

    const req = httpMock.expectOne('/api/v1/retail/laptops/devices/d1/sell');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.customerName).toBe('Mohamed');
    req.flush({
      id: 'd1',
      serialNumber: 'SN-123',
      brand: 'Lenovo',
      model: 'ThinkPad T14',
      cpu: 'i7',
      ramGb: 16,
      storageGb: 512,
      storageType: 'SSD',
      conditionGrade: 'NEW',
      purchasePrice: 25000,
      sellingPrice: 31000,
      margin: 6000,
      status: 'SOLD',
      customerName: 'Mohamed',
      isWarrantyActive: true,
    });
  });

  it('createRepairTicket posts ticket request', () => {
    service.createRepairTicket({
      serialNumber: 'SN-123',
      customerName: 'Mohamed',
      customerPhone: '01000000000',
      reportedIssue: 'Fan noise',
    }).subscribe((ticket) => {
      expect(ticket.ticketNumber).toBe('RPR-001');
      expect(ticket.isUnderWarranty).toBe(true);
    });

    const req = httpMock.expectOne('/api/v1/retail/laptops/repairs');
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 't1',
      ticketNumber: 'RPR-001',
      serialNumber: 'SN-123',
      customerName: 'Mohamed',
      customerPhone: '01000000000',
      reportedIssue: 'Fan noise',
      costAmount: 0,
      chargedAmount: 0,
      status: 'RECEIVED',
      isUnderWarranty: true,
      createdAt: '2026-08-30T10:00:00Z',
    });
  });
});
