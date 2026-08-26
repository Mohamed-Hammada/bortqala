import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { AttendanceCategory, Employee, EmployeePayload } from './employees.models';
@Injectable()
export class EmployeesStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly items = signal<Employee[]>([]);
  readonly categories = signal<AttendanceCategory[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [items, categories] = await Promise.all([
        firstValueFrom(this.http.get<Employee[]>('/api/v1/employees')),
        firstValueFrom(this.http.get<AttendanceCategory[]>('/api/v1/categories')),
      ]);
      this.items.set(items);
      this.categories.set(categories.filter((item) => item.active));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }
  async save(id: string | null, payload: EmployeePayload) {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        id
          ? this.http.put<Employee>(`/api/v1/employees/${id}`, payload)
          : this.http.post<Employee>('/api/v1/employees', payload),
      );
      await this.load();
      return true;
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
  async deactivate(id: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`/api/v1/employees/${id}`));
    await this.load();
  }
  async reactivate(id: string, payload: EmployeePayload): Promise<void> {
    await firstValueFrom(this.http.put<Employee>(`/api/v1/employees/${id}`, payload));
    await this.load();
  }
  async export() {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get('/api/v1/exports/employees.xlsx', { responseType: 'blob' }),
        ),
        timestampedExcelFileName(this.i18n.t('export.file.employees'), 'employees', this.i18n.locale()),
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
