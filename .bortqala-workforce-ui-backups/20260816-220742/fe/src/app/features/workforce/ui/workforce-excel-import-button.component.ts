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
      <div class="excel-actions">
        <button class="button secondary" type="button" (click)="downloadTemplate()" [disabled]="busy()">
          ⇩ {{ ar() ? 'تحميل قالب Excel' : 'Download Excel template' }}
        </button>
        <input #excelInput class="hidden-file" type="file" accept=".xlsx,.xls"
               (change)="upload($event)" [disabled]="busy()" />
        <button class="button gold" type="button" (click)="excelInput.click()" [disabled]="busy()">
          {{ busy() ? (ar() ? 'جارٍ الاستيراد…' : 'Importing…') : (ar() ? '⇧ استيراد Excel' : '⇧ Import Excel') }}
        </button>
      </div>
      <label class="identity-mode">
        <span>{{ ar() ? 'طريقة المطابقة' : 'Matching method' }}</span>
        <select [value]="identityMode()" (change)="setMode($event)" [disabled]="busy()">
          <option value="USER_CODE">{{ ar() ? 'بكود المستخدم / العامل' : 'User / worker code' }}</option>
          <option value="AUTO">{{ ar() ? 'تلقائي (الكود ثم الهوية)' : 'Automatic (code, then identity)' }}</option>
        </select>
      </label>
      <small class="template-hint">{{ ar() ? 'ابدأ بالقالب لتجنب أخطاء أسماء الأعمدة والحقول المطلوبة.' : 'Start from the template to avoid header and required-field errors.' }}</small>
      @if (message()) { <small class="import-message" [class.error]="failed()">{{ message() }}</small> }
      @if (errors().length) {
        <details class="row-errors">
          <summary>{{ ar() ? 'عرض أخطاء الصفوف' : 'Show row errors' }} ({{ errors().length }})</summary>
          <ul>
            @for (item of errors().slice(0, 20); track item.rowNumber) {
              <li><strong>{{ ar() ? 'صف' : 'Row' }} {{ item.rowNumber }}:</strong> {{ item.message }}</li>
            }
          </ul>
          @if (errors().length > 20) { <small>{{ ar() ? 'يتم عرض أول 20 خطأ فقط.' : 'Showing the first 20 errors.' }}</small> }
        </details>
      }
    </div>
  `,
  styles: [`
    :host { display: block; min-width:min(100%, 420px); }
    .workforce-excel-import { display:grid; gap:.55rem; padding:.7rem; border:1px solid var(--line,#d9dde5); border-radius:12px; background:var(--surface-muted,#f8fafc); }
    .excel-actions { display:flex; align-items:center; flex-wrap:wrap; gap:.45rem; }
    .identity-mode { display:grid; gap:.25rem; font-size:.78rem; color:var(--muted, #667085); }
    .identity-mode select { width:100%; padding:.52rem .62rem; border:1px solid var(--line, #d9dde5); border-radius:9px; background:var(--surface, #fff); color:var(--ink, #111827); }
    .hidden-file { display:none; }
    .template-hint { color:var(--muted,#667085); }
    .import-message { font-weight:650; }
    .import-message.error { color:var(--danger, #b42318); }
    .row-errors { border-top:1px solid var(--line,#d9dde5); padding-top:.5rem; }
    .row-errors summary { cursor:pointer; font-weight:700; }
    .row-errors ul { margin:.45rem 0; padding-inline-start:1.2rem; max-height:180px; overflow:auto; }
    .row-errors li { margin:.2rem 0; font-size:.78rem; }
    @media (max-width:720px) { .excel-actions,.excel-actions .button { width:100%; } .excel-actions { display:grid; } }
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
  readonly errors = signal<{ rowNumber: number; message: string }[]>([]);

  ar(): boolean { return this.i18n.locale().toLowerCase().startsWith('ar'); }

  setMode(event: Event): void {
    this.identityMode.set((event.target as HTMLSelectElement).value === 'AUTO' ? 'AUTO' : 'USER_CODE');
  }

  async downloadTemplate(): Promise<void> {
    this.failed.set(false); this.message.set(''); this.errors.set([]);
    try {
      const blob = await firstValueFrom(this.http.get(`/api/v1/workforce/excel-import/template/${this.kind}`, { responseType: 'blob' }));
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url; anchor.download = `workforce-${this.kind}-import-template.xlsx`; anchor.click();
      URL.revokeObjectURL(url);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || (this.ar() ? 'تعذر تحميل قالب Excel.' : 'Could not download the Excel template.'));
    }
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
    if (file.size > 20 * 1024 * 1024) {
      this.failed.set(true);
      this.message.set(this.ar() ? 'حجم الملف يتجاوز 20 ميجابايت.' : 'The file exceeds the 20 MB limit.');
      input.value = '';
      return;
    }
    this.busy.set(true);
    this.failed.set(false);
    this.message.set('');
    this.errors.set([]);
    try {
      const body = new FormData(); body.append('file', file);
      const result = await firstValueFrom(this.http.post<WorkforceExcelImportResult>(
        `/api/v1/workforce/excel-import/${this.kind}`, body, { params: { identityMode: this.identityMode() } },
      ));
      this.failed.set(result.failedRows > 0);
      this.errors.set(result.errors ?? []);
      this.message.set(this.ar()
        ? `تم استيراد ${result.importedRows} من ${result.totalRows}. ${result.failedRows ? `تعذر ${result.failedRows}.` : ''}`
        : `Imported ${result.importedRows} of ${result.totalRows}. ${result.failedRows ? `${result.failedRows} failed.` : ''}`);
      if (result.importedRows > 0) window.setTimeout(() => window.location.reload(), 450);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || (this.ar() ? 'تعذر استيراد ملف Excel.' : 'Could not import the Excel file.'));
    } finally {
      this.busy.set(false); input.value = '';
    }
  }
}
