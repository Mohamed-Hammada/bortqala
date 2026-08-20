import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { SampleTemplateService } from '../../../core/sample-template.service';
import { formatDate } from '../../../core/date';

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

export interface Cashbox {
  id: string;
  code: string;
  name: string;
  branchId?: string;
  currency: string;
  custodianUserId?: string;
  glAccountId?: string;
  currentBalance: number;
  active: boolean;
  createdAt: number;
}

export interface CashboxTransaction {
  id: string;
  cashboxId: string;
  transactionType: 'RECEIPT' | 'PAYMENT' | 'PETTY_CASH_ADVANCE' | 'PETTY_CASH_SETTLEMENT' | 'PHYSICAL_COUNT_ADJUSTMENT';
  amount: number;
  voucherNumber?: string;
  counterpartyPartyId?: string;
  description?: string;
  status: string;
  transactionDate: number;
}

export interface CommercialCheque {
  id: string;
  chequeNumber: string;
  chequeType: 'RECEIVED' | 'ISSUED';
  bankName?: string;
  bankAccountId?: string;
  drawerPayeeName: string;
  partyId?: string;
  amount: number;
  currency: string;
  issueDate: number;
  dueDate: number;
  status: 'RECEIVED' | 'ISSUED' | 'DEPOSITED' | 'COLLECTED' | 'BOUNCED' | 'CANCELLED';
  bounceReason?: string;
  notes?: string;
  createdAt: number;
}

export interface UnifiedLiquiditySummary {
  totalBankBalance: number;
  totalCashBalance: number;
  chequesUnderCollection: number;
  chequesIssuedOutstanding: number;
  netLiquidityPosition: number;
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
  imports: [ReactiveFormsModule, ModalDialogComponent, DecimalPipe],
  templateUrl: './banks.page.html',
  styleUrl: './banks.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BanksPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly sampleTemplates = inject(SampleTemplateService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly banks = signal<BankAccount[]>([]);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly tab = signal<'accounts' | 'reconciliation' | 'cash' | 'cashboxes' | 'cheques' | 'liquidity'>('accounts');
  readonly accounts = signal<Account[]>([]);
  readonly statements = signal<Statement[]>([]);
  readonly workbench = signal<Workbench | null>(null);
  readonly cashPosition = signal<CashPosition | null>(null);
  readonly importOpen = signal(false);
  readonly matchOpen = signal(false);
  readonly importFile = signal<File | null>(null);
  readonly selectedLine = signal<StatementLine | null>(null);

  // Cashbox & Cheques signals
  readonly cashboxes = signal<Cashbox[]>([]);
  readonly selectedCashbox = signal<Cashbox | null>(null);
  readonly cashboxTransactions = signal<CashboxTransaction[]>([]);
  readonly cheques = signal<CommercialCheque[]>([]);
  readonly liquiditySummary = signal<UnifiedLiquiditySummary | null>(null);

  readonly cashboxModalOpen = signal(false);
  readonly cashTxModalOpen = signal(false);
  readonly chequeModalOpen = signal(false);
  readonly chequeDepositModalOpen = signal(false);
  readonly selectedCheque = signal<CommercialCheque | null>(null);

  readonly form = new FormGroup({
    bankName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    accountNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    iban: new FormControl('', { nonNullable: true }),
    swiftCode: new FormControl('', { nonNullable: true }),
    accountId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    currencyCode: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  readonly cashboxForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    branchId: new FormControl('', { nonNullable: true }),
    currency: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    custodianUserId: new FormControl('', { nonNullable: true }),
    glAccountId: new FormControl('', { nonNullable: true }),
  });

  readonly cashTxForm = new FormGroup({
    transactionType: new FormControl<'RECEIPT' | 'PAYMENT' | 'PETTY_CASH_ADVANCE' | 'PETTY_CASH_SETTLEMENT' | 'PHYSICAL_COUNT_ADJUSTMENT'>('RECEIPT', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0.01)] }),
    voucherNumber: new FormControl('', { nonNullable: true }),
    counterpartyPartyId: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  readonly chequeForm = new FormGroup({
    chequeNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    chequeType: new FormControl<'RECEIVED' | 'ISSUED'>('RECEIVED', { nonNullable: true, validators: [Validators.required] }),
    bankName: new FormControl('', { nonNullable: true }),
    bankAccountId: new FormControl('', { nonNullable: true }),
    drawerPayeeName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    partyId: new FormControl('', { nonNullable: true }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.min(0.01)] }),
    currency: new FormControl('EGP', { nonNullable: true, validators: [Validators.required] }),
    issueDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    dueDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    notes: new FormControl('', { nonNullable: true }),
  });

  readonly depositForm = new FormGroup({
    targetBankAccountId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
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
      const [banks, accounts, statements, cash, cashboxes, cheques, liquidity] = await Promise.all([
        firstValueFrom(this.http.get<BankAccount[]>('/api/v1/finance/banks')),
        firstValueFrom(this.http.get<Account[]>('/api/v1/finance/accounts')),
        firstValueFrom(this.http.get<Statement[]>('/api/v1/finance/bank-reconciliation/statements')),
        firstValueFrom(this.http.get<CashPosition>('/api/v1/finance/bank-reconciliation/cash-position')),
        firstValueFrom(this.http.get<Cashbox[]>('/api/v1/finance/treasury/cashboxes')).catch((): Cashbox[] => []),
        firstValueFrom(this.http.get<CommercialCheque[]>('/api/v1/finance/treasury/cheques')).catch((): CommercialCheque[] => []),
        firstValueFrom(this.http.get<UnifiedLiquiditySummary>('/api/v1/finance/treasury/liquidity-summary')).catch((): UnifiedLiquiditySummary | null => null),
      ]);
      this.banks.set(banks);
      this.accounts.set(accounts);
      this.statements.set(statements);
      this.cashPosition.set(cash);
      this.cashboxes.set(cashboxes);
      this.cheques.set(cheques);
      this.liquiditySummary.set(liquidity);
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

  openNewCashbox() {
    this.cashboxForm.reset({ code: '', name: '', branchId: '', currency: 'EGP', custodianUserId: '', glAccountId: '' });
    this.cashboxModalOpen.set(true);
  }

  async submitCashbox() {
    if (this.cashboxForm.invalid) return;
    try {
      await firstValueFrom(this.http.post('/api/v1/finance/treasury/cashboxes', this.cashboxForm.getRawValue()));
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.cashboxModalOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async selectCashbox(cb: Cashbox) {
    this.selectedCashbox.set(cb);
    try {
      const txs = await firstValueFrom(this.http.get<CashboxTransaction[]>(`/api/v1/finance/treasury/cashboxes/${cb.id}/transactions`));
      this.cashboxTransactions.set(txs);
    } catch {
      this.cashboxTransactions.set([]);
    }
  }

  openCashTx(type: 'RECEIPT' | 'PAYMENT' | 'PETTY_CASH_ADVANCE' | 'PETTY_CASH_SETTLEMENT' | 'PHYSICAL_COUNT_ADJUSTMENT' = 'RECEIPT') {
    this.cashTxForm.reset({ transactionType: type, amount: 0, voucherNumber: '', counterpartyPartyId: '', description: '' });
    this.cashTxModalOpen.set(true);
  }

  async submitCashTx() {
    const cb = this.selectedCashbox();
    if (!cb || this.cashTxForm.invalid) return;
    try {
      const val = this.cashTxForm.getRawValue();
      await firstValueFrom(this.http.post(`/api/v1/finance/treasury/cashboxes/${cb.id}/transactions`, {
        ...val,
        transactionDate: Date.now(),
      }));
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.cashTxModalOpen.set(false);
      await this.load();
      await this.selectCashbox(cb);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  openNewCheque() {
    const today = new Date().toISOString().substring(0, 10);
    this.chequeForm.reset({
      chequeNumber: '',
      chequeType: 'RECEIVED',
      bankName: '',
      bankAccountId: '',
      drawerPayeeName: '',
      partyId: '',
      amount: 0,
      currency: 'EGP',
      issueDate: today,
      dueDate: today,
      notes: '',
    });
    this.chequeModalOpen.set(true);
  }

  async submitCheque() {
    if (this.chequeForm.invalid) return;
    try {
      const val = this.chequeForm.getRawValue();
      await firstValueFrom(this.http.post('/api/v1/finance/treasury/cheques', {
        ...val,
        issueDate: new Date(val.issueDate).getTime(),
        dueDate: new Date(val.dueDate).getTime(),
      }));
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.chequeModalOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  openDepositCheque(chq: CommercialCheque) {
    this.selectedCheque.set(chq);
    this.depositForm.reset({ targetBankAccountId: this.banks()[0]?.id ?? '' });
    this.chequeDepositModalOpen.set(true);
  }

  async submitDepositCheque() {
    const chq = this.selectedCheque();
    if (!chq || this.depositForm.invalid) return;
    try {
      await firstValueFrom(this.http.post(`/api/v1/finance/treasury/cheques/${chq.id}/deposit`, this.depositForm.getRawValue()));
      this.notification.success(this.i18n.t('treasury.deposit') + ' ✓');
      this.chequeDepositModalOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async collectCheque(chq: CommercialCheque) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/finance/treasury/cheques/${chq.id}/collect`, {}));
      this.notification.success(this.i18n.t('treasury.collect') + ' ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async bounceCheque(chq: CommercialCheque) {
    const reason = window.prompt(this.i18n.t('treasury.bounceReason')) || this.i18n.t('treasury.bounceReasonDefault');
    try {
      await firstValueFrom(this.http.post(`/api/v1/finance/treasury/cheques/${chq.id}/bounce`, { reason }));
      this.notification.success(this.i18n.t('treasury.bounce') + ' ✓');
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async cancelCheque(chq: CommercialCheque) {
    const reason = window.prompt(this.i18n.t('treasury.cancel')) || this.i18n.t('treasury.cancelDefault');
    try {
      await firstValueFrom(this.http.post(`/api/v1/finance/treasury/cheques/${chq.id}/cancel`, { reason }));
      this.notification.success(this.i18n.t('treasury.cancel') + ' ✓');
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

  downloadStatementTemplate(): void {
    void this.sampleTemplates.bankStatement()
      .then(() => this.notification.success(this.i18n.t('imports.templateDownloadSuccess')))
      .catch(e => this.error.set(apiErrorMessage(e, this.i18n)));
  }

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
      bankFeeAmount: v.feeAmount, bankFeeExpenseAccountId: v.feeExpenseAccountId || null,
    });
    this.matchOpen.set(false);
  }

  async unmatch(match: Match): Promise<void> {
    const statement = this.workbench()?.statement; if (!statement) return;
    await this.reconcileRequest(`/statements/${statement.id}/matches/${match.id}/unmatch`, { operationId: crypto.randomUUID() });
  }

  async postDifference(): Promise<void> {
    const statement = this.workbench()?.statement; if (!statement) return;
    await this.reconcileRequest(`/statements/${statement.id}/post-difference`, {
      operationId: crypto.randomUUID(), differenceAccountId: this.accounts().find(a => a.code === '5800')?.id ?? this.accounts()[0]?.id,
    });
  }

  private async reconcileRequest(path: string, payload: unknown): Promise<void> {
    try {
      this.workbench.set(await firstValueFrom(this.http.post<Workbench>(`/api/v1/finance/bank-reconciliation${path}`, payload)));
      await this.load();
    } catch (e) { this.error.set(apiErrorMessage(e, this.i18n)); }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
