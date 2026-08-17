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
  templateUrl: './reports-import.component.html',
  styleUrls: ['./reports-import.component.scss']
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
