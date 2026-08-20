import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { TransitionResponse } from '../../../core/api.models';
import {
  Contractor,
  WorkerCategory,
  Worker,
  LaborRequest,
  SettlementPeriod,
  SettlementCalculationSummary,
  WorkforceAdvance,
  AttendanceCell,
  ManualAttendanceEntry,
  BatchAttendanceResponse,
  AdvancePolicy,
  SettlementIssue,
  WorkforceImportBatch,
  WorkforceImportValidation,
  WorkforceImportCommit,
  AdvanceEmployeeOption,
  ContractorSettlementDetail,
  LinkInvoicePayload,
  SettlementPostingPayload,
  RecordSettlementPaymentPayload,
  LaborDispatch,
  WorkerAssignment,
  WorkforceDispute,
  CreateLaborDispatchPayload,
  CreateWorkerAssignmentPayload,
  CreateWorkforceDisputePayload,
} from '../models/workforce.models';

@Injectable({ providedIn: 'root' })
export class WorkforceService {
  private http = inject(HttpClient);

  contractors = signal<Contractor[]>([]);
  categories = signal<WorkerCategory[]>([]);
  workers = signal<Worker[]>([]);
  employees = signal<AdvanceEmployeeOption[]>([]);
  laborRequests = signal<LaborRequest[]>([]);
  settlementPeriods = signal<SettlementPeriod[]>([]);
  advances = signal<WorkforceAdvance[]>([]);
  advancePolicies = signal<AdvancePolicy[]>([]);
  dispatches = signal<LaborDispatch[]>([]);
  loading = signal<boolean>(false);

  // Contractors
  loadContractors(): Observable<Contractor[]> {
    this.loading.set(true);
    return this.http.get<Contractor[]>('/api/v1/workforce/contractors').pipe(
      tap(res => {
        this.contractors.set(res);
        this.loading.set(false);
      })
    );
  }

  createContractor(payload: Partial<Contractor>): Observable<Contractor> {
    return this.http.post<Contractor>('/api/v1/workforce/contractors', payload).pipe(
      tap(() => this.loadContractors().subscribe())
    );
  }

  updateContractor(id: string, payload: Partial<Contractor>): Observable<Contractor> {
    return this.http.put<Contractor>(`/api/v1/workforce/contractors/${id}`, payload).pipe(
      tap(() => this.loadContractors().subscribe())
    );
  }

  exportContractorsExcel(): Observable<Blob> {
    return this.http.get('/api/v1/workforce/contractors/export.xlsx', { responseType: 'blob' });
  }

  // Worker Categories
  loadCategories(): Observable<WorkerCategory[]> {
    return this.http.get<WorkerCategory[]>('/api/v1/workforce/categories').pipe(
      tap(res => this.categories.set(res))
    );
  }

  createCategory(payload: Partial<WorkerCategory>): Observable<WorkerCategory> {
    return this.http.post<WorkerCategory>('/api/v1/workforce/categories', payload).pipe(
      tap(() => this.loadCategories().subscribe())
    );
  }

  exportCategoriesExcel(): Observable<Blob> {
    return this.http.get('/api/v1/workforce/categories/export.xlsx', { responseType: 'blob' });
  }

  // Workers
  loadWorkers(): Observable<Worker[]> {
    return this.http.get<Worker[]>('/api/v1/workforce/workers').pipe(
      tap(res => this.workers.set(res))
    );
  }

  createWorker(payload: Partial<Worker>): Observable<Worker> {
    return this.http.post<Worker>('/api/v1/workforce/workers', payload).pipe(
      tap(() => this.loadWorkers().subscribe())
    );
  }

  updateWorker(id: string, payload: Partial<Worker>): Observable<Worker> {
    return this.http.put<Worker>(`/api/v1/workforce/workers/${id}`, payload).pipe(
      tap(() => this.loadWorkers().subscribe())
    );
  }

  loadEmployees(): Observable<AdvanceEmployeeOption[]> {
    return this.http.get<AdvanceEmployeeOption[]>('/api/v1/employees').pipe(
      tap(res => this.employees.set(res.filter(employee => employee.active)))
    );
  }

  exportWorkersExcel(): Observable<Blob> {
    return this.http.get('/api/v1/workforce/workers/export.xlsx', { responseType: 'blob' });
  }

  // Labor Requests
  loadLaborRequests(): Observable<LaborRequest[]> {
    return this.http.get<LaborRequest[]>('/api/v1/workforce/labor-requests').pipe(
      tap(res => this.laborRequests.set(res))
    );
  }

  createLaborRequest(payload: any): Observable<LaborRequest> {
    return this.http.post<LaborRequest>('/api/v1/workforce/labor-requests', payload).pipe(
      tap(() => this.loadLaborRequests().subscribe())
    );
  }

  // Attendance Matrix
  loadAttendance(startDate: string, endDate: string): Observable<ManualAttendanceEntry[]> {
    return this.http.get<ManualAttendanceEntry[]>('/api/v1/workforce/attendance', {
      params: { startDate, endDate },
    });
  }

  saveAttendanceBatch(entries: AttendanceCell[]): Observable<BatchAttendanceResponse> {
    return this.http.post<BatchAttendanceResponse>('/api/v1/workforce/attendance/batch', { entries });
  }

  // Settlement Periods
  loadSettlementPeriods(): Observable<SettlementPeriod[]> {
    return this.http.get<SettlementPeriod[]>('/api/v1/workforce/settlements/periods').pipe(
      tap(res => this.settlementPeriods.set(res))
    );
  }

  createSettlementPeriod(payload: any): Observable<SettlementPeriod> {
    return this.http.post<SettlementPeriod>('/api/v1/workforce/settlements/periods', payload).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  calculatePeriod(id: string): Observable<SettlementCalculationSummary> {
    return this.http.post<SettlementCalculationSummary>(`/api/v1/workforce/settlements/periods/${id}/calculate`, {});
  }

  loadSettlementIssues(id: string): Observable<SettlementIssue[]> {
    return this.http.get<SettlementIssue[]>(`/api/v1/workforce/settlements/periods/${id}/issues`);
  }

  reviewPeriod(id: string): Observable<TransitionResponse> {
    return this.http.post<TransitionResponse>(`/api/v1/workforce/settlements/periods/${id}/review`, {}).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  approvePeriod(id: string): Observable<TransitionResponse> {
    return this.http.post<TransitionResponse>(`/api/v1/workforce/settlements/periods/${id}/approve`, {}).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  exportSettlementPeriodExcel(id: string): Observable<Blob> {
    return this.http.get(`/api/v1/workforce/settlements/periods/${id}/export-excel`, {
      responseType: 'blob'
    });
  }

  loadContractorSettlementsForPeriod(periodId: string): Observable<ContractorSettlementDetail[]> {
    return this.http.get<ContractorSettlementDetail[]>(`/api/v1/workforce/settlements/periods/${periodId}/contractor-settlements`);
  }

  getContractorSettlementDetail(id: string): Observable<ContractorSettlementDetail> {
    return this.http.get<ContractorSettlementDetail>(`/api/v1/workforce/settlements/contractor-settlements/${id}`);
  }

  postSettlementToFinance(id: string, payload: SettlementPostingPayload): Observable<ContractorSettlementDetail> {
    return this.http.post<ContractorSettlementDetail>(`/api/v1/workforce/settlements/contractor-settlements/${id}/post`, payload);
  }

  linkSettlementInvoice(id: string, payload: LinkInvoicePayload): Observable<ContractorSettlementDetail> {
    return this.http.post<ContractorSettlementDetail>(`/api/v1/workforce/settlements/contractor-settlements/${id}/link-invoice`, payload);
  }

  recordSettlementPayment(id: string, payload: RecordSettlementPaymentPayload): Observable<ContractorSettlementDetail> {
    return this.http.post<ContractorSettlementDetail>(`/api/v1/workforce/settlements/contractor-settlements/${id}/mark-paid`, payload);
  }

  // Advances
  loadAdvances(): Observable<WorkforceAdvance[]> {
    return this.http.get<WorkforceAdvance[]>('/api/v1/workforce/advances').pipe(
      tap(res => this.advances.set(res))
    );
  }

  createAdvance(payload: any): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>('/api/v1/workforce/advances', payload).pipe(
      tap(() => this.loadAdvances().subscribe())
    );
  }

  lockPeriod(id: string): Observable<TransitionResponse> {
    return this.http.post<TransitionResponse>(`/api/v1/workforce/settlements/periods/${id}/lock`, {}).pipe(
      tap(() => this.loadSettlementPeriods().subscribe())
    );
  }

  loadAdvancePolicies(): Observable<AdvancePolicy[]> {
    return this.http.get<AdvancePolicy[]>('/api/v1/workforce/advances/policies').pipe(
      tap(res => this.advancePolicies.set(res))
    );
  }

  saveAdvancePolicy(payload: AdvancePolicy): Observable<AdvancePolicy> {
    return this.http.put<AdvancePolicy>('/api/v1/workforce/advances/policies', payload).pipe(
      tap(() => this.loadAdvancePolicies().subscribe())
    );
  }

  loadEffectiveAdvancePolicy(recipientType: 'WORKER' | 'EMPLOYEE', recipientId: string, date: string): Observable<AdvancePolicy> {
    return this.http.get<AdvancePolicy>('/api/v1/workforce/advances/policies/effective', {
      params: recipientType === 'EMPLOYEE'
        ? { recipientType, employeeId: recipientId, date }
        : { recipientType, workerId: recipientId, date },
    });
  }

  pauseAdvance(id: string): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/pause`, {});
  }

  resumeAdvance(id: string): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/resume`, {});
  }

  repayAdvance(id: string, payload: any): Observable<WorkforceAdvance> {
    return this.http.post<WorkforceAdvance>(`/api/v1/workforce/advances/${id}/repay`, payload);
  }

  // Workforce import workflow
  loadImportBatches(): Observable<WorkforceImportBatch[]> {
    return this.http.get<WorkforceImportBatch[]>('/api/v1/workforce/imports');
  }

  uploadImport(file: File): Observable<WorkforceImportBatch> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<WorkforceImportBatch>('/api/v1/workforce/imports/upload', formData);
  }

  saveImportMapping(id: string, columns: Record<string, string>): Observable<WorkforceImportBatch> {
    return this.http.post<WorkforceImportBatch>(`/api/v1/workforce/imports/${id}/mapping`, { columns });
  }

  validateImport(id: string): Observable<WorkforceImportValidation> {
    return this.http.post<WorkforceImportValidation>(`/api/v1/workforce/imports/${id}/validate`, {});
  }

  previewImport(id: string): Observable<WorkforceImportValidation> {
    return this.http.get<WorkforceImportValidation>(`/api/v1/workforce/imports/${id}/preview`);
  }

  commitImport(id: string, operationId: string, importValidRowsOnly: boolean): Observable<WorkforceImportCommit> {
    return this.http.post<WorkforceImportCommit>(`/api/v1/workforce/imports/${id}/commit`, { operationId, importValidRowsOnly });
  }

  reverseImport(id: string): Observable<WorkforceImportBatch> {
    return this.http.post<WorkforceImportBatch>(`/api/v1/workforce/imports/${id}/reverse`, {});
  }

  downloadImportErrors(id: string): Observable<Blob> {
    return this.http.get(`/api/v1/workforce/imports/${id}/errors.xlsx`, { responseType: 'blob' });
  }

  downloadImportOriginal(id: string): Observable<Blob> {
    return this.http.get(`/api/v1/workforce/imports/${id}/original`, { responseType: 'blob' });
  }

  // Legacy diagnostic compatibility
  analyzeImport(summaryDays?: number, settlementDays?: number): Observable<any> {
    return this.http.post('/api/v1/workforce/imports/analyze', null, {
      params: { summaryDays: summaryDays || 1550, settlementDays: settlementDays || 1635 }
    });
  }

  loadDispatches(): Observable<LaborDispatch[]> {
    return this.http.get<LaborDispatch[]>('/api/v1/workforce/dispatches').pipe(
      tap(items => this.dispatches.set(items)),
    );
  }

  createDispatch(payload: CreateLaborDispatchPayload): Observable<LaborDispatch> {
    return this.http.post<LaborDispatch>('/api/v1/workforce/dispatches', payload).pipe(
      tap(() => this.loadDispatches().subscribe()),
    );
  }

  transitionDispatch(id: string, action: 'dispatch' | 'accept' | 'cancel'): Observable<LaborDispatch> {
    return this.http.post<LaborDispatch>(`/api/v1/workforce/dispatches/${id}/${action}`, {}).pipe(
      tap(updated => this.dispatches.update(items => items.map(item => item.id === updated.id ? updated : item))),
    );
  }

  loadAssignments(dispatchId: string): Observable<WorkerAssignment[]> {
    return this.http.get<WorkerAssignment[]>(`/api/v1/workforce/dispatches/${dispatchId}/assignments`);
  }

  assignWorker(dispatchId: string, payload: CreateWorkerAssignmentPayload): Observable<WorkerAssignment> {
    return this.http.post<WorkerAssignment>(`/api/v1/workforce/dispatches/${dispatchId}/assignments`, payload);
  }

  transitionAssignment(id: string, action: 'accept' | 'reject', reason?: string): Observable<WorkerAssignment> {
    return this.http.post<WorkerAssignment>(`/api/v1/workforce/assignments/${id}/${action}`,
      action === 'reject' ? { reason } : {});
  }

  loadDisputes(periodId: string): Observable<WorkforceDispute[]> {
    return this.http.get<WorkforceDispute[]>(`/api/v1/workforce/settlements/${periodId}/disputes`);
  }

  createDispute(periodId: string, payload: CreateWorkforceDisputePayload): Observable<WorkforceDispute> {
    return this.http.post<WorkforceDispute>(`/api/v1/workforce/settlements/${periodId}/disputes`, payload);
  }

  transitionDispute(id: string, action: 'submit' | 'resolve' | 'reject', notes?: string): Observable<WorkforceDispute> {
    const payload = action === 'resolve' ? { resolutionNotes: notes } : action === 'reject' ? { reason: notes } : {};
    return this.http.post<WorkforceDispute>(`/api/v1/workforce/disputes/${id}/${action}`, payload);
  }

  getProjectLaborCostReport(projectId: string, periodId?: string): Observable<import('../models/workforce.models').ProjectLaborCostReport> {
    const params: Record<string, string> = {};
    if (periodId) params['periodId'] = periodId;
    return this.http.get<import('../models/workforce.models').ProjectLaborCostReport>(
      `/api/v1/workforce/settlements/projects/${projectId}/labor-cost-report`,
      { params }
    );
  }
}
