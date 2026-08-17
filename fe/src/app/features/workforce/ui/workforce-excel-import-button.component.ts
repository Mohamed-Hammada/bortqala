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
    <!-- BORTQALA_WORKFORCE_UI_20260816_COMPACT_IMPORT -->
    <button class="button secondary workforce-import-trigger" type="button" (click)="openDialog()" [disabled]="busy()">
      <span class="action-icon" aria-hidden="true">⇧</span>
      <span>{{ busy() ? (ar() ? 'جارٍ الاستيراد…' : 'Importing…') : (ar() ? 'استيراد Excel' : 'Import Excel') }}</span>
    </button>

    @if (open()) {
      <div class="import-backdrop" role="presentation" (click)="closeDialog()">
        <section class="import-dialog" role="dialog" aria-modal="true"
                 [attr.aria-label]="ar() ? 'استيراد Excel' : 'Excel import'"
                 (click)="$event.stopPropagation()">
          <header class="dialog-header">
            <div class="dialog-copy">
              <h3>{{ ar() ? 'استيراد من Excel' : 'Import from Excel' }}</h3>
              <p>{{ ar() ? 'حمّل القالب المعتمد، اختر طريقة المطابقة، ثم ارفع الملف.' : 'Download the approved template, choose the matching method, then upload the file.' }}</p>
            </div>
            <button class="dialog-close" type="button" (click)="closeDialog()" [disabled]="busy()" aria-label="Close">×</button>
          </header>

          <div class="dialog-body">
            <button class="button secondary dialog-action" type="button" (click)="downloadTemplate()" [disabled]="busy()">
              <span aria-hidden="true">⇩</span>
              <span>{{ ar() ? 'تحميل قالب Excel' : 'Download Excel template' }}</span>
            </button>

            <label class="identity-mode">
              <span>{{ ar() ? 'طريقة المطابقة' : 'Matching method' }}</span>
              <select [value]="identityMode()" (change)="setMode($event)" [disabled]="busy()">
                <option value="USER_CODE">{{ ar() ? 'بكود المستخدم / العامل' : 'User / worker code' }}</option>
                <option value="AUTO">{{ ar() ? 'تلقائي (الكود ثم الهوية)' : 'Automatic (code, then identity)' }}</option>
              </select>
            </label>

            <p class="template-hint">{{ ar() ? 'ابدأ بالقالب لتجنب أخطاء أسماء الأعمدة والحقول المطلوبة.' : 'Start from the template to avoid header and required-field errors.' }}</p>

            <input #excelInput class="hidden-file" type="file" accept=".xlsx,.xls"
                   (change)="upload($event)" [disabled]="busy()" />
            <button class="button gold dialog-action" type="button" (click)="excelInput.click()" [disabled]="busy()">
              {{ busy() ? (ar() ? 'جارٍ الاستيراد…' : 'Importing…') : (ar() ? 'اختيار الملف وبدء الاستيراد' : 'Choose file and import') }}
            </button>

            @if (message()) {
              <div class="import-message" [class.error]="failed()">{{ message() }}</div>
            }
            @if (errors().length) {
              <details class="row-errors" open>
                <summary>{{ ar() ? 'أخطاء الصفوف' : 'Row errors' }} ({{ errors().length }})</summary>
                <ul>
                  @for (item of errors().slice(0, 20); track item.rowNumber) {
                    <li><strong>{{ ar() ? 'صف' : 'Row' }} {{ item.rowNumber }}:</strong> {{ item.message }}</li>
                  }
                </ul>
                @if (errors().length > 20) {
                  <small>{{ ar() ? 'يتم عرض أول 20 خطأ فقط.' : 'Showing the first 20 errors.' }}</small>
                }
              </details>
            }
          </div>
        </section>
      </div>
    }
  `,
  styles: [`
    :host { display:inline-flex; min-width:0; vertical-align:middle; }
    .workforce-import-trigger {
      min-height:44px;
      padding:.7rem 1rem;
      display:inline-flex;
      align-items:center;
      justify-content:center;
      gap:.45rem;
      white-space:nowrap;
      border-radius:10px;
    }
    .action-icon { font-size:1rem; line-height:1; }
    .import-backdrop {
      position:fixed;
      inset:0;
      z-index:1200;
      display:grid;
      place-items:center;
      padding:1rem;
      background:rgba(15,23,42,.48);
      backdrop-filter:blur(2px);
    }
    .import-dialog {
      width:min(94vw,560px);
      max-height:min(86dvh,720px);
      overflow:auto;
      border:1px solid var(--line,#d9dde5);
      border-radius:16px;
      background:var(--surface,#fff);
      color:var(--ink,#111827);
      box-shadow:0 24px 70px rgba(15,23,42,.22);
    }
    .dialog-header {
      display:flex;
      align-items:flex-start;
      justify-content:space-between;
      gap:1rem;
      padding:1rem 1rem .85rem;
      border-bottom:1px solid var(--line,#e5e7eb);
    }
    .dialog-copy { min-width:0; }
    .dialog-header h3 { margin:0; font-size:1.05rem; }
    .dialog-header p { margin:.3rem 0 0; color:var(--muted,#667085); font-size:.84rem; line-height:1.55; }
    .dialog-close {
      flex:0 0 auto;
      width:36px;
      height:36px;
      border:0;
      border-radius:9px;
      background:transparent;
      color:inherit;
      font-size:1.55rem;
      line-height:1;
      cursor:pointer;
    }
    .dialog-close:hover { background:rgba(127,127,127,.1); }
    .dialog-body { display:grid; gap:.85rem; padding:1rem; }
    .dialog-action { min-height:44px; justify-content:center; }
    .identity-mode { display:grid; gap:.35rem; font-size:.83rem; color:var(--muted,#667085); }
    .identity-mode select {
      width:100%;
      min-height:44px;
      padding:.6rem .7rem;
      border:1px solid var(--line,#d9dde5);
      border-radius:10px;
      background:var(--surface,#fff);
      color:var(--ink,#111827);
    }
    .hidden-file { display:none; }
    .template-hint { margin:0; color:var(--muted,#667085); font-size:.82rem; line-height:1.5; }
    .import-message { padding:.7rem .8rem; border-radius:10px; background:#ecfdf3; color:#027a48; font-weight:650; }
    .import-message.error { background:#fef3f2; color:var(--danger,#b42318); }
    .row-errors { border-top:1px solid var(--line,#d9dde5); padding-top:.7rem; }
    .row-errors summary { cursor:pointer; font-weight:700; }
    .row-errors ul { margin:.55rem 0; padding-inline-start:1.2rem; max-height:220px; overflow:auto; }
    .row-errors li { margin:.25rem 0; font-size:.78rem; }
    @media (max-width:640px) {
      :host { width:100%; }
      .workforce-import-trigger { width:100%; }
      .import-dialog { width:100%; }
      .dialog-body .button { width:100%; }
    }
  `],
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

  ar(): boolean { return this.i18n.locale().toLowerCase().startsWith('ar'); }
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
      const body = new FormData();
      body.append('file', file);
      const result = await firstValueFrom(this.http.post<WorkforceExcelImportResult>(
        `/api/v1/workforce/excel-import/${this.kind}`,
        body,
        { params: { identityMode: this.identityMode() } },
      ));
      this.failed.set(result.failedRows > 0);
      this.errors.set(result.errors ?? []);
      this.message.set(this.ar()
        ? `تم استيراد ${result.importedRows} من ${result.totalRows}.${result.failedRows ? ` تعذر ${result.failedRows}.` : ''}`
        : `Imported ${result.importedRows} of ${result.totalRows}.${result.failedRows ? ` ${result.failedRows} failed.` : ''}`);
      if (result.importedRows > 0) window.setTimeout(() => window.location.reload(), 450);
    } catch (error: any) {
      this.failed.set(true);
      this.message.set(error?.error?.message || (this.ar() ? 'تعذر استيراد ملف Excel.' : 'Could not import the Excel file.'));
    } finally {
      this.busy.set(false);
      input.value = '';
    }
  }
}
