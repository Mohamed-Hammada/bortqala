import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

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

export interface WorkCenter {
  id: string;
  code: string;
  name: string;
  hourlyRate: number;
  capacityHoursPerDay: number;
  active: boolean;
}

export interface RoutingHeader {
  id: string;
  routingCode: string;
  name: string;
  itemId: string;
  active: boolean;
}

export interface QualityInspection {
  id: string;
  inspectionNumber: string;
  productionOrderId: string;
  inspectorId: string;
  result: 'PASSED' | 'FAILED';
  notes?: string;
  createdAt: number;
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
  readonly activeTab = signal<'orders' | 'boms' | 'routings' | 'workCenters' | 'inspections'>('orders');

  readonly boms = signal<BomHeader[]>([]);
  readonly orders = signal<ProductionOrder[]>([]);
  readonly workCenters = signal<WorkCenter[]>([]);
  readonly routings = signal<RoutingHeader[]>([]);
  readonly inspections = signal<QualityInspection[]>([]);

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

  readonly workCenterForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    hourlyRate: new FormControl(50, { nonNullable: true, validators: [Validators.required] }),
    capacityHoursPerDay: new FormControl(8, { nonNullable: true, validators: [Validators.required] }),
  });

  readonly routingForm = new FormGroup({
    routingCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    itemId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly inspectionForm = new FormGroup({
    inspectionNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    productionOrderId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    inspectorId: new FormControl('QA-Lead', { nonNullable: true, validators: [Validators.required] }),
    result: new FormControl<'PASSED' | 'FAILED'>('PASSED', { nonNullable: true, validators: [Validators.required] }),
    notes: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [bomsData, ordersData, wcData, routingsData, inspData] = await Promise.all([
        firstValueFrom(this.http.get<BomHeader[]>('/api/v1/manufacturing/boms')),
        firstValueFrom(this.http.get<ProductionOrder[]>('/api/v1/manufacturing/orders')),
        firstValueFrom(this.http.get<WorkCenter[]>('/api/v1/manufacturing/work-centers')),
        firstValueFrom(this.http.get<RoutingHeader[]>('/api/v1/manufacturing/routings')),
        firstValueFrom(this.http.get<QualityInspection[]>('/api/v1/manufacturing/quality')),
      ]);
      this.boms.set(bomsData);
      this.orders.set(ordersData);
      this.workCenters.set(wcData);
      this.routings.set(routingsData);
      this.inspections.set(inspData);
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
        finishedGoodName: '',
        yieldQuantity: 1,
        notes: '',
      });
    } else if (this.activeTab() === 'orders') {
      this.orderForm.reset({
        orderNumber: 'MO-' + Math.floor(1000 + Math.random() * 9000),
        bomId: this.boms()[0]?.id || '',
        targetQuantity: 50,
        startDate: new Date().toISOString().substring(0, 10),
      });
    } else if (this.activeTab() === 'workCenters') {
      this.workCenterForm.reset({
        code: 'WC-' + Math.floor(10 + Math.random() * 90),
        name: '',
        hourlyRate: 50,
        capacityHoursPerDay: 8,
      });
    } else if (this.activeTab() === 'routings') {
      this.routingForm.reset({
        routingCode: 'RT-' + Math.floor(100 + Math.random() * 900),
        name: '',
        itemId: this.boms()[0]?.id || '',
      });
    } else if (this.activeTab() === 'inspections') {
      this.inspectionForm.reset({
        inspectionNumber: 'QC-' + Math.floor(1000 + Math.random() * 9000),
        productionOrderId: this.orders()[0]?.id || '',
        inspectorId: 'QA-Lead',
        result: 'PASSED',
        notes: '',
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
      this.notification.success(this.i18n.t('production.bomSaved'));
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
      this.notification.success(this.i18n.t('production.orderCreated'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitWorkCenter() {
    if (this.workCenterForm.invalid) return;
    try {
      const val = this.workCenterForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/manufacturing/work-centers', val));
      this.notification.success(this.i18n.t('production.workCenterSaved'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitRouting() {
    if (this.routingForm.invalid) return;
    try {
      const val = this.routingForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/manufacturing/routings', val));
      this.notification.success(this.i18n.t('production.routingSaved'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitInspection() {
    if (this.inspectionForm.invalid) return;
    try {
      const val = this.inspectionForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/manufacturing/quality', val));
      this.notification.success(this.i18n.t('production.inspectionSaved'));
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async startOrder(order: ProductionOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/manufacturing/orders/${order.id}/start`, {}));
      this.notification.success(this.i18n.t('production.orderStarted'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async completeOrder(order: ProductionOrder) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/manufacturing/orders/${order.id}/complete`, {}));
      this.notification.success(this.i18n.t('production.orderCompleted'));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
