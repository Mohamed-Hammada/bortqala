import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { dateInputToEpoch, epochToDateInput, formatDate } from '../../core/date';
import { Employee, EmployeePayload, EmploymentType } from './employees.models';
import { EmployeesStore } from './employees.store';
@Component({
  selector: 'app-employees-page',
  imports: [ReactiveFormsModule],
  providers: [EmployeesStore],
  templateUrl: './employees.page.html',
  styleUrl: './employees.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeesPage {
  readonly store = inject(EmployeesStore);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly search = signal('');
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
  readonly form = new FormGroup({
    employeeCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    fullName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    deviceUserId: new FormControl('', { nonNullable: true }),
    categoryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    employmentType: new FormControl<EmploymentType>('FIXED', { nonNullable: true }),
    activeFrom: new FormControl(new Date().toISOString().slice(0, 10), {
      nonNullable: true,
      validators: [Validators.required],
    }),
    activeTo: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
  });
  constructor() {
    void this.store.load();
  }
  openNew() {
    this.editingId.set(null);
    this.form.reset({
      employeeCode: '',
      fullName: '',
      deviceUserId: '',
      categoryId: this.store.categories()[0]?.id ?? '',
      employmentType: 'FIXED',
      activeFrom: new Date().toISOString().slice(0, 10),
      activeTo: '',
      active: true,
      version: null,
    });
    this.drawerOpen.set(true);
  }
  openEdit(item: Employee) {
    this.editingId.set(item.id);
    this.form.reset({
      employeeCode: item.employeeCode,
      fullName: item.fullName,
      deviceUserId: item.deviceUserId ?? '',
      categoryId: item.categoryId,
      employmentType: item.employmentType,
      activeFrom: epochToDateInput(item.activeFrom),
      activeTo: item.activeTo ? epochToDateInput(item.activeTo) : '',
      active: item.active,
      version: item.version,
    });
    this.drawerOpen.set(true);
  }
  async submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: EmployeePayload = {
      ...raw,
      deviceUserId: raw.deviceUserId.trim() || null,
      activeFrom: dateInputToEpoch(raw.activeFrom),
      activeTo: raw.activeTo ? dateInputToEpoch(raw.activeTo) : null,
    };
    if (await this.store.save(this.editingId(), payload)) this.drawerOpen.set(false);
  }
  async deactivate(item: Employee) {
    if (confirm(`تعطيل ${item.fullName}؟`)) await this.store.deactivate(item.id);
  }
  typeLabel(value: EmploymentType) {
    return value === 'FIXED' ? 'ثابت' : 'يومية';
  }
  date(value: number) {
    return formatDate(value);
  }
}
