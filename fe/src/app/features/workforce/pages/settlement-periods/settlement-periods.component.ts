import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { SettlementIssue, SettlementPeriod, SettlementCalculationSummary, ContractorSettlementDetail } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { ContractorSettlementDetailModalComponent } from '../../ui/contractor-settlement-detail-modal.component';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { downloadBlob } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-settlement-periods',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, ContractorSettlementDetailModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './settlement-periods.component.html',
  styleUrls: ['./settlement-periods.component.scss']
})
export class SettlementPeriodsComponent implements OnInit {
  readonly workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly createOpen = signal(false);
  readonly summaryOpen = signal(false);
  readonly contractorListOpen = signal(false);
  readonly detailModalOpen = signal(false);
  readonly loadingContractorSettlements = signal(false);

  readonly calculatingId = signal<string | null>(null);
  readonly summary = signal<SettlementCalculationSummary | null>(null);
  readonly issues = signal<SettlementIssue[]>([]);
  readonly selectedPeriod = signal<SettlementPeriod | null>(null);
  readonly contractorSettlements = signal<ContractorSettlementDetail[]>([]);
  readonly selectedContractorSettlement = signal<ContractorSettlementDetail | null>(null);
  readonly calculationError = signal<string | null>(null);
  readonly pageError = signal<string | null>(null);

  createForm = { periodCode: '', startDate: new Date().toISOString().slice(0, 10), endDate: new Date().toISOString().slice(0, 10), cycleType: 'HALF_MONTH' };

  ngOnInit(): void { this.reload(); }
  settlementStatusLabel(status: string): string { const keys:Record<string,string>={DRAFT:'workforce.ui.settlement.statusDraft',CALCULATED:'workforce.ui.settlement.statusCalculated',REVIEWED:'workforce.ui.settlement.statusReviewed',APPROVED:'workforce.ui.settlement.statusApproved',POSTED:'workforce.ui.settlement.statusPosted',PAID:'workforce.ui.settlement.statusPaid',LOCKED:'workforce.ui.settlement.statusLocked'}; return keys[status]?this.i18n.t(keys[status]):status; }

  reload(): void {
    this.workforceService.loadSettlementPeriods().subscribe({
      error: error => this.pageError.set(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.loadFailed')))
    });
  }

  openCreateModal(): void {
    this.createForm = { periodCode: `PER-${Date.now().toString().slice(-6)}`, startDate: new Date().toISOString().slice(0, 10), endDate: new Date().toISOString().slice(0, 10), cycleType: 'HALF_MONTH' };
    this.createOpen.set(true);
  }

  savePeriod(): void {
    this.workforceService.createSettlementPeriod(this.createForm).subscribe({
      next: () => { this.createOpen.set(false); this.notification.success(this.i18n.t('workforce.settlementPeriodCreated')); },
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.createFailed')))
    });
  }

  calculatePeriod(period: SettlementPeriod): void {
    this.calculatingId.set(period.id); this.calculationError.set(null); this.summary.set(null); this.issues.set([]); this.summaryOpen.set(true);
    this.workforceService.calculatePeriod(period.id).subscribe({
      next: result => {
        this.summary.set(result); this.issues.set(result.issues); this.calculatingId.set(null); this.reload();
        this.notification.success(this.i18n.t('workforce.ui.settlement.calculatedSuccess', { version: result.calculationVersion }));
      },
      error: error => {
        this.calculatingId.set(null); this.calculationError.set(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.recalcFailed'))); this.reload();
      }
    });
  }

  showIssues(period: SettlementPeriod): void {
    this.summary.set(null); this.calculationError.set(period.lastCalculationError ?? null); this.summaryOpen.set(true);
    this.workforceService.loadSettlementIssues(period.id).subscribe({
      next: value => this.issues.set(value),
      error: error => this.calculationError.set(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.issuesFailed')))
    });
  }

  openContractorSettlementsModal(period: SettlementPeriod): void {
    this.selectedPeriod.set(period);
    this.contractorListOpen.set(true);
    this.loadingContractorSettlements.set(true);
    this.workforceService.loadContractorSettlementsForPeriod(period.id).subscribe({
      next: list => {
        this.contractorSettlements.set(list);
        this.loadingContractorSettlements.set(false);
      },
      error: error => {
        this.loadingContractorSettlements.set(false);
        this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.contractorsFailed')));
      }
    });
  }

  openDetailModal(cs: ContractorSettlementDetail): void {
    this.selectedContractorSettlement.set(cs);
    this.detailModalOpen.set(true);
  }

  onSettlementUpdated(updatedItem: ContractorSettlementDetail): void {
    this.selectedContractorSettlement.set(updatedItem);
    if (this.selectedPeriod()) {
      this.openContractorSettlementsModal(this.selectedPeriod()!);
    }
  }

  reviewPeriod(period: SettlementPeriod): void {
    this.workforceService.reviewPeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodReviewed')); },
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.reviewFailed')))
    });
  }

  approvePeriod(period: SettlementPeriod): void {
    this.workforceService.approvePeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodApproved')); },
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.approveFailed')))
    });
  }

  lockPeriod(period: SettlementPeriod): void {
    this.workforceService.lockPeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodLocked')); },
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.lockFailed')))
    });
  }

  exportExcel(period: SettlementPeriod): void {
    this.workforceService.exportSettlementPeriodExcel(period.id).subscribe({
      next: blob => downloadBlob(blob, `settlement-${period.periodCode}-v${period.calculationVersion}.xlsx`),
      error: error => this.notification.error(apiErrorDetail(error, this.i18n.t('workforce.ui.settlement.exportFailed')))
    });
  }

  statusLabel(status: string): string { const keys:Record<string,string>={DRAFT:'workforce.ui.settlement.statusDraft',CALCULATED:'workforce.ui.settlement.statusCalculated',REVIEWED:'workforce.ui.settlement.statusReviewed',APPROVED:'workforce.ui.settlement.statusApproved',LOCKED:'workforce.ui.settlement.statusLocked'}; return keys[status]?this.i18n.t(keys[status]):status; }

  cycleLabel(cycle: string): string { const keys:Record<string,string>={HALF_MONTH:'workforce.ui.categories.halfMonth',MONTHLY:'workforce.ui.categories.monthly'}; return keys[cycle]?this.i18n.t(keys[cycle]):cycle; }
}
