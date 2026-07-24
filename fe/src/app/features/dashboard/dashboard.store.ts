import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { Dashboard } from './dashboard.models';

@Injectable()
export class DashboardStore {
  private readonly httpClient = inject(HttpClient);
  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  async load(year: number, month: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.data.set(
        await firstValueFrom(
          this.httpClient.get<Dashboard>('/api/v1/dashboard', { params: { year, month } }),
        ),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
