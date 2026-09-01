import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of } from 'rxjs';
import { Router, provideRouter } from '@angular/router';
import { AppShellComponent } from './app-shell.component';
import { AuthService } from '../auth/auth.service';
import { I18nService } from '../i18n.service';
import { ConfirmDialogService } from '../confirm-dialog.service';
import { NetworkService } from '../network.service';
import { NotificationCenterService } from '../notification-center/notification-center.service';
import { WebPushService } from '../notification-center/web-push.service';
import { ScreenShortcutService } from '../shortcuts/screen-shortcut.service';
import { ProductAnalyticsClient } from '../product-analytics-client.service';
import { DialogStateService } from './dialog-state.service';
import { signal } from '@angular/core';
import { ScreenShortcut } from '../shortcuts/screen-shortcut.models';

describe('AppShellComponent - Global Shortcuts & Single Gate', () => {
  let fixture: ComponentFixture<AppShellComponent>;
  let component: AppShellComponent;
  let dialogState: DialogStateService;
  let screenShortcutService: {
    runtimeShortcuts: any;
    findByCode: any;
    load: any;
  };
  let router: {
    events: any;
    navigateByUrl: any;
    url: string;
  };
  let authService: {
    user: any;
    app: any;
    preferences: any;
    hasMenuAccess: any;
    updateNavigationPreferences: any;
  };

  const mockShortcuts: ScreenShortcut[] = [
    {
      id: 'sc-1',
      pageCode: 'EMPLOYEES',
      menuId: 'employees',
      route: '/employees',
      titleKey: 'nav.employees',
      secondKeyCode: 'KeyE',
      displayKey: 'E',
      enabled: true,
      defaultShortcut: true,
      availabilityStatus: 'AVAILABLE',
      unavailableReasonKey: null,
    },
    {
      id: 'sc-2',
      pageCode: 'PAYROLL',
      menuId: 'payroll',
      route: '/payroll',
      titleKey: 'nav.payroll',
      secondKeyCode: 'KeyP',
      displayKey: 'P',
      enabled: true,
      defaultShortcut: true,
      availabilityStatus: 'AVAILABLE',
      unavailableReasonKey: null,
    },
  ];

  beforeEach(async () => {
    const userSignal = signal({
      id: 'u1',
      username: 'admin',
      roles: ['SUPER_ADMIN', 'ADMIN'],
      allowedMenus: [],
    });
    const appSignal = signal({
      id: 'app-1',
      name: 'Bemo ERP',
      code: 'BEMO',
    });
    const prefSignal = signal({
      favoriteMenuIds: [],
      recentMenuIds: [],
      showFavorites: true,
      showRecentlyUsed: true,
      maxRecentlyUsed: 5,
    });

    authService = {
      user: userSignal,
      app: appSignal,
      preferences: prefSignal,
      hasMenuAccess: vi.fn().mockReturnValue(true),
      updateNavigationPreferences: vi.fn().mockReturnValue(of({})),
    };

    router = {
      events: of(),
      navigateByUrl: vi.fn().mockResolvedValue(true),
      url: '/dashboard',
    };

    screenShortcutService = {
      runtimeShortcuts: signal(mockShortcuts),
      findByCode: vi.fn((code: string) => mockShortcuts.find((s) => s.secondKeyCode === code)),
      load: vi.fn().mockResolvedValue(undefined),
    };

    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: signal('en-US'), dir: signal('ltr') } },
        ConfirmDialogService,
        { provide: NetworkService, useValue: { isOnline: signal(true), backendState: signal('ONLINE'), backendStatus: signal(null), lastCheckedAt: signal(null), checkNow: vi.fn() } },
        {
          provide: NotificationCenterService,
          useValue: {
            unreadCount: signal(0),
            notifications: signal([]),
            loading: signal(false),
            panelOpen: signal(false),
            loadUnreadCount: vi.fn().mockResolvedValue(undefined),
            loadNotifications: vi.fn().mockResolvedValue(undefined),
            markAsRead: vi.fn().mockResolvedValue(undefined),
            markAllAsRead: vi.fn().mockResolvedValue(undefined),
          },
        },
        { provide: WebPushService, useValue: { isSupported: signal(false), permissionState: signal('default'), initialize: vi.fn().mockResolvedValue(undefined) } },
        { provide: ScreenShortcutService, useValue: screenShortcutService },
        { provide: ProductAnalyticsClient, useValue: { track: vi.fn() } },
        DialogStateService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    component = fixture.componentInstance;
    dialogState = TestBed.inject(DialogStateService);
    router = TestBed.inject(Router) as any;
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('opens quick nav when / is pressed on normal page', () => {
    const event = new KeyboardEvent('keydown', { key: '/', bubbles: true });
    document.dispatchEvent(event);
    expect(component.quickNavOpen()).toBe(true);
  });

  it('opens shortcut help when ? is pressed on normal page', () => {
    const event = new KeyboardEvent('keydown', { key: '?', bubbles: true });
    document.dispatchEvent(event);
    expect(component.shortcutHelpOpen()).toBe(true);
  });

  it('toggles command palette when Ctrl+K is pressed', () => {
    const event = new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true });
    document.dispatchEvent(event);
    expect(component.paletteOpen()).toBe(true);
  });

  it('suppresses shortcuts when target is an input or textarea', () => {
    const input = document.createElement('input');
    document.body.appendChild(input);

    const eventSlash = new KeyboardEvent('keydown', { key: '/', bubbles: true });
    input.dispatchEvent(eventSlash);
    expect(component.quickNavOpen()).toBe(false);

    const eventHelp = new KeyboardEvent('keydown', { key: '?', bubbles: true });
    input.dispatchEvent(eventHelp);
    expect(component.shortcutHelpOpen()).toBe(false);

    const eventCtrlK = new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true });
    input.dispatchEvent(eventCtrlK);
    expect(component.paletteOpen()).toBe(false);

    document.body.removeChild(input);
  });

  it('suppresses all shortcuts when a page modal dialog is open', () => {
    dialogState.modalOpened();
    fixture.detectChanges();
    expect(dialogState.modalOpen()).toBe(true);

    // / is suppressed
    document.dispatchEvent(new KeyboardEvent('keydown', { key: '/', bubbles: true }));
    expect(component.quickNavOpen()).toBe(false);

    // ? is suppressed
    document.dispatchEvent(new KeyboardEvent('keydown', { key: '?', bubbles: true }));
    expect(component.shortcutHelpOpen()).toBe(false);

    // Ctrl+K is suppressed
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }));
    expect(component.paletteOpen()).toBe(false);

    // G chord start is suppressed
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
    expect(component.chordWaiting()).toBe(false);

    // Escape does NOT close shell panels behind the modal
    component.paletteOpen.set(true);
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(component.paletteOpen()).toBe(true);

    dialogState.modalClosed();
  });

  it('starts G-chord and navigates on second key resolution', () => {
    // Start chord: G
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
    expect(component.chordWaiting()).toBe(true);

    // Resolve chord: E for employees
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'e', code: 'KeyE', bubbles: true }));
    expect(component.chordWaiting()).toBe(false);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/employees');
  });

  it('clears active G-chord when modal dialog opens', () => {
    // Start chord: G
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
    expect(component.chordWaiting()).toBe(true);

    // Modal opens
    dialogState.modalOpened();
    fixture.detectChanges();

    expect(component.chordWaiting()).toBe(false);
    dialogState.modalClosed();
  });

  it('clears active G-chord on window blur', () => {
    // Start chord: G
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
    expect(component.chordWaiting()).toBe(true);

    // Trigger window blur
    window.dispatchEvent(new Event('blur'));
    expect(component.chordWaiting()).toBe(false);
  });

  it('clears active G-chord after deterministic timeout (1800ms)', () => {
    vi.useFakeTimers();
    try {
      // Start chord: G
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
      expect(component.chordWaiting()).toBe(true);

      vi.advanceTimersByTime(1000);
      expect(component.chordWaiting()).toBe(true);

      vi.advanceTimersByTime(800);
      expect(component.chordWaiting()).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('does not navigate to restricted / invisible destination', () => {
    authService.user.set({
      id: 'u2',
      username: 'guest',
      roles: ['USER'],
      allowedMenus: [],
    });
    authService.hasMenuAccess.mockReturnValue(false);

    // Start chord: G
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'g', code: 'KeyG', bubbles: true }));
    expect(component.chordWaiting()).toBe(true);

    // Try resolving restricted destination
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'e', code: 'KeyE', bubbles: true }));
    expect(component.chordWaiting()).toBe(false);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
