import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import {
  BiometricDevice,
  BiometricDeviceRequest,
  BiometricDeviceSyncResult,
  BiometricSource,
  BiometricSourceRequest,
  EmployeeCategoryOption,
  ImportBatch,
  ImportPreview,
  UnmatchedIdentity,
} from './imports.models';
@Injectable()
export class ImportsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly batches = signal<ImportBatch[]>([]);
  readonly unmatched = signal<UnmatchedIdentity[]>([]);
  readonly devices = signal<BiometricDevice[]>([]);
  readonly sources = signal<BiometricSource[]>([]);
  readonly categories = signal<EmployeeCategoryOption[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [batches, unmatched, devices, sources, categories] = await Promise.all([
        firstValueFrom(this.http.get<ImportBatch[]>('/api/v1/imports')),
        firstValueFrom(this.http.get<UnmatchedIdentity[]>('/api/v1/imports/unmatched')),
        firstValueFrom(this.http.get<BiometricDevice[]>('/api/v1/imports/devices')),
        firstValueFrom(this.http.get<BiometricSource[]>('/api/v1/imports/sources')),
        firstValueFrom(this.http.get<EmployeeCategoryOption[]>('/api/v1/categories')),
      ]);
      this.batches.set(batches);
      this.unmatched.set(unmatched);
      this.devices.set(devices);
      this.sources.set(sources);
      this.categories.set(categories);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }
  async saveDevice(payload: BiometricDeviceRequest, id?: string): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(id
        ? this.http.put<BiometricDevice>(`/api/v1/imports/devices/${id}`, payload)
        : this.http.post<BiometricDevice>('/api/v1/imports/devices', payload));
      await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
  async syncDevice(id: string): Promise<BiometricDeviceSyncResult | null> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<BiometricDeviceSyncResult>(`/api/v1/imports/devices/${id}/sync`, {}),
      );
      await this.load();
      return result;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }
  async saveSource(payload: BiometricSourceRequest, id?: string): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(id
        ? this.http.put<BiometricSource>(`/api/v1/imports/sources/${id}`, payload)
        : this.http.post<BiometricSource>('/api/v1/imports/sources', payload));
      await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
  async deleteSource(id: string): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.delete<void>(`/api/v1/imports/sources/${id}`));
      await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
  async isDuplicate(sourceId: string, checksum: string): Promise<boolean> {
    try {
      const result = await firstValueFrom(this.http.get<{ duplicate: boolean }>('/api/v1/imports/preflight', {
        params: { sourceId, checksum },
      }));
      return !!result.duplicate;
    } catch {
      return false;
    }
  }

  async upload(file: File, sourceId: string) {
    this.loading.set(true);
    this.error.set(null);
    this.success.set(null);
    const data = new FormData();
    data.append('file', file);
    data.append('sourceId', sourceId);
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
  async preview(file: File): Promise<ImportPreview | null> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = new FormData();
      data.append('file', file);
      return await firstValueFrom(this.http.post<ImportPreview>('/api/v1/imports/preview', data));
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }
  async reverseBatch(id: string): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post<ImportBatch>(`/api/v1/imports/${id}/reverse`, {}));
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
      const fileName = scope === 'imports'
        ? this.i18n.t('imports.exportImportHistory')
        : this.i18n.t('imports.exportUnmatchedIdentities');
      downloadBlob(
        await firstValueFrom(
          this.http.get(`/api/v1/exports/${scope}.xlsx`, { responseType: 'blob' }),
        ),
        timestampedExcelFileName(fileName, scope, this.i18n.locale()),
      );
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }
}
