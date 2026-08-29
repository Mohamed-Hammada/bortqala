import { Routes } from '@angular/router';

export const CLINIC_ROUTES: Routes = [
  {
    path: 'patients',
    loadComponent: () =>
      import('./patients.page').then((m) => m.PatientsPageComponent),
  },
  {
    path: 'patients/:id/chart',
    loadComponent: () =>
      import('./patient-chart.page').then((m) => m.PatientChartPageComponent),
  },
  {
    path: 'queue',
    loadComponent: () =>
      import('./clinic-queue.page').then((m) => m.ClinicQueuePageComponent),
  },
  {
    path: 'commissions',
    loadComponent: () =>
      import('./clinic-commissions.page').then((m) => m.ClinicCommissionsPageComponent),
  },
  {
    path: 'appointments',
    loadComponent: () =>
      import('./appointments.page').then((m) => m.AppointmentsPageComponent),
  },
  {
    path: 'pharmacy',
    loadComponent: () =>
      import('./pharmacy.page').then((m) => m.PharmacyPageComponent),
  },
  {
    path: 'lab',
    loadComponent: () =>
      import('./lab-orders.page').then((m) => m.LabOrdersPageComponent),
  },
  {
    path: 'insurance',
    loadComponent: () =>
      import('./insurance.page').then((m) => m.InsurancePageComponent),
  },
  {
    path: 'hospital',
    loadComponent: () =>
      import('./hospital-ops.page').then((m) => m.HospitalOpsPageComponent),
  },
  {
    path: 'dental',
    loadComponent: () =>
      import('./dental-charting.page').then((m) => m.DentalChartingPageComponent),
  },
];
