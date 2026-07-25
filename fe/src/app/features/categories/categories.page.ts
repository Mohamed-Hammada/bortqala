import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { dateInputToEpoch, epochToDateInput } from '../../core/date';
import {
  AttendanceCategory,
  AttendanceMode,
  CategoryPayload,
  DayOfWeek,
  PayCycle,
  ScheduleRule,
} from './categories.models';
import { CategoriesStore } from './categories.store';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';

type ScheduleForm = FormGroup<{
  name: FormControl<string>;
  effectiveFrom: FormControl<string>;
  effectiveTo: FormControl<string>;
  startTime: FormControl<string>;
  expectedMinutesOverride: FormControl<number | null>;
  graceMinutes: FormControl<number>;
}>;
@Component({
  selector: 'app-categories-page',
  imports: [ReactiveFormsModule, TablePaginationComponent],
  providers: [CategoriesStore],
  templateUrl: './categories.page.html',
  styleUrl: './categories.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoriesPage {
  readonly store = inject(CategoriesStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly drawerOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly paged = computed(() => this.pagination.slice(this.store.items()));
  readonly days: Array<{ code: DayOfWeek; labelKey: string }> = [
    { code: 'SATURDAY', labelKey: 'day.saturday' },
    { code: 'SUNDAY', labelKey: 'day.sunday' },
    { code: 'MONDAY', labelKey: 'day.monday' },
    { code: 'TUESDAY', labelKey: 'day.tuesday' },
    { code: 'WEDNESDAY', labelKey: 'day.wednesday' },
    { code: 'THURSDAY', labelKey: 'day.thursday' },
    { code: 'FRIDAY', labelKey: 'day.friday' },
  ];
  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    expectedDailyMinutes: new FormControl(480, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(1440)],
    }),
    payCycle: new FormControl<PayCycle>('MONTHLY', { nonNullable: true }),
    attendanceMode: new FormControl<AttendanceMode>('BIOMETRIC', { nonNullable: true }),
    singlePunchCounts: new FormControl(false, { nonNullable: true }),
    allowsEmployeeAdvances: new FormControl(false, { nonNullable: true }),
    workDays: new FormControl<DayOfWeek[]>(
      ['SATURDAY', 'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY'],
      { nonNullable: true, validators: [Validators.required] },
    ),
    active: new FormControl(true, { nonNullable: true }),
    version: new FormControl<number | null>(null),
    schedules: new FormArray<ScheduleForm>([]),
  });
  constructor() {
    void this.store.load();
  }
  openNew() {
    this.editingId.set(null);
    this.form.reset({
      code: '',
      name: '',
      expectedDailyMinutes: 480,
      payCycle: 'MONTHLY',
      attendanceMode: 'BIOMETRIC',
      singlePunchCounts: false,
      allowsEmployeeAdvances: false,
      workDays: ['SATURDAY', 'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY'],
      active: true,
      version: null,
    });
    this.form.controls.schedules.clear();
    this.addSchedule();
    this.drawerOpen.set(true);
  }
  openEdit(item: AttendanceCategory) {
    this.editingId.set(item.id);
    this.form.reset({
      code: item.code,
      name: item.name,
      expectedDailyMinutes: item.expectedDailyMinutes,
      payCycle: item.payCycle,
      attendanceMode: item.attendanceMode,
      singlePunchCounts: item.singlePunchCounts,
      allowsEmployeeAdvances: item.allowsEmployeeAdvances,
      workDays: item.workDays,
      active: item.active,
      version: item.version,
    });
    this.form.controls.schedules.clear();
    item.schedules.forEach((schedule) => this.addSchedule(schedule));
    if (!item.schedules.length) this.addSchedule();
    this.drawerOpen.set(true);
  }
  addSchedule(value?: ScheduleRule) {
    this.form.controls.schedules.push(
      new FormGroup({
        name: new FormControl(value?.name ?? this.i18n.t('categories.defaultSchedule'), {
          nonNullable: true,
          validators: [Validators.required],
        }),
        effectiveFrom: new FormControl(
          value ? epochToDateInput(value.effectiveFrom) : `${new Date().getFullYear()}-01-01`,
          { nonNullable: true, validators: [Validators.required] },
        ),
        effectiveTo: new FormControl(
          value?.effectiveTo ? epochToDateInput(value.effectiveTo) : '',
          {
            nonNullable: true,
          },
        ),
        startTime: new FormControl(value?.startTime?.slice(0, 5) ?? '08:00', {
          nonNullable: true,
          validators: [Validators.required],
        }),
        expectedMinutesOverride: new FormControl(value?.expectedMinutesOverride ?? null),
        graceMinutes: new FormControl(value?.graceMinutes ?? 0, {
          nonNullable: true,
          validators: [Validators.min(0), Validators.max(240)],
        }),
      }),
    );
  }
  removeSchedule(index: number) {
    this.form.controls.schedules.removeAt(index);
  }
  toggleDay(day: DayOfWeek, checked: boolean) {
    const current = this.form.controls.workDays.value;
    this.form.controls.workDays.setValue(
      checked ? [...current, day] : current.filter((value) => value !== day),
    );
  }
  toggleDayFromEvent(day: DayOfWeek, event: Event) {
    this.toggleDay(day, (event.target as HTMLInputElement).checked);
  }
  hasDay(day: DayOfWeek) {
    return this.form.controls.workDays.value.includes(day);
  }
  async submit() {
    if (this.form.invalid || !this.form.controls.workDays.value.length) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: CategoryPayload = {
      ...raw,
      schedules: raw.schedules.map((item) => ({
        ...item,
        effectiveFrom: dateInputToEpoch(item.effectiveFrom),
        effectiveTo: item.effectiveTo ? dateInputToEpoch(item.effectiveTo) : null,
        startTime: item.startTime.length === 5 ? `${item.startTime}:00` : item.startTime,
      })),
    };
    if (await this.store.save(this.editingId(), payload)) {
      this.notification.success(this.i18n.t('common.save') + ' ✓');
      this.drawerOpen.set(false);
    }
  }
  async deactivate(item: AttendanceCategory) {
    if (confirm(this.i18n.t('categories.deactivateConfirm', { name: item.name }))) {
      await this.store.deactivate(item.id);
      this.notification.info(this.i18n.t('common.save') + ' ✓');
    }
  }
  closeDrawer(): void {
    this.drawerOpen.set(false);
  }
  @HostListener('document:keydown', ['$event']) onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.drawerOpen()) {
      this.closeDrawer();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
      if (this.drawerOpen()) {
        event.preventDefault();
        this.submit();
      }
    }
  }
  modeLabel(value: AttendanceMode) {
    return this.i18n.t(
      {
        BIOMETRIC: 'attendance.biometric',
        MANUAL: 'attendance.manual',
        HYBRID: 'attendance.hybrid',
      }[value],
    );
  }
  cycleLabel(value: PayCycle) {
    return this.i18n.t(
      {
        MONTHLY: 'payCycle.monthly',
        HALF_MONTHLY: 'payCycle.halfMonthly',
        THIRTY_DAYS: 'payCycle.thirtyDays',
      }[value],
    );
  }
}
