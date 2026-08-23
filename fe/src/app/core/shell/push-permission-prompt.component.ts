import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { I18nService } from '../i18n.service';
import { WebPushService } from '../notification-center/web-push.service';import { DialogStateService } from './dialog-state.service';

export const PUSH_PROMPT_STORAGE_KEY = 'bemo_push_prompt_v1';
const SNOOZE_MS = 14 * 24 * 60 * 60 * 1000;
const SHOW_DELAY_MS = 2000;

export type PushPromptAnswer = 'enabled' | 'later' | 'never';

export interface PushPromptState {
  userId: string;
  answer: PushPromptAnswer;
  askedAt: number;
}

export function readPushPromptState(userId: string): PushPromptState | null {
  try {
    const raw = localStorage.getItem(PUSH_PROMPT_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<PushPromptState>;
    if (parsed.userId !== userId || typeof parsed.answer !== 'string' || typeof parsed.askedAt !== 'number') {
      return null;
    }
    return parsed as PushPromptState;
  } catch {
    return null;
  }
}

export function writePushPromptState(state: PushPromptState): void {
  try {
    localStorage.setItem(PUSH_PROMPT_STORAGE_KEY, JSON.stringify(state));
  } catch {
    /* storage unavailable — prompt simply re-evaluates next session */
  }
}

@Component({
  selector: 'app-push-permission-prompt',
  standalone: true,
  templateUrl: './push-permission-prompt.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PushPermissionPromptComponent {
  readonly webPush = inject(WebPushService);
  private readonly authService = inject(AuthService);
  private readonly dialogState = inject(DialogStateService);
  readonly i18n = inject(I18nService);
  private readonly destroyRef = inject(DestroyRef);

  readonly visible = signal(false);
  private holdingOverlay = false;
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.timer = setTimeout(() => this.evaluate(), SHOW_DELAY_MS);
    this.destroyRef.onDestroy(() => {
      if (this.timer) clearTimeout(this.timer);
      this.releaseOverlay();
    });
  }

  /** All gate conditions; exported logic kept inline for one-screen readability. */
  evaluate(now: number = Date.now()): void {
    if (this.timer) clearTimeout(this.timer);
    const user = this.authService.user();
    if (!user) return;

    // Single source of truth: the WebPushService mirrors browser permission state.
    const configReady = this.webPush.supported() && this.webPush.configured()
      && !this.webPush.subscribed() && !this.webPush.permissionDenied();
    if (!configReady) return;

    const previous = readPushPromptState(user.id);
    if (previous) {
      if (previous.answer === 'never') return;
      if (previous.answer === 'enabled') return;
      if (previous.answer === 'later' && now - previous.askedAt < SNOOZE_MS) return;
    }

    this.captureOverlay();
    this.visible.set(true);
  }

  async enable(): Promise<void> {
    const user = this.authService.user();
    this.record(user ? user.id : '', 'enabled');
    this.hide();
    try {
      await this.webPush.enable(this.webPush.storedPreferences());
    } catch {
      /* enable() already surfaces busy/unsupported states in Settings */
    }
  }

  later(): void {
    const user = this.authService.user();
    this.record(user ? user.id : '', 'later');
    this.hide();
  }

  neverAsk(): void {
    const user = this.authService.user();
    this.record(user ? user.id : '', 'never');
    this.hide();
  }

  deniedHintVisible(): boolean {
    return this.webPush.permissionDenied();
  }

  private hide(): void {
    this.visible.set(false);
    this.releaseOverlay();
  }

  private captureOverlay(): void {
    if (!this.holdingOverlay) {
      this.dialogState.modalOpened();
      this.holdingOverlay = true;
    }
  }

  private releaseOverlay(): void {
    if (this.holdingOverlay) {
      this.dialogState.modalClosed();
      this.holdingOverlay = false;
    }
  }

  private record(userId: string, answer: PushPromptAnswer): void {
    writePushPromptState({ userId, answer, askedAt: Date.now() });
  }
}
