import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  AdjustBalancePayload,
  CreateLeaveTypePayload,
  LeaveBalance,
  LeaveRequest,
  LeaveRequestStatus,
  LeaveType,
  RejectLeaveRequestPayload,
  SubmitLeaveRequestPayload,
} from './leaves.models';

@Injectable({ providedIn: 'root' })
export class LeavesService {
  private readonly http = inject(HttpClient);

  async listTypes(): Promise<LeaveType[]> {
    return firstValueFrom(this.http.get<LeaveType[]>('/api/v1/leaves/types'));
  }

  async createType(payload: CreateLeaveTypePayload): Promise<LeaveType> {
    return firstValueFrom(this.http.post<LeaveType>('/api/v1/leaves/types', payload));
  }

  async listBalances(employeeId?: string, year?: number): Promise<LeaveBalance[]> {
    let params = new HttpParams();
    if (employeeId) params = params.set('employeeId', employeeId);
    if (year) params = params.set('year', year.toString());
    return firstValueFrom(this.http.get<LeaveBalance[]>('/api/v1/leaves/balances', { params }));
  }

  async adjustBalance(payload: AdjustBalancePayload): Promise<LeaveBalance> {
    return firstValueFrom(this.http.post<LeaveBalance>('/api/v1/leaves/balances/adjust', payload));
  }

  async listRequests(employeeId?: string, status?: LeaveRequestStatus): Promise<LeaveRequest[]> {
    let params = new HttpParams();
    if (employeeId) params = params.set('employeeId', employeeId);
    if (status) params = params.set('status', status);
    return firstValueFrom(this.http.get<LeaveRequest[]>('/api/v1/leaves/requests', { params }));
  }

  async submitRequest(payload: SubmitLeaveRequestPayload): Promise<LeaveRequest> {
    return firstValueFrom(this.http.post<LeaveRequest>('/api/v1/leaves/requests', payload));
  }

  async approveRequest(id: string): Promise<LeaveRequest> {
    return firstValueFrom(this.http.post<LeaveRequest>(`/api/v1/leaves/requests/${id}/approve`, {}));
  }

  async rejectRequest(id: string, payload: RejectLeaveRequestPayload): Promise<LeaveRequest> {
    return firstValueFrom(this.http.post<LeaveRequest>(`/api/v1/leaves/requests/${id}/reject`, payload));
  }

  async cancelRequest(id: string): Promise<LeaveRequest> {
    return firstValueFrom(this.http.post<LeaveRequest>(`/api/v1/leaves/requests/${id}/cancel`, {}));
  }

  async listEmployees(): Promise<Array<{ id: string; fullName: string; employeeCode: string }>> {
    return firstValueFrom(
      this.http.get<Array<{ id: string; fullName: string; employeeCode: string }>>('/api/v1/employees')
    );
  }
}
