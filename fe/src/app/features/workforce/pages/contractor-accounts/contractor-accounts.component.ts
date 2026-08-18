import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WorkforceService } from '../../data-access/workforce.service';
import { I18nService } from '../../../../core/i18n.service';

@Component({
  selector: 'app-contractor-accounts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contractor-accounts.component.html',
  styleUrls: ['./contractor-accounts.component.scss']
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
