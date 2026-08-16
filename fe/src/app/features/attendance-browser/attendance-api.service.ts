import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AttendanceEmployeeSummary,
  AttendanceMonthSummary,
  EmployeeAttendanceDetails,
} from './attendance.models';

@Injectable({ providedIn: 'root' })
export class AttendanceApiService {
  private readonly http = inject(HttpClient);

  months(): Promise<AttendanceMonthSummary[]> {
    return firstValueFrom(this.http.get<AttendanceMonthSummary[]>('/api/v1/imports/attendance/months'));
  }

  employees(month: string): Promise<AttendanceEmployeeSummary[]> {
    return firstValueFrom(
      this.http.get<AttendanceEmployeeSummary[]>(
        `/api/v1/imports/attendance/months/${encodeURIComponent(month)}/employees`,
      ),
    );
  }

  employee(deviceUserId: string, month?: string | null): Promise<EmployeeAttendanceDetails> {
    const params = month ? { month } : undefined;
    return firstValueFrom(
      this.http.get<EmployeeAttendanceDetails>(
        `/api/v1/imports/attendance/employees/${encodeURIComponent(deviceUserId)}`,
        { params },
      ),
    );
  }
}
