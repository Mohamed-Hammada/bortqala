import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { Branch, Company, Department, OrganizationHierarchy, Warehouse } from './organization.models';

@Component({
  selector: 'app-organization-page',
  imports: [ReactiveFormsModule],
  templateUrl: './organization.page.html',
  styleUrl: './organization.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activeTab = signal<'companies' | 'branches' | 'warehouses' | 'departments'>('companies');

  readonly companies = signal<Company[]>([]);
  readonly branches = signal<Branch[]>([]);
  readonly warehouses = signal<Warehouse[]>([]);
  readonly departments = signal<Department[]>([]);

  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);

  readonly companyForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    taxNumber: new FormControl('', { nonNullable: true }),
    commercialRegistry: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly branchForm = new FormGroup({
    companyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    location: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly warehouseForm = new FormGroup({
    branchId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    location: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly departmentForm = new FormGroup({
    companyId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    managerId: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<OrganizationHierarchy>('/api/v1/organization'),
      );
      this.companies.set(data.companies);
      this.branches.set(data.branches);
      this.warehouses.set(data.warehouses);
      this.departments.set(data.departments);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.editingId.set(null);
    const tab = this.activeTab();
    if (tab === 'companies') {
      this.companyForm.reset({ code: '', name: '', taxNumber: '', commercialRegistry: '', active: true });
    } else if (tab === 'branches') {
      this.branchForm.reset({ companyId: this.companies()[0]?.id ?? '', code: '', name: '', location: '', active: true });
    } else if (tab === 'warehouses') {
      this.warehouseForm.reset({ branchId: this.branches()[0]?.id ?? '', code: '', name: '', location: '', active: true });
    } else {
      this.departmentForm.reset({ companyId: this.companies()[0]?.id ?? '', code: '', name: '', managerId: '', active: true });
    }
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitCompany() {
    if (this.companyForm.invalid) return;
    try {
      const payload = this.companyForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/companies/${id}`, payload)
          : this.http.post('/api/v1/organization/companies', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitBranch() {
    if (this.branchForm.invalid) return;
    try {
      const payload = this.branchForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/branches/${id}`, payload)
          : this.http.post('/api/v1/organization/branches', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitWarehouse() {
    if (this.warehouseForm.invalid) return;
    try {
      const payload = this.warehouseForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/warehouses/${id}`, payload)
          : this.http.post('/api/v1/organization/warehouses', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async submitDepartment() {
    if (this.departmentForm.invalid) return;
    try {
      const payload = this.departmentForm.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/organization/departments/${id}`, payload)
          : this.http.post('/api/v1/organization/departments', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
