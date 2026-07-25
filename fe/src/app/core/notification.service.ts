import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: string;
  type: ToastType;
  message: string;
  durationMs: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly toasts = signal<ToastMessage[]>([]);

  show(message: string, type: ToastType = 'info', durationMs = 4000): string {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 6)}`;
    const toast: ToastMessage = { id, type, message, durationMs };

    this.toasts.update((current) => [...current, toast]);

    if (durationMs > 0) {
      setTimeout(() => this.remove(id), durationMs);
    }

    return id;
  }

  success(message: string, durationMs = 4000): string {
    return this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = 5000): string {
    return this.show(message, 'error', durationMs);
  }

  info(message: string, durationMs = 4000): string {
    return this.show(message, 'info', durationMs);
  }

  warning(message: string, durationMs = 4000): string {
    return this.show(message, 'warning', durationMs);
  }

  remove(id: string): void {
    this.toasts.update((current) => current.filter((t) => t.id !== id));
  }
}
