import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { formatDate, formatTime } from '../../core/date';
import { I18nService } from '../../core/i18n.service';
import { FormsModule } from '@angular/forms';
import {
  AttendanceDecision,
  DailyResult,
  DailyStatus,
  HolidayProposal,
  ReportStatus,
  BulkDecisionRequest,
  BulkDecisionResponse,
  DayAnomaly,
  DayAnomalyDecision,
  AttendanceExceptionView,
  AttendanceExceptionResolution,
  AttendanceExceptionBulkPreview,
} from './reports.models';
import { ReportsStore } from './reports.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { NotificationService } from '../../core/notification.service';
import { CommonModule } from '@angular/common';

interface BulkPreviewData {
  decision: AttendanceDecision;
  statusType: DailyStatus;
  matchingCount: number;
  editableCount: number;
  excludedCount: number;
  decisionLabel: string;
  statusLabel: string;
  operationId: string;
}

interface ReportFilters {
  dateFrom: string;
  dateTo: string;
  categoryId: string;
  attendanceCondition: string;
  reviewStatus: string;
}

@Component({
  selector: 'app-report-review-page',
  imports: [RouterLink, TablePaginationComponent, FormsModule, CommonModule],
  providers: [ReportsStore],
  templateUrl: './report-review.page.html',
  styleUrl: './report-review.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportReviewPage {
  readonly store = inject(ReportsStore);
  readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly filter = signal<'ALL' | 'UNRESOLVED' | 'GREEN' | 'YELLOW' | 'RED'>('UNRESOLVED');
  readonly expandedRowId = signal<string | null>(null);
  readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  readonly pagination = new TablePagination();

  // Enhanced filters
  readonly reportFilters = signal<ReportFilters>({
    dateFrom: '', dateTo: '', categoryId: '', attendanceCondition: '', reviewStatus: ''
  });
  readonly showFilterPanel = signal(false);

  // Bulk preview modal
  readonly bulkPreview = signal<BulkPreviewData | null>(null);
  readonly bulkResult = signal<BulkDecisionResponse | null>(null);
  readonly executing = signal(false);

  // Prompt modal state (generic for chained prompts)
  readonly promptState = signal<{
    titleKey: string;
    defaultValue: string;
    error: string | null;
    onConfirm: (value: string) => void;
    onCancel: () => void;
  } | null>(null);
  readonly savingRowId = signal<string | null>(null);

  readonly detectingAnomalies = signal(false);
  readonly anomalySavingId = signal<string | null>(null);
  readonly anomalyPreview = signal<{ anomaly: DayAnomaly; decision: DayAnomalyDecision; reason: string } | null>(null);
  readonly dayAnomalies = computed(() => this.store.details()?.dayAnomalies ?? []);
  readonly openDayAnomalies = computed(() => this.dayAnomalies().filter(item => item.status === 'OPEN'));
  readonly dayAnomalyHistory = computed(() => this.dayAnomalies().filter(item => item.status !== 'OPEN'));
  readonly exceptionFilter = signal<'ALL' | 'OPEN' | 'CRITICAL'>('OPEN');
  readonly selectedExceptionIds = signal<string[]>([]);
  readonly exceptionResolution = signal<AttendanceExceptionResolution>('ACCEPT');
  readonly exceptionReason = signal('');
  readonly exceptionPreview = signal<AttendanceExceptionBulkPreview | null>(null);
  readonly resolvingExceptions = signal(false);
  readonly attendanceExceptions = computed(() => {
    const items = this.store.exceptionWorkbench()?.exceptions ?? [];
    if (this.exceptionFilter() === 'OPEN') return items.filter(item => item.status === 'OPEN');
    if (this.exceptionFilter() === 'CRITICAL') return items.filter(item => item.status === 'OPEN' && item.payrollBlocking);
    return items;
  });
  readonly exceptionSummary = computed(() => this.store.exceptionWorkbench()?.summary ?? { total: 0, open: 0, critical: 0, resolved: 0, affectedEmployees: 0 });

  // Filtered base data — all counts come from this to ensure consistency
  readonly filteredResults = computed(() => {
    const all = this.store.details()?.dailyResults ?? [];
    const f = this.reportFilters();
    return all.filter(r => {
      const wd = typeof r.workDate === 'number' ? new Date(r.workDate).toISOString().slice(0, 10) : String(r.workDate).slice(0, 10);
      if (f.dateFrom && wd < f.dateFrom) return false;
      if (f.dateTo && wd > f.dateTo) return false;
      if (f.categoryId && r.categoryId !== f.categoryId) return false;
      if (f.attendanceCondition) {
        const cond = f.attendanceCondition;
        if (cond === 'COMPLETE' && (r.punchCount < 2 || r.status !== 'PRESENT')) return false;
        if (cond === 'ONE_PUNCH' && r.status !== 'SINGLE_PUNCH') return false;
        if (cond === 'NO_PUNCH' && r.status !== 'NO_PUNCH') return false;
        if (cond === 'LATE' && r.lateMinutes <= 0) return false;
        if (cond === 'EARLY' && r.earlyLeaveMinutes <= 0) return false;
        if (cond === 'MISSING_HOURS' && r.effectiveWorkedMinutes >= r.expectedMinutes) return false;
      }
      if (f.reviewStatus) {
        if (f.reviewStatus === 'REVIEWED' && !r.decision) return false;
        if (f.reviewStatus === 'UNRESOLVED' && !this.blocking(r)) return false;
        if (f.reviewStatus === 'APPROVED' && this.blocking(r)) return false;
        if (f.reviewStatus === 'LOCKED' && this.blocking(r)) return false;
      }
      return true;
    });
  });
  readonly greenCount = computed(() => this.filteredResults().filter((r) => this.healthTier(r) === 'GREEN').length);
  readonly yellowCount = computed(() => this.filteredResults().filter((r) => this.healthTier(r) === 'YELLOW').length);
  readonly redCount = computed(() => this.filteredResults().filter((r) => this.healthTier(r) === 'RED').length);
  readonly totalCount = computed(() => this.filteredResults().length);
  readonly unresolvedCount = computed(() => this.filteredResults().filter((r) => this.blocking(r)).length);
  readonly singlePunchCount = computed(() => this.filteredResults().filter((r) => r.status === 'SINGLE_PUNCH' && this.blocking(r)).length);
  readonly noPunchCount = computed(() => this.filteredResults().filter((r) => r.status === 'NO_PUNCH' && this.blocking(r)).length);
  readonly manualEntryCount = computed(() => this.filteredResults().filter((r) => r.status === 'MANUAL_ENTRY' && this.blocking(r)).length);
  readonly missingScheduleCount = computed(() => this.filteredResults().filter((r) => r.status === 'MISSING_SCHEDULE' && this.blocking(r)).length);
  readonly lateCount = computed(() => this.filteredResults().filter((r) => r.lateMinutes > 0 && this.blocking(r)).length);
  readonly earlyDepartureCount = computed(() => this.filteredResults().filter((r) => r.earlyLeaveMinutes > 0 && this.blocking(r)).length);
  readonly reviewedCount = computed(() => Math.max(0, this.totalCount() - this.unresolvedCount()));
  readonly reviewedPercent = computed(() => (this.totalCount() > 0 ? Math.round((this.reviewedCount() / this.totalCount()) * 100) : 0));
  readonly canReview = computed(() => this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER']));
  readonly canApprove = computed(() => this.totalCount() > 0 && this.unresolvedCount() === 0
    && this.exceptionSummary().critical === 0 && this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER']));
  readonly rows = computed(() => {
    const f = this.filter();
    const all = this.filteredResults();
    if (f === 'UNRESOLVED') return all.filter((row) => this.blocking(row));
    if (f === 'GREEN') return all.filter((row) => this.healthTier(row) === 'GREEN');
    if (f === 'YELLOW') return all.filter((row) => this.healthTier(row) === 'YELLOW');
    if (f === 'RED') return all.filter((row) => this.healthTier(row) === 'RED');
    return all;
  });
  readonly pagedRows = computed(() => this.pagination.slice(this.rows()));
  readonly uniqueCategories = computed(() => {
    const all = this.store.details()?.dailyResults ?? [];
    const seen = new Set<string>();
    return all.filter(r => { if (seen.has(r.categoryId)) return false; seen.add(r.categoryId); return true; })
      .map(r => ({ id: r.categoryId, name: r.categoryName }));
  });
  constructor() {
    void this.store.load(this.id);
  }

  async detectDayAnomalies(): Promise<void> {
    this.detectingAnomalies.set(true);
    const success = await this.store.detectDayAnomalies(this.id);
    this.detectingAnomalies.set(false);
    if (success) this.notification.success(this.i18n.t('review.anomalyDetectSuccess', {}));
    else this.notification.error(this.store.error() ?? this.i18n.t('review.anomalyDetectFailed', {}));
  }

  previewAnomalyDecision(anomaly: DayAnomaly, decision: DayAnomalyDecision): void {
    this.anomalyPreview.set({ anomaly, decision, reason: '' });
  }

  updateAnomalyReason(reason: string): void {
    this.anomalyPreview.update(current => current ? { ...current, reason } : null);
  }

  async executeAnomalyDecision(): Promise<void> {
    const preview = this.anomalyPreview();
    if (!preview) return;
    if (!preview.reason.trim()) {
      this.notification.warning(this.i18n.t('review.anomalyReasonRequired', {}));
      return;
    }
    this.anomalySavingId.set(preview.anomaly.id);
    const response = await this.store.decideDayAnomaly(this.id, preview.anomaly.id, {
      decision: preview.decision,
      reason: preview.reason.trim(),
      operationId: crypto.randomUUID(),
    });
    this.anomalySavingId.set(null);
    if (response) {
      this.notification.success(this.i18n.t('review.anomalyAppliedCount', { applied: response.appliedCount, skipped: response.skippedCount }));
      this.anomalyPreview.set(null);
    } else this.notification.error(this.store.error() ?? this.i18n.t('review.anomalyApplyFailed', {}));
  }

  async reverseDayAnomaly(anomaly: DayAnomaly): Promise<void> {
    this.anomalySavingId.set(anomaly.id);
    const response = await this.store.reverseDayAnomaly(this.id, anomaly.id);
    this.anomalySavingId.set(null);
    if (response) this.notification.success(this.i18n.t('review.anomalyReversedCount', { applied: response.appliedCount }));
    else this.notification.error(this.store.error() ?? this.i18n.t('review.anomalyReverseFailed', {}));
  }

  async reopenDayAnomaly(anomaly: DayAnomaly): Promise<void> {
    this.anomalySavingId.set(anomaly.id);
    const success = await this.store.reopenDayAnomaly(this.id, anomaly.id);
    this.anomalySavingId.set(null);
    if (success) this.notification.success(this.i18n.t('review.anomalyReopened', {}));
    else this.notification.error(this.store.error() ?? this.i18n.t('review.anomalyReopenFailed', {}));
  }

  anomalyDecisionLabel(decision: DayAnomalyDecision | null): string {
    const key = ({ DEVICE_OUTAGE: 'review.anomalyDeviceOutage', OFFICIAL_HOLIDAY: 'review.anomalyOfficialHoliday',
      ABSENCE: 'review.anomalyAbsence', PRESENT: 'review.anomalyPresent', DEFER: 'review.anomalyDefer' } as Record<string, string>)[decision ?? ''];
    return key ? this.i18n.t(key) : '—';
  }

  anomalyHours(minutes: number): string {
    return (minutes / 60).toFixed(1);
  }

  exceptionSelected(id: string): boolean { return this.selectedExceptionIds().includes(id); }
  toggleException(id: string): void { this.selectedExceptionIds.update(ids => ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]); }
  selectVisibleExceptions(): void { this.selectedExceptionIds.set(this.attendanceExceptions().filter(x => x.status === 'OPEN').map(x => x.id)); }
  exceptionTypeLabel(item: AttendanceExceptionView): string { return this.i18n.t(`attendance.exception.${({ NO_PUNCH: 'noPunch', SINGLE_PUNCH: 'singlePunch', MISSING_SCHEDULE: 'missingSchedule', LATE: 'late', EARLY_LEAVE: 'earlyLeave', EXCESS_SHIFT: 'excessShift' } as Record<string, string>)[item.exceptionType]}`); }
  async detectAttendanceExceptions(): Promise<void> {
    const ok = await this.store.detectAttendanceExceptions(this.id);
    ok ? this.notification.success(this.i18n.t('review.exceptionDetectionComplete')) : this.notification.error(this.store.error() ?? this.i18n.t('review.exceptionActionFailed'));
  }
  async previewExceptionBulk(): Promise<void> {
    if (!this.selectedExceptionIds().length || !this.exceptionReason().trim()) { this.notification.warning(this.i18n.t('review.exceptionSelectionReasonRequired')); return; }
    this.pendingExceptionOperationId = crypto.randomUUID();
    const preview = await this.store.previewAttendanceExceptions(this.id, { exceptionIds: this.selectedExceptionIds(), resolution: this.exceptionResolution(), reason: this.exceptionReason().trim(), operationId: this.pendingExceptionOperationId });
    this.exceptionPreview.set(preview);
  }
  async applyExceptionBulk(): Promise<void> {
    const preview = this.exceptionPreview(); if (!preview) return;
    this.resolvingExceptions.set(true);
    const response = await this.store.resolveAttendanceExceptions(this.id, { exceptionIds: this.selectedExceptionIds(), resolution: this.exceptionResolution(), reason: this.exceptionReason().trim(), operationId: this.pendingExceptionOperationId });
    this.resolvingExceptions.set(false);
    if (response) { this.notification.success(this.i18n.t('review.exceptionApplied', { count: response.applied })); this.exceptionPreview.set(null); this.selectedExceptionIds.set([]); this.exceptionReason.set(''); }
    else this.notification.error(this.store.error() ?? this.i18n.t('review.exceptionActionFailed'));
  }
  private pendingExceptionOperationId = '';

  toggleFilterPanel() {
    this.showFilterPanel.update(v => !v);
  }

  updateFilter(key: keyof ReportFilters, value: string) {
    this.reportFilters.update(f => ({ ...f, [key]: value }));
  }

  clearFilters() {
    this.reportFilters.set({ dateFrom: '', dateTo: '', categoryId: '', attendanceCondition: '', reviewStatus: '' });
  }

  toggleRowExpand(id: string) {
    this.expandedRowId.update((prev) => (prev === id ? null : id));
  }
  healthTier(row: DailyResult): 'GREEN' | 'YELLOW' | 'RED' {
    if (row.status === 'NO_PUNCH' || row.status === 'MANUAL_ENTRY' || row.status === 'MISSING_SCHEDULE') return 'RED';
    if (row.status === 'SINGLE_PUNCH' || row.lateMinutes > 0 || row.earlyLeaveMinutes > 0) return 'YELLOW';
    return 'GREEN';
  }
  blocking(row: DailyResult) {
    return (
      !row.decision &&
      ['NO_PUNCH', 'SINGLE_PUNCH', 'MANUAL_ENTRY', 'MISSING_SCHEDULE'].includes(row.status)
    );
  }
  ruleState(row: DailyResult): 'AUTO' | 'BLOCKING' | null {
    if (row.status === 'SINGLE_PUNCH' && !row.decision) return 'BLOCKING';
    if (row.warning === 'Presence counted from one punch by category policy.') return 'AUTO';
    return null;
  }
  async bulkDecide(decision: AttendanceDecision, statusType: DailyStatus) {
    const allResults = this.filteredResults();
    const reportStatus = this.store.details()?.report.status;

    if (reportStatus === 'APPROVED' || reportStatus === 'EXPORTED') {
      this.notification.warning(this.i18n.t('review.confirmApproved'));
      return;
    }

    const allMatching = allResults.filter((r) => r.status === statusType);
    const editable = allMatching.filter((r) => this.blocking(r));
    const excludedCount = allMatching.length - editable.length;

    if (!editable.length) {
      if (excludedCount > 0) {
        this.notification.warning(this.i18n.t('review.confirmAllProcessed', { count: excludedCount }));
      } else {
        this.notification.warning(this.i18n.t('review.confirmNoUnresolved'));
      }
      return;
    }

    const statusText = statusType === 'SINGLE_PUNCH'
      ? this.i18n.t('review.statusSinglePunch')
      : statusType === 'NO_PUNCH'
        ? this.i18n.t('review.statusNoPunch')
        : statusType === 'MANUAL_ENTRY'
          ? this.i18n.t('review.statusManualEntry')
          : statusType === 'MISSING_SCHEDULE'
            ? this.i18n.t('review.statusMissingSchedule')
            : this.i18n.t('review.status' + statusType);
    const decText = decision === 'NORMAL_DAY'
      ? this.i18n.t('review.decisionNormalDay')
      : decision === 'DEDUCT'
        ? this.i18n.t('review.decisionDeduct')
        : decision === 'ABSENCE'
          ? this.i18n.t('review.absence', undefined)
          : decision === 'OFFICIAL_HOLIDAY'
            ? this.i18n.t('review.officialHoliday', undefined)
            : decision === 'INDIVIDUAL_REVIEW'
              ? this.i18n.t('review.individualReview', undefined)
              : this.i18n.t('review.approvedLeave');

    this.bulkPreview.set({
      decision,
      statusType,
      matchingCount: allMatching.length,
      editableCount: editable.length,
      excludedCount,
      decisionLabel: decText,
      statusLabel: statusText,
      operationId: 'BULK-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    });
    this.bulkResult.set(null);
  }

  closeBulkPreview() {
    if (this.executing()) return;
    this.bulkPreview.set(null);
    this.bulkResult.set(null);
  }

  async executeBulkDecision() {
    const preview = this.bulkPreview();
    if (!preview) return;

    this.executing.set(true);
    try {
      const request: BulkDecisionRequest = {
        decision: preview.decision,
        statusFilter: preview.statusType,
        note: `${this.i18n.t('review.bulkProcessId')} [${preview.operationId}] - ${preview.decisionLabel}`,
        operationId: preview.operationId
      };
      const result = await this.store.bulkDecision(this.id, request);
      if (result) {
        this.bulkResult.set(result);
        this.notification.success(
          this.i18n.t('review.bulkSuccess', { count: result.successCount }) +
          (result.excludedCount > 0 ? this.i18n.t('review.bulkExcludedNote', { count: result.excludedCount }) : '')
        );
        this.bulkPreview.set(null);
        this.bulkResult.set(null);
        await this.store.load(this.id);
      }
    } catch (e: any) {
      this.notification.error(this.i18n.t('review.bulkError', { error: e?.message ?? this.i18n.t('api.unexpected') }));
    } finally {
      this.executing.set(false);
    }
  }
  time(value: number | null) {
    return value === null ? '—' : formatTime(value);
  }
  date(value: number) {
    return formatDate(value);
  }
  status(value: DailyStatus) {
    return this.i18n.t(
      {
        PRESENT: 'dailyStatus.present',
        SINGLE_PUNCH: 'dailyStatus.singlePunch',
        NO_PUNCH: 'dailyStatus.noPunch',
        MANUAL_ENTRY: 'dailyStatus.manualEntry',
        NON_WORKDAY: 'dailyStatus.nonWorkday',
        HOLIDAY: 'dailyStatus.holiday',
        MISSING_SCHEDULE: 'dailyStatus.missingSchedule',
      }[value],
    );
  }
  reportStatus(value: ReportStatus): string {
    return this.i18n.t(
      {
        DRAFT: 'status.draft',
        IN_REVIEW: 'status.inReview',
        APPROVED: 'status.approved',
        EXPORTED: 'status.exported',
      }[value],
    );
  }
  decisionLabel(value: AttendanceDecision | string | null): string {
    if (!value) return '';
    switch (value) {
      case 'NORMAL_DAY':
        return this.i18n.t('decision.normalDay');
      case 'DEDUCT':
        return this.i18n.t('decision.deduct');
      case 'APPROVED_LEAVE':
        return this.i18n.t('decision.approvedLeave');
      case 'ABSENCE':
        return this.i18n.t('review.absence', undefined);
      case 'OFFICIAL_HOLIDAY':
        return this.i18n.t('review.officialHoliday', undefined);
      case 'INDIVIDUAL_REVIEW':
        return this.i18n.t('review.individualReview', undefined);
      default:
        return value;
    }
  }
  warningLabel(value: string | null): string {
    if (!value) return '';
    switch (value) {
      case 'Manual attendance confirmation is required.':
        return this.i18n.t('warning.manualConfirmationRequired');
      case 'No biometric punch found.':
        return this.i18n.t('warning.noBiometricPunch');
      case 'Presence counted from one punch by category policy.':
        return this.i18n.t('warning.singlePunchPolicy');
      case 'One punch is incomplete and requires review.':
        return this.i18n.t('warning.singlePunchIncomplete');
      case 'No effective schedule rule for this workday.':
        return this.i18n.t('warning.missingScheduleRule');
      default:
        return value;
    }
  }
  decide(row: DailyResult, decision: AttendanceDecision) {
    if (
      decision === 'NORMAL_DAY' &&
      (row.status === 'MANUAL_ENTRY' || row.status === 'SINGLE_PUNCH')
    ) {
      this.promptState.set({
        titleKey: 'review.workedMinutesPrompt',
        defaultValue: String(row.expectedMinutes),
        error: null,
        onConfirm: (input) => {
          const worked = Number(input);
          if (!Number.isFinite(worked) || worked < 0) return;
          this.promptState.set({
            titleKey: 'review.decisionNotePrompt',
            defaultValue: '',
            error: null,
            onConfirm: (note) => void this.submitDecision(row, decision, worked, note || null),
            onCancel: () => this.promptState.set(null),
          });
        },
        onCancel: () => this.promptState.set(null),
      });
    } else {
      this.promptState.set({
        titleKey: 'review.decisionNotePrompt',
        defaultValue: '',
        error: null,
        onConfirm: (note) => void this.submitDecision(row, decision, null, note || null),
        onCancel: () => this.promptState.set(null),
      });
    }
  }

  private async submitDecision(row: DailyResult, decision: AttendanceDecision,
                               workedMinutes: number | null, note: string | null): Promise<void> {
    if (this.savingRowId() === row.id) return;
    this.savingRowId.set(row.id);
    const success = await this.store.decide(this.id, row.id, decision, workedMinutes, note, row.version);
    this.savingRowId.set(null);
    if (success) {
      this.promptState.set(null);
    } else {
      this.promptState.update(ps => ps ? { ...ps, error: this.store.error() ?? this.i18n.t('review.decisionPersistenceFailed') } : ps);
    }
  }
  holiday(item: HolidayProposal, confirming: boolean) {
    if (confirming) {
      this.promptState.set({
        titleKey: 'review.holidayNamePrompt',
        defaultValue: this.i18n.t('review.confirmedHoliday'),
        error: null,
        onConfirm: (name) => {
          if (!name) return;
          this.promptState.set({
            titleKey: 'review.notePrompt',
            defaultValue: '',
            error: null,
            onConfirm: (note) => {
              this.promptState.set(null);
              this.store.decideHoliday(this.id, item.id, 'CONFIRMED', name, note || null);
            },
            onCancel: () => this.promptState.set(null),
          });
        },
        onCancel: () => this.promptState.set(null),
      });
    } else {
      this.promptState.set({
        titleKey: 'review.notePrompt',
        defaultValue: '',
        error: null,
        onConfirm: (note) => {
          this.promptState.set(null);
          this.store.decideHoliday(this.id, item.id, 'REJECTED', null, note || null);
        },
        onCancel: () => this.promptState.set(null),
      });
    }
  }
}
