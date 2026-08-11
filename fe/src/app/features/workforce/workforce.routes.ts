import { Routes } from '@angular/router';
import { menuAccessGuard } from '../../core/auth/auth.guard';
import { unsavedChangesGuard } from '../../core/unsaved-changes.guard';
import { WORKFORCE_ACCOUNT_ROLES, WORKFORCE_BASE_ROLES, WORKFORCE_IMPORT_ROLES, workforceRoleGuard } from '../../core/auth/workforce-role.guard';

export const WORKFORCE_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-dashboard' },
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.WorkforceDashboardComponent)
  },
  {
    path: 'contractors',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-contractors' },
    loadComponent: () => import('./pages/contractors/contractors.component').then(m => m.ContractorsComponent)
  },
  {
    path: 'workers',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-workers' },
    loadComponent: () => import('./pages/workers/workers.component').then(m => m.WorkersComponent)
  },
  {
    path: 'categories',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-categories' },
    loadComponent: () => import('./pages/categories/categories.component').then(m => m.CategoriesComponent)
  },
  {
    path: 'labor-requests',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-requests' },
    loadComponent: () => import('./pages/labor-requests/labor-requests.component').then(m => m.LaborRequestsComponent)
  },
  {
    path: 'attendance',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    canDeactivate: [unsavedChangesGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-attendance' },
    loadComponent: () => import('./pages/manual-attendance/manual-attendance.component').then(m => m.ManualAttendanceComponent)
  },
  {
    path: 'settlement-periods',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-settlements' },
    loadComponent: () => import('./pages/settlement-periods/settlement-periods.component').then(m => m.SettlementPeriodsComponent)
  },
  {
    path: 'advances',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_BASE_ROLES, menuId: 'workforce-advances' },
    loadComponent: () => import('./pages/advances/advances.component').then(m => m.AdvancesComponent)
  },
  {
    path: 'contractor-accounts',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_ACCOUNT_ROLES, menuId: 'workforce-accounts' },
    loadComponent: () => import('./pages/contractor-accounts/contractor-accounts.component').then(m => m.ContractorAccountsComponent)
  },
  {
    path: 'reports-import',
    canActivate: [workforceRoleGuard, menuAccessGuard],
    data: { roles: WORKFORCE_IMPORT_ROLES, menuId: 'workforce-reports' },
    loadComponent: () => import('./pages/reports-import/reports-import.component').then(m => m.ReportsImportComponent)
  }
];
