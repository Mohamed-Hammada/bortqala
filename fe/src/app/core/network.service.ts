import { Injectable, computed, inject, signal } from '@angular/core';
import { SystemStatusService } from './system-status.service';

@Injectable({
  providedIn: 'root',
})
export class NetworkService {
  private readonly systemStatusService = inject(SystemStatusService);
  private readonly browserOnline = signal<boolean>(navigator.onLine);

  readonly backendState = this.systemStatusService.connectionState;
  readonly backendStatus = this.systemStatusService.status;
  readonly lastCheckedAt = this.systemStatusService.lastCheckedAt;
  readonly isOnline = computed(
    () => this.browserOnline() && this.backendState() === 'ONLINE',
  );

  constructor() {
    window.addEventListener('online', () => {
      this.browserOnline.set(true);
      void this.systemStatusService.checkNow();
    });
    window.addEventListener('offline', () => this.browserOnline.set(false));
  }

  checkNow(): Promise<void> {
    return this.systemStatusService.checkNow();
  }
}
