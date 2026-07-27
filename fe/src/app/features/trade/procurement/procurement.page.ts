import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { formatDate } from '../../../core/date';

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  poDate: number;
  supplierId: string;
  purchaseRequestId?: string;
  paymentTerms?: string;
  status: 'DRAFT' | 'ISSUED' | 'RECEIVED' | 'CANCELLED';
  totalAmount: number;
  createdAt: number;
  updatedAt: number;
}

@Component({
  selector: 'app-procurement-page',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './procurement.page.html',
  styleUrl: './procurement.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcurementPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly orders = signal<PurchaseOrder[]>([]);

  readonly drawerOpen = signal(false);

  readonly poForm = new FormGroup({
    poNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    poDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    paymentTerms: new FormControl('Net 30 Days', { nonNullable: true }),
    totalAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<PurchaseOrder[]>('/api/v1/trade/procurement/orders'),
      );
      this.orders.set(data);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.poForm.reset({
      poNumber: 'PO-' + Math.floor(1000 + Math.random() * 9000),
      poDate: new Date().toISOString().substring(0, 10),
      supplierId: 'SUPP-01',
      paymentTerms: 'Net 30 Days',
      totalAmount: 5000,
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitPo() {
    if (this.poForm.invalid) return;
    try {
      const val = this.poForm.getRawValue();
      const dateMs = new Date(val.poDate).getTime();
      const payload = {
        poNumber: val.poNumber,
        poDate: dateMs,
        supplierId: val.supplierId,
        paymentTerms: val.paymentTerms,
        totalAmount: val.totalAmount,
      };
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/orders', payload));
      this.notification.success('تم إنشاء أمر الشراء بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async issuePo(po: PurchaseOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/trade/procurement/orders/${po.id}/issue`, {}));
      this.notification.success('تم إصدار أمر الشراء للمورد بنجاح ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
