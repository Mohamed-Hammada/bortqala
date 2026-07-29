import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { DecimalPipe } from '@angular/common';
import { dateInputToEpoch, epochToDateInput, formatDate } from '../../../core/date';
import { downloadBlob, exportCsv } from '../../../core/download';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { RouterLink } from '@angular/router';
import { AppTooltipDirective } from '../../../shared/ui/app-tooltip/app-tooltip.directive';
import { IconButtonComponent } from '../../../shared/ui/icon-button/icon-button.component';

interface PurchaseOrderLineResponse { id: string; itemId: string; itemName: string; itemCategory: string; quantity: number; unitOfMeasure: string; unitPrice: number; lineTotal: number; }
interface PurchaseOrder { id: string; poNumber: string; poDate: number; supplierId: string; supplierName?: string; paymentTerms?: string; currencyCode: string; baseCurrencyCode: string; exchangeRate: number; exchangeRateDate: number; exchangeRateSource: string; exchangeRateOverrideReason?: string; baseTotalAmount: number; status: string; totalAmount: number; items: PurchaseOrderLineResponse[]; createdAt: number; updatedAt: number; }
interface GoodsReceiptLineResponse { id: string; purchaseOrderLineId: string; itemId: string; itemName: string; itemCategory: string; deliveredQuantity: number; rejectedQuantity: number; deductedQuantity: number; quantity: number; unitOfMeasure: string; unitPrice: number; locationId?: string; lotNumber?: string; qualityReason?: string; }
interface GoodsReceipt { id: string; grnNumber: string; receiptDate: number; purchaseOrderId: string; supplierId: string; supplierName?: string; warehouseId?: string; status: string; currencyCode: string; notes?: string; lines: GoodsReceiptLineResponse[]; createdAt: number; }
interface SupplierInvoice { id: string; invoiceNumber?: string; internalReference: string; missingInvoiceReason?: string; currencyCode: string; baseCurrencyCode: string; exchangeRate: number; exchangeRateDate: number; exchangeRateSource: string; exchangeRateOverrideReason?: string; baseNetAmount: number; supplierId: string; supplierName?: string; purchaseOrderId?: string; goodsReceiptId?: string; responsiblePartyId?: string; invoiceDate: number; totalAmount: number; discountAmount?: number; taxAmount?: number; netAmount: number; paidAmount: number; outstandingAmount: number; dueDate?: number; notes?: string; status: string; createdAt: number; updatedAt: number; }
interface SupplierPayment { id: string; paymentNumber: string; paymentDate: number; supplierId: string; supplierName?: string; supplierInvoiceId: string; amount: number; currencyCode: string; paymentMethod: string; notes?: string; operationId: string; status: string; createdAt: number; }
interface Party { id: string; code: string; name: string; partyType: string; active: boolean; managedType?: 'DIRECT' | 'MANAGED'; responsiblePartyId?: string; currencyCode?: string; paymentTerms?: string; }
interface InventoryItem { id: string; code: string; name: string; categoryName?: string; uomName?: string; unitCode?: string; active: boolean; }
interface NumberingSettings { automaticNumbering: boolean; }
interface Currency { code: string; name: string; symbol: string; isBase: boolean; exchangeRate: number; active: boolean; }

export function calculatePurchaseOrderTotal(items: ReadonlyArray<{ quantity: number; unitPrice: number }>): number {
  return items.reduce((sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPrice) || 0), 0);
}

export function filterPayableInvoices<T extends { supplierId: string; status: string }>(
  invoices: readonly T[], supplierId: string,
): T[] {
  return invoices.filter(invoice => invoice.supplierId === supplierId
    && (invoice.status === 'UNPAID' || invoice.status === 'PARTIALLY_PAID'));
}

@Component({
  selector: 'app-procurement-page',
  imports: [ReactiveFormsModule, FormsModule, DecimalPipe, ModalDialogComponent, RouterLink, AppTooltipDirective, IconButtonComponent],
  templateUrl: './procurement.page.html',
  styleUrl: './procurement.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcurementPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly orders = signal<PurchaseOrder[]>([]);
  readonly goodsReceipts = signal<GoodsReceipt[]>([]);
  readonly invoices = signal<SupplierInvoice[]>([]);
  readonly payments = signal<SupplierPayment[]>([]);
  readonly suppliers = signal<Party[]>([]);
  readonly inventoryItems = signal<InventoryItem[]>([]);
  readonly currencies = signal<Currency[]>([]);
  readonly activeTab = signal<'po' | 'grn' | 'invoice' | 'payment'>('po');
  readonly automaticNumbering = signal(true);

  // ─── PO Form ──────────────────────────────────────────────────────

  readonly modalOpen = signal(false);
  readonly editingPoId = signal<string | null>(null);
  readonly editingPoSnapshot = signal<PurchaseOrder | null>(null);
  readonly poItems = signal<Array<{ itemId: string; itemName: string; itemCategory: string; quantity: number; unitOfMeasure: string; unitPrice: number; currency: string; warehouse: string; deliveryDate: string }>>([]);

  readonly poForm = new FormGroup({
    poNumber: new FormControl('', { nonNullable: true }),
    poDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    paymentTerms: new FormControl('Net 30 Days', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    exchangeRate: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.000001)] }),
    exchangeRateOverrideReason: new FormControl('', { nonNullable: true }),
  });

  totalCalculated(): number {
    return calculatePurchaseOrderTotal(this.poItems());
  }
  poBaseTotal(): number { return this.totalCalculated() * this.poForm.controls.exchangeRate.value; }

  // ─── GRN Form ─────────────────────────────────────────────────────

  readonly grnModalOpen = signal(false);
  readonly grnForm = new FormGroup({
    grnNumber: new FormControl('', { nonNullable: true }),
    receiptDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    purchaseOrderId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    warehouseId: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });
  readonly grnItems = signal<Array<{ purchaseOrderLineId: string; itemId: string; itemName: string; itemCategory: string; deliveredQuantity: number; rejectedQuantity: number; deductedQuantity: number; quantity: number; unitOfMeasure: string; unitPrice: number; locationId: string; lotNumber: string; qualityReason: string }>>([]);

  readonly receivableOrders = computed(() => this.orders().filter(o => ['DRAFT', 'ISSUED', 'PARTIALLY_RECEIVED'].includes(o.status)));
  readonly selectedGrnSupplierName = computed(() => {
    const po = this.orders().find(order => order.id === this.grnForm.controls.purchaseOrderId.value);
    return po?.supplierName ?? this.suppliers().find(supplier => supplier.id === po?.supplierId)?.name ?? '—';
  });

  // ─── Invoice Form ─────────────────────────────────────────────────

  readonly invModalOpen = signal(false);
  readonly invForm = new FormGroup({
    hasSupplierInvoice: new FormControl(true, { nonNullable: true }),
    invoiceNumber: new FormControl('', { nonNullable: true }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    purchaseOrderId: new FormControl('', { nonNullable: true }),
    goodsReceiptId: new FormControl('', { nonNullable: true }),
    internalReference: new FormControl('', { nonNullable: true }),
    missingInvoiceReason: new FormControl('', { nonNullable: true }),
    invoiceDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    totalAmount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    discountAmount: new FormControl(0, { nonNullable: true }),
    taxAmount: new FormControl(0, { nonNullable: true }),
    dueDate: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    exchangeRate: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.000001)] }),
    exchangeRateOverrideReason: new FormControl('', { nonNullable: true }),
  });
  invNetAmount(): number {
    const v = this.invForm.getRawValue();
    return Math.max(0, v.totalAmount - v.discountAmount + v.taxAmount);
  }
  invBaseNetAmount(): number { return this.invNetAmount() * this.invForm.controls.exchangeRate.value; }

  // ─── Payment Form ─────────────────────────────────────────────────

  readonly pmtModalOpen = signal(false);
  readonly paymentOperationId = signal('');
  readonly selectedPaymentSupplierId = signal('');
  readonly pmtForm = new FormGroup({
    paymentNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    paymentDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    supplierInvoiceId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    paymentMethod: new FormControl('BANK_TRANSFER', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly unpaidInvoices = computed(() => this.invoices().filter(i => i.status === 'UNPAID' || i.status === 'PARTIALLY_PAID'));
  readonly payableInvoices = computed(() => filterPayableInvoices(this.invoices(), this.selectedPaymentSupplierId()));

  constructor() { void this.loadAll(); }

  async loadAll() {
    this.loading.set(true);
    this.error.set(null);
    try { await Promise.all([this.loadNumberingSettings(), this.loadOrders(), this.loadSuppliers(), this.loadCurrencies(), this.loadInventoryItems(), this.loadGoodsReceipts(), this.loadInvoices(), this.loadPayments()]); }
    catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
    finally { this.loading.set(false); }
  }

  async loadOrders() { this.orders.set(await firstValueFrom(this.http.get<PurchaseOrder[]>('/api/v1/trade/procurement/orders')) ?? []); }
  async loadGoodsReceipts() { this.goodsReceipts.set(await firstValueFrom(this.http.get<GoodsReceipt[]>('/api/v1/trade/procurement/goods-receipts')) ?? []); }
  async loadInvoices() { this.invoices.set(await firstValueFrom(this.http.get<SupplierInvoice[]>('/api/v1/trade/procurement/invoices')) ?? []); }
  async loadPayments() { this.payments.set(await firstValueFrom(this.http.get<SupplierPayment[]>('/api/v1/trade/procurement/payments')) ?? []); }
  async loadNumberingSettings() {
    const settings = await firstValueFrom(this.http.get<NumberingSettings>('/api/v1/trade/procurement/numbering-settings'));
    this.automaticNumbering.set(settings?.automaticNumbering ?? true);
  }
  async loadSuppliers() {
    const parties = await firstValueFrom(this.http.get<Party[]>('/api/v1/parties')) ?? [];
    this.suppliers.set(parties.filter(party => party.active && party.partyType === 'SUPPLIER'));
  }
  async loadCurrencies() {
    const currencies = await firstValueFrom(this.http.get<Currency[]>('/api/v1/finance/currencies')) ?? [];
    this.currencies.set(currencies.filter(currency => currency.active));
  }
  async loadInventoryItems() {
    const snapshot = await firstValueFrom(this.http.get<{ items: InventoryItem[] }>('/api/v1/operations'));
    this.inventoryItems.set((snapshot?.items ?? []).filter(item => item.active));
  }

  // ─── PO Methods ───────────────────────────────────────────────────

  openNewPo() {
    const s = this.suppliers();
    this.editingPoId.set(null);
    this.editingPoSnapshot.set(null);
    this.poForm.controls.poDate.enable({ emitEvent: false });
    this.poForm.controls.currencyCode.enable({ emitEvent: false });
    this.poForm.controls.exchangeRate.enable({ emitEvent: false });
    this.poForm.controls.exchangeRateOverrideReason.enable({ emitEvent: false });
    const supplier = s[0];
    const currencyCode = supplier?.currencyCode ?? 'EGP';
    this.poForm.reset({ poNumber: '', poDate: new Date().toISOString().substring(0, 10), supplierId: supplier?.id ?? '', paymentTerms: supplier?.paymentTerms ?? 'Net 30 Days', currencyCode, exchangeRate: this.configuredRate(currencyCode), exchangeRateOverrideReason: '' });
    const firstItem = this.inventoryItems()[0];
    this.poItems.set([{ itemId: firstItem?.id ?? '', itemName: firstItem?.name ?? '', itemCategory: firstItem?.categoryName ?? '', quantity: 1, unitOfMeasure: firstItem?.uomName ?? firstItem?.unitCode ?? '', unitPrice: 0, currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: new Date().toISOString().substring(0, 10) }]);
    this.modalOpen.set(true);
  }

  openEditPo(po: PurchaseOrder) {
    if (po.status !== 'DRAFT') return;
    this.editingPoId.set(po.id);
    this.editingPoSnapshot.set(po);
    this.poForm.reset({ poNumber: po.poNumber, poDate: epochToDateInput(po.poDate), supplierId: po.supplierId, paymentTerms: po.paymentTerms ?? '', currencyCode: po.currencyCode ?? 'EGP', exchangeRate: po.exchangeRate ?? 1, exchangeRateOverrideReason: po.exchangeRateOverrideReason ?? '' });
    this.poForm.controls.poDate.disable({ emitEvent: false });
    this.poForm.controls.currencyCode.disable({ emitEvent: false });
    this.poForm.controls.exchangeRate.disable({ emitEvent: false });
    this.poForm.controls.exchangeRateOverrideReason.disable({ emitEvent: false });
    this.poItems.set(po.items.map(item => ({
      itemId: item.itemId, itemName: item.itemName, itemCategory: item.itemCategory ?? '',
      quantity: item.quantity, unitOfMeasure: item.unitOfMeasure ?? '', unitPrice: item.unitPrice,
      currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: epochToDateInput(po.poDate),
    })));
    this.modalOpen.set(true);
  }

  addItemLine() { this.poItems.update(i => [...i, { itemId: '', itemName: '', itemCategory: '', quantity: 1, unitOfMeasure: '', unitPrice: 0, currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: new Date().toISOString().substring(0, 10) }]); }
  onPoItemSelected(index: number) {
    const line = this.poItems()[index];
    const item = this.inventoryItems().find(candidate => candidate.id === line.itemId);
    if (item) Object.assign(line, { itemName: item.name, itemCategory: item.categoryName ?? '', unitOfMeasure: item.uomName ?? item.unitCode ?? '' });
  }
  onPoSupplierSelected(): void {
    const supplier = this.suppliers().find(item => item.id === this.poForm.controls.supplierId.value);
    if (supplier) {
      if (this.editingPoId()) {
        this.poForm.patchValue({ paymentTerms: supplier.paymentTerms ?? this.poForm.controls.paymentTerms.value });
        return;
      }
      const currencyCode = supplier.currencyCode ?? 'EGP';
      this.poForm.patchValue({ currencyCode, exchangeRate: this.configuredRate(currencyCode), exchangeRateOverrideReason: '', paymentTerms: supplier.paymentTerms ?? this.poForm.controls.paymentTerms.value });
    }
  }
  onPoCurrencyChanged(): void {
    if (this.editingPoId()) return;
    this.poForm.patchValue({ exchangeRate: this.configuredRate(this.poForm.controls.currencyCode.value), exchangeRateOverrideReason: '' });
  }
  removeItemLine(idx: number) { if (this.poItems().length > 1) this.poItems.update(i => i.filter((_, n) => n !== idx)); else this.notification.warning('يجب وجود بند شراء واحد على الأقل'); }

  async submitPo() {
    if (this.submitting()) return;
    if (this.poForm.invalid) { this.poForm.markAllAsTouched(); return; }
    if (!this.automaticNumbering() && !this.poForm.controls.poNumber.value.trim()) { this.poForm.controls.poNumber.markAsTouched(); return; }
    if (this.poItems().length === 0 || this.poItems().some(item => !item.itemId || item.quantity <= 0 || item.unitPrice < 0)) { this.notification.warning('اختر صنف مخزون وأدخل كمية وسعراً صحيحين لكل بند'); return; }
    if (!this.editingPoId() && this.poRateOverridden() && !this.poForm.controls.exchangeRateOverrideReason.value.trim()) { this.notification.warning('اكتب سبب تعديل سعر الصرف يدوياً.'); return; }
    this.submitting.set(true);
    try {
      const v = this.poForm.getRawValue();
      const payload = { poNumber: this.automaticNumbering() ? null : v.poNumber.trim(), poDate: dateInputToEpoch(v.poDate), supplierId: v.supplierId, paymentTerms: v.paymentTerms, currencyCode: v.currencyCode, exchangeRate: v.exchangeRate, exchangeRateOverrideReason: v.exchangeRateOverrideReason || null, items: this.poItems() };
      const editingId = this.editingPoId();
      await firstValueFrom(editingId
        ? this.http.put(`/api/v1/trade/procurement/orders/${editingId}`, payload)
        : this.http.post('/api/v1/trade/procurement/orders', payload));
      this.notification.success(editingId ? 'تم تعديل أمر الشراء بنجاح ✓' : 'تم إنشاء أمر الشراء بنجاح ✓');
      this.modalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error('فشل إنشاء أمر الشراء: ' + apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  async issuePo(po: PurchaseOrder) { try { await firstValueFrom(this.http.post(`/api/v1/trade/procurement/orders/${po.id}/issue`, {})); this.notification.success('تم إصدار أمر الشراء للمورد ✓'); await this.loadAll(); } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); } }
  async cancelPo(po: PurchaseOrder) { try { await firstValueFrom(this.http.post(`/api/v1/trade/procurement/orders/${po.id}/cancel`, {})); this.notification.success('تم إلغاء أمر الشراء ✓'); await this.loadAll(); } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); } }

  // ─── GRN Methods ──────────────────────────────────────────────────

  openNewGrn(preselected?: PurchaseOrder) {
    const receivable = this.receivableOrders();
    const selectedId = preselected?.id ?? (receivable.length > 0 ? receivable[0].id : '');
    this.grnForm.reset({ grnNumber: '', receiptDate: new Date().toISOString().substring(0, 10), purchaseOrderId: selectedId, supplierId: '', warehouseId: '', notes: '' });
    this.grnItems.set([]);
    this.onGrnPoSelected();
    this.grnModalOpen.set(true);
  }

  onGrnPoSelected() {
    const poId = this.grnForm.getRawValue().purchaseOrderId;
    const po = this.orders().find(o => o.id === poId);
    if (po) {
      this.grnForm.patchValue({ supplierId: po.supplierId });
      this.grnItems.set(po.items.map(l => ({ purchaseOrderLineId: l.id, itemId: l.itemId, itemName: l.itemName, itemCategory: l.itemCategory ?? '', deliveredQuantity: l.quantity, rejectedQuantity: 0, deductedQuantity: 0, quantity: l.quantity, unitOfMeasure: l.unitOfMeasure ?? '', unitPrice: l.unitPrice, locationId: '', lotNumber: '', qualityReason: '' })));
    }
  }

  async submitGrn() {
    if (this.submitting()) return;
    if (this.grnForm.invalid) { this.grnForm.markAllAsTouched(); return; }
    if (!this.automaticNumbering() && !this.grnForm.controls.grnNumber.value.trim()) { this.grnForm.controls.grnNumber.markAsTouched(); return; }
    if (this.grnItems().length === 0 || this.grnItems().some(item => item.deliveredQuantity <= 0 || item.rejectedQuantity < 0 || item.deductedQuantity < 0 || item.rejectedQuantity + item.deductedQuantity > item.deliveredQuantity)) { this.notification.warning('راجع الكمية المستلمة والمرفوضة والمخصومة'); return; }
    this.submitting.set(true);
    try {
      const v = this.grnForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/goods-receipts', { grnNumber: this.automaticNumbering() ? null : v.grnNumber.trim(), receiptDate: dateInputToEpoch(v.receiptDate), purchaseOrderId: v.purchaseOrderId, supplierId: v.supplierId, warehouseId: v.warehouseId || null, notes: v.notes || null, lines: this.grnItems() }));
      this.notification.success('تم تسجيل إذن الاستلام وإضافة الأصناف للمخزن ✓');
      this.grnModalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error('فشل تسجيل إذن الاستلام: ' + apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  // ─── Invoice Methods ──────────────────────────────────────────────

  openNewInvoice() {
    const s = this.suppliers();
    const supplier = s[0];
    const currencyCode = supplier?.currencyCode ?? 'EGP';
    this.invForm.reset({ hasSupplierInvoice: true, invoiceNumber: '', supplierId: supplier?.id ?? '', purchaseOrderId: '', goodsReceiptId: '', internalReference: '', missingInvoiceReason: '', invoiceDate: new Date().toISOString().substring(0, 10), totalAmount: 0, discountAmount: 0, taxAmount: 0, dueDate: '', notes: '', currencyCode, exchangeRate: this.configuredRate(currencyCode), exchangeRateOverrideReason: '' });
    this.invModalOpen.set(true);
  }

  onInvoiceSupplierSelected(): void {
    const supplier = this.suppliers().find(item => item.id === this.invForm.controls.supplierId.value);
    if (supplier) {
      const currencyCode = supplier.currencyCode ?? 'EGP';
      this.invForm.patchValue({ currencyCode, exchangeRate: this.configuredRate(currencyCode), exchangeRateOverrideReason: '' });
    }
  }
  onInvoiceCurrencyChanged(): void {
    this.invForm.patchValue({ exchangeRate: this.configuredRate(this.invForm.controls.currencyCode.value), exchangeRateOverrideReason: '' });
  }

  onInvoiceAvailabilityChanged(): void {
    if (!this.invForm.controls.hasSupplierInvoice.value) this.invForm.controls.invoiceNumber.setValue('');
  }

  async submitInvoice() {
    if (this.submitting()) return;
    if (this.invForm.invalid) { this.invForm.markAllAsTouched(); return; }
    const invoiceValue = this.invForm.getRawValue();
    if (invoiceValue.hasSupplierInvoice && !invoiceValue.invoiceNumber.trim()) { this.notification.warning('أدخل رقم فاتورة المورد، أو اختر «لا توجد فاتورة من المورد».'); return; }
    if (!invoiceValue.hasSupplierInvoice && (!invoiceValue.internalReference.trim() || !invoiceValue.missingInvoiceReason.trim())) { this.notification.warning('أدخل المرجع الداخلي وسبب عدم وجود فاتورة المورد.'); return; }
    if (this.invoiceRateOverridden() && !invoiceValue.exchangeRateOverrideReason.trim()) { this.notification.warning('اكتب سبب تعديل سعر الصرف يدوياً.'); return; }
    this.submitting.set(true);
    try {
      const v = this.invForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/invoices', {
        invoiceNumber: v.hasSupplierInvoice ? v.invoiceNumber.trim() : null, supplierId: v.supplierId, purchaseOrderId: v.purchaseOrderId || null,
        goodsReceiptId: v.goodsReceiptId || null, internalReference: v.internalReference || null,
        missingInvoiceReason: v.hasSupplierInvoice ? null : v.missingInvoiceReason || null, currencyCode: v.currencyCode,
        exchangeRate: v.exchangeRate, exchangeRateOverrideReason: v.exchangeRateOverrideReason || null,
        invoiceDate: new Date(v.invoiceDate).getTime(), totalAmount: v.totalAmount,
        discountAmount: v.discountAmount > 0 ? v.discountAmount : null, taxAmount: v.taxAmount > 0 ? v.taxAmount : null,
        dueDate: v.dueDate ? new Date(v.dueDate).getTime() : null, notes: v.notes || null,
      }));
      this.notification.success('تم تسجيل فاتورة المورد بنجاح ✓');
      this.invModalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error('فشل تسجيل الفاتورة: ' + apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  // ─── Payment Methods ──────────────────────────────────────────────

  openNewPayment(inv?: SupplierInvoice) {
    const s = this.suppliers();
    const supplierId = inv ? inv.supplierId : (s.length > 0 ? s[0].id : '');
    this.paymentOperationId.set(crypto.randomUUID());
    this.selectedPaymentSupplierId.set(supplierId);
    this.pmtForm.reset({ paymentNumber: 'PMT-' + Math.floor(1000 + Math.random() * 9000), paymentDate: new Date().toISOString().substring(0, 10), supplierId, supplierInvoiceId: inv ? inv.id : '', amount: inv ? inv.outstandingAmount : 0, paymentMethod: 'BANK_TRANSFER', notes: '' });
    this.pmtModalOpen.set(true);
  }

  onPaymentSupplierChanged(): void {
    const supplierId = this.pmtForm.controls.supplierId.value;
    this.selectedPaymentSupplierId.set(supplierId);
    const selected = this.invoices().find(invoice => invoice.id === this.pmtForm.controls.supplierInvoiceId.value);
    if (selected && selected.supplierId !== supplierId) {
      this.pmtForm.patchValue({ supplierInvoiceId: '', amount: 0 });
      this.notification.warning('تم إلغاء اختيار الفاتورة السابقة لأنها لا تخص المورد المحدد.');
    }
  }

  onPaymentInvoiceChanged(): void {
    const invoice = this.payableInvoices().find(item => item.id === this.pmtForm.controls.supplierInvoiceId.value);
    this.pmtForm.controls.amount.setValue(invoice?.outstandingAmount ?? 0);
  }

  async submitPayment() {
    if (this.submitting()) return;
    if (this.pmtForm.invalid) { this.pmtForm.markAllAsTouched(); return; }
    this.submitting.set(true);
    try {
      const v = this.pmtForm.getRawValue();
      const invoice = this.payableInvoices().find(item => item.id === v.supplierInvoiceId);
      if (!invoice) { this.notification.error('اختر فاتورة مفتوحة تخص المورد المحدد.'); return; }
      if (v.amount > invoice.outstandingAmount) { this.notification.error(`المبلغ يتجاوز الرصيد المتبقي ${invoice.outstandingAmount} ${invoice.currencyCode}.`); return; }
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/payments', { paymentNumber: v.paymentNumber, paymentDate: new Date(v.paymentDate).getTime(), supplierId: v.supplierId, supplierInvoiceId: v.supplierInvoiceId, amount: v.amount, paymentMethod: v.paymentMethod, notes: v.notes || null, operationId: this.paymentOperationId() }));
      this.notification.success('تم تسجيل دفعة المورد بنجاح ✓');
      this.pmtModalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error('فشل تسجيل الدفعة: ' + apiErrorMessage(e, this.i18n)); }
    finally { this.submitting.set(false); }
  }

  // ─── Shared ────────────────────────────────────────────────────────

  date(ms: number) { return formatDate(ms); }
  poStatusLabel(status: string): string {
    return ({ DRAFT: 'مسودة', ISSUED: 'صادر', PARTIALLY_RECEIVED: 'استلام جزئي', RECEIVED: 'مستلم بالكامل', CANCELLED: 'ملغي' } as Record<string, string>)[status] ?? status;
  }
  invoiceReference(invoice: SupplierInvoice): string { return invoice.invoiceNumber || invoice.internalReference; }
  baseCurrencyCode(): string { return this.currencies().find(currency => currency.isBase)?.code ?? 'EGP'; }
  configuredRate(currencyCode: string): number {
    const currency = this.currencies().find(item => item.code === currencyCode);
    return currency?.isBase ? 1 : Number(currency?.exchangeRate ?? 0);
  }
  poRateOverridden(): boolean { return Math.abs(this.poForm.controls.exchangeRate.value - this.configuredRate(this.poForm.controls.currencyCode.value)) > 0.000001; }
  invoiceRateOverridden(): boolean { return Math.abs(this.invForm.controls.exchangeRate.value - this.configuredRate(this.invForm.controls.currencyCode.value)) > 0.000001; }

  async exportExcel(): Promise<void> {
    try {
      const blob = await firstValueFrom(this.http.get('/api/v1/trade/procurement/export.xlsx', { responseType: 'blob' }));
      downloadBlob(blob, `procurement-${new Date().toISOString().slice(0, 10)}.xlsx`);
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    }
  }

  exportCsv() {
    const rows: any[] = this.activeTab() === 'po' ? this.orders().map(p => ({ poNumber: p.poNumber, poDate: this.date(p.poDate), supplier: p.supplierName || p.supplierId, paymentTerms: p.paymentTerms || '', totalAmount: p.totalAmount, status: p.status }))
      : this.activeTab() === 'grn' ? this.goodsReceipts().map(g => ({ grnNumber: g.grnNumber, receiptDate: this.date(g.receiptDate), supplier: g.supplierName || g.supplierId, notes: g.notes || '', status: g.status }))
      : this.activeTab() === 'invoice' ? this.invoices().map(i => ({ invoiceNumber: i.invoiceNumber, supplier: i.supplierName || i.supplierId, totalAmount: i.totalAmount, netAmount: i.netAmount, paidAmount: i.paidAmount, outstandingAmount: i.outstandingAmount, status: i.status }))
      : this.payments().map(p => ({ paymentNumber: p.paymentNumber, paymentDate: this.date(p.paymentDate), supplier: p.supplierName || p.supplierId, amount: p.amount, method: p.paymentMethod }));
    const cols = this.activeTab() === 'po' ? [{ key: 'poNumber', label: 'رقم أمر الشراء' }, { key: 'poDate', label: 'تاريخ الأمر' }, { key: 'supplier', label: 'المورد' }, { key: 'paymentTerms', label: 'شروط الدفع' }, { key: 'totalAmount', label: 'الإجمالي' }, { key: 'status', label: 'الحالة' }]
      : this.activeTab() === 'grn' ? [{ key: 'grnNumber', label: 'رقم الإذن' }, { key: 'receiptDate', label: 'تاريخ الاستلام' }, { key: 'supplier', label: 'المورد' }, { key: 'notes', label: 'ملاحظات' }, { key: 'status', label: 'الحالة' }]
      : this.activeTab() === 'invoice' ? [{ key: 'invoiceNumber', label: 'رقم الفاتورة' }, { key: 'supplier', label: 'المورد' }, { key: 'totalAmount', label: 'الإجمالي' }, { key: 'netAmount', label: 'الصافي' }, { key: 'status', label: 'الحالة' }]
      : [{ key: 'paymentNumber', label: 'رقم الدفعة' }, { key: 'paymentDate', label: 'تاريخ الدفع' }, { key: 'supplier', label: 'المورد' }, { key: 'amount', label: 'المبلغ' }, { key: 'method', label: 'طريقة الدفع' }];
    exportCsv(rows, cols, `procurement-${this.activeTab()}-${new Date().toISOString().slice(0, 10)}.csv`);
  }
}
