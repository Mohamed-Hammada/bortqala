import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CrmActivity,
  CrmChannelConfig,
  CrmConversation,
  CrmLead,
  CrmLeadStatus,
  CrmMessage,
  CrmSummary,
  CreateActivityRequest,
  SaveChannelConfigRequest,
  SaveLeadRequest,
  SendMessageRequest,
} from './crm.models';

@Injectable({
  providedIn: 'root',
})
export class CrmService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/crm';

  getSummary(): Observable<CrmSummary> {
    return this.http.get<CrmSummary>(`${this.baseUrl}/summary`);
  }

  listLeads(status?: CrmLeadStatus): Observable<CrmLead[]> {
    const params: { [key: string]: string } = {};
    if (status) params['status'] = status;
    return this.http.get<CrmLead[]>(`${this.baseUrl}/leads`, { params });
  }

  saveLead(request: SaveLeadRequest): Observable<CrmLead> {
    return this.http.post<CrmLead>(`${this.baseUrl}/leads`, request);
  }

  convertLeadToCustomer(leadId: string): Observable<CrmLead> {
    return this.http.post<CrmLead>(`${this.baseUrl}/leads/${leadId}/convert`, {});
  }

  listActivities(leadId: string): Observable<CrmActivity[]> {
    return this.http.get<CrmActivity[]>(`${this.baseUrl}/leads/${leadId}/activities`);
  }

  createActivity(request: CreateActivityRequest): Observable<CrmActivity> {
    return this.http.post<CrmActivity>(`${this.baseUrl}/activities`, request);
  }

  completeActivity(id: string): Observable<CrmActivity> {
    return this.http.post<CrmActivity>(`${this.baseUrl}/activities/${id}/complete`, {});
  }

  listChannelConfigs(): Observable<CrmChannelConfig[]> {
    return this.http.get<CrmChannelConfig[]>(`${this.baseUrl}/channels`);
  }

  saveChannelConfig(request: SaveChannelConfigRequest): Observable<CrmChannelConfig> {
    return this.http.post<CrmChannelConfig>(`${this.baseUrl}/channels`, request);
  }

  listConversations(): Observable<CrmConversation[]> {
    return this.http.get<CrmConversation[]>(`${this.baseUrl}/conversations`);
  }

  getConversationMessages(id: string): Observable<CrmMessage[]> {
    return this.http.get<CrmMessage[]>(`${this.baseUrl}/conversations/${id}/messages`);
  }

  sendMessage(conversationId: string, request: SendMessageRequest): Observable<CrmMessage> {
    return this.http.post<CrmMessage>(`${this.baseUrl}/conversations/${conversationId}/messages`, request);
  }
}
