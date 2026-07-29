import { Routes } from '@angular/router';
import { unsavedChangesGuard } from '../../core/unsaved-changes.guard';

export const WORKFORCE_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.WorkforceDashboardComponent)
  },
  {
    path: 'contractors',
    loadComponent: () => import('./pages/contractors/contractors.component').then(m => m.ContractorsComponent)
  },
  {
    path: 'workers',
    loadComponent: () => import('./pages/workers/workers.component').then(m => m.WorkersComponent)
  },
  {
    path: 'categories',
    loadComponent: () => import('./pages/categories/categories.component').then(m => m.CategoriesComponent)
  },
  {
    path: 'labor-requests',
    loadComponent: () => import('./pages/labor-requests/labor-requests.component').then(m => m.LaborRequestsComponent)
  },
  {
    path: 'attendance',
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/manual-attendance/manual-attendance.component').then(m => m.ManualAttendanceComponent)
  },
  {
    path: 'settlement-periods',
    loadComponent: () => import('./pages/settlement-periods/settlement-periods.component').then(m => m.SettlementPeriodsComponent)
  },
  {
    path: 'advances',
    loadComponent: () => import('./pages/advances/advances.component').then(m => m.AdvancesComponent)
  },
  {
    path: 'contractor-accounts',
    loadComponent: () => import('./pages/contractor-accounts/contractor-accounts.component').then(m => m.ContractorAccountsComponent)
  },
  {
    path: 'reports-import',
    loadComponent: () => import('./pages/reports-import/reports-import.component').then(m => m.ReportsImportComponent)
  }
];
