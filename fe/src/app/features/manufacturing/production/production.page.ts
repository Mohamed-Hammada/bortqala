import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';

export interface BomHeader {
  id: string;
  bomCode: string;
  finishedGoodName: string;
  yieldQuantity: number;
  notes?: string;
  active: boolean;
}

export interface ProductionOrder {
  id: string;
  orderNumber: string;
  bomId: string;
  targetQuantity: number;
  startDate: number;
  status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  createdAt: number;
  updatedAt: number;
}

@Component({
  selector: 'app-production-page',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './production.page.html',
  styleUrl: './production.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'orders' | 'boms'>('orders');

  readonly boms = signal<BomHeader[]>([]);
  readonly orders = signal<ProductionOrder[]>([]);

  readonly drawerOpen = signal(false);

  readonly bomForm = new FormGroup({
    bomCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    finishedGoodName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    yieldQuantity: new FormControl(1, { nonNullable: true, validators: [Validators.required] }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly orderForm = new FormGroup({
    orderNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    bomId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    targetQuantity: new FormControl(100, { nonNullable: true, validators: [Validators.required] }),
    startDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [bomsData, ordersData] = await Promise.all([
        firstValueFrom(this.http.get<BomHeader[]>('/api/v1/manufacturing/boms')),
        firstValueFrom(this.http.get<ProductionOrder[]>('/api/v1/manufacturing/orders')),
      ]);
      this.boms.set(bomsData);
      this.orders.set(ordersData);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    if (this.activeTab() === 'boms') {
      this.bomForm.reset({
        bomCode: 'BOM-' + Math.floor(100 + Math.random() * 900),
        finishedGoodName: 'منتج تام التصنيع أ',
        yieldQuantity: 1,
        notes: 'مكونات الإنتاج التجميعي',
      });
    } else {
      this.orderForm.reset({
        orderNumber: 'MO-' + Math.floor(1000 + Math.random() * 9000),
        bomId: this.boms()[0]?.id || '',
        targetQuantity: 50,
        startDate: new Date().toISOString().substring(0, 10),
      });
    }
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitBom() {
    if (this.bomForm.invalid) return;
    try {
      const val = this.bomForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/manufacturing/boms', { ...val, active: true }));
      this.notification.success('تمت إضافة قائمة المكونات (BOM) بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitOrder() {
    if (this.orderForm.invalid) return;
    try {
      const val = this.orderForm.getRawValue();
      const startDateMs = new Date(val.startDate).getTime();
      const payload = {
        orderNumber: val.orderNumber,
        bomId: val.bomId,
        targetQuantity: val.targetQuantity,
        startDate: startDateMs,
      };
      await firstValueFrom(this.http.post('/api/v1/manufacturing/orders', payload));
      this.notification.success('تم إنشاء أمر الإنتاج والتصنيع بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async startOrder(order: ProductionOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/manufacturing/orders/${order.id}/start`, {}));
      this.notification.success('تم بدء العمل بأمر الإنتاج بنجاح ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async completeOrder(order: ProductionOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/manufacturing/orders/${order.id}/complete`, {}));
      this.notification.success('تم إكتمال التصنيع إضافة المنتج التام للمخزن ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
