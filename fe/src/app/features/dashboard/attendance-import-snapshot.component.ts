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
  template: `
    <section class="attendance-import-card" aria-live="polite">
      <div class="card-heading">
        <div>
          <span class="eyebrow">{{ ar() ? 'الحضور المستورد' : 'Imported attendance' }}</span>
          <h2>{{ ar() ? 'ملخص آخر شهر بالبصمة' : 'Latest biometric month' }}</h2>
        </div>
        <div class="heading-actions">
          <button class="icon-button" type="button" (click)="refresh()" [disabled]="loading()" [attr.aria-label]="ar() ? 'تحديث' : 'Refresh'">↻</button>
          <a class="button secondary" routerLink="/imports/attendance">{{ ar() ? 'التفاصيل' : 'Details' }}</a>
        </div>
      </div>

      @if (loading()) {
        <div class="skeleton-row"><span></span><span></span><span></span></div>
      } @else if (error()) {
        <div class="state error">{{ error() }}</div>
      } @else if (latest(); as item) {
        <div class="month-line"><strong>{{ monthLabel(item.month) }}</strong><small>{{ ar() ? 'من بيانات البصمة المستوردة' : 'from imported biometric punches' }}</small></div>
        <div class="metrics">
          <div><strong>{{ item.employeeCount }}</strong><span>{{ ar() ? 'مستخدم / موظف' : 'employees / users' }}</span></div>
          <div><strong>{{ item.mappedEmployeeCount }}</strong><span>{{ ar() ? 'مربوط بموظف' : 'mapped employees' }}</span></div>
          <div><strong>{{ item.punchCount }}</strong><span>{{ ar() ? 'بصمة' : 'punches' }}</span></div>
        </div>
        @if (item.unmatchedEmployeeCount > 0) {
          <div class="state warning">{{ ar() ? 'يوجد ' + item.unmatchedEmployeeCount + ' مستخدم غير مربوط بموظف.' : item.unmatchedEmployeeCount + ' biometric users are still unmapped.' }}</div>
        }
      } @else {
        <div class="state">{{ ar() ? 'لا توجد بيانات بصمة مستوردة بعد.' : 'No imported biometric attendance yet.' }}</div>
      }
    </section>
  `,
  styles: [`
    :host{display:block}.attendance-import-card{display:grid;gap:.9rem;padding:1rem;border:1px solid var(--line,#e5e7eb);border-radius:16px;background:var(--surface,#fff);box-shadow:0 6px 24px rgba(15,23,42,.04)}
    .card-heading,.heading-actions,.month-line{display:flex;align-items:center;justify-content:space-between;gap:.75rem}.card-heading h2{margin:.15rem 0 0;font-size:1.05rem}.eyebrow{font-size:.72rem;font-weight:750;letter-spacing:.04em;color:var(--muted,#667085)}.heading-actions{justify-content:flex-end}.icon-button{width:36px;height:36px;border-radius:10px;border:1px solid var(--line,#d1d5db);background:var(--surface,#fff);cursor:pointer;font-size:1.1rem}.icon-button:disabled{opacity:.55;cursor:default}.month-line{justify-content:flex-start;align-items:baseline;flex-wrap:wrap}.month-line small{color:var(--muted,#667085)}
    .metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:.65rem}.metrics>div{display:grid;gap:.15rem;padding:.75rem;border-radius:12px;background:var(--surface-muted,#f8fafc)}.metrics strong{font-size:1.25rem}.metrics span{font-size:.76rem;color:var(--muted,#667085)}.state{padding:.65rem .75rem;border-radius:10px;background:var(--surface-muted,#f8fafc);color:var(--muted,#667085)}.state.warning{background:#fff7ed;color:#9a3412}.state.error{background:#fef2f2;color:#b42318}.skeleton-row{display:grid;grid-template-columns:repeat(3,1fr);gap:.65rem}.skeleton-row span{height:72px;border-radius:12px;background:var(--surface-muted,#f3f4f6)}
    @media(max-width:720px){.card-heading{align-items:flex-start}.heading-actions{flex-wrap:wrap}.metrics{grid-template-columns:1fr}.skeleton-row{grid-template-columns:1fr}.skeleton-row span{height:54px}}
  `],
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
