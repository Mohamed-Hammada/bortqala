import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { HelpdeskCategory, Ticket, TicketMessage, TicketListResponse, KbArticle } from './helpdesk.models';

@Injectable({ providedIn: 'root' })
export class HelpdeskService {
  private readonly http = inject(HttpClient);

  async listCategories(): Promise<HelpdeskCategory[]> {
    return firstValueFrom(this.http.get<HelpdeskCategory[]>('/api/v1/helpdesk/categories'));
  }

  async createCategory(payload: { nameAr: string; nameEn: string; slaFirstResponseHours: number; slaResolutionHours: number }): Promise<HelpdeskCategory> {
    return firstValueFrom(this.http.post<HelpdeskCategory>('/api/v1/helpdesk/categories', payload));
  }

  async listTickets(status?: string, assigneeUserId?: string): Promise<TicketListResponse> {
    const params: Record<string, string> = {};
    if (status) params['status'] = status;
    if (assigneeUserId) params['assigneeUserId'] = assigneeUserId;
    return firstValueFrom(this.http.get<TicketListResponse>('/api/v1/helpdesk/tickets', { params }));
  }

  async createTicket(payload: { categoryId: string; title: string; description: string; priority?: string }): Promise<Ticket> {
    return firstValueFrom(this.http.post<Ticket>('/api/v1/helpdesk/tickets', payload));
  }

  async getTicket(id: string): Promise<Ticket> {
    return firstValueFrom(this.http.get<Ticket>(`/api/v1/helpdesk/tickets/${id}`));
  }

  async assignTicket(id: string, assigneeUserId: string): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/helpdesk/tickets/${id}/assign`, { assigneeUserId }));
  }

  async transitionTicket(id: string, status: string): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/helpdesk/tickets/${id}/transition`, { status }));
  }

  async addMessage(ticketId: string, body: string, internalNote: boolean): Promise<TicketMessage> {
    return firstValueFrom(this.http.post<TicketMessage>(`/api/v1/helpdesk/tickets/${ticketId}/messages`, { body, internalNote }));
  }

  async listMessages(ticketId: string, includeInternal: boolean): Promise<TicketMessage[]> {
    return firstValueFrom(this.http.get<TicketMessage[]>(`/api/v1/helpdesk/tickets/${ticketId}/messages`, { params: { includeInternal: String(includeInternal) } }));
  }

  async listKbArticles(q?: string): Promise<KbArticle[]> {
    const params: Record<string, string> = {};
    if (q) params['q'] = q;
    return firstValueFrom(this.http.get<KbArticle[]>('/api/v1/kb/articles', { params }));
  }

  async createKbArticle(payload: { titleAr: string; titleEn: string; bodyAr: string; bodyEn: string; tags?: string }): Promise<KbArticle> {
    return firstValueFrom(this.http.post<KbArticle>('/api/v1/kb/articles', payload));
  }

  async publishKbArticle(id: string): Promise<KbArticle> {
    return firstValueFrom(this.http.post<KbArticle>(`/api/v1/kb/articles/${id}/publish`, {}));
  }

  async voteKbArticle(id: string, up: boolean): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/kb/articles/${id}/vote`, { up }));
  }

  async createFromTicket(ticketId: string, titleAr: string, titleEn: string): Promise<KbArticle> {
    return firstValueFrom(this.http.post<KbArticle>(`/api/v1/kb/articles/from-ticket/${ticketId}`, { titleAr, titleEn }));
  }
}
