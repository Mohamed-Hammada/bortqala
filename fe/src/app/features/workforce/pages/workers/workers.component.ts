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
  templateUrl: './workers.component.html',
  styleUrls: ['./workers.component.scss']
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

// BORTQALA_WORKFORCE_UI_20260816_V2
