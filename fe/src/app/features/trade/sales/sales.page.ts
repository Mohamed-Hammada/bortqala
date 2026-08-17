import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { formatDate } from '../../../core/date';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface SalesOrder {
  id: string;
  soNumber: string;
  soDate: number;
  customerId: string;
  quotationId?: string;
  status: 'DRAFT' | 'CONFIRMED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  warehouseId: string;
  currencyCode: string;
  lines: SalesOrderLine[];
  createdAt: number;
  updatedAt: number;
}

interface SalesOrderLine { id:string;itemId:string;itemName:string;orderedQuantity:number;deliveredQuantity:number;unitPrice:number;discountRate:number;netPrice:number;lineTotal:number; }
interface SalesDeliveryLine { id:string;salesOrderLineId:string;itemId:string;quantity:number;unitPrice:number;stockMovementId:string;unitCogs:number;cogsAmount:number; }
interface SalesDelivery { id:string;deliveryNumber:string;salesOrderId:string;deliveryDate:number;warehouseId:string;invoiceId:string;invoiceNumber:string;status:string;lines:SalesDeliveryLine[]; }
interface CustomerReturn { id:string;returnNumber:string;deliveryId:string;creditNoteId:string;creditNoteNumber:string;status:string; }

interface CustomerInvoice { id:string; invoiceNumber:string; customerId:string; invoiceDate:number; dueDate:number; currencyCode:string; amount:number; outstandingAmount:number; status:string; version:number; }
interface CustomerReceipt { id:string; receiptNumber:string; customerId:string; receiptDate:number; currencyCode:string; amount:number; unallocatedAmount:number; operationId:string; }
interface Aging { asOf:number; current:number; days1To30:number; days31To60:number; days61To90:number; over90:number; total:number; }
interface CollectionTask { id:string; invoiceNumber:string; customerId:string; outstandingAmount:number; dueDate:number; daysOverdue:number; status:string; ownerUserId?:string; nextActionDate:number; note?:string; version:number; }
interface CreditProfile { customerId:string; creditLimit:number; paymentTermsDays:number; creditHold:boolean; outstanding:number; available:number; version:number; }

@Component({
  selector: 'app-sales-page',
  imports: [ReactiveFormsModule, DecimalPipe, ModalDialogComponent],
  templateUrl: './sales.page.html',
  styleUrl: './sales.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalesPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly orders = signal<SalesOrder[]>([]);
  readonly invoices = signal<CustomerInvoice[]>([]);
  readonly receipts = signal<CustomerReceipt[]>([]);
  readonly aging = signal<Aging | null>(null);
  readonly collections = signal<CollectionTask[]>([]);
  readonly credit = signal<CreditProfile | null>(null);
  readonly receivablesTab = signal<'INVOICES'|'RECEIPTS'|'COLLECTIONS'|'CREDIT'>('INVOICES');
  readonly savingAr = signal(false);
  readonly arAsOfDate = signal(new Date().toISOString().slice(0, 10));
  readonly openInvoiceCount = computed(() => this.invoices().filter((row) => row.status === 'OPEN' || row.status === 'PARTIALLY_PAID').length);

  readonly drawerOpen = signal(false);
  readonly orderLines = signal<Array<{itemId:string;itemName:string;quantity:number;unitPrice:number;discountRate:number}>>([]);
  readonly deliveriesByOrder = signal<Record<string,SalesDelivery[]>>({});
  readonly returnsByOrder = signal<Record<string,CustomerReturn[]>>({});
  readonly fulfillmentOrder = signal<SalesOrder|null>(null);
  readonly savingFulfillment = signal(false);

  readonly soForm = new FormGroup({
    soNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    soDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    totalAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
    warehouseId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
  });
  readonly lineForm = new FormGroup({
    itemId:new FormControl('',{nonNullable:true,validators:[Validators.required]}),itemName:new FormControl('',{nonNullable:true,validators:[Validators.required]}),
    quantity:new FormControl(1,{nonNullable:true,validators:[Validators.required,Validators.min(.0001)]}),unitPrice:new FormControl(0,{nonNullable:true,validators:[Validators.required,Validators.min(.01)]}),
    discountRate:new FormControl(0,{nonNullable:true,validators:[Validators.required,Validators.min(0),Validators.max(100)]}),
  });
  readonly returnForm = new FormGroup({deliveryId:new FormControl('',{nonNullable:true,validators:[Validators.required]}),deliveryLineId:new FormControl('',{nonNullable:true,validators:[Validators.required]}),quantity:new FormControl(1,{nonNullable:true,validators:[Validators.required,Validators.min(.0001)]}),reason:new FormControl('',{nonNullable:true,validators:[Validators.required]})});
  readonly invoiceForm = new FormGroup({
    invoiceNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    invoiceDate: new FormControl(new Date().toISOString().slice(0,10), { nonNullable: true, validators: [Validators.required] }),
    dueDate: new FormControl('', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
  });
  readonly receiptForm = new FormGroup({
    receiptNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    receiptDate: new FormControl(new Date().toISOString().slice(0,10), { nonNullable: true, validators: [Validators.required] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    invoiceId: new FormControl('', { nonNullable: true }),
    allocationAmount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
  });
  readonly creditForm = new FormGroup({
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    creditLimit: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    paymentTermsDays: new FormControl(30, { nonNullable: true, validators: [Validators.min(0)] }),
    creditHold: new FormControl(false, { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [orders,invoices,receipts,aging,collections] = await Promise.all([
        firstValueFrom(this.http.get<SalesOrder[]>('/api/v1/trade/sales/orders')),
        firstValueFrom(this.http.get<CustomerInvoice[]>('/api/v1/trade/sales/receivables/invoices')),
        firstValueFrom(this.http.get<CustomerReceipt[]>('/api/v1/trade/sales/receivables/receipts')),
        firstValueFrom(this.http.get<Aging>('/api/v1/trade/sales/receivables/aging', { params: { asOf: this.businessDate() } })),
        firstValueFrom(this.http.get<CollectionTask[]>('/api/v1/trade/sales/receivables/collections', { params: { asOf: this.businessDate() } })),
      ]);
      this.orders.set(orders); this.invoices.set(invoices); this.receipts.set(receipts); this.aging.set(aging); this.collections.set(collections);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.soForm.reset({
      soNumber: 'SO-' + Math.floor(1000 + Math.random() * 9000),
      soDate: new Date().toISOString().substring(0, 10),
      customerId: 'CUST-01',
      totalAmount: 12000,
      warehouseId:'',currencyCode:'EGP',
    });
    this.orderLines.set([]);
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitSo() {
    if (this.soForm.invalid||this.orderLines().length===0) return;
    try {
      const val = this.soForm.getRawValue();
      const dateMs = new Date(val.soDate).getTime();
      const payload = {
        soNumber: val.soNumber,
        soDate: dateMs,
        customerId: val.customerId,
        totalAmount: val.totalAmount,
        warehouseId:val.warehouseId,currencyCode:val.currencyCode,lines:this.orderLines(),
      };
      await firstValueFrom(this.http.post('/api/v1/trade/sales/orders', payload));
      this.notification.success(this.i18n.t('sales.soCreated'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  businessDate(): number {
    const value = this.arAsOfDate();
    return new Date(`${value}T00:00:00Z`).getTime();
  }

  addOrderLine(){if(this.lineForm.invalid)return;const row=this.lineForm.getRawValue();if(this.orderLines().some(line=>line.itemId===row.itemId))return;this.orderLines.update(lines=>[...lines,row]);this.lineForm.reset({itemId:'',itemName:'',quantity:1,unitPrice:0,discountRate:0});}
  removeOrderLine(index:number){this.orderLines.update(lines=>lines.filter((_,i)=>i!==index));}

  async confirmSo(so: SalesOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/trade/sales/orders/${so.id}/confirm`, {}));
      this.notification.success(this.i18n.t('sales.soConfirmed'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async inspectFulfillment(so:SalesOrder){this.fulfillmentOrder.set(so);this.error.set(null);try{const [deliveries,returns]=await Promise.all([firstValueFrom(this.http.get<SalesDelivery[]>(`/api/v1/trade/sales/orders/${so.id}/deliveries`)),firstValueFrom(this.http.get<CustomerReturn[]>(`/api/v1/trade/sales/orders/${so.id}/returns`))]);this.deliveriesByOrder.update(rows=>({...rows,[so.id]:deliveries}));this.returnsByOrder.update(rows=>({...rows,[so.id]:returns}));const delivery=deliveries[0];const line=delivery?.lines?.[0];this.returnForm.reset({deliveryId:delivery?.id??'',deliveryLineId:line?.id??'',quantity:1,reason:''});}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}}
  async deliverOrder(so:SalesOrder){if(this.savingFulfillment())return;this.savingFulfillment.set(true);this.error.set(null);try{await firstValueFrom(this.http.post(`/api/v1/trade/sales/orders/${so.id}/deliveries`,{deliveryNumber:`DN-${Date.now()}`,deliveryDate:Date.now(),operationId:crypto.randomUUID()}));this.notification.success(this.i18n.t('sales.deliveryCreated'));await this.load();const refreshed=this.orders().find(row=>row.id===so.id);if(refreshed)await this.inspectFulfillment(refreshed);}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingFulfillment.set(false);}}
  async receiveReturn(){const so=this.fulfillmentOrder();if(!so||this.returnForm.invalid||this.savingFulfillment())return;const value=this.returnForm.getRawValue();this.savingFulfillment.set(true);this.error.set(null);try{await firstValueFrom(this.http.post(`/api/v1/trade/sales/orders/${so.id}/returns`,{returnNumber:`RET-${Date.now()}`,deliveryId:value.deliveryId,returnDate:Date.now(),reason:value.reason,operationId:crypto.randomUUID(),lines:[{deliveryLineId:value.deliveryLineId,quantity:value.quantity,disposition:'AVAILABLE'}]}));this.notification.success(this.i18n.t('sales.returnCreated'));await this.load();const refreshed=this.orders().find(row=>row.id===so.id);if(refreshed)await this.inspectFulfillment(refreshed);}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingFulfillment.set(false);}}

  async createInvoice() {
    if (this.invoiceForm.invalid || this.savingAr()) return;
    this.savingAr.set(true); this.error.set(null);
    try { const value=this.invoiceForm.getRawValue(); await firstValueFrom(this.http.post('/api/v1/trade/sales/receivables/invoices',{
      ...value,invoiceDate:new Date(value.invoiceDate).getTime(),dueDate:value.dueDate?new Date(value.dueDate).getTime():0,
    })); this.notification.success(this.i18n.t('sales.ar.invoiceCreated')); this.invoiceForm.reset({invoiceNumber:'',customerId:value.customerId,invoiceDate:new Date().toISOString().slice(0,10),dueDate:'',currencyCode:value.currencyCode,amount:0}); await this.load();
    } catch(e){this.error.set(apiErrorMessage(e,this.i18n));} finally{this.savingAr.set(false);}
  }

  async issueInvoice(invoice: CustomerInvoice) {
    if(this.savingAr())return;this.savingAr.set(true);this.error.set(null);
    try{await firstValueFrom(this.http.post(`/api/v1/trade/sales/receivables/invoices/${invoice.id}/issue`,{}));this.notification.success(this.i18n.t('sales.ar.invoiceIssued'));await this.load();}
    catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingAr.set(false);}
  }

  async recordReceipt() {
    if(this.receiptForm.invalid||this.savingAr())return;const value=this.receiptForm.getRawValue();
    if(value.allocationAmount>value.amount){this.error.set(this.i18n.t('sales.ar.allocationTooHigh'));return;}
    const allocations=value.invoiceId&&value.allocationAmount>0?[{invoiceId:value.invoiceId,amount:value.allocationAmount}]:[];
    this.savingAr.set(true);this.error.set(null);try{await firstValueFrom(this.http.post('/api/v1/trade/sales/receivables/receipts',{
      receiptNumber:value.receiptNumber,customerId:value.customerId,receiptDate:new Date(value.receiptDate).getTime(),currencyCode:value.currencyCode,amount:value.amount,operationId:crypto.randomUUID(),allocations,
    }));this.notification.success(this.i18n.t('sales.ar.receiptRecorded'));this.receiptForm.reset({receiptNumber:'',customerId:value.customerId,receiptDate:new Date().toISOString().slice(0,10),currencyCode:value.currencyCode,amount:0,invoiceId:'',allocationAmount:0});await this.load();}
    catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingAr.set(false);}
  }

  async loadCredit(){if(this.creditForm.controls.customerId.invalid)return;this.error.set(null);try{const profile=await firstValueFrom(this.http.get<CreditProfile>(`/api/v1/trade/sales/customers/${this.creditForm.controls.customerId.value}/credit`));this.credit.set(profile);this.creditForm.patchValue({creditLimit:profile.creditLimit,paymentTermsDays:profile.paymentTermsDays,creditHold:profile.creditHold});}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}}
  async saveCredit(){if(this.creditForm.invalid||this.savingAr())return;const value=this.creditForm.getRawValue();this.savingAr.set(true);try{const profile=await firstValueFrom(this.http.put<CreditProfile>(`/api/v1/trade/sales/customers/${value.customerId}/credit`,{creditLimit:value.creditLimit,paymentTermsDays:value.paymentTermsDays,creditHold:value.creditHold}));this.credit.set(profile);this.notification.success(this.i18n.t('sales.ar.creditSaved'));}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingAr.set(false);}}
  async markContacted(task:CollectionTask){if(this.savingAr())return;this.savingAr.set(true);try{await firstValueFrom(this.http.put(`/api/v1/trade/sales/receivables/collections/${task.id}`,{status:'CONTACTED',ownerUserId:task.ownerUserId??'',nextActionDate:this.businessDate()+7*86400000,note:task.note??'',version:task.version,asOf:this.businessDate()}));this.notification.success(this.i18n.t('sales.ar.collectionUpdated'));await this.load();}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingAr.set(false);}}

  receivableStatusLabel(status: string): string {
    const keys: Record<string, string> = {
      DRAFT: 'sales.ar.statusDraft',
      OPEN: 'sales.ar.statusOpen',
      PARTIALLY_PAID: 'sales.ar.statusPartiallyPaid',
      PAID: 'sales.ar.statusPaid',
      CANCELLED: 'sales.ar.statusCancelled',
      CONTACTED: 'sales.ar.statusContacted',
      CLOSED: 'sales.ar.statusClosed',
    };
    return this.i18n.t(keys[status] ?? 'sales.ar.statusUnknown');
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
