import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WorkforceService } from '../../data-access/workforce.service';
import { I18nService } from '../../../../core/i18n.service';

@Component({
  selector: 'app-contractor-accounts',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">{{ i18n.t('workforce.ui.contractorAccounts.eyebrow') }}</span>
          <h1>{{ i18n.t('workforce.ui.contractorAccounts.title') }}</h1>
        </div>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.contractor') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.accountingModel') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.workerNetTotal') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.feeOrRate') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.totalPayable') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.paid') }}</th>
              <th>{{ i18n.t('workforce.ui.contractorAccounts.remaining') }}</th>
              <th>{{ i18n.t('workforce.ui.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of workforceService.contractors()">
              <td><strong>{{ c.name }}</strong> ({{ c.code }})</td>
              <td>{{ getModelLabel(c.accountingModel) }}</td>
              <td>0.00 {{ i18n.t('workforce.ui.currencyEgp') }}</td>
              <td>{{ c.feeValue || c.defaultDailyRate || 0 }} {{ i18n.t('workforce.ui.currencyEgp') }}</td>
              <td><strong>0.00 {{ i18n.t('workforce.ui.currencyEgp') }}</strong></td>
              <td>0.00 {{ i18n.t('workforce.ui.currencyEgp') }}</td>
              <td><span class="due-bal">0.00 {{ i18n.t('workforce.ui.currencyEgp') }}</span></td>
              <td>
                <button type="button" class="btn btn-sm btn-secondary" (click)="printReceipt(c)">{{ i18n.t('workforce.ui.contractorAccounts.printReceipt') }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: var(--ink); margin: 0.25rem 0 0 0; }
    .card { background: var(--surface); border-radius: 12px; border: 1px solid var(--line); padding: 1.25rem; }
    .data-table { width: 100%; border-collapse: collapse; text-align: start; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid var(--line); }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn-secondary { background: var(--line); color: var(--secondary-text); }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .due-bal { color: #d97706; font-weight: 700; }
  `]
})
export class ContractorAccountsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);

  ngOnInit() {
    this.workforceService.loadContractors().subscribe();
  }

  getModelLabel(model: string): string {
    const labels: Record<string, string> = {
      worker_net_total: 'workforce.ui.model.workerNetTotal',
      contractor_daily_rate: 'workforce.ui.model.contractorDailyRate',
      worker_cost_plus_fee: 'workforce.ui.model.workerCostPlusFee',
      fixed_period_amount: 'workforce.ui.model.fixedPeriodAmount'
    };
    return labels[model] ? this.i18n.t(labels[model]) : model;
  }

  printReceipt(c: any) {
    window.print();
  }
}
