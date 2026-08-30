import { computed, Injectable, inject, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

export const SERVER_URL_KEY = 'bemo-server-url';

@Injectable({ providedIn: 'root' })
export class NativeBridgeService {
  private readonly serverUrl = signal<string | null>(null);
  readonly isNative = Capacitor.isNativePlatform();
  readonly platform = Capacitor.getPlatform();
  readonly configuredServerUrl = computed(() => this.serverUrl());

  async loadServerUrl(): Promise<string | null> {
    if (this.serverUrl()) return this.serverUrl();
    const { value } = await Preferences.get({ key: SERVER_URL_KEY });
    this.serverUrl.set(value && value.trim() ? value.trim().replace(/\/+$/, '') : null);
    return this.serverUrl();
  }

  async storeServerUrl(rawUrl: string): Promise<void> {
    const normalized = normalizeServerUrl(rawUrl);
    await Preferences.set({ key: SERVER_URL_KEY, value: normalized });
    this.serverUrl.set(normalized);
  }

  async clearServerUrl(): Promise<void> {
    await Preferences.remove({ key: SERVER_URL_KEY });
    this.serverUrl.set(null);
  }

  /** Absolute base for API calls when running inside the native shell; empty string keeps web relative. */
  apiBase(): string {
    return this.isNative ? this.serverUrl() ?? '' : '';
  }
}


export function normalizeServerUrl(rawUrl: string): string {
  const trimmed = rawUrl.trim().replace(/\/+$/, '');
  if (!trimmed) return trimmed;
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  return `https://${trimmed}`;
}
