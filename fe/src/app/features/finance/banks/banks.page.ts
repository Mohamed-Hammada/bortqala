import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';

export interface BankAccount {
  id: string;
  bankName: string;
  accountNumber: string;
  iban?: string;
  swiftCode?: string;
  accountId?: string;
  currencyCode: string;
  active: boolean;
}

interface Account { id: string; code: string; name: string; type: string; isHeader: boolean; active: boolean }
interface Statement { id: string; bankAccountId: string; statementReference: string; periodStart: number; periodEnd: number; openingBalance: number; closingBalance: number; currencyCode: string; status: string; lineCount: number; unmatchedCount: number }
interface Match { id: string; journalEntryId: string; matchedAmount: number; matchType: string; status: string }
interface Candidate { journalEntryId: string; entryNumber: string; entryDate: number; description: string; availableAmount: number; score: number; reason: string }
interface StatementLine { id: string; transactionDate: number; description: string; bankReference?: string; amount: number; status: string; matchedAmount: number; remainingAmount: number; matches: Match[]; suggestions: Candidate[] }
interface Workbench { statement: Statement; lines: StatementLine[] }
interface CashPosition { accounts: { bankAccountId: string; bankName: string; currencyCode: string; latestStatementBalance: number; asOfDate: number | null; unmatchedLines: number }[]; totalsByCurrency: Record<string, number> }

@Component({
  selector: 'app-banks-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './banks.page.html',
  styleUrl: './banks.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BanksPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly banks = signal<BankAccount[]>([]);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly tab = signal<'accounts' | 'reconciliation' | 'cash'>('accounts');
  readonly accounts = signal<Account[]>([]);
  readonly statements = signal<Statement[]>([]);
  readonly workbench = signal<Workbench | null>(null);
  readonly cashPosition = signal<CashPosition | null>(null);
  readonly importOpen = signal(false);
  readonly matchOpen = signal(false);
  readonly importFile = signal<File | null>(null);
  readonly selectedLine = signal<StatementLine | null>(null);

  readonly form = new FormGroup({
    bankName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    accountNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    iban: new FormControl('', { nonNullable: true }),
    swiftCode: new FormControl('', { nonNullable: true }),
    accountId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });
  readonly importForm = new FormGroup({
    bankAccountId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    statementReference: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    openingBalance: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
    closingBalance: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
  });
  readonly matchForm = new FormGroup({
    journalEntryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0.01)] }),
    feeAmount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0)] }),
    feeExpenseAccountId: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [banks, accounts, statements, cash] = await Promise.all([
        firstValueFrom(this.http.get<BankAccount[]>('/api/v1/finance/banks')),
        firstValueFrom(this.http.get<Account[]>('/api/v1/finance/accounts')),
        firstValueFrom(this.http.get<Statement[]>('/api/v1/finance/bank-reconciliation/statements')),
        firstValueFrom(this.http.get<CashPosition>('/api/v1/finance/bank-reconciliation/cash-position')),
      ]);
      this.banks.set(banks); this.accounts.set(accounts); this.statements.set(statements); this.cashPosition.set(cash);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.editingId.set(null);
    this.form.reset({ bankName: '', accountNumber: '', iban: '', swiftCode: '', accountId: '', currencyCode: 'EGP', active: true });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submit() {
    if (this.form.invalid) return;
    try {
      const payload = this.form.getRawValue();
      const id = this.editingId();
      await firstValueFrom(
        id
          ? this.http.put(`/api/v1/finance/banks/${id}`, payload)
          : this.http.post('/api/v1/finance/banks', payload),
      );
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  openImport(): void {
    this.importForm.reset({ bankAccountId: this.banks()[0]?.id ?? '', statementReference: '', openingBalance: 0, closingBalance: 0 });
    this.importFile.set(null); this.importOpen.set(true);
  }

  onImportFile(event: Event): void { this.importFile.set((event.target as HTMLInputElement).files?.item(0) ?? null); }

  async importStatement(): Promise<void> {
    const file = this.importFile();
    if (this.importForm.invalid || !file) { this.importForm.markAllAsTouched(); return; }
    const value = this.importForm.getRawValue();
    const body = new FormData(); body.append('file', file, file.name);
    try {
      this.workbench.set(await firstValueFrom(this.http.post<Workbench>('/api/v1/finance/bank-reconciliation/statements/import', body, {
        params: { bankAccountId: value.bankAccountId, statementReference: value.statementReference,
          openingBalance: String(value.openingBalance), closingBalance: String(value.closingBalance) },
      })));
      this.importOpen.set(false); this.tab.set('reconciliation'); await this.load();
    } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
  }

  async openStatement(id: string): Promise<void> {
    try { this.workbench.set(await firstValueFrom(this.http.get<Workbench>(`/api/v1/finance/bank-reconciliation/statements/${id}`))); }
    catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
  }

  async autoMatch(): Promise<void> {
    const statement = this.workbench()?.statement; if (!statement) return;
    await this.reconcileRequest(`/statements/${statement.id}/auto-match`, { operationId: crypto.randomUUID() });
  }

  openMatch(line: StatementLine, candidate?: Candidate): void {
    this.selectedLine.set(line);
    this.matchForm.reset({ journalEntryId: candidate?.journalEntryId ?? '',
      amount: candidate ? Math.min(line.remainingAmount, candidate.availableAmount) : line.remainingAmount,
      feeAmount: 0, feeExpenseAccountId: '' });
    this.matchOpen.set(true);
  }

  async submitMatch(): Promise<void> {
    const statement = this.workbench()?.statement, line = this.selectedLine();
    if (!statement || !line || this.matchForm.invalid) return;
    const v = this.matchForm.getRawValue();
    await this.reconcileRequest(`/statements/${statement.id}/lines/${line.id}/match`, {
      operationId: crypto.randomUUID(), allocations: v.journalEntryId ? [{ journalEntryId: v.journalEntryId, amount: v.amount }] : [],
      feeAmount: v.feeAmount, feeExpenseAccountId: v.feeExpenseAccountId || null,
    });
    this.matchOpen.set(false);
  }

  async reverseMatch(match: Match): Promise<void> {
    const statement = this.workbench()?.statement; if (!statement) return;
    const reason = window.prompt(this.i18n.t('banks.reconciliation.reverseReason'))?.trim(); if (!reason) return;
    await this.reconcileRequest(`/statements/${statement.id}/matches/${match.id}/reverse`, { operationId: crypto.randomUUID(), reason });
  }

  private async reconcileRequest(path: string, body: object): Promise<void> {
    try {
      this.workbench.set(await firstValueFrom(this.http.post<Workbench>(`/api/v1/finance/bank-reconciliation${path}`, body)));
      await this.load();
    } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
  }

  money(value: number, currency = 'EGP'): string { return new Intl.NumberFormat(this.i18n.locale(), { style: 'currency', currency }).format(value); }
  date(value: number | null): string { return value ? new Intl.DateTimeFormat(this.i18n.locale(), { dateStyle: 'medium' }).format(value) : '—'; }
  reconciliationStatus(status: string): string {
    switch (status) {
      case 'IMPORTED': return this.i18n.t('banks.reconciliation.status.imported');
      case 'IN_PROGRESS': return this.i18n.t('banks.reconciliation.status.inProgress');
      case 'RECONCILED': return this.i18n.t('banks.reconciliation.status.reconciled');
      case 'UNMATCHED': return this.i18n.t('banks.reconciliation.status.unmatched');
      case 'PARTIAL': return this.i18n.t('banks.reconciliation.status.partial');
      case 'MATCHED': return this.i18n.t('banks.reconciliation.status.matched');
      case 'IGNORED': return this.i18n.t('banks.reconciliation.status.ignored');
      default: return status;
    }
  }
  cashTotals(): { currency: string; amount: number }[] {
    return Object.entries(this.cashPosition()?.totalsByCurrency ?? {}).map(([currency, amount]) => ({ currency, amount }));
  }
}
