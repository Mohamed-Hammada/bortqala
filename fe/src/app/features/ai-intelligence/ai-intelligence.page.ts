import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiIntelligenceService } from './ai-intelligence.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-ai-intelligence-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-intelligence.page.html',
  styleUrls: ['./ai-intelligence.page.scss'],
})
export class AiIntelligencePageComponent implements OnInit {
  readonly ai = inject(AiIntelligenceService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'cashflow' | 'anomalies' | 'demand' | 'collections' | 'nlquery'>('cashflow');
  readonly selectedMonths = signal<number>(3);
  readonly nlQuestion = signal<string>('');

  async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  async setTab(tab: 'cashflow' | 'anomalies' | 'demand' | 'collections' | 'nlquery'): Promise<void> {
    this.activeTab.set(tab);
    await this.loadData();
  }

  async loadData(): Promise<void> {
    const tab = this.activeTab();
    if (tab === 'cashflow') {
      await this.ai.loadCashFlowForecast(this.selectedMonths());
    } else if (tab === 'anomalies') {
      await this.ai.loadExpenseAnomalies();
    } else if (tab === 'demand') {
      await this.ai.loadDemandForecast();
    } else if (tab === 'collections') {
      await this.ai.loadCollectionsRisk();
    }
  }

  async onMonthsChange(months: number): Promise<void> {
    this.selectedMonths.set(months);
    await this.ai.loadCashFlowForecast(months);
  }

  async askQuestion(): Promise<void> {
    const q = this.nlQuestion().trim();
    if (!q) return;
    await this.ai.askNlQuestion(q);
  }
}
