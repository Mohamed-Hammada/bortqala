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
import { apiErrorMessage } from '../../../../core/api-error';
import { WorkforceExcelImportButtonComponent } from '../../ui/workforce-excel-import-button.component';

@Component({
  selector: 'app-contractors',
  standalone: true,
  imports: [WorkforceExcelImportButtonComponent, CommonModule, FormsModule, ModalDialogComponent],
  templateUrl: './contractors.component.html',
  styleUrls: ['./contractors.component.scss']
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

      if (!saved?.id) {
        this.saveError.set(this.i18n.t('workforce.ui.contractors.verifyFailed')); 
        return;
      }

      const refreshed = await firstValueFrom(this.workforceService.loadContractors());
      if (!refreshed.some(contractor => contractor.id === saved.id)) {
        this.saveError.set(this.i18n.t('workforce.ui.contractors.verifyFailed')); 
        return;
      }

      this.editingContractor = saved;
      this.isModalOpen = false;
      this.notification.success(this.i18n.t(wasEditing ? 'workforce.ui.contractors.updatedSuccess' : 'workforce.ui.contractors.createdSuccess')); 
    } catch (error: unknown) {
      this.saveError.set(apiErrorMessage(error, this.i18n));
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
