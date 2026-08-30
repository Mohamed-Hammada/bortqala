import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';

export interface ReconciliationDomainSummary {
  domainKey: string;
  subledgerType: string;
  subledgerBalance: number;
  glBalance: number;
  varianceAmount: number;
  isBalanced: boolean;
  discrepancyCount: number;
  status: string;
}

export interface DiscrepancyDetailItem {
  documentId: string;
  documentNumber: string;
  subledgerAmount: number;
  glAmount: number;
  variance: number;
  discrepancyReason: string;
}

@Component({
  selector: 'app-reconciliation-center',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reconciliation-center.component.html',
  styleUrl: './reconciliation-center.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReconciliationCenterComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly summaries = signal<ReconciliationDomainSummary[]>([]);
  readonly loading = signal<boolean>(false);
  readonly selectedDomain = signal<ReconciliationDomainSummary | null>(null);
  readonly discrepancies = signal<DiscrepancyDetailItem[]>([]);
  readonly drilldownLoading = signal<boolean>(false);

  ngOnInit(): void {
    this.loadOverview();
  }

  async loadOverview(): Promise<void> {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.get<ReconciliationDomainSummary[]>('/api/v1/finance/reconciliation-center/overview')
      );
      this.summaries.set(data || []);
    } catch {
      // Keep previous
    } finally {
      this.loading.set(false);
    }
  }

  async openDrilldown(domain: ReconciliationDomainSummary): Promise<void> {
    this.selectedDomain.set(domain);
    this.drilldownLoading.set(true);
    try {
      const items = await firstValueFrom(
        this.http.get<DiscrepancyDetailItem[]>(
          `/api/v1/finance/reconciliation-center/drilldown?subledgerType=${domain.subledgerType}`
        )
      );
      this.discrepancies.set(items || []);
    } catch {
      this.discrepancies.set([]);
    } finally {
      this.drilldownLoading.set(false);
    }
  }

  closeDrilldown(): void {
    this.selectedDomain.set(null);
    this.discrepancies.set([]);
  }

  getDomainLabel(domainKey: string): string {
    const keyMap: Record<string, string> = {
      inventory: 'reconciliation.inventory',
      ar: 'reconciliation.ar',
      ap: 'reconciliation.ap',
      cash: 'reconciliation.cash',
      bank: 'reconciliation.bank',
      payroll: 'reconciliation.payroll',
      project_cost: 'reconciliation.projectCost',
      manufacturing_cost: 'reconciliation.manufacturingCost',
      treasury: 'reconciliation.bank',
    };
    const i18nKey = keyMap[domainKey.toLowerCase()] || `reconciliation.${domainKey}`;
    return this.i18n.t(i18nKey);
  }
}
