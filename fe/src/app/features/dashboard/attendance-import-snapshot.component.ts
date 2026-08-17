import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { AttendanceMonthSummary } from '../attendance-browser/attendance.models';

@Component({
  selector: 'app-attendance-import-snapshot',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './attendance-import-snapshot.component.html',
  styleUrls: ['./attendance-import-snapshot.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceImportSnapshotComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly latest = signal<AttendanceMonthSummary | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  private readonly onAttendanceChanged = () => { void this.load(); };

  constructor() {
    window.addEventListener('bortqala:attendance-updated', this.onAttendanceChanged);
    void this.load();
  }

  ngOnDestroy(): void { window.removeEventListener('bortqala:attendance-updated', this.onAttendanceChanged); }
  ar(): boolean { return this.i18n.locale().toLowerCase().startsWith('ar'); }
  refresh(): void { void this.load(); }

  monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    if (!year || !monthNumber) return month;
    return new Intl.DateTimeFormat(this.i18n.locale(), { month: 'long', year: 'numeric' })
      .format(new Date(year, monthNumber - 1, 1));
  }

  private async load(): Promise<void> {
    this.loading.set(true); this.error.set('');
    try {
      const rows = await firstValueFrom(this.http.get<AttendanceMonthSummary[]>(
        `/api/v1/imports/attendance/months?_refresh=${Date.now()}`,
      ));
      const sorted = [...rows].sort((a, b) => b.month.localeCompare(a.month));
      this.latest.set(sorted[0] ?? null);
    } catch {
      this.error.set(this.ar() ? 'تعذر تحديث ملخص الحضور المستورد.' : 'Could not refresh imported attendance summary.');
    } finally { this.loading.set(false); }
  }
}
