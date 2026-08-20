import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';

interface WorkforceExcelImportResult {
  totalRows: number;
  importedRows: number;
  failedRows: number;
  errors: { rowNumber: number; message: string }[];
  identityMode: string;
}

@Component({
  selector: 'app-workforce-excel-import-button',
  standalone: true,
  templateUrl: './workforce-excel-import-button.component.html',
  styleUrls: ['./workforce-excel-import-button.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkforceExcelImportButtonComponent {
  @Input({ required: true }) kind!: 'workers' | 'contractors';
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly open = signal(false);
  readonly identityMode = signal<'USER_CODE' | 'AUTO'>('USER_CODE');
  readonly busy = signal(false);
  readonly message = signal('');
  readonly failed = signal(false);
  readonly errors = signal<{ rowNumber: number; message: string }[]>([]);

  openDialog(): void { this.open.set(true); }
  closeDialog(): void { if (!this.busy()) this.open.set(false); }

  setMode(event: Event): void {
    this.identityMode.set((event.target as HTMLSelectElement).value === 'AUTO' ? 'AUTO' : 'USER_CODE');
  }

  async downloadTemplate(): Promise<void> {
    this.failed.set(false);
    this.message.set('');
    this.errors.set([]);
    try {
      const blob = await firstValueFrom(this.http.get(
        `/api/v1/workforce/excel-import/template/${this.kind}`,
        { responseType: 'blob' },
      ));
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `workforce-${this.kind}-import-template.xlsx`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || this.i18n.t('workforce.importTemplateDownloadFailed'));
    }
  }

  async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      this.failed.set(true);
      this.message.set(this.i18n.t('workforce.importInvalidFileFormat'));
      input.value = '';
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      this.failed.set(true);
      this.message.set(this.i18n.t('workforce.importFileTooLarge'));
      input.value = '';
      return;
    }

    this.busy.set(true);
    this.failed.set(false);
    this.message.set('');
    this.errors.set([]);
    try {
      const body = new FormData();
      body.append('file', file);
      const result = await firstValueFrom(this.http.post<WorkforceExcelImportResult>(
        `/api/v1/workforce/excel-import/${this.kind}`,
        body,
        { params: { identityMode: this.identityMode() } },
      ));
      this.failed.set(result.failedRows > 0);
      this.errors.set(result.errors ?? []);
      this.message.set(
        this.i18n.t('workforce.importResult', { imported: result.importedRows, total: result.totalRows })
        + (result.failedRows ? ' ' + this.i18n.t('workforce.importResultFailed', { failed: result.failedRows }) : '')
      );
      if (result.importedRows > 0) window.setTimeout(() => window.location.reload(), 450);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || this.i18n.t('workforce.importFailed'));
    } finally {
      this.busy.set(false);
      input.value = '';
    }
  }
}
