import { Injectable, inject, signal } from '@angular/core';
import { I18nService } from './i18n.service';
import { apiErrorMessage } from './api-error';

export interface ConfirmDetail {
  label: string;
  value: string;
}

export interface ConfirmOptions {
  titleKey?: string;
  messageKey: string;
  params?: Record<string, string | number>;
  confirmKey?: string;
  cancelKey?: string;
  danger?: boolean;
  dangerMessageKey?: string;
  details?: ConfirmDetail[];
}

interface ConfirmState {
  options: ConfirmOptions;
  busy: boolean;
  error: string | null;
  action: (() => Promise<void>) | null;
  resolver: (value: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly confirmState = signal<ConfirmState | null>(null);
  private readonly i18n = inject(I18nService);

  confirm(message: string): Promise<boolean> {
    return this.confirmOptions({ messageKey: message });
  }

  confirmOptions(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmState.set({ options, busy: false, error: null, action: null, resolver: resolve });
    });
  }

  /** Opens a confirmation that runs `action` after confirm; keeps the dialog open and shows errors on failure. */
  confirmAndRun(options: ConfirmOptions, action: () => Promise<void>): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmState.set({ options, busy: false, error: null, action, resolver: resolve });
    });
  }

  proceed(): void {
    const state = this.confirmState();
    if (!state || state.busy) return;
    if (!state.action) {
      this.close(true);
      return;
    }
    this.confirmState.update((s) => (s ? { ...s, busy: true, error: null } : s));
    state.action().then(
      () => this.close(true),
      (error: unknown) => {
        this.confirmState.update((s) =>
          s ? { ...s, busy: false, error: apiErrorMessage(error, this.i18n) } : s,
        );
      },
    );
  }

  cancel(): void {
    this.close(false);
  }

  resolve(value: boolean): void {
    this.close(value);
  }

  private close(value: boolean): void {
    const state = this.confirmState();
    if (!state || state.busy) return;
    this.confirmState.set(null);
    state.resolver(value);
  }
}
