import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  EssProfileDto,
  EssPayslipSummaryDto,
  EssPayslipDetailDto,
  EssLeaveDto,
  EssAdvanceDto,
  EssAttendanceRecordDto,
} from './ess.models';

@Injectable({
  providedIn: 'root',
})
export class EssService {
  private readonly http = inject(HttpClient);

  readonly profile = signal<EssProfileDto | null>(null);
  readonly payslips = signal<EssPayslipSummaryDto[]>([]);
  readonly selectedPayslip = signal<EssPayslipDetailDto | null>(null);
  readonly leaves = signal<EssLeaveDto[]>([]);
  readonly advances = signal<EssAdvanceDto[]>([]);
  readonly attendance = signal<EssAttendanceRecordDto[]>([]);
  readonly loading = signal(false);

  async loadProfile(): Promise<EssProfileDto> {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(this.http.get<EssProfileDto>('/api/v1/ess/profile'));
      this.profile.set(data);
      return data;
    } finally {
      this.loading.set(false);
    }
  }

  async loadPayslips(year?: number): Promise<EssPayslipSummaryDto[]> {
    const url = year ? `/api/v1/ess/payslips?year=${year}` : '/api/v1/ess/payslips';
    const list = await firstValueFrom(this.http.get<EssPayslipSummaryDto[]>(url));
    this.payslips.set(list);
    return list;
  }

  async loadPayslipDetail(paymentId: string): Promise<EssPayslipDetailDto> {
    const detail = await firstValueFrom(this.http.get<EssPayslipDetailDto>(`/api/v1/ess/payslips/${paymentId}`));
    this.selectedPayslip.set(detail);
    return detail;
  }

  async submitLeave(payload: { leaveTypeId: string; startDate: string; endDate: string; reason: string }): Promise<EssLeaveDto> {
    const created = await firstValueFrom(this.http.post<EssLeaveDto>('/api/v1/ess/leaves', payload));
    this.leaves.update((list) => [created, ...list]);
    return created;
  }

  async loadLeaves(): Promise<EssLeaveDto[]> {
    const list = await firstValueFrom(this.http.get<EssLeaveDto[]>('/api/v1/ess/leaves'));
    this.leaves.set(list);
    return list;
  }

  async submitAdvance(payload: { amount: number; totalInstallments: number; firstInstallmentDate?: string; reason?: string }): Promise<EssAdvanceDto> {
    const created = await firstValueFrom(this.http.post<EssAdvanceDto>('/api/v1/ess/advances', payload));
    this.advances.update((list) => [created, ...list]);
    return created;
  }

  async loadAdvances(): Promise<EssAdvanceDto[]> {
    const list = await firstValueFrom(this.http.get<EssAdvanceDto[]>('/api/v1/ess/advances'));
    this.advances.set(list);
    return list;
  }

  async loadAttendance(): Promise<EssAttendanceRecordDto[]> {
    const list = await firstValueFrom(this.http.get<EssAttendanceRecordDto[]>('/api/v1/ess/attendance'));
    this.attendance.set(list);
    return list;
  }
}
