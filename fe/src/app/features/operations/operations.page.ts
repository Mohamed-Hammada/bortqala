import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { formatDateTime } from '../../core/date';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { OperationsStore } from './operations.store';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { BarcodeScannerService } from '../../core/native/barcode-scanner.service';
import { ItemCategory, StockMovement, TransactionPayload, UnitOfMeasure } from './operations.models';

type DocumentReferenceKey =
  | 'purchaseOrderNo'
  | 'receiptNo'
  | 'deliveryNoteNo'
  | 'invoiceNo'
  | 'voucherNo';

import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-operations-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, ModalDialogComponent, DecimalPipe],
  providers: [OperationsStore],
  templateUrl: './operations.page.html',
  styleUrl: './operations.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OperationsPage {
  readonly store = inject(OperationsStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly drawer = signal<'item' | 'transaction' | 'advance' | 'adjustment' | 'category' | 'uom' | 'valuation' | 'revaluation' | 'cycle-count' | 'transfer' | 'bin' | 'analytics' | null>(null);
  readonly editingTransferId = signal<string | null>(null);
  readonly activeAnalyticsTab = signal<'aging' | 'dead-stock' | 'reorder' | 'bins'>('aging');
  readonly barcodeSearchQuery = signal('');
  readonly barcodeLookupMatch = signal<any | null>(null);
  readonly searchingBarcode = signal(false);
  readonly scanningBarcode = signal(false);
  private readonly barcodeScanner = inject(BarcodeScannerService);
  readonly itemPagination = new TablePagination();
  readonly movementPagination = new TablePagination();
  readonly balancePagination = new TablePagination();
  readonly advancePagination = new TablePagination();
  readonly items = computed(() => this.itemPagination.slice(this.store.snapshot().items));
  readonly movements = computed(() =>
    this.movementPagination.slice(this.store.snapshot().movements),
  );
  readonly balances = computed(() =>
    this.balancePagination.slice(this.store.snapshot().partyBalances),
  );
  readonly advances = computed(() =>
    this.advancePagination.slice(this.store.snapshot().employeeAdvances),
  );

  readonly itemForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: Validators.required }),
    name: new FormControl('', { nonNullable: true, validators: Validators.required }),
    itemType: new FormControl('RAW_MATERIAL', {
      nonNullable: true,
      validators: Validators.required,
    }),
    customItemType: new FormControl('', { nonNullable: true }),
    unitCode: new FormControl('KG', { nonNullable: true, validators: Validators.required }),
    categoryId: new FormControl('', { nonNullable: true }),
    uomId: new FormControl('', { nonNullable: true }),
    reorderPoint: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    reorderQuantity: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    barcode: new FormControl('', { nonNullable: true }),
    barcodeAliases: new FormControl('', { nonNullable: true }),
    trackingType: new FormControl('NONE', { nonNullable: true }),
    shelfLifeDays: new FormControl<number | null>(null),
    isDeadStock: new FormControl(false, { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });
  readonly transactionForm = new FormGroup({
    itemId: new FormControl('', { nonNullable: true }),
    partyId: new FormControl('', { nonNullable: true }),
    operationType: new FormControl('SUPPLY_RECEIPT', {
      nonNullable: true,
      validators: Validators.required,
    }),
    quantityDelta: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0.01)],
    }),
    amountDelta: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    unitCost: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    lossPercentage: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.max(100)],
    }),
    referenceCode: new FormControl('', { nonNullable: true }),
    documentType: new FormControl('', { nonNullable: true }),
    reason: new FormControl('', { nonNullable: true }),
    note: new FormControl('', { nonNullable: true }),
    purchaseOrderNo: new FormControl('', { nonNullable: true }),
    receiptNo: new FormControl('', { nonNullable: true }),
    deliveryNoteNo: new FormControl('', { nonNullable: true }),
    invoiceNo: new FormControl('', { nonNullable: true }),
    voucherNo: new FormControl('', { nonNullable: true }),
    externalRef: new FormControl('', { nonNullable: true }),
    warehouse: new FormControl('', { nonNullable: true }),
    projectId: new FormControl('', { nonNullable: true }),
    wbsNodeId: new FormControl('', { nonNullable: true }),
    costCodeId: new FormControl('', { nonNullable: true }),
    lotNumber: new FormControl('', { nonNullable: true }),
    serialNumber: new FormControl('', { nonNullable: true }),
    expiryDate: new FormControl('', { nonNullable: true }),
    binId: new FormControl('', { nonNullable: true }),
    attachmentFile: new FormControl<File | null>(null),
    occurredAt: new FormControl(this.nowInput(), {
      nonNullable: true,
      validators: Validators.required,
    }),
  });
  readonly advanceForm = new FormGroup({
    employeeId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    amountDelta: new FormControl(0, { nonNullable: true, validators: Validators.required }),
    entryType: new FormControl('ADVANCE', { nonNullable: true, validators: Validators.required }),
    note: new FormControl('', { nonNullable: true }),
    occurredAt: new FormControl(this.nowInput(), {
      nonNullable: true,
      validators: Validators.required,
    }),
  });
  readonly adjustmentForm = new FormGroup({
    itemId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    quantityDelta: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    referenceCode: new FormControl('', { nonNullable: true }),
    reason: new FormControl('', { nonNullable: true, validators: Validators.required }),
    approved: new FormControl(false, { nonNullable: true, validators: Validators.requiredTrue }),
    occurredAt: new FormControl(this.nowInput(), {
      nonNullable: true,
      validators: Validators.required,
    }),
  });
  readonly categoryForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: Validators.required }),
    description: new FormControl('', { nonNullable: true }),
  });
  readonly uomForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: Validators.required }),
    abbreviation: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });
  readonly valuationForm = new FormGroup({
    valuationMethod: new FormControl<'FIFO' | 'WEIGHTED_AVERAGE'>('WEIGHTED_AVERAGE', { nonNullable: true }),
    inventoryAccountId: new FormControl('', { nonNullable: true }),
    receiptOffsetAccountId: new FormControl('', { nonNullable: true }),
    cogsAccountId: new FormControl('', { nonNullable: true }),
    adjustmentAccountId: new FormControl('', { nonNullable: true }),
    glPostingEnabled: new FormControl(false, { nonNullable: true }),
    allowBackdatedPosting: new FormControl(false, { nonNullable: true }),
  });
  readonly revaluationForm = new FormGroup({
    itemId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    newUnitCost: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.000001)] }),
    reason: new FormControl('', { nonNullable: true, validators: Validators.required }),
    occurredAt: new FormControl(this.nowInput(), { nonNullable: true, validators: Validators.required }),
  });
  readonly cycleCountForm = new FormGroup({
    warehouseId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    itemId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    countedQuantity: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    countedAt: new FormControl(this.nowInput(), { nonNullable: true, validators: Validators.required }),
  });
  readonly transferForm = new FormGroup({
    transferNumber: new FormControl('', { nonNullable: true, validators: Validators.required }),
    sourceWarehouseId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    targetWarehouseId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    transferDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: Validators.required }),
    itemId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    quantity: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.0001)] }),
  });
  readonly binForm = new FormGroup({
    warehouseId: new FormControl('', { nonNullable: true, validators: Validators.required }),
    binCode: new FormControl('', { nonNullable: true, validators: Validators.required }),
    aisle: new FormControl('', { nonNullable: true }),
    rack: new FormControl('', { nonNullable: true }),
    shelf: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    void this.store.load();
  }
  open(kind: 'item' | 'transaction' | 'advance' | 'adjustment' | 'category' | 'uom' | 'valuation' | 'revaluation' | 'cycle-count' | 'transfer' | 'bin' | 'analytics'): void {
    if (kind === 'valuation') {
      const policy = this.store.valuation()?.policy;
      if (policy) this.valuationForm.reset({
        valuationMethod: policy.valuationMethod,
        inventoryAccountId: policy.inventoryAccountId ?? '',
        receiptOffsetAccountId: policy.receiptOffsetAccountId ?? '',
        cogsAccountId: policy.cogsAccountId ?? '',
        adjustmentAccountId: policy.adjustmentAccountId ?? '',
        glPostingEnabled: policy.glPostingEnabled,
        allowBackdatedPosting: policy.allowBackdatedPosting,
      });
    }
    this.drawer.set(kind);
  }
  close(): void {
    this.drawer.set(null);
  }
  async saveItem(): Promise<void> {
    if (this.itemForm.invalid) return this.itemForm.markAllAsTouched();
    const raw = this.itemForm.getRawValue();
    const finalItemType = raw.itemType === 'CUSTOM' ? raw.customItemType : raw.itemType;
    if (!finalItemType || !finalItemType.trim()) return;
    if (await this.store.createItem({
      ...raw,
      itemType: finalItemType.trim(),
      categoryId: raw.categoryId || null,
      uomId: raw.uomId || null,
      version: null,
    })) {
      this.notification.success(this.i18n.t('operations.itemCreatedSuccess')); 
      this.close();
    }
  }
  async saveBin(): Promise<void> {
    if (this.binForm.invalid) return this.binForm.markAllAsTouched();
    const { warehouseId, ...payload } = this.binForm.getRawValue();
    if (await this.store.createBin(warehouseId, payload)) {
      this.notification.success(this.i18n.t('operations.binSaved'));
      this.binForm.reset({ warehouseId: '', binCode: '', aisle: '', rack: '', shelf: '' });
      this.close();
    }
  }
  async saveCycleCount(): Promise<void> {
    if (this.cycleCountForm.invalid) return this.cycleCountForm.markAllAsTouched();
    const value = this.cycleCountForm.getRawValue();
    if (await this.store.recordCycleCount({ ...value, operationId: crypto.randomUUID(), countDate: new Date(value.countedAt).toISOString().slice(0, 10), countedAt: undefined })) {
      this.notification.success(this.i18n.t('operations.cycleCountSaved'));
      this.cycleCountForm.reset({ warehouseId: '', itemId: '', countedQuantity: 0, countedAt: this.nowInput() });
      this.close();
    }
  }
  async saveTransfer(): Promise<void> {
    if (this.transferForm.invalid) return this.transferForm.markAllAsTouched();
    const value = this.transferForm.getRawValue();
    if (value.sourceWarehouseId === value.targetWarehouseId) {
      this.notification.error(this.i18n.t('TRANSFER_WAREHOUSES_DIFFERENT'));
      return;
    }
    let transferId = this.editingTransferId();
    if (!transferId) {
      const created = await this.store.createTransfer({
        transferNumber: value.transferNumber,
        sourceWarehouseId: value.sourceWarehouseId,
        targetWarehouseId: value.targetWarehouseId,
        transferDate: value.transferDate,
      });
      if (!created) return;
      transferId = created.id;
      this.editingTransferId.set(created.id);
    }
    if (await this.store.addTransferLine(transferId, { itemId: value.itemId, quantity: value.quantity })) {
      this.notification.success(this.i18n.t('operations.transferSaved'));
      this.editingTransferId.set(null);
      this.transferForm.reset({ transferNumber: '', sourceWarehouseId: '', targetWarehouseId: '',
        transferDate: new Date().toISOString().slice(0, 10), itemId: '', quantity: 1 });
      this.close();
    }
  }
  async transitionTransfer(id: string, action: 'ship' | 'receive' | 'cancel'): Promise<void> {
    if (await this.store.transitionTransfer(id, action)) {
      this.notification.success(this.i18n.t(`operations.transfer.${action}Success`));
    }
  }
  transferStatusLabel(status: 'DRAFT' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED'): string {
    const key = {
      DRAFT: 'operations.transfer.status.DRAFT',
      SHIPPED: 'operations.transfer.status.SHIPPED',
      RECEIVED: 'operations.transfer.status.RECEIVED',
      CANCELLED: 'operations.transfer.status.CANCELLED',
    }[status];
    return this.i18n.t(key);
  }
  async saveTransaction(): Promise<void> {
    if (this.transactionForm.invalid) {
      this.transactionForm.markAllAsTouched();
      if (this.transactionForm.controls.quantityDelta.invalid) {
        this.notification.error(
          this.i18n.t(
            'operations.invalidNegativeQuantity',
            undefined,
          ),
        );
      }
      return;
    }
    const value = this.transactionForm.getRawValue();
    if (value.quantityDelta < 0.01) {
      this.transactionForm.controls.quantityDelta.setErrors({ min: true });
      this.transactionForm.controls.quantityDelta.markAsTouched();
      this.notification.error(
        this.i18n.t(
          'operations.invalidNegativeQuantity',
          undefined,
        ),
      );
      return;
    }
    const missing = this.requiredReferences().find((key) => !(value[key] ?? '').trim());
    if (missing) {
      this.transactionForm.controls[missing].markAsTouched();
      this.notification.error(this.i18n.t(this.referenceErrorKey(missing)));
      return;
    }
    const { attachmentFile, ...rest } = value;
    const payload: TransactionPayload = {
      ...rest,
      itemId: value.itemId || null,
      partyId: value.partyId || null,
      documentType: value.documentType || null,
      reason: value.reason?.trim() || null,
      purchaseOrderNo: value.purchaseOrderNo?.trim() || null,
      receiptNo: value.receiptNo?.trim() || null,
      deliveryNoteNo: value.deliveryNoteNo?.trim() || null,
      invoiceNo: value.invoiceNo?.trim() || null,
      voucherNo: value.voucherNo?.trim() || null,
      externalRef: value.externalRef?.trim() || null,
      warehouse: value.warehouse?.trim() || null,
      attachmentName: attachmentFile?.name ?? null,
      attachmentContentType: attachmentFile?.type ?? null,
      attachmentSize: attachmentFile?.size ?? null,
      occurredAt: new Date(value.occurredAt).getTime(),
    };
    if (await this.store.transaction(payload)) {
      this.notification.success(this.i18n.t('operations.transactionSaved')); 
      this.close();
    }
  }
  async saveAdvance(): Promise<void> {
    if (this.advanceForm.invalid) return this.advanceForm.markAllAsTouched();
    const value = this.advanceForm.getRawValue();
    if (await this.store.advance({ ...value, occurredAt: new Date(value.occurredAt).getTime() })) {
      this.notification.success(this.i18n.t('operations.advanceSaved')); 
      this.close();
    }
  }
  async saveAdjustment(): Promise<void> {
    if (this.adjustmentForm.invalid) return this.adjustmentForm.markAllAsTouched();
    const value = this.adjustmentForm.getRawValue();
    if (value.quantityDelta === 0) {
      this.notification.error(this.i18n.t('operations.adjustmentQuantityZero'));
      return;
    }
    if (await this.store.adjustment({
      itemId: value.itemId,
      quantityDelta: value.quantityDelta,
      referenceCode: value.referenceCode || null,
      reason: value.reason,
      approved: value.approved,
      occurredAt: new Date(value.occurredAt).getTime(),
    })) {
      this.notification.success(this.i18n.t('operations.adjustmentSuccess'));
      this.close();
    }
  }
  async saveCategory(): Promise<void> {
    if (this.categoryForm.invalid) return this.categoryForm.markAllAsTouched();
    const value = this.categoryForm.getRawValue();
    if (await this.store.createCategory({ name: value.name, description: value.description || null })) {
      this.notification.success(this.i18n.t('operations.categoryCreatedSuccess'));
      this.close();
    }
  }
  async saveUom(): Promise<void> {
    if (this.uomForm.invalid) return this.uomForm.markAllAsTouched();
    const value = this.uomForm.getRawValue();
    if (await this.store.createUom({
      name: value.name,
      abbreviation: value.abbreviation || null,
      description: value.description || null,
    })) {
      this.notification.success(this.i18n.t('operations.uomCreatedSuccess'));
      this.close();
    }
  }
  async saveValuationPolicy(): Promise<void> {
    if (this.valuationForm.invalid) return this.valuationForm.markAllAsTouched();
    const value = this.valuationForm.getRawValue();
    if (value.glPostingEnabled && (!value.inventoryAccountId || !value.receiptOffsetAccountId || !value.cogsAccountId || !value.adjustmentAccountId)) {
      this.notification.error(this.i18n.t('INV_VAL_GL_ACCOUNTS_REQUIRED'));
      return;
    }
    if (await this.store.saveValuationPolicy({
      ...value,
      inventoryAccountId: value.inventoryAccountId || null,
      receiptOffsetAccountId: value.receiptOffsetAccountId || null,
      cogsAccountId: value.cogsAccountId || null,
      adjustmentAccountId: value.adjustmentAccountId || null,
      version: this.store.valuation()?.policy.version ?? 0,
    })) {
      this.notification.success(this.i18n.t('operations.valuation.settingsSaved'));
      this.close();
    }
  }
  async saveRevaluation(): Promise<void> {
    if (this.revaluationForm.invalid) return this.revaluationForm.markAllAsTouched();
    const value = this.revaluationForm.getRawValue();
    if (await this.store.revalue({
      ...value,
      occurredAt: new Date(value.occurredAt).getTime(),
      operationId: crypto.randomUUID(),
    })) {
      this.notification.success(this.i18n.t('operations.valuation.revalued'));
      this.close();
    }
  }
  money(value: number): string {
    return new Intl.NumberFormat(this.i18n.locale(), { style: 'currency', currency: 'EGP' }).format(value);
  }
  valuationMethodLabel(): string {
    return this.i18n.t(this.store.valuation()?.policy.valuationMethod === 'FIFO'
      ? 'operations.valuation.fifo'
      : 'operations.valuation.weightedAverage');
  }
  valuationMethodBadge(method: string | null | undefined): string {
    return this.i18n.t(method === 'FIFO' ? 'operations.valuation.fifo' : 'operations.valuation.weightedAverage');
  }
  readonly valuationAsOf = signal<string>('');
  readonly valuationWarehouseId = signal<string>('');
  async applyValuationFilters(): Promise<void> {
    const asOfRaw = this.valuationAsOf();
    await this.store.loadValuation({
      asOf: asOfRaw ? new Date(asOfRaw).getTime() : undefined,
      warehouseId: this.valuationWarehouseId() || undefined,
    });
  }
  valuationVarianceNonZero(): boolean {
    const variance = this.store.valuation()?.inventoryVarianceFromGl;
    return variance != null && Math.abs(variance) >= 0.005;
  }
  date(value: number): string {
    return formatDateTime(value);
  }
  itemTypeLabel(value: string): string {
    const key = (
      {
        RAW_MATERIAL: 'rawMaterial',
        PACKAGING: 'packaging',
        PRODUCTION_SUPPLY: 'productionSupply',
        SORTING_OUTPUT: 'sortingOutput',
        FINISHED_GOOD: 'finishedGood',
      } as Record<string, string>
    )[value];
    return key ? this.i18n.t(`operations.${key}`) : value.replaceAll('_', ' ');
  }
  operationLabel(value: string): string {
    return switchOperationLabel(this.i18n, value);
  }
  partyTypeLabel(value: string): string {
    switch (value) {
      case 'SUPPLIER':
        return this.i18n.t('partyType.supplier');
      case 'PROCESSING_CUSTOMER':
        return this.i18n.t('partyType.processingCustomer');
      case 'EXPORT_CUSTOMER':
        return this.i18n.t('partyType.exportCustomer');
      case 'SORTING_TRADER':
        return this.i18n.t('partyType.sortingTrader');
      case 'FARM':
        return this.i18n.t('partyType.farm');
      case 'OTHER':
        return this.i18n.t('partyType.other');
      default:
        return value.replaceAll('_', ' ');
    }
  }
  advanceEntryLabel(value: string): string {
    return this.i18n.t(
      value === 'REPAYMENT' ? 'operations.advanceRepaid' : 'operations.advancePaid',
    );
  }
  requiredReferences(): DocumentReferenceKey[] {
    switch (this.transactionForm.controls.operationType.value) {
      case 'SUPPLY_RECEIPT':
        return ['purchaseOrderNo', 'receiptNo'];
      case 'PROCESSING_INTAKE':
        return ['receiptNo'];
      case 'PROCESSING_DELIVERY':
      case 'EXPORT_SALE':
      case 'SORTING_SALE':
        return ['deliveryNoteNo'];
      case 'ADJUSTMENT':
        return ['voucherNo'];
      default:
        return [];
    }
  }
  referenceErrorKey(key: DocumentReferenceKey): string {
    const map: Record<string, string> = {
      purchaseOrderNo: 'OPS_MOVEMENT_PURCHASE_ORDER_REQUIRED',
      receiptNo: 'OPS_MOVEMENT_RECEIPT_REQUIRED',
      deliveryNoteNo: 'OPS_MOVEMENT_DELIVERY_NOTE_REQUIRED',
      voucherNo: 'OPS_MOVEMENT_VOUCHER_REQUIRED',
    };
    return map[key] ?? 'OPS_MOVEMENT_RECEIPT_REQUIRED';
  }
  documentTypeLabel(value: string | null): string {
    if (!value) return '—';
    const key = (
      {
        GOODS_RECEIPT: 'goodsReceipt',
        PURCHASE_ORDER: 'purchaseOrder',
        SUPPLIER_INVOICE: 'supplierInvoice',
        SUPPLIER_PAYMENT: 'supplierPayment',
        DELIVERY_NOTE: 'deliveryNote',
        ADJUSTMENT: 'adjustment',
        VOUCHER: 'voucher',
      } as Record<string, string>
    )[value];
    return key ? this.i18n.t(`operations.documentType.${key}`) : value.replaceAll('_', ' ');
  }
  primaryReference(row: StockMovement): string {
    return (
      row.receiptNo ??
      row.invoiceNo ??
      row.deliveryNoteNo ??
      row.purchaseOrderNo ??
      row.voucherNo ??
      row.externalRef ??
      row.referenceCode ??
      '—'
    );
  }
  onAttachmentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      this.transactionForm.controls.attachmentFile.setValue(null);
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.transactionForm.controls.attachmentFile.setValue(null);
      this.notification.error(this.i18n.t('operations.attachmentSizeError'));
      return;
    }
    const type = file.type.toLowerCase();
    const allowed =
      type.startsWith('image/') ||
      type === 'application/pdf' ||
      type === 'application/vnd.ms-excel' ||
      type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    if (!allowed) {
      this.transactionForm.controls.attachmentFile.setValue(null);
      this.notification.error(this.i18n.t('operations.attachmentTypeError'));
      return;
    }
    this.transactionForm.controls.attachmentFile.setValue(file);
  }
  removeAttachment(): void {
    this.transactionForm.controls.attachmentFile.setValue(null);
  }
  private nowInput(): string {
    const date = new Date();
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 16);
  }

  async onBarcodeSearch(): Promise<void> {
    const q = this.barcodeSearchQuery().trim();
    if (!q) {
      this.barcodeLookupMatch.set(null);
      return;
    }
    this.searchingBarcode.set(true);
    try {
      const match = await this.store.lookupBarcode(q);
      this.barcodeLookupMatch.set(match);
      if (!match) {
        this.notification.warning(this.i18n.t('common.noResults'));
      }
    } finally {
      this.searchingBarcode.set(false);
    }
  }

  async scanBarcode(): Promise<void> {
    this.scanningBarcode.set(true);
    try {
      const value = await this.barcodeScanner.scan(this.i18n.t('native.scanPrompt'));
      if (value) {
        this.barcodeSearchQuery.set(value);
        await this.onBarcodeSearch();
      }
    } finally {
      this.scanningBarcode.set(false);
    }
  }

  urgencyClass(urgency: string): string {
    switch (urgency) {
      case 'CRITICAL': return 'danger';
      case 'WARNING': return 'warning';
      default: return 'info';
    }
  }

  urgencyLabel(urgency: string): string {
    switch (urgency) {
      case 'CRITICAL': return this.i18n.t('common.critical');
      case 'WARNING': return this.i18n.t('common.warning');
      default: return this.i18n.t('common.notice');
    }
  }

  trackingTypeLabel(type: string): string {
    switch (type) {
      case 'LOT': return this.i18n.t('inventory.trackingLot');
      case 'SERIAL': return this.i18n.t('inventory.trackingSerial');
      case 'EXPIRY': return this.i18n.t('inventory.trackingExpiry');
      default: return this.i18n.t('inventory.trackingNone');
    }
  }

  @HostListener('document:keydown', ['$event']) onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.drawer() !== null) {
      this.close();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      if (this.drawer() === 'item') {
        event.preventDefault();
        void this.saveItem();
      } else if (this.drawer() === 'transaction') {
        event.preventDefault();
        void this.saveTransaction();
      } else if (this.drawer() === 'advance') {
        event.preventDefault();
        void this.saveAdvance();
      } else if (this.drawer() === 'adjustment') {
        event.preventDefault();
        void this.saveAdjustment();
      } else if (this.drawer() === 'category') {
        event.preventDefault();
        void this.saveCategory();
      } else if (this.drawer() === 'uom') {
        event.preventDefault();
        void this.saveUom();
      } else if (this.drawer() === 'valuation') {
        event.preventDefault();
        void this.saveValuationPolicy();
      } else if (this.drawer() === 'revaluation') {
        event.preventDefault();
        void this.saveRevaluation();
      }
    }
  }
}

function switchOperationLabel(i18n: I18nService, value: string): string {
  switch (value) {
    case 'SUPPLY_RECEIPT':
      return i18n.t('operations.operationType.supplyReceipt');
    case 'PAYMENT':
      return i18n.t('operations.operationType.payment');
    case 'PROCESSING_INTAKE':
      return i18n.t('operations.operationType.processingIntake');
    case 'PROCESSING_DELIVERY':
      return i18n.t('operations.operationType.processingDelivery');
    case 'EXPORT_SALE':
      return i18n.t('operations.operationType.exportSale');
    case 'SORTING_SALE':
      return i18n.t('operations.operationType.sortingSale');
    case 'ADJUSTMENT':
      return i18n.t('operations.operationType.adjustment');
    default:
      return value.replaceAll('_', ' ');
  }
}
