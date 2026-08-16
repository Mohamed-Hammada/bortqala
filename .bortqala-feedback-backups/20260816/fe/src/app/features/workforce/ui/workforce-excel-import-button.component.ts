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
  template: `
    <div class="workforce-excel-import">
      <label class="identity-mode">
        <span>{{ ar() ? 'المطابقة' : 'Matching' }}</span>
        <select [value]="identityMode()" (change)="setMode($event)" [disabled]="busy()">
          <option value="USER_CODE">{{ ar() ? 'بكود المستخدم / العامل' : 'User / worker code' }}</option>
          <option value="AUTO">{{ ar() ? 'تلقائي (الكود ثم الهوية)' : 'Automatic (code, then identity)' }}</option>
        </select>
      </label>
      <input #excelInput class="hidden-file" type="file" accept=".xlsx,.xls"
             (change)="upload($event)" [disabled]="busy()" />
      <button class="button secondary" type="button" (click)="excelInput.click()" [disabled]="busy()">
        {{ busy() ? (ar() ? 'جارٍ الاستيراد…' : 'Importing…') : (ar() ? '⇧ استيراد Excel' : '⇧ Import Excel') }}
      </button>
      @if (message()) { <small class="import-message" [class.error]="failed()">{{ message() }}</small> }
    </div>
  `,
  styles: [`
    :host { display: inline-flex; }
    .workforce-excel-import { display:flex; align-items:end; flex-wrap:wrap; gap:.5rem; }
    .identity-mode { display:grid; gap:.2rem; font-size:.78rem; color:var(--muted, #667085); }
    .identity-mode select { min-width:175px; padding:.48rem .6rem; border:1px solid var(--line, #d9dde5); border-radius:9px; background:var(--surface, #fff); color:var(--ink, #111827); }
    .hidden-file { display:none; }
    .import-message { flex-basis:100%; max-width:420px; }
    .import-message.error { color:var(--danger, #b42318); }
    @media (max-width:720px) { :host,.workforce-excel-import,.identity-mode,.identity-mode select { width:100%; } .button { width:100%; } }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkforceExcelImportButtonComponent {
  @Input({ required: true }) kind!: 'workers' | 'contractors';
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly identityMode = signal<'USER_CODE' | 'AUTO'>('USER_CODE');
  readonly busy = signal(false);
  readonly message = signal('');
  readonly failed = signal(false);

  ar(): boolean { return this.i18n.locale().toLowerCase().startsWith('ar'); }

  setMode(event: Event): void {
    this.identityMode.set((event.target as HTMLSelectElement).value === 'AUTO' ? 'AUTO' : 'USER_CODE');
  }

  async upload(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      this.failed.set(true);
      this.message.set(this.ar() ? 'اختر ملف Excel بصيغة xlsx أو xls.' : 'Choose an .xlsx or .xls file.');
      input.value = '';
      return;
    }
    this.busy.set(true);
    this.failed.set(false);
    this.message.set('');
    try {
      const body = new FormData();
      body.append('file', file);
      const result = await firstValueFrom(this.http.post<WorkforceExcelImportResult>(
        `/api/v1/workforce/excel-import/${this.kind}`,
        body,
        { params: { identityMode: this.identityMode() } },
      ));
      this.failed.set(result.failedRows > 0);
      this.message.set(this.ar()
        ? `تم استيراد ${result.importedRows} من ${result.totalRows}. ${result.failedRows ? `تعذر ${result.failedRows}.` : ''}`
        : `Imported ${result.importedRows} of ${result.totalRows}. ${result.failedRows ? `${result.failedRows} failed.` : ''}`);
      if (result.importedRows > 0) window.setTimeout(() => window.location.reload(), 600);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || (this.ar() ? 'تعذر استيراد ملف Excel.' : 'Could not import the Excel file.'));
    } finally {
      this.busy.set(false);
      input.value = '';
    }
  }
}
