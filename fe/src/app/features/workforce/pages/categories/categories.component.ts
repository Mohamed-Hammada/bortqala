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
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.scss']
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
