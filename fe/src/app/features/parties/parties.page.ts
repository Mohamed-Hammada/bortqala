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
import { SampleTemplateService } from '../../core/sample-template.service';

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
  readonly sampleTemplates = inject(SampleTemplateService);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly submitted = signal(false);
  readonly duplicateWarning = signal<string | null>(null);
  readonly supplierModalOpen = signal(false);
  readonly documentFile = signal<File | null>(null);
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
    nameEn: new FormControl('', { nonNullable: true }),
    partyType: new FormControl('SUPPLIER', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    managedType: new FormControl('DIRECT', { nonNullable: true }),
    responsiblePartyId: new FormControl('', { nonNullable: true }),
    relationshipStartDate: new FormControl('', { nonNullable: true }),
    relationshipEndDate: new FormControl('', { nonNullable: true }),
    currencyCode: new FormControl('EGP', { nonNullable: true }),
    invoicePolicy: new FormControl('E_INVOICE', { nonNullable: true }),
    paymentTerms: new FormControl('CASH', { nonNullable: true }),
    taxId: new FormControl('', { nonNullable: true }),
    bankAccount: new FormControl('', { nonNullable: true }),
    supplierCategory: new FormControl('', { nonNullable: true }),
    riskLevel: new FormControl('LOW', { nonNullable: true }),
    ownerUserId: new FormControl('', { nonNullable: true }),
    contactPerson: new FormControl('', { nonNullable: true }),
    phone: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^(01[0125][0-9]{8}|\+?[0-9]{8,15})?$/)]
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^([\w.%+-]+@[\w.-]+\.[a-zA-Z]{2,})?$/)]
    }),
    address: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });
  readonly documentForm = new FormGroup({
    documentType: new FormControl('TAX_CARD', { nonNullable: true, validators: [Validators.required] }),
    documentNumber: new FormControl('', { nonNullable: true }),
    issueDate: new FormControl('', { nonNullable: true }),
    expiryDate: new FormControl('', { nonNullable: true }),
    mandatory: new FormControl(true, { nonNullable: true }),
  });
  readonly bankForm = new FormGroup({
    accountName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    iban: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    bankName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    primary: new FormControl(true, { nonNullable: true }),
  });

  readonly phoneError = computed(() => {
    const ctrl = this.form.controls.phone;
    if ((this.submitted() || ctrl.touched) && ctrl.hasError('pattern')) {
      return 'رقم الهاتف غير صحيح. يرجى إدخال رقم هاتف صحيح (مثال: 01012345678)';
    }
    return null;
  });

  readonly emailError = computed(() => {
    const ctrl = this.form.controls.email;
    if ((this.submitted() || ctrl.touched) && ctrl.hasError('pattern')) {
      return 'البريد الإلكتروني غير صحيح';
    }
    return null;
  });

  constructor() {
    void this.store.load();
  }

  openNew(): void {
    this.editingId.set(null);
    this.submitted.set(false);
    this.duplicateWarning.set(null);
    this.form.reset({
      code: '',
      name: '',
      nameEn: '',
      partyType: 'SUPPLIER',
      contactPerson: '',
      phone: '',
      email: '',
      address: '',
      notes: '',
      active: true,
      managedType: 'DIRECT',
      responsiblePartyId: '',
      relationshipStartDate: '',
      relationshipEndDate: '',
      currencyCode: 'EGP',
      invoicePolicy: 'E_INVOICE',
      paymentTerms: 'CASH',
      taxId: '',
      bankAccount: '',
      supplierCategory: '',
      riskLevel: 'LOW',
      ownerUserId: '',
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
      nameEn: item.nameEn ?? '',
      partyType: item.partyType,
      contactPerson: item.contactPerson ?? '',
      phone: item.phone ?? '',
      email: item.email ?? '',
      address: item.address ?? '',
      notes: item.notes ?? '',
      active: item.active,
      managedType: item.managedType,
      responsiblePartyId: item.responsiblePartyId ?? '',
      relationshipStartDate: item.relationshipStartDate ?? '',
      relationshipEndDate: item.relationshipEndDate ?? '',
      currencyCode: item.currencyCode,
      invoicePolicy: item.invoicePolicy,
      paymentTerms: item.paymentTerms,
      taxId: item.taxId ?? '',
      bankAccount: item.bankAccount ?? '',
      supplierCategory: item.supplierCategory ?? '',
      riskLevel: item.riskLevel ?? 'LOW',
      ownerUserId: item.ownerUserId ?? '',
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
    if (!this.editingId() && value.partyType === 'SUPPLIER' && !value.taxId.trim()) {
      this.form.controls.taxId.setErrors({ required: true });
      this.form.controls.taxId.markAsTouched();
      return;
    }
    if (!this.editingId() && value.partyType === 'SUPPLIER' && value.taxId.trim()) {
      const duplicates = await this.store.checkDuplicates(value.taxId.trim(), null, null);
      if (duplicates.duplicateFound) {
        this.duplicateWarning.set(this.i18n.t('parties.duplicateTaxWarning', { name: duplicates.taxIdMatches[0]?.name ?? '' }));
        return;
      }
    }
    const payload: BusinessPartyPayload = {
      ...value,
      contactPerson: value.contactPerson.trim() || null,
      phone: value.phone.trim() || null,
      email: value.email.trim() || null,
      address: value.address.trim() || null,
      nameEn: value.nameEn.trim() || null,
      notes: value.notes.trim() || null,
      responsiblePartyId: value.responsiblePartyId.trim() || null,
      relationshipStartDate: value.relationshipStartDate.trim() || null,
      relationshipEndDate: value.relationshipEndDate.trim() || null,
      taxId: value.taxId.trim() || null,
      bankAccount: value.bankAccount.trim() || null,
      supplierCategory: value.supplierCategory.trim() || null,
      riskLevel: value.riskLevel.trim() || null,
      ownerUserId: value.ownerUserId.trim() || null,
    };
    if (await this.store.save(this.editingId(), payload)) this.closeDrawer();
  }

  async openSupplier360(item: BusinessParty): Promise<void> {
    if (item.partyType !== 'SUPPLIER') { this.openEdit(item); return; }
    if (await this.store.loadSupplier360(item.id)) this.supplierModalOpen.set(true);
  }

  closeSupplier360(): void { this.supplierModalOpen.set(false); this.store.supplier360.set(null); }

  async addDocument(): Promise<void> {
    const supplier = this.store.supplier360()?.supplier;
    const file = this.documentFile();
    if (!supplier || this.documentForm.invalid || !file) { this.documentForm.markAllAsTouched(); return; }
    const v = this.documentForm.getRawValue();
    if (await this.store.addDocument(supplier.id, {
      ...v, documentNumber: v.documentNumber.trim() || null,
      issueDate: v.issueDate || null, expiryDate: v.expiryDate || null,
    }, file)) {
      this.documentForm.reset({ documentType: 'TAX_CARD', documentNumber: '', issueDate: '', expiryDate: '', mandatory: true });
      this.documentFile.set(null);
    }
  }

  onDocumentFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.documentFile.set(input.files?.item(0) ?? null);
  }

  downloadDocumentRequirements(): void {
    void this.sampleTemplates.supplierDocuments();
  }

  async addBankAccount(): Promise<void> {
    const supplier = this.store.supplier360()?.supplier;
    if (!supplier || this.bankForm.invalid) { this.bankForm.markAllAsTouched(); return; }
    const duplicate = await this.store.checkDuplicates(null, this.bankForm.controls.iban.value, supplier.id);
    if (duplicate.duplicateFound) {
      this.store.error.set(this.i18n.t('parties.duplicateBankWarning', { name: duplicate.bankMatches[0]?.name ?? '' }));
      return;
    }
    if (await this.store.addBankAccount(supplier.id, this.bankForm.getRawValue())) {
      this.bankForm.reset({ accountName: '', iban: '', bankName: '', currencyCode: 'EGP', primary: true });
    }
  }

  async supplierTransition(action: 'submit' | 'approve' | 'activate' | 'suspend' | 'blacklist'): Promise<void> {
    const supplier = this.store.supplier360()?.supplier;
    if (!supplier) return;
    let reason = '';
    if (action === 'blacklist') {
      reason = window.prompt(this.i18n.t('parties.blacklistReasonPrompt'))?.trim() ?? '';
      if (!reason) return;
    }
    await this.store.transition(supplier.id, action, reason);
  }

  statusLabel(status: string): string { return this.i18n.t(`parties.status.${status}`); }

  complianceLabel(code: string): string {
    switch (code) {
      case 'TAX_ID': return this.i18n.t('parties.compliance.taxId');
      case 'MANDATORY_DOCUMENTS': return this.i18n.t('parties.compliance.documents');
      case 'BANK_VERIFICATION': return this.i18n.t('parties.compliance.bank');
      default: return code;
    }
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
