import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkerCategory } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div><span class="eyebrow">{{ i18n.t('workforce.ui.categories.eyebrow') }}</span><h1>{{ i18n.t('workforce.ui.categories.title') }}</h1></div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="exportExcel()">{{ i18n.t('workforce.ui.categories.export') }}</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">{{ i18n.t('workforce.ui.categories.add') }}</button>
        </div>
      </header>

      <div class="scope-note">
        {{ i18n.t('workforce.ui.categories.scopeNote') }}
      </div>

      <div class="card">
        <table class="data-table">
          <thead><tr>
            <th>{{ i18n.t('workforce.ui.categories.code') }}</th><th>{{ i18n.t('workforce.ui.categories.name') }}</th><th>{{ i18n.t('workforce.ui.categories.description') }}</th><th>{{ i18n.t('workforce.ui.categories.dailyRate') }}</th>
            <th>{{ i18n.t('workforce.ui.categories.hours') }}</th><th>{{ i18n.t('workforce.ui.categories.cycle') }}</th><th>{{ i18n.t('workforce.ui.categories.scope') }}</th><th>{{ i18n.t('workforce.ui.status') }}</th>
          </tr></thead>
          <tbody>
            <tr *ngFor="let cat of workforceService.categories()">
              <td><strong>{{ cat.code }}</strong></td><td>{{ cat.name }}</td><td>{{ cat.description || '—' }}</td>
              <td>{{ cat.defaultDailyRate | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</td><td>{{ cat.standardDailyHours }} {{ i18n.t('workforce.ui.hoursShort') }}</td>
              <td>{{ getCycleLabel(cat.defaultSettlementCycle) }}</td><td>{{ getScopeLabel(cat.scope) }}</td><td><span class="badge active">{{ getStatusLabel(cat.status) }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog [isOpen]="isModalOpen" [title]="i18n.t('workforce.ui.categories.addTitle')" size="normal"
        [preventOutsideClose]="true" (close)="closeModal()">
        <form (ngSubmit)="saveCategory()" class="modal-form">
          <div class="form-group">
            <label for="worker-category-code">{{ i18n.t('workforce.ui.categories.codeRequired') }}</label>
            <input id="worker-category-code" type="text" [(ngModel)]="form.code" name="code" required
              class="form-input" [disabled]="saving()" [attr.aria-invalid]="submitted() && !form.code?.trim()"
              aria-describedby="worker-category-code-error" />
            @if (submitted() && !form.code?.trim()) {
              <small id="worker-category-code-error" class="field-error">{{ i18n.t('workforce.ui.categories.codeError') }}</small>
            }
          </div>
          <div class="form-group">
            <label for="worker-category-name">{{ i18n.t('workforce.ui.categories.nameRequired') }}</label>
            <input id="worker-category-name" type="text" [(ngModel)]="form.name" name="name" required
              class="form-input" [disabled]="saving()" [attr.aria-invalid]="submitted() && !form.name?.trim()"
              aria-describedby="worker-category-name-error" />
            @if (submitted() && !form.name?.trim()) {
              <small id="worker-category-name-error" class="field-error">{{ i18n.t('workforce.ui.categories.nameError') }}</small>
            }
          </div>
          <div class="form-group"><label>{{ i18n.t('workforce.ui.categories.description') }}</label><textarea [(ngModel)]="form.description" name="description" rows="2" class="form-input" [disabled]="saving()"></textarea></div>
          <div class="form-group"><label>{{ i18n.t('workforce.ui.categories.dailyRateRequired') }}</label><input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" required class="form-input" [disabled]="saving()" /></div>
          <div class="form-group"><label>{{ i18n.t('workforce.ui.categories.hoursRequired') }}</label><input type="number" min="0.5" step="0.5" [(ngModel)]="form.standardDailyHours" name="standardDailyHours" required class="form-input" [disabled]="saving()" /></div>
          <div class="form-group">
            <label>{{ i18n.t('workforce.ui.categories.scopeRequired') }}</label>
            <select [(ngModel)]="form.scope" name="scope" class="form-input" [disabled]="saving()">
              <option value="WORKER">{{ i18n.t('workforce.ui.categories.workerOnly') }}</option>
              <option value="BOTH">{{ i18n.t('workforce.ui.categories.both') }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>{{ i18n.t('workforce.ui.categories.cycle') }} *</label>
            <select [(ngModel)]="form.defaultSettlementCycle" name="defaultSettlementCycle" class="form-input" [disabled]="saving()">
              <option value="HALF_MONTH">{{ i18n.t('workforce.ui.categories.halfMonth') }}</option>
              <option value="MONTHLY">{{ i18n.t('workforce.ui.categories.monthly') }}</option>
            </select>
          </div>
          @if (saveError()) { <div class="save-error" role="alert">{{ saveError() }}</div> }
        </form>
        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveCategory()" [disabled]="saving()">{{ saving() ? i18n.t('common.saving') : i18n.t('workforce.ui.categories.save') }}</button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()" [disabled]="saving()">{{ i18n.t('workforce.ui.cancel') }}</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container{padding: 1.5rem;display: flex;flex-direction: column;gap: 1.5rem}.eyebrow{font-size: .875rem;color: #d97706;font-weight: 600}
    .page-header{display: flex;justify-content: space-between;align-items: center}.header-actions{display: flex;gap: .75rem}.page-header h1{font-size: 1.75rem;font-weight: 800;color: var(--ink);margin: .25rem 0 0}
    .scope-note{padding: .875rem 1rem;border: 1px solid #bae6fd;border-radius: 10px;background: #f0f9ff;color: #0c4a6e}
    .card{background: var(--surface);border-radius: 12px;border: 1px solid var(--line);padding: 1.25rem;overflow-x: auto}.data-table{width: 100%;border-collapse: collapse;text-align: start}
    .data-table th,.data-table td{padding: .75rem 1rem;border-bottom: 1px solid var(--line);white-space: nowrap}.btn{padding: .625rem 1.25rem;border-radius: 8px;font-weight: 600;cursor: pointer;border: none}
    .btn:disabled{opacity: .65;cursor:not-allowed}.btn-primary{background: #d97706;color: #fff}.btn-secondary{background: var(--line);color: var(--secondary-text)}.badge{padding: .25rem .625rem;border-radius: 6px;font-size: .75rem;font-weight: 600}
    .badge.active{background: #dcfce7;color: var(--success)}.form-group{display: flex;flex-direction: column;gap: .5rem;margin-bottom: 1rem}.form-group label{font-weight: 600;font-size: .875rem;color: var(--secondary-text)}
    .form-input{padding: .625rem;border: 1px solid var(--line);border-radius: 8px;font-size: .875rem}.modal-actions-bar{width: 100%;display: flex;gap: .75rem;justify-content: flex-end}
    .save-error{margin-top: 1rem;padding: .75rem 1rem;border: 1px solid color-mix(in srgb, var(--danger) 45%, var(--line));border-radius: 8px;background: var(--danger-soft);color: var(--danger)}.field-error{color: var(--danger);font-size: .78rem}
    @media(max-width: 760px){.page-header{align-items: stretch;flex-direction: column;gap: 1rem}}
  `]
})
export class CategoriesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  notification = inject(NotificationService);
  i18n = inject(I18nService);
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly submitted = signal(false);
  isModalOpen = false;

  form: Partial<WorkerCategory> = {
    code: '', name: '', description: '', defaultDailyRate: 0, standardDailyHours: 8,
    defaultSettlementCycle: 'HALF_MONTH', status: 'ACTIVE', scope: 'WORKER'
  };

  ngOnInit() {
    this.workforceService.loadCategories().subscribe();
  }

  exportExcel(): void {
    this.workforceService.exportCategoriesExcel().subscribe(blob => downloadBlob(blob, `worker-categories-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  getCycleLabel(cycle: string): string { const keys:Record<string,string>={HALF_MONTH:'workforce.ui.categories.halfMonth',MONTHLY:'workforce.ui.categories.monthly',THIRTY_DAYS:'workforce.ui.categories.monthly',HALF_MONTHLY:'workforce.ui.categories.halfMonth'}; return keys[cycle]?this.i18n.t(keys[cycle]):cycle; }

  getStatusLabel(status: string): string { const keys:Record<string,string>={ACTIVE:'workforce.ui.active',INACTIVE:'workforce.ui.inactive',SUSPENDED:'workforce.ui.categories.statusSuspended'}; return keys[status]?this.i18n.t(keys[status]):status; }

  getScopeLabel(scope?: string): string { const keys:Record<string,string>={WORKER:'workforce.ui.categories.scopeWorker',EMPLOYEE:'workforce.ui.categories.scopeEmployee',BOTH:'workforce.ui.categories.scopeBoth'}; return scope?(keys[scope]?this.i18n.t(keys[scope]):scope):'—'; }

  openCreateModal() {
    this.saveError.set(null);
    this.submitted.set(false);
    this.form = {
      code: 'CAT-' + Math.floor(10 + Math.random() * 90),
      name: '', description: '', defaultDailyRate: 0, standardDailyHours: 8,
      defaultSettlementCycle: 'HALF_MONTH', status: 'ACTIVE', scope: 'WORKER'
    };
    this.isModalOpen = true;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
    this.submitted.set(false);
  }

  async saveCategory(): Promise<void> {
    if (this.saving()) return;
    this.submitted.set(true);
    if (!this.form.code?.trim() || !this.form.name?.trim()) {
      this.saveError.set(this.i18n.t('workforce.ui.categories.requiredError')); 
      return;
    }
    if ((this.form.defaultDailyRate ?? 0) < 0 || (this.form.standardDailyHours ?? 0) <= 0) {
      this.saveError.set(this.i18n.t('workforce.ui.categories.rateHoursError')); 
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = await firstValueFrom(this.workforceService.createCategory(this.form));
      const refreshed = await firstValueFrom(this.workforceService.loadCategories());
      if (!saved?.id || !refreshed.some(category => category.id === saved.id)) {
        throw new Error(this.i18n.t('workforce.ui.categories.verifyFailed')); 
      }
      this.isModalOpen = false;
      this.submitted.set(false);
      this.notification.success(this.i18n.t('workforce.categoryCreatedSuccess'));
    } catch (error: any) {
      this.saveError.set(error?.error?.message ?? error?.error?.detail ?? error?.message ?? this.i18n.t('workforce.ui.categories.saveFailed'));
    } finally {
      this.saving.set(false);
    }
  }
}
