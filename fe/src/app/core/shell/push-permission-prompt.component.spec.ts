import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { WebPushService } from '../../core/notification-center/web-push.service';
import { I18nService } from '../../core/i18n.service';
import { DialogStateService } from './dialog-state.service';
import {
  PUSH_PROMPT_STORAGE_KEY,
  PushPermissionPromptComponent,
  readPushPromptState,
  writePushPromptState,
} from './push-permission-prompt.component';

type Stub = Record<
  'supported' | 'configured' | 'subscribed' | 'permissionDenied' | 'storedPreferences' | 'enable',
  ReturnType<typeof vi.fn>
>;

function webPushStub(overrides: Partial<Record<'supported' | 'configured' | 'subscribed' | 'permissionDenied', boolean>> = {}): Stub {
  return {
    supported: vi.fn(() => overrides.supported ?? true),
    configured: vi.fn(() => overrides.configured ?? true),
    subscribed: vi.fn(() => overrides.subscribed ?? false),
    permissionDenied: vi.fn(() => overrides.permissionDenied ?? false),
    storedPreferences: vi.fn(() => ({ emailApprovals: true, emailPayroll: false, pushApprovals: true, pushPayroll: false })),
    enable: vi.fn(async () => undefined),
  };
}

describe('PushPermissionPromptComponent (WP-09)', () => {
  let fixture!: ComponentFixture<PushPermissionPromptComponent>;
  let webPush!: Stub;

  function configure(
    pushOverrides: Parameters<typeof webPushStub>[0] = {},
    user: { id: string } | null = { id: 'u-1' },
  ): void {
    localStorage.removeItem(PUSH_PROMPT_STORAGE_KEY);
    TestBed.resetTestingModule();
    webPush = webPushStub(pushOverrides);
    TestBed.configureTestingModule({
      providers: [
        DialogStateService,
        { provide: AuthService, useValue: { user: () => user, preferences: () => ({ pushApprovals: true, pushPayroll: false }) } },
        { provide: WebPushService, useValue: webPush },
        { provide: I18nService, useValue: { locale: () => 'ar-EG', t: (_k: string, _p?: unknown, f?: string) => f ?? _k } },
      ],
    });
    fixture = TestBed.createComponent(PushPermissionPromptComponent);
    fixture.detectChanges();
  }

  function evaluate(now = Date.now()): void {
    fixture.componentInstance.evaluate(now);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.stubGlobal('Notification', { permission: 'default' });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.removeItem(PUSH_PROMPT_STORAGE_KEY);
  });

  it('shows after delayed evaluation when all gates pass, holding dialog state', () => {
    configure();
    expect(fixture.componentInstance.visible()).toBe(false);
    evaluate();
    expect(fixture.componentInstance.visible()).toBe(true);
    expect(TestBed.inject(DialogStateService).modalOpen()).toBe(true);
  });

  it('hides when unsupported / unconfigured / already subscribed / denied', () => {
    const cases = [
      { supported: false },
      { configured: false },
      { subscribed: true },
      { permissionDenied: true },
    ] as const;
    for (const overrides of cases) {
      configure(overrides);
      evaluate();
      expect(fixture.componentInstance.visible(), JSON.stringify(overrides)).toBe(false);
    }
  });

  it('enable() records answer, hides, releases state, and calls webPush.enable(preferences)', async () => {
    configure();
    evaluate();
    await fixture.componentInstance.enable();
    fixture.detectChanges();
    expect(fixture.componentInstance.visible()).toBe(false);
    expect(TestBed.inject(DialogStateService).modalOpen()).toBe(false);
    expect(webPush.enable).toHaveBeenCalledWith(expect.objectContaining({ pushApprovals: true, pushPayroll: false }));
    expect(readPushPromptState('u-1')?.answer).toBe('enabled');
  });

  it('"later" snoozes 14 days, then re-prompts on day 15', () => {
    configure();
    evaluate();
    fixture.componentInstance.later();
    fixture.detectChanges();
    expect(fixture.componentInstance.visible()).toBe(false);
    expect(readPushPromptState('u-1')?.answer).toBe('later');

    evaluate(Date.now() + 13 * 24 * 60 * 60 * 1000);
    expect(fixture.componentInstance.visible()).toBe(false);

    evaluate(Date.now() + 15 * 24 * 60 * 60 * 1000);
    expect(fixture.componentInstance.visible()).toBe(true);
  });

  it('"never" hides permanently for that user but not for another user', () => {
    configure();
    evaluate();
    fixture.componentInstance.neverAsk();
    evaluate();
    expect(fixture.componentInstance.visible()).toBe(false);

    configure({}, { id: 'u-2' });
    evaluate();
    expect(fixture.componentInstance.visible()).toBe(true);
  });

  it('never shows when a stored "never" exists for the same user', () => {
    configure();
    writePushPromptState({ userId: 'u-1', answer: 'never', askedAt: Date.now() });
    evaluate();
    expect(fixture.componentInstance.visible()).toBe(false);
  });
});
