import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { RoleCode } from '../auth/auth.models';
import { I18nService } from '../i18n.service';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';
import { ToastContainerComponent } from '../../shared/ui/toast/toast-container.component';

interface NavItem {
  menuId: string;
  labelKey: string;
  descriptionKey: string;
  path: string;
  icon: IconName;
  roles?: RoleCode[];
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
  readonly menuOpen = signal(false);
  readonly collapsed = signal(false);
  private readonly router = inject(Router);
  readonly items: NavItem[] = [
    {
      menuId: 'dashboard',
      labelKey: 'nav.dashboard',
      descriptionKey: 'nav.dashboardHint',
      path: '/dashboard',
      icon: 'dashboard',
    },
    {
      menuId: 'categories',
      labelKey: 'nav.categories',
      descriptionKey: 'nav.categoriesHint',
      path: '/categories',
      icon: 'categories',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'employees',
      labelKey: 'nav.employees',
      descriptionKey: 'nav.employeesHint',
      path: '/employees',
      icon: 'employees',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'imports',
      labelKey: 'nav.imports',
      descriptionKey: 'nav.importsHint',
      path: '/imports',
      icon: 'imports',
      roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
    },
    {
      menuId: 'parties',
      labelKey: 'nav.parties',
      descriptionKey: 'nav.partiesHint',
      path: '/parties',
      icon: 'users',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'reports',
      labelKey: 'nav.reports',
      descriptionKey: 'nav.reportsHint',
      path: '/reports',
      icon: 'reports',
    },
    {
      menuId: 'operations',
      labelKey: 'nav.operations',
      descriptionKey: 'nav.operationsHint',
      path: '/operations',
      icon: 'categories',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      menuId: 'users',
      labelKey: 'nav.users',
      descriptionKey: 'nav.usersHint',
      path: '/users',
      icon: 'users',
      roles: ['ADMIN'],
    },
  ];

  visible(item: NavItem): boolean {
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
