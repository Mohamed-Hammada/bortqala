import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { formatDateTime, dateInputToEpoch } from '../../core/date';
import { I18nService } from '../../core/i18n.service';
import { apiErrorMessage } from '../../core/api-error';
import { NotificationService } from '../../core/notification.service';
import { AttendanceCategory } from '../categories/categories.models';
import { EmployeePayload } from '../employees/employees.models';
import { AttendanceApiService } from './attendance-api.service';
import { EmployeeAttendanceDetails } from './attendance.models';

@Component({
  selector: 'app-attendance-employee-page',
  imports: [RouterLink, FormsModule],
  templateUrl: './attendance-employee.page.html',
  styleUrl: './attendance-employee.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceEmployeePage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  readonly api = inject(AttendanceApiService);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly deviceUserId = this.route.snapshot.paramMap.get('deviceUserId') ?? '';
  readonly month = signal(this.route.snapshot.queryParamMap.get('month'));
  readonly details = signal<EmployeeAttendanceDetails | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly showAdd = signal(this.route.snapshot.queryParamMap.get('add') === '1');
  readonly categories = signal<AttendanceCategory[]>([]);
  readonly saving = signal(false);

  employeeCode = this.deviceUserId;
  fullName = '';
  categoryId = '';
  employmentType: 'FIXED' | 'DAILY' = 'FIXED';
  baseSalary = 0;
  activeFrom = new Date().toISOString().slice(0, 10);

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
      this.fullName ||= details.observedName || details.deviceUserId;
      this.employeeCode ||= details.deviceUserId;
      if (!details.mapped) await this.loadCategories();
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async loadCategories(): Promise<void> {
    if (this.categories().length > 0) return;
    try {
      const categories = await firstValueFrom(this.http.get<AttendanceCategory[]>('/api/v1/categories'));
      const allowed = categories.filter((item) => item.active && item.scope !== 'WORKER');
      this.categories.set(allowed);
      this.categoryId ||= allowed[0]?.id ?? '';
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    }
  }

  async openAddEmployee(): Promise<void> {
    await this.loadCategories();
    this.showAdd.set(true);
  }

  async addEmployee(): Promise<void> {
    if (!this.employeeCode.trim() || !this.fullName.trim() || !this.categoryId) {
      this.notification.warning(this.i18n.locale() === 'ar-EG' ? 'أكمل الحقول المطلوبة.' : 'Complete the required fields.');
      return;
    }
    this.saving.set(true);
    try {
      const payload: EmployeePayload = {
        employeeCode: this.employeeCode.trim(),
        fullName: this.fullName.trim(),
        deviceUserId: this.deviceUserId,
        categoryId: this.categoryId,
        employmentType: this.employmentType,
        baseSalary: Number(this.baseSalary) || 0,
        activeFrom: dateInputToEpoch(this.activeFrom),
        activeTo: null,
        active: true,
        version: null,
      };
      await firstValueFrom(this.http.post('/api/v1/employees', payload));
      this.notification.success(this.i18n.locale() === 'ar-EG' ? 'تمت إضافة الموظف وربطه برقم جهاز البصمة.' : 'Employee added and mapped to the biometric device ID.');
      this.showAdd.set(false);
      await this.load();
    } catch (error) {
      this.error.set(apiErrorMessage(error, this.i18n));
    } finally {
      this.saving.set(false);
    }
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
