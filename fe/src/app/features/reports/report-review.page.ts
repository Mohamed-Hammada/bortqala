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
  readonly filter = signal<'ALL' | 'UNRESOLVED' | 'GREEN' | 'YELLOW' | 'RED'>('UNRESOLVED');
  readonly expandedRowId = signal<string | null>(null);
  readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  readonly pagination = new TablePagination();
  readonly greenCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'GREEN').length);
  readonly yellowCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'YELLOW').length);
  readonly redCount = computed(() => (this.store.details()?.dailyResults ?? []).filter((r) => this.healthTier(r) === 'RED').length);
  readonly totalCount = computed(() => this.store.details()?.dailyResults.length ?? 0);
  readonly unresolvedCount = computed(() => this.store.details()?.report.unresolvedCount ?? 0);
  readonly reviewedCount = computed(() => Math.max(0, this.totalCount() - this.unresolvedCount()));
  readonly reviewedPercent = computed(() => (this.totalCount() > 0 ? Math.round((this.reviewedCount() / this.totalCount()) * 100) : 100));
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
    const targets = this.rows().filter((r) => r.status === statusType && this.blocking(r));
    if (!targets.length) return;
    const confirmMsg = this.i18n.t('review.bulkConfirmPrompt') || `تطبيق القرار على ${targets.length} سجل؟`;
    if (!confirm(confirmMsg)) return;
    for (const r of targets) {
      await this.store.decide(this.id, r.id, decision, decision === 'NORMAL_DAY' ? r.expectedMinutes : 0, 'Bulk HR Decision');
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
  canReview() {
    return this.auth.hasAnyRole(['ADMIN', 'HR_MANAGER', 'HR_REVIEWER']);
  }
  canApprove() {
    return this.auth.hasAnyRole(['ADMIN', 'HR_MANAGER']);
  }
}
