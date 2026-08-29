import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-project-manager-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pm-grid">
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'مؤشر أداء التكلفة (CPI)' : 'Cost Performance Index (CPI)' }}</span>
        <span class="kpi-value">1.14</span>
        <span class="kpi-trend positive">✓ Under Budget (CPI > 1.0)</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'مؤشر أداء الجدول (SPI)' : 'Schedule Performance (SPI)' }}</span>
        <span class="kpi-value">1.08</span>
        <span class="kpi-trend positive">✓ Ahead of Schedule</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'المستخلصات المعتمدة (IPC)' : 'Certified Claims (IPC)' }}</span>
        <span class="kpi-value">3,200,000.00 EGP</span>
        <span class="kpi-subtext">Retention (10%): 320,000 EGP</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">{{ i18n.locale() === 'ar-EG' ? 'الربحية التقديرية (EAC)' : 'Project Margin (EAC)' }}</span>
        <span class="kpi-value">18.2%</span>
        <span class="kpi-trend positive">Target: 15.0%</span>
      </div>
    </div>
  `,
  styles: [`
    .pm-grid {
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
export class ProjectManagerDashboardComponent {
  readonly i18n = inject(I18nService);
}
