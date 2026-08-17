import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { apiErrorDetail } from '../../../../core/api-error';
import { I18nService } from '../../../../core/i18n.service';
import { WorkforceService } from '../../data-access/workforce.service';

@Component({
  selector: 'app-workforce-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class WorkforceDashboardComponent implements OnInit {
  workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  loading = signal(true);
  loadError = signal<string | null>(null);
  animationsEnabled = computed(() => this.auth.preferences().dashboardAnimationsEnabled);
  selectedContractorId = signal('');
  selectedCategoryId = signal('');
  selectedLocationId = signal('');
  selectedStatus = signal('');
  filteredWorkers = computed(() => this.workforceService.workers().filter(worker =>
    (!this.selectedContractorId() || worker.contractorId === this.selectedContractorId())
    && (!this.selectedCategoryId() || worker.categoryId === this.selectedCategoryId())
    && (!this.selectedLocationId() || worker.branchId === this.selectedLocationId())
    && (!this.selectedStatus() || worker.status === this.selectedStatus())));
  filteredContractors = computed(() => this.workforceService.contractors().filter(item =>
    !this.selectedContractorId() || item.id === this.selectedContractorId()));
  filteredContractorCount = computed(() => new Set(this.filteredWorkers().map(worker => worker.contractorId)).size);
  locationOptions = computed(() => [...new Set(this.workforceService.workers().map(worker => worker.branchId).filter((value): value is string => !!value))]);
  filterQueryParams = computed(() => ({ contractorId: this.selectedContractorId() || null, categoryId: this.selectedCategoryId() || null, branchId: this.selectedLocationId() || null, status: this.selectedStatus() || null }));
  contractorSeries = computed(() => this.breakdown(this.workforceService.contractors().map(item => ({ id: item.id, label: item.name })), worker => worker.contractorId));
  categorySeries = computed(() => this.breakdown(this.workforceService.categories().map(item => ({ id: item.id, label: item.name })), worker => worker.categoryId));
  requestedWorkers = computed(() => this.workforceService.laborRequests().flatMap(request => request.items ?? []).reduce((sum, item) => sum + item.requestedCount, 0));
  acceptedWorkers = computed(() => this.workforceService.laborRequests().flatMap(request => request.items ?? []).reduce((sum, item) => sum + item.acceptedCount, 0));
  requestRemaining = computed(() => Math.max(0, this.requestedWorkers() - this.acceptedWorkers()));
  requestCoverage = computed(() => this.requestedWorkers() > 0 ? Math.min(100, Math.round(this.acceptedWorkers() * 100 / this.requestedWorkers())) : 0);
  requestCoverageStyle = computed(() => `conic-gradient(#16a34a 0 ${this.requestCoverage()}%, #e2e8f0 ${this.requestCoverage()}% 100%)`);
  advanceGranted = computed(() => this.workforceService.advances().reduce((sum, item) => sum + Number(item.amount || 0), 0));
  advanceRemaining = computed(() => this.workforceService.advances().reduce((sum, item) => sum + Number(item.remainingBalance || 0), 0));
  advancePaid = computed(() => Math.max(0, this.advanceGranted() - this.advanceRemaining()));
  advanceRemainingPercent = computed(() => this.advanceGranted() > 0 ? Math.min(100, this.advanceRemaining() * 100 / this.advanceGranted()) : 0);

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.selectedContractorId.set(params['contractorId'] ?? '');
      this.selectedCategoryId.set(params['categoryId'] ?? '');
      this.selectedLocationId.set(params['locationId'] ?? '');
      this.selectedStatus.set(params['status'] ?? '');
    });
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      contractors: this.workforceService.loadContractors(),
      workers: this.workforceService.loadWorkers(),
      categories: this.workforceService.loadCategories(),
      requests: this.workforceService.loadLaborRequests(),
      advances: this.workforceService.loadAdvances(),
    }).subscribe({
      next: () => this.loading.set(false),
      error: (error) => { this.loadError.set(this.i18n.t('workforce.ui.dashboard.loadFailed', { detail: apiErrorDetail(error, error?.message ?? this.i18n.t('workforce.ui.unexpectedError')) })); this.loading.set(false); },
    });
  }

  persistFilters(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: {
      contractorId: this.selectedContractorId() || null, categoryId: this.selectedCategoryId() || null,
      locationId: this.selectedLocationId() || null, status: this.selectedStatus() || null,
    }, queryParamsHandling: 'merge' });
  }

  clearFilters(): void {
    this.selectedContractorId.set(''); this.selectedCategoryId.set(''); this.selectedLocationId.set(''); this.selectedStatus.set('');
    this.persistFilters();
  }

  getModelLabel(model: string): string { const keys:Record<string,string>={worker_net_total:'workforce.ui.model.workerNetTotal',contractor_daily_rate:'workforce.ui.model.contractorDailyRate',worker_cost_plus_fee:'workforce.ui.model.workerCostPlusFee',fixed_period_amount:'workforce.ui.model.fixedPeriodAmount'}; return keys[model]?this.i18n.t(keys[model]):model; }
  contractorStatusLabel(status:string):string { const key=status==='ACTIVE'?'workforce.ui.dashboard.statusActive':status==='INACTIVE'?'workforce.ui.dashboard.statusInactive':''; return key?this.i18n.t(key):status; }

  private breakdown(labels: Array<{ id: string; label: string }>, selector: (worker: { contractorId: string; categoryId: string }) => string) {
    const counts = labels.map(label => ({ ...label, value: this.filteredWorkers().filter(worker => selector(worker) === label.id).length }));
    const max = Math.max(1, ...counts.map(item => item.value));
    return counts.filter(item => item.value > 0).sort((a, b) => b.value - a.value).slice(0, 6).map(item => ({ ...item, percent: item.value * 100 / max }));
  }
}
