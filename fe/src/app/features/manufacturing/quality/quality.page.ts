import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { formatDate } from '../../../core/date';

export interface QualityInspection {
  id: string;
  inspectionNumber: string;
  inspectionDate: number;
  sourceType: string;
  passedQuantity: number;
  failedQuantity: number;
  status: 'PASSED' | 'FAILED' | 'REJECTED';
  inspectorName: string;
  notes?: string;
  createdAt: number;
}

@Component({
  selector: 'app-quality-page',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './quality.page.html',
  styleUrl: './quality.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QualityPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly inspections = signal<QualityInspection[]>([]);

  readonly drawerOpen = signal(false);

  readonly qiForm = new FormGroup({
    inspectionNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    inspectionDate: new FormControl(new Date().toISOString().substring(0, 10), { nonNullable: true, validators: [Validators.required] }),
    sourceType: new FormControl('INCOMING_GRN', { nonNullable: true, validators: [Validators.required] }),
    passedQuantity: new FormControl(100, { nonNullable: true, validators: [Validators.required] }),
    failedQuantity: new FormControl(0, { nonNullable: true, validators: [Validators.required] }),
    status: new FormControl('PASSED', { nonNullable: true, validators: [Validators.required] }),
    inspectorName: new FormControl('مهندس فحص الجودة', { nonNullable: true, validators: [Validators.required] }),
    notes: new FormControl('مطابق للمواصفات القياسية', { nonNullable: true }),
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<QualityInspection[]>('/api/v1/manufacturing/quality'),
      );
      this.inspections.set(data);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  openNew() {
    this.qiForm.reset({
      inspectionNumber: 'QC-' + Math.floor(1000 + Math.random() * 9000),
      inspectionDate: new Date().toISOString().substring(0, 10),
      sourceType: 'INCOMING_GRN',
      passedQuantity: 98,
      failedQuantity: 2,
      status: 'PASSED',
      inspectorName: 'مهندس فحص الجودة',
      notes: 'تم قبول الشحنة مع عزل 2 قطعة تالفة',
    });
    this.drawerOpen.set(true);
  }

  closeDrawer() {
    this.drawerOpen.set(false);
  }

  async submitQi() {
    if (this.qiForm.invalid) return;
    try {
      const val = this.qiForm.getRawValue();
      const dateMs = new Date(val.inspectionDate).getTime();
      const payload = {
        ...val,
        inspectionDate: dateMs,
      };
      await firstValueFrom(this.http.post('/api/v1/manufacturing/quality', payload));
      this.notification.success('تم تسجيل محضر فحص الجودة بنجاح ✓');
      this.drawerOpen.set(false);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
