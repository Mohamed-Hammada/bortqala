import { Routes } from '@angular/router';
import { unsavedChangesGuard } from '../../core/unsaved-changes.guard';
import { WORKFORCE_ROLES, workforceRoleGuard } from '../../core/auth/workforce-role.guard';

export const WORKFORCE_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.WorkforceDashboardComponent)
  },
  {
    path: 'contractors',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/contractors/contractors.component').then(m => m.ContractorsComponent)
  },
  {
    path: 'workers',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/workers/workers.component').then(m => m.WorkersComponent)
  },
  {
    path: 'categories',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/categories/categories.component').then(m => m.CategoriesComponent)
  },
  {
    path: 'labor-requests',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/labor-requests/labor-requests.component').then(m => m.LaborRequestsComponent)
  },
  {
    path: 'attendance',
    canActivate: [workforceRoleGuard],
    canDeactivate: [unsavedChangesGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/manual-attendance/manual-attendance.component').then(m => m.ManualAttendanceComponent)
  },
  {
    path: 'settlement-periods',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/settlement-periods/settlement-periods.component').then(m => m.SettlementPeriodsComponent)
  },
  {
    path: 'advances',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/advances/advances.component').then(m => m.AdvancesComponent)
  },
  {
    path: 'contractor-accounts',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/contractor-accounts/contractor-accounts.component').then(m => m.ContractorAccountsComponent)
  },
  {
    path: 'reports-import',
    canActivate: [workforceRoleGuard],
    data: { roles: WORKFORCE_ROLES },
    loadComponent: () => import('./pages/reports-import/reports-import.component').then(m => m.ReportsImportComponent)
  }
];
