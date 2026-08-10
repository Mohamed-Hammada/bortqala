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
  createdAt: number;
  updatedAt: number;
}

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
  readonly openInvoiceCount = computed(() => this.invoices().filter((row) => row.status === 'OPEN' || row.status === 'PARTIALLY_PAID').length);

  readonly drawerOpen = signal(false);

  readonly soForm = new FormGroup({
    soNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    soDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    totalAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
  });
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
        firstValueFrom(this.http.get<Aging>('/api/v1/trade/sales/receivables/aging')),
        firstValueFrom(this.http.get<CollectionTask[]>('/api/v1/trade/sales/receivables/collections')),
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
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitSo() {
    if (this.soForm.invalid) return;
    try {
      const val = this.soForm.getRawValue();
      const dateMs = new Date(val.soDate).getTime();
      const payload = {
        soNumber: val.soNumber,
        soDate: dateMs,
        customerId: val.customerId,
        totalAmount: val.totalAmount,
      };
      await firstValueFrom(this.http.post('/api/v1/trade/sales/orders', payload));
      this.notification.success(this.i18n.t('sales.soCreated'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async confirmSo(so: SalesOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/trade/sales/orders/${so.id}/confirm`, {}));
      this.notification.success(this.i18n.t('sales.soConfirmed'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

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
  async markContacted(task:CollectionTask){if(this.savingAr())return;this.savingAr.set(true);try{await firstValueFrom(this.http.put(`/api/v1/trade/sales/receivables/collections/${task.id}`,{status:'CONTACTED',ownerUserId:task.ownerUserId??'',nextActionDate:new Date(Date.now()+7*86400000).setUTCHours(0,0,0,0),note:task.note??'',version:task.version}));this.notification.success(this.i18n.t('sales.ar.collectionUpdated'));await this.load();}catch(e){this.error.set(apiErrorMessage(e,this.i18n));}finally{this.savingAr.set(false);}}

  date(ms: number) {
    return formatDate(ms);
  }
}
