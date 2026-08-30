import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ModalDialogComponent } from '../../../shared/ui/modal-dialog/modal-dialog.component';
import { DprService } from '../data-access/dpr.service';
import { DailyReportResponse, DailyReportStatus } from '../models/dpr.models';
import { WbsNodeResponse } from '../models/project.models';
import { DailyReportEditorModalComponent } from './daily-report-editor-modal.component';

@Component({
  selector: 'app-daily-reports-list',
  standalone: true,
  imports: [
    CommonModule,
    ModalDialogComponent,
    DailyReportEditorModalComponent,
  ],
  templateUrl: './daily-reports-list.component.html',
  styleUrl: './daily-reports-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DailyReportsListComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly dprService = inject(DprService);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);

  @Input({ required: true }) projectId!: string;
  @Input() availableWbsNodes: WbsNodeResponse[] = [];
  @Output() reportChanged = new EventEmitter<void>();

  readonly loading = signal(false);
  readonly editorOpen = signal(false);
  readonly selectedReport = signal<DailyReportResponse | null>(null);

  readonly statusFilter = signal<string>('ALL');
  readonly searchQuery = signal<string>('');

  // Reopen prompt dialog
  readonly reopenModalOpen = signal(false);
  readonly reopenReportTarget = signal<DailyReportResponse | null>(null);
  readonly reopenReason = signal<string>('');
  readonly reopening = signal(false);

  readonly filteredReports = computed(() => {
    const list = this.dprService.reports();
    const status = this.statusFilter();
    const query = this.searchQuery().trim().toLowerCase();

    return list.filter((r) => {
      const matchesStatus = status === 'ALL' || r.status === status;
      const matchesQuery =
        !query ||
        r.reportNumber.toLowerCase().includes(query) ||
        (r.generalNotes && r.generalNotes.toLowerCase().includes(query));
      return matchesStatus && matchesQuery;
    });
  });

  readonly totalReports = computed(() => this.dprService.reports().length);
  readonly approvedReports = computed(() => this.dprService.reports().filter((r) => r.status === 'APPROVED').length);
  readonly submittedReports = computed(() => this.dprService.reports().filter((r) => r.status === 'SUBMITTED').length);
  readonly draftReports = computed(() => this.dprService.reports().filter((r) => r.status === 'DRAFT').length);

  async ngOnInit(): Promise<void> {
    await this.loadReports();
  }

  async loadReports(): Promise<void> {
    this.loading.set(true);
    try {
      await firstValueFrom(this.dprService.loadReports(this.projectId));
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openCreate(): void {
    this.selectedReport.set(null);
    this.editorOpen.set(true);
  }

  async openEdit(report: DailyReportResponse): Promise<void> {
    try {
      const full = await firstValueFrom(this.dprService.getReport(this.projectId, report.id));
      this.selectedReport.set(full);
      this.editorOpen.set(true);
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  onEditorClosed(saved: boolean): void {
    this.editorOpen.set(false);
    this.selectedReport.set(null);
    if (saved) {
      this.loadReports();
      this.reportChanged.emit();
    }
  }

  async copyPrevious(): Promise<void> {
    const todayEpoch = Date.now();
    try {
      await firstValueFrom(this.dprService.copyPreviousDay(this.projectId, todayEpoch));
      this.notification.success(this.i18n.t('dpr.copySuccess'));
      await this.loadReports();
      this.reportChanged.emit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async submitReport(report: DailyReportResponse): Promise<void> {
    try {
      await firstValueFrom(this.dprService.submitReport(this.projectId, report.id));
      this.notification.success(this.i18n.t('dpr.submittedSuccess'));
      await this.loadReports();
      this.reportChanged.emit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  async approveReport(report: DailyReportResponse): Promise<void> {
    try {
      await firstValueFrom(this.dprService.approveReport(this.projectId, report.id));
      this.notification.success(this.i18n.t('dpr.approvedSuccess'));
      await this.loadReports();
      this.reportChanged.emit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  openReopenModal(report: DailyReportResponse): void {
    this.reopenReportTarget.set(report);
    this.reopenReason.set('');
    this.reopenModalOpen.set(true);
  }

  async confirmReopen(): Promise<void> {
    const report = this.reopenReportTarget();
    if (!report) return;

    this.reopening.set(true);
    try {
      await firstValueFrom(this.dprService.reopenReport(this.projectId, report.id, this.reopenReason().trim()));
      this.notification.success(this.i18n.t('dpr.reopenedSuccess'));
      this.reopenModalOpen.set(false);
      await this.loadReports();
      this.reportChanged.emit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    } finally {
      this.reopening.set(false);
    }
  }

  async deleteReport(report: DailyReportResponse): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'dpr.deleteConfirmTitle',
      messageKey: 'dpr.deleteConfirmMessage',
    });
    if (!confirmed) return;

    try {
      await firstValueFrom(this.dprService.deleteReport(this.projectId, report.id));
      this.notification.success(this.i18n.t('dpr.deletedSuccess'));
      await this.loadReports();
      this.reportChanged.emit();
    } catch (err) {
      this.notification.error(apiErrorMessage(err, this.i18n));
    }
  }

  formatEpoch(epoch?: number | null): string {
    if (!epoch) return '—';
    return formatDate(epoch);
  }

  getStatusClass(status: DailyReportStatus): string {
    switch (status) {
      case 'APPROVED':
        return 'status-approved';
      case 'SUBMITTED':
        return 'status-submitted';
      case 'REOPENED':
        return 'status-reopened';
      case 'DRAFT':
      default:
        return 'status-draft';
    }
  }

  getWeatherIcon(w?: string | null): string {
    switch (w) {
      case 'SUNNY':
      case 'CLEAR':
        return '☀️';
      case 'RAINY':
        return '🌧️';
      case 'WINDY':
        return '💨';
      case 'HOT':
        return '🔥';
      case 'DUSTY':
        return '🌪️';
      case 'STORMY':
        return '⛈️';
      default:
        return '🌤️';
    }
  }
}
