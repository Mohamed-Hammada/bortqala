import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDateTime } from '../../core/date';
import { ImportsStore } from './imports.store';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
import { ImportBatch, ImportStatus, ImportPreview } from './imports.models';
import { AppTooltipDirective } from '../../shared/ui/app-tooltip/app-tooltip.directive';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { FormsModule } from '@angular/forms';
import { BiometricDevice, BiometricSource, BiometricSourceType } from './imports.models';
import { SampleTemplateService } from '../../core/sample-template.service';

@Component({
  selector: 'app-imports-page',
  imports: [RouterLink, TablePaginationComponent, AppTooltipDirective, FormsModule, ModalDialogComponent],
  providers: [ImportsStore],
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsPage {
  readonly store = inject(ImportsStore);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly sampleTemplates = inject(SampleTemplateService);
  readonly file = signal<File | null>(null);
  readonly duplicateChecking = signal(false);
  readonly isDragging = signal(false);
  readonly historyView = signal(new URLSearchParams(window.location.search).get('history') === 'all');
  readonly selectedSourceId = signal('');
  readonly expanded = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly pagedUnmatched = computed(() => this.pagination.slice(this.store.unmatched()));
  readonly activeSources = computed(() => this.store.sources().filter((s) => s.active));
  readonly uploadSources = computed(() => this.store.sources().filter((s) => s.active && s.sourceType === 'FILE_DEVICE'));
  readonly employeeCategories = computed(() =>
    this.store.categories().filter((category) => category.active && category.scope !== 'WORKER'));
  readonly editingDeviceId = signal<string | null>(null);
  readonly connectionName = signal('');
  readonly endpointUrl = signal('');
  readonly syncIntervalMinutes = signal(15);
  readonly connectionEnabled = signal(true);
  readonly deviceUsername = signal('');
  readonly devicePassword = signal('');
  readonly syncingDeviceId = signal<string | null>(null);
  readonly showTemplateModal = signal(false);
  readonly previewResult = signal<ImportPreview | null>(null);
  readonly previewing = signal(false);
  readonly reversingBatchId = signal<string | null>(null);
  readonly editingSourceId = signal<string | null>(null);
  readonly sourceName = signal('');
  readonly sourceType = signal<BiometricSourceType>('FILE_DEVICE');
  readonly sourceActive = signal(true);
  readonly sourceAutoCreateEmployees = signal(false);
  readonly sourceAutoCreateCategoryId = signal('');
  readonly sourceAutoCreateEmploymentType = signal<'FIXED' | 'DAILY'>('FIXED');
  readonly sourceAutoCreateActiveFromMode = signal<'FIRST_PUNCH' | 'IMPORT_DATE'>('FIRST_PUNCH');
  readonly sourceAutoCreateEmployeeActive = signal(true);
  readonly showSourceModal = signal(false);

  downloadTemplate(): void {
    void this.sampleTemplates.attendance()
      .then(() => this.notification.success(this.i18n.t('imports.templateDownloadSuccess', {})))
      .catch(() => this.notification.error(this.i18n.t('common.loadError')));
  }

  constructor() {
    void this.store.load().then(() => {
      this.markAttendanceDataChanged();
      if (!this.selectedSourceId() && this.uploadSources().length > 0) {
        this.selectedSourceId.set(this.uploadSources()[0].id);
      }
    });
  }

  async choose(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const candidate = input.files?.item(0) ?? null;
    input.value = '';
    if (candidate) await this.acceptFile(candidate);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    const candidate = event.dataTransfer?.files?.item(0);
    if (candidate) void this.acceptFile(candidate);
  }

  clearFile(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    this.file.set(null);
    this.previewResult.set(null);
  }

  private async acceptFile(candidate: File): Promise<void> {
    this.previewResult.set(null);
    const sourceId = this.selectedSourceId();
    if (!sourceId || candidate.size > 64 * 1024 * 1024) { this.file.set(candidate); return; }
    this.duplicateChecking.set(true);
    try {
      const checksum = await this.sha256(candidate);
      if (await this.store.isDuplicate(sourceId, checksum)) {
        this.file.set(null);
        this.notification.warning(this.i18n.locale() === 'ar-EG'
          ? 'تم استيراد هذا الملف مسبقاً — لن يتم رفعه أو تحليله مرة أخرى.'
          : 'This file was already imported — it will not be uploaded or parsed again.');
        return;
      }
      this.file.set(candidate);
    } catch {
      this.file.set(candidate);
    } finally {
      this.duplicateChecking.set(false);
    }
  }

  private async sha256(file: File): Promise<string> {
    const buffer = await file.arrayBuffer();
    const digest = await crypto.subtle.digest('SHA-256', buffer);
    return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, '0')).join('');
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  async upload() {
    const file = this.file();
    if (file && this.selectedSourceId()) {
      if (file.size <= 64 * 1024 * 1024) {
        const checksum = await this.sha256(file);
        if (await this.store.isDuplicate(this.selectedSourceId(), checksum)) {
          this.file.set(null);
          this.previewResult.set(null);
          this.notification.warning(this.i18n.locale() === 'ar-EG'
            ? 'تم استيراد هذا الملف مسبقاً — تم إلغاء الرفع قبل إرسال البيانات.'
            : 'This file was already imported — upload was cancelled before sending the file.');
          return;
        }
      }
      if (await this.store.upload(file, this.selectedSourceId())) {
        this.notification.success(this.i18n.t('imports.uploadSuccess') || 'تم استيراد ملف البصمة بنجاح ✓');
        this.file.set(null);
        this.previewResult.set(null);
        this.markAttendanceDataChanged();
      }
    } else if (!this.selectedSourceId()) {
      this.notification.warning(this.i18n.t('imports.sourceRequired', {}));
    }
  }

  async previewFile(): Promise<void> {
    const file = this.file();
    if (!file) return;
    this.previewing.set(true);
    try {
      const result = await this.store.preview(file);
      if (result) this.previewResult.set(result);
    } finally {
      this.previewing.set(false);
    }
  }

  async reverseBatch(item: ImportBatch): Promise<void> {
    if (!window.confirm(this.i18n.t('imports.reverseBatchConfirm', {}))) return;
    this.reversingBatchId.set(item.id);
    try {
      if (await this.store.reverseBatch(item.id)) {
        this.notification.success(this.i18n.t('imports.batchReversedSuccess', {}));
      }
    } finally {
      this.reversingBatchId.set(null);
    }
  }

  prepareReimport(item: ImportBatch): void {
    this.selectedSourceId.set(item.sourceId);
    this.file.set(null);
    this.previewResult.set(null);
    const input = document.querySelector<HTMLInputElement>('.hidden-file-input');
    if (input) {
      input.value = '';
      input.click();
    }
  }

  dateTime(value: number) {
    return formatDateTime(value);
  }

  statusLabel(status: ImportStatus): string {
    if (status === 'REVERSED') return this.i18n.t('imports.reversedStatus', {});
    return this.i18n.t(status === 'COMPLETED' ? 'imports.completed' : 'imports.completedWithErrors');
  }

  previewShortHash(checksum: string): string {
    return checksum.length > 12 ? checksum.substring(0, 12) + '…' : checksum;
  }

  editDevice(device: BiometricDevice): void {
    this.editingDeviceId.set(device.id);
    this.connectionName.set(device.name);
    this.endpointUrl.set(device.endpointUrl);
    this.syncIntervalMinutes.set(device.syncIntervalMinutes);
    this.connectionEnabled.set(device.enabled);
    this.deviceUsername.set(device.username ?? '');
    this.devicePassword.set('');
  }

  resetDeviceForm(): void {
    this.editingDeviceId.set(null);
    this.connectionName.set('');
    this.endpointUrl.set('');
    this.syncIntervalMinutes.set(15);
    this.connectionEnabled.set(true);
    this.deviceUsername.set('');
    this.devicePassword.set('');
  }

  async saveDevice(): Promise<void> {
    const name = this.connectionName().trim();
    const endpointUrl = this.endpointUrl().trim();
    if (!name || !endpointUrl || this.syncIntervalMinutes() < 1) {
      this.notification.warning(this.i18n.t('imports.deviceFormInvalid', {}));
      return;
    }
    const saved = await this.store.saveDevice({
      name,
      endpointUrl,
      enabled: this.connectionEnabled(),
      syncIntervalMinutes: this.syncIntervalMinutes(),
      username: this.deviceUsername().trim() || undefined,
      password: this.devicePassword() || undefined,
    }, this.editingDeviceId() ?? undefined);
    if (saved) {
      this.notification.success(this.i18n.t(this.editingDeviceId() ? 'imports.deviceUpdated' : 'imports.deviceConnected',
        {}, this.editingDeviceId() ? 'تم تحديث إعدادات جهاز البصمة.' : 'تم إضافة جهاز البصمة للربط المباشر.'));
      this.resetDeviceForm();
    }
  }

  async syncDevice(device: BiometricDevice): Promise<void> {
    this.syncingDeviceId.set(device.id);
    try {
      const result = await this.store.syncDevice(device.id);
      if (!result) return;
      if (result.device.lastStatus === 'FAILED') {
        this.notification.error(result.device.lastMessage || this.i18n.t('imports.syncFailed', {}));
      } else {
        this.notification.success(this.i18n.t('imports.syncResult', { imported: result.importedRows, duplicates: result.duplicateRows }));
      }
    } finally {
      this.syncingDeviceId.set(null);
    }
  }

  isDeviceSource(source: BiometricSource): boolean {
    return source.sourceType === 'DEVICE';
  }

  editSource(source: BiometricSource): void {
    this.editingSourceId.set(source.id);
    this.sourceName.set(source.name);
    this.sourceType.set(source.sourceType);
    this.sourceActive.set(source.active);
    this.sourceAutoCreateEmployees.set(source.autoCreateEmployees);
    this.sourceAutoCreateCategoryId.set(source.autoCreateCategoryId ?? '');
    this.sourceAutoCreateEmploymentType.set(source.autoCreateEmploymentType ?? 'FIXED');
    this.sourceAutoCreateActiveFromMode.set(source.autoCreateActiveFromMode ?? 'FIRST_PUNCH');
    this.sourceAutoCreateEmployeeActive.set(source.autoCreateEmployeeActive ?? true);
    this.showSourceModal.set(true);
  }

  resetSourceForm(): void {
    this.editingSourceId.set(null);
    this.sourceName.set('');
    this.sourceType.set('FILE_DEVICE');
    this.sourceActive.set(true);
    this.sourceAutoCreateEmployees.set(false);
    this.sourceAutoCreateCategoryId.set('');
    this.sourceAutoCreateEmploymentType.set('FIXED');
    this.sourceAutoCreateActiveFromMode.set('FIRST_PUNCH');
    this.sourceAutoCreateEmployeeActive.set(true);
    this.showSourceModal.set(false);
  }

  onAutoCreateEmployeesChange(enabled: boolean): void {
    this.sourceAutoCreateEmployees.set(enabled);
    if (!enabled) return;
    if (!this.sourceAutoCreateCategoryId() && this.employeeCategories().length > 0) {
      this.sourceAutoCreateCategoryId.set(this.employeeCategories()[0].id);
    }
    this.sourceAutoCreateEmploymentType.set(this.sourceAutoCreateEmploymentType() || 'FIXED');
    this.sourceAutoCreateActiveFromMode.set(this.sourceAutoCreateActiveFromMode() || 'FIRST_PUNCH');
  }

  async saveSource(): Promise<void> {
    const name = this.sourceName().trim();
    if (!name) {
      this.notification.warning(this.i18n.t('imports.sourceNameInvalid', {}));
      return;
    }
    if (this.sourceAutoCreateEmployees()
        && this.employeeCategories().length > 0
        && !this.sourceAutoCreateCategoryId()) {
      this.notification.warning(this.i18n.t('imports.autoCreateCategoryRequired'));
      return;
    }
    if (this.sourceAutoCreateEmployees()
        && (!this.sourceAutoCreateEmploymentType() || !this.sourceAutoCreateActiveFromMode())) {
      this.notification.warning(this.i18n.t('imports.autoCreateRequiredDefaults'));
      return;
    }
    const saved = await this.store.saveSource({
      name,
      sourceType: this.sourceType(),
      active: this.sourceActive(),
      autoCreateEmployees: this.sourceAutoCreateEmployees(),
      autoCreateCategoryId: this.sourceAutoCreateEmployees() ? this.sourceAutoCreateCategoryId() : null,
      autoCreateEmploymentType: this.sourceAutoCreateEmploymentType(),
      autoCreateActiveFromMode: this.sourceAutoCreateActiveFromMode(),
      autoCreateEmployeeActive: this.sourceAutoCreateEmployeeActive(),
    }, this.editingSourceId() ?? undefined);
    if (saved) {
      this.notification.success(this.i18n.t(this.editingSourceId() ? 'imports.sourceUpdated' : 'imports.sourceCreated'));
      if (!this.selectedSourceId() && this.sourceActive()) {
        this.selectedSourceId.set(this.store.sources().find((s) => s.name === name)?.id ?? '');
      }
      this.resetSourceForm();
    }
  }

  async deleteSource(source: BiometricSource): Promise<void> {
    if (!window.confirm(this.i18n.t('imports.deleteSourceConfirm', {}))) return;
    if (await this.store.deleteSource(source.id)) {
      this.notification.success(this.i18n.t('imports.sourceDeleted', {}));
      if (this.selectedSourceId() === source.id) this.selectedSourceId.set('');
    }
  }

  // BORTQALA_FEEDBACK_20260816_IMPORT_HISTORY
  visibleBatches() {
    const batches = this.store.batches();
    return this.historyView() ? batches : batches.slice(0, 5);
  }


  // BORTQALA_RUNTIME_20260816_V2_ATTENDANCE_REFRESH_SIGNAL
  private markAttendanceDataChanged(): void {
    const value = Date.now().toString();
    window.localStorage.setItem('bortqala.attendance.changedAt', value);
    window.dispatchEvent(new CustomEvent('bortqala:attendance-updated', { detail: value }));
  }

}
