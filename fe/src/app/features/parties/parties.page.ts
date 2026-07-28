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
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { BusinessParty, BusinessPartyPayload } from './parties.models';
import { PartiesStore } from './parties.store';

import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';

@Component({
  selector: 'app-parties-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, ModalDialogComponent],
  providers: [PartiesStore],
  templateUrl: './parties.page.html',
  styleUrl: './parties.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartiesPage {
  readonly store = inject(PartiesStore);
  readonly i18n = inject(I18nService);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly submitted = signal(false);
  readonly search = signal('');
  readonly pagination = new TablePagination();
  readonly knownTypes = [
    'SUPPLIER',
    'PROCESSING_CUSTOMER',
    'EXPORT_CUSTOMER',
    'SORTING_TRADER',
    'FARM',
    'OTHER',
  ];
  readonly filtered = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.store
      .items()
      .filter(
        (item) =>
          !query || `${item.code} ${item.name} ${item.phone ?? ''}`.toLowerCase().includes(query),
      );
  });
  readonly paged = computed(() => this.pagination.slice(this.filtered()));
  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    partyType: new FormControl('SUPPLIER', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    managedType: new FormControl('DIRECT', { nonNullable: true }),
    responsiblePartyId: new FormControl('', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true }),
    invoicePolicy: new FormControl('E_INVOICE', { nonNullable: true }),
    paymentTerms: new FormControl('CASH', { nonNullable: true }),
    taxId: new FormControl('', { nonNullable: true }),
    bankAccount: new FormControl('', { nonNullable: true }),
    contactPerson: new FormControl('', { nonNullable: true }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(01[0125][0-9]{8}|\+?[0-9]{8,15})?$/)]
    }),
    notes: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });

  readonly phoneError = computed(() => {
    const ctrl = this.form.controls.phone;
    if ((this.submitted() || ctrl.touched) && ctrl.hasError('pattern')) {
      return 'رقم الهاتف غير صحيح. يرجى إدخال رقم هاتف صحيح (مثال: 01012345678)';
    }
    return null;
  });

  constructor() {
    void this.store.load();
  }

  openNew(): void {
    this.editingId.set(null);
    this.submitted.set(false);
    this.form.reset({
      code: '',
      name: '',
      partyType: 'SUPPLIER',
      contactPerson: '',
      phone: '',
      notes: '',
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }

  openEdit(item: BusinessParty): void {
    this.editingId.set(item.id);
    this.submitted.set(false);
    this.form.reset({
      code: item.code,
      name: item.name,
      partyType: item.partyType,
      contactPerson: item.contactPerson ?? '',
      phone: item.phone ?? '',
      notes: item.notes ?? '',
      active: item.active,
      version: item.version,
    });
    this.drawerOpen.set(true);
  }

  async submit(): Promise<void> {
    this.submitted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const payload: BusinessPartyPayload = {
      ...value,
      contactPerson: value.contactPerson.trim() || null,
      phone: value.phone.trim() || null,
      notes: value.notes.trim() || null,
    };
    if (await this.store.save(this.editingId(), payload)) this.closeDrawer();
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.submitted.set(false);
  }

  @HostListener('document:keydown', ['$event']) onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.drawerOpen()) {
      this.closeDrawer();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      if (this.drawerOpen()) {
        event.preventDefault();
        void this.submit();
      }
    }
  }

  typeLabel(type: string): string {
    switch (type) {
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
        return type.replaceAll('_', ' ');
    }
  }
}
