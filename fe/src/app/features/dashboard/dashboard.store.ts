import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { Dashboard } from './dashboard.models';
import { I18nService } from '../../core/i18n.service';

@Injectable()
export class DashboardStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private currentRequestId = 0;
  async load(year: number, month: number): Promise<void> {
    const reqId = ++this.currentRequestId;
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.httpClient.get<Dashboard>('/api/v1/dashboard', { params: { year, month } }),
      );
      if (reqId === this.currentRequestId) {
        this.data.set(res);
      }
    } catch (error) {
      if (reqId === this.currentRequestId) {
        this.error.set(apiErrorMessage(error, this.i18n));
      }
    } finally {
      if (reqId === this.currentRequestId) {
        this.loading.set(false);
      }
    }
  }
}
