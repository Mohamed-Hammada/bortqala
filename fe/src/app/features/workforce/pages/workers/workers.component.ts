import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Worker } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';
import { WorkforceExcelImportButtonComponent } from '../../ui/workforce-excel-import-button.component';

@Component({
  selector: 'app-workers',
  standalone: true,
  imports: [WorkforceExcelImportButtonComponent, CommonModule, FormsModule, RouterLink, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div><span class="eyebrow">{{ i18n.t('workforce.ui.workers.eyebrow') }}</span><h1>{{ i18n.t('workforce.ui.workers.title') }}</h1></div>
        <div class="header-actions">
        <app-workforce-excel-import-button kind="workers" />
          <button type="button" class="btn btn-secondary" (click)="exportExcel()">{{ i18n.t('workforce.ui.exportExcel') }}</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">{{ i18n.t('workforce.ui.workers.add') }}</button>
        </div>
      </header>

      <div class="card">
        <table class="data-table">
          <thead><tr>
            <th>{{ i18n.t('reportsImport.ui.workerCode') }}</th><th>{{ i18n.t('workforce.ui.contractors.fullName') }}</th><th>{{ i18n.t('workforce.ui.workers.contractor') }}</th><th>{{ i18n.t('workforce.ui.workers.category') }}</th>
            <th>{{ i18n.t('workforce.ui.categories.dailyRate') }}</th><th>{{ i18n.t('workforce.ui.workers.hours') }}</th><th>{{ i18n.t('workforce.ui.workers.attendanceMode') }}</th><th>{{ i18n.t('workforce.ui.status') }}</th><th>{{ i18n.t('workforce.ui.actions') }}</th>
          </tr></thead>
          <tbody>
            <tr *ngFor="let w of workforceService.workers()">
              <td><strong>{{ w.code }}</strong></td><td>{{ w.fullName }}</td><td>{{ w.contractorName }}</td>
              <td><span class="badge category-badge">{{ w.categoryName }}</span></td>
              <td>{{ w.defaultDailyRate | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</td><td>{{ w.standardDailyHours }} {{ i18n.t('workforce.ui.hoursShort') }}</td>
              <td>{{ w.attendanceMode === 'MANUAL' ? i18n.t('workforce.ui.advances.manual') : i18n.t('workforce.ui.workers.biometric') }}</td>
              <td><span class="badge active">{{ workerStatusLabel(w.status) }}</span></td>
              <td><button type="button" class="btn btn-sm" (click)="openEditModal(w)">{{ i18n.t('workforce.ui.edit') }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog [isOpen]="isModalOpen" [title]="editingWorker ? i18n.t('workforce.ui.workers.editTitle') : i18n.t('workforce.ui.workers.addTitle')"
        size="wide" [preventOutsideClose]="true" (close)="closeModal()">
        <form (ngSubmit)="saveWorker()" class="modal-form">
          <div class="form-grid">
            <div class="form-group"><label>{{ i18n.t('workforce.ui.workers.codeRequired') }}</label><input type="text" [(ngModel)]="form.code" name="code" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.workers.nameRequired') }}</label><input type="text" [(ngModel)]="form.fullName" name="fullName" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.workers.contractorRequired') }}</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" required class="form-input" [disabled]="saving()">
                <option value="" disabled>{{ i18n.t('workforce.ui.workers.selectContractor') }}</option>
                <option *ngFor="let c of workforceService.contractors()" [value]="c.id">{{ c.name }} ({{ c.code }})</option>
              </select>
            </div>
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.workers.categoryRequired') }}</label>
              <select
                [(ngModel)]="form.categoryId"
                name="categoryId"
                required
                class="form-input"
                [disabled]="saving() || categoriesLoading()"
                (ngModelChange)="onCategoryChange($event)">
                @if (categoriesLoading()) {
                  <option value="" disabled>{{ i18n.t('workforce.ui.workers.loadingCategories') }}</option>
                } @else if (categoriesLoadError()) {
                  <option value="" disabled>{{ i18n.t('workforce.ui.workers.categoriesFailed') }}</option>
                } @else if (workforceService.categories().length === 0) {
                  <option value="" disabled>{{ i18n.t('workforce.ui.workers.noCategories') }}</option>
                } @else {
                  <option value="" disabled>{{ i18n.t('workforce.ui.workers.selectCategory') }}</option>
                  <option *ngFor="let cat of workforceService.categories()" [value]="cat.id">{{ cat.name }} — {{ cat.defaultDailyRate | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</option>
                }
              </select>

              @if (categoriesLoadError()) {
                <div class="category-helper category-helper-error">
                  <span>{{ categoriesLoadError() }}</span>
                  <button type="button" class="inline-action-button" (click)="loadWorkerCategories()" [disabled]="categoriesLoading()">
                    {{ i18n.t('workforce.ui.retry') }}
                  </button>
                </div>
              } @else if (!categoriesLoading() && workforceService.categories().length === 0) {
                <div class="category-helper category-helper-warning">
                  <span>{{ i18n.t('workforce.ui.workers.noCategoriesHelp') }}</span>
                  <a routerLink="/workforce/categories" class="inline-action-link">{{ i18n.t('workforce.ui.workers.createCategory') }}</a>
                </div>
              } @else {
                <small>{{ i18n.t('workforce.ui.workers.categoryInheritance') }}</small>
              }
            </div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.categories.dailyRate') }} ({{ i18n.t('workforce.ui.currencyEgp') }}) *</label><input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.workers.standardHoursRequired') }}</label><input type="number" min="0" step="0.5" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.workers.phone') }}</label><input type="text" [(ngModel)]="form.phone" name="phone" class="form-input" [disabled]="saving()" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.workers.nationalId') }}</label><input type="text" [(ngModel)]="form.nationalId" name="nationalId" class="form-input" [disabled]="saving()" /></div>
          </div>
          @if (saveError()) { <div class="save-error" role="alert">{{ saveError() }}</div> }
        </form>
        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveWorker()" [disabled]="saving()">{{ saving() ? i18n.t('common.saving') : i18n.t('workforce.ui.saveData') }}</button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()" [disabled]="saving()">{{ i18n.t('workforce.ui.cancel') }}</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .header-actions { display: flex; gap: .75rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: var(--ink); margin: .25rem 0 0; }
    .card { background: var(--surface); border-radius: 12px; border: 1px solid var(--line); padding: 1.25rem; overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: start; }
    .data-table th,.data-table td { padding: .75rem 1rem; border-bottom: 1px solid var(--line); white-space: nowrap; }
    .btn { padding: .625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { opacity: .65; cursor:not-allowed; }
    .btn-primary { background: #d97706; color: #fff; }.btn-secondary { background: var(--line);color: var(--secondary-text); }.btn-sm { padding: .375rem .75rem;font-size: .875rem; }
    .badge { padding: .25rem .625rem;border-radius: 6px;font-size: .75rem;font-weight: 600; }.badge.active { background: #dcfce7;color: var(--success); }.badge.category-badge { background: #e0f2fe;color: #0369a1; }
    .form-grid { display: grid;grid-template-columns: repeat(2,minmax(0,1fr));gap: 1rem; }.form-group { display: flex;flex-direction: column;gap: .5rem; }
    .form-group label { font-weight: 600;font-size: .875rem;color: var(--secondary-text); }.form-group small { color: var(--muted); }
    .form-input { padding: .625rem;border: 1px solid var(--line);border-radius: 8px;font-size: .875rem; }.modal-actions-bar { width: 100%;display: flex;gap: .75rem;justify-content: flex-end; }
    .category-helper { display: flex;align-items: center;justify-content: space-between;gap: .75rem;padding: .625rem .75rem;border: 1px solid var(--line);border-radius: 8px;font-size: .8125rem; }
    .category-helper-warning { background: color-mix(in srgb, #f59e0b 10%, var(--surface)); }
    .category-helper-error { background: var(--danger-soft);color: var(--danger);border-color: color-mix(in srgb, var(--danger) 45%, var(--line)); }
    .inline-action-link,.inline-action-button { font: inherit;font-weight: 700;white-space: nowrap;text-decoration: none;color: #b45309;background: transparent;border: 0;padding: 0;cursor: pointer; }
    .inline-action-button:disabled { opacity: .65;cursor: not-allowed; }
    .save-error { margin-top: 1rem;padding: .75rem 1rem;border: 1px solid color-mix(in srgb, var(--danger) 45%, var(--line));border-radius: 8px;background: var(--danger-soft);color: var(--danger); }
    @media (max-width: 760px){.page-header{align-items: stretch;flex-direction: column;gap: 1rem}.form-grid{grid-template-columns: 1fr}.category-helper{align-items:flex-start;flex-direction:column}}
  `]
})
export class WorkersComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);
  notification = inject(NotificationService);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly categoriesLoading = signal(true);
  readonly categoriesLoadError = signal<string | null>(null);

  workerStatusLabel(s: string): string {
    return s === 'ACTIVE' ? this.i18n.t('common.active') : this.i18n.t('common.inactive');
  }

  isModalOpen = false;
  editingWorker: Worker | null = null;
  form: Partial<Worker> = {
    code: '', fullName: '', contractorId: '', categoryId: '',
    defaultDailyRate: 0, standardDailyHours: 8, attendanceMode: 'MANUAL', status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
    this.loadWorkerCategories();
  }

  loadWorkerCategories(): void {
    this.categoriesLoading.set(true);
    this.categoriesLoadError.set(null);

    this.workforceService.loadCategories().subscribe({
      next: () => this.categoriesLoading.set(false),
      error: (error: any) => {
        this.categoriesLoading.set(false);
        this.categoriesLoadError.set(
          error?.error?.message ??
          error?.error?.detail ??
          this.i18n.t('workforce.ui.workers.categoriesLoadFailedDetail')
        );
      }
    });
  }

  exportExcel(): void {
    this.workforceService.exportWorkersExcel().subscribe(blob => downloadBlob(blob, `workers-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  openCreateModal() {
    this.editingWorker = null;
    this.saveError.set(null);
    const cats = this.workforceService.categories();
    const ctrs = this.workforceService.contractors();
    const firstCategory = cats[0];
    this.form = {
      code: 'WRK-' + Math.floor(1000 + Math.random() * 9000),
      fullName: '',
      contractorId: ctrs[0]?.id ?? '',
      categoryId: firstCategory?.id ?? '',
      defaultDailyRate: firstCategory?.defaultDailyRate ?? 0,
      standardDailyHours: firstCategory?.standardDailyHours ?? 8,
      attendanceMode: 'MANUAL',
      status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  openEditModal(w: Worker) {
    this.editingWorker = w;
    this.saveError.set(null);
    this.form = { ...w };
    this.isModalOpen = true;
  }

  onCategoryChange(categoryId: string): void {
    if (this.editingWorker) return;
    const category = this.workforceService.categories().find(item => item.id === categoryId);
    if (!category) return;
    this.form.defaultDailyRate = category.defaultDailyRate;
    this.form.standardDailyHours = category.standardDailyHours;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
  }

  async saveWorker(): Promise<void> {
    if (this.saving()) return;
    if (!this.form.code?.trim() || !this.form.fullName?.trim() || !this.form.contractorId || !this.form.categoryId) {
      this.saveError.set(this.i18n.t('workforce.ui.workers.requiredError')); 
      return;
    }
    if ((this.form.defaultDailyRate ?? 0) < 0 || (this.form.standardDailyHours ?? 0) <= 0) {
      this.saveError.set(this.i18n.t('workforce.ui.workers.rateHoursError')); 
      return;
    }

    const wasEditing = !!this.editingWorker;
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = this.editingWorker
        ? await firstValueFrom(this.workforceService.updateWorker(this.editingWorker.id, this.form))
        : await firstValueFrom(this.workforceService.createWorker(this.form));
      const refreshed = await firstValueFrom(this.workforceService.loadWorkers());
      if (!saved?.id || !refreshed.some(worker => worker.id === saved.id)) {
        throw new Error(this.i18n.t('workforce.ui.workers.verifyFailed')); 
      }
      this.isModalOpen = false;
      this.notification.success(this.i18n.t(wasEditing ? 'workforce.ui.workers.updatedSuccess' : 'workforce.ui.workers.createdSuccess')); 
    } catch (error: any) {
      this.saveError.set(error?.error?.message ?? error?.error?.detail ?? error?.message ?? this.i18n.t('workforce.ui.workers.saveFailed'));
    } finally {
      this.saving.set(false);
    }
  }
}

// BORTQALA_FEEDBACK_20260816_EXCEL_WORKERS
