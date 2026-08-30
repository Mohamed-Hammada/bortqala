import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-warehouse-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="warehouse-grid">
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'تقييم المخزون الحالي' : 'On-Hand Stock Valuation' }}</span>
        <span class="kpi-value">2,340,100.00 EGP</span>
        <span class="kpi-subtext">Across 4 central warehouses</span>
      </div>
      <div class="kpi-card warning">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'أصناف قاربت على النفاد' : 'Low Stock Alerts' }}</span>
        <span class="kpi-value">8 Items</span>
        <span class="kpi-subtext">Below reorder minimum point</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'أوامر استلام قيد الانتظار' : 'Pending GRN Receipts' }}</span>
        <span class="kpi-value">5 Shipments</span>
        <span class="kpi-subtext">Expected in 48 hours</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'كميات محجوزة للمبيعات' : 'Reserved for Orders' }}</span>
        <span class="kpi-value">420 Units</span>
        <span class="kpi-subtext">Ready for dispatch</span>
      </div>
    </div>
  `,
  styles: [`
    .warehouse-grid {
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
      &.warning {
        border-color: #fde047;
        background: #fefce8;
      }
    }
    .kpi-label { font-size: 0.85rem; color: var(--text-muted, #64748b); font-weight: 500; }
    .kpi-value { font-size: 1.4rem; font-weight: 700; color: var(--text-color, #0f172a); }
    .kpi-subtext { font-size: 0.75rem; color: var(--text-muted, #94a3b8); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WarehouseDashboardComponent {
  readonly i18n = inject(I18nService);
}
