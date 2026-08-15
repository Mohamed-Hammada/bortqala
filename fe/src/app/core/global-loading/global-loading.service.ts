import { Injectable, signal } from '@angular/core';

const SHOW_DELAY_MS = 150;

@Injectable({ providedIn: 'root' })
export class GlobalLoadingService {
  private readonly pendingCount = signal(0);
  private readonly visibleState = signal(false);
  private showTimer: ReturnType<typeof setTimeout> | null = null;

  readonly visible = this.visibleState.asReadonly();

  begin(): void {
    this.pendingCount.update((count) => count + 1);

    if (this.pendingCount() !== 1 || this.visibleState()) return;

    this.showTimer = setTimeout(() => {
      this.showTimer = null;
      if (this.pendingCount() > 0) {
        this.visibleState.set(true);
      }
    }, SHOW_DELAY_MS);
  }

  end(): void {
    this.pendingCount.update((count) => Math.max(0, count - 1));

    if (this.pendingCount() > 0) return;

    this.cancelPendingShow();
    this.visibleState.set(false);
  }

  private cancelPendingShow(): void {
    if (this.showTimer === null) return;

    clearTimeout(this.showTimer);
    this.showTimer = null;
  }
}
