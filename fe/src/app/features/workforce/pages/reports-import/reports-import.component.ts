import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceImportBatch, WorkforceImportCommit, WorkforceImportValidation } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { downloadBlob, exportCsv } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-reports-import',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page" dir="rtl">
      <header><span class="eyebrow">التكامل والتقارير</span><h1>استيراد تقارير العمال من Excel</h1><p>دورة محفوظة من رفع الملف حتى المعاينة والتنفيذ والتراجع.</p></header>
      <ol class="steps"><li [class.active]="step() >= 1">1 رفع الملف</li><li [class.active]="step() >= 2">2 مطابقة الأعمدة</li><li [class.active]="step() >= 3">3 التحقق</li><li [class.active]="step() >= 4">4 المعاينة</li><li [class.active]="step() >= 5">5 التنفيذ</li><li [class.active]="step() >= 6">6 النتائج</li></ol>
      @if (error()) { <div class="alert error">{{ error() }}</div> }

      <div class="grid">
        <article class="card workflow">
          <section>
            <h2>1. رفع ملف XLSX</h2>
            <input #fileInput type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,.csv" (change)="selectFile($event)" />
            <button class="btn primary" type="button" [disabled]="!selectedFile() || busy()" (click)="upload()">{{ busy() ? 'جارٍ الرفع…' : 'رفع وحفظ الملف الأصلي' }}</button>
            <button class="btn" type="button" (click)="downloadTemplate()">{{ i18n.t('reportsImport.downloadTemplate') }}</button>
          </section>

          @if (batch(); as current) {
            <section><div class="batch-head"><h2>2. مطابقة الأعمدة</h2><span class="status">{{ statusLabel(current.status) }}</span></div>
              <p>{{ current.fileName }} · بصمة <code>{{ current.checksum.slice(0, 12) }}</code></p>
              <div class="mapping"><label>كود العامل<select [(ngModel)]="mapping.workerCode">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label><label>تاريخ العمل<select [(ngModel)]="mapping.workDate">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label><label>قيمة الحضور<select [(ngModel)]="mapping.attendanceValue">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label></div>
              <button class="btn" type="button" [disabled]="busy() || !mappingComplete()" (click)="saveMapping()">حفظ المطابقة</button>
              <button class="btn primary" type="button" [disabled]="busy() || current.status !== 'MAPPED'" (click)="validate()">3. التحقق من البيانات</button>
            </section>
          }

          @if (validation(); as result) {
            <section><h2>4. معاينة قبل الحفظ</h2><div class="summary"><span>إجمالي <b>{{ result.batch.totalRows }}</b></span><span class="ok">صحيح <b>{{ result.batch.validRows }}</b></span><span class="bad">أخطاء <b>{{ result.batch.invalidRows }}</b></span></div>
              @if (result.batch.invalidRows > 0) { <div class="alert warning">لن تُخفى الأخطاء. يمكنك تنزيل ملف الأخطاء أو تنفيذ الصفوف الصحيحة فقط.<button class="link" type="button" (click)="downloadErrors(result.batch)">تنزيل ملف الأخطاء Excel</button></div> }
              <div class="table-wrap"><table><thead><tr><th>الصف</th><th>العامل</th><th>التاريخ</th><th>الحضور</th><th>الحالة / السبب</th></tr></thead><tbody>@for (row of result.preview; track row.rowNumber) {<tr [class.invalid]="row.validationStatus === 'INVALID'"><td>{{ row.rowNumber }}</td><td>{{ row.workerCode }}<small>{{ row.workerName }}</small></td><td>{{ row.workDate || '—' }}</td><td>{{ row.attendanceValue ?? '—' }}</td><td>{{ row.validationStatus === 'VALID' ? 'صحيح' : row.errorMessage }}</td></tr>}</tbody></table></div>
              @if (result.batch.invalidRows > 0) { <label class="check"><input type="checkbox" [(ngModel)]="importValidRowsOnly" /> استيراد الصفوف الصحيحة فقط وعزل الصفوف الخاطئة</label> }
              <button class="btn primary" type="button" [disabled]="busy() || !result.canCommitValidRows || (result.batch.invalidRows > 0 && !importValidRowsOnly)" (click)="commit()">5. تنفيذ الاستيراد</button>
            </section>
          }

          @if (commitResult(); as committed) { <section class="result"><h2>6. تم التنفيذ</h2><p>تم إنشاء {{ committed.createdRows }} سجل وتحديث {{ committed.updatedRows }} وتجاوز {{ committed.skippedInvalidRows }} صف غير صالح.</p><button class="btn" type="button" (click)="downloadOriginal(committed.batch)">تنزيل الملف الأصلي</button><button class="btn danger" type="button" (click)="reverse(committed.batch)">إنشاء قيد عكسي والتراجع</button></section> }
        </article>

        <aside class="card"><h2>سجل عمليات الاستيراد</h2>@for (item of batches(); track item.id) {<article class="history"><div><strong>{{ item.fileName }}</strong><small>{{ item.createdAt | date:'yyyy-MM-dd HH:mm' }} · {{ item.createdBy }}</small></div><span class="status">{{ statusLabel(item.status) }}</span><div class="history-actions"><button class="link" type="button" (click)="openBatch(item)">فتح</button><button class="link" type="button" (click)="downloadOriginal(item)">الأصل</button>@if (item.invalidRows > 0) {<button class="link" type="button" (click)="downloadErrors(item)">الأخطاء</button>}@if (item.status === 'IMPORTED') {<button class="link danger-text" type="button" (click)="reverse(item)">تراجع</button>}</div></article>} @empty {<p>لا توجد عمليات سابقة.</p>}
          <hr /><h2>التقارير المتاحة فعلياً</h2><a class="report-link" routerLink="/workforce/settlement-periods">كشوف التسوية والحضور والمستحقات والسلف والمقاولين ⇦</a><p class="muted">تم إخفاء أي تقرير لا يملك إجراءً فعلياً حتى لا تظهر أزرار وهمية.</p>
        </aside>
      </div>
    </section>
  `,
  styles: [`
    .page{padding:1.5rem;display:grid;gap:1.25rem}.eyebrow{color:#b7791f;font-weight:800}header h1{margin:.2rem 0}.steps{display:grid;grid-template-columns:repeat(6,1fr);list-style:none;padding:0;margin:0;gap:.5rem}.steps li{padding:.7rem;text-align:center;background:#eef2f7;color:#64748b;border-radius:10px}.steps li.active{background:#fff3cd;color:#815500;font-weight:800}.grid{display:grid;grid-template-columns:minmax(0,2fr) minmax(320px,1fr);gap:1rem}.card{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:1.2rem}.workflow{display:grid;gap:1rem}.workflow section{border-bottom:1px solid #edf0f4;padding-bottom:1rem}.workflow section:last-child{border:0}.btn{border:0;border-radius:8px;padding:.6rem .85rem;margin:.25rem;font-weight:800;cursor:pointer;background:#e8edf3}.btn:disabled{opacity:.5}.primary{background:#b7791f;color:#fff}.danger{background:#fee2e2;color:#991b1b}.mapping{display:grid;grid-template-columns:repeat(3,1fr);gap:.7rem}.mapping label{display:grid;gap:.3rem;font-weight:700}.mapping select,input[type=file]{padding:.6rem;border:1px solid #cbd5e1;border-radius:8px}.batch-head,.summary,.history{display:flex;justify-content:space-between;gap:.8rem;align-items:center}.status{background:#eef2f7;border-radius:999px;padding:.25rem .6rem}.summary{justify-content:flex-start}.summary span{padding:.6rem;border-radius:8px;background:#f8fafc}.summary .ok{background:#ecfdf5}.summary .bad{background:#fef2f2}.alert{padding:.75rem;border-radius:9px}.error{background:#fef2f2;color:#991b1b}.warning{background:#fff7ed;color:#9a3412}.table-wrap{overflow:auto;margin:.8rem 0}table{width:100%;border-collapse:collapse}th,td{padding:.55rem;border-bottom:1px solid #edf0f4;text-align:right}td small,.history small{display:block;color:#64748b}.invalid{background:#fef2f2}.check{display:block;padding:.6rem;background:#fff7ed}.history{display:grid;grid-template-columns:1fr auto;padding:.8rem 0;border-bottom:1px solid #edf0f4}.history-actions{grid-column:1/-1}.link{border:0;background:transparent;color:#946200;text-decoration:underline;cursor:pointer}.danger-text{color:#b91c1c}.report-link{display:block;padding:.8rem;background:#f8fafc;border-radius:9px;color:#694900}.muted{color:#64748b;font-size:.9rem}@media(max-width:950px){.grid{grid-template-columns:1fr}.steps{grid-template-columns:repeat(3,1fr)}.mapping{grid-template-columns:1fr}}
  `]
})
export class ReportsImportComponent implements OnInit {
  private readonly service = inject(WorkforceService);
  private readonly notification = inject(NotificationService);
  readonly i18n = inject(I18nService);
  readonly selectedFile = signal<File | null>(null);
  readonly batch = signal<WorkforceImportBatch | null>(null);
  readonly batches = signal<WorkforceImportBatch[]>([]);
  readonly validation = signal<WorkforceImportValidation | null>(null);
  readonly commitResult = signal<WorkforceImportCommit | null>(null);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly step = signal(1);
  mapping: Record<'workerCode' | 'workDate' | 'attendanceValue', string> = { workerCode: '', workDate: '', attendanceValue: '' };
  importValidRowsOnly = false;
  private operationId = crypto.randomUUID();

  ngOnInit(): void { this.refreshHistory(); }
  selectFile(event: Event): void { this.selectedFile.set((event.target as HTMLInputElement).files?.[0] ?? null); }
  upload(): void { const file = this.selectedFile(); if (!file) return; this.run(() => this.service.uploadImport(file), value => { this.batch.set(value); this.mapping = this.inferMapping(value.headers); this.step.set(2); this.resetResults(); }); }
  saveMapping(): void { const current = this.batch(); if (!current) return; this.run(() => this.service.saveImportMapping(current.id, this.mapping), value => { this.batch.set(value); this.step.set(3); }); }
  validate(): void { const current = this.batch(); if (!current) return; this.run(() => this.service.validateImport(current.id), value => { this.validation.set(value); this.batch.set(value.batch); this.step.set(4); this.refreshHistory(); }); }
  commit(): void { const current = this.batch(); if (!current) return; this.run(() => this.service.commitImport(current.id, this.operationId, this.importValidRowsOnly), value => { this.commitResult.set(value); this.batch.set(value.batch); this.step.set(6); this.notification.success('تم تنفيذ الاستيراد وحفظ سجل التغييرات.'); this.refreshHistory(); }); }
  reverse(item: WorkforceImportBatch): void { this.run(() => this.service.reverseImport(item.id), value => { this.batch.set(value); this.commitResult.set(null); this.notification.success('تم إنشاء قيد عكسي دون حذف الملف أو سجل العملية.'); this.refreshHistory(); }); }
  openBatch(item: WorkforceImportBatch): void { this.batch.set(item); this.mapping = { workerCode: item.columnMapping['workerCode'] ?? '', workDate: item.columnMapping['workDate'] ?? '', attendanceValue: item.columnMapping['attendanceValue'] ?? '' }; this.commitResult.set(null); this.error.set(null); if (['VALIDATED','READY','IMPORTED','REVERSED'].includes(item.status)) { this.run(() => this.service.previewImport(item.id), value => { this.validation.set(value); this.step.set(item.status === 'IMPORTED' || item.status === 'REVERSED' ? 6 : 4); }); } else { this.validation.set(null); this.step.set(item.status === 'MAPPED' ? 3 : 2); } }
  downloadErrors(item: WorkforceImportBatch): void { this.service.downloadImportErrors(item.id).subscribe({ next: blob => downloadBlob(blob, `workforce-import-errors-${item.id}.xlsx`), error: e => this.fail(e) }); }
  downloadOriginal(item: WorkforceImportBatch): void { this.service.downloadImportOriginal(item.id).subscribe({ next: blob => downloadBlob(blob, item.fileName), error: e => this.fail(e) }); }
  downloadTemplate(): void {
    const columns = [
      { key: 'workerCode', label: 'كود العامل' },
      { key: 'workDate', label: 'تاريخ العمل' },
      { key: 'attendanceValue', label: 'قيمة الحضور' }
    ];
    const sampleRows = [
      { workerCode: 'EMP-001', workDate: '2026-07-31', attendanceValue: '1' },
      { workerCode: 'EMP-002', workDate: '2026-07-31', attendanceValue: '0.5' },
      { workerCode: 'EMP-003', workDate: '2026-07-31', attendanceValue: '0' }
    ];
    exportCsv(sampleRows, columns, 'قالب_استيراد_تقارير_العمالة.csv');
    this.notification.success('تم تنزيل نموذج استيراد تقارير العمالة بنجاح.');
  }
  mappingComplete(): boolean { return Boolean(this.mapping.workerCode && this.mapping.workDate && this.mapping.attendanceValue); }
  statusLabel(status: string): string { return ({UPLOADED:'مرفوع',MAPPED:'تمت المطابقة',VALIDATED:'تم التحقق مع أخطاء',READY:'جاهز',IMPORTED:'تم الاستيراد',REVERSED:'تم التراجع'} as Record<string,string>)[status] ?? status; }
  private refreshHistory(): void { this.service.loadImportBatches().subscribe({ next: value => this.batches.set(value), error: e => this.fail(e) }); }
  private inferMapping(headers: string[]): Record<'workerCode'|'workDate'|'attendanceValue',string> { const find=(terms:string[])=>headers.find(h=>terms.some(t=>h.toLowerCase().includes(t)))??''; return {workerCode:find(['كود','worker code','code']),workDate:find(['تاريخ','date']),attendanceValue:find(['حضور','attendance','value'])}; }
  private resetResults(): void { this.validation.set(null); this.commitResult.set(null); this.operationId = crypto.randomUUID(); this.importValidRowsOnly = false; }
  private run<T>(request: () => import('rxjs').Observable<T>, done: (value: T) => void): void { this.busy.set(true); this.error.set(null); request().subscribe({ next: value => { this.busy.set(false); done(value); }, error: error => { this.busy.set(false); this.fail(error); } }); }
  private fail(error: { error?: { detail?: string }; message?: string }): void { this.error.set(apiErrorDetail(error, error?.message ?? 'تعذر تنفيذ العملية.')); }
}
