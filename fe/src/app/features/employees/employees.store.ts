import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { AttendanceCategory, Employee, EmployeePayload } from './employees.models';
@Injectable()
export class EmployeesStore {
  private readonly http = inject(HttpClient);
  readonly items = signal<Employee[]>([]);
  readonly categories = signal<AttendanceCategory[]>([]);
  readonly loading = signal(false);
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
      this.error.set(apiErrorMessage(e));
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
      this.error.set(apiErrorMessage(e));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
  async deactivate(id: string) {
    try {
      await firstValueFrom(this.http.delete<void>(`/api/v1/employees/${id}`));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    }
  }
  async export() {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get('/api/v1/exports/employees.xlsx', { responseType: 'blob' }),
        ),
        'employees.xlsx',
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    }
  }
}
