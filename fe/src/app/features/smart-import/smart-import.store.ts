import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { downloadBlob } from '../../core/download';
import { I18nService } from '../../core/i18n.service';
import {
  SmartImportCommitResult,
  SmartImportPreview,
  SmartImportPreviewRow,
  SmartImportWorkflow,
} from './smart-import.models';

@Injectable()
export class SmartImportStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  readonly workflows = signal<SmartImportWorkflow[]>([]);
  readonly preview = signal<SmartImportPreview | null>(null);
  readonly result = signal<SmartImportCommitResult | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadWorkflows(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.workflows.set(await firstValueFrom(this.http.get<SmartImportWorkflow[]>('/api/v1/smart-imports/workflows')));
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async previewFile(workflow: string, file: File): Promise<SmartImportPreview | null> {
    this.loading.set(true);
    this.error.set(null);
    this.result.set(null);
    const data = new FormData();
    data.append('file', file);
    try {
      const preview = await firstValueFrom(
        this.http.post<SmartImportPreview>(`/api/v1/smart-imports/${workflow}/preview`, data),
      );
      this.preview.set(preview);
      return preview;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async commit(workflow: string, skipInvalid: boolean, rows: SmartImportPreviewRow[]): Promise<SmartImportCommitResult | null> {
    const preview = this.preview();
    if (!preview) return null;
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(
        this.http.post<SmartImportCommitResult>(`/api/v1/smart-imports/${workflow}/commit`, {
          previewId: preview.previewId,
          skipInvalid,
          rows: rows.map((row) => ({ rowNumber: row.rowNumber, sheet: row.sheet, values: row.values })),
        }),
      );
      this.result.set(result);
      return result;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return null;
    } finally {
      this.loading.set(false);
    }
  }

  async downloadTemplate(workflow: SmartImportWorkflow, sample: boolean): Promise<void> {
    this.error.set(null);
    try {
      const blob = await firstValueFrom(
        this.http.get(`/api/v1/smart-imports/${workflow.key}/template.xlsx?sample=${sample}`, { responseType: 'blob' }),
      );
      downloadBlob(blob, workflow.templateFileName);
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  async downloadRejected(batchId: string): Promise<void> {
    this.error.set(null);
    try {
      const blob = await firstValueFrom(
        this.http.get(`/api/v1/smart-imports/batches/${batchId}/rejected.xlsx`, { responseType: 'blob' }),
      );
      downloadBlob(blob, `smart-import-rejected-${batchId}.xlsx`);
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  reset(): void {
    this.preview.set(null);
    this.result.set(null);
    this.error.set(null);
  }
}
