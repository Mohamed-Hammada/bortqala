import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import {
  ComparativeTrends,
  CreateSnapshotPayload,
  ExecutiveKpiSnapshot,
  ExecutiveOverview,
  KpiCategory,
  KpiDefinition,
  ReconciliationStatus,
  TrendDirection,
} from './executive-analytics.models';
import { ExecutiveAnalyticsService } from './executive-analytics.service';

@Component({
  selector: 'app-executive-analytics-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalDialogComponent, DecimalPipe],
  templateUrl: './executive-analytics.page.html',
  styleUrl: './executive-analytics.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutiveAnalyticsPage implements OnInit {
  protected readonly i18n = inject(I18nService);
  private readonly service = inject(ExecutiveAnalyticsService);
  private readonly fb = inject(FormBuilder);

  activeTab = signal<'overview' | 'trends' | 'registry' | 'snapshots'>('overview');
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  overview = signal<ExecutiveOverview | null>(null);
  trends = signal<ComparativeTrends | null>(null);
  registry = signal<KpiDefinition[]>([]);
  snapshots = signal<ExecutiveKpiSnapshot[]>([]);

  selectedMonths = signal<number>(6);
  snapshotModalOpen = signal<boolean>(false);
  savingSnapshot = signal<boolean>(false);

  filterForm = this.fb.group({
    period: [''],
    companyId: [''],
    branchId: [''],
    projectId: [''],
  });

  snapshotForm = this.fb.group({
    periodKey: ['', Validators.required],
    category: ['FINANCIAL' as KpiCategory, Validators.required],
    kpiKey: ['', Validators.required],
    targetValue: [0],
    actualValue: [0, Validators.required],
    varianceValue: [0],
    variancePercent: [0],
    trendDirection: ['STABLE' as TrendDirection, Validators.required],
    reconciliationStatus: ['RECONCILED' as ReconciliationStatus, Validators.required],
    drilldownUrl: [''],
    metadataJson: [''],
  });

  ngOnInit(): void {
    void this.loadAll();
  }

  async loadAll(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const formVal = this.filterForm.value;
      const [overviewData, trendsData, registryData, snapshotsData] = await Promise.all([
        firstValueFrom(this.service.getOverview(formVal.period || undefined, formVal.companyId || undefined, formVal.branchId || undefined, formVal.projectId || undefined)),
        firstValueFrom(this.service.getTrends(this.selectedMonths())),
        firstValueFrom(this.service.getKpiRegistry()),
        firstValueFrom(this.service.getSnapshots()),
      ]);
      this.overview.set(overviewData);
      this.trends.set(trendsData);
      this.registry.set(registryData);
      this.snapshots.set(snapshotsData);
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async changeMonths(months: number): Promise<void> {
    this.selectedMonths.set(months);
    try {
      const data = await firstValueFrom(this.service.getTrends(months));
      this.trends.set(data);
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
    }
  }

  openSnapshotModal(): void {
    this.snapshotForm.reset({
      periodKey: this.overview()?.period || '2026-Q3',
      category: 'FINANCIAL',
      kpiKey: 'NET_PROFIT_MARGIN',
      targetValue: 25.0,
      actualValue: this.overview()?.netMarginPercent || 28.5,
      varianceValue: 3.5,
      variancePercent: 14.0,
      trendDirection: 'UP',
      reconciliationStatus: 'RECONCILED',
      drilldownUrl: '/finance/accounts',
      metadataJson: '{}',
    });
    this.snapshotModalOpen.set(true);
  }

  closeSnapshotModal(): void {
    this.snapshotModalOpen.set(false);
  }

  async submitSnapshot(): Promise<void> {
    if (this.snapshotForm.invalid) return;
    this.savingSnapshot.set(true);
    try {
      const val = this.snapshotForm.getRawValue();
      const payload: CreateSnapshotPayload = {
        periodKey: val.periodKey!,
        category: val.category!,
        kpiKey: val.kpiKey!,
        targetValue: val.targetValue || undefined,
        actualValue: val.actualValue!,
        varianceValue: val.varianceValue || undefined,
        variancePercent: val.variancePercent || undefined,
        trendDirection: val.trendDirection!,
        reconciliationStatus: val.reconciliationStatus!,
        drilldownUrl: val.drilldownUrl || undefined,
        metadataJson: val.metadataJson || undefined,
      };
      await firstValueFrom(this.service.recordSnapshot(payload));
      this.closeSnapshotModal();
      const snapshots = await firstValueFrom(this.service.getSnapshots());
      this.snapshots.set(snapshots);
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
    } finally {
      this.savingSnapshot.set(false);
    }
  }

  getCategoryLabel(category: KpiCategory): string {
    switch (category) {
      case 'FINANCIAL': return this.i18n.t('analytics.catFinancial');
      case 'COMMERCIAL': return this.i18n.t('analytics.catCommercial');
      case 'OPERATIONS': return this.i18n.t('analytics.catOperations');
      case 'PROJECTS': return this.i18n.t('analytics.catProjects');
      case 'WORKFORCE': return this.i18n.t('analytics.catWorkforce');
      case 'COMPLIANCE': return this.i18n.t('analytics.catCompliance');
      default: return category;
    }
  }

  getReconciliationLabel(status: ReconciliationStatus): string {
    switch (status) {
      case 'RECONCILED': return this.i18n.t('analytics.reconciled');
      case 'PENDING_REVIEW': return this.i18n.t('analytics.pendingReview');
      case 'DISCREPANCY': return this.i18n.t('analytics.discrepancy');
      default: return status;
    }
  }

  getTrendIcon(trend: TrendDirection): string {
    switch (trend) {
      case 'UP': return '↑';
      case 'DOWN': return '↓';
      case 'STABLE': return '→';
      default: return '—';
    }
  }
}
