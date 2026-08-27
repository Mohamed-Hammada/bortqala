import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Campaign, CampaignRecipient, Survey, SurveyQuestion } from './marketing.models';
import { ReportDataset, SavedReport, ReportColumn } from './marketing.models';

@Injectable({ providedIn: 'root' })
export class MarketingService {
  private readonly http = inject(HttpClient);

  async listCampaigns(): Promise<Campaign[]> {
    return firstValueFrom(this.http.get<Campaign[]>('/api/v1/marketing/campaigns'));
  }

  async createCampaign(payload: { name: string; channel: string; subject: string; bodyAr: string; bodyEn: string }): Promise<Campaign> {
    return firstValueFrom(this.http.post<Campaign>('/api/v1/marketing/campaigns', payload));
  }

  async getCampaign(id: string): Promise<Campaign> {
    return firstValueFrom(this.http.get<Campaign>(`/api/v1/marketing/campaigns/${id}`));
  }

  async sendCampaign(id: string): Promise<Campaign> {
    return firstValueFrom(this.http.post<Campaign>(`/api/v1/marketing/campaigns/${id}/send`, {}));
  }

  async addRecipients(id: string, recipients: { targetRef: string; email: string; phone: string; locale: string }[]): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/marketing/campaigns/${id}/recipients`, { recipients }));
  }

  async listRecipients(id: string): Promise<CampaignRecipient[]> {
    return firstValueFrom(this.http.get<CampaignRecipient[]>(`/api/v1/marketing/campaigns/${id}/recipients`));
  }

  async abortCampaign(id: string): Promise<Campaign> {
    return firstValueFrom(this.http.post<Campaign>(`/api/v1/marketing/campaigns/${id}/abort`, {}));
  }

  async listSurveys(): Promise<Survey[]> {
    return firstValueFrom(this.http.get<Survey[]>('/api/v1/marketing/surveys'));
  }

  async createSurvey(title: string, description: string): Promise<Survey> {
    return firstValueFrom(this.http.post<Survey>('/api/v1/marketing/surveys', { title, description }));
  }

  async getSurvey(id: string): Promise<Survey> {
    return firstValueFrom(this.http.get<Survey>(`/api/v1/marketing/surveys/${id}`));
  }

  async addQuestion(surveyId: string, text: string, type: string, options: string, order: number, required: boolean): Promise<SurveyQuestion> {
    return firstValueFrom(this.http.post<SurveyQuestion>(`/api/v1/marketing/surveys/${surveyId}/questions`, { questionText: text, questionType: type, options, sortOrder: order, required }));
  }

  async listQuestions(surveyId: string): Promise<SurveyQuestion[]> {
    return firstValueFrom(this.http.get<SurveyQuestion[]>(`/api/v1/marketing/surveys/${surveyId}/questions`));
  }

  async submitResponse(surveyId: string, respondentToken: string, answers: { questionId: string; answer: string }[]): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/marketing/surveys/${surveyId}/respond`, { respondentToken, answers }));
  }

  async getResults(surveyId: string): Promise<Record<string, unknown>> {
    return firstValueFrom(this.http.get<Record<string, unknown>>(`/api/v1/marketing/surveys/${surveyId}/results`));
  }

  async listDatasets(): Promise<ReportDataset[]> {
    return firstValueFrom(this.http.get<ReportDataset[]>('/api/v1/report-builder/datasets'));
  }

  async runQuery(payload: { datasetCode: string; dimensions: string[]; measures: string[]; limit: number }): Promise<{ columns: ReportColumn[]; rows: Record<string, unknown>[]; totalRows: number }> {
    return firstValueFrom(this.http.post<{ columns: ReportColumn[]; rows: Record<string, unknown>[]; totalRows: number }>('/api/v1/report-builder/query', payload));
  }

  async listSavedReports(): Promise<SavedReport[]> {
    return firstValueFrom(this.http.get<SavedReport[]>('/api/v1/report-builder/reports'));
  }

  async saveReport(name: string, datasetCode: string, definition: string): Promise<SavedReport> {
    return firstValueFrom(this.http.post<SavedReport>('/api/v1/report-builder/reports', { name, datasetCode, definition }));
  }

  async deleteReport(id: string): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/v1/report-builder/reports/${id}`));
  }
}
