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
import { ItemCategory, UnitOfMeasure } from './operations.models';

@Component({
  selector: 'app-operations-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, ModalDialogComponent],
  providers: [OperationsStore],
  templateUrl: './operations.page.html',
  styleUrl: './operations.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OperationsPage {
  readonly store = inject(OperationsStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly drawer = signal<'item' | 'transaction' | 'advance' | 'adjustment' | 'category' | 'uom' | null>(null);
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
    lossPercentage: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.max(100)],
    }),
    referenceCode: new FormControl('', { nonNullable: true }),
    note: new FormControl('', { nonNullable: true }),
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

  constructor() {
    void this.store.load();
  }
  open(kind: 'item' | 'transaction' | 'advance' | 'adjustment' | 'category' | 'uom'): void {
    this.drawer.set(kind);
  }
  close(): void {
    this.drawer.set(null);
  }
  @HostListener('document:keydown.escape') onEscape(): void {
    this.close();
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
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.close();
    }
  }
  async saveTransaction(): Promise<void> {
    if (this.transactionForm.invalid) {
      this.transactionForm.markAllAsTouched();
      if (this.transactionForm.controls.quantityDelta.invalid) {
        this.notification.error(
          this.i18n.t(
            'operations.invalidNegativeQuantity',
            undefined,
            'كمية الحركة يجب أن تكون رقماً موجباً أكبر من الصفر.',
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
          'كمية الحركة يجب أن تكون رقماً موجباً أكبر من الصفر.',
        ),
      );
      return;
    }
    if (
      await this.store.transaction({
        ...value,
        itemId: value.itemId || null,
        partyId: value.partyId || null,
        occurredAt: new Date(value.occurredAt).getTime(),
      })
    ) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.close();
    }
  }
  async saveAdvance(): Promise<void> {
    if (this.advanceForm.invalid) return this.advanceForm.markAllAsTouched();
    const value = this.advanceForm.getRawValue();
    if (await this.store.advance({ ...value, occurredAt: new Date(value.occurredAt).getTime() })) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.close();
    }
  }
  async saveAdjustment(): Promise<void> {
    if (this.adjustmentForm.invalid) return this.adjustmentForm.markAllAsTouched();
    const value = this.adjustmentForm.getRawValue();
    if (value.quantityDelta === 0) {
      this.notification.error('كمية التسوية يجب ألا تكون صفراً');
      return;
    }
    if (await this.store.adjustment({
      itemId: value.itemId,
      quantityDelta: value.quantityDelta,
      referenceCode: value.referenceCode || null,
      reason: value.reason,
      occurredAt: new Date(value.occurredAt).getTime(),
    })) {
      this.notification.success('تم إجراء تسوية المخزون بنجاح ✓');
      this.close();
    }
  }
  async saveCategory(): Promise<void> {
    if (this.categoryForm.invalid) return this.categoryForm.markAllAsTouched();
    const value = this.categoryForm.getRawValue();
    if (await this.store.createCategory({ name: value.name, description: value.description || null })) {
      this.notification.success('تم إنشاء تصنيف المخزون بنجاح ✓');
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
      this.notification.success('تم إنشاء وحدة القياس بنجاح ✓');
      this.close();
    }
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
  private nowInput(): string {
    const date = new Date();
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 16);
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
