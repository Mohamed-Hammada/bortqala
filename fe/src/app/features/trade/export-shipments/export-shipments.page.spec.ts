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
});
