import { RoleCode } from './auth.models';

/**
 * Static snapshot of the backend access catalog's page route-guard contract.
 *
 * <p>This fixture mirrors {@code AccessCatalog.pages()} in
 * {@code be/src/main/java/com/bemo/hr/access/domain/AccessCatalog.java}. The
 * backend catalog remains the single source of truth; whenever it changes, this
 * fixture must be updated to match or {@code page-access-consistency.spec.ts}
 * fails. {@code roles} includes ADMIN/SUPER_ADMIN exactly like the backend
 * (they bypass every route guard). {@code requiredFeature} is the tenant
 * feature key gating the menu, or null when the menu is not feature-gated.
 */
export interface CatalogPageContract {
  menuId: string;
  route: string;
  roles: RoleCode[];
  requiredFeature: string | null;
}

export const CATALOG_PAGE_CONTRACT: CatalogPageContract[] = [
  { menuId: 'dashboard', route: '/dashboard', roles: [], requiredFeature: null },
  { menuId: 'employees', route: '/employees', roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER'], requiredFeature: null },
  { menuId: 'categories', route: '/categories', roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER'], requiredFeature: null },
  { menuId: 'imports', route: '/imports', roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'HR_REVIEWER'], requiredFeature: null },
  { menuId: 'parties', route: '/parties', roles: ['ADMIN', 'SUPER_ADMIN', 'HR_MANAGER'], requiredFeature: null },
  { menuId: 'reports', route: '/reports', roles: [], requiredFeature: null },
  { menuId: 'operations', route: '/operations', roles: ['ADMIN', 'SUPER_ADMIN'], requiredFeature: null },
  {
    menuId: 'procurement',
    route: '/trade/procurement',
    roles: [
      'ADMIN', 'SUPER_ADMIN', 'PROCUREMENT_MANAGER', 'PROCUREMENT_USER',
      'INVENTORY_MANAGER', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR',
    ],
    requiredFeature: null,
  },
  { menuId: 'sales', route: '/trade/sales', roles: ['ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER'], requiredFeature: 'sales.enabled' },
  { menuId: 'production', route: '/manufacturing/production', roles: ['ADMIN', 'SUPER_ADMIN', 'MANUFACTURING_MANAGER'], requiredFeature: 'manufacturing.enabled' },
  { menuId: 'quality', route: '/manufacturing/quality', roles: ['ADMIN', 'SUPER_ADMIN', 'MANUFACTURING_MANAGER', 'QUALITY_MANAGER'], requiredFeature: 'quality.enabled' },
  { menuId: 'payroll', route: '/payroll', roles: ['ADMIN', 'SUPER_ADMIN', 'PAYROLL_MANAGER', 'HR_MANAGER', 'HR_REVIEWER'], requiredFeature: 'payroll.enabled' },
  {
    menuId: 'accounts',
    route: '/finance/accounts',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  {
    menuId: 'journal-entries',
    route: '/finance/journal-entries',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  {
    menuId: 'banks',
    route: '/finance/banks',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  {
    menuId: 'tax-currency',
    route: '/finance/tax-currency',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  {
    menuId: 'fiscal-periods',
    route: '/fiscal-periods',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  {
    menuId: 'budgets',
    route: '/finance/budgets',
    roles: ['ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'],
    requiredFeature: 'finance.enabled',
  },
  { menuId: 'organization', route: '/organization', roles: ['ADMIN', 'SUPER_ADMIN'], requiredFeature: null },
  { menuId: 'audit-logs', route: '/audit-logs', roles: ['ADMIN', 'SUPER_ADMIN'], requiredFeature: null },
  { menuId: 'users', route: '/users', roles: ['ADMIN', 'SUPER_ADMIN'], requiredFeature: null },
  { menuId: 'settings', route: '/settings', roles: [], requiredFeature: null },
  {
    menuId: 'workforce-dashboard',
    route: '/workforce/dashboard',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-contractors',
    route: '/workforce/contractors',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-workers',
    route: '/workforce/workers',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-categories',
    route: '/workforce/categories',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-requests',
    route: '/workforce/labor-requests',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-attendance',
    route: '/workforce/attendance',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-settlements',
    route: '/workforce/settlement-periods',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: 'workforce.contractorAccounts.enabled',
  },
  {
    menuId: 'workforce-advances',
    route: '/workforce/advances',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER', 'WORKFORCE_FINANCE'],
    requiredFeature: null,
  },
  {
    menuId: 'workforce-accounts',
    route: '/workforce/contractor-accounts',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_FINANCE'],
    requiredFeature: 'workforce.contractorAccounts.enabled',
  },
  {
    menuId: 'workforce-reports',
    route: '/workforce/reports-import',
    roles: ['ADMIN', 'SUPER_ADMIN', 'WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER'],
    requiredFeature: null,
  },
  {
    menuId: 'approvals-my-tasks',
    route: '/approvals/my-tasks',
    roles: [],
    requiredFeature: null,
  },
  {
    menuId: 'approvals-workflows',
    route: '/approvals/definitions',
    roles: ['ADMIN', 'SUPER_ADMIN'],
    requiredFeature: null,
  },
  {
    menuId: 'notifications-send',
    route: '/notifications/send',
    roles: ['ADMIN', 'SUPER_ADMIN'],
    requiredFeature: null,
  },
];
