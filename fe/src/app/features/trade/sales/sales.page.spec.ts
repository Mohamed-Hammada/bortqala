import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { SalesPage } from './sales.page';

describe('SalesPage accounts receivable',()=>{
  let fixture:ComponentFixture<SalesPage>;let component:SalesPage;let http:HttpTestingController;
  beforeEach(async()=>{await TestBed.configureTestingModule({imports:[SalesPage],providers:[provideHttpClient(),provideHttpClientTesting(),
    {provide:I18nService,useValue:{t:(key:string)=>key}},{provide:NotificationService,useValue:{success:vi.fn(),error:vi.fn()}}]}).compileComponents();
    fixture=TestBed.createComponent(SalesPage);component=fixture.componentInstance;http=TestBed.inject(HttpTestingController);flushLoad();await fixture.whenStable();fixture.detectChanges();});
  afterEach(()=>{try{http.verify();}finally{TestBed.resetTestingModule();}});

  it('loads aging invoices receipts and collection work in one workbench',()=>{expect(component.aging()?.total).toBe(500);expect(component.invoices()).toHaveLength(1);expect(component.openInvoiceCount()).toBe(1);expect(component.collections()).toHaveLength(1);});

  it('creates an invoice with epoch dates and reloads backend calculations',async()=>{component.invoiceForm.setValue({invoiceNumber:'INV-2',customerId:'customer-1',invoiceDate:'2026-08-09',dueDate:'2026-09-08',currencyCode:'EGP',amount:250});
    const promise=component.createInvoice();const request=http.expectOne('/api/v1/trade/sales/receivables/invoices');expect(request.request.body.amount).toBe(250);expect(request.request.body.invoiceDate).toBe(new Date('2026-08-09').getTime());request.flush({});
    await Promise.resolve();await Promise.resolve();flushLoad();await promise;});

  it('records one optional invoice allocation with a retry-safe operation id',async()=>{component.receiptForm.setValue({receiptNumber:'RC-2',customerId:'customer-1',receiptDate:'2026-08-09',currencyCode:'EGP',amount:200,invoiceId:'invoice-1',allocationAmount:150});
    const promise=component.recordReceipt();const request=http.expectOne('/api/v1/trade/sales/receivables/receipts');expect(request.request.body.operationId).toBeTruthy();expect(request.request.body.allocations).toEqual([{invoiceId:'invoice-1',amount:150}]);request.flush({});
    await Promise.resolve();await Promise.resolve();flushLoad();await promise;});

  function flushLoad(){http.expectOne('/api/v1/trade/sales/orders').flush([]);http.expectOne('/api/v1/trade/sales/receivables/invoices').flush([{id:'invoice-1',invoiceNumber:'INV-1',customerId:'customer-1',invoiceDate:1,dueDate:2,currencyCode:'EGP',amount:500,outstandingAmount:500,status:'OPEN',version:0}]);
    http.expectOne('/api/v1/trade/sales/receivables/receipts').flush([]);http.expectOne('/api/v1/trade/sales/receivables/aging').flush({asOf:1,current:0,days1To30:500,days31To60:0,days61To90:0,over90:0,total:500});
    http.expectOne('/api/v1/trade/sales/receivables/collections').flush([{id:'task-1',invoiceNumber:'INV-1',customerId:'customer-1',outstandingAmount:500,dueDate:2,daysOverdue:10,status:'OPEN',nextActionDate:0,version:0}]);}
});
