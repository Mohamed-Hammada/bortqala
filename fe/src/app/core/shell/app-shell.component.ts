import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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

@Component({
  selector: 'app-shell',
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

  visible(item: NavItem): boolean {
    const user = this.authService.user();
    if (user && user.roles.includes('SUPER_ADMIN')) {
      return true;
    }
    const roleOk = !item.roles || this.authService.hasAnyRole(item.roles);
    const menuOk = this.authService.hasMenuAccess(item.menuId);
    return roleOk && menuOk;
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
