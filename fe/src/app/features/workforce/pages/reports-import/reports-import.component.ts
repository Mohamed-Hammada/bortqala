import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceImportBatch, WorkforceImportCommit, WorkforceImportValidation } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { downloadBlob } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';
import { SampleTemplateService } from '../../../../core/sample-template.service';

@Component({
  selector: 'app-reports-import',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page">
      <header><span class="eyebrow">{{ i18n.t('reportsImport.ui.eyebrow') }}</span><h1>{{ i18n.t('reportsImport.ui.title') }}</h1><p>{{ i18n.t('reportsImport.ui.description') }}</p></header>
      <ol class="steps"><li [class.active]="step() >= 1">{{ i18n.t('reportsImport.ui.step1') }}</li><li [class.active]="step() >= 2">{{ i18n.t('reportsImport.ui.step2') }}</li><li [class.active]="step() >= 3">{{ i18n.t('reportsImport.ui.step3') }}</li><li [class.active]="step() >= 4">{{ i18n.t('reportsImport.ui.step4') }}</li><li [class.active]="step() >= 5">{{ i18n.t('reportsImport.ui.step5') }}</li><li [class.active]="step() >= 6">{{ i18n.t('reportsImport.ui.step6') }}</li></ol>
      @if (error()) { <div class="alert error">{{ error() }}</div> }

      <div class="grid">
        <article class="card workflow">
          <section>
            <h2>{{ i18n.t('reportsImport.ui.uploadTitle') }}</h2>
            <input #fileInput class="native-file-input" type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,.csv" (change)="selectFile($event)" />
            <button class="btn" type="button" (click)="fileInput.click()">{{ i18n.t('reportsImport.ui.chooseFile') }}</button>
            <span class="selected-file-name">{{ selectedFile()?.name || i18n.t('reportsImport.ui.noFileChosen') }}</span>
            <button class="btn primary" type="button" [disabled]="!selectedFile() || busy()" (click)="upload()">{{ busy() ? i18n.t('reportsImport.ui.uploading') : i18n.t('reportsImport.ui.uploadOriginal') }}</button>
            <button class="btn" type="button" (click)="downloadTemplate()">{{ i18n.t('reportsImport.downloadTemplate') }}</button>
            <button class="btn" type="button" (click)="downloadWorkersTemplate()">{{ i18n.t('common.downloadTemplate') }}</button>
          </section>

          @if (batch(); as current) {
            <section><div class="batch-head"><h2>{{ i18n.t('reportsImport.ui.mappingTitle') }}</h2><span class="status">{{ statusLabel(current.status) }}</span></div>
              <p>{{ current.fileName }} · {{ i18n.t('reportsImport.ui.checksum') }} <code>{{ current.checksum.slice(0, 12) }}</code></p>
              <div class="mapping"><label>{{ i18n.t('reportsImport.ui.workerCode') }}<select [(ngModel)]="mapping.workerCode">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label><label>{{ i18n.t('reportsImport.ui.workDate') }}<select [(ngModel)]="mapping.workDate">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label><label>{{ i18n.t('reportsImport.ui.attendanceValue') }}<select [(ngModel)]="mapping.attendanceValue">@for (header of current.headers; track header) {<option [value]="header">{{ header }}</option>}</select></label></div>
              <button class="btn" type="button" [disabled]="busy() || !mappingComplete()" (click)="saveMapping()">{{ i18n.t('reportsImport.ui.saveMapping') }}</button>
              <button class="btn primary" type="button" [disabled]="busy() || current.status !== 'MAPPED'" (click)="validate()">{{ i18n.t('reportsImport.ui.validate') }}</button>
            </section>
          }

          @if (validation(); as result) {
            <section><h2>{{ i18n.t('reportsImport.ui.previewTitle') }}</h2><div class="summary"><span>{{ i18n.t('reportsImport.ui.total') }} <b>{{ result.batch.totalRows }}</b></span><span class="ok">{{ i18n.t('reportsImport.ui.valid') }} <b>{{ result.batch.validRows }}</b></span><span class="bad">{{ i18n.t('reportsImport.ui.errors') }} <b>{{ result.batch.invalidRows }}</b></span></div>
              @if (result.batch.invalidRows > 0) { <div class="alert warning">{{ i18n.t('reportsImport.ui.errorPolicy') }}<button class="link" type="button" (click)="downloadErrors(result.batch)">{{ i18n.t('reportsImport.ui.downloadErrors') }}</button></div> }
              <div class="table-wrap"><table><thead><tr><th>{{ i18n.t('reportsImport.ui.row') }}</th><th>{{ i18n.t('reportsImport.ui.worker') }}</th><th>{{ i18n.t('reportsImport.ui.date') }}</th><th>{{ i18n.t('reportsImport.ui.attendance') }}</th><th>{{ i18n.t('reportsImport.ui.statusReason') }}</th></tr></thead><tbody>@for (row of result.preview; track row.rowNumber) {<tr [class.invalid]="row.validationStatus === 'INVALID'"><td>{{ row.rowNumber }}</td><td>{{ row.workerCode }}<small>{{ row.workerName }}</small></td><td>{{ row.workDate || '—' }}</td><td>{{ row.attendanceValue ?? '—' }}</td><td>{{ row.validationStatus === 'VALID' ? i18n.t('reportsImport.ui.valid') : row.errorMessage }}</td></tr>}</tbody></table></div>
              @if (result.batch.invalidRows > 0) { <label class="check"><input type="checkbox" [(ngModel)]="importValidRowsOnly" /> {{ i18n.t('reportsImport.ui.validOnly') }}</label> }
              <button class="btn primary" type="button" [disabled]="busy() || !result.canCommitValidRows || (result.batch.invalidRows > 0 && !importValidRowsOnly)" (click)="commit()">{{ i18n.t('reportsImport.ui.commit') }}</button>
            </section>
          }

          @if (commitResult(); as committed) { <section class="result"><h2>{{ i18n.t('reportsImport.ui.completed') }}</h2><p>{{ i18n.t('reportsImport.ui.commitSummary', { created: committed.createdRows, updated: committed.updatedRows, skipped: committed.skippedInvalidRows }) }}</p><button class="btn" type="button" (click)="downloadOriginal(committed.batch)">{{ i18n.t('reportsImport.ui.downloadOriginal') }}</button><button class="btn danger" type="button" (click)="reverse(committed.batch)">{{ i18n.t('reportsImport.ui.reverse') }}</button></section> }
        </article>

        <aside class="card"><h2>{{ i18n.t('reportsImport.ui.history') }}</h2>@for (item of batches(); track item.id) {<article class="history"><div><strong>{{ item.fileName }}</strong><small>{{ item.createdAt | date:'yyyy-MM-dd HH:mm' }} · {{ item.createdBy }}</small></div><span class="status">{{ statusLabel(item.status) }}</span><div class="history-actions"><button class="link" type="button" (click)="openBatch(item)">{{ i18n.t('reportsImport.ui.open') }}</button><button class="link" type="button" (click)="downloadOriginal(item)">{{ i18n.t('reportsImport.ui.original') }}</button>@if (item.invalidRows > 0) {<button class="link" type="button" (click)="downloadErrors(item)">{{ i18n.t('reportsImport.ui.errors') }}</button>}@if (item.status === 'IMPORTED') {<button class="link danger-text" type="button" (click)="reverse(item)">{{ i18n.t('reportsImport.ui.undo') }}</button>}</div></article>} @empty {<p>{{ i18n.t('reportsImport.ui.noHistory') }}</p>}
          <hr /><h2>{{ i18n.t('reportsImport.ui.availableReports') }}</h2><a class="report-link" routerLink="/workforce/settlement-periods">{{ i18n.t('reportsImport.ui.reportsLink') }}</a><p class="muted">{{ i18n.t('reportsImport.ui.realReportsOnly') }}</p>
        </aside>
      </div>
    </section>
  `,
  styles: [`
    .page{padding: 1.5rem;display: grid;gap: 1.25rem}.eyebrow{color: #b7791f;font-weight: 800}header h1{margin: .2rem 0}.steps{display: grid;grid-template-columns: repeat(6,1fr);list-style: none;padding: 0;margin: 0;gap: .5rem}.steps li{padding: .7rem;text-align: center;background: var(--surface-muted);color: var(--muted);border-radius: 10px}.steps li.active{background: var(--warning-soft);color: var(--warning-text);font-weight: 800}.grid{display: grid;grid-template-columns: minmax(0,2fr) minmax(320px,1fr);gap: 1rem}.card{background: var(--surface);border: 1px solid var(--line);border-radius: 14px;padding: 1.2rem}.workflow{display: grid;gap: 1rem}.workflow section{border-bottom: 1px solid var(--line);padding-bottom: 1rem}.workflow section:last-child{border: 0}.btn{border: 0;border-radius: 8px;padding: .6rem .85rem;margin: .25rem;font-weight: 800;cursor: pointer;background: var(--surface-muted)}.btn:disabled{opacity: .5}.primary{background: #b7791f;color: #fff}.danger{background: var(--danger-soft);color: var(--danger)}.mapping{display: grid;grid-template-columns: repeat(3,1fr);gap: .7rem}.mapping label{display: grid;gap: .3rem;font-weight: 700}.mapping select{padding: .6rem;border: 1px solid var(--line);border-radius: 8px}.native-file-input{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap}.selected-file-name{color:var(--muted);font-size:.9rem}.batch-head,.summary,.history{display: flex;justify-content: space-between;gap: .8rem;align-items: center}.status{background: var(--surface-muted);border-radius: 999px;padding: .25rem .6rem}.summary{justify-content: flex-start}.summary span{padding: .6rem;border-radius: 8px;background: var(--surface-muted)}.summary .ok{background: var(--success-soft)}.summary .bad{background: var(--danger-soft)}.alert{padding: .75rem;border-radius: 9px}.error{background: var(--danger-soft);color: var(--danger)}.warning{background: var(--warning-soft);color: var(--warning-text)}.table-wrap{overflow: auto;margin: .8rem 0}table{width: 100%;border-collapse: collapse}th,td{padding: .55rem;border-bottom: 1px solid var(--line);text-align: start}td small,.history small{display: block;color: var(--muted)}.invalid{background: var(--danger-soft)}.check{display: block;padding: .6rem;background: var(--warning-soft)}.history{display: grid;grid-template-columns: 1fr auto;padding: .8rem 0;border-bottom: 1px solid var(--line)}.history-actions{grid-column: 1/-1}.link{border: 0;background: transparent;color: #946200;text-decoration: underline;cursor: pointer}.danger-text{color: var(--danger)}.report-link{display: block;padding: .8rem;background: var(--surface-muted);border-radius: 9px;color: var(--warning-text)}.muted{color: var(--muted);font-size: .9rem}@media(max-width: 950px){.grid{grid-template-columns: 1fr}.steps{grid-template-columns: repeat(3,1fr)}.mapping{grid-template-columns: 1fr}}
  `]
})
export class ReportsImportComponent implements OnInit {
  private readonly service = inject(WorkforceService);
  private readonly notification = inject(NotificationService);
  private readonly sampleTemplates = inject(SampleTemplateService);
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
  commit(): void { const current = this.batch(); if (!current) return; this.run(() => this.service.commitImport(current.id, this.operationId, this.importValidRowsOnly), value => { this.commitResult.set(value); this.batch.set(value.batch); this.step.set(6); this.notification.success(this.i18n.t('reportsImport.commitSuccess')); this.refreshHistory(); }); }
  reverse(item: WorkforceImportBatch): void { this.run(() => this.service.reverseImport(item.id), value => { this.batch.set(value); this.commitResult.set(null); this.notification.success(this.i18n.t('reportsImport.reverseSuccess')); this.refreshHistory(); }); }
  openBatch(item: WorkforceImportBatch): void { this.batch.set(item); this.mapping = { workerCode: item.columnMapping['workerCode'] ?? '', workDate: item.columnMapping['workDate'] ?? '', attendanceValue: item.columnMapping['attendanceValue'] ?? '' }; this.commitResult.set(null); this.error.set(null); if (['VALIDATED','READY','IMPORTED','REVERSED'].includes(item.status)) { this.run(() => this.service.previewImport(item.id), value => { this.validation.set(value); this.step.set(item.status === 'IMPORTED' || item.status === 'REVERSED' ? 6 : 4); }); } else { this.validation.set(null); this.step.set(item.status === 'MAPPED' ? 3 : 2); } }
  downloadErrors(item: WorkforceImportBatch): void { this.service.downloadImportErrors(item.id).subscribe({ next: blob => downloadBlob(blob, `workforce-import-errors-${item.id}.xlsx`), error: e => this.fail(e) }); }
  downloadOriginal(item: WorkforceImportBatch): void { this.service.downloadImportOriginal(item.id).subscribe({ next: blob => downloadBlob(blob, item.fileName), error: e => this.fail(e) }); }
  downloadTemplate(): void {
    void this.sampleTemplates.workforceAttendance()
      .then(() => this.notification.success(this.i18n.t('reportsImport.templateDownloadSuccess')))
      .catch(e => this.fail(e));
  }
  downloadWorkersTemplate(): void {
    void this.sampleTemplates.workforceWorkers()
      .then(() => this.notification.success(this.i18n.t('reportsImport.templateDownloadSuccess')))
      .catch(e => this.fail(e));
  }
  mappingComplete(): boolean { return Boolean(this.mapping.workerCode && this.mapping.workDate && this.mapping.attendanceValue); }
  statusLabel(status: string): string { const keys: Record<string,string>={UPLOADED:'reportsImport.ui.status.uploaded',MAPPED:'reportsImport.ui.status.mapped',VALIDATED:'reportsImport.ui.status.validated',READY:'reportsImport.ui.status.ready',IMPORTED:'reportsImport.ui.status.imported',REVERSED:'reportsImport.ui.status.reversed'}; return keys[status] ? this.i18n.t(keys[status]) : status; }
  private refreshHistory(): void { this.service.loadImportBatches().subscribe({ next: value => this.batches.set(value), error: e => this.fail(e) }); }
  private inferMapping(headers: string[]): Record<'workerCode'|'workDate'|'attendanceValue',string> { const find=(terms:string[])=>headers.find(h=>terms.some(t=>h.toLowerCase().includes(t)))??''; return {workerCode:find(['كود','worker code','code']),workDate:find(['تاريخ','date']),attendanceValue:find(['حضور','attendance','value'])}; }
  private resetResults(): void { this.validation.set(null); this.commitResult.set(null); this.operationId = crypto.randomUUID(); this.importValidRowsOnly = false; }
  private run<T>(request: () => import('rxjs').Observable<T>, done: (value: T) => void): void { this.busy.set(true); this.error.set(null); request().subscribe({ next: value => { this.busy.set(false); done(value); }, error: error => { this.busy.set(false); this.fail(error); } }); }
  private fail(error: { error?: { detail?: string }; message?: string }): void { this.error.set(apiErrorDetail(error, error?.message ?? this.i18n.t('reportsImport.ui.operationFailed'))); }
}
