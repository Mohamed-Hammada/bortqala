import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { formatDate, formatTime } from '../../core/date';
import { I18nService } from '../../core/i18n.service';
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

@Component({
  selector: 'app-report-review-page',
  imports: [RouterLink, TablePaginationComponent],
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
    void this.store.load(this.id);
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

    const confirmMsg =
      `${this.i18n.t('review.confirmBulkPreview')}\n` +
      `${this.i18n.t('review.confirmBulkMatching', { count: allMatching.length })}\n` +
      `${this.i18n.t('review.confirmBulkEditable', { count: editable.length })}\n` +
      `${this.i18n.t('review.confirmBulkExcluded', { count: excludedCount })}\n\n` +
      `${this.i18n.t('review.confirmBulkApply', { decision: decText, count: editable.length, status: statusText })}`;

    if (!confirm(confirmMsg)) return;

    try {
      let successCount = 0;
      const opId = 'BULK-' + Date.now();
      for (const r of editable) {
        await this.store.decide(
          this.id,
          r.id,
          decision,
          decision === 'NORMAL_DAY' ? r.expectedMinutes : 0,
          `${this.i18n.t('review.bulkProcessId')} [${opId}] - ${decText}`
        );
        successCount++;
      }
      this.notification.success(
        this.i18n.t('review.bulkSuccess', { count: successCount }) +
        (excludedCount > 0 ? this.i18n.t('review.bulkExcludedNote', { count: excludedCount }) : '')
      );
    } catch (e: any) {
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
  async decide(row: DailyResult, decision: AttendanceDecision) {
    let worked: number | null = null;
    if (
      decision === 'NORMAL_DAY' &&
      (row.status === 'MANUAL_ENTRY' || row.status === 'SINGLE_PUNCH')
    ) {
      const input = prompt(this.i18n.t('review.workedMinutesPrompt'), String(row.expectedMinutes));
      if (input === null) return;
      worked = Number(input);
      if (!Number.isFinite(worked) || worked < 0) return;
    }
    const note = prompt(this.i18n.t('review.decisionNotePrompt')) ?? null;
    await this.store.decide(this.id, row.id, decision, worked, note);
  }
  async holiday(item: HolidayProposal, confirming: boolean) {
    const name = confirming
      ? (prompt(this.i18n.t('review.holidayNamePrompt'), this.i18n.t('review.confirmedHoliday')) ??
        null)
      : null;
    if (confirming && name === null) return;
    const note = prompt(this.i18n.t('review.notePrompt')) ?? null;
    await this.store.decideHoliday(
      this.id,
      item.id,
      confirming ? 'CONFIRMED' : 'REJECTED',
      name,
      note,
    );
  }
}
