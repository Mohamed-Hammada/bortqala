import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';

export interface MigrationBatch {
  id: string;
  entityType: string;
  status: string;
  fileName: string;
  totalRecords: number;
  importedRecords: number;
  rejectedRecords: number;
  duplicateRecords: number;
  totalAmount: number;
  glAccountCode: string;
  glBalanceMatch: boolean;
  createdBy: string;
  startedAt: string;
  completedAt?: string;
}

@Component({
  selector: 'app-data-migration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './data-migration.component.html',
  styleUrl: './data-migration.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataMigrationComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly selectedEntityType = signal<string>('CUSTOMERS');
  readonly batches = signal<MigrationBatch[]>([]);
  readonly loading = signal<boolean>(false);
  readonly currentBatch = signal<MigrationBatch | null>(null);
  readonly dryRunResult = signal<any | null>(null);
  readonly message = signal<string>('');

  readonly entityTypes = [
    { key: 'CUSTOMERS', labelAr: 'العملاء', labelEn: 'Customers' },
    { key: 'SUPPLIERS', labelAr: 'الموردين', labelEn: 'Suppliers' },
    { key: 'EMPLOYEES', labelAr: 'الموظفين', labelEn: 'Employees' },
    { key: 'ITEMS', labelAr: 'الأصناف والمخزون', labelEn: 'Items & Inventory' },
    { key: 'WAREHOUSES', labelAr: 'المستودعات', labelEn: 'Warehouses' },
    { key: 'CHART_OF_ACCOUNTS', labelAr: 'دليل الحسابات', labelEn: 'Chart of Accounts' },
    { key: 'PROJECTS', labelAr: 'المشاريع والعقود', labelEn: 'Projects & Contracts' },
    { key: 'BOM', labelAr: 'شجرة المنتج (BOM)', labelEn: 'Bill of Materials (BOM)' },
    { key: 'OPENING_STOCK', labelAr: 'بضاعة أول المدة', labelEn: 'Opening Stock' },
    { key: 'OPENING_AR', labelAr: 'أرصدة عملاء افتتاحية', labelEn: 'Opening AR Invoices' },
    { key: 'OPENING_AP', labelAr: 'أرصدة موردين افتتاحية', labelEn: 'Opening AP Bills' },
    { key: 'BANK_BALANCES', labelAr: 'أرصدة البنوك الافتتاحية', labelEn: 'Bank Balances' },
    { key: 'CASH_BALANCES', labelAr: 'أرصدة الخزائن النقدية', labelEn: 'Cash Balances' },
    { key: 'FIXED_ASSETS', labelAr: 'سجل الأصول الثابتة', labelEn: 'Fixed Assets Register' },
  ];

  ngOnInit(): void {
    this.loadBatches();
  }

  async loadBatches(): Promise<void> {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(this.http.get<MigrationBatch[]>('/api/v1/migration/batches'));
      this.batches.set(data || []);
    } catch {
      // Keep empty
    } finally {
      this.loading.set(false);
    }
  }

  downloadTemplate(): void {
    const type = this.selectedEntityType();
    window.open(`/api/v1/migration/templates/${type}`, '_blank');
  }

  async simulateUpload(): Promise<void> {
    this.loading.set(true);
    try {
      const payload = {
        entityType: this.selectedEntityType(),
        fileName: `${this.selectedEntityType().toLowerCase()}_import.csv`,
        rows: [
          { code: 'MIG-001', nameAr: 'سجل تجريبي 1', outstandingAmount: 25000.00 },
          { code: 'MIG-002', nameAr: 'سجل تجريبي 2', outstandingAmount: 35000.00 }
        ]
      };
      const batch = await firstValueFrom(this.http.post<MigrationBatch>('/api/v1/migration/batches', payload));
      this.currentBatch.set(batch);
      this.message.set(this.i18n.t('common.success') || 'Batch created successfully');
      await this.loadBatches();
    } catch (err: any) {
      this.message.set(err?.message || 'Error creating batch');
    } finally {
      this.loading.set(false);
    }
  }

  async executeValidation(): Promise<void> {
    const batch = this.currentBatch();
    if (!batch) return;
    this.loading.set(true);
    try {
      await firstValueFrom(this.http.post(`/api/v1/migration/batches/${batch.id}/validate`, {}));
      this.message.set(this.i18n.t('common.success') || 'Validation completed');
      await this.loadBatches();
    } catch (err: any) {
      this.message.set(err?.message || 'Validation error');
    } finally {
      this.loading.set(false);
    }
  }

  async executeDryRun(): Promise<void> {
    const batch = this.currentBatch();
    if (!batch) return;
    this.loading.set(true);
    try {
      const dry = await firstValueFrom(this.http.post(`/api/v1/migration/batches/${batch.id}/dry-run`, {}));
      this.dryRunResult.set(dry);
      this.message.set(this.i18n.t('migration.reconciliationPassed') || 'Reconciliation passed');
      await this.loadBatches();
    } catch (err: any) {
      this.message.set(err?.message || 'Dry-run failed');
    } finally {
      this.loading.set(false);
    }
  }

  async executeCommit(): Promise<void> {
    const batch = this.currentBatch();
    if (!batch) return;
    this.loading.set(true);
    try {
      await firstValueFrom(this.http.post(`/api/v1/migration/batches/${batch.id}/commit`, {}));
      this.message.set(this.i18n.t('common.success') || 'Batch committed successfully');
      this.currentBatch.set(null);
      this.dryRunResult.set(null);
      await this.loadBatches();
    } catch (err: any) {
      this.message.set(err?.message || 'Commit failed');
    } finally {
      this.loading.set(false);
    }
  }

  async executeRollback(batchId: string): Promise<void> {
    this.loading.set(true);
    try {
      await firstValueFrom(this.http.post(`/api/v1/migration/batches/${batchId}/rollback`, {}));
      this.message.set(this.i18n.t('common.success') || 'Batch rolled back successfully');
      await this.loadBatches();
    } catch (err: any) {
      this.message.set(err?.message || 'Rollback failed');
    } finally {
      this.loading.set(false);
    }
  }
}
