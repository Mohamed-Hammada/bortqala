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
  readonly isDragging = signal(false);
  readonly selectedSourceId = signal('');
  readonly expanded = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly pagedUnmatched = computed(() => this.pagination.slice(this.store.unmatched()));
  readonly activeSources = computed(() => this.store.sources().filter((s) => s.active));
  readonly uploadSources = computed(() => this.store.sources().filter((s) => s.active && s.sourceType === 'FILE_DEVICE'));
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
  readonly showSourceModal = signal(false);

  downloadTemplate(): void {
    void this.sampleTemplates.attendance()
      .then(() => this.notification.success(this.i18n.t('imports.templateDownloadSuccess', {})))
      .catch(() => this.notification.error(this.i18n.t('common.loadError')));
  }

  constructor() {
    void this.store.load().then(() => {
      if (!this.selectedSourceId() && this.uploadSources().length > 0) {
        this.selectedSourceId.set(this.uploadSources()[0].id);
      }
    });
  }

  choose(event: Event) {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.item(0) ?? null);
    this.previewResult.set(null);
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

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const droppedFile = event.dataTransfer.files[0];
      this.file.set(droppedFile);
      this.previewResult.set(null);
    }
  }

  clearFile(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    this.file.set(null);
    this.previewResult.set(null);
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
      if (await this.store.upload(file, this.selectedSourceId())) {
        this.notification.success(this.i18n.t('imports.uploadSuccess') || 'تم استيراد ملف البصمة بنجاح ✓');
        this.file.set(null);
        this.previewResult.set(null);
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
    this.showSourceModal.set(true);
  }

  resetSourceForm(): void {
    this.editingSourceId.set(null);
    this.sourceName.set('');
    this.sourceType.set('FILE_DEVICE');
    this.sourceActive.set(true);
    this.showSourceModal.set(false);
  }

  async saveSource(): Promise<void> {
    const name = this.sourceName().trim();
    if (!name) {
      this.notification.warning(this.i18n.t('imports.sourceNameInvalid', {}));
      return;
    }
    const saved = await this.store.saveSource({
      name,
      sourceType: this.sourceType(),
      active: this.sourceActive(),
    }, this.editingSourceId() ?? undefined);
    if (saved) {
      this.notification.success(this.i18n.t(this.editingSourceId() ? 'imports.sourceUpdated' : 'imports.sourceCreated',
        {}, this.editingSourceId() ? 'تم تحديث مصدر البصمات.' : 'تم إنشاء مصدر البصمات.'));
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
}
