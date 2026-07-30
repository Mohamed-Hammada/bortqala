import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface SystemStatus {
  status: 'UP';
  service: string;
  version: string;
  cacheVersion: string;
  serverTime: number;
  cacheUpdatedAt: number | null;
  cacheUpdatedBy: string | null;
}

export type BackendConnectionState = 'CHECKING' | 'ONLINE' | 'OFFLINE';

const CACHE_VERSION_KEY = 'bemo-erp-client-cache-version';
const CACHE_RELOAD_KEY = 'bemo-erp-cache-reload';
const STATUS_POLL_INTERVAL_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class SystemStatusService {
  private readonly httpClient = inject(HttpClient);
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private requestInFlight: Promise<void> | null = null;

  readonly connectionState = signal<BackendConnectionState>('CHECKING');
  readonly status = signal<SystemStatus | null>(null);
  readonly lastCheckedAt = signal<number | null>(null);

  async initialize(): Promise<void> {
    await this.checkNow();
    this.startPolling();
  }

  checkNow(): Promise<void> {
    if (this.requestInFlight) return this.requestInFlight;
    this.requestInFlight = this.fetchStatus().finally(() => {
      this.requestInFlight = null;
    });
    return this.requestInFlight;
  }

  private async fetchStatus(): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.httpClient.get<SystemStatus>('/api/v1/system/status'),
      );
      this.status.set(response);
      this.connectionState.set(response.status === 'UP' ? 'ONLINE' : 'OFFLINE');
      this.lastCheckedAt.set(Date.now());
      await this.refreshClientCacheIfNeeded(response.cacheVersion);
    } catch {
      this.connectionState.set('OFFLINE');
      this.lastCheckedAt.set(Date.now());
    }
  }

  private startPolling(): void {
    if (this.pollTimer) return;
    this.pollTimer = setInterval(() => void this.checkNow(), STATUS_POLL_INTERVAL_MS);
  }

  private async refreshClientCacheIfNeeded(cacheVersion: string): Promise<void> {
    const previousVersion = localStorage.getItem(CACHE_VERSION_KEY);
    localStorage.setItem(CACHE_VERSION_KEY, cacheVersion);
    if (!previousVersion || previousVersion === cacheVersion) return;
    if (sessionStorage.getItem(CACHE_RELOAD_KEY) === cacheVersion) return;

    sessionStorage.setItem(CACHE_RELOAD_KEY, cacheVersion);
    if ('caches' in window) {
      const cacheNames = await caches.keys();
      await Promise.all(cacheNames.map((cacheName) => caches.delete(cacheName)));
    }
    if ('serviceWorker' in navigator) {
      const registrations = await navigator.serviceWorker.getRegistrations();
      await Promise.all(registrations.map((registration) => registration.unregister()));
    }
    window.location.reload();
  }
}
