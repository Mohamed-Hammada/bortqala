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
  readonly filter = signal<'ALL' | 'UNRESOLVED'>('UNRESOLVED');
  readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  readonly pagination = new TablePagination();
  readonly rows = computed(() => {
    const rows = this.store.details()?.dailyResults ?? [];
    return this.filter() === 'UNRESOLVED' ? rows.filter((row) => this.blocking(row)) : rows;
  });
  readonly pagedRows = computed(() => this.pagination.slice(this.rows()));
  constructor() {
    void this.store.load(this.id);
  }
  blocking(row: DailyResult) {
    return (
      !row.decision &&
      ['NO_PUNCH', 'SINGLE_PUNCH', 'MANUAL_ENTRY', 'MISSING_SCHEDULE'].includes(row.status)
    );
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
