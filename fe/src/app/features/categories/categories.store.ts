import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { AttendanceCategory, CategoryPayload } from './categories.models';
@Injectable()
export class CategoriesStore {
  private readonly http = inject(HttpClient);
  readonly items = signal<AttendanceCategory[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.items.set(
        await firstValueFrom(this.http.get<AttendanceCategory[]>('/api/v1/categories')),
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }
  async save(id: string | null, payload: CategoryPayload) {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        id
          ? this.http.put<AttendanceCategory>(`/api/v1/categories/${id}`, payload)
          : this.http.post<AttendanceCategory>('/api/v1/categories', payload),
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
    this.loading.set(true);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/v1/categories/${id}`));
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.loading.set(false);
    }
  }
  async export() {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get('/api/v1/exports/categories.xlsx', { responseType: 'blob' }),
        ),
        'categories.xlsx',
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    }
  }
}
