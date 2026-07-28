import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly confirmState = signal<{ message: string; resolver: (value: boolean) => void } | null>(null);

  confirm(message: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmState.set({ message, resolver: resolve });
    });
  }

  resolve(value: boolean): void {
    const state = this.confirmState();
    if (state) {
      state.resolver(value);
      this.confirmState.set(null);
    }
  }
}
