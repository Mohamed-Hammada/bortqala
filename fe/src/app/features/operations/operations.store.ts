import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import {
  EmployeeOption, ItemCategory, NegativeBalance, OperationsSnapshot,
  AccountOption, PartyOption, StockMovement, UnitOfMeasure, ValuationPolicy, ValuationReport,
  CycleCount, ReorderAlert, WarehouseOption,
} from './operations.models';
import { downloadBlob, timestampedExcelFileName } from '../../core/download';
import { I18nService } from '../../core/i18n.service';

const empty: OperationsSnapshot = {
  items: [], movements: [], partyBalances: [], ledgerEntries: [], employeeAdvances: [],
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
  readonly accounts = signal<AccountOption[]>([]);
  readonly valuation = signal<ValuationReport | null>(null);
  readonly reorderAlerts = signal<ReorderAlert[]>([]);
  readonly cycleCounts = signal<CycleCount[]>([]);
  readonly warehouses = signal<WarehouseOption[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try {
      const [snapshot, parties, employees, categories, uoms, negativeBalances, valuation, accounts, reorderAlerts, cycleCounts, warehouses] = await Promise.all([
        firstValueFrom(this.http.get<OperationsSnapshot>('/api/v1/operations')),
        firstValueFrom(this.http.get<PartyOption[]>('/api/v1/parties')),
        firstValueFrom(this.http.get<EmployeeOption[]>('/api/v1/employees')),
        firstValueFrom(this.http.get<ItemCategory[]>('/api/v1/operations/item-categories')),
        firstValueFrom(this.http.get<UnitOfMeasure[]>('/api/v1/operations/uoms')),
        firstValueFrom(this.http.get<NegativeBalance[]>('/api/v1/operations/negative-balances')),
        firstValueFrom(this.http.get<ValuationReport>('/api/v1/operations/valuation/report')),
        firstValueFrom(this.http.get<AccountOption[]>('/api/v1/finance/accounts')),
        firstValueFrom(this.http.get<ReorderAlert[]>('/api/v1/operations/reorder-alerts')),
        firstValueFrom(this.http.get<CycleCount[]>('/api/v1/operations/cycle-counts')),
        firstValueFrom(this.http.get<WarehouseOption[]>('/api/v1/inventory/warehouses')),
      ]);
      this.snapshot.set(this.normalizeSnapshot(snapshot));
      this.parties.set(parties); this.employees.set(employees); this.categories.set(categories);
      this.uoms.set(uoms); this.negativeBalances.set(negativeBalances);
      this.valuation.set(valuation); this.accounts.set(accounts.filter((account) => account.active && !account.isHeader));
      this.reorderAlerts.set(reorderAlerts); this.cycleCounts.set(cycleCounts);
      this.warehouses.set(warehouses);
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
    finally { this.loading.set(false); }
  }

  async createItem(payload: object): Promise<boolean> { return this.post('/api/v1/operations/items', payload, false); }
  async transaction(payload: object): Promise<boolean> { return this.post('/api/v1/operations/transactions', payload, true); }
  async advance(payload: object): Promise<boolean> { return this.post('/api/v1/operations/advances', payload, true); }
  async adjustment(payload: object): Promise<boolean> { return this.post('/api/v1/operations/adjustments', payload, true); }
  async createCategory(payload: object): Promise<boolean> { return this.post('/api/v1/operations/item-categories', payload, false); }
  async createUom(payload: object): Promise<boolean> { return this.post('/api/v1/operations/uoms', payload, false); }
  async saveValuationPolicy(payload: object): Promise<boolean> {
    this.loading.set(true); this.error.set(null);
    try {
      const policy = await firstValueFrom(this.http.put<ValuationPolicy>('/api/v1/operations/valuation/settings', payload));
      const report = this.valuation();
      if (report) this.valuation.set({ ...report, policy });
      await this.load();
      return true;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
    finally { this.loading.set(false); }
  }
  async revalue(payload: object): Promise<boolean> { return this.post('/api/v1/operations/valuation/revaluations', payload, false); }
  async recordCycleCount(payload: object): Promise<boolean> { return this.post('/api/v1/operations/cycle-counts/reconcile', payload, false); }

  async export(): Promise<void> {
    try {
      downloadBlob(await firstValueFrom(this.http.get('/api/v1/operations/export.xlsx', { responseType: 'blob' })),
        timestampedExcelFileName('المخزون-والحسابات', 'inventory-and-ledgers', this.i18n.locale()));
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); }
  }

  private normalizeSnapshot(snapshot: OperationsSnapshot): OperationsSnapshot {
    return {
      ...snapshot,
      movements: snapshot.movements.map((row) => ({
        ...row,
        referenceCode: row.referenceCode?.trim() || this.internalMovementReference(row),
      })),
    };
  }

  private internalMovementReference(row: StockMovement): string {
    const compactId = (row.id || '').replaceAll('-', '').slice(0, 10).toUpperCase();
    return `MOV-${compactId || 'UNASSIGNED'}`;
  }

  private async post(url: string, payload: object, returnsSnapshot: boolean): Promise<boolean> {
    this.loading.set(true); this.error.set(null);
    try {
      const result = await firstValueFrom(this.http.post<OperationsSnapshot>(url, payload));
      if (returnsSnapshot) this.snapshot.set(this.normalizeSnapshot(result));
      else await this.load();
      return true;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return false; }
    finally { this.loading.set(false); }
  }
}
