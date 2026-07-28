import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import {
  EmployeeOption,
  ItemCategory,
  NegativeBalance,
  OperationsSnapshot,
  PartyOption,
  UnitOfMeasure,
} from './operations.models';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';

const empty: OperationsSnapshot = {
  items: [],
  movements: [],
  partyBalances: [],
  ledgerEntries: [],
  employeeAdvances: [],
};

@Injectable()
export class OperationsStore {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);
  readonly snapshot = signal<OperationsSnapshot>(empty);
  readonly parties = signal<PartyOption[]>([]);
  readonly employees = signal<EmployeeOption[]>([]);
  readonly categories = signal<ItemCategory[]>([]);
  readonly uoms = signal<UnitOfMeasure[]>([]);
  readonly negativeBalances = signal<NegativeBalance[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [snapshot, parties, employees, categories, uoms, negativeBalances] = await Promise.all([
        firstValueFrom(this.http.get<OperationsSnapshot>('/api/v1/operations')),
        firstValueFrom(this.http.get<PartyOption[]>('/api/v1/parties')),
        firstValueFrom(this.http.get<EmployeeOption[]>('/api/v1/employees')),
        firstValueFrom(this.http.get<ItemCategory[]>('/api/v1/operations/item-categories')),
        firstValueFrom(this.http.get<UnitOfMeasure[]>('/api/v1/operations/uoms')),
        firstValueFrom(this.http.get<NegativeBalance[]>('/api/v1/operations/negative-balances')),
      ]);
      this.snapshot.set(snapshot);
      this.parties.set(parties);
      this.employees.set(employees);
      this.categories.set(categories);
      this.uoms.set(uoms);
      this.negativeBalances.set(negativeBalances);
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async createItem(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/items', payload, false);
  }
  async transaction(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/transactions', payload, true);
  }
  async advance(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/advances', payload, true);
  }
  async adjustment(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/adjustments', payload, true);
  }
  async createCategory(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/item-categories', payload, false);
  }
  async createUom(payload: object): Promise<boolean> {
    return this.post('/api/v1/operations/uoms', payload, false);
  }
  async export(): Promise<void> {
    try {
      downloadBlob(
        await firstValueFrom(
          this.http.get('/api/v1/operations/export.xlsx', { responseType: 'blob' }),
        ),
        timestampedExcelFileName('المخزون-والحسابات', 'inventory-and-ledgers', this.i18n.locale()),
      );
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  private async post(url: string, payload: object, returnsSnapshot: boolean): Promise<boolean> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.http.post<OperationsSnapshot>(url, payload));
      if (returnsSnapshot) this.snapshot.set(result);
      else await this.load();
      return true;
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
      return false;
    } finally {
      this.loading.set(false);
    }
  }
}
