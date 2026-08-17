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
  template: `
    <section class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">{{ i18n.t('workforce.ui.settlement.eyebrow') }}</span>
          <h1>{{ i18n.t('workforce.ui.settlement.title') }}</h1>
          <p>{{ i18n.t('workforce.ui.settlement.description') }}</p>
        </div>
        <button type="button" class="btn primary" (click)="openCreateModal()">{{ i18n.t('workforce.ui.settlement.new') }}</button>
      </header>

      <div class="state-flow" [attr.aria-label]="i18n.t('workforce.ui.settlement.flowAria')">
        <span>{{ i18n.t('workforce.ui.settlement.flowDraft') }}</span><b>←</b><span>{{ i18n.t('workforce.ui.settlement.flowCalculated') }}</span><b>←</b><span>{{ i18n.t('workforce.ui.settlement.flowReviewed') }}</span><b>←</b><span>{{ i18n.t('workforce.ui.settlement.flowApproved') }}</span><b>←</b><span>{{ i18n.t('workforce.ui.settlement.flowPosted') }}</span><b>←</b><span>{{ i18n.t('workforce.ui.settlement.flowLocked') }}</span>
      </div>

      @if (pageError()) { <div class="alert error">{{ pageError() }}</div> }
      @if (workforceService.loading()) { <div class="alert">{{ i18n.t('workforce.ui.settlement.loading') }}</div> }

      <div class="card table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ i18n.t('workforce.ui.settlement.period') }}</th>
              <th>{{ i18n.t('workforce.ui.status') }}</th>
              <th>{{ i18n.t('workforce.ui.settlement.lastCalculation') }}</th>
              <th>{{ i18n.t('workforce.ui.settlement.version') }}</th>
              <th>{{ i18n.t('workforce.ui.settlement.summary') }}</th>
              <th>{{ i18n.t('workforce.ui.settlement.contractorSettlements') }}</th>
              <th>{{ i18n.t('workforce.ui.settlement.warnings') }}</th>
              <th>{{ i18n.t('workforce.ui.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            @for (period of workforceService.settlementPeriods(); track period.id) {
              <tr [class.stale]="period.needsRecalculation">
                <td>
                  <strong>{{ period.periodCode }}</strong>
                  <small>{{ period.startDate }} ← {{ period.endDate }} · {{ cycleLabel(period.cycleType) }}</small>
                </td>
                <td>
                  <span class="badge" [class]="'badge ' + period.status.toLowerCase()">{{ statusLabel(period.status) }}</span>
                  @if (period.needsRecalculation) { <small class="stale-note">{{ i18n.t('workforce.ui.settlement.needsRecalc') }}</small> }
                </td>
                <td>
                  @if (period.lastCalculatedAt) {
                    <strong>{{ period.lastCalculatedAt | date:'yyyy-MM-dd HH:mm' }}</strong>
                    <small>{{ period.lastCalculatedBy }}</small>
                  } @else { — }
                  @if (period.lastCalculationError) {
                    <small class="failure">{{ i18n.t('workforce.ui.settlement.lastCalculationFailed', { detail: period.lastCalculationError }) }}</small>
                  }
                </td>
                <td>v{{ period.calculationVersion }}</td>
                <td>
                  <span>{{ i18n.t('workforce.ui.settlement.recordsCount', { count: period.resultRecordCount }) }}</span>
                  <small>{{ i18n.t('workforce.ui.settlement.summaryDetails', { gross: (period.resultGrossAmount | number:'1.2-2') ?? '0', deductions: (period.resultDeductions | number:'1.2-2') ?? '0', advances: (period.resultAdvances | number:'1.2-2') ?? '0', net: (period.resultNetAmount | number:'1.2-2') ?? '0' }) }}</small>
                </td>
                <td>
                  <button type="button" class="btn secondary link-btn-full" [disabled]="period.calculationVersion === 0" (click)="openContractorSettlementsModal(period)">
                    {{ i18n.t('workforce.ui.settlement.contractorSettlementsButton') }}
                  </button>
                </td>
                <td>
                  <button type="button" class="link-btn" (click)="showIssues(period)">
                    ⚠ {{ period.resultWarningCount }} · ⛔ {{ period.resultErrorCount }}
                  </button>
                </td>
                <td class="actions">
                  <button type="button" class="btn secondary" [disabled]="calculatingId() === period.id || period.status === 'APPROVED' || period.status === 'LOCKED'" (click)="calculatePeriod(period)">
                    {{ calculatingId() === period.id ? i18n.t('workforce.ui.settlement.calculating') : i18n.t('workforce.ui.settlement.recalculate') }}
                  </button>
                  @if (period.status === 'CALCULATED' && !period.needsRecalculation) {
                    <button type="button" class="btn" (click)="reviewPeriod(period)">{{ i18n.t('workforce.ui.settlement.review') }}</button>
                  }
                  @if (period.status === 'REVIEWED' && !period.needsRecalculation && period.resultErrorCount === 0) {
                    <button type="button" class="btn primary" (click)="approvePeriod(period)">{{ i18n.t('workforce.ui.settlement.approve') }}</button>
                  }
                  @if (period.status === 'APPROVED') {
                    <button type="button" class="btn danger" (click)="lockPeriod(period)">{{ i18n.t('workforce.ui.settlement.lock') }}</button>
                  }
                  <button type="button" class="btn success" [disabled]="period.calculationVersion === 0" (click)="exportExcel(period)">{{ i18n.t('workforce.ui.exportExcel') }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="empty">{{ i18n.t('workforce.ui.settlement.empty') }}</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Contractor Settlements List Modal for Period -->
      <app-modal-dialog [isOpen]="contractorListOpen()" [title]="i18n.t('workforce.ui.settlement.contractorsForPeriod', { code: (selectedPeriod()?.periodCode || '') })" size="wide" (close)="contractorListOpen.set(false)">
        @if (loadingContractorSettlements()) {
          <div class="alert">{{ i18n.t('workforce.ui.settlement.loadingContractors') }}</div>
        } @else {
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ i18n.t('workforce.ui.contractor') }}</th>
                <th>{{ i18n.t('workforce.ui.contractors.accountingModel') }}</th>
                <th>{{ i18n.t('workforce.ui.settlement.workerGross') }}</th>
                <th>{{ i18n.t('workforce.ui.settlement.serviceFee') }}</th>
                <th>{{ i18n.t('workforce.ui.settlement.netPayable') }}</th>
                <th>{{ i18n.t('workforce.ui.settlement.paid') }}</th>
                <th>{{ i18n.t('workforce.ui.status') }}</th>
                <th>{{ i18n.t('workforce.ui.settlement.action') }}</th>
              </tr>
            </thead>
            <tbody>
              @for (cs of contractorSettlements(); track cs.id) {
                <tr>
                  <td><strong>{{ cs.contractorName }}</strong></td>
                  <td>{{ cs.accountingModel }}</td>
                  <td>{{ cs.workersNetTotal | number:'1.2-2' }}</td>
                  <td>{{ cs.commissionAmount | number:'1.2-2' }}</td>
                  <td><strong>{{ cs.netPayable | number:'1.2-2' }}</strong></td>
                  <td>{{ cs.paidAmount | number:'1.2-2' }}</td>
                  <td>
                    <span class="badge" [class.posted]="cs.status === 'POSTED'" [class.paid]="cs.status === 'PAID'">
                      {{ settlementStatusLabel(cs.status) }}
                    </span>
                  </td>
                  <td>
                    <button type="button" class="btn primary" (click)="openDetailModal(cs)">
                      {{ i18n.t('workforce.ui.settlement.details') }}
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="8" class="empty">{{ i18n.t('workforce.ui.settlement.noContractors') }}</td></tr>
              }
            </tbody>
          </table>
        }
        <div modal-actions>
          <button type="button" class="btn secondary" (click)="contractorListOpen.set(false)">{{ i18n.t('workforce.ui.close') }}</button>
        </div>
      </app-modal-dialog>

      <!-- Contractor Settlement Single Detail Modal -->
      <app-contractor-settlement-detail-modal
        [isOpen]="detailModalOpen()"
        [settlement]="selectedContractorSettlement()"
        (close)="detailModalOpen.set(false)"
        (updated)="onSettlementUpdated($event)">
      </app-contractor-settlement-detail-modal>

      <!-- Calculation Result Modal -->
      <app-modal-dialog [isOpen]="summaryOpen()" [title]="i18n.t('workforce.ui.settlement.resultTitle')" size="wide" (close)="summaryOpen.set(false)">
        @if (calculationError()) {
          <div class="alert error">
            <strong>{{ i18n.t('workforce.ui.settlement.runFailed') }}</strong>
            <span>{{ calculationError() }}</span>
            <small>{{ i18n.t('workforce.ui.settlement.previousKept') }}</small>
          </div>
        }
        @if (summary(); as result) {
          <div class="run-meta">
            <span>{{ i18n.t('workforce.ui.settlement.runSuccess') }}</span>
            <span>{{ i18n.t('workforce.ui.settlement.version') }} v{{ result.calculationVersion }}</span>
            <span>{{ result.executedAt | date:'yyyy-MM-dd HH:mm:ss' }}</span>
            <span>{{ i18n.t('workforce.ui.settlement.by') }} {{ result.executedBy }}</span>
          </div>
          <div class="summary-grid">
            <article><small>{{ i18n.t('workforce.ui.settlement.records') }}</small><strong>{{ result.totalWorkers }}</strong></article>
            <article><small>{{ i18n.t('workforce.ui.settlement.gross') }}</small><strong>{{ result.grossWorkersAmount | number:'1.2-2' }}</strong></article>
            <article><small>{{ i18n.t('workforce.ui.settlement.deductions') }}</small><strong>{{ result.totalDeductions | number:'1.2-2' }}</strong></article>
            <article><small>{{ i18n.t('workforce.ui.settlement.advances') }}</small><strong>{{ result.totalAdvanceDeductions | number:'1.2-2' }}</strong></article>
            <article><small>{{ i18n.t('workforce.ui.settlement.workerNet') }}</small><strong>{{ result.netWorkersAmount | number:'1.2-2' }}</strong></article>
            <article><small>{{ i18n.t('workforce.ui.settlement.contractorNet') }}</small><strong>{{ result.netContractorsPayable | number:'1.2-2' }}</strong></article>
          </div>
        }
        @if (issues().length) {
          <h3 id="settlement-issues">{{ i18n.t('workforce.ui.settlement.issues') }} ({{ issues().length }})</h3>
          <table class="data-table">
            <thead><tr><th>{{ i18n.t('workforce.ui.worker') }}</th><th>{{ i18n.t('workforce.ui.type') }}</th><th>{{ i18n.t('workforce.ui.settlement.problem') }}</th><th>{{ i18n.t('workforce.ui.settlement.action') }}</th></tr></thead>
            <tbody>
              @for (issue of issues(); track issue.id) {
                <tr>
                  <td>{{ issue.workerName || issue.workerId || '—' }}</td>
                  <td>{{ issue.severity === 'ERROR' ? i18n.t('workforce.ui.settlement.error') : i18n.t('workforce.ui.settlement.warning') }}</td>
                  <td>{{ issue.message }}</td>
                  <td><a href="/workforce/workers">{{ i18n.t('workforce.ui.settlement.openWorkers') }}</a></td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="empty">{{ i18n.t('workforce.ui.settlement.noIssues') }}</p>
        }
        <div modal-actions><button type="button" class="btn primary" (click)="summaryOpen.set(false)">{{ i18n.t('workforce.ui.close') }}</button></div>
      </app-modal-dialog>

      <!-- Create Period Modal -->
      <app-modal-dialog [isOpen]="createOpen()" [title]="i18n.t('workforce.ui.settlement.createTitle')" [preventOutsideClose]="true" (close)="createOpen.set(false)">
        <form class="form" (ngSubmit)="savePeriod()">
          <label>{{ i18n.t('workforce.ui.settlement.periodCode') }}<input [(ngModel)]="createForm.periodCode" name="periodCode" required /></label>
          <label>{{ i18n.t('workforce.ui.settlement.startDate') }}<input type="date" [(ngModel)]="createForm.startDate" name="startDate" required /></label>
          <label>{{ i18n.t('workforce.ui.settlement.endDate') }}<input type="date" [(ngModel)]="createForm.endDate" name="endDate" required /></label>
        </form>
        <div modal-actions>
          <button type="button" class="btn primary" (click)="savePeriod()">{{ i18n.t('workforce.ui.settlement.create') }}</button>
          <button type="button" class="btn secondary" (click)="createOpen.set(false)">{{ i18n.t('workforce.ui.cancel') }}</button>
        </div>
      </app-modal-dialog>
    </section>
  `,
  styles: [`
    .workforce-container{padding: 1.5rem;display: grid;gap: 1.25rem}.page-header{display: flex;justify-content: space-between;gap: 1rem;align-items: center}.page-header h1{margin: .2rem 0}.page-header p,small{color: var(--muted)}.eyebrow{color: #b7791f;font-weight: 800}.state-flow{display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:.65rem;color:var(--secondary-text)}.state-flow span{display:inline-flex;align-items:center;min-height:32px;padding:.3rem .65rem;border:1px solid var(--line);border-radius:8px;background:var(--surface-muted);color:var(--ink);font-weight:700}.state-flow b{color:var(--gold);font-weight:800}.card{background: var(--surface);border: 1px solid var(--line);border-radius: 14px}.table-wrap{overflow: auto}.data-table{width: 100%;border-collapse: collapse;min-width: 1050px}.data-table th,.data-table td{padding: .75rem;border-bottom: 1px solid var(--line);text-align: start;vertical-align: top}.data-table td small{display: block;margin-top: .3rem}.actions{display: flex;gap: .35rem;flex-wrap: wrap}.btn{border: 0;border-radius: 8px;padding: .55rem .75rem;font-weight: 700;cursor: pointer;background: var(--surface-muted);color: #243247}.btn:disabled{opacity: .5;cursor:not-allowed}.primary{background: #b7791f;color: #fff}.secondary{background: var(--surface-muted)}.success{background: #dcfce7;color: var(--success)}.danger{background: var(--danger-soft);color: var(--danger)}.badge{display: inline-block;border-radius: 999px;padding: .25rem .6rem;background: var(--surface-muted)}.badge.calculated{background: #dbeafe;color: var(--secondary-text)}.badge.reviewed{background: #fef3c7;color: #92400e}.badge.approved,.badge.locked,.badge.posted,.badge.paid{background: #dcfce7;color: var(--success)}.stale{background: #fffaf0}.stale-note,.failure{color: #b45309!important}.failure{max-width: 280px}.alert{padding: .8rem;border-radius: 10px;background: var(--surface-muted);display: grid;gap: .25rem}.alert.error{background: var(--danger-soft);color: var(--danger)}.run-meta,.summary-grid{display: grid;grid-template-columns: repeat(4,minmax(0,1fr));gap: .75rem}.run-meta{margin-bottom: 1rem}.run-meta span,.summary-grid article{padding: .8rem;border: 1px solid var(--line);border-radius: 10px}.summary-grid article{display: grid;gap: .25rem}.summary-grid strong{font-size: 1.25rem}.link-btn{border: 0;background: transparent;color: #9a6700;text-decoration: underline;cursor: pointer}.link-btn-full{color: #b7791f;font-weight: 800}.form{display: grid;gap: 1rem}.form label{display: grid;gap: .35rem;font-weight: 700}.form input{padding: .65rem;border: 1px solid var(--line);border-radius: 8px}.empty{padding: 1rem;text-align: center;color: var(--muted)}@media(max-width: 900px){.page-header{align-items: stretch;flex-direction: column}.run-meta,.summary-grid{grid-template-columns: 1fr 1fr}}
  `]
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
