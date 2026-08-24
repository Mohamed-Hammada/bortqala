import { Injectable, inject } from '@angular/core';
import { Capacitor, registerPlugin } from '@capacitor/core';
import { PushNotifications, Token, PushNotificationActionPerformed } from '@capacitor/push-notifications';
import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { NativeBridgeService } from './native-bridge.service';

export interface PushRegistrationPlugin {
  /** Opens the Android system notification channel settings (optional UX hook). */
  requestPermission(): Promise<{ granted: boolean }>;
}

/**
 * WP-14 AC-2: registers the device FCM token with the backend push pipeline by posting
 * `platform: 'ANDROID'` + `fcmToken` to the existing `/api/v1/notifications/push/subscriptions`
 * endpoint (extended in BE V346). Web/PWA keeps using the VAPID flow untouched.
 */
@Injectable({ providedIn: 'root' })
export class PushRegistrationService {
  private readonly http = inject(HttpClient);
  private readonly native = inject(NativeBridgeService);

  async register(locale: string): Promise<void> {
    if (!Capacitor.isNativePlatform()) return;
    const status = await PushNotifications.checkPermissions();
    let granted = status.receive === 'granted';
    if (!granted) {
      const requested = await PushNotifications.requestPermissions();
      granted = requested.receive === 'granted';
    }
    if (!granted) return;

    return new Promise((resolve) => {
      void PushNotifications.addListener('registration', (token: Token) => {
        void this.submitToken(token.value, locale).finally(() => resolve());
      });
      void PushNotifications.addListener('registrationError', () => resolve());
      void PushNotifications.addListener('pushNotificationActionPerformed', (_action: PushNotificationActionPerformed) => {
        // Deep-link handling lands with the notification-center routing epic; shell opens for now.
      });
      void PushNotifications.register();
    });
  }

  private async submitToken(fcmToken: string, locale: string): Promise<void> {
    await firstValueFrom(this.http.post(`${this.native.apiBase()}/api/v1/notifications/push/subscriptions`, {
      platform: 'ANDROID',
      fcmToken,
      locale,
      pushApprovals: true,
      pushPayroll: false,
    }));
  }
}
