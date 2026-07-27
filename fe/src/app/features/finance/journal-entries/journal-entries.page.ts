import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { TablePagination } from '../../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../../shared/ui/table-pagination/table-pagination.component';
import { Account } from '../accounts/accounts.page';

export interface JournalEntryLine {
  id?: string;
  accountId: string;
  partyId?: string;
  debit: number;
  credit: number;
  memo?: string;
}

export interface JournalEntry {
  id: string;
  entryNumber: string;
  entryDate: number;
  description: string;
  reference?: string;
  status: 'DRAFT' | 'POSTED' | 'REVERSED';
  fiscalPeriodId?: string;
  postedBy?: string;
  postedAt?: number;
  lines: JournalEntryLine[];
  totalDebit: number;
  totalCredit: number;
  createdAt: number;
  updatedAt: number;
}

export interface JournalEntryPage {
  content: JournalEntry[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

@Component({
  selector: 'app-journal-entries-page',
  imports: [ReactiveFormsModule, TablePaginationComponent, DecimalPipe],
  templateUrl: './journal-entries.page.html',
  styleUrl: './journal-entries.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JournalEntriesPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly entries = signal<JournalEntry[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly totalElements = signal<number>(0);
  readonly pagination = new TablePagination();

  readonly drawerOpen = signal(false);

  readonly entryForm = new FormGroup({
    entryNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    entryDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    reference: new FormControl('', { nonNullable: true }),
  });

  readonly lines = signal<{ accountId: string; debit: number; credit: number; memo: string }[]>([
    { accountId: '', debit: 0, credit: 0, memo: '' },
    { accountId: '', debit: 0, credit: 0, memo: '' },
  ]);

  constructor() {
    void this.load(0);
    void this.loadAccounts();
  }

  async load(pageIndex: number = 0) {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.http.get<JournalEntryPage>('/api/v1/finance/journal-entries', {
          params: { page: pageIndex, size: this.pagination.pageSize() },
        }),
      );
      this.entries.set(res.content);
      this.totalElements.set(res.totalElements);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadAccounts() {
    try {
      const data = await firstValueFrom(this.http.get<Account[]>('/api/v1/finance/accounts'));
      this.accounts.set(data.filter((a) => !a.isHeader));
    } catch {}
  }

  openNew() {
    this.entryForm.reset({
      entryNumber: 'JV-' + Math.floor(1000 + Math.random() * 9000),
      entryDate: new Date().toISOString().substring(0, 10),
      description: '',
      reference: '',
    });
    this.lines.set([
      { accountId: this.accounts()[0]?.id ?? '', debit: 0, credit: 0, memo: '' },
      { accountId: this.accounts()[1]?.id ?? '', debit: 0, credit: 0, memo: '' },
    ]);
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  addLine() {
    this.lines.update((arr) => [...arr, { accountId: this.accounts()[0]?.id ?? '', debit: 0, credit: 0, memo: '' }]);
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
    if (this.entryForm.invalid) return;
    const sumDebit = this.calculateSumDebit();
    const sumCredit = this.calculateSumCredit();

    if (Math.abs(sumDebit - sumCredit) > 0.001) {
      this.error.set(`القيد غير متوازن! مجموع المدين (${sumDebit}) لا يساوي مجموع الدائن (${sumCredit})`);
      return;
    }

    try {
      const formVal = this.entryForm.getRawValue();
      const dateMs = new Date(formVal.entryDate).getTime();
      const payload = {
        entryNumber: formVal.entryNumber,
        entryDate: dateMs,
        description: formVal.description,
        reference: formVal.reference,
        lines: this.lines(),
      };
      await firstValueFrom(this.http.post('/api/v1/finance/journal-entries', payload));
      this.notification.success('تم تسجيل قيد اليومية بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load(0);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  async postEntry(entry: JournalEntry) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/finance/journal-entries/${entry.id}/post`, {}));
      this.notification.success('تم ترحيل قيد اليومية للمحاسبة العامة بنجاح ✓');
      await this.load(0);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
