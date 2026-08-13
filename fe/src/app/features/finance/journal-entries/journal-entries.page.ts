import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { TablePagination } from '../../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../../shared/ui/table-pagination/table-pagination.component';
import { Account } from '../accounts/accounts.page';
import { AppTooltipDirective } from '../../../shared/ui/app-tooltip/app-tooltip.directive';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface JournalEntryLine {
  id?: string;
  accountId: string;
  partyId?: string;
  debit: number;
  credit: number;
  memo?: string;
  costCenterId?: string;
  projectId?: string;
  departmentId?: string;
}

export interface JournalEntry {
  id: string;
  entryNumber: string;
  entryDate: number;
  description: string;
  reference?: string;
  status: 'DRAFT' | 'POSTED' | 'REVERSED';
  fiscalPeriodId?: string;
  currency?: string;
  postedBy?: string;
  postedAt?: number;
  operationId?: string;
  version: number;
  lines: JournalEntryLine[];
  totalDebit: number;
  totalCredit: number;
  createdAt: number;
  updatedAt: number;
}

export interface JournalActionRequest {
  operationId: string;
  expectedVersion: number;
  reason?: string | null;
}

export interface JournalEntryPage {
  content: JournalEntry[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

interface NumberingSettings {
  automaticNumbering: boolean;
}

@Component({
  selector: 'app-journal-entries-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, DecimalPipe, AppTooltipDirective, ModalDialogComponent],
  templateUrl: './journal-entries.page.html',
  styleUrl: './journal-entries.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalEntriesPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly confirm = inject(ConfirmDialogService);

  readonly loadingEntries = signal(true);
  readonly loadingAccounts = signal(false);
  readonly savingDraft = signal(false);
  readonly postingIds = signal<Set<string>>(new Set());
  readonly entriesError = signal<string | null>(null);
  readonly accountsError = signal<string | null>(null);
  readonly dialogError = signal<string | null>(null);
  readonly pendingPostOperations = signal<Record<string, string>>({});

  readonly entries = signal<JournalEntry[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly totalElements = signal<number>(0);
  readonly pagination = new TablePagination();

  readonly drawerOpen = signal(false);
  readonly submitAttempted = signal(false);
  readonly automaticNumbering = signal(true);

  readonly entryForm = new FormGroup({
    entryNumber: new FormControl('', { nonNullable: true }),
    entryDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    reference: new FormControl('', { nonNullable: true }),
  });

  readonly lines = signal<{ accountId: string; debit: number; credit: number; memo: string; costCenterId?: string; projectId?: string; departmentId?: string }[]>([
    { accountId: '', debit: 0, credit: 0, memo: '', costCenterId: '', projectId: '', departmentId: '' },
    { accountId: '', debit: 0, credit: 0, memo: '', costCenterId: '', projectId: '', departmentId: '' },
  ]);

  readonly lineErrors = computed(() => {
    const errors = new Map<number, string>();
    this.lines().forEach((line, index) => {
      if (!line.accountId) {
        errors.set(index, this.i18n.t('journal.lineAccountRequired'));
      } else if (line.debit < 0 || line.credit < 0) {
        errors.set(index, this.i18n.t('journal.lineNegativeAmount'));
      } else if (line.debit > 0 && line.credit > 0) {
        errors.set(index, this.i18n.t('journal.lineOneSideOnly'));
      } else if (!(line.debit > 0 || line.credit > 0)) {
        errors.set(index, this.i18n.t('journal.lineEmptyAmount'));
      }
    });
    return errors;
  });

  constructor() {
    void this.load(0);
    void this.loadAccounts();
    void this.loadNumberingSettings();
  }

  async loadNumberingSettings() {
    try {
      const settings = await firstValueFrom(
        this.http.get<NumberingSettings>('/api/v1/finance/numbering-settings'),
      );
      this.automaticNumbering.set(settings?.automaticNumbering ?? true);
    } catch {
      this.automaticNumbering.set(true);
    }
    this.applyNumberingValidators();
  }

  applyNumberingValidators(): void {
    const auto = this.automaticNumbering();
    const entryNumber = this.entryForm.controls.entryNumber;
    entryNumber.setValidators(auto ? [] : [Validators.required]);
    entryNumber.updateValueAndValidity();
  }

  async load(pageIndex: number = 0) {
    this.loadingEntries.set(true);
    this.entriesError.set(null);
    try {
      const res = await firstValueFrom(
        this.http.get<JournalEntryPage>('/api/v1/finance/journal-entries', {
          params: { page: pageIndex, size: this.pagination.pageSize() },
        }),
      );
      this.entries.set(res.content);
      this.totalElements.set(res.totalElements);
    } catch (e) {
      this.entriesError.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loadingEntries.set(false);
    }
  }

  async loadAccounts() {
    this.loadingAccounts.set(true);
    this.accountsError.set(null);
    try {
      const data = await firstValueFrom(this.http.get<Account[]>('/api/v1/finance/accounts'));
      this.accounts.set(data.filter((a) => !a.isHeader));
    } catch (e) {
      this.accountsError.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loadingAccounts.set(false);
    }
  }

  retryAccounts() {
    void this.loadAccounts();
  }

  openNew() {
    this.entryForm.reset({
      entryNumber: '',
      entryDate: new Date().toISOString().substring(0, 10),
      description: '',
      reference: '',
    });
    this.applyNumberingValidators();
    this.lines.set([
      { accountId: this.accounts()[0]?.id ?? '', debit: 0, credit: 0, memo: '', costCenterId: '', projectId: '', departmentId: '' },
      { accountId: this.accounts()[1]?.id ?? '', debit: 0, credit: 0, memo: '', costCenterId: '', projectId: '', departmentId: '' },
    ]);
    this.dialogError.set(null);
    this.submitAttempted.set(false);
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
    this.dialogError.set(null);
    this.submitAttempted.set(false);
  }

  addLine() {
    this.lines.update((arr) => [...arr, { accountId: this.accounts()[0]?.id ?? '', debit: 0, credit: 0, memo: '', costCenterId: '', projectId: '', departmentId: '' }]);
  }

  removeLine(index: number) {
    if (this.lines().length <= 2) return;
    this.lines.update((arr) => arr.filter((_, i) => i !== index));
  }

  updateLine(index: number, field: string, value: any) {
    this.lines.update((arr) => {
      const updated = [...arr];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  }

  calculateSumDebit(): number {
    return this.lines().reduce((sum, l) => sum + (+l.debit || 0), 0);
  }

  calculateSumCredit(): number {
    return this.lines().reduce((sum, l) => sum + (+l.credit || 0), 0);
  }

  async submitEntry() {
    if (this.entryForm.invalid || this.savingDraft()) return;
    this.submitAttempted.set(true);
    if (this.lineErrors().size > 0) {
      this.dialogError.set(this.i18n.t('journal.lineValidationError'));
      return;
    }
    const sumDebit = this.calculateSumDebit();
    const sumCredit = this.calculateSumCredit();

    if (Math.abs(sumDebit - sumCredit) > 0.001) {
      this.dialogError.set(this.i18n.t('journal.unbalancedError', { debit: sumDebit, credit: sumCredit }));
      return;
    }

    this.savingDraft.set(true);
    this.dialogError.set(null);
    try {
      const formVal = this.entryForm.getRawValue();
      const dateMs = new Date(formVal.entryDate).getTime();
      const payload = {
        entryNumber: formVal.entryNumber.trim() || null,
        entryDate: dateMs,
        description: formVal.description,
        reference: formVal.reference,
        lines: this.lines(),
      };
      await firstValueFrom(this.http.post('/api/v1/finance/journal-entries', payload));
      this.notification.success(this.i18n.t('journal.saved'));
      this.drawerOpen.set(false);
      await this.load(0);
    } catch (e) {
      this.dialogError.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.savingDraft.set(false);
    }
  }

  requestPost(entry: JournalEntry) {
    void this.confirm.confirmAndRun(
      {
        titleKey: 'journal.post.confirmTitle',
        messageKey: 'journal.post.confirmMessage',
        confirmKey: 'journal.post',
        danger: true,
        dangerMessageKey: 'journal.post.dangerWarning',
        details: [
          { label: this.i18n.t('journal.colNumber'), value: entry.entryNumber },
          { label: this.i18n.t('journal.colDate'), value: this.date(entry.entryDate) },
          { label: this.i18n.t('journal.colDescription'), value: entry.description },
          { label: this.i18n.t('journal.colDebitTotal'), value: entry.totalDebit.toFixed(2) },
          { label: this.i18n.t('journal.colCreditTotal'), value: entry.totalCredit.toFixed(2) },
        ],
      },
      () => this.postEntry(entry),
    );
  }

  isPosting(entryId: string): boolean {
    return this.postingIds().has(entryId);
  }

  private getPostOperationId(entryId: string): string {
    const existing = this.pendingPostOperations()[entryId];
    if (existing) return existing;

    const generated = crypto.randomUUID();
    this.pendingPostOperations.update((value) => ({
      ...value,
      [entryId]: generated,
    }));
    return generated;
  }

  private clearPendingPostOperation(entryId: string): void {
    this.pendingPostOperations.update((value) => {
      const next = { ...value };
      delete next[entryId];
      return next;
    });
  }

  async postEntry(entry: JournalEntry): Promise<void> {
    if (entry.status !== 'DRAFT' || this.postingIds().has(entry.id)) return;

    const operationId = this.getPostOperationId(entry.id);
    this.postingIds.update((ids) => new Set(ids).add(entry.id));

    try {
      const payload: JournalActionRequest = {
        operationId,
        expectedVersion: entry.version,
        reason: null,
      };

      await firstValueFrom(
        this.http.post(`/api/v1/finance/journal-entries/${entry.id}/post`, payload),
      );

      this.clearPendingPostOperation(entry.id);
      this.notification.success(this.i18n.t('journal.post.success'));
      await this.load(this.pagination.currentPage(this.totalElements()));
    } catch (error) {
      const status = error instanceof HttpErrorResponse ? error.status : 0;
      if (status !== 0) {
        this.clearPendingPostOperation(entry.id);
      }
      throw error;
    } finally {
      this.postingIds.update((ids) => {
        const next = new Set(ids);
        next.delete(entry.id);
        return next;
      });
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
