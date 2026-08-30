import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { SkeletonComponent } from '../../shared/ui/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import {
  CreateCyclePayload,
  CreateKpiPayload,
  KpiCategory,
  PerformanceAppraisal,
  PerformanceCycle,
  PerformanceKpi,
} from './performance.models';
import { PerformanceService } from './performance.service';

@Component({
  selector: 'app-performance-page',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    ReactiveFormsModule,
    TablePaginationComponent,
    SkeletonComponent,
    EmptyStateComponent,
    ModalDialogComponent,
  ],
  templateUrl: './performance.page.html',
  styleUrl: './performance.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerformancePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);
  readonly auth = inject(AuthService);
  private readonly notification = inject(NotificationService);
  private readonly performanceService = inject(PerformanceService);

  readonly activeTab = signal<'appraisals' | 'cycles' | 'kpis'>('appraisals');
  readonly loading = signal(true);

  readonly appraisals = signal<PerformanceAppraisal[]>([]);
  readonly cycles = signal<PerformanceCycle[]>([]);
  readonly kpis = signal<PerformanceKpi[]>([]);
  readonly employees = signal<Array<{ id: string; fullName: string; employeeCode: string }>>([]);

  readonly selectedCycleFilter = signal<string>('');
  readonly selectedAppraisal = signal<PerformanceAppraisal | null>(null);

  readonly pagination = new TablePagination(10);

  readonly isInitAppraisalOpen = signal(false);
  readonly isEvaluateOpen = signal(false);
  readonly isCreateCycleOpen = signal(false);
  readonly isCreateKpiOpen = signal(false);

  readonly initAppraisalForm = this.fb.group({
    cycleId: ['', Validators.required],
    employeeId: ['', Validators.required],
    reviewerId: [''],
  });

  readonly createCycleForm = this.fb.group({
    nameAr: ['', Validators.required],
    nameEn: ['', Validators.required],
    periodYear: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    startDate: [new Date().toISOString().substring(0, 10), Validators.required],
    endDate: [new Date().toISOString().substring(0, 10), Validators.required],
  });

  readonly createKpiForm = this.fb.group({
    cycleId: ['', Validators.required],
    code: ['', Validators.required],
    titleAr: ['', Validators.required],
    titleEn: ['', Validators.required],
    category: ['OPERATIONAL' as KpiCategory, Validators.required],
    targetValue: [100, [Validators.required, Validators.min(0)]],
    weightPercentage: [20, [Validators.required, Validators.min(1), Validators.max(100)]],
  });

  readonly evaluateForm = this.fb.group({
    managerFeedback: ['', Validators.required],
    developmentPlan: [''],
  });

  readonly outstandingCount = computed(
    () => this.appraisals().filter((a) => a.ratingBand === 'OUTSTANDING').length,
  );
  readonly meetsCount = computed(
    () =>
      this.appraisals().filter(
        (a) => a.ratingBand === 'EXCEEDS_EXPECTATIONS' || a.ratingBand === 'MEETS_EXPECTATIONS',
      ).length,
  );
  readonly needsImprovementCount = computed(
    () =>
      this.appraisals().filter(
        (a) => a.ratingBand === 'NEEDS_IMPROVEMENT' || a.ratingBand === 'UNSATISFACTORY',
      ).length,
  );

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.performanceService.listCycles().subscribe({
      next: (cycles) => {
        this.cycles.set(cycles);
        if (cycles.length > 0 && !this.selectedCycleFilter()) {
          this.selectedCycleFilter.set(cycles[0].id);
        }
        this.loadTabData();
      },
      error: () => this.loading.set(false),
    });

    this.performanceService.listEmployees().subscribe({
      next: (emps) => this.employees.set(emps),
    });
  }

  loadTabData(): void {
    this.loading.set(true);
    const cycleId = this.selectedCycleFilter();

    this.performanceService.listAppraisals(cycleId).subscribe({
      next: (data) => {
        this.appraisals.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.performanceService.listKpis(cycleId).subscribe({
      next: (data) => this.kpis.set(data),
    });
  }

  setTab(tab: 'appraisals' | 'cycles' | 'kpis'): void {
    this.activeTab.set(tab);
  }

  openInitAppraisal(): void {
    this.initAppraisalForm.reset({
      cycleId: this.selectedCycleFilter() || (this.cycles()[0]?.id ?? ''),
      employeeId: '',
      reviewerId: '',
    });
    this.isInitAppraisalOpen.set(true);
  }

  submitInitAppraisal(): void {
    if (this.initAppraisalForm.invalid) return;
    const v = this.initAppraisalForm.getRawValue();
    this.performanceService
      .initAppraisal({
        cycleId: v.cycleId!,
        employeeId: v.employeeId!,
        reviewerId: v.reviewerId || undefined,
      })
      .subscribe({
        next: () => {
          this.notification.success(this.i18n.t('performance.appraisalSaved'));
          this.isInitAppraisalOpen.set(false);
          this.loadTabData();
        },
      });
  }

  openEvaluateModal(appr: PerformanceAppraisal): void {
    this.selectedAppraisal.set(appr);
    this.evaluateForm.reset({
      managerFeedback: appr.managerFeedback || '',
      developmentPlan: appr.developmentPlan || '',
    });
    this.isEvaluateOpen.set(true);
  }

  submitEvaluation(): void {
    const appr = this.selectedAppraisal();
    if (!appr || this.evaluateForm.invalid) return;

    const v = this.evaluateForm.getRawValue();
    const scores = this.kpis().map((kpi) => ({
      kpiId: kpi.id,
      selfRating: 90,
      managerRating: 90,
      comments: '',
    }));

    this.performanceService
      .submitAppraisal(appr.id, {
        kpiScores: scores,
        managerFeedback: v.managerFeedback!,
        developmentPlan: v.developmentPlan || '',
      })
      .subscribe({
        next: () => {
          this.notification.success(this.i18n.t('performance.appraisalSaved'));
          this.isEvaluateOpen.set(false);
          this.loadTabData();
        },
      });
  }

  finalizeAppraisal(appr: PerformanceAppraisal): void {
    this.performanceService.finalizeAppraisal(appr.id).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('performance.appraisalFinalized'));
        this.loadTabData();
      },
    });
  }

  openCreateCycle(): void {
    this.createCycleForm.reset({
      nameAr: '',
      nameEn: '',
      periodYear: new Date().getFullYear(),
      startDate: new Date().toISOString().substring(0, 10),
      endDate: new Date().toISOString().substring(0, 10),
    });
    this.isCreateCycleOpen.set(true);
  }

  submitCreateCycle(): void {
    if (this.createCycleForm.invalid) return;
    const v = this.createCycleForm.getRawValue() as CreateCyclePayload;
    this.performanceService.createCycle(v).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('performance.cycleSaved'));
        this.isCreateCycleOpen.set(false);
        this.loadAll();
      },
    });
  }

  lockCycle(cycle: PerformanceCycle): void {
    this.performanceService.lockCycle(cycle.id).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('performance.cycleSaved'));
        this.loadAll();
      },
    });
  }

  openCreateKpi(): void {
    this.createKpiForm.reset({
      cycleId: this.selectedCycleFilter() || (this.cycles()[0]?.id ?? ''),
      code: '',
      titleAr: '',
      titleEn: '',
      category: 'OPERATIONAL',
      targetValue: 100,
      weightPercentage: 20,
    });
    this.isCreateKpiOpen.set(true);
  }

  submitCreateKpi(): void {
    if (this.createKpiForm.invalid) return;
    const v = this.createKpiForm.getRawValue() as CreateKpiPayload;
    this.performanceService.createKpi(v).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('performance.kpiSaved'));
        this.isCreateKpiOpen.set(false);
        this.loadTabData();
      },
    });
  }
}
