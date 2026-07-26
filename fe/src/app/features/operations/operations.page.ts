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

@Component({
  selector: 'app-operations-page',
  imports: [ReactiveFormsModule, TablePaginationComponent],
  providers: [OperationsStore],
  templateUrl: './operations.page.html',
  styleUrl: './operations.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OperationsPage {
  readonly store = inject(OperationsStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly drawer = signal<'item' | 'transaction' | 'advance' | null>(null);
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
    active: new FormControl(true, { nonNullable: true }),
  });
  readonly transactionForm = new FormGroup({
    itemId: new FormControl('', { nonNullable: true }),
    partyId: new FormControl('', { nonNullable: true }),
    operationType: new FormControl('SUPPLY_RECEIPT', {
      nonNullable: true,
      validators: Validators.required,
    }),
    quantityDelta: new FormControl(0, { nonNullable: true }),
    amountDelta: new FormControl(0, { nonNullable: true }),
    lossPercentage: new FormControl<number | null>(null),
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

  constructor() {
    void this.store.load();
  }
  open(kind: 'item' | 'transaction' | 'advance'): void {
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
    if (await this.store.createItem({ ...raw, itemType: finalItemType.trim(), version: null })) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.close();
    }
  }
  async saveTransaction(): Promise<void> {
    if (this.transactionForm.invalid) return this.transactionForm.markAllAsTouched();
    const value = this.transactionForm.getRawValue();
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
