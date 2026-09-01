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

describe('AppShellComponent - Sidebar Navigation & Favorites Interaction', () => {
  let fixture: ComponentFixture<AppShellComponent>;
  let component: AppShellComponent;
  let router: Router;
  let authService: {
    user: any;
    app: any;
    preferences: any;
    hasMenuAccess: any;
    updateNavigationPreferences: any;
  };

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
      favoriteMenuIds: ['dashboard'],
      recentMenuIds: ['employees'],
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

    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: I18nService, useValue: { t: (k: string) => k, locale: signal('en-US'), dir: signal('ltr') } },
        ConfirmDialogService,
        {
          provide: NetworkService,
          useValue: {
            isOnline: signal(true),
            backendState: signal('ONLINE'),
            backendStatus: signal(null),
            lastCheckedAt: signal(null),
            checkNow: vi.fn(),
          },
        },
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
        {
          provide: ScreenShortcutService,
          useValue: {
            runtimeShortcuts: signal([]),
            findByCode: vi.fn(),
            load: vi.fn().mockResolvedValue(undefined),
          },
        },
        { provide: ProductAnalyticsClient, useValue: { track: vi.fn() } },
        DialogStateService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders favorite items and recent items with separate un-nested star buttons', () => {
    expect(component.favoriteItems().map((i) => i.menuId)).toContain('dashboard');
    expect(component.recentItems().map((i) => i.menuId)).toContain('employees');

    const favLink = fixture.nativeElement.querySelector('.favorites-section .nav-item');
    const favStarBtn = fixture.nativeElement.querySelector('.favorites-section .fav-star-btn');

    expect(favLink).toBeTruthy();
    expect(favStarBtn).toBeTruthy();
    // Verify button is a sibling, not nested inside the link
    expect(favLink.contains(favStarBtn)).toBe(false);
  });

  it('clicking the star toggles favorite and does not navigate', () => {
    const starBtn = fixture.nativeElement.querySelector('.recents-section .fav-star-btn') as HTMLButtonElement;
    expect(starBtn).toBeTruthy();

    starBtn.click();
    fixture.detectChanges();

    expect(component.isFavorite('employees')).toBe(true);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(authService.updateNavigationPreferences).toHaveBeenCalled();
  });

  it('clicking the link records recent navigation without toggling favorite', () => {
    const item = component.items.find((i) => i.menuId === 'payroll')!;
    component.onNavItemClick(item);

    expect(component.recentIds()).toContain('payroll');
    expect(component.isFavorite('payroll')).toBe(false);
    expect(authService.updateNavigationPreferences).toHaveBeenCalled();
  });

  it('removing a favorite does not clear the item from recent items', () => {
    // Make dashboard both favorite and recent
    component.recentIds.set(['dashboard', 'employees']);
    component.favorites.set(['dashboard']);
    fixture.detectChanges();

    // Toggle off dashboard favorite
    component.toggleFavorite('dashboard');

    expect(component.isFavorite('dashboard')).toBe(false);
    expect(component.recentIds()).toContain('dashboard');
    expect(component.recentItems().map((i) => i.menuId)).toContain('dashboard');
  });

  it('sidebar collapse tools expand and collapse all workspace sections', () => {
    component.collapseAllGroups();
    expect(component.collapsedGroups().length).toBeGreaterThan(0);

    component.expandAllGroups();
    expect(component.collapsedGroups()).toEqual([]);
  });

  it('mobile close button and backdrop click close the mobile sidebar menu', () => {
    component.menuOpen.set(true);
    expect(component.menuOpen()).toBe(true);

    component.closeMenu();
    expect(component.menuOpen()).toBe(false);
  });
});
