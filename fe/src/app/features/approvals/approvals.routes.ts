import { Routes } from '@angular/router';

export const APPROVAL_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'my-tasks',
    pathMatch: 'full'
  },
  {
    path: 'my-tasks',
    loadComponent: () => import('./pages/pending-approvals/pending-approvals.component').then(m => m.PendingApprovalsComponent),
    title: 'الاعتمادات المعلقة'
  },
  {
    path: 'definitions',
    loadComponent: () => import('./pages/workflow-definitions/workflow-definitions.component').then(m => m.WorkflowDefinitionsComponent),
    title: 'مسارات الاعتماد'
  }
];
