import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { PaymentLink, CreateLinkPayload, PublicPagePayload } from './payment-link.models';

@Injectable({ providedIn: 'root' })
export class PaymentLinkService {
  private readonly http = inject(HttpClient);

  async listLinks(): Promise<PaymentLink[]> {
    const res = await firstValueFrom(this.http.get<{ links: PaymentLink[] }>('/api/v1/finance/payment-links'));
    return res.links ?? [];
  }

  async createLink(payload: CreateLinkPayload): Promise<PaymentLink> {
    return firstValueFrom(this.http.post<PaymentLink>('/api/v1/finance/payment-links', payload));
  }

  async cancelLink(id: string): Promise<void> {
    await firstValueFrom(this.http.post(`/api/v1/finance/payment-links/${id}/cancel`, {}));
  }

  async getGatewayConfig(): Promise<{ enabled: boolean }> {
    return firstValueFrom(this.http.get<{ enabled: boolean }>('/api/v1/finance/payment-links/config'));
  }

  async getPublicPage(token: string): Promise<PublicPagePayload> {
    return firstValueFrom(this.http.get<PublicPagePayload>(`/p/${token}`));
  }
}
