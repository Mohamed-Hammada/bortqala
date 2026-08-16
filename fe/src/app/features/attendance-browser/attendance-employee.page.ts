import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { formatDateTime } from '../../core/date';
import { I18nService } from '../../core/i18n.service';
import { apiErrorMessage } from '../../core/api-error';
import { NotificationService } from '../../core/notification.service';
import { AttendanceApiService } from './attendance-api.service';
import { EmployeeAttendanceDetails } from './attendance.models';

@Component({
  selector: 'app-attendance-employee-page',
  imports: [RouterLink],
  templateUrl: './attendance-employee.page.html',
  styleUrl: './attendance-employee.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceEmployeePage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly api = inject(AttendanceApiService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly deviceUserId = this.route.snapshot.paramMap.get('deviceUserId') ?? '';
  readonly month = signal(this.route.snapshot.queryParamMap.get('month'));
  readonly details = signal<EmployeeAttendanceDetails | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private readonly addRequested = this.route.snapshot.queryParamMap.get('add') === '1';

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const details = await this.api.employee(this.deviceUserId, this.month());
      this.details.set(details);
      this.month.set(details.month);
      if (this.addRequested && !details.mapped) {
        await this.openAddEmployee();
        return;
      }
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async openAddEmployee(): Promise<void> {
    const details = this.details();
    const month = this.month();
    const returnUrl = `/imports/attendance/${encodeURIComponent(this.deviceUserId)}${month ? `?month=${encodeURIComponent(month)}` : ''}`;
    await this.router.navigate(['/employees'], {
      queryParams: {
        fromBiometric: '1',
        deviceUserId: this.deviceUserId,
        employeeCode: this.deviceUserId,
        fullName: details?.observedName || details?.deviceUserId || this.deviceUserId,
        month: month ?? '',
        returnUrl,
      },
    });
  }

  dateTime(value: number | null): string {
    return value == null ? '—' : formatDateTime(value);
  }

  hours(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return `${hours}h ${rest.toString().padStart(2, '0')}m`;
  }

  displayName(): string {
    const details = this.details();
    return details?.employeeName || details?.observedName || details?.deviceUserId || this.deviceUserId;
  }
}

// BORTQALA_REMAINING_20260816_V3_BIOMETRIC_EMPLOYEE_PARITY
