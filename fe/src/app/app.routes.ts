import { Routes } from '@angular/router';
import { authGuard, mustChangePasswordGuard, roleGuard } from './core/auth/auth.guard';
import { unsavedChangesGuard } from './core/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((module) => module.LoginPage),
  },
  {
    path: 'change-password',
    canActivate: [mustChangePasswordGuard],
    loadComponent: () =>
      import('./features/change-password/change-password.page').then((module) => module.ChangePasswordPage),
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
        canDeactivate: [unsavedChangesGuard],
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
        path: 'trade/procurement',
        canActivate: [roleGuard],
        data: {
          roles: [
            'PROCUREMENT_MANAGER',
            'PROCUREMENT_USER',
            'INVENTORY_MANAGER',
            'FINANCE_MANAGER',
            'ACCOUNTANT',
            'TREASURY_USER',
            'HR_MANAGER',
            'AUDITOR',
          ],
        },
        loadComponent: () =>
          import('./features/trade/procurement/procurement.page').then((module) => module.ProcurementPage),
      },
      {
        path: 'trade/sales',
        canActivate: [roleGuard],
        data: { roles: ['SALES_MANAGER', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/trade/sales/sales.page').then((module) => module.SalesPage),
      },
      {
        path: 'manufacturing/production',
        canActivate: [roleGuard],
        data: { roles: ['MANUFACTURING_MANAGER', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/manufacturing/production/production.page').then((module) => module.ProductionPage),
      },
      {
        path: 'manufacturing/quality',
        canActivate: [roleGuard],
        data: { roles: ['MANUFACTURING_MANAGER', 'QUALITY_MANAGER', 'HR_MANAGER'] },
        loadComponent: () =>
          import('./features/manufacturing/quality/quality.page').then((module) => module.QualityPage),
      },
      {
        path: 'payroll',
        canActivate: [roleGuard],
        data: { roles: ['PAYROLL_MANAGER', 'HR_MANAGER', 'HR_REVIEWER'] },
        loadComponent: () =>
          import('./features/payroll/payroll.page').then((module) => module.PayrollPage),
      },
      {
        path: 'finance/accounts',
        canActivate: [roleGuard],
        data: {
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'HR_MANAGER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/accounts/accounts.page').then((module) => module.AccountsPage),
      },
      {
        path: 'finance/journal-entries',
        canActivate: [roleGuard],
        data: {
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'HR_MANAGER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/journal-entries/journal-entries.page').then((module) => module.JournalEntriesPage),
      },
      {
        path: 'finance/banks',
        canActivate: [roleGuard],
        data: {
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'HR_MANAGER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/banks/banks.page').then((module) => module.BanksPage),
      },
      {
        path: 'finance/tax-currency',
        canActivate: [roleGuard],
        data: {
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'HR_MANAGER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/tax-currency/tax-currency.page').then((module) => module.TaxCurrencyPage),
      },
      {
        path: 'organization',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/organization/organization.page').then((module) => module.OrganizationPage),
      },
      {
        path: 'fiscal-periods',
        canActivate: [roleGuard],
        data: {
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'HR_MANAGER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/fiscal-periods/fiscal-periods.page').then((module) => module.FiscalPeriodsPage),
      },
      {
        path: 'audit-logs',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/audit-logs/audit-logs.page').then((module) => module.AuditLogsPage),
      },
      {
        path: 'reports/:id',
        loadComponent: () =>
          import('./features/reports/report-review.page').then((module) => module.ReportReviewPage),
      },
      {
        path: 'settings',
        canDeactivate: [unsavedChangesGuard],
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
      {
        path: 'workforce',
        canActivate: [roleGuard],
        data: {
          roles: ['WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE', 'HR_MANAGER', 'HR_REVIEWER'],
        },
        loadChildren: () =>
          import('./features/workforce/workforce.routes').then((module) => module.WORKFORCE_ROUTES),
      },
      {
        path: 'forbidden',
        loadComponent: () =>
          import('./features/errors/forbidden.page').then((module) => module.ForbiddenPage),
      },
      {
        path: 'not-found',
        loadComponent: () =>
          import('./features/errors/not-found.page').then((module) => module.NotFoundPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: '**', redirectTo: 'not-found' },
    ],
  },
  { path: '**', redirectTo: 'not-found' },
];
