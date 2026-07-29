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
  DowntimeDecisionRequest,
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

interface DeviceDowntimeEvent {
  date: string;
  categoryName: string;
  categoryId: string;
  location: string;
  affectedCount: number;
  decision?: 'NORMAL_DAY' | 'ABSENT' | 'HOLIDAY' | 'DEVICE_FAILURE' | 'INDIVIDUAL_REVIEW';
  saving?: boolean;
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
    onConfirm: (value: string) => void;
    onCancel: () => void;
  } | null>(null);

  // Device downtime detection
  readonly showDowntimeSection = signal(false);
  readonly downtimeEvents = signal<DeviceDowntimeEvent[]>([]);

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
  readonly canApprove = computed(() => this.totalCount() > 0 && this.unresolvedCount() === 0 && this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER']));
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
    this.store.load(this.id).then(() => this.detectDowntime());
  }
  // Device downtime detection
  detectDowntime() {
    const results = this.store.details()?.dailyResults ?? [];
    const byDateAndCategory = new Map<string, Map<string, { results: DailyResult[]; categoryId: string }>>();
    for (const r of results) {
      const dateKey = typeof r.workDate === 'number'
        ? new Date(r.workDate).toISOString().slice(0, 10)
        : String(r.workDate).slice(0, 10);
      if (!byDateAndCategory.has(dateKey)) byDateAndCategory.set(dateKey, new Map());
      const catMap = byDateAndCategory.get(dateKey)!;
      if (!catMap.has(r.categoryName)) catMap.set(r.categoryName, { results: [], categoryId: r.categoryId });
      catMap.get(r.categoryName)!.results.push(r);
    }

    const events: DeviceDowntimeEvent[] = [];
    for (const [date, catMap] of byDateAndCategory) {
      for (const [catName, catData] of catMap) {
        const noPunchCount = catData.results.filter(r => r.status === 'NO_PUNCH' || r.status === 'MANUAL_ENTRY').length;
        const totalCount = catData.results.length;
        if (totalCount >= 2 && noPunchCount / totalCount >= 0.7) {
          events.push({
            date,
            categoryName: catName,
            categoryId: catData.categoryId,
            location: '',
            affectedCount: noPunchCount,
          });
        }
      }
    }
    this.downtimeEvents.set(events);
    this.showDowntimeSection.set(events.length > 0);
  }

  downtimeDecision(event: DeviceDowntimeEvent, decision: NonNullable<DeviceDowntimeEvent['decision']>) {
    event.decision = decision;
    event.saving = true;
    this.downtimeEvents.update(events => [...events]);
    const request: DowntimeDecisionRequest = {
      date: event.date,
      categoryId: event.categoryId,
      location: event.location,
      decision: decision,
    };
    this.store.saveDowntimeDecision(this.id, request).then(success => {
      event.saving = false;
      this.downtimeEvents.update(events => [...events]);
      if (success) {
        this.notification.success(this.i18n.t('review.downtimeSaved', undefined, 'تم حفظ قرار عطل الجهاز بنجاح ✓'));
      } else {
        this.notification.error(this.i18n.t('review.downtimeSaveError', undefined, 'فشل حفظ قرار عطل الجهاز'));
        this.downtimeEvents.update(events => [...events]);
      }
    });
  }

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
          ? this.i18n.t('review.absence', undefined, 'غياب')
          : decision === 'OFFICIAL_HOLIDAY'
            ? this.i18n.t('review.officialHoliday', undefined, 'إجازة رسمية')
            : decision === 'INDIVIDUAL_REVIEW'
              ? this.i18n.t('review.individualReview', undefined, 'مراجعة فردية')
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
        return this.i18n.t('review.absence', undefined, 'غياب');
      case 'OFFICIAL_HOLIDAY':
        return this.i18n.t('review.officialHoliday', undefined, 'إجازة رسمية');
      case 'INDIVIDUAL_REVIEW':
        return this.i18n.t('review.individualReview', undefined, 'مراجعة فردية');
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
        onConfirm: (input) => {
          const worked = Number(input);
          if (!Number.isFinite(worked) || worked < 0) return;
          this.promptState.set({
            titleKey: 'review.decisionNotePrompt',
            defaultValue: '',
            onConfirm: (note) => {
              this.promptState.set(null);
              this.store.decide(this.id, row.id, decision, worked, note || null);
            },
            onCancel: () => this.promptState.set(null),
          });
        },
        onCancel: () => this.promptState.set(null),
      });
    } else {
      this.promptState.set({
        titleKey: 'review.decisionNotePrompt',
        defaultValue: '',
        onConfirm: (note) => {
          this.promptState.set(null);
          this.store.decide(this.id, row.id, decision, null, note || null);
        },
        onCancel: () => this.promptState.set(null),
      });
    }
  }
  holiday(item: HolidayProposal, confirming: boolean) {
    if (confirming) {
      this.promptState.set({
        titleKey: 'review.holidayNamePrompt',
        defaultValue: this.i18n.t('review.confirmedHoliday'),
        onConfirm: (name) => {
          if (!name) return;
          this.promptState.set({
            titleKey: 'review.notePrompt',
            defaultValue: '',
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
        onConfirm: (note) => {
          this.promptState.set(null);
          this.store.decideHoliday(this.id, item.id, 'REJECTED', null, note || null);
        },
        onCancel: () => this.promptState.set(null),
      });
    }
  }
}
