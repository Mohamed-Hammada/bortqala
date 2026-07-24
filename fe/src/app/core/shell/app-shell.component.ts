import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { RoleCode } from '../auth/auth.models';
import { I18nService } from '../i18n.service';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';

interface NavItem {
  labelKey: string;
  descriptionKey: string;
  path: string;
  icon: IconName;
  roles?: RoleCode[];
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly menuOpen = signal(false);
  private readonly router = inject(Router);
  readonly items: NavItem[] = [
    {
      labelKey: 'nav.dashboard',
      descriptionKey: 'nav.dashboardHint',
      path: '/dashboard',
      icon: 'dashboard',
    },
    {
      labelKey: 'nav.categories',
      descriptionKey: 'nav.categoriesHint',
      path: '/categories',
      icon: 'categories',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      labelKey: 'nav.employees',
      descriptionKey: 'nav.employeesHint',
      path: '/employees',
      icon: 'employees',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      labelKey: 'nav.imports',
      descriptionKey: 'nav.importsHint',
      path: '/imports',
      icon: 'imports',
      roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
    },
    {
      labelKey: 'nav.parties',
      descriptionKey: 'nav.partiesHint',
      path: '/parties',
      icon: 'users',
      roles: ['ADMIN', 'HR_MANAGER'],
    },
    {
      labelKey: 'nav.reports',
      descriptionKey: 'nav.reportsHint',
      path: '/reports',
      icon: 'reports',
    },
    {
      labelKey: 'nav.users',
      descriptionKey: 'nav.usersHint',
      path: '/users',
      icon: 'users',
      roles: ['ADMIN'],
    },
  ];

  visible(item: NavItem): boolean {
    return !item.roles || this.authService.hasAnyRole(item.roles);
  }
  closeMenu(): void {
    this.menuOpen.set(false);
  }
  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
