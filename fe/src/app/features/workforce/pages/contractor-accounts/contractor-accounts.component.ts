import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WorkforceService } from '../../data-access/workforce.service';

@Component({
  selector: 'app-contractor-accounts',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">حسابات المقاولين</span>
          <h1>كشوف استلام المقاولين وأرصدة المستحقات</h1>
        </div>
      </header>

      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>المقاول</th>
              <th>نموذج الحساب</th>
              <th>مجموع صافي العمال</th>
              <th>العمولة / السعر المستقل</th>
              <th>إجمالي المستحق</th>
              <th>المسدد</th>
              <th>المتبقي للصرف</th>
              <th>إجراءات</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of workforceService.contractors()">
              <td><strong>{{ c.name }}</strong> ({{ c.code }})</td>
              <td>{{ getModelLabel(c.accountingModel) }}</td>
              <td>0.00 ج.م</td>
              <td>{{ c.feeValue || c.defaultDailyRate || 0 }} ج.م</td>
              <td><strong>0.00 ج.م</strong></td>
              <td>0.00 ج.م</td>
              <td><span class="due-bal">0.00 ج.م</span></td>
              <td>
                <button type="button" class="btn btn-sm btn-secondary" (click)="printReceipt(c)">طباعة كشف الاستلام</button>
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
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th, .data-table td { padding: 0.75rem 1rem; border-bottom: 1px solid #e2e8f0; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; }
    .due-bal { color: #d97706; font-weight: 700; }
  `]
})
export class ContractorAccountsComponent implements OnInit {
  workforceService = inject(WorkforceService);

  ngOnInit() {
    this.workforceService.loadContractors().subscribe();
  }

  getModelLabel(model: string): string {
    switch (model) {
      case 'worker_net_total': return 'مجموع صافي العمال';
      case 'contractor_daily_rate': return 'سعر تعاقد مستقل';
      case 'worker_cost_plus_fee': return 'تكلفة العمال + عمولة';
      case 'fixed_period_amount': return 'مبلغ ثابت للفترة';
      default: return model;
    }
  }

  printReceipt(c: any) {
    window.print();
  }
}
