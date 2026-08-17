import { Component, HostListener, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { WorkforceService } from '../../data-access/workforce.service';
import { NotificationService } from '../../../../core/notification.service';
import { exportCsv } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';
import { Worker, AttendanceCell, ManualAttendanceEntry, BatchAttendanceResponse } from '../../models/workforce.models';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { I18nService } from '../../../../core/i18n.service';

interface DayCell {
  attendanceValue: number; // 1, 0.5, 0
  overtimeHours: number;
  deductionHours: number;
  notes: string;
}

type AttendanceMatrix = { [workerId: string]: { [date: string]: DayCell } };

interface CalculationRules {
  overtimeRate: number;
  overtimeThresholdHours: number;
  deductionRatePerHour: number;
  holidayPayRate: number;
  standardDailyHours: string;
  description: string;
}

type StatusOption<T = string> = { value: T; label: string };

export function shouldRenderAttendanceMatrix(loading: boolean, loadError: string | null, workerCount: number): boolean {
  return !loading && !loadError && workerCount > 0;
}

@Component({
  selector: 'app-manual-attendance',
  standalone: true,
  imports: [CommonModule, FormsModule, AppTooltipDirective, ModalDialogComponent],
  templateUrl: './manual-attendance.component.html',
  styleUrls: ['./manual-attendance.component.scss']
})
export class ManualAttendanceComponent implements OnInit {
  private workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);
  private http = inject(HttpClient);
  readonly i18n = inject(I18nService);

  workers = this.workforceService.workers;
  contractors = this.workforceService.contractors;
  categories = this.workforceService.categories;
  loading = signal(false);
  saving = signal(false);
  loadError = signal<string | null>(null);
  dates = signal<string[]>([]);
  dirtyCellKeys = signal<Set<string>>(new Set());
  cellErrors = signal<Map<string, string>>(new Map());
  saveSummary = signal<BatchAttendanceResponse | null>(null);
  private lastLoadedPeriod: { startDate: string; endDate: string } | null = null;

  selectedContractorId = signal<string>('');
  selectedCategoryId = signal<string>('');
  searchText = signal<string>('');
  selectedAttendanceStatus = signal<string>('all');
  showInactive = signal<boolean>(false);
  selectedWorkerIds = signal<Set<string>>(new Set());
  selectedDateIds = signal<Set<string>>(new Set());
  bulkStatusValue = signal<number>(1);
  bulkOvertimeHours = signal<number>(0);
  bulkPreview = signal<{ kind: 'attendance' | 'overtime'; workerCount: number; dayCount: number; cellCount: number } | null>(null);
  rules = signal<CalculationRules | null>(null);

  entryModalOpen = signal(false);
  importModalOpen = signal(false);
  entryWorkerId = signal('');
  entryDate = signal('');
  entryAttendanceValue = signal<number>(1);
  entryOvertimeHours = signal<number>(0);
  entryDeductionHours = signal<number>(0);
  entryNotes = signal('');
  importFile = signal<File | null>(null);

  openManualEntryModal(): void {
    this.entryWorkerId.set(this.filteredWorkers()[0]?.id ?? '');
    this.entryDate.set(this.startDate || new Date().toISOString().slice(0, 10));
    this.entryAttendanceValue.set(1);
    this.entryOvertimeHours.set(0);
    this.entryDeductionHours.set(0);
    this.entryNotes.set('');
    this.entryModalOpen.set(true);
  }

  closeManualEntryModal(): void {
    this.entryModalOpen.set(false);
  }

  submitManualEntry(): void {
    const wId = this.entryWorkerId();
    const d = this.entryDate();
    if (!wId || !d) return;

    if (!this.matrix[wId]) {
      this.matrix[wId] = {};
    }
    if (!this.matrix[wId][d]) {
      this.matrix[wId][d] = { attendanceValue: 1, overtimeHours: 0, deductionHours: 0, notes: '' };
    }

    this.matrix[wId][d].attendanceValue = this.entryAttendanceValue();
    this.matrix[wId][d].overtimeHours = this.entryOvertimeHours();
    this.matrix[wId][d].deductionHours = this.entryDeductionHours();
    this.matrix[wId][d].notes = this.entryNotes();

    this.markCellDirty(wId, d);
    this.closeManualEntryModal();
    this.notificationService.success(this.i18n.t('manualAttendance.entrySuccess'));
  }

  openImportModal(): void {
    this.importFile.set(null);
    this.importModalOpen.set(true);
  }

  closeImportModal(): void {
    this.importModalOpen.set(false);
    this.importFile.set(null);
  }

  downloadExcelTemplate(): void {
    const columns = [
      { key: 'workerCode', label: this.i18n.t('manualAttendance.ui.workerCode') },
      { key: 'date', label: this.i18n.t('manualAttendance.ui.date') },
      { key: 'attendanceValue', label: this.i18n.t('manualAttendance.ui.attendanceValue') },
      { key: 'overtimeHours', label: this.i18n.t('manualAttendance.ui.overtimeHours') },
      { key: 'deductionHours', label: this.i18n.t('manualAttendance.ui.deductionHours') },
      { key: 'notes', label: this.i18n.t('manualAttendance.ui.notes') }
    ];
    const sampleRows = [
      { workerCode: 'EMP-001', date: '2026-07-31', attendanceValue: '1', overtimeHours: '2', deductionHours: '0', notes: this.i18n.t('manualAttendance.ui.sampleFullShift') },
      { workerCode: 'EMP-002', date: '2026-07-31', attendanceValue: '0.5', overtimeHours: '0', deductionHours: '0', notes: this.i18n.t('manualAttendance.ui.halfDay') },
      { workerCode: 'EMP-003', date: '2026-07-31', attendanceValue: '0', overtimeHours: '0', deductionHours: '0', notes: this.i18n.t('manualAttendance.ui.absence') }
    ];
    exportCsv(sampleRows, columns, 'manual-attendance-import-template.csv');
    this.notificationService.success(this.i18n.t('manualAttendance.templateSuccess'));
  }

  onImportFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.importFile.set(file);
  }

  processImportFile(): void {
    const file = this.importFile();
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target?.result as string;
      if (!text) return;

      const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
      if (lines.length < 2) {
        this.notificationService.warning(this.i18n.t('manualAttendance.emptyFileError'));
        return;
      }

      let importedCount = 0;
      const workerMap = new Map(this.workers().map(w => [w.code.trim().toLowerCase(), w]));

      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(c => c.replace(/^"|"$/g, '').trim());
        if (cols.length < 2) continue;

        const code = cols[0]?.toLowerCase();
        const date = cols[1];
        if (!code || !date) continue;

        const worker = workerMap.get(code);
        if (!worker) continue;

        let attVal = 1;
        const rawVal = (cols[2] || '').toLowerCase();
        if (rawVal === '0.5' || rawVal === 'half' || rawVal.includes('نصف')) attVal = 0.5;
        else if (rawVal === '0' || rawVal === 'absent' || rawVal.includes('غا')) attVal = 0;
        else if (rawVal === '1' || rawVal === 'full' || rawVal.includes('حاضر')) attVal = 1;

        const overtime = parseFloat(cols[3]) || 0;
        const deduction = parseFloat(cols[4]) || 0;
        const notes = cols[5] || '';

        if (!this.matrix[worker.id]) this.matrix[worker.id] = {};
        if (!this.matrix[worker.id][date]) {
          this.matrix[worker.id][date] = { attendanceValue: attVal, overtimeHours: overtime, deductionHours: deduction, notes };
        } else {
          this.matrix[worker.id][date].attendanceValue = attVal;
          this.matrix[worker.id][date].overtimeHours = overtime;
          this.matrix[worker.id][date].deductionHours = deduction;
          this.matrix[worker.id][date].notes = notes;
        }

        this.markCellDirty(worker.id, date);
        importedCount++;
      }

      this.closeImportModal();
      if (importedCount > 0) {
        this.notificationService.success(this.i18n.t('manualAttendance.importSuccessCount', { count: importedCount }));
      } else {
        this.notificationService.warning(this.i18n.t('manualAttendance.noMatchingWorkers'));
      }
    };
    reader.readAsText(file, 'UTF-8');
  }

  get attendanceStatusOptions(): StatusOption[] {
    return [
      { value: 'all', label: this.i18n.t('workforce.ui.all') },
      { value: 'present', label: this.i18n.t('manualAttendance.ui.presentOne') },
      { value: 'half', label: this.i18n.t('manualAttendance.ui.halfDayHalf') },
      { value: 'absent', label: this.i18n.t('manualAttendance.ui.absentDash') },
    ];
  }

  get bulkStatusOptions(): StatusOption<number>[] {
    return [
      { value: 1, label: this.i18n.t('manualAttendance.ui.fullDayOne') },
      { value: 0.5, label: this.i18n.t('manualAttendance.ui.halfDayHalf') },
      { value: 0, label: this.i18n.t('manualAttendance.ui.absentDash') },
    ];
  }

  filteredWorkers = computed(() => {
    let list = this.workers();
    const contractorId = this.selectedContractorId();
    const categoryId = this.selectedCategoryId();
    const search = this.searchText().trim().toLowerCase();
    const statusFilter = this.selectedAttendanceStatus();
    const inactiveShown = this.showInactive();

    if (contractorId) {
      list = list.filter(w => w.contractorId === contractorId);
    }
    if (categoryId) {
      list = list.filter(w => w.categoryId === categoryId);
    }
    if (search) {
      list = list.filter(w =>
        w.fullName.toLowerCase().includes(search) ||
        w.code.toLowerCase().includes(search)
      );
    }
    if (!inactiveShown) {
      list = list.filter(w => w.status !== 'INACTIVE');
    }
    if (statusFilter !== 'all') {
      const targetValue = statusFilter === 'present' ? 1 : statusFilter === 'half' ? 0.5 : 0;
      list = list.filter(w => {
        const cells = this.matrix[w.id];
        if (!cells) return false;
        return Object.values(cells).some(c => c.attendanceValue === targetValue);
      });
    }
    return list;
  });

  matrix: AttendanceMatrix = {};

  startDate = '';
  endDate = '';

  allSelected = computed(() => {
    const fw = this.filteredWorkers();
    if (fw.length === 0) return false;
    return fw.every(w => this.selectedWorkerIds().has(w.id));
  });

  grandTotal = computed(() => {
    let total = 0;
    for (const w of this.filteredWorkers()) {
      total += this.getWorkerTotalAmount(w);
    }
    return total;
  });

  matrixVisible(): boolean {
    return shouldRenderAttendanceMatrix(this.loading(), this.loadError(), this.filteredWorkers().length);
  }

  ngOnInit() {
    this.setCurrentHalfMonth();
  }

  setCurrentHalfMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const mStr = String(month + 1).padStart(2, '0');
    if (today.getDate() <= 15) {
      this.startDate = `${year}-${mStr}-01`;
      this.endDate = `${year}-${mStr}-15`;
    } else {
      const lastDay = new Date(year, month + 1, 0).getDate();
      this.startDate = `${year}-${mStr}-16`;
      this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    }
    this.onPeriodChange();
  }

  setLastHalfMonth() {
    const today = new Date();
    let year = today.getFullYear();
    let month = today.getMonth(); // 0-indexed
    if (today.getDate() <= 15) {
      month -= 1;
      if (month < 0) { month = 11; year -= 1; }
      const lastDay = new Date(year, month + 1, 0).getDate();
      const mStr = String(month + 1).padStart(2, '0');
      this.startDate = `${year}-${mStr}-16`;
      this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    } else {
      const mStr = String(month + 1).padStart(2, '0');
      this.startDate = `${year}-${mStr}-01`;
      this.endDate = `${year}-${mStr}-15`;
    }
    this.onPeriodChange();
  }

  setCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    const mStr = String(month + 1).padStart(2, '0');
    this.startDate = `${year}-${mStr}-01`;
    this.endDate = `${year}-${mStr}-${String(lastDay).padStart(2, '0')}`;
    this.onPeriodChange();
  }

  onFilterChange() {
    this.selectedWorkerIds.set(new Set());
  }

  onPeriodChange() {
    if (!this.confirmDiscardChanges()) {
      if (this.lastLoadedPeriod) {
        this.startDate = this.lastLoadedPeriod.startDate;
        this.endDate = this.lastLoadedPeriod.endDate;
      }
      return;
    }
    this.clearEditState();
    const newDates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(newDates);
    this.selectedDateIds.set(new Set(newDates));
    this.loadData();
  }

  loadData() {
    if (!this.startDate || !this.endDate) return;
    this.loading.set(true);
    this.loadError.set(null);
    this.loadCalculationRules();

    forkJoin({
      contractors: this.workforceService.loadContractors(),
      categories: this.workforceService.loadCategories(),
      workers: this.workforceService.loadWorkers(),
      attendance: this.workforceService.loadAttendance(this.startDate, this.endDate),
    }).subscribe({
      next: ({ workers, attendance }) => {
        this.initMatrix(workers, attendance);
        this.selectedDateIds.set(new Set(this.dates()));
        this.lastLoadedPeriod = { startDate: this.startDate, endDate: this.endDate };
        this.clearEditState();
        this.loading.set(false);
      },
      error: (e) => {
        this.loadError.set(this.i18n.t('manualAttendance.ui.loadError', { detail: apiErrorDetail(e, e?.message ?? this.i18n.t('workforce.ui.unexpectedError')) }));
        this.loading.set(false);
      }
    });
  }

  refreshData(): void {
    if (!this.confirmDiscardChanges()) return;
    this.clearEditState();
    this.loadData();
  }

  private loadCalculationRules() {
    this.http.get<CalculationRules>('/api/v1/workforce/attendance/calculation-rules', {
      params: { date: this.startDate || '' }
    }).subscribe({
      next: (r) => this.rules.set(r),
      error: () => { /* non-critical */ }
    });
  }

  exportCsv(): void {
    const workers = this.filteredWorkers();
    const dateList = this.dates();
    const rows = workers.map((w) => {
      const row: Record<string, string | number | null | undefined> = {
        code: w.code,
        name: w.fullName,
        dailyRate: w.defaultDailyRate ?? 0,
        totalDays: this.getWorkerTotalDays(w.id),
        totalAmount: this.getWorkerTotalAmount(w),
      };
      for (const date of dateList) {
        row[date] = this.matrix[w.id]?.[date]?.attendanceValue ?? 0;
      }
      return row;
    });
    const columns = [
      { key: 'code', label: this.i18n.t('manualAttendance.ui.workerCode') },
      { key: 'name', label: this.i18n.t('manualAttendance.ui.workerName') },
      { key: 'dailyRate', label: this.i18n.t('manualAttendance.ui.dailyRate') },
      ...dateList.map((d) => ({ key: d, label: d })),
      { key: 'totalDays', label: this.i18n.t('manualAttendance.ui.totalDays') },
      { key: 'totalAmount', label: this.i18n.t('manualAttendance.ui.totalAmountEgp') },
    ];
    exportCsv(rows, columns, `manual-attendance-${this.startDate}-${this.endDate}.csv`);
  }

  private initMatrix(workers: Worker[], entries: ManualAttendanceEntry[]) {
    const dates = this.generateDateRange(this.startDate, this.endDate);
    this.dates.set(dates);
    this.matrix = {};
    for (const w of workers) {
      this.matrix[w.id] = {};
      for (const date of dates) {
        this.matrix[w.id][date] = { attendanceValue: 1, overtimeHours: 0, deductionHours: 0, notes: '' };
      }
    }
    for (const entry of entries) {
      const cell = this.matrix[entry.workerId]?.[entry.workDate];
      if (!cell) continue;
      cell.attendanceValue = Number(entry.attendanceValue);
      cell.overtimeHours = Number(entry.overtimeHours ?? 0);
      cell.deductionHours = Number(entry.deductionHours ?? 0);
      cell.notes = entry.notes ?? '';
    }
  }

  applyFullDayAll() {
    for (const w of this.filteredWorkers()) {
      for (const date of this.dates()) {
        if (this.matrix[w.id]?.[date] && this.matrix[w.id][date].attendanceValue !== 1) {
          this.matrix[w.id][date].attendanceValue = 1;
          this.markCellDirty(w.id, date);
        }
      }
    }
  }

  // --- Bulk selection ---
  toggleSelectAll() {
    const current = this.selectedWorkerIds();
    if (this.allSelected()) {
      this.selectedWorkerIds.set(new Set());
    } else {
      this.selectedWorkerIds.set(new Set(this.filteredWorkers().map(w => w.id)));
    }
  }

  isSelected(workerId: string): boolean {
    return this.selectedWorkerIds().has(workerId);
  }

  clearSelection() {
    this.selectedWorkerIds.set(new Set());
    this.bulkPreview.set(null);
  }

  toggleDate(date: string): void {
    const next = new Set(this.selectedDateIds());
    if (next.has(date)) next.delete(date); else next.add(date);
    this.selectedDateIds.set(next);
    this.bulkPreview.set(null);
  }

  toggleWorker(workerId: string) {
    const next = new Set(this.selectedWorkerIds());
    if (next.has(workerId)) {
      next.delete(workerId);
    } else {
      next.add(workerId);
    }
    this.selectedWorkerIds.set(next);
  }

  previewBulk(kind: 'attendance' | 'overtime'): void {
    const ids = [...this.selectedWorkerIds()];
    const selectedDates = [...this.selectedDateIds()];
    if (ids.length === 0 || selectedDates.length === 0) {
      this.notificationService.warning(this.i18n.t('manualAttendance.ui.selectWorkerWarning'));
      return;
    }
    if (kind === 'overtime' && this.bulkOvertimeHours() < 0) {
      this.notificationService.warning(this.i18n.t('manualAttendance.ui.negativeOvertimeWarning'));
      return;
    }
    this.bulkPreview.set({ kind, workerCount: ids.length, dayCount: selectedDates.length, cellCount: ids.length * selectedDates.length });
  }

  confirmBulk(): void {
    const preview = this.bulkPreview();
    if (!preview) return;
    const ids = [...this.selectedWorkerIds()];
    const selectedDates = [...this.selectedDateIds()];
    const value = this.bulkStatusValue();
    for (const wId of ids) {
      for (const date of selectedDates) {
        if (this.matrix[wId]?.[date]) {
          if (preview.kind === 'attendance') this.matrix[wId][date].attendanceValue = value;
          else this.matrix[wId][date].overtimeHours = this.bulkOvertimeHours();
          this.markCellDirty(wId, date);
        }
      }
    }
    this.notificationService.success(this.i18n.t('manualAttendance.ui.bulkApplied', { count: preview.cellCount }));
    this.selectedWorkerIds.set(new Set());
    this.bulkPreview.set(null);
  }

  saveAttendance() {
    const entries: AttendanceCell[] = [];
    for (const cellKey of this.dirtyCellKeys()) {
      const separator = cellKey.lastIndexOf('|');
      const workerId = cellKey.slice(0, separator);
      const workDate = cellKey.slice(separator + 1);
      const cell = this.matrix[workerId]?.[workDate];
      if (!cell) continue;
      entries.push({
        workerId,
        workDate,
        attendanceValue: cell.attendanceValue,
        overtimeHours: cell.overtimeHours || undefined,
        deductionHours: cell.deductionHours || undefined,
        notes: cell.notes || undefined
      });
    }

    if (entries.length === 0) {
      this.notificationService.warning(this.i18n.t('manualAttendance.ui.noEntriesToSave'));
      return;
    }

    this.saving.set(true);
    this.workforceService.saveAttendanceBatch(entries).subscribe({
      next: (response) => {
        this.saving.set(false);
        this.saveSummary.set(response);
        const failedKeys = new Set(response.errors
          .filter(error => error.workerId && error.workDate)
          .map(error => this.cellKey(error.workerId!, error.workDate!)));
        this.cellErrors.set(new Map(response.errors
          .filter(error => error.workerId && error.workDate)
          .map(error => [this.cellKey(error.workerId!, error.workDate!), error.message])));
        this.dirtyCellKeys.update(current => {
          const next = new Set(current);
          for (const entry of entries) {
            const key = this.cellKey(entry.workerId, entry.workDate);
            if (!failedKeys.has(key)) next.delete(key);
          }
          return next;
        });
        if (response.failedCount > 0) {
          this.notificationService.warning(this.i18n.t('manualAttendance.ui.partialSave', { saved: response.createdCount + response.updatedCount, failed: response.failedCount }));
        } else {
          this.notificationService.success(this.i18n.t('manualAttendance.ui.saveSuccess', { created: response.createdCount, updated: response.updatedCount, skipped: response.skippedCount }));
        }
      },
      error: (e) => {
        this.saving.set(false);
        this.notificationService.error(this.i18n.t('manualAttendance.ui.saveError', { detail: apiErrorDetail(e, e?.message ?? this.i18n.t('workforce.ui.unexpectedError')) }));
      }
    });
  }

  markCellDirty(workerId: string, workDate: string): void {
    const key = this.cellKey(workerId, workDate);
    this.dirtyCellKeys.update(current => new Set(current).add(key));
    this.cellErrors.update(current => {
      if (!current.has(key)) return current;
      const next = new Map(current);
      next.delete(key);
      return next;
    });
    this.saveSummary.set(null);
  }

  isCellDirty(workerId: string, workDate: string): boolean {
    return this.dirtyCellKeys().has(this.cellKey(workerId, workDate));
  }

  cellError(workerId: string, workDate: string): string | null {
    return this.cellErrors().get(this.cellKey(workerId, workDate)) ?? null;
  }

  hasUnsavedChanges(): boolean {
    return this.dirtyCellKeys().size > 0;
  }

  @HostListener('window:beforeunload', ['$event'])
  preventUnload(event: BeforeUnloadEvent): void {
    if (!this.hasUnsavedChanges()) return;
    event.preventDefault();
    event.returnValue = '';
  }

  private cellKey(workerId: string, workDate: string): string {
    return `${workerId}|${workDate}`;
  }

  private clearEditState(): void {
    this.dirtyCellKeys.set(new Set());
    this.cellErrors.set(new Map());
    this.saveSummary.set(null);
  }

  private confirmDiscardChanges(): boolean {
    return !this.hasUnsavedChanges() || window.confirm(this.i18n.t('manualAttendance.discardUnsavedConfirm'));
  }

  // --- Calculation helpers ---
  getWorkerTotalDays(workerId: string): number {
    return this.dates().reduce((sum, date) => sum + (this.matrix[workerId]?.[date]?.attendanceValue ?? 0), 0);
  }

  getWorkerTotalAmount(worker: Worker): number {
    return this.getWorkerTotalDays(worker.id) * (worker.defaultDailyRate ?? 0);
  }

  getDayTotal(date: string): number {
    return this.filteredWorkers().reduce((sum, w) => sum + (this.matrix[w.id]?.[date]?.attendanceValue ?? 0), 0);
  }

  getStatusLabel(value: number): string {
    if (value === 1) return this.i18n.t('manualAttendance.ui.present');
    if (value === 0.5) return this.i18n.t('manualAttendance.ui.halfDay');
    return this.i18n.t('manualAttendance.ui.absent');
  }

  getStatusClass(value: number): string {
    if (value === 1) return 'status-present';
    if (value === 0.5) return 'status-half';
    return 'status-absent';
  }

  hasOvertime(workerId: string): boolean {
    return this.dates().some(date =>
      (this.matrix[workerId]?.[date]?.overtimeHours ?? 0) > 0
    );
  }

  hasDeduction(workerId: string): boolean {
    return this.dates().some(date =>
      (this.matrix[workerId]?.[date]?.deductionHours ?? 0) > 0
    );
  }

  // --- Date helpers ---
  private generateDateRange(start: string, end: string): string[] {
    if (!start || !end) return [];
    const dates: string[] = [];
    const [sY, sM, sD] = start.split('-').map(Number);
    const [eY, eM, eD] = end.split('-').map(Number);
    const cur = new Date(sY, sM - 1, sD, 12, 0, 0);
    const endDate = new Date(eY, eM - 1, eD, 12, 0, 0);
    while (cur <= endDate && dates.length <= 31) {
      dates.push(this.toIso(cur));
      cur.setDate(cur.getDate() + 1);
    }
    return dates;
  }

  isWeekend(dateStr: string): boolean {
    const [y, m, d] = dateStr.split('-').map(Number);
    const day = new Date(y, m - 1, d, 12, 0, 0).getDay();
    return day === 5 || day === 6;
  }

  getDayName(dateStr: string): string {
    const names = ['manualAttendance.ui.daySun','manualAttendance.ui.dayMon','manualAttendance.ui.dayTue','manualAttendance.ui.dayWed','manualAttendance.ui.dayThu','manualAttendance.ui.dayFri','manualAttendance.ui.daySat'];
    const [y, m, d] = dateStr.split('-').map(Number);
    return this.i18n.t(names[new Date(y, m - 1, d, 12, 0, 0).getDay()]);
  }

  private toIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
