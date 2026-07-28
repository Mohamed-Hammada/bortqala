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
}

interface DeviceDowntimeEvent {
  date: string;
  categoryName: string;
  location: string;
  affectedCount: number;
  decision?: 'NORMAL_DAY' | 'ABSENT' | 'HOLIDAY' | 'DEVICE_FAILURE' | 'INDIVIDUAL_REVIEW';
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

  // Bulk preview modal
  readonly bulkPreview = signal<BulkPreviewData | null>(null);
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
  readonly greenCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'GREEN').length);
  readonly yellowCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'YELLOW').length);
  readonly redCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'RED').length);
  readonly totalCount = computed(() => this.store.details()?.dailyResults.length ?? 0);
  readonly unresolvedCount = computed(() => this.store.details()?.report.unresolvedCount ?? 0);
  readonly unresolvedRowsCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.blocking(r)).length);
  readonly singlePunchCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => r.status === 'SINGLE_PUNCH' && this.blocking(r)).length);
  readonly noPunchCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => r.status === 'NO_PUNCH' && this.blocking(r)).length);
  readonly manualEntryCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => r.status === 'MANUAL_ENTRY' && this.blocking(r)).length);
  readonly missingScheduleCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => r.status === 'MISSING_SCHEDULE' && this.blocking(r)).length);
  readonly reviewedCount = computed(() => Math.max(0, this.totalCount() - this.unresolvedCount()));
  readonly reviewedPercent = computed(() => (this.totalCount() > 0 ? Math.round((this.reviewedCount() / this.totalCount()) * 100) : 0));
  readonly canReview = computed(() => this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER']));
  readonly canApprove = computed(() => this.totalCount() > 0 && this.unresolvedCount() === 0 && this.auth.hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER']));
  readonly rows = computed(() => {
    const all = this.store.details()?.dailyResults ?? [];
    const f = this.filter();
    if (f === 'UNRESOLVED') return all.filter((row) => this.blocking(row));
    if (f === 'GREEN') return all.filter((row) => this.healthTier(row) === 'GREEN');
    if (f === 'YELLOW') return all.filter((row) => this.healthTier(row) === 'YELLOW');
    if (f === 'RED') return all.filter((row) => this.healthTier(row) === 'RED');
    return all;
  });
  readonly pagedRows = computed(() => this.pagination.slice(this.rows()));
  constructor() {
    this.store.load(this.id).then(() => this.detectDowntime());
  }
  // Device downtime detection
  detectDowntime() {
    const results = this.store.details()?.dailyResults ?? [];
    const categories = this.store.details()?.categories ?? [];

    const byDateAndCategory = new Map<string, Map<string, DailyResult[]>>();
    for (const r of results) {
      const dateKey = new Date(r.workDate).toISOString().slice(0, 10);
      if (!byDateAndCategory.has(dateKey)) byDateAndCategory.set(dateKey, new Map());
      const catMap = byDateAndCategory.get(dateKey)!;
      if (!catMap.has(r.categoryName)) catMap.set(r.categoryName, []);
      catMap.get(r.categoryName)!.push(r);
    }

    const events: DeviceDowntimeEvent[] = [];
    for (const [date, catMap] of byDateAndCategory) {
      for (const [catName, catResults] of catMap) {
        const noPunchCount = catResults.filter(r => r.status === 'NO_PUNCH' || r.status === 'MANUAL_ENTRY').length;
        const totalCount = catResults.length;
        if (totalCount >= 3 && noPunchCount / totalCount >= 0.7) {
          events.push({
            date,
            categoryName: catName,
            location: '',
            affectedCount: noPunchCount,
          });
        }
      }
    }

    this.downtimeEvents.set(events);
    this.showDowntimeSection.set(events.length > 0);
  }

  downtimeDecision(event: DeviceDowntimeEvent, decision: DeviceDowntimeEvent['decision']) {
    event.decision = decision;
    this.downtimeEvents.update(events => [...events]);
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
    const allResults = this.store.details()?.dailyResults ?? [];
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
          : this.i18n.t('review.statusMissingSchedule');
    const decText = decision === 'NORMAL_DAY'
      ? this.i18n.t('review.decisionNormalDay')
      : decision === 'DEDUCT'
        ? this.i18n.t('review.decisionDeduct')
        : this.i18n.t('review.approvedLeave');

    this.bulkPreview.set({
      decision,
      statusType,
      matchingCount: allMatching.length,
      editableCount: editable.length,
      excludedCount,
      decisionLabel: decText,
      statusLabel: statusText
    });
  }

  closeBulkPreview() {
    if (this.executing()) return;
    this.bulkPreview.set(null);
  }

  async executeBulkDecision() {
    const preview = this.bulkPreview();
    if (!preview) return;

    const allResults = this.store.details()?.dailyResults ?? [];
    const editable = allResults.filter((r) => r.status === preview.statusType && this.blocking(r));
    if (!editable.length) {
      this.bulkPreview.set(null);
      return;
    }

    this.executing.set(true);
    try {
      let successCount = 0;
      const opId = 'BULK-' + Date.now();
      for (const r of editable) {
        await this.store.decide(
          this.id,
          r.id,
          preview.decision,
          preview.decision === 'NORMAL_DAY' ? r.expectedMinutes : 0,
          `${this.i18n.t('review.bulkProcessId')} [${opId}] - ${preview.decisionLabel}`
        );
        successCount++;
      }
      this.bulkPreview.set(null);
      this.executing.set(false);
      this.notification.success(
        this.i18n.t('review.bulkSuccess', { count: successCount }) +
        (preview.excludedCount > 0 ? this.i18n.t('review.bulkExcludedNote', { count: preview.excludedCount }) : '')
      );
    } catch (e: any) {
      this.executing.set(false);
      this.notification.error(this.i18n.t('review.bulkError', { error: e?.message ?? this.i18n.t('api.unexpected') }));
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
