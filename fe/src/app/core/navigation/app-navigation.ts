import { RoleCode } from '../auth/auth.models';
import { IconName } from '../../shared/ui/icon/icon.component';

/**
 * Canonical application navigation metadata.
 *
 * Keep menu ids and routes stable because they are part of the authorization,
 * favorites/recent-pages, shortcuts, and deep-link contracts. Information
 * architecture (workspace + labels) can evolve independently here.
 */
export type WorkspaceGroup =
  | 'workspace.homeOverview'
  | 'workspace.projects'
  | 'workspace.peopleHr'
  | 'workspace.attendanceTime'
  | 'workspace.contractorWorkforce'
  | 'workspace.approvalsWorkflow'
  | 'workspace.supplyChainInventory'
  | 'workspace.salesCommercial'
  | 'workspace.manufacturingDomain'
  | 'workspace.businessPartners'
  | 'workspace.financeAccounting'
  | 'workspace.documents'
  | 'workspace.esign'
  | 'workspace.medical'
  | 'workspace.administration'
  | 'workspace.platformAdministration';

export interface NavItem {
  menuId: string;
  labelKey: string;
  descriptionKey: string;
  path: string;
  icon: IconName;
  workspace: WorkspaceGroup;
  roles?: RoleCode[];
  /** Optional authorization menu id when a child page shares an existing permission contract. */
  permissionMenuId?: string;
  /** Keep ADMIN from bypassing an explicitly narrower role scope. */
  strictRoles?: boolean;
  /** Pages such as Settings/Audit should not be selectable as the normal landing page. */
  allowAsLandingPage?: boolean;
  /** When false, this navigation item is not exposed in the user permission/menu editor. */
  showInPermissionEditor?: boolean;
}

export interface WorkspaceSection {
  titleKey: WorkspaceGroup;
  items: NavItem[];
}

export const WORKSPACE_ORDER: readonly WorkspaceGroup[] = [
  'workspace.homeOverview',
  'workspace.projects',
  'workspace.peopleHr',
  'workspace.attendanceTime',
  'workspace.contractorWorkforce',
  'workspace.approvalsWorkflow',
  'workspace.supplyChainInventory',
  'workspace.salesCommercial',
  'workspace.manufacturingDomain',
  'workspace.businessPartners',
  'workspace.financeAccounting',
  'workspace.documents',
  'workspace.esign',
  'workspace.medical',
  'workspace.administration',
  'workspace.platformAdministration',
];

const PROJECT_ROLES: RoleCode[] = ['SUPER_ADMIN', 'ADMIN', 'PROJECT_MANAGER', 'FINANCE_MANAGER', 'AUDITOR', 'VIEWER'];
const FINANCE_ROLES: RoleCode[] = ['FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR'];
const FINANCE_REPORT_ROLES: RoleCode[] = ['FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR'];
const PROCUREMENT_ROLES: RoleCode[] = [
  'PROCUREMENT_MANAGER',
  'PROCUREMENT_USER',
  'INVENTORY_MANAGER',
  'FINANCE_MANAGER',
  'ACCOUNTANT',
  'TREASURY_USER',
  'AUDITOR',
];
const SALES_ROLES: RoleCode[] = ['SALES_MANAGER'];
const EXPORT_ROLES: RoleCode[] = ['PROCUREMENT_MANAGER', 'PROCUREMENT_USER', 'SALES_MANAGER', 'FINANCE_MANAGER', 'ACCOUNTANT', 'AUDITOR'];
const PRODUCTION_ROLES: RoleCode[] = ['MANUFACTURING_MANAGER'];
const QUALITY_ROLES: RoleCode[] = ['MANUFACTURING_MANAGER', 'QUALITY_MANAGER'];
const PAYROLL_ROLES: RoleCode[] = ['PAYROLL_MANAGER', 'HR_MANAGER', 'HR_REVIEWER'];
const WORKFORCE_BASE_ROLES: RoleCode[] = [
  'WORKFORCE_MANAGER',
  'WORKFORCE_REVIEWER',
  'WORKFORCE_FINANCE',
];
const WORKFORCE_IMPORT_ROLES: RoleCode[] = ['WORKFORCE_MANAGER', 'WORKFORCE_REVIEWER'];
const WORKFORCE_ACCOUNT_ROLES: RoleCode[] = ['WORKFORCE_MANAGER', 'WORKFORCE_FINANCE'];

/**
 * Single source of truth for shell navigation and Settings > default landing page.
 * Existing menuId/path values are intentionally preserved.
 */
export const NAV_ITEMS: NavItem[] = [
  // Home / overview
  {
    menuId: 'dashboard',
    labelKey: 'nav.dashboard',
    descriptionKey: 'nav.dashboardHint',
    path: '/dashboard',
    icon: 'dashboard',
    workspace: 'workspace.homeOverview',
  },
  {
    menuId: 'executive-analytics',
    labelKey: 'nav.executiveAnalytics',
    descriptionKey: 'nav.executiveAnalyticsHint',
    path: '/analytics/executive',
    icon: 'reports',
    workspace: 'workspace.homeOverview',
    roles: PROJECT_ROLES,
  },

  // Projects / Construction Backbone
  {
    menuId: 'projects',
    labelKey: 'nav.projects',
    descriptionKey: 'nav.projectsHint',
    path: '/projects',
    icon: 'dashboard',
    workspace: 'workspace.projects',
    roles: PROJECT_ROLES,
  },

  // People & HR
  {
    menuId: 'employees',
    labelKey: 'nav.employees',
    descriptionKey: 'nav.employeesHint',
    path: '/employees',
    icon: 'employees',
    workspace: 'workspace.peopleHr',
    roles: ['ADMIN', 'HR_MANAGER'],
  },
  {
    menuId: 'leaves',
    labelKey: 'nav.leaves',
    descriptionKey: 'nav.leavesHint',
    path: '/leaves',
    icon: 'employees',
    workspace: 'workspace.peopleHr',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'performance',
    labelKey: 'nav.performance',
    descriptionKey: 'nav.performanceHint',
    path: '/performance',
    icon: 'dashboard',
    workspace: 'workspace.peopleHr',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'expenses',
    labelKey: 'nav.expenses',
    descriptionKey: 'nav.expensesHint',
    path: '/expenses',
    icon: 'dashboard',
    workspace: 'workspace.peopleHr',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'FINANCE_MANAGER', 'ACCOUNTANT'],
  },
  {
    menuId: 'recruitment',
    labelKey: 'nav.recruitment',
    descriptionKey: 'nav.recruitmentHint',
    path: '/recruitment',
    icon: 'employees',
    workspace: 'workspace.peopleHr',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'organization',
    labelKey: 'nav.organization',
    descriptionKey: 'nav.organizationHint',
    path: '/organization',
    icon: 'categories',
    workspace: 'workspace.peopleHr',
    roles: ['ADMIN', 'HR_MANAGER'],
  },
  {
    menuId: 'payroll',
    labelKey: 'nav.payroll',
    descriptionKey: 'nav.payrollHint',
    path: '/payroll',
    icon: 'reports',
    workspace: 'workspace.peopleHr',
    roles: PAYROLL_ROLES,
  },

  // Attendance & Time
  {
    menuId: 'categories',
    labelKey: 'nav.attendanceRulesCategories',
    descriptionKey: 'nav.attendanceRulesCategoriesHint',
    path: '/categories',
    icon: 'categories',
    workspace: 'workspace.attendanceTime',
    roles: ['ADMIN', 'HR_MANAGER'],
  },
  {
    menuId: 'imports',
    labelKey: 'nav.biometricAttendanceIntegration',
    descriptionKey: 'nav.biometricAttendanceIntegrationHint',
    path: '/imports',
    icon: 'imports',
    workspace: 'workspace.attendanceTime',
    roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'reports',
    labelKey: 'nav.attendanceReports',
    descriptionKey: 'nav.attendanceReportsHint',
    path: '/reports',
    icon: 'reports',
    workspace: 'workspace.attendanceTime',
  },

  // Contractor Workforce
  {
    menuId: 'workforce-dashboard',
    labelKey: 'workforce.dashboard.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/dashboard',
    icon: 'dashboard',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-contractors',
    labelKey: 'workforce.contractors.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/contractors',
    icon: 'users',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-workers',
    labelKey: 'workforce.workers.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/workers',
    icon: 'employees',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-categories',
    labelKey: 'workforce.categories.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/categories',
    icon: 'categories',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-requests',
    labelKey: 'workforce.laborRequests.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/labor-requests',
    icon: 'imports',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-attendance',
    labelKey: 'workforce.attendance.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/attendance',
    icon: 'reports',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-dispatch-disputes',
    labelKey: 'workforce.dispatch.title',
    descriptionKey: 'workforce.dispatch.hint',
    path: '/workforce/dispatch-disputes',
    icon: 'reports',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-settlements',
    labelKey: 'workforce.settlements.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/settlement-periods',
    icon: 'dashboard',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-advances',
    labelKey: 'workforce.advances.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/advances',
    icon: 'categories',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-accounts',
    labelKey: 'workforce.accounts.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/contractor-accounts',
    icon: 'users',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_ACCOUNT_ROLES,
  },
  {
    menuId: 'workforce-client-billing',
    labelKey: 'workforce.clientBilling.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/client-billing',
    icon: 'imports',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_BASE_ROLES,
  },
  {
    menuId: 'workforce-reports',
    labelKey: 'workforce.reports.title',
    descriptionKey: 'nav.workforceHint',
    path: '/workforce/reports-import',
    icon: 'reports',
    workspace: 'workspace.contractorWorkforce',
    roles: WORKFORCE_IMPORT_ROLES,
  },

  // Approvals & Workflow. This workspace was defined on items previously but omitted from shell groups.
  {
    menuId: 'approvals-my-tasks',
    labelKey: 'approvals.myTasks',
    descriptionKey: 'nav.approvalsHint',
    path: '/approvals/my-tasks',
    icon: 'reports',
    workspace: 'workspace.approvalsWorkflow',
  },
  {
    menuId: 'approvals-workflows',
    labelKey: 'approvals.workflows',
    descriptionKey: 'nav.approvalsHint',
    path: '/approvals/definitions',
    icon: 'categories',
    workspace: 'workspace.approvalsWorkflow',
    roles: ['SUPER_ADMIN', 'ADMIN'],
    allowAsLandingPage: false,
  },

  // Supply Chain & Inventory
  {
    menuId: 'operations',
    labelKey: 'nav.inventoryWarehouse',
    descriptionKey: 'nav.inventoryWarehouseHint',
    path: '/operations',
    icon: 'categories',
    workspace: 'workspace.supplyChainInventory',
    roles: ['ADMIN'],
  },
  {
    menuId: 'procurement',
    labelKey: 'nav.procurement',
    descriptionKey: 'nav.procurementHint',
    path: '/trade/procurement',
    icon: 'imports',
    workspace: 'workspace.supplyChainInventory',
    roles: PROCUREMENT_ROLES,
  },

  // Sales / Commercial
  {
    menuId: 'sales',
    labelKey: 'nav.sales',
    descriptionKey: 'nav.salesHint',
    path: '/trade/sales',
    icon: 'reports',
    workspace: 'workspace.salesCommercial',
    roles: SALES_ROLES,
  },
  {
    menuId: 'pos',
    labelKey: 'nav.pos',
    descriptionKey: 'nav.posHint',
    path: '/trade/pos',
    icon: 'reports',
    workspace: 'workspace.salesCommercial',
    roles: SALES_ROLES,
  },
  {
    menuId: 'crm',
    labelKey: 'nav.crm',
    descriptionKey: 'nav.crmHint',
    path: '/crm',
    icon: 'users',
    workspace: 'workspace.salesCommercial',
    roles: SALES_ROLES,
  },
  {
    menuId: 'export-shipments',
    labelKey: 'nav.exportShipments',
    descriptionKey: 'nav.exportShipmentsHint',
    path: '/trade/export-shipments',
    icon: 'imports',
    workspace: 'workspace.salesCommercial',
    roles: EXPORT_ROLES,
  },
  {
    menuId: 'specialized-verticals',
    labelKey: 'nav.specializedVerticals',
    descriptionKey: 'nav.specializedVerticalsHint',
    path: '/verticals/specialized',
    icon: 'categories',
    workspace: 'workspace.salesCommercial',
  },
  {
    menuId: 'growth',
    labelKey: 'nav.growth',
    descriptionKey: 'nav.growthHint',
    path: '/growth',
    icon: 'categories',
    workspace: 'workspace.salesCommercial',
    roles: ['ADMIN', 'SUPER_ADMIN', 'SALES_MANAGER', 'HR_MANAGER', 'FINANCE_MANAGER'],
  },
  {
    menuId: 'helpdesk',
    labelKey: 'nav.helpdesk',
    descriptionKey: 'nav.helpdeskHint',
    path: '/helpdesk',
    icon: 'dashboard',
    workspace: 'workspace.homeOverview',
    roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'kb',
    labelKey: 'nav.kb',
    descriptionKey: 'nav.kbHint',
    path: '/kb',
    icon: 'reports',
    workspace: 'workspace.homeOverview',
    roles: ['ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'marketing',
    labelKey: 'nav.marketing',
    descriptionKey: 'nav.marketingHint',
    path: '/marketing',
    icon: 'dashboard',
    workspace: 'workspace.salesCommercial',
    roles: ['ADMIN', 'SALES_MANAGER'],
  },
  {
    menuId: 'report-builder',
    labelKey: 'nav.reportBuilder',
    descriptionKey: 'nav.reportBuilderHint',
    path: '/report-builder',
    icon: 'reports',
    workspace: 'workspace.homeOverview',
    roles: ['ADMIN', 'FINANCE_MANAGER', 'HR_MANAGER', 'VIEWER'],
  },
  {
    menuId: 'documents',
    labelKey: 'nav.documents',
    descriptionKey: 'nav.documentsHint',
    path: '/documents',
    icon: 'reports',
    workspace: 'workspace.documents',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },
  {
    menuId: 'esign',
    labelKey: 'nav.esign',
    descriptionKey: 'nav.esignHint',
    path: '/esign',
    icon: 'reports',
    workspace: 'workspace.esign',
    roles: ['SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER'],
  },

  // Manufacturing
  {
    menuId: 'production',
    labelKey: 'nav.production',
    descriptionKey: 'nav.productionHint',
    path: '/manufacturing/production',
    icon: 'dashboard',
    workspace: 'workspace.manufacturingDomain',
    roles: PRODUCTION_ROLES,
  },
  {
    menuId: 'quality',
    labelKey: 'nav.quality',
    descriptionKey: 'nav.qualityHint',
    path: '/manufacturing/quality',
    icon: 'settings',
    workspace: 'workspace.manufacturingDomain',
    roles: QUALITY_ROLES,
  },

  // Business Partners / Master Data
  {
    menuId: 'parties',
    labelKey: 'nav.parties',
    descriptionKey: 'nav.partiesHint',
    path: '/parties',
    icon: 'users',
    workspace: 'workspace.businessPartners',
    roles: ['ADMIN', 'HR_MANAGER'],
  },

  {
    menuId: 'partner-risk',
    labelKey: 'risk.tab',
    descriptionKey: 'risk.description',
    path: '/partner-risk',
    icon: 'settings',
    workspace: 'workspace.businessPartners',
    roles: ['ADMIN', 'PROCUREMENT_MANAGER', 'WORKFORCE_MANAGER', 'FINANCE_MANAGER'],
    permissionMenuId: 'settings',
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },

  // Finance
  {
    menuId: 'accounts',
    labelKey: 'nav.accounts',
    descriptionKey: 'nav.accountsHint',
    path: '/finance/accounts',
    icon: 'categories',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'journal-entries',
    labelKey: 'nav.journalEntries',
    descriptionKey: 'nav.journalEntriesHint',
    path: '/finance/journal-entries',
    icon: 'reports',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'banks',
    labelKey: 'nav.banks',
    descriptionKey: 'nav.banksHint',
    path: '/finance/banks',
    icon: 'users',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'tax-currency',
    labelKey: 'nav.taxCurrency',
    descriptionKey: 'nav.taxCurrencyHint',
    path: '/finance/tax-currency',
    icon: 'settings',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'fiscal-periods',
    labelKey: 'nav.fiscalPeriods',
    descriptionKey: 'nav.fiscalPeriodsHint',
    path: '/fiscal-periods',
    icon: 'dashboard',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_REPORT_ROLES,
  },
  {
    menuId: 'budgets',
    labelKey: 'nav.budgets',
    descriptionKey: 'nav.budgetsHint',
    path: '/finance/budgets',
    icon: 'reports',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'fixed-assets',
    labelKey: 'nav.fixedAssets',
    descriptionKey: 'nav.fixedAssetsHint',
    path: '/finance/fixed-assets',
    icon: 'reports',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'payment-links',
    labelKey: 'nav.paymentLinks',
    descriptionKey: 'nav.paymentLinksHint',
    path: '/finance/payment-links',
    icon: 'reports',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },
  {
    menuId: 'reconciliation-center',
    labelKey: 'reconciliation.title',
    descriptionKey: 'reconciliation.subtitle',
    path: '/finance/reconciliation',
    icon: 'dashboard',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
    permissionMenuId: 'accounts',
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },
  {
    menuId: 'eta-tax',
    labelKey: 'nav.etaTax',
    descriptionKey: 'nav.etaTaxHint',
    path: '/compliance/eta-tax',
    icon: 'reports',
    workspace: 'workspace.financeAccounting',
    roles: FINANCE_ROLES,
  },

  // Medical & Clinics
  {
    menuId: 'clinic-patients',
    labelKey: 'nav.clinicPatients',
    descriptionKey: 'nav.clinicPatientsHint',
    path: '/clinic/patients',
    icon: 'users',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-queue',
    labelKey: 'nav.clinicQueue',
    descriptionKey: 'nav.clinicQueueHint',
    path: '/clinic/queue',
    icon: 'dashboard',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-commissions',
    labelKey: 'nav.clinicCommissions',
    descriptionKey: 'nav.clinicCommissionsHint',
    path: '/clinic/commissions',
    icon: 'reports',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-appointments',
    labelKey: 'nav.clinicAppointments',
    descriptionKey: 'nav.clinicAppointmentsHint',
    path: '/clinic/appointments',
    icon: 'dashboard',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-pharmacy',
    labelKey: 'nav.clinicPharmacy',
    descriptionKey: 'nav.clinicPharmacyHint',
    path: '/clinic/pharmacy',
    icon: 'categories',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-lab',
    labelKey: 'nav.clinicLab',
    descriptionKey: 'nav.clinicLabHint',
    path: '/clinic/lab',
    icon: 'categories',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'clinic-insurance',
    labelKey: 'nav.clinicInsurance',
    descriptionKey: 'nav.clinicInsuranceHint',
    path: '/clinic/insurance',
    icon: 'categories',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'hospital-ops',
    labelKey: 'nav.hospitalOps',
    descriptionKey: 'nav.hospitalOpsHint',
    path: '/clinic/hospital',
    icon: 'categories',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'dental-charting',
    labelKey: 'nav.dentalCharting',
    descriptionKey: 'nav.dentalChartingHint',
    path: '/clinic/dental',
    icon: 'categories',
    workspace: 'workspace.medical',
  },
  {
    menuId: 'medical-tools',
    labelKey: 'nav.medicalTools',
    descriptionKey: 'nav.medicalToolsHint',
    path: '/clinic/tools',
    icon: 'categories',
    workspace: 'workspace.medical',
  },

  // Administration
  {
    menuId: 'audit-logs',
    labelKey: 'nav.auditLogs',
    descriptionKey: 'nav.auditLogsHint',
    path: '/audit-logs',
    icon: 'reports',
    workspace: 'workspace.administration',
    roles: ['ADMIN'],
    allowAsLandingPage: false,
  },
  {
    menuId: 'users',
    labelKey: 'nav.users',
    descriptionKey: 'nav.usersHint',
    path: '/users',
    icon: 'users',
    workspace: 'workspace.administration',
    roles: ['ADMIN'],
    allowAsLandingPage: false,
  },
  {
    menuId: 'notifications-send',
    labelKey: 'nav.notificationsSend',
    descriptionKey: 'nav.notificationsSendHint',
    path: '/notifications/send',
    icon: 'reports',
    workspace: 'workspace.administration',
    roles: ['ADMIN'],
    allowAsLandingPage: false,
  },
  {
    menuId: 'admin-setup-readiness',
    labelKey: 'onboarding.tab',
    descriptionKey: 'onboarding.description',
    path: '/admin/setup-readiness',
    icon: 'dashboard',
    workspace: 'workspace.administration',
    roles: ['SUPER_ADMIN', 'ADMIN'],
    permissionMenuId: 'settings',
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },
  {
    menuId: 'admin-product-insights',
    labelKey: 'analytics.title',
    descriptionKey: 'analytics.description',
    path: '/admin/product-insights',
    icon: 'reports',
    workspace: 'workspace.administration',
    roles: ['SUPER_ADMIN', 'ADMIN'],
    permissionMenuId: 'settings',
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },
  {
    menuId: 'settings',
    labelKey: 'nav.settings',
    descriptionKey: 'nav.settingsHint',
    path: '/settings',
    icon: 'settings',
    workspace: 'workspace.administration',
    allowAsLandingPage: false,
  },
  {
    menuId: 'migration',
    labelKey: 'migration.title',
    descriptionKey: 'migration.subtitle',
    path: '/migration',
    icon: 'imports',
    workspace: 'workspace.administration',
    roles: ['SUPER_ADMIN', 'ADMIN'],
    permissionMenuId: 'settings',
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },
  // Platform Administration is intentionally separated from tenant/user Settings.
  {
    menuId: 'platform-admin',
    labelKey: 'settings.groupPlatformAdministration',
    descriptionKey: 'nav.settingsHint',
    path: '/platform-admin',
    icon: 'settings',
    workspace: 'workspace.platformAdministration',
    roles: ['SUPER_ADMIN'],
    permissionMenuId: 'settings',
    strictRoles: true,
    allowAsLandingPage: false,
    showInPermissionEditor: false,
  },
];

/** Route-guard roles per shell menu id; empty array means no role guard. */
export const SHELL_MENU_ROLES: Record<string, RoleCode[]> = Object.fromEntries(
  NAV_ITEMS.map((item) => [item.menuId, item.roles ?? []]),
);

export const LANDING_PAGE_ITEMS: NavItem[] = NAV_ITEMS.filter(
  (item) => item.allowAsLandingPage !== false,
);

/**
 * Shared authorization predicate so shell visibility and Settings landing-page
 * choices cannot drift apart again.
 */
export function canAccessNavigationItem(
  item: NavItem,
  roles: readonly RoleCode[],
  hasMenuAccess: (menuId: string) => boolean,
): boolean {
  if (roles.includes('SUPER_ADMIN')) return true;
  const roleOk = roles.includes('ADMIN') && !item.strictRoles
    ? true
    : !item.roles || item.roles.some((role) => roles.includes(role));
  return roleOk && hasMenuAccess(item.permissionMenuId ?? item.menuId);
}
