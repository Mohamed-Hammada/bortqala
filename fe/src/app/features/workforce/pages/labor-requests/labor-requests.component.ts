import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { apiErrorDetail } from '../../../../core/api-error';
import { LaborRequest, LaborRequestItem } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-labor-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, AppTooltipDirective],
  templateUrl: './labor-requests.component.html',
  styleUrls: ['./labor-requests.component.scss']
})
export class LaborRequestsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;

  form: {
    requestNumber: string; contractorId: string; shiftName: string;
    notes: string; items: LaborRequestItem[];
  } = this.defaultForm();

  ngOnInit() {
    this.loading.set(true);
    forkJoin({
      requests: this.workforceService.loadLaborRequests(),
      contractors: this.workforceService.loadContractors(),
      categories: this.workforceService.loadCategories(),
    }).subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
  }

  openCreateModal() {
    this.form = this.defaultForm();
    const ctrs = this.workforceService.contractors();
    if (ctrs.length > 0) this.form.contractorId = ctrs[0].id;
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  addItem() {
    const cats = this.workforceService.categories();
    this.form.items.push({
      categoryId: cats.length > 0 ? cats[0].id : '',
      categoryName: cats.length > 0 ? cats[0].name : '',
      requestedCount: 1,
      sentCount: 0,
      acceptedCount: 0,
      varianceCount: 0
    });
  }

  removeItem(index: number) {
    this.form.items.splice(index, 1);
  }

  saveRequest() {
    if (!this.form.requestNumber || !this.form.contractorId) {
      this.notificationService.warning(this.i18n.t('workforce.ui.requests.requiredWarning'));
      return;
    }
    this.saving.set(true);
    this.workforceService.createLaborRequest(this.form).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.closeModal();
        this.notificationService.success(this.i18n.t('workforce.ui.requests.createdSuccess', { number: res.requestNumber }));
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? this.i18n.t('workforce.ui.unexpectedError'));
        this.notificationService.error(this.i18n.t('workforce.ui.requests.createFailed', { detail: msg }));
      }
    });
  }

  approveRequest(id: string) {
    // Approve endpoint: PUT /api/v1/workforce/labor-requests/{id}/approve
    this.notificationService.info(this.i18n.t('workforce.ui.requests.approving'));
  }

  // --- Helpers ---
  countByStatus(status: string): number {
    return this.workforceService.laborRequests().filter(r => r.status === status).length;
  }

  getTotalRequested(req: LaborRequest): number {
    return (req.items ?? []).reduce((s, i) => s + (i.requestedCount ?? 0), 0);
  }

  getTotalRequestedFromForm(): number {
    return this.form.items.reduce((s, i) => s + (i.requestedCount ?? 0), 0);
  }

  getStatusLabel(status: string): string { const keys:Record<string,string>={DRAFT:'workforce.ui.requests.draft',SENT:'workforce.ui.requests.statusSent',APPROVED:'workforce.ui.requests.approved',COMPLETED:'workforce.ui.requests.statusCompleted',CANCELLED:'workforce.ui.requests.statusCancelled'}; return keys[status]?this.i18n.t(keys[status]):status; }

  private defaultForm() {
    return {
      requestNumber: 'REQ-' + String(Date.now()).slice(-6),
      contractorId: '', shiftName: this.i18n.t('workforce.ui.requests.firstShift'),
      notes: '', items: [] as LaborRequestItem[]
    };
  }
}
