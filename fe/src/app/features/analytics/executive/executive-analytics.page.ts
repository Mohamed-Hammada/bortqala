import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import {
  ComparativeTrends,
  CreateSnapshotPayload,
  ExecutiveKpiSnapshot,
  ExecutiveOverview,
  KpiCategory,
  KpiDefinition,
  OwnerCockpitResponse,
  ReconciliationStatus,
  SaveCockpitTargetRequest,
  TrendDirection,
} from './executive-analytics.models';
import { ExecutiveAnalyticsService } from './executive-analytics.service';

@Component({
  selector: 'app-executive-analytics-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalDialogComponent, DecimalPipe, RouterLink],
  templateUrl: './executive-analytics.page.html',
  styleUrl: './executive-analytics.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExecutiveAnalyticsPage implements OnInit {
  protected readonly i18n = inject(I18nService);
  private readonly service = inject(ExecutiveAnalyticsService);
  private readonly notification = inject(NotificationService);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  activeTab = signal<'cockpit' | 'overview' | 'trends' | 'registry' | 'snapshots'>('cockpit');
  loading = signal<boolean>(false);
  cockpitLoading = signal<boolean>(false);
  error = signal<string | null>(null);

  cockpitData = signal<OwnerCockpitResponse | null>(null);
  overview = signal<ExecutiveOverview | null>(null);
  trends = signal<ComparativeTrends | null>(null);
  registry = signal<KpiDefinition[]>([]);
  snapshots = signal<ExecutiveKpiSnapshot[]>([]);
  branches = signal<any[]>([]);

  selectedMonths = signal<number>(6);
  snapshotModalOpen = signal<boolean>(false);
  savingSnapshot = signal<boolean>(false);

  targetModalOpen = signal<boolean>(false);
  savingTarget = signal<boolean>(false);
  exportingExcel = signal<boolean>(false);

  cockpitFilterForm = this.fb.group({
    periodPreset: ['THIS_MONTH'],
    period: [''],
    companyId: [''],
    branchId: [''],
  });

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

  targetForm = this.fb.group({
    periodKey: ['', Validators.required],
    targetRevenue: [1500000, [Validators.required, Validators.min(0)]],
    targetGrossMarginPercent: [35.0, [Validators.required, Validators.min(0), Validators.max(100)]],
    targetMaxOpex: [250000, [Validators.required, Validators.min(0)]],
    targetMinLiquidity: [300000, [Validators.required, Validators.min(0)]],
    targetMaxOverdueAr: [50000, [Validators.required, Validators.min(0)]],
    notes: [''],
  });

  ngOnInit(): void {
    const now = new Date();
    const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    this.cockpitFilterForm.patchValue({ period: currentMonth });
    void this.loadCockpit();
    void this.loadBranches();
  }

  async loadBranches(): Promise<void> {
    try {
      const list = await firstValueFrom(this.http.get<any[]>('/api/v1/organization/branches'));
      this.branches.set(list || []);
    } catch {
      this.branches.set([]);
    }
  }

  async loadCockpit(): Promise<void> {
    this.cockpitLoading.set(true);
    this.error.set(null);
    try {
      const val = this.cockpitFilterForm.value;
      const data = await firstValueFrom(
        this.service.getCockpit(
          val.period || undefined,
          val.companyId || undefined,
          val.branchId || undefined
        )
      );
      this.cockpitData.set(data);
    } catch (err) {
      this.error.set(apiErrorMessage(err, this.i18n));
    } finally {
      this.cockpitLoading.set(false);
    }
  }

  onPeriodPresetChange(preset: string): void {
    const now = new Date();
    let periodVal = '';
    if (preset === 'TODAY') {
      periodVal = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    } else if (preset === 'THIS_MONTH') {
      periodVal = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    } else if (preset === 'THIS_QUARTER') {
      const q = Math.floor(now.getMonth() / 3) + 1;
      periodVal = `${now.getFullYear()}-Q${q}`;
    } else if (preset === 'THIS_YEAR') {
      periodVal = `${now.getFullYear()}`;
    }
    this.cockpitFilterForm.patchValue({ periodPreset: preset, period: periodVal });
    void this.loadCockpit();
  }

  setTab(tab: 'cockpit' | 'overview' | 'trends' | 'registry' | 'snapshots'): void {
    this.activeTab.set(tab);
    if (tab === 'cockpit' && !this.cockpitData()) {
      void this.loadCockpit();
    } else if (tab === 'overview' && !this.overview()) {
      void this.loadAll();
    }
  }

  openTargetModal(): void {
    const targets = this.cockpitData()?.targets;
    const currentPeriod = this.cockpitFilterForm.value.period || '2026-Q3';
    this.targetForm.patchValue({
      periodKey: targets?.periodKey || currentPeriod,
      targetRevenue: targets?.targetRevenue ?? 1500000,
      targetGrossMarginPercent: targets?.targetGrossMarginPercent ?? 35,
      targetMaxOpex: targets?.targetMaxOpex ?? 250000,
      targetMinLiquidity: targets?.targetMinLiquidity ?? 300000,
      targetMaxOverdueAr: targets?.targetMaxOverdueAr ?? 50000,
      notes: targets?.notes || '',
    });
    this.targetModalOpen.set(true);
  }

  closeTargetModal(): void {
    this.targetModalOpen.set(false);
  }

  async submitTarget(): Promise<void> {
    if (this.targetForm.invalid) return;
    this.savingTarget.set(true);
    try {
      const val = this.targetForm.getRawValue();
      const payload: SaveCockpitTargetRequest = {
        periodKey: val.periodKey!,
        targetRevenue: Number(val.targetRevenue),
        targetGrossMarginPercent: Number(val.targetGrossMarginPercent),
        targetMaxOpex: Number(val.targetMaxOpex),
        targetMinLiquidity: Number(val.targetMinLiquidity),
        targetMaxOverdueAr: Number(val.targetMaxOverdueAr),
        notes: val.notes || '',
      };
      await firstValueFrom(this.service.saveTargets(payload));
      this.notification.success(this.i18n.t('executive.saveTargetSuccess'));
      this.closeTargetModal();
      await this.loadCockpit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.savingTarget.set(false);
    }
  }

  async exportCockpitExcel(): Promise<void> {
    this.exportingExcel.set(true);
    try {
      const formVal = this.cockpitFilterForm.value;
      const blob = await firstValueFrom(
        this.service.exportCockpitExcel(
          formVal.period || undefined,
          formVal.companyId || undefined,
          formVal.branchId || undefined
        )
      );
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const p = formVal.period ? formVal.period : 'ALL';
      a.download = `Executive_Cockpit_${p}.xlsx`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.exportingExcel.set(false);
    }
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

