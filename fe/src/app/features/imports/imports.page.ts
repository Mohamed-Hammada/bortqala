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
import { BiometricDevice } from './imports.models';
import { exportCsv } from '../../core/download';

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
  readonly file = signal<File | null>(null);
  readonly isDragging = signal(false);
  readonly deviceName = signal(this.i18n.t('imports.defaultDevice'));
  readonly expanded = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly pagedUnmatched = computed(() => this.pagination.slice(this.store.unmatched()));
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

  downloadTemplate(): void {
    const columns = [
      { key: 'deviceUserId', label: this.i18n.t('imports.sampleColumnCode', {}, 'كود البصمة / العامل') },
      { key: 'punchedAt', label: this.i18n.t('imports.sampleColumnTime', {}, 'تاريخ ووقت البصمة') },
      { key: 'deviceName', label: this.i18n.t('imports.sampleColumnDevice', {}, 'اسم الجهاز') }
    ];
    const sampleRows = [
      { deviceUserId: '101', punchedAt: '2026-07-31 08:30:00', deviceName: this.i18n.t('imports.sampleDeviceGate', {}, 'بوابة المصنع') },
      { deviceUserId: '101', punchedAt: '2026-07-31 17:00:00', deviceName: this.i18n.t('imports.sampleDeviceGate', {}, 'بوابة المصنع') },
      { deviceUserId: '102', punchedAt: '2026-07-31 08:45:00', deviceName: this.i18n.t('imports.sampleDeviceMain', {}, 'البوابة الرئيسية') }
    ];
    exportCsv(sampleRows, columns, this.i18n.t('imports.templateFileName', {}, 'قالب_استيراد_البصمات.csv'));
    this.notification.success(this.i18n.t('imports.templateDownloadSuccess', {}, 'تم تنزيل قالب استيراد البصمة بنجاح.'));
  }

  constructor() {
    void this.store.load();
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
    if (file && this.deviceName().trim()) {
      if (await this.store.upload(file, this.deviceName())) {
        this.notification.success(this.i18n.t('imports.uploadSuccess') || 'تم استيراد ملف البصمة بنجاح ✓');
        this.file.set(null);
        this.previewResult.set(null);
      }
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
    if (!window.confirm(this.i18n.t('imports.reverseBatchConfirm', {}, 'هل تريد تراجع سجل الاستيراد وحذف كل البصمات المستوردة منه؟'))) return;
    this.reversingBatchId.set(item.id);
    try {
      if (await this.store.reverseBatch(item.id)) {
        this.notification.success(this.i18n.t('imports.batchReversedSuccess', {}, 'تم تراجع سجل الاستيراد وحذف البصمات المرتبطة به.'));
      }
    } finally {
      this.reversingBatchId.set(null);
    }
  }

  dateTime(value: number) {
    return formatDateTime(value);
  }

  statusLabel(status: ImportStatus): string {
    if (status === 'REVERSED') return this.i18n.t('imports.reversedStatus', {}, 'ملغي');
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
      this.notification.warning(this.i18n.t('imports.deviceFormInvalid', {}, 'أدخل اسم الجهاز ورابط API وفترة مزامنة صحيحة.'));
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
        this.notification.error(result.device.lastMessage || this.i18n.t('imports.syncFailed', {}, 'فشلت مزامنة جهاز البصمة.'));
      } else {
        this.notification.success(this.i18n.t('imports.syncResult', { imported: result.importedRows, duplicates: result.duplicateRows }, 'اكتملت المزامنة: بصمة جديدة، مكررة.'));
      }
    } finally {
      this.syncingDeviceId.set(null);
    }
  }
}
