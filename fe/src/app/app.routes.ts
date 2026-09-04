import { Routes } from '@angular/router';
import { authGuard, menuAccessGuard, mustChangePasswordGuard, roleGuard, superAdminGuard } from './core/auth/auth.guard';
import { unsavedChangesGuard } from './core/unsaved-changes.guard';
import { WORKFORCE_BASE_ROLES } from './core/auth/workforce-role.guard';

export const routes: Routes = [
  {
    // WP-14 AC-1: native first-launch server picker — deliberately outside the auth guard.
    path: 'server-setup',
    loadComponent: () => import('./features/server-setup/server-setup.page').then((module) => module.ServerSetupPage),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.page').then((module) => module.LoginPage),
  },
  {
    // WP-14 AC-3: employee selfie punch — any authenticated user, no menu row required.
    path: 'selfie-punch',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/selfie-punch/selfie-punch.page').then((module) => module.SelfiePunchPage),
  },
  {
    path: 'change-password',
    canActivate: [mustChangePasswordGuard],
    loadComponent: () =>
      import('./features/change-password/change-password.page').then((module) => module.ChangePasswordPage),
  },
  {
    // WP-29: public payment page — no auth required
    path: 'p/:token',
    loadComponent: () =>
      import('./features/public/pay/pay.page').then((module) => module.PublicPayPage),
  },
  {
    // P1-01: Public storefront product catalog browsing
    path: 'products',
    loadComponent: () =>
      import('./features/public/catalog/public-catalog.page').then((module) => module.PublicCatalogPage),
  },
  {
    path: 'products/:slug',
    loadComponent: () =>
      import('./features/public/catalog/public-product-detail.page').then((module) => module.PublicProductDetailPage),
  },
  {
    path: 'categories/:slug',
    loadComponent: () =>
      import('./features/public/catalog/public-catalog.page').then((module) => module.PublicCatalogPage),
  },
  {
    path: 'brands/:slug',
    loadComponent: () =>
      import('./features/public/catalog/public-catalog.page').then((module) => module.PublicCatalogPage),
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
        path: 'projects',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          roles: ['SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER'],
          menuId: 'projects',
        },
        loadChildren: () =>
          import('./features/projects/projects.routes').then((module) => module.PROJECT_ROUTES),
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
        path: 'leaves',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'leaves' },
        loadComponent: () =>
          import('./features/leaves/leaves.page').then((module) => module.LeavesPage),
      },
      {
        path: 'performance',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'performance' },
        loadComponent: () =>
          import('./features/performance/performance.page').then((module) => module.PerformancePage),
      },
      {
        path: 'imports',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/imports/imports.page').then((module) => module.ImportsPage),
      },
      {
        path: 'imports/history',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () => import('./features/imports/import-history.page').then((m) => m.ImportHistoryPage),
      },
      {
        path: 'imports/attendance/:deviceUserId',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/attendance-browser/attendance-employee.page').then((module) => module.AttendanceEmployeePage),
      },
      {
        path: 'imports/attendance',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/attendance-browser/attendance-browser.page').then((module) => module.AttendanceBrowserPage),
      },
      {
        // device-integrations-route: shares the Attendance Imports permission/menu scope.
        path: 'imports/device-integrations',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'imports' },
        loadComponent: () =>
          import('./features/device-integrations/device-integrations.page').then(
            (module) => module.DeviceIntegrationsPage,
          ),
      },
      {
        path: 'smart-import',
        canActivate: [roleGuard],
        data: {
          roles: [
            'SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER', 'PAYROLL_MANAGER',
            'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR',
            'PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'INVENTORY_MANAGER',
            'SALES_MANAGER', 'MANUFACTURING_MANAGER', 'QUALITY_MANAGER',
          ],
        },
        loadComponent: () =>
          import('./features/smart-import/smart-import.page').then((module) => module.SmartImportPage),
      },
      {
        path: 'smart-import/:workflow',
        canActivate: [roleGuard],
        data: {
          roles: [
            'SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER', 'PAYROLL_MANAGER',
            'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR',
            'PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'INVENTORY_MANAGER',
            'SALES_MANAGER', 'MANUFACTURING_MANAGER', 'QUALITY_MANAGER',
          ],
        },
        loadComponent: () =>
          import('./features/smart-import/smart-import.page').then((module) => module.SmartImportPage),
      },
      {
        path: 'migration',
        canActivate: [roleGuard],
        data: {
          roles: ['SUPER_ADMIN', 'ADMIN'],
        },
        loadComponent: () =>
          import('./features/migration/data-migration.component').then((module) => module.DataMigrationComponent),
      },
      {
        path: 'parties',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'], menuId: 'parties' },
        loadComponent: () =>
          import('./features/parties/parties.page').then((module) => module.PartiesPage),
      },
      {
        path: 'partner-risk',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          roles: ['SUPER_ADMIN', 'ADMIN', 'PROCUREMENT_MANAGER', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER'],
          menuId: 'settings',
        },
        loadComponent: () =>
          import('./features/partner-risk/partner-risk.page').then((module) => module.PartnerRiskPage),
      },
      {
        path: 'analytics/executive',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          roles: ['SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER'],
          menuId: 'executive-analytics',
        },
        loadComponent: () =>
          import('./features/analytics/executive/executive-analytics.page').then((m) => m.ExecutiveAnalyticsPage),
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
        data: { roles: ['SUPER_ADMIN', 'ADMIN'], menuId: 'approvals-workflows' },
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
        path: 'trade/export-shipments',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'export-shipments',
          roles: ['PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'SALES_MANAGER', 'FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/trade/export-shipments/export-shipments.page').then((module) => module.ExportShipmentsPage),
      },
      {
        path: 'trade/pos',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SALES_MANAGER'], menuId: 'pos' },
        loadComponent: () =>
          import('./features/trade/pos/pos.page').then((module) => module.PosPage),
      },
      {
        path: 'trade/field-sales',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SALES_MANAGER'], menuId: 'field-sales' },
        loadComponent: () =>
          import('./features/trade/field-sales/field-sales.page').then((module) => module.FieldSalesPage),
      },
      {
        path: 'crm',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SALES_MANAGER'], menuId: 'crm' },
        loadComponent: () =>
          import('./features/crm/crm.page').then((module) => module.CrmPage),
      },
      {
        path: 'verticals/specialized',
        canActivate: [menuAccessGuard],
        data: { menuId: 'specialized-verticals' },
        loadComponent: () =>
          import('./features/verticals/verticals.page').then((m) => m.VerticalsPage),
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
        path: 'finance/fixed-assets',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'fixed-assets',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/finance/fixed-assets/fixed-assets.page').then((module) => module.FixedAssetsPage),
      },
      {
        path: 'finance/payment-links',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'payment-links',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR', 'ADMIN', 'SUPER_ADMIN'],
        },
        loadComponent: () =>
          import('./features/finance/payment-links/payment-links.page').then((module) => module.PaymentLinksPage),
      },
      {
        path: 'finance/reconciliation',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'accounts',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR', 'ADMIN', 'SUPER_ADMIN'],
        },
        loadComponent: () =>
          import('./features/finance/reconciliation-center/reconciliation-center.component').then(
            (module) => module.ReconciliationCenterComponent,
          ),
      },
      {
        path: 'whatsapp',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN'], menuId: 'settings' },
        loadComponent: () =>
          import('./features/whatsapp/whatsapp.page').then((module) => module.WhatsAppPage),
      },
      {
        path: 'automation',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN'], menuId: 'settings' },
        loadComponent: () =>
          import('./features/automation/automation.page').then((module) => module.AutomationPage),
      },
      {
        path: 'expenses',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'expenses',
          roles: ['HR_MANAGER', 'ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT'],
        },
        loadComponent: () =>
          import('./features/expenses/expenses.page').then((module) => module.ExpensesPage),
      },
      {
        path: 'compliance/eta-tax',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'eta-tax',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
        },
        loadComponent: () =>
          import('./features/compliance/eta-tax/eta-tax.page').then((module) => module.EtaTaxPage),
      },
      {
        path: 'organization',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER'], menuId: 'organization' },
        loadComponent: () =>
          import('./features/organization/organization.page').then((module) => module.OrganizationPage),
      },
      {
        path: 'fiscal-periods',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'fiscal-periods',
          roles: ['FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR'],
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
        path: 'notifications/send',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'notifications-send' },
        loadComponent: () =>
          import('./features/notifications-send/notifications-send.page').then((module) => module.NotificationsSendPage),
      },
      {
        path: 'admin/setup-readiness',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN'], menuId: 'settings' },
        loadComponent: () =>
          import('./features/admin/setup-readiness/setup-readiness.page').then((module) => module.SetupReadinessPage),
      },
      {
        path: 'admin/product-insights',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN'], menuId: 'settings' },
        loadComponent: () =>
          import('./features/admin/product-insights/product-insights.page').then((module) => module.ProductInsightsPage),
      },
      {
        path: 'platform-admin',
        canActivate: [superAdminGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN'], menuId: 'settings' },
        loadComponent: () =>
          import('./features/platform-admin/platform-admin.page').then((module) => module.PlatformAdminPage),
      },
      {
        path: 'platform-admin/outbox',
        canActivate: [superAdminGuard],
        data: { roles: ['SUPER_ADMIN'] },
        loadComponent: () =>
          import('./features/platform-admin/pages/outbox/system-outbox.page').then((module) => module.SystemOutboxPage),
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
        path: 'support',
        loadComponent: () =>
          import('./features/support/support.page').then((module) => module.SupportPage),
      },
      {
        path: 'retail/laptops',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER'], menuId: 'pos' },
        loadComponent: () =>
          import('./features/retail/laptop-retail.page').then((module) => module.LaptopRetailPage),
      },
      {
        path: 'helpdesk',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'helpdesk' },
        loadComponent: () =>
          import('./features/helpdesk/helpdesk.page').then((module) => module.HelpdeskPage),
      },
      {
        path: 'kb',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], menuId: 'kb' },
        loadComponent: () =>
          import('./features/knowledge-base/kb.page').then((module) => module.KbPage),
      },
      {
        path: 'marketing',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER'], menuId: 'marketing' },
        loadComponent: () =>
          import('./features/marketing/marketing.page').then((module) => module.MarketingPage),
      },
      {
        path: 'report-builder',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'HR_MANAGER', 'VIEWER'], menuId: 'report-builder' },
        loadComponent: () =>
          import('./features/report-builder/report-builder.page').then((module) => module.ReportBuilderPage),
      },
      {
        path: 'about',
        loadComponent: () =>
          import('./features/about/about.page').then((module) => module.AboutPage),
      },
      {
        path: 'users',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['ADMIN'], menuId: 'users' },
        loadComponent: () =>
          import('./features/users/users.page').then((module) => module.UsersPage),
      },
      {
        path: 'access/policy-groups',
        canActivate: [roleGuard, menuAccessGuard],
        data: { roles: ['SUPER_ADMIN', 'ADMIN'], menuId: 'users' },
        loadComponent: () =>
          import('./features/users/pages/policy-groups/policy-groups.page').then((m) => m.PolicyGroupsPageComponent),
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
        path: 'growth',
        canActivate: [menuAccessGuard],
        data: { menuId: 'growth', roles: ['ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER'] },
        loadComponent: () =>
          import('./features/growth/growth.page').then((module) => module.GrowthPage),
      },
      {
        path: 'recruitment',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'recruitment',
          roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
        },
        loadComponent: () =>
          import('./features/recruitment/recruitment.page').then((module) => module.RecruitmentPage),
      },
      {
        path: 'documents',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'documents',
          roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
        },
        loadComponent: () =>
          import('./features/documents/documents.page').then((module) => module.DocumentsPage),
      },
      {
        path: 'esign',
        canActivate: [roleGuard, menuAccessGuard],
        data: {
          menuId: 'esign',
          roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
        },
        loadComponent: () =>
          import('./features/esign/esign.page').then((module) => module.ESignPage),
      },
      {
        path: 'clinic/patients',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-patients' },
        loadComponent: () =>
          import('./features/clinic/patients.page').then((m) => m.PatientsPageComponent),
      },
      {
        path: 'clinic/patients/:id/chart',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-patients' },
        loadComponent: () =>
          import('./features/clinic/patient-chart.page').then((m) => m.PatientChartPageComponent),
      },
      {
        path: 'clinic/queue',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-queue' },
        loadComponent: () =>
          import('./features/clinic/clinic-queue.page').then((m) => m.ClinicQueuePageComponent),
      },
      {
        path: 'clinic/commissions',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-commissions' },
        loadComponent: () =>
          import('./features/clinic/clinic-commissions.page').then((m) => m.ClinicCommissionsPageComponent),
      },
      {
        path: 'clinic/appointments',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-appointments' },
        loadComponent: () =>
          import('./features/clinic/appointments.page').then((m) => m.AppointmentsPageComponent),
      },
      {
        path: 'clinic/pharmacy',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-pharmacy' },
        loadComponent: () =>
          import('./features/clinic/pharmacy.page').then((m) => m.PharmacyPageComponent),
      },
      {
        path: 'clinic/lab',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-lab' },
        loadComponent: () =>
          import('./features/clinic/lab-orders.page').then((m) => m.LabOrdersPageComponent),
      },
      {
        path: 'clinic/insurance',
        canActivate: [menuAccessGuard],
        data: { menuId: 'clinic-insurance' },
        loadComponent: () =>
          import('./features/clinic/insurance.page').then((m) => m.InsurancePageComponent),
      },
      {
        path: 'clinic/hospital',
        canActivate: [menuAccessGuard],
        data: { menuId: 'hospital-ops' },
        loadComponent: () =>
          import('./features/clinic/hospital-ops.page').then((m) => m.HospitalOpsPageComponent),
      },
      {
        path: 'clinic/dental',
        canActivate: [menuAccessGuard],
        data: { menuId: 'dental-charting' },
        loadComponent: () =>
          import('./features/clinic/dental-charting.page').then((m) => m.DentalChartingPageComponent),
      },
      {
        path: 'clinic/tools',
        canActivate: [menuAccessGuard],
        data: { menuId: 'medical-tools' },
        loadComponent: () =>
          import('./features/clinic/medical-tools.page').then((m) => m.MedicalToolsPageComponent),
      },
      {
        path: 'service-ops',
        canActivate: [menuAccessGuard],
        data: { menuId: 'service-ops' },
        loadComponent: () =>
          import('./features/service-ops/service-ops.page').then((m) => m.ServiceOpsPageComponent),
      },
      {
        path: 'fleet',
        canActivate: [menuAccessGuard],
        data: { menuId: 'fleet' },
        loadComponent: () =>
          import('./features/fleet/fleet.page').then((m) => m.FleetPageComponent),
      },
      {
        path: 'self-service',
        canActivate: [menuAccessGuard],
        data: { menuId: 'self-service' },
        loadComponent: () =>
          import('./features/self-service/ess.page').then((m) => m.EssPageComponent),
      },
      {
        path: 'ai-intelligence',
        canActivate: [menuAccessGuard],
        data: { menuId: 'ai-intelligence' },
        loadComponent: () =>
          import('./features/ai-intelligence/ai-intelligence.page').then((m) => m.AiIntelligencePageComponent),
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

// BORTQALA_FEEDBACK_20260816_ORGANIZATION route parity
