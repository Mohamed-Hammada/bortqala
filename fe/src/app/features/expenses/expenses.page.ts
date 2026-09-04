import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/api-error';
import { ConfirmDialogService } from '../../core/confirm-dialog.service';
import { dateInputToEpoch } from '../../core/date';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { ExpenseService } from './expense.service';
import {
  CreateClaimRequest,
  ExpenseCategory,
  ExpenseClaimResponse,
  ExpenseStatus,
} from './expense.models';

@Component({
  selector: 'app-expenses-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './expenses.page.html',
  styleUrl: './expenses.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpensesPage {
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly expenseService = inject(ExpenseService);
  private readonly confirm = inject(ConfirmDialogService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly claims = signal<ExpenseClaimResponse[]>([]);
  readonly pendingClaims = signal<ExpenseClaimResponse[]>([]);
  readonly activeTab = signal<'mine' | 'pending'>('mine');
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly processing = signal<string | null>(null);

  readonly isHrOrAdmin = computed(() => {
    return this.auth.isSuperAdmin() || this.auth.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  });

  readonly isFinance = computed(() => {
    return this.auth.isSuperAdmin() || this.auth.hasAnyRole(['ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT']);
  });

  readonly filteredClaims = computed(() => {
    if (this.activeTab() === 'pending') return this.pendingClaims();
    return this.claims();
  });

  readonly form = new FormGroup({
    category: new FormControl<ExpenseCategory>('MEAL', { nonNullable: true, validators: [Validators.required] }),
    spentOn: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.01)] }),
    currency: new FormControl('EGP', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  readonly reimburseForm = new FormGroup({
    reference: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly reimburseTarget = signal<ExpenseClaimResponse | null>(null);
  readonly reimburseOpen = signal(false);
  readonly rejectTarget = signal<ExpenseClaimResponse | null>(null);
  readonly rejectOpen = signal(false);
  readonly rejectNote = new FormControl('', { nonNullable: true });
  readonly approveTarget = signal<ExpenseClaimResponse | null>(null);
  readonly approveOpen = signal(false);
  readonly approveNote = new FormControl('', { nonNullable: true });

  readonly categories: ExpenseCategory[] = ['MEAL', 'TRANSPORT', 'LODGING', 'SUPPLIES', 'OTHER'];
  readonly statuses: ExpenseStatus[] = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'REIMBURSED'];

  readonly categoryLabels: Record<string, string> = {};

  constructor() {
    this.categories.forEach((c) => {
      this.categoryLabels[c] = this.i18n.t(`expenses.category.${c.toLowerCase()}`);
    });
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.claims.set(await this.expenseService.listMine() ?? []);
      if (this.isHrOrAdmin()) {
        this.pendingClaims.set(await this.expenseService.listPending() ?? []);
      }
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  categoryLabel(cat: ExpenseCategory): string {
    return this.categoryLabels[cat] || cat;
  }

  statusLabel(status: ExpenseStatus): string {
    return this.i18n.t(`expenses.status.${status.toLowerCase()}`);
  }

  formatAmount(amount: number, currency: string): string {
    return `${currency || 'EGP'} ${amount?.toFixed(2) || '0.00'}`;
  }

  formatTimestamp(ts: number | null): string {
    if (!ts) return '—';
    return new Date(ts).toLocaleDateString();
  }

  openCreate() {
    this.editingId.set(null);
    this.form.reset({ category: 'MEAL', spentOn: '', amount: 0, currency: 'EGP', description: '' });
    this.drawerOpen.set(true);
  }

  openEdit(claim: ExpenseClaimResponse) {
    this.editingId.set(claim.id);
    this.form.patchValue({
      category: claim.category,
      spentOn: claim.spentOn,
      amount: claim.amount,
      currency: claim.currency || 'EGP',
      description: claim.description || '',
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
    this.editingId.set(null);
  }

  async save() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    try {
      const val = this.form.value;
      const payload: CreateClaimRequest = {
        category: val.category as ExpenseCategory,
        spentOn: dateInputToEpoch(val.spentOn!),
        amount: val.amount!,
        currency: val.currency || 'EGP',
        description: val.description || undefined,
      };
      if (this.editingId()) {
        await this.expenseService.update(this.editingId()!, payload);
        this.notification.success(this.i18n.t('common.saved'));
      } else {
        await this.expenseService.create(payload);
        this.notification.success(this.i18n.t('common.created'));
      }
      this.closeDrawer();
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async submitClaim(claim: ExpenseClaimResponse) {
    if (!await this.confirm.confirm(this.i18n.t('expenses.confirmSubmit'))) return;
    this.processing.set(claim.id);
    try {
      await this.expenseService.submit(claim.id);
      this.notification.success(this.i18n.t('expenses.submitSuccess'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.processing.set(null);
    }
  }

  openApproveReject(claim: ExpenseClaimResponse, action: 'approve' | 'reject') {
    if (action === 'approve') {
      if (claim.limitExceeded) {
        this.approveTarget.set(claim);
        this.approveNote.reset();
        this.approveOpen.set(true);
      } else {
        void this.approveClaim(claim);
      }
    } else {
      this.rejectTarget.set(claim);
      this.rejectNote.reset();
      this.rejectOpen.set(true);
    }
  }

  async approveClaim(claim: ExpenseClaimResponse, note?: string) {
    this.processing.set(claim.id);
    try {
      await this.expenseService.approve(claim.id, note);
      this.notification.success(this.i18n.t('expenses.approveSuccess'));
      this.approveOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.processing.set(null);
    }
  }

  approveFromModal() {
    const target = this.approveTarget();
    if (!target) return;
    void this.approveClaim(target, this.approveNote.value || undefined);
  }

  async rejectClaim() {
    const target = this.rejectTarget();
    if (!target) return;
    this.processing.set(target.id);
    try {
      await this.expenseService.reject(target.id, this.rejectNote.value || undefined);
      this.notification.success(this.i18n.t('expenses.rejectSuccess'));
      this.rejectOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.processing.set(null);
    }
  }

  openReimburse(claim: ExpenseClaimResponse) {
    this.reimburseTarget.set(claim);
    this.reimburseForm.reset();
    this.reimburseOpen.set(true);
  }

  async doReimburse() {
    const target = this.reimburseTarget();
    if (!target || this.reimburseForm.invalid) return;
    if (!await this.confirm.confirm(this.i18n.t('expenses.confirmReimburse'))) return;
    this.processing.set(target.id);
    try {
      await this.expenseService.reimburse(target.id, this.reimburseForm.value.reference!);
      this.notification.success(this.i18n.t('expenses.reimburseSuccess'));
      this.reimburseOpen.set(false);
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.processing.set(null);
    }
  }

}
