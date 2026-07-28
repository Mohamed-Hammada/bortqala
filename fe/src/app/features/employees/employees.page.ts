import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { dateInputToEpoch, epochToDateInput, formatDateReadable } from '../../core/date';
import { Employee, EmployeePayload, EmploymentType } from './employees.models';
import { EmployeesStore } from './employees.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-employees-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TablePaginationComponent,
    DecimalPipe,
    SkeletonComponent,
    EmptyStateComponent,
    ModalDialogComponent,
  ],
  providers: [EmployeesStore],
  templateUrl: './employees.page.html',
  styleUrl: './employees.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeesPage {
  readonly auth = inject(AuthService);
  readonly store = inject(EmployeesStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly confirmAction = signal<{ message: string; onConfirm: () => void } | null>(null);
  readonly drawerOpen = signal(false);
  readonly submitAttempted = signal(false);
  readonly pagination = new TablePagination();
  readonly editingId = signal<string | null>(null);
  readonly search = signal('');

  isBiometricCategorySelected(): boolean {
    const selectedId = this.form.controls.categoryId.value;
    const cat = this.store.categories().find((c) => c.id === selectedId);
    return cat?.attendanceMode === 'BIOMETRIC';
  }

  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.store
      .items()
      .filter(
        (item) =>
          !q ||
          item.fullName.toLowerCase().includes(q) ||
          item.employeeCode.toLowerCase().includes(q) ||
          (item.deviceUserId ?? '').toLowerCase().includes(q),
      );
  });

  readonly paged = computed(() => this.pagination.slice(this.filtered()));

  readonly form = new FormGroup({
    employeeCode: new FormControl('', { nonNullable: true }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    deviceUserId: new FormControl('', { nonNullable: true }),
    categoryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    employmentType: new FormControl<EmploymentType>('FIXED', { nonNullable: true }),
    baseSalary: new FormControl<number>(0, { nonNullable: true, validators: [Validators.min(0)] }),
    activeFrom: new FormControl(new Date().toISOString().slice(0, 10), {
      nonNullable: true,
      validators: [Validators.required],
    }),
    activeTo: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });

  constructor() {
    void this.reload();
    this.form.controls.categoryId.valueChanges.subscribe((catId) => {
      const cat = this.store.categories().find((c) => c.id === catId);
      if (cat?.attendanceMode === 'BIOMETRIC') {
        this.form.controls.deviceUserId.setValidators([Validators.required]);
      } else {
        this.form.controls.deviceUserId.clearValidators();
      }
      this.form.controls.deviceUserId.updateValueAndValidity();
    });
  }

  async reload(): Promise<void> {
    await this.store.load();
  }

  openNew() {
    this.submitAttempted.set(false);
    this.editingId.set(null);
    this.form.reset({
      employeeCode: '',
      fullName: '',
      deviceUserId: '',
      categoryId: this.store.categories()[0]?.id ?? '',
      employmentType: 'FIXED',
      baseSalary: 0,
      activeFrom: new Date().toISOString().slice(0, 10),
      activeTo: '',
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }

  openEdit(item: Employee) {
    this.submitAttempted.set(false);
    this.editingId.set(item.id);
    this.form.reset({
      employeeCode: item.employeeCode,
      fullName: item.fullName,
      deviceUserId: item.deviceUserId ?? '',
      categoryId: item.categoryId,
      employmentType: item.employmentType,
      baseSalary: item.baseSalary ?? 0,
      activeFrom: epochToDateInput(item.activeFrom),
      activeTo: item.activeTo ? epochToDateInput(item.activeTo) : '',
      active: item.active,
      version: item.version,
    });
    this.drawerOpen.set(true);
  }

  async submit() {
    this.submitAttempted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: EmployeePayload = {
      ...raw,
      deviceUserId: raw.deviceUserId.trim() || null,
      baseSalary: Number(raw.baseSalary) || 0,
      activeFrom: dateInputToEpoch(raw.activeFrom),
      activeTo: raw.activeTo ? dateInputToEpoch(raw.activeTo) : null,
    };
    if (await this.store.save(this.editingId(), payload)) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.submitAttempted.set(false);
      this.drawerOpen.set(false);
    }
  }

  deactivate(item: Employee) {
    this.confirmAction.set({
      message: this.i18n.t('employees.deactivateConfirm', { name: item.fullName }),
      onConfirm: () => {
        this.confirmAction.set(null);
        this.store.deactivate(item.id).then(() => {
          this.notification.info(this.i18n.t('common.save') + ' ✓');
        });
      },
    });
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.submitAttempted.set(false);
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && this.drawerOpen();
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

  typeLabel(value: EmploymentType) {
    return this.i18n.t(value === 'FIXED' ? 'employment.fixed' : 'employment.daily');
  }

  date(value: number) {
    return formatDateReadable(value, this.i18n.locale());
  }
}
