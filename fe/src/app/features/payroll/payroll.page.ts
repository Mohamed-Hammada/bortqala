import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { PaymentMethod, PayrollRow } from './payroll.models';
import { PayrollStore } from './payroll.store';

@Component({
  selector: 'app-payroll-page',
  imports: [DecimalPipe, ReactiveFormsModule, TablePaginationComponent],
  providers: [PayrollStore],
  templateUrl: './payroll.page.html',
  styleUrl: './payroll.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PayrollPage {
  readonly store = inject(PayrollStore);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly year = signal(new Date().getFullYear());
  readonly month = signal(new Date().getMonth() + 1);
  readonly selectedCategory = signal<string>('');
  readonly drawerOpen = signal(false);
  readonly selectedRow = signal<PayrollRow | null>(null);

  readonly pagination = new TablePagination();
  readonly rows = computed(() => this.store.data()?.rows ?? []);
  readonly pagedRows = computed(() => this.pagination.slice(this.rows()));

  readonly monthKeys = [
    'month.1',
    'month.2',
    'month.3',
    'month.4',
    'month.5',
    'month.6',
    'month.7',
    'month.8',
    'month.9',
    'month.10',
    'month.11',
    'month.12',
  ];

  readonly payForm = this.formBuilder.nonNullable.group({
    grossAmount: [{ value: 0, disabled: true }],
    advancesDeducted: [0, [Validators.required, Validators.min(0)]],
    otherDeductions: [0, [Validators.required, Validators.min(0)]],
    bonuses: [0, [Validators.required, Validators.min(0)]],
    netAmount: [{ value: 0, disabled: true }],
    paymentMethod: ['CASH' as PaymentMethod, Validators.required],
    referenceCode: [''],
    note: [''],
  });

  constructor() {
    void this.reload();
  }

  async reload(): Promise<void> {
    await this.store.load(this.year(), this.month(), this.selectedCategory());
  }

  changePeriod(yearStr: string, monthStr: string): void {
    const y = Number(yearStr);
    const m = Number(monthStr);
    if (y >= 2000 && m >= 1 && m <= 12) {
      this.year.set(y);
      this.month.set(m);
      void this.reload();
    }
  }

  openPaymentDrawer(row: PayrollRow): void {
    if (row.incompleteProfile) {
      this.notification.error('ملف الموظف غير مكتمل (الراتب الأساسي أو الفئة غائبة). يرجى التوجه لصفحة /employees لاستكمال البيانات.');
      return;
    }
    this.selectedRow.set(row);
    this.payForm.patchValue({
      grossAmount: row.grossAmount,
      advancesDeducted: row.advancesDeducted,
      otherDeductions: row.otherDeductions,
      bonuses: row.bonuses,
      netAmount: row.netAmount,
      paymentMethod: row.paymentMethod ?? 'CASH',
      referenceCode: row.referenceCode ?? '',
      note: row.note ?? '',
    });
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.selectedRow.set(null);
  }

  recalculateNet(): void {
    const row = this.selectedRow();
    if (!row) return;
    const gross = row.grossAmount;
    const advances = this.payForm.controls.advancesDeducted.value || 0;
    const deductions = this.payForm.controls.otherDeductions.value || 0;
    const bonuses = this.payForm.controls.bonuses.value || 0;
    const net = Math.max(0, gross - advances - deductions + bonuses);
    this.payForm.patchValue({ netAmount: net }, { emitEvent: false });
  }

  async submitPayment(): Promise<void> {
    const row = this.selectedRow();
    if (!row || this.payForm.invalid) return;
    const raw = this.payForm.getRawValue();
    const ok = await this.store.recordPayment({
      employeeId: row.employeeId,
      periodYear: row.periodYear,
      periodMonth: row.periodMonth,
      periodKind: row.periodKind,
      periodStart: row.periodStart,
      periodEnd: row.periodEnd,
      grossAmount: row.grossAmount,
      advancesDeducted: raw.advancesDeducted,
      otherDeductions: raw.otherDeductions,
      bonuses: raw.bonuses,
      netAmount: Math.max(0, row.grossAmount - raw.advancesDeducted - raw.otherDeductions + raw.bonuses),
      paymentMethod: raw.paymentMethod,
      referenceCode: raw.referenceCode,
      note: raw.note,
    });

    if (ok) {
      this.notification.success(this.i18n.t('payroll.paymentSaved', undefined, 'تم تسجيل صرف المرتب وقيد المستند بنجاح.'));
      this.closeDrawer();
    }
  }

  async transitionPeriod(targetStatus: any): Promise<void> {
    const ok = await this.store.transitionStatus({
      periodYear: this.year(),
      periodMonth: this.month(),
      targetStatus,
      categoryId: this.selectedCategory(),
    });
    if (ok) {
      this.notification.success('تم تحديث حالة كشف المرتبات للفترة إلى: ' + targetStatus);
    }
  }

  async reversePayment(row: PayrollRow): Promise<void> {
    if (!row.id) return;
    const reason = prompt('يرجى إدخال سبب التراجع عن صرف الراتب:');
    if (!reason || !reason.trim()) return;

    const ok = await this.store.reversePayment({
      paymentId: row.id,
      reason: reason.trim(),
    });
    if (ok) {
      this.notification.success('تم التراجع عن قيد صرف الراتب واسترداد خصم السلفة بنجاح.');
    }
  }

  async payBulk(): Promise<void> {
    if (!confirm(this.i18n.t('payroll.confirmBulkPay', undefined, 'هل أنت تأكد من صرف المرتبات لجميع الموظفين المتبقيين؟'))) {
      return;
    }
    const ok = await this.store.payBulk({
      periodYear: this.year(),
      periodMonth: this.month(),
      categoryId: this.selectedCategory(),
    });
    if (ok) {
      this.notification.success(this.i18n.t('payroll.bulkSaved', undefined, 'تم الصرف الجماعي وقيد المعاملات بنجاح.'));
    }
  }

  formatDate(value: string | null): string {
    if (!value) return '—';
    try {
      const d = new Date(value);
      return d.toLocaleDateString('ar-EG', { year: 'numeric', month: '2-digit', day: '2-digit' }) + ' ' + d.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return value;
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'DRAFT': return 'مسودة (Draft)';
      case 'CALCULATED': return 'محسوب (Calculated)';
      case 'REVIEWED': return 'مُرَاجَع (Reviewed)';
      case 'APPROVED': return 'معتمد ومقفول (Approved)';
      case 'POSTED': return 'مُرحّل محاسبياً (Posted)';
      case 'PAID': return 'مدفوع ومكتمل (Paid)';
      case 'REVERSED': return 'متراجع عنه (Reversed)';
      default: return status;
    }
  }

  async exportExcel(): Promise<void> {
    await this.store.exportExcel(this.year(), this.month(), this.selectedCategory());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.drawerOpen()) this.closeDrawer();
  }
}
