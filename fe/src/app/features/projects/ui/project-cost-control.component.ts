import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { CostControlService } from '../data-access/cost-control.service';
import {
  CostControlSummary,
  ProjectBudgetVersion,
  ProjectCostLedgerEntry,
  ProjectForecastEac,
  CreateBudgetVersionRequest,
  RecordCostLedgerEntryRequest,
  CostCategory,
  CostLedgerEntryType,
} from '../models/cost-control.models';
import { ProjectResponse, WbsNodeResponse } from '../models/project.models';
import { ProjectBudgetModalComponent } from './project-budget-modal.component';
import { ProjectCostLedgerModalComponent } from './project-cost-ledger-modal.component';

@Component({
  selector: 'app-project-cost-control',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    ProjectBudgetModalComponent,
    ProjectCostLedgerModalComponent
  ],
  templateUrl: './project-cost-control.component.html',
  styleUrls: ['./project-cost-control.component.scss']
})
export class ProjectCostControlComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly costControlService = inject(CostControlService);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);

  @Input({ required: true }) projectId!: string;
  @Input() project: ProjectResponse | null = null;
  @Input() wbsNodes: WbsNodeResponse[] = [];

  readonly activeTab = signal<'profitability' | 'wbsVariance' | 'forecastEac' | 'costLedger'>('profitability');
  readonly loading = signal<boolean>(false);

  readonly summary = signal<CostControlSummary | null>(null);
  readonly budgetVersions = signal<ProjectBudgetVersion[]>([]);
  readonly costLedgerEntries = signal<ProjectCostLedgerEntry[]>([]);
  readonly forecastEacList = signal<ProjectForecastEac[]>([]);

  readonly ledgerFilterType = signal<string>('ALL');
  readonly showBudgetModal = signal<boolean>(false);
  readonly showLedgerModal = signal<boolean>(false);

  readonly filteredLedgerEntries = computed(() => {
    const entries = this.costLedgerEntries();
    const filter = this.ledgerFilterType();
    if (filter === 'ALL') return entries;
    return entries.filter(e => e.entryType === filter);
  });

  ngOnInit(): void {
    if (this.projectId) {
      this.loadData();
    }
  }

  getCategoryLabel(cat: CostCategory): string {
    switch (cat) {
      case 'LABOR': return this.i18n.t('costControl.catLabor');
      case 'EQUIPMENT': return this.i18n.t('costControl.catEquipment');
      case 'MATERIAL': return this.i18n.t('costControl.catMaterial');
      case 'SUBCONTRACTOR': return this.i18n.t('costControl.catSubcontractor');
      case 'OVERHEAD': return this.i18n.t('costControl.catOverhead');
      case 'CONTINGENCY': return this.i18n.t('costControl.catContingency');
    }
  }

  getEntryTypeLabel(type: CostLedgerEntryType): string {
    switch (type) {
      case 'COMMITTED': return this.i18n.t('costControl.typeCommitted');
      case 'ACTUAL': return this.i18n.t('costControl.typeActual');
      case 'REVENUE': return this.i18n.t('costControl.typeRevenue');
      default: return type;
    }
  }

  loadData(): void {
    this.loading.set(true);
    this.costControlService.getSummary(this.projectId).subscribe({
      next: sum => {
        this.summary.set(sum);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    this.costControlService.listBudgetVersions(this.projectId).subscribe({
      next: versions => this.budgetVersions.set(versions)
    });

    this.costControlService.listCostLedgerEntries(this.projectId).subscribe({
      next: entries => this.costLedgerEntries.set(entries)
    });

    this.costControlService.listForecastEac(this.projectId).subscribe({
      next: forecasts => this.forecastEacList.set(forecasts)
    });
  }

  onSaveBudgetVersion(req: CreateBudgetVersionRequest): void {
    this.costControlService.createBudgetVersion(this.projectId, req).subscribe({
      next: () => {
        this.showBudgetModal.set(false);
        this.notification.success(this.i18n.t('costControl.budgetVersionSaved'));
        this.loadData();
      }
    });
  }

  onApproveBudgetVersion(version: ProjectBudgetVersion): void {
    this.confirm.confirm(
      this.i18n.t('costControl.approveBudgetVersion')
    ).then(ok => {
      if (ok) {
        this.costControlService.approveBudgetVersion(this.projectId, version.id).subscribe({
          next: () => {
            this.notification.success(this.i18n.t('costControl.approveBudgetVersion'));
            this.loadData();
          }
        });
      }
    });
  }

  onRecordLedgerEntry(req: RecordCostLedgerEntryRequest): void {
    this.costControlService.recordCostLedgerEntry(this.projectId, req).subscribe({
      next: () => {
        this.showLedgerModal.set(false);
        this.notification.success(this.i18n.t('costControl.costLedgerSaved'));
        this.loadData();
      }
    });
  }

  onSaveForecastEac(item: ProjectForecastEac): void {
    this.costControlService.updateForecastEac(this.projectId, {
      wbsNodeId: item.wbsNodeId || item.id,
      estimateToComplete: item.estimateToComplete,
      notes: item.notes
    }).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('costControl.saveForecastSuccess'));
        this.loadData();
      }
    });
  }
}
