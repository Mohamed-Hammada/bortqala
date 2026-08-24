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
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { RouterLink } from '@angular/router';
import { AppTooltipDirective } from '../../../shared/ui/app-tooltip/app-tooltip.directive';
import { IconButtonComponent } from '../../../shared/ui/icon-button/icon-button.component';

interface PurchaseOrderLineResponse { id: string; itemId: string; itemName: string; itemCategory: string; quantity: number; receivedQuantity: number; remainingQuantity: number; unitOfMeasure: string; unitPrice: number; lineTotal: number; projectId?: string; wbsNodeId?: string; costCodeId?: string; }
interface PurchaseOrder { id: string; poNumber: string; poDate: number; supplierId: string; supplierName?: string; paymentTerms?: string; currencyCode: string; baseCurrencyCode: string; exchangeRate: number; exchangeRateDate: number; exchangeRateSource: string; exchangeRateOverrideReason?: string; baseTotalAmount: number; status: string; departmentId?: string; departmentName?: string; projectId?: string; wbsNodeId?: string; costCodeId?: string; totalAmount: number; items: PurchaseOrderLineResponse[]; createdAt: number; updatedAt: number; }
interface GoodsReceiptLineResponse { id: string; purchaseOrderLineId: string; itemId: string; itemName: string; itemCategory: string; deliveredQuantity: number; rejectedQuantity: number; deductedQuantity: number; quantity: number; unitOfMeasure: string; unitPrice: number; locationId?: string; lotNumber?: string; qualityReason?: string; projectId?: string; wbsNodeId?: string; costCodeId?: string; }
interface GoodsReceipt { id: string; grnNumber: string; receiptDate: number; purchaseOrderId: string; supplierId: string; supplierName?: string; warehouseId?: string; status: string; currencyCode: string; notes?: string; lines: GoodsReceiptLineResponse[]; createdAt: number; }
interface SupplierInvoice { id: string; invoiceNumber?: string; internalReference: string; missingInvoiceReason?: string; currencyCode: string; baseCurrencyCode: string; exchangeRate: number; exchangeRateDate: number; exchangeRateSource: string; exchangeRateOverrideReason?: string; baseNetAmount: number; supplierId: string; supplierName?: string; purchaseOrderId?: string; goodsReceiptId?: string; projectId?: string; wbsNodeId?: string; costCodeId?: string; responsiblePartyId?: string; invoiceDate: number; totalAmount: number; discountAmount?: number; taxAmount?: number; netAmount: number; paidAmount: number; outstandingAmount: number; dueDate?: number; notes?: string; status: string; createdAt: number; updatedAt: number; }
interface SupplierPayment { id: string; paymentNumber: string; paymentDate: number; supplierId: string; supplierName?: string; supplierInvoiceId: string; amount: number; settlementDiscount?: number | null; originalDue?: number | null; currencyCode: string; paymentMethod: string; notes?: string; operationId: string; status: string; createdAt: number; }
interface SupplierPaymentPlanRow { id: string; invoiceId: string; installmentNo: number; dueDate: number; amount: number; paidAt: number | null; }
interface PurchaseRequestLineResponse { id: string; itemId: string; itemName: string; quantity: number; unitOfMeasure?: string; estimatedUnitPrice?: number; convertedQuantity: number; }
interface PurchaseRequest { id: string; requestNumber: string; requestedBy: string; departmentId?: string; departmentName?: string; status: string; neededBy?: number | null; notes?: string; convertedPoId?: string; estimatedTotal?: number; lines: PurchaseRequestLineResponse[]; createdAt: number; updatedAt: number; }
interface PrDraftLine { itemId: string; itemName: string; quantity: number; unitOfMeasure: string; estimatedUnitPrice: number; }
interface PaymentProposalAllocation { id: string; invoiceId: string; amount: number; supplierPaymentId?: string; paymentOperationId?: string; }
interface PaymentProposal { id: string; proposalNumber: string; supplierId: string; invoiceId: string; proposedAmount: number; currencyCode: string; dueDate: string; status: string; createdBy: string; approvedBy?: string; executedBy?: string; supplierPaymentId?: string; allocations: PaymentProposalAllocation[]; }
interface PaymentProposalDraftAllocation { invoiceId: string; amount: number; }
interface ProcurementThreeWayMatch { id: string; purchaseOrderId: string; goodsReceiptId?: string; supplierInvoiceId: string; matchStatus: string; priceVarianceAmount: number; quantityVarianceAmount: number; tolerancePercentage: number; varianceReason?: string; resolvedBy?: string; resolvedAt?: number; createdAt: number; }
interface Party { id: string; code: string; name: string; partyType: string; active: boolean; managedType?: 'DIRECT' | 'MANAGED'; responsiblePartyId?: string; currencyCode?: string; paymentTerms?: string; }
interface InventoryItem { id: string; code: string; name: string; categoryName?: string; uomName?: string; unitCode?: string; active: boolean; }
interface Department { id: string; companyId: string; code: string; name: string; managerId?: string; active: boolean; }
interface ProjectOption { id: string; code: string; name: string; }
interface SupplierScorecard { supplierId: string; supplierName: string; totalOrdersCount: number; totalOrdersValue: number; onTimeDeliveryRate: number; priceVarianceRate: number; matchExceptionsCount: number; overallRating: string; }
interface NumberingSettings { automaticNumbering: boolean; }
interface Currency { code: string; name: string; symbol: string; isBase: boolean; exchangeRate: number; active: boolean; }
interface GoodsReceiptDraftLine {
  purchaseOrderLineId: string;
  itemId: string;
  itemName: string;
  itemCategory: string;
  orderedQuantity: number;
  previouslyReceivedQuantity: number;
  remainingQuantity: number;
  deliveredQuantity: number;
  rejectedQuantity: number;
  deductedQuantity: number;
  quantity: number;
  unitOfMeasure: string;
  unitPrice: number;
  locationId: string;
  lotNumber: string;
  qualityReason: string;
}

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
  private readonly confirm = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly savingPo = signal(false);
  readonly savingGrn = signal(false);
  readonly savingInvoice = signal(false);
  readonly savingPayment = signal(false);
  readonly savingProposal = signal(false);
  readonly resolvingMatch = signal(false);
  readonly submitting = computed(
    () =>
      this.savingPo() ||
      this.savingGrn() ||
      this.savingInvoice() ||
      this.savingPayment() ||
      this.savingProposal() ||
      this.resolvingMatch(),
  );
  readonly error = signal<string | null>(null);
  readonly orders = signal<PurchaseOrder[]>([]);
  readonly goodsReceipts = signal<GoodsReceipt[]>([]);
  readonly invoices = signal<SupplierInvoice[]>([]);
  readonly payments = signal<SupplierPayment[]>([]);
  readonly paymentProposals = signal<PaymentProposal[]>([]);
  readonly proposalDraftAllocations = signal<PaymentProposalDraftAllocation[]>([]);
  readonly suppliers = signal<Party[]>([]);
  readonly inventoryItems = signal<InventoryItem[]>([]);
  readonly currencies = signal<Currency[]>([]);
  readonly departments = signal<Department[]>([]);
  readonly projects = signal<ProjectOption[]>([]);
  readonly supplierScorecards = signal<SupplierScorecard[]>([]);
  readonly activeTab = signal<'pr' | 'po' | 'grn' | 'invoice' | 'proposal' | 'payment' | 'scorecard'>('po');
  readonly automaticNumbering = signal(true);
  readonly documentAutomaticNumbering = signal(true);

  readonly matchModalOpen = signal(false);
  readonly activeMatch = signal<ProcurementThreeWayMatch | null>(null);

  // ─── Installment Plans ────────────────────────────────────────────

  readonly planModalOpen = signal(false);
  readonly planInvoice = signal<SupplierInvoice | null>(null);
  readonly planRows = signal<SupplierPaymentPlanRow[]>([]);
  readonly loadingPlan = signal(false);
  readonly savingPlan = signal(false);
  readonly planForm = new FormGroup({
    installmentCount: new FormControl(3, { nonNullable: true, validators: [Validators.required, Validators.min(2), Validators.max(60)] }),
    firstDueDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
  });

  async openPaymentPlan(invoice: SupplierInvoice): Promise<void> {
    this.planInvoice.set(invoice);
    this.planRows.set([]);
    const nextMonth = new Date();
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    this.planForm.reset({ installmentCount: 3, firstDueDate: nextMonth.toISOString().substring(0, 10) });
    this.planModalOpen.set(true);
    this.loadingPlan.set(true);
    try {
      const rows = await firstValueFrom(
        this.http.get<SupplierPaymentPlanRow[]>(`/api/v1/supplier-invoices/${invoice.id}/payment-plan`)
      );
      this.planRows.set(rows ?? []);
    } catch {
      this.planRows.set([]);
    } finally {
      this.loadingPlan.set(false);
    }
  }

  async submitPaymentPlan(): Promise<void> {
    if (this.savingPlan()) return;
    const invoice = this.planInvoice();
    if (!invoice || this.planForm.invalid) { this.planForm.markAllAsTouched(); return; }
    this.savingPlan.set(true);
    try {
      const v = this.planForm.getRawValue();
      await firstValueFrom(this.http.post(`/api/v1/supplier-invoices/${invoice.id}/payment-plan`, {
        installmentCount: v.installmentCount,
        firstDueDate: new Date(v.firstDueDate).getTime(),
      }));
      this.notification.success(this.i18n.t('procurement.planCreateSuccess'));
      await this.openPaymentPlan(invoice);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.savingPlan.set(false);
    }
  }

  async performThreeWayMatch(invoice: SupplierInvoice): Promise<void> {
    try {
      const match = await firstValueFrom(
        this.http.post<ProcurementThreeWayMatch>(`/api/v1/trade/procurement/invoices/${invoice.id}/three-way-match`, { tolerancePercentage: 0 })
      );
      this.activeMatch.set(match);
      this.matchModalOpen.set(true);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async resolveMatch(resolutionNotes: string): Promise<void> {
    if (this.resolvingMatch()) return;
    const match = this.activeMatch();
    if (!match || !resolutionNotes || !resolutionNotes.trim()) {
      this.notification.error(this.i18n.t('procurement.matchResolutionNotesRequired'));
      return;
    }
    this.resolvingMatch.set(true);
    try {
      const updated = await firstValueFrom(
        this.http.post<ProcurementThreeWayMatch>(`/api/v1/trade/procurement/three-way-matches/${match.id}/resolve`, { resolutionNotes })
      );
      this.activeMatch.set(updated);
      this.notification.success(this.i18n.t('procurement.matchResolvedSuccess'));
      await this.loadAll();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.resolvingMatch.set(false);
    }
  }

  // ─── Purchase Requests ────────────────────────────────────────────

  readonly purchaseRequests = signal<PurchaseRequest[]>([]);
  readonly prModalOpen = signal(false);
  readonly editingPrId = signal<string | null>(null);
  readonly savingPr = signal(false);
  readonly prLines = signal<PrDraftLine[]>([]);
  readonly convertingPr = signal<PurchaseRequest | null>(null);
  readonly convertForm = new FormGroup({
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });
  readonly prForm = new FormGroup({
    requestedBy: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    departmentId: new FormControl('', { nonNullable: true }),
    neededBy: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  async loadPurchaseRequests() {
    try {
      this.purchaseRequests.set(await firstValueFrom(this.http.get<PurchaseRequest[]>('/api/v1/purchase-requests')) ?? []);
    } catch {
      this.purchaseRequests.set([]);
    }
  }

  openNewPr() {
    this.editingPrId.set(null);
    this.prForm.reset({ requestedBy: '', departmentId: '', neededBy: new Date().toISOString().substring(0, 10), notes: '' });
    this.prLines.set([{ itemId: '', itemName: '', quantity: 1, unitOfMeasure: '', estimatedUnitPrice: 0 }]);
    this.prModalOpen.set(true);
  }

  openEditPr(request: PurchaseRequest) {
    this.editingPrId.set(request.id);
    this.prForm.reset({
      requestedBy: request.requestedBy,
      departmentId: request.departmentId ?? '',
      neededBy: request.neededBy ? epochToDateInput(request.neededBy) : '',
      notes: request.notes ?? '',
    });
    this.prLines.set(request.lines.map(line => ({
      itemId: line.itemId,
      itemName: line.itemName,
      quantity: line.quantity,
      unitOfMeasure: line.unitOfMeasure ?? '',
      estimatedUnitPrice: line.estimatedUnitPrice ?? 0,
    })));
    this.prModalOpen.set(true);
  }

  addPrLine() {
    this.prLines.update(lines => [...lines, { itemId: '', itemName: '', quantity: 1, unitOfMeasure: '', estimatedUnitPrice: 0 }]);
  }

  removePrLine(index: number) {
    if (this.prLines().length <= 1) return;
    this.prLines.update(lines => lines.filter((_, i) => i !== index));
  }

  onPrItemChange(index: number, itemId: string) {
    const item = this.inventoryItems().find(entry => entry.id === itemId);
    this.prLines.update(lines => lines.map((line, i) => i === index
      ? { ...line, itemId, itemName: item?.name ?? itemId, unitOfMeasure: line.unitOfMeasure || item?.unitCode || item?.uomName || '' }
      : line));
  }

  async submitPr() {
    if (this.savingPr()) return;
    if (this.prForm.invalid || this.prLines().some(line => !line.itemId || !(line.quantity > 0))) {
      this.prForm.markAllAsTouched();
      this.notification.error(this.i18n.t('procurement.prLineInvalid'));
      return;
    }
    this.savingPr.set(true);
    const body = {
      requestedBy: this.prForm.controls.requestedBy.value,
      departmentId: this.prForm.controls.departmentId.value || undefined,
      neededBy: dateInputToEpoch(this.prForm.controls.neededBy.value),
      notes: this.prForm.controls.notes.value || undefined,
      lines: this.prLines(),
    };
    try {
      const editing = this.editingPrId();
      if (editing) {
        await firstValueFrom(this.http.put(`/api/v1/purchase-requests/${editing}`, body));
      } else {
        await firstValueFrom(this.http.post('/api/v1/purchase-requests', body));
      }
      this.notification.success(this.i18n.t(editing ? 'procurement.prActionSuccess' : 'procurement.prCreateSuccess'));
      this.prModalOpen.set(false);
      await this.loadPurchaseRequests();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.savingPr.set(false);
    }
  }

  prAction(request: PurchaseRequest, action: 'submit' | 'approve' | 'reject' | 'cancel') {
    const actionLabels: Record<string, string> = {
      submit: this.i18n.t('procurement.prSubmit'),
      approve: this.i18n.t('procurement.prApprove'),
      reject: this.i18n.t('procurement.prReject'),
      cancel: this.i18n.t('procurement.cancel'),
    };
    void this.confirm.confirmAndRun(
      {
        titleKey: 'procurement.prConfirmTitle',
        messageKey: 'procurement.prConfirmMessage',
        params: { number: request.requestNumber, action: actionLabels[action] },
        confirmKey: 'procurement.prConfirmOk',
      },
      async () => {
        try {
          await firstValueFrom(this.http.post(`/api/v1/purchase-requests/${request.id}/${action}`, {}));
          this.notification.success(this.i18n.t('procurement.prActionSuccess') + ' ✓');
          await this.loadPurchaseRequests();
        } catch (e) {
          this.notification.error(apiErrorMessage(e, this.i18n));
          throw e;
        }
      },
    );
  }

  openConvert(request: PurchaseRequest) {
    this.convertingPr.set(request);
    this.convertForm.reset({ supplierId: '' });
  }

  async confirmConvert() {
    const request = this.convertingPr();
    if (!request || this.convertForm.invalid) { this.convertForm.markAllAsTouched(); return; }
    try {
      await firstValueFrom(this.http.post(`/api/v1/purchase-requests/${request.id}/convert`, {
        supplierId: this.convertForm.controls.supplierId.value,
      }));
      this.notification.success(this.i18n.t('procurement.prConvertSuccess'));
      this.convertingPr.set(null);
      await Promise.all([this.loadPurchaseRequests(), this.loadOrders()]);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  // ─── PO Form ──────────────────────────────────────────────────────

  readonly modalOpen = signal(false);
  readonly editingPoId = signal<string | null>(null);
  readonly editingPoSnapshot = signal<PurchaseOrder | null>(null);
  readonly poItems = signal<Array<{ itemId: string; itemName: string; itemCategory: string; quantity: number; unitOfMeasure: string; unitPrice: number; currency: string; warehouse: string; deliveryDate: string }>>([]);

  readonly poForm = new FormGroup({
    poNumber: new FormControl('', { nonNullable: true }),
    poDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    departmentId: new FormControl('', { nonNullable: true }),
    projectId: new FormControl('', { nonNullable: true }),
    wbsNodeId: new FormControl('', { nonNullable: true }),
    costCodeId: new FormControl('', { nonNullable: true }),
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
  readonly grnItems = signal<GoodsReceiptDraftLine[]>([]);

  readonly receivableOrders = computed(() => this.orders().filter(o => ['ISSUED', 'PARTIALLY_RECEIVED'].includes(o.status)));
  readonly selectedGrnSupplierName = computed(() => {
    const po = this.orders().find(order => order.id === this.grnForm.controls.purchaseOrderId.value);
    return po?.supplierName ?? this.suppliers().find(supplier => supplier.id === po?.supplierId)?.name ?? '—';
  });
  readonly selectedGrnCurrencyCode = computed(() =>
    this.orders().find(order => order.id === this.grnForm.controls.purchaseOrderId.value)?.currencyCode ?? '',
  );

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
    discountAmount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    taxAmount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    dueDate: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    exchangeRate: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.000001)] }),
    exchangeRateOverrideReason: new FormControl('', { nonNullable: true }),
  });
  invNetAmount(): number {
    const v = this.invForm.getRawValue();
    return v.totalAmount - v.discountAmount + v.taxAmount;
  }
  invBaseNetAmount(): number { return this.invNetAmount() * this.invForm.controls.exchangeRate.value; }

  // ─── Payment Form ─────────────────────────────────────────────────

  readonly pmtModalOpen = signal(false);
  readonly paymentOperationId = signal('');
  readonly selectedPaymentSupplierId = signal('');
  readonly pmtForm = new FormGroup({
    paymentNumber: new FormControl('', { nonNullable: true }),
    paymentDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    supplierInvoiceId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    settlementDiscount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    paymentMethod: new FormControl('BANK_TRANSFER', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly unpaidInvoices = computed(() => this.invoices().filter(i => i.status === 'UNPAID' || i.status === 'PARTIALLY_PAID'));
  readonly payableInvoices = computed(() => filterPayableInvoices(this.invoices(), this.selectedPaymentSupplierId()));
  readonly proposalDraftTotal = computed(() => this.proposalDraftAllocations()
    .reduce((total, allocation) => total + allocation.amount, 0));

  constructor() {
    void this.loadAll();
  }

  applyNumberingValidators(): void {
    const auto = this.automaticNumbering();
    const poNumber = this.poForm.controls.poNumber;
    poNumber.setValidators(auto ? [] : [Validators.required]);
    poNumber.updateValueAndValidity();
    const grnNumber = this.grnForm.controls.grnNumber;
    grnNumber.setValidators(auto ? [] : [Validators.required]);
    grnNumber.updateValueAndValidity();
  }

  applyPaymentNumberingValidators(): void {
    const auto = this.documentAutomaticNumbering();
    const paymentNumber = this.pmtForm.controls.paymentNumber;
    paymentNumber.setValidators(auto ? [] : [Validators.required]);
    paymentNumber.updateValueAndValidity();
  }

  async loadAll() {
    this.loading.set(true);
    this.error.set(null);
    try { await Promise.all([this.loadNumberingSettings(), this.loadDocumentNumberingSettings(), this.loadOrders(), this.loadSuppliers(), this.loadCurrencies(), this.loadInventoryItems(), this.loadDepartments(), this.loadProjects(), this.loadGoodsReceipts(), this.loadInvoices(), this.loadPaymentProposals(), this.loadPayments(), this.loadSupplierScorecards(), this.loadPurchaseRequests()]); }
    catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
    finally { this.loading.set(false); }
  }

  async loadOrders() { this.orders.set(await firstValueFrom(this.http.get<PurchaseOrder[]>('/api/v1/trade/procurement/orders')) ?? []); }
  async loadGoodsReceipts() { this.goodsReceipts.set(await firstValueFrom(this.http.get<GoodsReceipt[]>('/api/v1/trade/procurement/goods-receipts')) ?? []); }
  async loadInvoices() { this.invoices.set(await firstValueFrom(this.http.get<SupplierInvoice[]>('/api/v1/trade/procurement/invoices')) ?? []); }
  async loadPayments() { this.payments.set(await firstValueFrom(this.http.get<SupplierPayment[]>('/api/v1/trade/procurement/payments')) ?? []); }
  async loadPaymentProposals() { this.paymentProposals.set(await firstValueFrom(this.http.get<PaymentProposal[]>('/api/v1/procurement/payment-proposals')) ?? []); }
  async loadProjects() {
    try {
      this.projects.set(await firstValueFrom(this.http.get<ProjectOption[]>('/api/v1/projects')) ?? []);
    } catch {
      this.projects.set([]);
    }
  }
  async loadSupplierScorecards() {
    try {
      this.supplierScorecards.set(await firstValueFrom(this.http.get<SupplierScorecard[]>('/api/v1/procurement/suppliers/scorecards')) ?? []);
    } catch {
      this.supplierScorecards.set([]);
    }
  }

  async createPaymentProposal(invoice: SupplierInvoice): Promise<void> {
    await this.submitPaymentProposal(invoice.supplierId, [{ invoiceId: invoice.id, amount: invoice.outstandingAmount }],
      invoice.dueDate ? epochToDateInput(invoice.dueDate) : new Date().toISOString().substring(0, 10));
  }

  toggleProposalInvoice(invoice: SupplierInvoice, selected: boolean): void {
    if (!selected) {
      this.proposalDraftAllocations.update(allocations => allocations.filter(allocation => allocation.invoiceId !== invoice.id));
      return;
    }
    const selectedInvoices = this.proposalDraftAllocations()
      .map(allocation => this.invoices().find(candidate => candidate.id === allocation.invoiceId))
      .filter((candidate): candidate is SupplierInvoice => candidate !== undefined);
    if (selectedInvoices.some(candidate => candidate.supplierId !== invoice.supplierId
      || candidate.currencyCode !== invoice.currencyCode)) {
      this.notification.warning(this.i18n.t('procurement.proposalSameSupplierCurrency'));
      return;
    }
    this.proposalDraftAllocations.update(allocations => [...allocations,
      { invoiceId: invoice.id, amount: invoice.outstandingAmount }]);
  }

  isProposalInvoiceSelected(invoiceId: string): boolean {
    return this.proposalDraftAllocations().some(allocation => allocation.invoiceId === invoiceId);
  }

  proposalAllocationAmount(invoiceId: string): number | undefined {
    return this.proposalDraftAllocations().find(allocation => allocation.invoiceId === invoiceId)?.amount;
  }

  updateProposalAllocation(invoiceId: string, value: string): void {
    const amount = Number(value);
    this.proposalDraftAllocations.update(allocations => allocations.map(allocation =>
      allocation.invoiceId === invoiceId ? { ...allocation, amount } : allocation));
  }

  async createSelectedPaymentProposal(): Promise<void> {
    const allocations = this.proposalDraftAllocations();
    if (allocations.length === 0 || allocations.some(allocation => !Number.isFinite(allocation.amount) || allocation.amount <= 0)) {
      this.notification.warning(this.i18n.t('procurement.proposalSelectionRequired'));
      return;
    }
    const firstInvoice = this.invoices().find(invoice => invoice.id === allocations[0].invoiceId);
    if (!firstInvoice) return;
    const dueDate = allocations
      .map(allocation => this.invoices().find(invoice => invoice.id === allocation.invoiceId)?.dueDate)
      .filter((date): date is number => date !== undefined)
      .sort((left, right) => left - right)[0];
    await this.submitPaymentProposal(firstInvoice.supplierId, allocations,
      dueDate ? epochToDateInput(dueDate) : new Date().toISOString().substring(0, 10));
  }

  private async submitPaymentProposal(supplierId: string, allocations: PaymentProposalDraftAllocation[], dueDate: string): Promise<void> {
    if (this.savingProposal()) return;
    this.savingProposal.set(true);
    try {
      await firstValueFrom(this.http.post('/api/v1/procurement/payment-proposals', {
        supplierId, dueDate, allocations,
      }));
      this.notification.success(this.i18n.t('procurement.proposalCreated'));
      this.proposalDraftAllocations.set([]);
      await this.loadPaymentProposals();
      this.activeTab.set('proposal');
    } catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
    finally { this.savingProposal.set(false); }
  }

  invoiceById(invoiceId: string): SupplierInvoice | undefined {
    return this.invoices().find(invoice => invoice.id === invoiceId);
  }

  async approvePaymentProposal(proposal: PaymentProposal): Promise<void> {
    if (this.savingProposal()) return;
    this.savingProposal.set(true);
    try {
      await firstValueFrom(this.http.post(`/api/v1/procurement/payment-proposals/${proposal.id}/approve`, {}));
      this.notification.success(this.i18n.t('procurement.proposalApproved'));
      await this.loadPaymentProposals();
    } catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
    finally { this.savingProposal.set(false); }
  }

  async executePaymentProposal(proposal: PaymentProposal): Promise<void> {
    if (this.savingProposal()) return;
    this.savingProposal.set(true);
    try {
      await firstValueFrom(this.http.post(`/api/v1/procurement/payment-proposals/${proposal.id}/execute`, {
        operationId: crypto.randomUUID(), paymentMethod: 'BANK_TRANSFER',
      }));
      this.notification.success(this.i18n.t('procurement.proposalExecuted'));
      await Promise.all([this.loadPaymentProposals(), this.loadPayments(), this.loadInvoices()]);
    } catch (e) { this.notification.error(apiErrorMessage(e, this.i18n)); }
    finally { this.savingProposal.set(false); }
  }
  async loadNumberingSettings() {
    const settings = await firstValueFrom(this.http.get<NumberingSettings>('/api/v1/trade/procurement/numbering-settings'));
    this.automaticNumbering.set(settings?.automaticNumbering ?? true);
    this.applyNumberingValidators();
  }
  async loadDocumentNumberingSettings() {
    const settings = await firstValueFrom(this.http.get<NumberingSettings>('/api/v1/finance/numbering-settings'));
    this.documentAutomaticNumbering.set(settings?.automaticNumbering ?? true);
    this.applyPaymentNumberingValidators();
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
  async loadDepartments() {
    const departments = await firstValueFrom(this.http.get<Department[]>('/api/v1/organization/departments')) ?? [];
    this.departments.set(departments.filter(department => department.active));
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
    this.poForm.reset({ poNumber: '', poDate: new Date().toISOString().substring(0, 10), supplierId: supplier?.id ?? '', departmentId: this.departments()[0]?.id ?? '', projectId: '', wbsNodeId: '', costCodeId: '', paymentTerms: supplier?.paymentTerms ?? 'Net 30 Days', currencyCode, exchangeRate: this.configuredRate(currencyCode), exchangeRateOverrideReason: '' });
    const firstItem = this.inventoryItems()[0];
    this.poItems.set([{ itemId: firstItem?.id ?? '', itemName: firstItem?.name ?? '', itemCategory: firstItem?.categoryName ?? '', quantity: 1, unitOfMeasure: firstItem?.uomName ?? firstItem?.unitCode ?? '', unitPrice: 0, currency: 'EGP', warehouse: 'المستودع الرئيسي', deliveryDate: new Date().toISOString().substring(0, 10) }]);
    this.modalOpen.set(true);
  }

  openEditPo(po: PurchaseOrder) {
    if (po.status !== 'DRAFT') return;
    this.editingPoId.set(po.id);
    this.editingPoSnapshot.set(po);
    this.poForm.reset({ poNumber: po.poNumber, poDate: epochToDateInput(po.poDate), supplierId: po.supplierId, departmentId: po.departmentId ?? '', projectId: po.projectId ?? '', wbsNodeId: po.wbsNodeId ?? '', costCodeId: po.costCodeId ?? '', paymentTerms: po.paymentTerms ?? '', currencyCode: po.currencyCode ?? 'EGP', exchangeRate: po.exchangeRate ?? 1, exchangeRateOverrideReason: po.exchangeRateOverrideReason ?? '' });
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
  removeItemLine(idx: number) { if (this.poItems().length > 1) this.poItems.update(i => i.filter((_, n) => n !== idx)); else this.notification.warning(this.i18n.t('procurement.poAtLeastOneLine')); }

  async submitPo() {
    if (this.savingPo()) return;
    if (this.poForm.invalid) { this.poForm.markAllAsTouched(); return; }
    if (!this.automaticNumbering() && !this.poForm.controls.poNumber.value.trim()) { this.poForm.controls.poNumber.markAsTouched(); return; }
    if (this.poItems().length === 0 || this.poItems().some(item => !item.itemId || item.quantity <= 0 || item.unitPrice < 0)) { this.notification.warning(this.i18n.t('procurement.poInvalidLineWarning')); return; }
    if (!this.editingPoId() && this.poRateOverridden() && !this.poForm.controls.exchangeRateOverrideReason.value.trim()) { this.notification.warning(this.i18n.t('procurement.rateOverrideReasonRequired')); return; }
    this.savingPo.set(true);
    try {
      const v = this.poForm.getRawValue();
      const payload = { poNumber: this.automaticNumbering() ? null : v.poNumber.trim(), poDate: dateInputToEpoch(v.poDate), supplierId: v.supplierId, departmentId: v.departmentId || null, projectId: v.projectId || null, wbsNodeId: v.wbsNodeId || null, costCodeId: v.costCodeId || null, paymentTerms: v.paymentTerms, currencyCode: v.currencyCode, exchangeRate: v.exchangeRate, exchangeRateOverrideReason: v.exchangeRateOverrideReason || null, items: this.poItems() };
      const editingId = this.editingPoId();
      await firstValueFrom(editingId
        ? this.http.put(`/api/v1/trade/procurement/orders/${editingId}`, payload)
        : this.http.post('/api/v1/trade/procurement/orders', payload));
      this.notification.success(editingId ? this.i18n.t('procurement.poUpdateSuccess') : this.i18n.t('procurement.poCreateSuccess'));
      this.modalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error(this.i18n.t('procurement.poCreateFail') + apiErrorMessage(e, this.i18n)); }
    finally { this.savingPo.set(false); }
  }

  issuePo(po: PurchaseOrder) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'procurement.issuePo.confirmTitle',
        messageKey: 'procurement.issuePo.confirmMessage',
        params: { number: po.poNumber },
        confirmKey: 'procurement.issuePo.confirm',
        details: [
          { label: this.i18n.t('procurement.poNumber'), value: po.poNumber },
          { label: this.i18n.t('procurement.poSupplier'), value: po.supplierName ?? po.supplierId },
          { label: this.i18n.t('procurement.poTotal'), value: `${po.totalAmount} ${po.currencyCode}` },
        ],
      },
      async () => {
        try {
          await firstValueFrom(this.http.post(`/api/v1/trade/procurement/orders/${po.id}/issue`, {}));
          this.notification.success(this.i18n.t('procurement.issuePo.success') + ' ✓');
          await this.loadAll();
        } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); throw e; }
      },
    );
  }

  cancelPo(po: PurchaseOrder) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'procurement.cancelPo.confirmTitle',
        messageKey: 'procurement.cancelPo.confirmMessage',
        params: { number: po.poNumber },
        confirmKey: 'procurement.cancelPo.confirm',
        danger: true,
        dangerMessageKey: 'procurement.cancelPo.dangerMessage',
        details: [
          { label: this.i18n.t('procurement.poNumber'), value: po.poNumber },
          { label: this.i18n.t('procurement.poSupplier'), value: po.supplierName ?? po.supplierId },
        ],
      },
      async () => {
        try {
          await firstValueFrom(this.http.post(`/api/v1/trade/procurement/orders/${po.id}/cancel`, {}));
          this.notification.success(this.i18n.t('procurement.cancelPo.success') + ' ✓');
          await this.loadAll();
        } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); throw e; }
      },
    );
  }

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
      this.grnItems.set(po.items
        .filter(line => Number(line.remainingQuantity ?? line.quantity) > 0)
        .map(line => {
          const remaining = Number(line.remainingQuantity ?? line.quantity);
          return {
            purchaseOrderLineId: line.id,
            itemId: line.itemId,
            itemName: line.itemName,
            itemCategory: line.itemCategory ?? '',
            orderedQuantity: Number(line.quantity),
            previouslyReceivedQuantity: Number(line.receivedQuantity ?? 0),
            remainingQuantity: remaining,
            deliveredQuantity: remaining,
            rejectedQuantity: 0,
            deductedQuantity: 0,
            quantity: remaining,
            unitOfMeasure: line.unitOfMeasure ?? '',
            unitPrice: line.unitPrice,
            locationId: '',
            lotNumber: '',
            qualityReason: '',
          };
        }));
    } else {
      this.grnForm.patchValue({ supplierId: '' });
      this.grnItems.set([]);
    }
  }

  acceptedGrnQuantity(item: GoodsReceiptDraftLine): number | null {
    const delivered = Number(item.deliveredQuantity);
    const rejected = Number(item.rejectedQuantity);
    const deducted = Number(item.deductedQuantity);
    if (![delivered, rejected, deducted].every(Number.isFinite)
      || delivered <= 0 || rejected < 0 || deducted < 0 || rejected + deducted > delivered) {
      return null;
    }
    return delivered - rejected - deducted;
  }

  grnLineError(item: GoodsReceiptDraftLine, field: 'delivered' | 'rejected' | 'deducted' | 'accepted'): string | null {
    const delivered = Number(item.deliveredQuantity);
    const rejected = Number(item.rejectedQuantity);
    const deducted = Number(item.deductedQuantity);
    if (field === 'delivered' && (!Number.isFinite(delivered) || delivered <= 0)) {
      return this.i18n.t('procurement.grnLineDeliveredPositive');
    }
    if (field === 'rejected' && (!Number.isFinite(rejected) || rejected < 0)) {
      return this.i18n.t('procurement.grnLineRejectedNonNegative');
    }
    if (field === 'deducted' && (!Number.isFinite(deducted) || deducted < 0)) {
      return this.i18n.t('procurement.grnLineDeductedNonNegative');
    }
    if (Number.isFinite(delivered) && Number.isFinite(rejected) && Number.isFinite(deducted)
      && rejected >= 0 && deducted >= 0 && rejected + deducted > delivered) {
      return this.i18n.t('procurement.grnLineRejectedDeductedExceed');
    }
    const accepted = this.acceptedGrnQuantity(item);
    if (field === 'accepted' && accepted !== null && accepted > item.remainingQuantity) {
      return this.i18n.t('procurement.grnLineAcceptedExceedsRemaining', { remaining: item.remainingQuantity, uom: item.unitOfMeasure });
    }
    return null;
  }

  grnHasErrors(): boolean {
    if (this.grnForm.invalid || this.grnItems().length === 0) return true;
    let acceptedTotal = 0;
    for (const item of this.grnItems()) {
      const accepted = this.acceptedGrnQuantity(item);
      if (accepted === null || accepted > item.remainingQuantity) return true;
      acceptedTotal += accepted;
    }
    return acceptedTotal <= 0;
  }

  async submitGrn() {
    if (this.savingGrn()) return;
    if (this.grnForm.invalid) {
      this.grnForm.markAllAsTouched();
      this.notification.warning(this.i18n.t('procurement.grnRequiredFields'));
      return;
    }
    if (!this.automaticNumbering() && !this.grnForm.controls.grnNumber.value.trim()) { this.grnForm.controls.grnNumber.markAsTouched(); return; }
    if (this.grnHasErrors()) {
      this.notification.warning(this.i18n.t('procurement.grnQuantityErrors'));
      return;
    }
    this.savingGrn.set(true);
    try {
      const v = this.grnForm.getRawValue();
      const lines = this.grnItems().map(item => ({
        ...item,
        quantity: this.acceptedGrnQuantity(item),
        locationId: item.locationId.trim() || null,
        lotNumber: item.lotNumber.trim() || null,
        qualityReason: item.qualityReason.trim() || null,
      }));
      const saved = await firstValueFrom(this.http.post<GoodsReceipt>('/api/v1/trade/procurement/goods-receipts', {
        grnNumber: this.automaticNumbering() ? null : v.grnNumber.trim(),
        receiptDate: dateInputToEpoch(v.receiptDate),
        purchaseOrderId: v.purchaseOrderId,
        supplierId: v.supplierId,
        warehouseId: v.warehouseId.trim() || null,
        notes: v.notes.trim() || null,
        lines,
      }));
      this.notification.success(this.i18n.t('procurement.grnSuccess', { number: saved.grnNumber }));
      this.grnModalOpen.set(false);
      await this.loadAll();
      this.activeTab.set('grn');
    } catch (e) {
      this.notification.error(this.i18n.t('procurement.grnFail') + apiErrorMessage(e, this.i18n));
    }
    finally { this.savingGrn.set(false); }
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
    if (this.savingInvoice()) return;
    if (this.invForm.invalid) { this.invForm.markAllAsTouched(); return; }
    const invoiceValue = this.invForm.getRawValue();
    if (invoiceValue.hasSupplierInvoice && !invoiceValue.invoiceNumber.trim()) { this.notification.warning(this.i18n.t('procurement.invoiceMissingNumber')); return; }
    if (!invoiceValue.hasSupplierInvoice && (!invoiceValue.internalReference.trim() || !invoiceValue.missingInvoiceReason.trim())) { this.notification.warning(this.i18n.t('procurement.invoiceMissingReference')); return; }
    if (this.invoiceRateOverridden() && !invoiceValue.exchangeRateOverrideReason.trim()) { this.notification.warning(this.i18n.t('procurement.rateOverrideReasonRequired')); return; }
    if (invoiceValue.dueDate && new Date(invoiceValue.dueDate) < new Date(invoiceValue.invoiceDate)) { this.notification.warning(this.i18n.t('procurement.invoiceDueDateBeforeInvoiceDate')); return; }
    if (invoiceValue.discountAmount > invoiceValue.totalAmount) { this.notification.warning(this.i18n.t('procurement.invoiceDiscountExceedsTotal')); return; }
    this.savingInvoice.set(true);
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
      this.notification.success(this.i18n.t('procurement.invoiceSaveSuccess'));
      this.invModalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error(this.i18n.t('procurement.invoiceSaveFail') + apiErrorMessage(e, this.i18n)); }
    finally { this.savingInvoice.set(false); }
  }

  // ─── Payment Methods ──────────────────────────────────────────────

  openNewPayment(inv?: SupplierInvoice) {
    const s = this.suppliers();
    const supplierId = inv ? inv.supplierId : (s.length > 0 ? s[0].id : '');
    this.paymentOperationId.set(crypto.randomUUID());
    this.selectedPaymentSupplierId.set(supplierId);
    this.pmtForm.reset({ paymentNumber: '', paymentDate: new Date().toISOString().substring(0, 10), supplierId, supplierInvoiceId: inv ? inv.id : '', amount: inv ? inv.outstandingAmount : 0, settlementDiscount: 0, paymentMethod: 'BANK_TRANSFER', notes: '' });
    this.applyPaymentNumberingValidators();
    this.pmtModalOpen.set(true);
  }

  onPaymentSupplierChanged(): void {
    const supplierId = this.pmtForm.controls.supplierId.value;
    this.selectedPaymentSupplierId.set(supplierId);
    const selected = this.invoices().find(invoice => invoice.id === this.pmtForm.controls.supplierInvoiceId.value);
    if (selected && selected.supplierId !== supplierId) {
      this.pmtForm.patchValue({ supplierInvoiceId: '', amount: 0 });
      this.notification.warning(this.i18n.t('procurement.paymentInvoiceChanged'));
    }
  }

  onPaymentInvoiceChanged(): void {
    const invoice = this.payableInvoices().find(item => item.id === this.pmtForm.controls.supplierInvoiceId.value);
    this.pmtForm.controls.amount.setValue(invoice?.outstandingAmount ?? 0);
  }

  settlementDiscountsByInvoice(invoiceId: string): number {
    return this.payments().filter(pmt => pmt.supplierInvoiceId === invoiceId)
      .reduce((sum, pmt) => sum + (pmt.settlementDiscount ?? 0), 0);
  }

  settlementPreview(): { originalDue: number; cash: number; discount: number; remainingAfter: number } {
    const invoice = this.payableInvoices().find(item => item.id === this.pmtForm.controls.supplierInvoiceId.value);
    const v = this.pmtForm.getRawValue();
    const cash = Number(v.amount) || 0;
    const discount = v.settlementDiscount > 0 ? Number(v.settlementDiscount) : 0;
    const originalDue = invoice?.outstandingAmount ?? 0;
    return { originalDue, cash, discount, remainingAfter: Math.max(originalDue - cash - discount, 0) };
  }

  settlementExceeds(): boolean {
    const preview = this.settlementPreview();
    return preview.cash + preview.discount > preview.originalDue;
  }

  async submitPayment() {
    if (this.savingPayment()) return;
    if (this.pmtForm.invalid) { this.pmtForm.markAllAsTouched(); return; }
    this.savingPayment.set(true);
    try {
      const v = this.pmtForm.getRawValue();
      const invoice = this.payableInvoices().find(item => item.id === v.supplierInvoiceId);
      if (!invoice) { this.notification.error(this.i18n.t('procurement.paymentSelectOpenInvoice')); return; }
      const discount = v.settlementDiscount > 0 ? v.settlementDiscount : 0;
      if (v.amount > invoice.outstandingAmount || v.amount + discount > invoice.outstandingAmount) { this.notification.error(this.i18n.t('procurement.paymentExceedsOutstanding', { outstanding: invoice.outstandingAmount, currency: invoice.currencyCode })); return; }
      await firstValueFrom(this.http.post('/api/v1/trade/procurement/payments', { paymentNumber: v.paymentNumber.trim() || null, paymentDate: new Date(v.paymentDate).getTime(), supplierId: v.supplierId, supplierInvoiceId: v.supplierInvoiceId, amount: v.amount, settlementDiscount: discount > 0 ? discount : null, paymentMethod: v.paymentMethod, notes: v.notes || null, operationId: this.paymentOperationId() }));
      this.notification.success(this.i18n.t('procurement.paymentSaveSuccess'));
      this.pmtModalOpen.set(false);
      await this.loadAll();
    } catch (e) { this.notification.error(this.i18n.t('procurement.paymentSaveFail') + apiErrorMessage(e, this.i18n)); }
    finally { this.savingPayment.set(false); }
  }

  // ─── Shared ────────────────────────────────────────────────────────

  date(ms: number) { return formatDate(ms); }
  poDepartmentName(po: PurchaseOrder): string {
    if (po.departmentName) return po.departmentName;
    return this.departments().find(department => department.id === po.departmentId)?.name ?? '—';
  }
  poProjectName(po: PurchaseOrder): string {
    if (!po.projectId) return '—';
    const proj = this.projects().find(p => p.id === po.projectId);
    return proj ? `${proj.code} - ${proj.name}` : po.projectId;
  }
  scorecardRatingClass(rating: string): string {
    switch (rating) {
      case 'EXCELLENT': return 'success';
      case 'GOOD': return 'info';
      case 'FAIR': return 'warning';
      default: return 'danger';
    }
  }
  scorecardRatingLabel(rating: string): string {
    const key = ({
      EXCELLENT: 'procurement.ratingExcellent',
      GOOD: 'procurement.ratingGood',
      FAIR: 'procurement.ratingFair',
      AT_RISK: 'procurement.ratingAtRisk',
    } as Record<string, string>)[rating];
    return key ? this.i18n.t(key) : rating;
  }
  poStatusLabel(status: string): string {
    const key = ({ DRAFT: 'procurement.poStatus.draft', ISSUED: 'procurement.poStatus.issued', PARTIALLY_RECEIVED: 'procurement.poStatus.partiallyReceived', RECEIVED: 'procurement.poStatus.received', CANCELLED: 'procurement.poStatus.cancelled' } as Record<string, string>)[status];
    return key ? this.i18n.t(key) : status;
  }
  invoiceReference(invoice?: SupplierInvoice): string { return invoice?.invoiceNumber || invoice?.internalReference || '—'; }
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
      : this.activeTab() === 'pr' ? this.purchaseRequests().map(r => ({ requestNumber: r.requestNumber, requestedBy: r.requestedBy, department: r.departmentName || r.departmentId || '', neededBy: r.neededBy ? this.date(r.neededBy) : '', lines: r.lines?.length ?? 0, estimatedTotal: r.estimatedTotal ?? 0, status: r.status }))
      : this.activeTab() === 'grn' ? this.goodsReceipts().map(g => ({ grnNumber: g.grnNumber, receiptDate: this.date(g.receiptDate), supplier: g.supplierName || g.supplierId, notes: g.notes || '', status: g.status }))
      : this.activeTab() === 'invoice' ? this.invoices().map(i => ({ invoiceNumber: i.invoiceNumber, supplier: i.supplierName || i.supplierId, totalAmount: i.totalAmount, netAmount: i.netAmount, paidAmount: i.paidAmount, outstandingAmount: i.outstandingAmount, status: i.status }))
      : this.payments().map(p => ({ paymentNumber: p.paymentNumber, paymentDate: this.date(p.paymentDate), supplier: p.supplierName || p.supplierId, amount: p.amount, method: p.paymentMethod }));
    const cols = this.activeTab() === 'po' ? [{ key: 'poNumber', label: this.i18n.t('procurement.colPoNumber') }, { key: 'poDate', label: this.i18n.t('procurement.colPoDate') }, { key: 'supplier', label: this.i18n.t('procurement.colSupplier') }, { key: 'paymentTerms', label: this.i18n.t('procurement.colPaymentTerms') }, { key: 'totalAmount', label: this.i18n.t('procurement.colTotal') }, { key: 'status', label: this.i18n.t('procurement.colStatus') }]
      : this.activeTab() === 'pr' ? [{ key: 'requestNumber', label: this.i18n.t('procurement.prNumber') }, { key: 'requestedBy', label: this.i18n.t('procurement.prRequestedBy') }, { key: 'department', label: this.i18n.t('procurement.department') }, { key: 'neededBy', label: this.i18n.t('procurement.prNeededBy') }, { key: 'lines', label: this.i18n.t('procurement.prLines') }, { key: 'estimatedTotal', label: this.i18n.t('procurement.colTotal') }, { key: 'status', label: this.i18n.t('procurement.prStatus') }]
      : this.activeTab() === 'grn' ? [{ key: 'grnNumber', label: this.i18n.t('procurement.colGrnNumber') }, { key: 'receiptDate', label: this.i18n.t('procurement.colReceiptDate') }, { key: 'supplier', label: this.i18n.t('procurement.colSupplier') }, { key: 'notes', label: this.i18n.t('procurement.colNotes') }, { key: 'status', label: this.i18n.t('procurement.colStatus') }]
      : this.activeTab() === 'invoice' ? [{ key: 'invoiceNumber', label: this.i18n.t('procurement.colInvoiceNumber') }, { key: 'supplier', label: this.i18n.t('procurement.colSupplier') }, { key: 'totalAmount', label: this.i18n.t('procurement.colTotal') }, { key: 'netAmount', label: this.i18n.t('procurement.colNetAmount') }, { key: 'status', label: this.i18n.t('procurement.colStatus') }]
      : [{ key: 'paymentNumber', label: this.i18n.t('procurement.colPaymentNumber') }, { key: 'paymentDate', label: this.i18n.t('procurement.colPaymentDate') }, { key: 'supplier', label: this.i18n.t('procurement.colSupplier') }, { key: 'amount', label: this.i18n.t('procurement.colAmount') }, { key: 'method', label: this.i18n.t('procurement.colPaymentMethod') }];
    exportCsv(rows, cols, `procurement-${this.activeTab()}-${new Date().toISOString().slice(0, 10)}.csv`);
  }
}
