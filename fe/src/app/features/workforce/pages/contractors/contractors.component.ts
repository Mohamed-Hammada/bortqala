import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';
import { Contractor } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { downloadBlob } from '../../../../core/download';
import { NotificationService } from '../../../../core/notification.service';
import { WorkforceExcelImportButtonComponent } from '../../ui/workforce-excel-import-button.component';

@Component({
  selector: 'app-contractors',
  standalone: true,
  imports: [WorkforceExcelImportButtonComponent, CommonModule, FormsModule, ModalDialogComponent],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">{{ i18n.t('workforce.ui.contractors.eyebrow') }}</span>
          <h1>{{ i18n.t('workforce.ui.contractors.title') }}</h1>
        </div>
        
      
      <!-- BORTQALA_WORKFORCE_UI_20260816_V2: unified compact header actions -->
      <div class="header-actions">
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">{{ i18n.t('workforce.ui.contractors.add') }}</button>
        <button type="button" class="btn btn-secondary" (click)="exportExcel()">{{ i18n.t('workforce.ui.exportExcel') }}</button>
        <app-workforce-excel-import-button kind="contractors" />
      </div>
</header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ i18n.t('workforce.ui.contractors.code') }}</th><th>{{ i18n.t('workforce.ui.contractors.fullName') }}</th><th>{{ i18n.t('workforce.ui.contractors.tradeName') }}</th><th>{{ i18n.t('workforce.ui.contractors.phone') }}</th>
              <th>{{ i18n.t('workforce.ui.contractors.accountingModel') }}</th><th>{{ i18n.t('workforce.ui.contractors.paymentRouting') }}</th><th>{{ i18n.t('workforce.ui.contractors.dealingStatus') }}</th><th>{{ i18n.t('workforce.ui.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of workforceService.contractors()">
              <td><strong>{{ c.code }}</strong></td>
              <td>{{ c.name }}</td>
              <td>{{ c.tradeName || '—' }}</td>
              <td>{{ c.phone }}</td>
              <td><span class="badge model-badge">{{ getModelLabel(c.accountingModel) }}</span></td>
              <td>{{ getRoutingLabel(c.paymentRouting) }}</td>
              <td><span class="badge active">{{ contractorStatusLabel(c.status) }}</span></td>
              <td><button type="button" class="btn btn-sm" (click)="openEditModal(c)">{{ i18n.t('workforce.ui.edit') }}</button></td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-modal-dialog
        [isOpen]="isModalOpen"
        [title]="editingContractor ? i18n.t('workforce.ui.contractors.editTitle') : i18n.t('workforce.ui.contractors.addTitle')"
        size="wide"
        [preventOutsideClose]="true"
        (close)="closeModal()">

        <form (ngSubmit)="saveContractor()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.codeRequired') }}</label>
              <input type="text" [(ngModel)]="form.code" name="code" required class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.nameRequired') }}</label>
              <input type="text" [(ngModel)]="form.name" name="name" required class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.tradeName') }}</label>
              <input type="text" [(ngModel)]="form.tradeName" name="tradeName" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.phoneRequired') }}</label>
              <input type="text" [(ngModel)]="form.phone" name="phone" required class="form-input" [disabled]="saving()" />
            </div>

            <div class="form-group col-span-2">
              <label>{{ i18n.t('workforce.ui.contractors.modelRequired') }}</label>
              <div class="model-cards">
                <label class="model-card" [class.selected]="form.accountingModel === 'worker_net_total'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_net_total" [disabled]="saving()" />
                  <strong>{{ i18n.t('workforce.ui.contractorAccounts.workerNetTotal') }}</strong>
                  <p>{{ i18n.t('workforce.ui.contractors.workerNetDescription') }}</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'contractor_daily_rate'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="contractor_daily_rate" [disabled]="saving()" />
                  <strong>{{ i18n.t('workforce.ui.model.contractorDailyRate') }}</strong>
                  <p>{{ i18n.t('workforce.ui.contractors.dailyRateDescription') }}</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'worker_cost_plus_fee'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="worker_cost_plus_fee" [disabled]="saving()" />
                  <strong>{{ i18n.t('workforce.ui.model.workerCostPlusFee') }}</strong>
                  <p>{{ i18n.t('workforce.ui.contractors.costPlusDescription') }}</p>
                </label>
                <label class="model-card" [class.selected]="form.accountingModel === 'fixed_period_amount'">
                  <input type="radio" [(ngModel)]="form.accountingModel" name="accountingModel" value="fixed_period_amount" [disabled]="saving()" />
                  <strong>{{ i18n.t('workforce.ui.model.fixedPeriodAmount') }}</strong>
                  <p>{{ i18n.t('workforce.ui.contractors.fixedDescription') }}</p>
                </label>
              </div>
            </div>

            <div class="form-group" *ngIf="form.accountingModel === 'contractor_daily_rate'">
              <label>{{ i18n.t('workforce.ui.contractors.defaultRate') }}</label>
              <input type="number" min="0" [(ngModel)]="form.defaultDailyRate" name="defaultDailyRate" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group" *ngIf="form.accountingModel === 'worker_cost_plus_fee'">
              <label>{{ i18n.t('workforce.ui.contractors.feeValue') }}</label>
              <input type="number" min="0" [(ngModel)]="form.feeValue" name="feeValue" class="form-input" [disabled]="saving()" />
            </div>
            <div class="form-group" *ngIf="form.accountingModel === 'fixed_period_amount'">
              <label>{{ i18n.t('workforce.ui.contractors.fixedAmount') }}</label>
              <input type="number" min="0" [(ngModel)]="form.fixedPeriodAmount" name="fixedPeriodAmount" class="form-input" [disabled]="saving()" />
            </div>

            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.cycle') }}</label>
              <select [(ngModel)]="form.settlementCycleDays" name="settlementCycleDays" class="form-input" [disabled]="saving()">
                <option [ngValue]="15">{{ i18n.t('workforce.ui.contractors.cycle15') }}</option>
                <option [ngValue]="30">{{ i18n.t('workforce.ui.contractors.cycle30') }}</option>
              </select>
            </div>

            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.contractors.paymentRouting') }}</label>
              <select [(ngModel)]="form.paymentRouting" name="paymentRouting" class="form-input" [disabled]="saving()">
                <option value="contractor_full">{{ i18n.t('workforce.ui.contractors.routingFull') }}</option>
                <option value="worker_direct">{{ i18n.t('workforce.ui.contractors.routingDirect') }}</option>
                <option value="mixed">{{ i18n.t('workforce.ui.contractors.routingMixed') }}</option>
              </select>
            </div>
          </div>

          @if (saveError()) {
            <div class="save-error" role="alert">
              <strong>{{ i18n.t('workforce.ui.contractors.saveErrorTitle') }}</strong>
              <span>{{ saveError() }}</span>
              <span>{{ i18n.t('workforce.ui.contractors.saveErrorHelp') }}</span>
            </div>
          }
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" (click)="saveContractor()" [disabled]="saving()">
            {{ saving() ? i18n.t('workforce.ui.contractors.saving') : i18n.t('workforce.ui.saveData') }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()" [disabled]="saving()">{{ i18n.t('workforce.ui.cancel') }}</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .header-actions { display: flex; gap: .75rem; align-items: center; }
    .btn-group { display: flex; gap: 0; }
    .btn-group .btn { border-radius: 0; }
    .btn-group .btn:first-child { border-radius: 8px 0 0 8px; }
    .btn-group .btn:last-child { border-radius: 0 8px 8px 0; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: var(--ink); margin: 0.25rem 0 0 0; }
    .card { background: var(--surface); border-radius: 12px; border: 1px solid var(--line); padding: 1.25rem; overflow-x: auto; }
    .data-table { width: 100%; border-collapse: collapse; text-align: start; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid var(--line); white-space: nowrap; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { cursor:not-allowed; opacity: .65; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: var(--line); color: var(--secondary-text); }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.active { background: #dcfce7; color: var(--success); }
    .badge.model-badge { background: #fef3c7; color: #92400e; }
    .form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.5rem; }
    .form-group label { font-weight: 600; font-size: 0.875rem; color: var(--secondary-text); }
    .form-input { padding: 0.625rem; border: 1px solid var(--line); border-radius: 8px; font-size: 0.875rem; }
    .model-cards { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
    .model-card { border: 1px solid var(--line); border-radius: 8px; padding: 0.875rem; cursor: pointer; display: flex; flex-direction: column; gap: 0.25rem; }
    .model-card.selected { border-color: #d97706; background: var(--warning-soft); }
    .model-card p { font-size: 0.75rem; color: var(--muted); margin: 0; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-end; }
    .save-error { grid-column: 1 / -1; display: flex; flex-direction: column; gap: .25rem; margin-top: 1rem; padding: .75rem 1rem; border: 1px solid color-mix(in srgb, var(--danger) 45%, var(--line)); border-radius: 8px; background: var(--danger-soft); color: var(--danger); }
    @media (max-width: 760px) {
      .page-header { align-items: stretch; flex-direction: column; gap: 1rem; }
      .form-grid, .model-cards { grid-template-columns: 1fr; }
      .col-span-2 { grid-column: auto; }
    }
  
    /* BORTQALA_WORKFORCE_UI_20260816_V2: one clean title/action row for Workers and Contractors. */
    .page-header {
      display:flex !important;
      align-items:flex-end !important;
      justify-content:space-between !important;
      gap:1.25rem !important;
      flex-wrap:wrap !important;
      margin-bottom:1.5rem;
    }
    .page-header > :not(.header-actions) {
      min-width:0;
      flex:1 1 360px;
    }
    .header-actions {
      flex:0 0 auto;
      display:flex !important;
      align-items:center !important;
      justify-content:flex-end !important;
      gap:.6rem !important;
      flex-wrap:wrap !important;
      margin:0 !important;
      align-self:flex-end;
    }
    .header-actions > button,
    .header-actions > .button,
    .header-actions > app-workforce-excel-import-button {
      flex:0 0 auto;
      margin:0 !important;
    }
    .header-actions button,
    .header-actions .button {
      min-height:44px;
      white-space:nowrap;
      border-radius:10px;
    }
    @media (max-width:900px) {
      .page-header { align-items:flex-start !important; }
      .page-header > :not(.header-actions) { flex-basis:100%; }
      .header-actions { width:100%; justify-content:flex-start !important; }
    }
    @media (max-width:560px) {
      .header-actions { display:grid !important; grid-template-columns:1fr; }
      .header-actions > button,
      .header-actions > .button,
      .header-actions > app-workforce-excel-import-button { width:100%; }
      .header-actions app-workforce-excel-import-button { display:flex; }
      .header-actions app-workforce-excel-import-button .button { width:100%; }
    }
`]
})
export class ContractorsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  i18n = inject(I18nService);
  notification = inject(NotificationService);

  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);

  contractorStatusLabel(s: string): string {
    return s === 'ACTIVE' ? this.i18n.t('common.active') : this.i18n.t('common.inactive');
  }

  isModalOpen = false;
  editingContractor: Contractor | null = null;
  form: Partial<Contractor> = {
    code: '', name: '', tradeName: '', phone: '',
    accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
    settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE'
  };

  ngOnInit() {
    this.workforceService.loadContractors().subscribe();
  }

  exportExcel(): void {
    this.workforceService.exportContractorsExcel().subscribe(blob => downloadBlob(blob, `contractors-${new Date().toISOString().slice(0, 10)}.xlsx`));
  }

  openCreateModal() {
    this.editingContractor = null;
    this.saveError.set(null);
    this.form = {
      code: 'CTR-' + Math.floor(100 + Math.random() * 900),
      name: '', tradeName: '', phone: '',
      accountingModel: 'worker_net_total', paymentRouting: 'contractor_full',
      settlementCycleDays: 15, defaultDailyRate: 0, status: 'ACTIVE'
    };
    this.isModalOpen = true;
  }

  openEditModal(c: Contractor) {
    this.editingContractor = c;
    this.saveError.set(null);
    this.form = { ...c };
    this.isModalOpen = true;
  }

  closeModal() {
    if (this.saving()) return;
    this.isModalOpen = false;
    this.saveError.set(null);
  }

  async saveContractor(): Promise<void> {
    if (this.saving()) return;
    if (!this.form.code?.trim() || !this.form.name?.trim() || !this.form.phone?.trim()) {
      this.saveError.set(this.i18n.t('workforce.ui.contractors.requiredError')); 
      return;
    }

    const wasEditing = !!this.editingContractor;
    this.saving.set(true);
    this.saveError.set(null);
    try {
      const saved = this.editingContractor
        ? await firstValueFrom(this.workforceService.updateContractor(this.editingContractor.id, this.form))
        : await firstValueFrom(this.workforceService.createContractor(this.form));

      const refreshed = await firstValueFrom(this.workforceService.loadContractors());
      if (!saved?.id || !refreshed.some(contractor => contractor.id === saved.id)) {
        throw new Error(this.i18n.t('workforce.ui.contractors.verifyFailed')); 
      }

      this.editingContractor = saved;
      this.isModalOpen = false;
      this.notification.success(this.i18n.t(wasEditing ? 'workforce.ui.contractors.updatedSuccess' : 'workforce.ui.contractors.createdSuccess')); 
    } catch (error: any) {
      const message =
        error?.error?.message ??
        error?.error?.detail ??
        error?.message ??
        this.i18n.t('workforce.ui.contractors.saveFailed');
      this.saveError.set(message);
    } finally {
      this.saving.set(false);
    }
  }

  getModelLabel(model: string): string {
    switch (model) {
      case 'worker_net_total': return 'مجموع صافي العمال';
      case 'contractor_daily_rate': return 'سعر تعاقد مستقل';
      case 'worker_cost_plus_fee': return 'تكلفة العمال + عمولة';
      case 'fixed_period_amount': return 'مبلغ ثابت للفترة';
      default: return model;
    }
  }

  getRoutingLabel(routing: string): string { const keys:Record<string,string>={contractor_full:'workforce.ui.contractors.routingFullShort',worker_direct:'workforce.ui.contractors.routingDirectShort',mixed:'workforce.ui.contractors.routingMixedShort'}; return keys[routing]?this.i18n.t(keys[routing]):routing; }
}

// BORTQALA_FEEDBACK_20260816_EXCEL_CONTRACTORS

// BORTQALA_WORKFORCE_UI_20260816_V2
