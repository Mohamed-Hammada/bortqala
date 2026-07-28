import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { formatDate } from '../../../core/date';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface PurchaseOrderItem {
  itemName: string;
  itemCategory: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  currency: string;
  warehouse: string;
  deliveryDate: string;
}

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  poDate: number;
  supplierId: string;
  supplierName?: string;
  paymentTerms?: string;
  status: 'DRAFT' | 'ISSUED' | 'RECEIVED' | 'CANCELLED';
  totalAmount: number;
  createdAt: number;
  updatedAt: number;
}

@Component({
  selector: 'app-procurement-page',
  imports: [ReactiveFormsModule, FormsModule, DecimalPipe, ModalDialogComponent],
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
  readonly suppliers = signal<Array<{ id: string; code: string; name: string }>>([]);

  readonly modalOpen = signal(false);
  readonly poItems = signal<PurchaseOrderItem[]>([
    { itemName: 'مادة خام أ', itemCategory: 'مواد أولية', quantity: 100, unitOfMeasure: 'طن', unitPrice: 50, currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: new Date().toISOString().substring(0, 10) }
  ]);

  readonly poForm = new FormGroup({
    poNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    poDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    paymentTerms: new FormControl('Net 30 Days', { nonNullable: true }),
  });

  totalCalculated = computed(() => {
    return this.poItems().reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
  });

  constructor() {
    void this.load();
    void this.loadSuppliers();
  }

  async loadSuppliers() {
    try {
      const res = await firstValueFrom(this.http.get<Array<{ id: string; code: string; name: string }>>('/api/v1/parties'));
      this.suppliers.set(res ?? []);
    } catch (e) {
      console.error('Failed to load suppliers:', e);
    }
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
    const supps = this.suppliers();
    this.poForm.reset({
      poNumber: 'PO-' + Math.floor(1000 + Math.random() * 9000),
      poDate: new Date().toISOString().substring(0, 10),
      supplierId: supps.length > 0 ? supps[0].id : '',
      paymentTerms: 'Net 30 Days',
    });
    this.poItems.set([
      { itemName: 'مادة خام تجميعية', itemCategory: 'مواد خام', quantity: 50, unitOfMeasure: 'كيلو', unitPrice: 100, currency: 'EGP', warehouse: 'مستودع 1', deliveryDate: new Date().toISOString().substring(0, 10) }
    ]);
    this.modalOpen.set(true);
  }

  closeModal() {
    this.modalOpen.set(false);
  }

  addItemLine() {
    this.poItems.update(items => [
      ...items,
      { itemName: '', itemCategory: 'عام', quantity: 1, unitOfMeasure: 'عدد', unitPrice: 0, currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: new Date().toISOString().substring(0, 10) }
    ]);
  }

  removeItemLine(index: number) {
    if (this.poItems().length <= 1) {
      this.notification.warning('يجب وجود بند شراء واحد على الأقل في أمر الشراء');
      return;
    }
    this.poItems.update(items => items.filter((_, i) => i !== index));
  }

  async submitPo() {
    if (this.poForm.invalid) {
      this.poForm.markAllAsTouched();
      return;
    }
    if (this.poItems().length === 0 || this.totalCalculated() <= 0) {
      this.notification.warning('يجب إضافة بند واحد على الأقل بقيمة أكبر من صفر');
      return;
    }

    try {
      const val = this.poForm.getRawValue();
      const dateMs = new Date(val.poDate).getTime();
      const payload = {
        poNumber: val.poNumber,
        poDate: dateMs,
        supplierId: val.supplierId,
        paymentTerms: val.paymentTerms,
        totalAmount: this.totalCalculated(),
        items: this.poItems()
      };
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/orders', payload));
      this.notification.success('تم إنشاء أمر الشراء وإضافة البنود بنجاح ✓');
      this.modalOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error('فشل إنشاء أمر الشراء: ' + apiErrorMessage(e, this.i18n));
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
