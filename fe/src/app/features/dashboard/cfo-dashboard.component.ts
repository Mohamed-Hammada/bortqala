import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-cfo-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cfo-grid">
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'إجمالي السيولة والنقدية' : 'Total Cash & Liquidity' }}</span>
        <span class="kpi-value">4,820,500.00 EGP</span>
        <span class="kpi-trend positive">+12.4% vs last month</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'حسابات العملاء (AR)' : 'Accounts Receivable (AR)' }}</span>
        <span class="kpi-value">1,450,200.00 EGP</span>
        <span class="kpi-subtext">Overdue > 60 days: 120,000 EGP</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'حسابات الموردين (AP)' : 'Accounts Payable (AP)' }}</span>
        <span class="kpi-value">890,400.00 EGP</span>
        <span class="kpi-subtext">Due this week: 140,000 EGP</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'هامش الربح الإجمالي' : 'Gross Profit Margin' }}</span>
        <span class="kpi-value">34.8%</span>
        <span class="kpi-trend positive">On Target (35%)</span>
      </div>
    </div>
  `,
  styles: [`
    .cfo-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    .kpi-card {
      background: var(--surface-primary, #ffffff);
      border: 1px solid var(--border-color, #e2e8f0);
      border-radius: 10px;
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    .kpi-label { font-size: 0.85rem; color: var(--text-muted, #64748b); font-weight: 500; }
    .kpi-value { font-size: 1.4rem; font-weight: 700; color: var(--text-color, #0f172a); }
    .kpi-trend { font-size: 0.8rem; font-weight: 600; &.positive { color: #16a34a; } }
    .kpi-subtext { font-size: 0.75rem; color: var(--text-muted, #94a3b8); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CfoDashboardComponent {
  readonly i18n = inject(I18nService);
}
