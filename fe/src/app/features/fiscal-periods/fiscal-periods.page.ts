import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { formatDate } from '../../core/date';
import { FiscalPeriod } from './fiscal-periods.models';

@Component({
  selector: 'app-fiscal-periods-page',
  imports: [],
  templateUrl: './fiscal-periods.page.html',
  styleUrl: './fiscal-periods.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FiscalPeriodsPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly periods = signal<FiscalPeriod[]>([]);
  readonly year = signal<number>(new Date().getFullYear());

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const data = await firstValueFrom(
        this.http.get<FiscalPeriod[]>('/api/v1/fiscal-periods', { params: { year: this.year() } }),
      );
      this.periods.set(data);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async generateYear() {
    this.loading.set(true);
    try {
      const data = await firstValueFrom(
        this.http.post<FiscalPeriod[]>(`/api/v1/fiscal-periods/generate-year?year=${this.year()}`, {}),
      );
      this.periods.set(data);
      this.notification.success('تم إنشاء الفترات المالية للسنة بنجاح ✓');
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  async toggleStatus(p: FiscalPeriod, targetStatus: 'OPEN' | 'CLOSED' | 'LOCKED') {
    try {
      await firstValueFrom(
        this.http.put(`/api/v1/fiscal-periods/${p.id}/status`, { status: targetStatus }),
      );
      this.notification.success(`تم تغيير حالة الفترة إلى (${targetStatus}) ✓`);
      await this.load();
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    }
  }

  date(ms: number) {
    return formatDate(ms);
  }
}
