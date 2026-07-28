import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { RoleCode } from '../auth/auth.models';
import { I18nService } from '../i18n.service';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';
import { ToastContainerComponent } from '../../shared/ui/toast/toast-container.component';
import { NetworkService } from '../network.service';

export type WorkspaceGroup =
  | 'workspace.people'
  | 'workspace.attendance'
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

const FAVORITES_STORAGE_KEY = 'hr-favorites';
const RECENT_STORAGE_KEY = 'hr-recent-menus';
const COLLAPSED_GROUPS_KEY = 'hr-collapsed-groups';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent, ToastContainerComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly network = inject(NetworkService);
  readonly router = inject(Router);

  readonly searchQuery = signal('');
  readonly menuOpen = signal(false);
  readonly collapsed = signal(false);

  readonly favorites = signal<string[]>(this.loadStoredArray(FAVORITES_STORAGE_KEY, ['payroll', 'employees']));
  readonly recentIds = signal<string[]>(this.loadStoredArray(RECENT_STORAGE_KEY, ['dashboard']));
  readonly collapsedGroups = signal<string[]>(this.loadStoredArray(COLLAPSED_GROUPS_KEY, []));

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

  readonly favoriteItems = computed<NavItem[]>(() => {
    const favs = this.favorites();
    return this.items.filter((item) => favs.includes(item.menuId) && this.visible(item));
  });

  readonly recentItems = computed<NavItem[]>(() => {
    const recents = this.recentIds();
    const favs = this.favorites();
    return this.items
      .filter((item) => recents.includes(item.menuId) && !favs.includes(item.menuId) && this.visible(item))
      .slice(0, 4);
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
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.trackRecentNavigation(event.urlAfterRedirects);
      });
  }

  visible(item: NavItem): boolean {
    const user = this.authService.user();
    if (user && user.roles.includes('SUPER_ADMIN')) {
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
    localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(updated));
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

  private pushRecent(menuId: string): void {
    const current = this.recentIds().filter((id) => id !== menuId);
    const updated = [menuId, ...current].slice(0, 5);
    this.recentIds.set(updated);
    localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(updated));
  }

  private loadStoredArray(key: string, fallback: string[]): string[] {
    try {
      const val = localStorage.getItem(key);
      return val ? JSON.parse(val) : fallback;
    } catch {
      return fallback;
    }
  }
}
