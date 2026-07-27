import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { formatDate } from '../../../core/date';

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

@Component({
  selector: 'app-sales-page',
  imports: [ReactiveFormsModule, DecimalPipe],
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

  readonly drawerOpen = signal(false);

  readonly soForm = new FormGroup({
    soNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    soDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    customerId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
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
        this.http.get<SalesOrder[]>('/api/v1/trade/sales/orders'),
      );
      this.orders.set(data);
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
      this.notification.success('تم إنشاء أمر البيع بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async confirmSo(so: SalesOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/trade/sales/orders/${so.id}/confirm`, {}));
      this.notification.success('تم تأكيد أمر البيع بنجاح ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
