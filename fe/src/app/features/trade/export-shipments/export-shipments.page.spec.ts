import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExportShipmentsPage } from './export-shipments.page';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';

describe('ExportShipmentsPage', () => {
  let fixture: ComponentFixture<ExportShipmentsPage>;
  let component: ExportShipmentsPage;
  let httpMock: HttpTestingController;

  function flushInit() {
    const req = httpMock.match(() => true);
    req.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExportShipmentsPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportShipmentsPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    flushInit();
    fixture.detectChanges();
  });

  afterEach(() => {
    flushInit();
  });

  it('should create and load shipments on init', () => {
    expect(component.shipments()).toEqual([]);
    expect(component.loading()).toBeFalsy();
  });

  it('should display shipments table', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('table')).toBeTruthy();
    expect(compiled.querySelector('.empty')).toBeTruthy();
  });

  it('should transition shipment status', async () => {
    component.transition('sh-1', 'BOOKED');

    const transReq = httpMock.expectOne('/api/v1/trade/export-shipments/sh-1/transition?status=BOOKED');
    expect(transReq.request.method).toBe('POST');
    transReq.flush({});

    await new Promise((r) => setTimeout(r, 0));

    const reloadReq = httpMock.expectOne('/api/v1/trade/export-shipments');
    reloadReq.flush([]);
  });

  it('should return next action for status transitions', () => {
    expect(component.nextAction('PREPARING')).toBe('BOOKED');
    expect(component.nextAction('BOOKED')).toBe('SHIPPED');
    expect(component.nextAction('SHIPPED')).toBe('SETTLED');
    expect(component.nextAction('SETTLED')).toBeNull();
  });

  it('should add and remove shipment lines', () => {
    expect(component.linesForm()).toEqual([]);

    component.addLine();
    expect(component.linesForm().length).toBe(1);

    component.addLine();
    expect(component.linesForm().length).toBe(2);

    component.removeLine(0);
    expect(component.linesForm().length).toBe(1);
  });

  it('should compute total lines quantity', () => {
    expect(component.totalLinesQuantity()).toBe(0);

    component.linesForm.set([
      { itemName: 'Tomatoes', itemCode: '', lotReference: '', quantity: 100, unitOfMeasure: 'KG', packagesCount: 0 },
      { itemName: 'Cucumbers', itemCode: '', lotReference: '', quantity: 50, unitOfMeasure: 'KG', packagesCount: 0 },
    ]);

    expect(component.totalLinesQuantity()).toBe(150);
  });

  it('should switch to DOCS tab and show buttons when a shipment is selected', () => {
    component.selectedShipment.set({
      id: 'sh-1', shipmentNumber: 'EXP-001', customerPartyId: 'p1', customerPartyName: 'Acme',
      status: 'BOOKED', daysOutstanding: 2, lines: [], createdAt: 0, updatedAt: 0,
    });
    component.switchTab('DOCS');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const buttons = compiled.querySelectorAll('.doc-buttons .button');
    expect(buttons.length).toBe(3);
  });

  it('should warn when downloading a document without a selected shipment', async () => {
    component.selectedShipment.set(null);
    await component.downloadDoc('coo');
    const notif = TestBed.inject(NotificationService);
    expect(notif.error).toHaveBeenCalled();
  });

  it('should download document blob for a selected shipment', async () => {
    component.selectedShipment.set({
      id: 'sh-1', shipmentNumber: 'EXP-001', customerPartyId: 'p1', customerPartyName: 'Acme',
      status: 'BOOKED', daysOutstanding: 2, lines: [], createdAt: 0, updatedAt: 0,
    });
    const blob = new Blob(['pk'], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const download = component.downloadDoc('coo');

    const req = httpMock.expectOne('/api/v1/trade/export-shipments/sh-1/docs/coo.xlsx');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);

    await download;
    expect(httpMock.match(() => true)).toEqual([]);
  });
});
