import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { Worker, AttendanceCell } from '../../models/workforce.models';

@Component({
  selector: 'app-manual-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">مصفوفة الإدخال اليدوي</span>
          <h1>جدول تسجيل حضور العمالة اليومية (Excel Matrix)</h1>
        </div>
        <div class="header-actions">
          <button type="button" class="btn btn-secondary" (click)="applyFullDayAll()">تعيين يوم كامل للكل</button>
          <button type="button" class="btn btn-primary" (click)="saveAttendance()">حفظ تسجيلات الحضور</button>
        </div>
      </header>

      <div class="card matrix-card">
        <div class="matrix-toolbar">
          <div class="filter-group">
            <label>التاريخ الحالي:</label>
            <input type="date" [(ngModel)]="currentDate" class="form-input" (change)="onDateChange()" />
          </div>
        </div>

        <div class="table-scroll-wrapper">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="sticky-col">كود العامل</th>
                <th class="sticky-col name-col">اسم العامل</th>
                <th>المقاول</th>
                <th>اليومية (ج.م)</th>
                <th>حالة الحضور (1 / 0.5 / 0)</th>
                <th>ساعات إضافية</th>
                <th>إجمالي اليوم (ج.م)</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let w of workforceService.workers()">
                <td class="sticky-col"><strong>{{ w.code }}</strong></td>
                <td class="sticky-col name-col">{{ w.fullName }}</td>
                <td>{{ w.contractorName }}</td>
                <td>{{ w.defaultDailyRate | number:'1.2-2' }}</td>
                <td>
                  <select [(ngModel)]="attendanceState[w.id].attendanceValue" class="matrix-select">
                    <option [ngValue]="1">1 - يوم كامل</option>
                    <option [ngValue]="0.5">0.5 - نصف يوم</option>
                    <option [ngValue]="0">0 - غياب</option>
                  </select>
                </td>
                <td>
                  <input type="number" [(ngModel)]="attendanceState[w.id].overtimeHours" class="matrix-input-num" min="0" step="0.5" />
                </td>
                <td>
                  <strong>{{ (attendanceState[w.id].attendanceValue * w.defaultDailyRate) | number:'1.2-2' }} ج.م</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .header-actions { display: flex; gap: 0.75rem; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .matrix-toolbar { display: flex; gap: 1rem; margin-bottom: 1rem; }
    .filter-group { display: flex; align-items: center; gap: 0.5rem; }
    .form-input { padding: 0.5rem; border: 1px solid #cbd5e1; border-radius: 6px; }
    .table-scroll-wrapper { overflow-x: auto; max-height: 70vh; }
    .matrix-table { width: 100%; border-collapse: collapse; text-align: right; }
    .matrix-table th, .matrix-table td { padding: 0.75rem 1rem; border: 1px solid #e2e8f0; }
    .matrix-table th { background: #f8fafc; position: sticky; top: 0; z-index: 5; }
    .sticky-col { position: sticky; right: 0; background: #fff; z-index: 4; }
    .sticky-col.name-col { right: 100px; }
    .matrix-select { padding: 0.375rem; border-radius: 6px; border: 1px solid #cbd5e1; font-weight: 600; }
    .matrix-input-num { width: 70px; padding: 0.375rem; border-radius: 6px; border: 1px solid #cbd5e1; text-align: center; }
  `]
})
export class ManualAttendanceComponent implements OnInit {
  workforceService = inject(WorkforceService);
  currentDate = new Date().toISOString().split('T')[0];
  attendanceState: { [workerId: string]: { attendanceValue: number; overtimeHours: number } } = {};

  ngOnInit() {
    this.workforceService.loadWorkers().subscribe(workers => {
      workers.forEach(w => {
        this.attendanceState[w.id] = { attendanceValue: 1, overtimeHours: 0 };
      });
    });
  }

  onDateChange() {
    // re-initialize or load state for date
  }

  applyFullDayAll() {
    Object.keys(this.attendanceState).forEach(id => {
      this.attendanceState[id].attendanceValue = 1;
    });
  }

  saveAttendance() {
    const entries: AttendanceCell[] = Object.keys(this.attendanceState).map(workerId => ({
      workerId,
      workDate: this.currentDate,
      attendanceValue: this.attendanceState[workerId].attendanceValue,
      overtimeHours: this.attendanceState[workerId].overtimeHours
    }));
    this.workforceService.saveAttendanceBatch(entries).subscribe(() => {
      alert('تم حفظ تسجيلات الحضور بنجاح');
    });
  }
}
