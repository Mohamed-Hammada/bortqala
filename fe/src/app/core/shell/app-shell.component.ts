import { ChangeDetectionStrategy, Component, HostListener, computed, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { RoleCode } from '../auth/auth.models';
import { I18nService } from '../i18n.service';
import { ConfirmDialogService } from '../confirm-dialog.service';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { ToastContainerComponent } from '../../shared/ui/toast/toast-container.component';
import { AppTooltipDirective } from '../../shared/ui/app-tooltip/app-tooltip.directive';
import { NetworkService } from '../network.service';
import { GLOBAL_SHORTCUTS } from '../app-shortcuts';
import { ScreenShortcutService } from '../shortcuts/screen-shortcut.service';
import { ScreenShortcut } from '../shortcuts/screen-shortcut.models';
import { ProductAnalyticsClient } from '../product-analytics-client.service';
import {
  NAV_ITEMS,
  WORKSPACE_ORDER,
  canAccessNavigationItem,
  type NavItem,
  type WorkspaceSection,
} from '../navigation/app-navigation';
export { NAV_ITEMS, SHELL_MENU_ROLES } from '../navigation/app-navigation';
export type { NavItem, WorkspaceGroup, WorkspaceSection } from '../navigation/app-navigation';

const COLLAPSED_GROUPS_KEY = 'hr-collapsed-groups';
import { NotificationCenterService } from '../notification-center/notification-center.service';
import { WebPushService } from '../notification-center/web-push.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent, ToastContainerComponent, AppTooltipDirective],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly confirmDialog = inject(ConfirmDialogService);
  readonly network = inject(NetworkService);
  readonly router = inject(Router);
  readonly notificationCenter = inject(NotificationCenterService);
  readonly webPush = inject(WebPushService);
  readonly screenShortcuts = inject(ScreenShortcutService);
  private readonly productAnalytics = inject(ProductAnalyticsClient);

  readonly searchQuery = signal('');
  readonly menuOpen = signal(false);
  readonly collapsed = signal(false);
  readonly quickNavOpen = signal(false);
  readonly shortcutHelpOpen = signal(false);
  readonly selectedQuickNavIndex = signal(0);
  readonly chordWaiting = signal(false);
  readonly logoutOptionsOpen = signal(false);
  readonly logoutAllDevicesBusy = signal(false);
  readonly logoutError = signal('');
  readonly globalShortcuts = GLOBAL_SHORTCUTS;
  private chordTimer: ReturnType<typeof setTimeout> | null = null;

  readonly favorites = signal<string[]>(this.authService.preferences().favoriteMenuIds);
  readonly recentIds = signal<string[]>(this.authService.preferences().recentMenuIds);
  readonly collapsedGroups = signal<string[]>(this.loadStoredCollapsedGroups());

  readonly items = NAV_ITEMS;

  readonly quickNavItems = computed<NavItem[]>(() => {
    const query = this.searchQuery().trim().toLocaleLowerCase();
    return this.items.filter((item) => {
      if (!this.visible(item)) return false;
      if (!query) return true;
      const searchable = `${this.i18n.t(item.labelKey)} ${this.i18n.t(item.descriptionKey)}`.toLocaleLowerCase();
      return searchable.includes(query);
    });
  });

  readonly favoriteItems = computed<NavItem[]>(() => {
    if (!this.authService.preferences().showFavorites) return [];
    const favs = this.favorites();
    return this.items.filter((item) => favs.includes(item.menuId) && this.visible(item));
  });

  readonly recentItems = computed<NavItem[]>(() => {
    if (!this.authService.preferences().showRecentlyUsed) return [];
    const recents = this.recentIds();
    const favs = this.favorites();
    return this.items
      .filter((item) => recents.includes(item.menuId) && !favs.includes(item.menuId) && this.visible(item))
      .slice(0, this.authService.preferences().maxRecentlyUsed);
  });

  readonly userRolesLabel = computed(() => {
    const roles = this.authService.user()?.roles ?? [];
    const labelKeys: Partial<Record<RoleCode, string>> = {
      SUPER_ADMIN: 'role.superAdmin',
      ADMIN: 'role.admin',
      HR_MANAGER: 'role.hrManager',
      HR_REVIEWER: 'role.hrReviewer',
      VIEWER: 'role.viewer',
      FINANCE_MANAGER: 'role.financeManager',
      ACCOUNTANT: 'role.accountant',
      TREASURY_USER: 'role.treasuryUser',
      PROCUREMENT_MANAGER: 'role.procurementManager',
      PROCUREMENT_USER: 'role.procurementUser',
      SALES_MANAGER: 'role.salesManager',
      INVENTORY_MANAGER: 'role.inventoryManager',
      MANUFACTURING_MANAGER: 'role.manufacturingManager',
      QUALITY_MANAGER: 'role.qualityManager',
      PAYROLL_MANAGER: 'role.payrollManager',
      WORKFORCE_MANAGER: 'role.workforceManager',
      WORKFORCE_REVIEWER: 'role.workforceReviewer',
      WORKFORCE_FINANCE: 'role.workforceFinance',
      AUDITOR: 'role.auditor',
    };
    return roles
      .map((role) => this.i18n.t(labelKeys[role] ?? role))
      .join(' · ');
  });

  readonly workspaceSections = computed<WorkspaceSection[]>(() => {
    const groups = WORKSPACE_ORDER.map((key) => ({ key, items: [] as NavItem[] }));

    for (const item of this.items) {
      if (this.visible(item)) {
        const group = groups.find((g) => g.key === item.workspace);
        if (group) group.items.push(item);
      }
    }

    return groups
      .filter((g) => g.items.length > 0)
      .map((g) => ({ titleKey: g.key, items: g.items }));
  });

  constructor() {
    this.notificationCenter.loadUnreadCount();
    void this.webPush.initialize();
    void this.screenShortcuts.load();
    effect(() => {
      const preferences = this.authService.preferences();
      this.favorites.set([...preferences.favoriteMenuIds]);
      this.recentIds.set([...preferences.recentMenuIds]);
    }, { allowSignalWrites: true });
    effect(() => {
      if (this.authService.user() === null) {
        queueMicrotask(() => void this.router.navigate(['/login']));
      }
    });
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.trackRecentNavigation(event.urlAfterRedirects);
        this.autoExpandActiveGroup(event.urlAfterRedirects);
      });
  }

  visible(item: NavItem): boolean {
    const user = this.authService.user();
    if (!user) return false;
    return canAccessNavigationItem(
      item,
      user.roles,
      (menuId) => this.authService.hasMenuAccess(menuId),
    );
  }

  isFavorite(menuId: string): boolean {
    return this.favorites().includes(menuId);
  }

  toggleFavorite(menuId: string, event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    const current = this.favorites();
    let updated: string[];
    if (current.includes(menuId)) {
      updated = current.filter((id) => id !== menuId);
    } else {
      updated = [...current, menuId];
    }
    this.favorites.set(updated);
    this.persistNavigation(updated, this.recentIds());
  }

  isGroupCollapsed(groupKey: string): boolean {
    return this.collapsedGroups().includes(groupKey);
  }

  toggleGroupCollapse(groupKey: string): void {
    const current = this.collapsedGroups();
    let updated: string[];
    if (current.includes(groupKey)) {
      updated = current.filter((k) => k !== groupKey);
    } else {
      updated = [...current, groupKey];
    }
    this.collapsedGroups.set(updated);
    localStorage.setItem(COLLAPSED_GROUPS_KEY, JSON.stringify(updated));
  }

  expandAllGroups(): void {
    this.collapsedGroups.set([]);
    localStorage.removeItem(COLLAPSED_GROUPS_KEY);
  }

  collapseAllGroups(): void {
    const allGroups = this.workspaceSections().map((s) => s.titleKey);
    this.collapsedGroups.set(allGroups);
    localStorage.setItem(COLLAPSED_GROUPS_KEY, JSON.stringify(allGroups));
  }

  openQuickNav(): void {
    this.shortcutHelpOpen.set(false);
    this.searchQuery.set('');
    this.selectedQuickNavIndex.set(0);
    this.quickNavOpen.set(true);
    queueMicrotask(() => {
      document.querySelector<HTMLInputElement>('[data-quick-nav-search]')?.focus();
    });
  }

  openShortcutHelp(): void {
    this.quickNavOpen.set(false);
    this.shortcutHelpOpen.set(true);
  }

  closeShortcutPanels(): void {
    this.quickNavOpen.set(false);
    this.shortcutHelpOpen.set(false);
    this.searchQuery.set('');
    this.clearChord();
  }

  updateQuickNavQuery(value: string): void {
    this.searchQuery.set(value);
    this.selectedQuickNavIndex.set(0);
  }

  onQuickNavKeydown(event: KeyboardEvent): void {
    const count = this.quickNavItems().length;
    if (event.key === 'ArrowDown' && count > 0) {
      event.preventDefault();
      this.selectedQuickNavIndex.update((index) => (index + 1) % count);
      return;
    }
    if (event.key === 'ArrowUp' && count > 0) {
      event.preventDefault();
      this.selectedQuickNavIndex.update((index) => (index - 1 + count) % count);
      return;
    }
    if (event.key === 'Enter' && count > 0) {
      event.preventDefault();
      const item = this.quickNavItems()[this.selectedQuickNavIndex()];
      if (item) this.navigateToItem(item);
    }
  }

  navigateToItem(item: NavItem): void {
    if (!this.visible(item)) return;
    this.onNavItemClick(item);
    this.closeShortcutPanels();
    void this.router.navigateByUrl(item.path);
  }

  shortcut(item: NavItem): ScreenShortcut | undefined {
    return this.screenShortcuts.runtimeShortcuts().find(
      (shortcut) => shortcut.menuId === item.menuId
    );
  }

  navTooltip(item: NavItem): string {
    const shortcut = this.shortcut(item);
    const details = `${this.i18n.t(item.labelKey)} — ${this.i18n.t(item.descriptionKey)}`;
    return shortcut ? `${details} · G → ${shortcut.displayKey}` : details;
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalShortcut(event: KeyboardEvent): void {
    if (event.isComposing || event.repeat) return;

    const target = event.target;
    const typing = target instanceof HTMLElement
      && target.matches('input, textarea, select, [contenteditable="true"], [data-shortcut-capture="true"]');

    const lowerKey = event.key.toLocaleLowerCase();

    if (
      ((event.ctrlKey || event.metaKey)
        && !event.altKey
        && (lowerKey === 'k' || lowerKey === '/'))
      || (!typing
        && !event.ctrlKey
        && !event.metaKey
        && !event.altKey
        && event.key === '/')
    ) {
      event.preventDefault();
      this.openQuickNav();
      return;
    }

    if (event.key === 'Escape' && this.logoutOptionsOpen()) {
      event.preventDefault();
      this.closeLogoutOptions();
      return;
    }

    if (
      event.key === 'Escape'
      && (this.quickNavOpen() || this.shortcutHelpOpen() || this.chordWaiting())
    ) {
      event.preventDefault();
      this.closeShortcutPanels();
      return;
    }

    if (
      !typing
      && !event.ctrlKey
      && !event.metaKey
      && !event.altKey
      && event.key === '?'
    ) {
      event.preventDefault();
      this.openShortcutHelp();
      return;
    }

    if (
      typing
      || event.ctrlKey
      || event.metaKey
      || event.altKey
      || this.quickNavOpen()
      || this.shortcutHelpOpen()
      || this.logoutOptionsOpen()
    ) return;

    if (this.chordWaiting()) {
      event.preventDefault();

      const shortcut = this.screenShortcuts.findByCode(event.code);
      this.clearChord();

      if (!shortcut) return;

      const item = this.items.find(
        (candidate) => candidate.menuId === shortcut.menuId
      );

      if (
        item
        && this.visible(item)
        && item.path === shortcut.route
      ) {
        this.navigateToItem(item);
      }
      return;
    }

    if (event.code === 'KeyG') {
      event.preventDefault();
      this.chordWaiting.set(true);
      this.chordTimer = setTimeout(() => this.clearChord(), 1800);
    }
  }

  @HostListener('keydown', ['$event'])
  submitFormOnEnter(event: KeyboardEvent): void {
    if (event.key !== 'Enter' || event.shiftKey || event.ctrlKey || event.altKey || event.metaKey || event.isComposing) return;
    const target = event.target;
    if (!(target instanceof HTMLElement)) return;
    if (target.matches('textarea, select, button, [contenteditable="true"]')) return;
    if (target instanceof HTMLInputElement && ['button', 'submit', 'reset', 'file'].includes(target.type)) return;
    const form = target.closest('form');
    if (!form) return;
    event.preventDefault();
    form.requestSubmit();
  }

  onNavItemClick(item: NavItem): void {
    this.pushRecent(item.menuId);
    this.closeMenu();
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.logoutError.set('');
    this.logoutOptionsOpen.set(true);
  }

  closeLogoutOptions(): void {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutOptionsOpen.set(false);
    this.logoutError.set('');
  }

  async logoutCurrentBrowser(): Promise<void> {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutOptionsOpen.set(false);
    await this.webPush.detachCurrentUser();
    this.authService.logoutCurrentBrowser();
    void this.router.navigate(['/login']);
  }

  async logoutAllDevices(): Promise<void> {
    if (this.logoutAllDevicesBusy()) return;
    this.logoutAllDevicesBusy.set(true);
    this.logoutError.set('');

    await this.webPush.detachAllDevices();
    this.authService.logoutAllDevices().subscribe({
      next: () => {
        this.logoutAllDevicesBusy.set(false);
        this.logoutOptionsOpen.set(false);
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.logoutAllDevicesBusy.set(false);
        this.logoutError.set(this.i18n.t('auth.logoutAllDevicesError'));
      },
    });
  }

  notificationPriorityKey(priority: string): string {
    const keys: Record<string, string> = {
      CRITICAL: 'actionCenter.priority.critical',
      HIGH: 'actionCenter.priority.high',
      MEDIUM: 'actionCenter.priority.medium',
      INFO: 'actionCenter.priority.info',
    };
    return keys[priority] ?? keys['INFO'];
  }

  private trackRecentNavigation(url: string): void {
    this.productAnalytics.captureNavigation(url);
    const matched = this.items.find((i) => url.startsWith(i.path));
    if (matched) {
      this.pushRecent(matched.menuId);
    }
  }

  private autoExpandActiveGroup(url: string): void {
    const matched = this.items.find((i) => url.startsWith(i.path));
    if (matched && matched.workspace) {
      const current = this.collapsedGroups();
      if (current.includes(matched.workspace)) {
        const updated = current.filter((k) => k !== matched.workspace);
        this.collapsedGroups.set(updated);
        localStorage.setItem(COLLAPSED_GROUPS_KEY, JSON.stringify(updated));
      }
    }
  }

  private pushRecent(menuId: string): void {
    const current = this.recentIds().filter((id) => id !== menuId);
    const updated = [menuId, ...current].slice(0, this.authService.preferences().maxRecentlyUsed);
    this.recentIds.set(updated);
    this.persistNavigation(this.favorites(), updated);
  }

  private persistNavigation(favoriteMenuIds: string[], recentMenuIds: string[]): void {
    const preferences = this.authService.preferences();
    this.authService.updateNavigationPreferences({
      showFavorites: preferences.showFavorites,
      showRecentlyUsed: preferences.showRecentlyUsed,
      maxRecentlyUsed: preferences.maxRecentlyUsed,
      favoriteMenuIds,
      recentMenuIds,
    }).subscribe();
  }

  private loadStoredCollapsedGroups(): string[] {
    try {
      const raw = localStorage.getItem(COLLAPSED_GROUPS_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw) as string[];
      if (Array.isArray(parsed) && parsed.length >= 5) {
        localStorage.removeItem(COLLAPSED_GROUPS_KEY);
        return [];
      }
      return parsed;
    } catch {
      return [];
    }
  }

  private clearChord(): void {
    this.chordWaiting.set(false);
    if (this.chordTimer) clearTimeout(this.chordTimer);
    this.chordTimer = null;
  }
}

// BORTQALA_FEEDBACK_20260816_ORGANIZATION: organization belongs to People and is available to HR managers.
