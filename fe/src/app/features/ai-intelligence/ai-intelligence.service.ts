import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  CashFlowForecastDto,
  ExpenseAnomalyDto,
  DemandForecastDto,
  CollectionsRiskDto,
  NlQueryResponseDto,
} from './ai-intelligence.models';

@Injectable({
  providedIn: 'root',
})
export class AiIntelligenceService {
  private readonly http = inject(HttpClient);

  readonly forecast = signal<CashFlowForecastDto | null>(null);
  readonly anomalies = signal<ExpenseAnomalyDto[]>([]);
  readonly demand = signal<DemandForecastDto[]>([]);
  readonly collectionsRisk = signal<CollectionsRiskDto[]>([]);
  readonly nlQueryResult = signal<NlQueryResponseDto | null>(null);
  readonly loading = signal(false);

  async loadCashFlowForecast(months = 3): Promise<CashFlowForecastDto> {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.get<CashFlowForecastDto>(`/api/v1/analytics/cashflow-forecast?months=${months}`)
      );
      this.forecast.set(data);
      return data;
    } finally {
      this.loading.set(false);
    }
  }

  async loadExpenseAnomalies(): Promise<ExpenseAnomalyDto[]> {
    const list = await firstValueFrom(
      this.http.get<ExpenseAnomalyDto[]>('/api/v1/analytics/expense-anomalies')
    );
    this.anomalies.set(list);
    return list;
  }

  async loadDemandForecast(): Promise<DemandForecastDto[]> {
    const list = await firstValueFrom(
      this.http.get<DemandForecastDto[]>('/api/v1/analytics/demand-forecast')
    );
    this.demand.set(list);
    return list;
  }

  async loadCollectionsRisk(): Promise<CollectionsRiskDto[]> {
    const list = await firstValueFrom(
      this.http.get<CollectionsRiskDto[]>('/api/v1/analytics/collections-risk')
    );
    this.collectionsRisk.set(list);
    return list;
  }

  async askNlQuestion(question: string): Promise<NlQueryResponseDto> {
    this.loading.set(true);
    try {
      const res = await firstValueFrom(
        this.http.post<NlQueryResponseDto>('/api/v1/analytics/nl-query', { question })
      );
      this.nlQueryResult.set(res);
      return res;
    } finally {
      this.loading.set(false);
    }
  }
}
