import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';
import { NotificationPreferences } from '../auth/auth.models';
import { I18nService } from '../i18n.service';

interface WebPushConfig {
  enabled: boolean;
  publicKey: string;
}

interface SubscriptionStatus {
  subscribed: boolean;
}

@Injectable({ providedIn: 'root' })
export class WebPushService {
  private readonly http = inject(HttpClient);
  private readonly swPush = inject(SwPush);
  private readonly i18n = inject(I18nService);

  readonly supported = signal(this.swPush.isEnabled);
  readonly configured = signal(false);
  readonly subscribed = signal(false);
  readonly busy = signal(false);

  private initialized = false;
  private latestPreferences: NotificationPreferences = {
    emailApprovals: true,
    emailPayroll: false,
    pushApprovals: true,
    pushPayroll: false,
  };

  async initialize(preferences: NotificationPreferences = this.storedPreferences()): Promise<void> {
    this.latestPreferences = preferences;
    if (this.initialized) return;
    this.initialized = true;

    if (!this.swPush.isEnabled) {
      this.supported.set(false);
      return;
    }

    try {
      const config = await this.loadConfig();
      const subscription = await firstValueFrom(this.swPush.subscription);
      this.subscribed.set(!!subscription);

      if (config.enabled && subscription) {
        await this.register(subscription);
      }

      this.swPush.pushSubscriptionChanges.subscribe(({ oldSubscription, newSubscription }) => {
        void this.handleSubscriptionChange(oldSubscription, newSubscription);
      });
    } catch {
      this.configured.set(false);
    }
  }

  async enable(preferences: NotificationPreferences): Promise<void> {
    this.latestPreferences = preferences;
    if (!this.swPush.isEnabled) throw new Error('WEB_PUSH_UNSUPPORTED');

    this.busy.set(true);
    try {
      const config = await this.loadConfig();
      if (!config.enabled || !config.publicKey) throw new Error('WEB_PUSH_NOT_CONFIGURED');

      const subscription = await this.swPush.requestSubscription({
        serverPublicKey: config.publicKey,
      });
      await this.register(subscription);
      this.subscribed.set(true);
    } finally {
      this.busy.set(false);
    }
  }

  async disable(): Promise<void> {
    this.busy.set(true);
    try {
      const subscription = await firstValueFrom(this.swPush.subscription);
      if (subscription) {
        try {
          await firstValueFrom(
            this.http.post<void>('/api/v1/notifications/push/subscriptions/unsubscribe', {
              endpoint: subscription.endpoint,
            }),
          );
        } finally {
          await this.swPush.unsubscribe();
        }
      }
      this.subscribed.set(false);
    } finally {
      this.busy.set(false);
    }
  }

  async syncPreferences(preferences: NotificationPreferences): Promise<void> {
    this.latestPreferences = preferences;
    if (!this.swPush.isEnabled || !this.configured()) return;
    try {
      const subscription = await firstValueFrom(this.swPush.subscription);
      if (!subscription) return;
      await this.register(subscription);
    } catch {
      // Preference persistence must not fail because the push provider is temporarily unavailable.
    }
  }

  async detachCurrentUser(): Promise<void> {
    if (!this.swPush.isEnabled) return;
    try {
      const subscription = await firstValueFrom(this.swPush.subscription);
      if (!subscription) return;
      await firstValueFrom(
        this.http.post<void>('/api/v1/notifications/push/subscriptions/unsubscribe', {
          endpoint: subscription.endpoint,
        }),
      );
      this.subscribed.set(false);
    } catch {
      // Logout must continue even if the push endpoint cannot be detached.
    }
  }

  async detachAllDevices(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<void>('/api/v1/notifications/push/subscriptions/unsubscribe-all', {}),
      );
      this.subscribed.set(false);
    } catch {
      // Session revocation remains authoritative even if push cleanup is temporarily unavailable.
    }
  }

  async sendTest(): Promise<void> {
    await firstValueFrom(this.http.post('/api/v1/notifications/push/test', {}));
  }

  permissionDenied(): boolean {
    return typeof Notification !== 'undefined' && Notification.permission === 'denied';
  }

  private async loadConfig(): Promise<WebPushConfig> {
    const config = await firstValueFrom(
      this.http.get<WebPushConfig>('/api/v1/notifications/push/config'),
    );
    this.configured.set(config.enabled);
    return config;
  }

  private async register(subscription: PushSubscription): Promise<void> {
    const raw = subscription.toJSON();
    await firstValueFrom(
      this.http.post<SubscriptionStatus>('/api/v1/notifications/push/subscriptions', {
        endpoint: subscription.endpoint,
        expirationTime: raw.expirationTime ?? null,
        keys: raw.keys,
        locale: this.i18n.locale(),
        pushApprovals: this.latestPreferences.pushApprovals,
        pushPayroll: this.latestPreferences.pushPayroll,
      }),
    );
    this.subscribed.set(true);
  }

  private storedPreferences(): NotificationPreferences {
    try {
      const raw = localStorage.getItem('bemo_notification_prefs');
      if (raw) {
        return { ...this.latestPreferences, ...(JSON.parse(raw) as Partial<NotificationPreferences>) };
      }
    } catch {
      // Ignore malformed local preferences and keep safe defaults.
    }
    return this.latestPreferences;
  }

  private async handleSubscriptionChange(
    oldSubscription: PushSubscription | null,
    newSubscription: PushSubscription | null,
  ): Promise<void> {
    if (newSubscription) {
      try {
        await this.register(newSubscription);
      } catch {
        this.subscribed.set(false);
      }
      return;
    }

    this.subscribed.set(false);
    if (!oldSubscription) return;
    try {
      await firstValueFrom(
        this.http.post<void>('/api/v1/notifications/push/subscriptions/unsubscribe', {
          endpoint: oldSubscription.endpoint,
        }),
      );
    } catch {
      // A stale endpoint will also be disabled by the backend when its push service returns 404/410.
    }
  }
}
