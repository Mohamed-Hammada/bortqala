import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import { ImportBatch, UnmatchedIdentity } from './imports.models';
@Injectable()
export class ImportsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly batches = signal<ImportBatch[]>([]);
  readonly unmatched = signal<UnmatchedIdentity[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [batches, unmatched] = await Promise.all([
        firstValueFrom(this.http.get<ImportBatch[]>('/api/v1/imports')),
        firstValueFrom(this.http.get<UnmatchedIdentity[]>('/api/v1/imports/unmatched')),
      ]);
      this.batches.set(batches);
      this.unmatched.set(unmatched);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }
  async upload(file: File, deviceName: string) {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    const data = new FormData();
    data.append('file', file);
    data.append('deviceName', deviceName);
    try {
      const result = await firstValueFrom(this.http.post<ImportBatch>('/api/v1/imports', data));
      this.success.set(
        result.duplicate
          ? this.i18n.t('imports.duplicateSuccess')
          : this.i18n.t(result.errorRows ? 'imports.savedWithErrors' : 'imports.saved', {
              imported: result.importedRows,
              errors: result.errorRows,
            }),
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
  async export(scope: 'imports' | 'unmatched') {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get(`/api/v1/exports/${scope}.xlsx`, { responseType: 'blob' }),
        ),
        timestampedExcelFileName(
          this.i18n.locale() === 'ar-EG'
            ? scope === 'imports'
              ? 'سجل-الاستيراد'
              : 'هويات-غير-مربوطة'
            : scope === 'imports'
              ? 'import-history'
              : 'unmatched-identities',
          scope,
          this.i18n.locale(),
        ),
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
