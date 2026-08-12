import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import {
  EmployeeOption, ItemCategory, NegativeBalance, OperationsSnapshot,
  AccountOption, PartyOption, StockMovement, UnitOfMeasure, ValuationPolicy, ValuationReport,
  CycleCount, ReorderAlert, StockTransfer, WarehouseOption,
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
  readonly transfers = signal<StockTransfer[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  async load(): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try {
      const [snapshot, parties, employees, categories, uoms, negativeBalances, valuation, accounts, reorderAlerts, cycleCounts, warehouses, transfers] = await Promise.all([
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
        firstValueFrom(this.http.get<StockTransfer[]>('/api/v1/operations/transfers')),
      ]);
      this.snapshot.set(this.normalizeSnapshot(snapshot));
      this.parties.set(parties); this.employees.set(employees); this.categories.set(categories);
      this.uoms.set(uoms); this.negativeBalances.set(negativeBalances);
      this.valuation.set(valuation); this.accounts.set(accounts.filter((account) => account.active && !account.isHeader));
      this.reorderAlerts.set(reorderAlerts); this.cycleCounts.set(cycleCounts);
      this.warehouses.set(warehouses);
      this.transfers.set(transfers);
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
  async createTransfer(payload: object): Promise<StockTransfer | null> { return this.transferPost('/api/v1/operations/transfers', payload); }
  async addTransferLine(id: string, payload: object): Promise<StockTransfer | null> { return this.transferPost(`/api/v1/operations/transfers/${id}/lines`, payload); }
  async transitionTransfer(id: string, action: 'ship' | 'receive' | 'cancel'): Promise<boolean> {
    return (await this.transferPost(`/api/v1/operations/transfers/${id}/${action}`, {})) !== null;
  }

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

  private async transferPost(url: string, payload: object): Promise<StockTransfer | null> {
    this.loading.set(true); this.error.set(null);
    try {
      const transfer = await firstValueFrom(this.http.post<StockTransfer>(url, payload));
      this.transfers.update((items) => [transfer, ...items.filter((item) => item.id !== transfer.id)]);
      return transfer;
    } catch (error) { this.error.set(apiErrorMessage(error, this.i18n)); return null; }
    finally { this.loading.set(false); }
  }
}
