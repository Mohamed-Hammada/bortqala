import { ChangeDetectionStrategy, Component, HostListener, computed, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { RoleCode } from '../auth/auth.models';
import { I18nService } from '../i18n.service';
import { ConfirmDialogService } from '../confirm-dialog.service';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';
import { ToastContainerComponent } from '../../shared/ui/toast/toast-container.component';
import { AppTooltipDirective } from '../../shared/ui/app-tooltip/app-tooltip.directive';
import { NetworkService } from '../network.service';
import {
  GLOBAL_SHORTCUTS,
  MENU_SHORTCUTS,
  MenuShortcut,
  shortcutForMenu,
} from '../app-shortcuts';

export type WorkspaceGroup =
  | 'workspace.people'
  | 'workspace.attendance'
  | 'workspace.workforce'
  | 'workspace.operations'
  | 'workspace.finance'
  | 'workspace.admin';

export interface NavItem {
  menuId: string;
  labelKey: string;
  descriptionKey: string;
  path: string;
  icon: IconName;
  workspace: WorkspaceGroup;
  roles?: RoleCode[];
}

export interface WorkspaceSection {
  titleKey: WorkspaceGroup;
  items: NavItem[];
}

const COLLAPSED_GROUPS_KEY = 'hr-collapsed-groups';

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

  readonly searchQuery = signal('');
  readonly menuOpen = signal(false);
  readonly collapsed = signal(false);
  readonly quickNavOpen = signal(false);
  readonly shortcutHelpOpen = signal(false);
  readonly selectedQuickNavIndex = signal(0);
  readonly chordWaiting = signal(false);
  readonly globalShortcuts = GLOBAL_SHORTCUTS;
  readonly menuShortcuts = MENU_SHORTCUTS;
  private chordTimer: ReturnType<typeof setTimeout> | null = null;

  readonly favorites = signal<string[]>(this.authService.preferences().favoriteMenuIds);
  readonly recentIds = signal<string[]>(this.authService.preferences().recentMenuIds);
  readonly collapsedGroups = signal<string[]>(this.loadStoredCollapsedGroups());

  readonly items: NavItem[] = [
    {
      menuId: 'employees',
      labelKey: 'nav.employees',
      descriptionKey: 'nav.employeesHint',
      path: '/employees',
      icon: 'employees',
      workspace: 'workspace.people',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'dashboard',
      labelKey: 'nav.dashboard',
      descriptionKey: 'nav.dashboardHint',
      path: '/dashboard',
      icon: 'dashboard',
      workspace: 'workspace.attendance',
    },
    {
      menuId: 'categories',
      labelKey: 'nav.categories',
      descriptionKey: 'nav.categoriesHint',
      path: '/categories',
      icon: 'categories',
      workspace: 'workspace.attendance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'imports',
      labelKey: 'nav.imports',
      descriptionKey: 'nav.importsHint',
      path: '/imports',
      icon: 'imports',
      workspace: 'workspace.attendance',
      roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
    },
    {
      menuId: 'reports',
      labelKey: 'nav.reports',
      descriptionKey: 'nav.reportsHint',
      path: '/reports',
      icon: 'reports',
      workspace: 'workspace.attendance',
    },
    {
      menuId: 'workforce-dashboard',
      labelKey: 'workforce.dashboard.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/dashboard',
      icon: 'dashboard',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-contractors',
      labelKey: 'workforce.contractors.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/contractors',
      icon: 'users',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-workers',
      labelKey: 'workforce.workers.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/workers',
      icon: 'employees',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-categories',
      labelKey: 'workforce.categories.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/categories',
      icon: 'categories',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-requests',
      labelKey: 'workforce.laborRequests.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/labor-requests',
      icon: 'imports',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-attendance',
      labelKey: 'workforce.attendance.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/attendance',
      icon: 'reports',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-settlements',
      labelKey: 'workforce.settlements.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/settlement-periods',
      icon: 'dashboard',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-advances',
      labelKey: 'workforce.advances.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/advances',
      icon: 'categories',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-accounts',
      labelKey: 'workforce.accounts.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/contractor-accounts',
      icon: 'users',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'workforce-reports',
      labelKey: 'workforce.reports.title',
      descriptionKey: 'nav.workforceHint',
      path: '/workforce/reports-import',
      icon: 'reports',
      workspace: 'workspace.workforce',
    },
    {
      menuId: 'operations',
      labelKey: 'nav.operations',
      descriptionKey: 'nav.operationsHint',
      path: '/operations',
      icon: 'categories',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'procurement',
      labelKey: 'nav.procurement',
      descriptionKey: 'nav.procurementHint',
      path: '/trade/procurement',
      icon: 'imports',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'sales',
      labelKey: 'nav.sales',
      descriptionKey: 'nav.salesHint',
      path: '/trade/sales',
      icon: 'reports',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'production',
      labelKey: 'nav.production',
      descriptionKey: 'nav.productionHint',
      path: '/manufacturing/production',
      icon: 'dashboard',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'quality',
      labelKey: 'nav.quality',
      descriptionKey: 'nav.qualityHint',
      path: '/manufacturing/quality',
      icon: 'settings',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'parties',
      labelKey: 'nav.parties',
      descriptionKey: 'nav.partiesHint',
      path: '/parties',
      icon: 'users',
      workspace: 'workspace.operations',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'payroll',
      labelKey: 'nav.payroll',
      descriptionKey: 'nav.payrollHint',
      path: '/payroll',
      icon: 'reports',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'accounts',
      labelKey: 'nav.accounts',
      descriptionKey: 'nav.accountsHint',
      path: '/finance/accounts',
      icon: 'categories',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'journal-entries',
      labelKey: 'nav.journalEntries',
      descriptionKey: 'nav.journalEntriesHint',
      path: '/finance/journal-entries',
      icon: 'reports',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'banks',
      labelKey: 'nav.banks',
      descriptionKey: 'nav.banksHint',
      path: '/finance/banks',
      icon: 'users',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'tax-currency',
      labelKey: 'nav.taxCurrency',
      descriptionKey: 'nav.taxCurrencyHint',
      path: '/finance/tax-currency',
      icon: 'settings',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'fiscal-periods',
      labelKey: 'nav.fiscalPeriods',
      descriptionKey: 'nav.fiscalPeriodsHint',
      path: '/fiscal-periods',
      icon: 'dashboard',
      workspace: 'workspace.finance',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'organization',
      labelKey: 'nav.organization',
      descriptionKey: 'nav.organizationHint',
      path: '/organization',
      icon: 'categories',
      workspace: 'workspace.admin',
      roles: ['ADMIN'],
    },
    {
      menuId: 'audit-logs',
      labelKey: 'nav.auditLogs',
      descriptionKey: 'nav.auditLogsHint',
      path: '/audit-logs',
      icon: 'reports',
      workspace: 'workspace.admin',
      roles: ['ADMIN'],
    },
    {
      menuId: 'users',
      labelKey: 'nav.users',
      descriptionKey: 'nav.usersHint',
      path: '/users',
      icon: 'users',
      workspace: 'workspace.admin',
      roles: ['ADMIN'],
    },
    {
      menuId: 'settings',
      labelKey: 'nav.settings',
      descriptionKey: 'nav.settingsHint',
      path: '/settings',
      icon: 'settings',
      workspace: 'workspace.admin',
    },
  ];

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
    return roles
      .map((r) => {
        switch (r) {
          case 'SUPER_ADMIN':
            return this.i18n.t('role.superAdmin');
          case 'ADMIN':
            return this.i18n.t('role.admin');
          case 'HR_MANAGER':
            return this.i18n.t('role.hrManager');
          case 'HR_REVIEWER':
            return this.i18n.t('role.hrReviewer');
          case 'VIEWER':
            return this.i18n.t('role.viewer');
          default:
            return r;
        }
      })
      .join(' · ');
  });

  readonly workspaceSections = computed<WorkspaceSection[]>(() => {
    const groups: { key: WorkspaceGroup; items: NavItem[] }[] = [
      { key: 'workspace.people', items: [] },
      { key: 'workspace.attendance', items: [] },
      { key: 'workspace.workforce', items: [] },
      { key: 'workspace.operations', items: [] },
      { key: 'workspace.finance', items: [] },
      { key: 'workspace.admin', items: [] },
    ];

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
    effect(() => {
      const preferences = this.authService.preferences();
      this.favorites.set([...preferences.favoriteMenuIds]);
      this.recentIds.set([...preferences.recentMenuIds]);
    }, { allowSignalWrites: true });
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
    if (user.roles.includes('SUPER_ADMIN') || user.roles.includes('ADMIN')) {
      return true;
    }
    if (item.workspace === 'workspace.workforce') {
      return true;
    }
    const roleOk = !item.roles || this.authService.hasAnyRole(item.roles);
    const menuOk = this.authService.hasMenuAccess(item.menuId);
    return roleOk && menuOk;
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

  shortcut(item: NavItem): MenuShortcut | undefined {
    return shortcutForMenu(item.menuId);
  }

  navTooltip(item: NavItem): string {
    const shortcut = this.shortcut(item);
    const details = `${this.i18n.t(item.labelKey)} — ${this.i18n.t(item.descriptionKey)}`;
    return shortcut ? `${details} · ${shortcut.keys}` : details;
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalShortcut(event: KeyboardEvent): void {
    if (event.isComposing) return;
    const key = event.key.toLocaleLowerCase();
    const target = event.target;
    const typing = target instanceof HTMLElement
      && target.matches('input, textarea, select, [contenteditable="true"]');

    if (((event.ctrlKey || event.metaKey) && !event.altKey && (key === 'k' || key === '/')) || (!typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key === '/')) {
      event.preventDefault();
      this.openQuickNav();
      return;
    }

    if (event.key === 'Escape' && (this.quickNavOpen() || this.shortcutHelpOpen() || this.chordWaiting())) {
      event.preventDefault();
      this.closeShortcutPanels();
      return;
    }

    if (!typing && !event.ctrlKey && !event.metaKey && !event.altKey && event.key === '?') {
      event.preventDefault();
      this.openShortcutHelp();
      return;
    }

    if (typing || event.ctrlKey || event.metaKey || event.altKey || this.quickNavOpen() || this.shortcutHelpOpen()) return;

    if (this.chordWaiting()) {
      event.preventDefault();
      const shortcut = this.menuShortcuts.find((item) => item.chordKey === key);
      this.clearChord();
      if (shortcut) {
        const item = this.items.find((candidate) => candidate.menuId === shortcut.menuId);
        if (item && this.visible(item)) this.navigateToItem(item);
      }
      return;
    }

    if (key === 'g') {
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
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  private trackRecentNavigation(url: string): void {
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
