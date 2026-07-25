import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((module) => module.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/shell/app-shell.component').then((module) => module.AppShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((module) => module.DashboardPage),
      },
      {
        path: 'categories',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/categories/categories.page').then((module) => module.CategoriesPage),
      },
      {
        path: 'employees',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/employees/employees.page').then((module) => module.EmployeesPage),
      },
      {
        path: 'imports',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'] },
        loadComponent: () =>
          import('./features/imports/imports.page').then((module) => module.ImportsPage),
      },
      {
        path: 'parties',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/parties/parties.page').then((module) => module.PartiesPage),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports.page').then((module) => module.ReportsPage),
      },
      {
        path: 'operations',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/operations/operations.page').then((module) => module.OperationsPage),
      },
      {
        path: 'reports/:id',
        loadComponent: () =>
          import('./features/reports/report-review.page').then((module) => module.ReportReviewPage),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.page').then((module) => module.SettingsPage),
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/users/users.page').then((module) => module.UsersPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: '' },
];
