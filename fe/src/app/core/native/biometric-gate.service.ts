import { Injectable, inject } from '@angular/core';
import { Capacitor, registerPlugin } from '@capacitor/core';
import { App } from '@capacitor/app';
import { Preferences } from '@capacitor/preferences';
import { I18nService } from '../i18n.service';

export interface BiometricGatePlugin {
  /** Runs the platform biometric prompt; resolves true on success. `reason` is shown to the user. */
  authenticate(options: { reason: string; fallbackTitle?: string }): Promise<{ ok: boolean; cancelled: boolean }>;
  isAvailable(): Promise<{ available: boolean }>;
}

const ENABLED_KEY = 'bemo-biometric-enabled';

/**
 * WP-14 AC-6: biometric unlock gate on resume. The Android implementation lives in
 * `fe/android/app/src/main/java/com/bemo/erp/nativebridge/` (androidx.biometric).
 * On web the gate is a no-op so browser/PWA sessions behave exactly as before.
 */
@Injectable({ providedIn: 'root' })
export class BiometricGateService {
  private readonly plugin: BiometricGatePlugin | null = Capacitor.isNativePlatform()
    ? registerPlugin<BiometricGatePlugin>('BiometricGate')
    : null;
  private readonly i18n = inject(I18nService);
  private registered = false;
  enabled = false;

  async register(): Promise<void> {
    if (this.registered || !this.plugin) return;
    this.registered = true;
    const stored = await Preferences.get({ key: ENABLED_KEY });
    this.enabled = stored.value !== 'false';
    void App.addListener('resume', () => {
      if (!this.enabled) return;
      void this.unlock();
    });
  }

  async setEnabled(enabled: boolean): Promise<void> {
    this.enabled = enabled;
    await Preferences.set({ key: ENABLED_KEY, value: String(enabled) });
  }

  async isAvailable(): Promise<boolean> {
    if (!this.plugin) return false;
    const { available } = await this.plugin.isAvailable();
    return available;
  }

  async unlock(): Promise<boolean> {
    const reason = this.i18n.t('native.biometricReason');
    if (!this.enabled || !this.plugin) return true;
    try {
      const { available } = await this.plugin.isAvailable();
      if (!available) return true;
      const result = await this.plugin.authenticate({ reason, fallbackTitle: reason });
      return result.ok || result.cancelled === false ? result.ok : false;
    } catch {
      return false;
    }
  }
}
