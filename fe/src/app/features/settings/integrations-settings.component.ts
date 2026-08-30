import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import {
  ApiKey,
  ApiKeyCreateResponse,
  ApiKeyCreateRequest,
  WebhookEndpoint,
  WebhookEndpointCreateRequest,
  WebhookDelivery,
} from '../settings/integrations.models';
import { formatDate } from '../../core/date';

@Component({
  selector: 'app-integrations-settings',
  standalone: true,
  templateUrl: './integrations-settings.component.html',
  styleUrl: './integrations-settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IntegrationsSettingsComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly keys = signal<ApiKey[]>([]);
  readonly endpoints = signal<WebhookEndpoint[]>([]);
  readonly deliveries = signal<WebhookDelivery[]>([]);
  readonly selectedEndpointId = signal<string | null>(null);
  readonly creatingKey = signal(false);
  readonly newKeyName = signal('');
  readonly newKeyScopes = signal('');
  readonly newKeyRateLimit = signal(120);
  readonly showKeyReveal = signal(false);
  readonly revealedKey = signal('');
  readonly creatingWebhook = signal(false);
  readonly newWebhookUrl = signal('');
  readonly newWebhookEvents = signal('');

  ngOnInit(): void {
    this.load();
  }

  async load() {
    this.loading.set(true);
    try {
      const [keys, endpoints] = await Promise.all([
        firstValueFrom(this.http.get<{ keys: ApiKey[] }>('/api/v1/platform/api-keys')),
        firstValueFrom(this.http.get<{ endpoints: WebhookEndpoint[] }>('/api/v1/platform/webhooks')),
      ]);
      this.keys.set(keys.keys);
      this.endpoints.set(endpoints.endpoints);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async createKey() {
    const name = this.newKeyName().trim();
    if (!name) return;
    this.creatingKey.set(true);
    try {
      const result = await firstValueFrom(
        this.http.post<ApiKeyCreateResponse>('/api/v1/platform/api-keys', {
          name,
          scopes: this.newKeyScopes(),
          rateLimitPerMin: this.newKeyRateLimit(),
        } as ApiKeyCreateRequest),
      );
      this.revealedKey.set(result.fullKey);
      this.showKeyReveal.set(true);
      this.newKeyName.set('');
      this.newKeyScopes.set('');
      this.newKeyRateLimit.set(120);
      await this.load();
      this.notification.success(this.i18n.t('integrations.keyCreated'));
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.creatingKey.set(false);
    }
  }

  async toggleKey(keyId: string, active: boolean) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/platform/api-keys/${keyId}/toggle?active=${active}`, {}));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async revokeKey(keyId: string) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/platform/api-keys/${keyId}/revoke`, {}));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async deleteKey(keyId: string) {
    try {
      await firstValueFrom(this.http.delete(`/api/v1/platform/api-keys/${keyId}`));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  copyKey() {
    navigator.clipboard.writeText(this.revealedKey());
    this.notification.success(this.i18n.t('integrations.copyKey'));
  }

  closeKeyReveal() {
    this.showKeyReveal.set(false);
    this.revealedKey.set('');
  }

  async createWebhook() {
    const url = this.newWebhookUrl().trim();
    if (!url) return;
    this.creatingWebhook.set(true);
    try {
      await firstValueFrom(
        this.http.post<WebhookEndpoint>('/api/v1/platform/webhooks', {
          url,
          events: this.newWebhookEvents(),
        } as WebhookEndpointCreateRequest),
      );
      this.newWebhookUrl.set('');
      this.newWebhookEvents.set('');
      await this.load();
      this.notification.success(this.i18n.t('integrations.webhookCreated'));
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.creatingWebhook.set(false);
    }
  }

  async toggleWebhook(epId: string, active: boolean) {
    try {
      await firstValueFrom(this.http.post(`/api/v1/platform/webhooks/${epId}/toggle?active=${active}`, {}));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async deleteWebhook(epId: string) {
    try {
      await firstValueFrom(this.http.delete(`/api/v1/platform/webhooks/${epId}`));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async viewDeliveries(epId: string) {
    this.selectedEndpointId.set(epId);
    try {
      const result = await firstValueFrom(
        this.http.get<{ deliveries: WebhookDelivery[] }>(`/api/v1/platform/webhooks/${epId}/deliveries`),
      );
      this.deliveries.set(result.deliveries);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async redriveDelivery(epId: string, deliveryId: number) {
    try {
      await firstValueFrom(
        this.http.post(`/api/v1/platform/webhooks/${epId}/deliveries/${deliveryId}/redrive`, {}),
      );
      await this.viewDeliveries(epId);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  closeDeliveries() {
    this.selectedEndpointId.set(null);
    this.deliveries.set([]);
  }

  formatDate(epochMs: number): string {
    return formatDate(epochMs);
  }

  statusClass(status: string): string {
    switch (status) {
      case 'DELIVERED': return 'success';
      case 'FAILED': return 'danger';
      case 'DEAD': return 'dead';
      default: return 'pending';
    }
  }
}
