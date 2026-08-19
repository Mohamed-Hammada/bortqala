import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { LeavesService } from './leaves.service';
import { LeaveBalance, LeaveRequest, LeaveRequestStatus, LeaveType } from './leaves.models';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-leaves-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ModalDialogComponent,
    SkeletonComponent,
    EmptyStateComponent,
    TablePaginationComponent,
  ],
  templateUrl: './leaves.page.html',
  styleUrl: './leaves.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeavesPage {
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly leavesService = inject(LeavesService);
  private readonly http = inject(HttpClient);

  readonly activeTab = signal<'REQUESTS' | 'BALANCES' | 'TYPES'>('REQUESTS');
  readonly loading = signal(false);

  // Master Data Signals
  readonly requests = signal<LeaveRequest[]>([]);
  readonly balances = signal<LeaveBalance[]>([]);
  readonly types = signal<LeaveType[]>([]);
  readonly employees = signal<Array<{ id: string; fullName: string; employeeCode: string }>>([]);

  // Modals and Drawers
  readonly requestDrawerOpen = signal(false);
  readonly typeDrawerOpen = signal(false);
  readonly rejectModalOpen = signal(false);
  readonly selectedRequestForReject = signal<LeaveRequest | null>(null);

  readonly pagination = new TablePagination();

  // Forms
  readonly requestForm = new FormGroup({
    employeeId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    leaveTypeId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    startDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
    endDate: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }),
    reason: new FormControl('', { nonNullable: true }),
  });

  readonly typeForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nameAr: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    nameEn: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    paid: new FormControl(true, { nonNullable: true }),
    requiresAttachment: new FormControl(false, { nonNullable: true }),
    maxConsecutiveDays: new FormControl(30, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
  });

  readonly rejectForm = new FormGroup({
    rejectionReason: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly pagedRequests = computed(() => this.pagination.slice(this.requests()));

  constructor() {
    void this.init();
  }

  async init() {
    this.loading.set(true);
    try {
      await Promise.all([this.loadRequests(), this.loadBalances(), this.loadTypes(), this.loadEmployees()]);
    } finally {
      this.loading.set(false);
    }
  }

  setTab(tab: 'REQUESTS' | 'BALANCES' | 'TYPES') {
    this.activeTab.set(tab);
  }

  async loadRequests() {
    try {
      const data = await this.leavesService.listRequests();
      this.requests.set(data);
    } catch {
      this.requests.set([]);
    }
  }

  async loadBalances() {
    try {
      const data = await this.leavesService.listBalances();
      this.balances.set(data);
    } catch {
      this.balances.set([]);
    }
  }

  async loadTypes() {
    try {
      const data = await this.leavesService.listTypes();
      this.types.set(data);
    } catch {
      this.types.set([]);
    }
  }

  async loadEmployees() {
    try {
      const data = await this.leavesService.listEmployees();
      this.employees.set(data);
    } catch {
      this.employees.set([]);
    }
  }

  openNewRequest() {
    this.requestForm.reset({
      employeeId: this.employees()[0]?.id ?? '',
      leaveTypeId: this.types()[0]?.id ?? '',
      startDate: new Date().toISOString().slice(0, 10),
      endDate: new Date().toISOString().slice(0, 10),
      reason: '',
    });
    this.requestDrawerOpen.set(true);
  }

  closeRequestDrawer() {
    this.requestDrawerOpen.set(false);
  }

  async submitRequest() {
    if (this.requestForm.invalid) return;
    try {
      await this.leavesService.submitRequest(this.requestForm.getRawValue());
      this.notification.success(this.i18n.t('leaves.saved'));
      this.closeRequestDrawer();
      await Promise.all([this.loadRequests(), this.loadBalances()]);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async approve(r: LeaveRequest) {
    try {
      await this.leavesService.approveRequest(r.id);
      this.notification.success(this.i18n.t('leaves.approved'));
      await Promise.all([this.loadRequests(), this.loadBalances()]);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  openRejectModal(r: LeaveRequest) {
    this.selectedRequestForReject.set(r);
    this.rejectForm.reset({ rejectionReason: '' });
    this.rejectModalOpen.set(true);
  }

  closeRejectModal() {
    this.rejectModalOpen.set(false);
    this.selectedRequestForReject.set(null);
  }

  async submitReject() {
    if (this.rejectForm.invalid) return;
    const r = this.selectedRequestForReject();
    if (!r) return;
    try {
      await this.leavesService.rejectRequest(r.id, this.rejectForm.getRawValue());
      this.notification.success(this.i18n.t('leaves.rejected'));
      this.closeRejectModal();
      await Promise.all([this.loadRequests(), this.loadBalances()]);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async cancel(r: LeaveRequest) {
    try {
      await this.leavesService.cancelRequest(r.id);
      this.notification.success(this.i18n.t('leaves.cancelled'));
      await Promise.all([this.loadRequests(), this.loadBalances()]);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  openNewType() {
    this.typeForm.reset({
      code: '',
      nameAr: '',
      nameEn: '',
      paid: true,
      requiresAttachment: false,
      maxConsecutiveDays: 30,
    });
    this.typeDrawerOpen.set(true);
  }

  closeTypeDrawer() {
    this.typeDrawerOpen.set(false);
  }

  async submitType() {
    if (this.typeForm.invalid) return;
    try {
      await this.leavesService.createType(this.typeForm.getRawValue());
      this.notification.success(this.i18n.t('leaves.typeSaved'));
      this.closeTypeDrawer();
      await this.loadTypes();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  statusLabel(status: LeaveRequestStatus): string {
    switch (status) {
      case 'PENDING_APPROVAL': return this.i18n.t('leaves.statusPending');
      case 'APPROVED': return this.i18n.t('leaves.statusApproved');
      case 'REJECTED': return this.i18n.t('leaves.statusRejected');
      case 'CANCELLED': return this.i18n.t('leaves.statusCancelled');
      default: return status;
    }
  }
}
