import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { BusinessParty, BusinessPartyPayload } from './parties.models';

@Injectable()
export class PartiesStore {
  private readonly httpClient = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly items = signal<BusinessParty[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.items.set(await firstValueFrom(this.httpClient.get<BusinessParty[]>('/api/v1/parties')));
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async save(id: string | null, payload: BusinessPartyPayload): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(
        id
          ? this.httpClient.put<BusinessParty>(`/api/v1/parties/${id}`, payload)
          : this.httpClient.post<BusinessParty>('/api/v1/parties', payload),
      );
      await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error));
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  async deactivate(id: string): Promise<void> {
    try {
      await firstValueFrom(this.httpClient.delete<void>(`/api/v1/parties/${id}`));
      await this.load();
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    }
  }

  async export(): Promise<void> {
    try {
      downloadBlob(
        await firstValueFrom(
          this.httpClient.get('/api/v1/exports/parties.xlsx', { responseType: 'blob' }),
        ),
        timestampedExcelFileName('جهات-التعامل', 'business-parties', this.i18n.locale()),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error));
    }
  }
}
