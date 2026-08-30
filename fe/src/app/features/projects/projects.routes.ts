import { Routes } from '@angular/router';

export const PROJECT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/projects.page').then((m) => m.ProjectsPage),
  },
  {
    path: 'executive-dashboard',
    loadComponent: () => import('../dashboard/project-executive-dashboard.component').then((m) => m.ProjectExecutiveDashboardComponent),
  },
  {
    path: ':id',
    loadComponent: () => import('./pages/project-detail.page').then((m) => m.ProjectDetailPage),
  },
];
