import { Routes } from '@angular/router';
import { authGuard, menuAccessGuard, mustChangePasswordGuard, roleGuard } from './core/auth/auth.guard';
import { unsavedChangesGuard } from './core/unsaved-changes.guard';
import { WORKFORCE_BASE_ROLES } from './core/auth/workforce-role.guard';

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
        canActivate: [menuAccessGuard],
        data: { menuId: 'dashboard' },
        loadComponent: () =>
          import('./features/dashboard/dashboard.page').then((module) => module.DashboardPage),
      },
      {
        path: 'categories',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'], menuId: 'categories' },
        loadComponent: () =>
          import('./features/categories/categories.page').then((module) => module.CategoriesPage),
      },
      {
        path: 'employees',
        canActivate: [roleGuard, menuAccessGuard],
        canDeactivate: [unsavedChangesGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'], menuId: 'employees' },
        loadComponent: () =>
          import('./features/employees/employees.page').then((module) => module.EmployeesPage),
      },
      {
        path: 'imports',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/imports/imports.page').then((module) => module.ImportsPage),
      },
      {
        path: 'parties',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'], menuId: 'parties' },
        loadComponent: () =>
          import('./features/parties/parties.page').then((module) => module.PartiesPage),
      },
      {
        path: 'reports',
        canActivate: [menuAccessGuard],
        data: { menuId: 'reports' },
        loadComponent: () =>
          import('./features/reports/reports.page').then((module) => module.ReportsPage),
      },
      {
        path: 'approvals/my-tasks',
        canActivate: [menuAccessGuard],
        data: { menuId: 'approvals-my-tasks' },
        loadComponent: () =>
          import('./features/approvals/pages/pending-approvals/pending-approvals.component').then(
            (m) => m.PendingApprovalsComponent,
          ),
      },
      {
        path: 'approvals/definitions',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'approvals-workflows' },
        loadComponent: () =>
          import('./features/approvals/pages/workflow-definitions/workflow-definitions.component').then(
            (m) => m.WorkflowDefinitionsComponent,
          ),
      },
      {
        path: 'operations',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'operations' },
        loadComponent: () =>
          import('./features/operations/operations.page').then((module) => module.OperationsPage),
      },
      {
        path: 'trade/procurement',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'procurement',
          roles: [
            'PROCUREMENT_MANAGER',
            'PROCUREMENT_USER',
            'INVENTORY_MANAGER',
            'FINANCE_MANAGER',
            'ACCOUNTANT',
            'TREASURY_USER',
            'AUDITOR',
          ],
        },
        loadComponent: () =>
          import('./features/trade/procurement/procurement.page').then((module) => module.ProcurementPage),
      },
      {
        path: 'trade/sales',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SALES_MANAGER'], menuId: 'sales' },
        loadComponent: () =>
          import('./features/trade/sales/sales.page').then((module) => module.SalesPage),
      },
      {
        path: 'manufacturing/production',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['MANUFACTURING_MANAGER'], menuId: 'production' },
        loadComponent: () =>
          import('./features/manufacturing/production/production.page').then((module) => module.ProductionPage),
      },
      {
        path: 'manufacturing/quality',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['MANUFACTURING_MANAGER', 'QUALITY_MANAGER'], menuId: 'quality' },
        loadComponent: () =>
          import('./features/manufacturing/quality/quality.page').then((module) => module.QualityPage),
      },
      {
        path: 'payroll',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['PAYROLL_MANAGER', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'payroll' },
        loadComponent: () =>
          import('./features/payroll/payroll.page').then((module) => module.PayrollPage),
      },
      {
        path: 'finance/accounts',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'accounts',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/accounts/accounts.page').then((module) => module.AccountsPage),
      },
      {
        path: 'finance/journal-entries',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'journal-entries',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/journal-entries/journal-entries.page').then((module) => module.JournalEntriesPage),
      },
      {
        path: 'finance/banks',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'banks',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/banks/banks.page').then((module) => module.BanksPage),
      },
      {
        path: 'finance/tax-currency',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'tax-currency',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/tax-currency/tax-currency.page').then((module) => module.TaxCurrencyPage),
      },
      {
        path: 'finance/budgets',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'budgets',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/budgets/budgets.page').then((module) => module.BudgetsPage),
      },
      {
        path: 'organization',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'organization' },
        loadComponent: () =>
          import('./features/organization/organization.page').then((module) => module.OrganizationPage),
      },
      {
        path: 'fiscal-periods',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'fiscal-periods',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/fiscal-periods/fiscal-periods.page').then((module) => module.FiscalPeriodsPage),
      },
      {
        path: 'audit-logs',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'audit-logs' },
        loadComponent: () =>
          import('./features/audit-logs/audit-logs.page').then((module) => module.AuditLogsPage),
      },
      {
        path: 'reports/:id',
        canActivate: [menuAccessGuard],
        data: { menuId: 'reports' },
        loadComponent: () =>
          import('./features/reports/report-review.page').then((module) => module.ReportReviewPage),
      },
      {
        path: 'settings',
        canActivate: [menuAccessGuard],
        canDeactivate: [unsavedChangesGuard],
        data: { menuId: 'settings' },
        loadComponent: () =>
          import('./features/settings/settings.page').then((module) => module.SettingsPage),
      },
      {
        path: 'users',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'users' },
        loadComponent: () =>
          import('./features/users/users.page').then((module) => module.UsersPage),
      },
      {
        path: 'workforce',
        canActivate: [roleGuard],
        data: {
          roles: [...WORKFORCE_BASE_ROLES],
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
