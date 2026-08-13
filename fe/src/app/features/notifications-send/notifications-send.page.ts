import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { BulkSendResult, ExcelPreview, NotificationAdminService, NotificationAppSummary, NotificationUserSummary } from './notification-admin.service';
import { exportCsv } from '../../core/download';

type TargetMode = 'USERS' | 'EXCEL' | 'APP';

@Component({
  selector: 'app-notifications-send-page',
  standalone: true,
  templateUrl: './notifications-send.page.html',
  styleUrl: './notifications-send.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationsSendPage {
  readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly api = inject(NotificationAdminService);
  private readonly toast = inject(NotificationService);

  readonly apps = signal<NotificationAppSummary[]>([]);
  readonly users = signal<NotificationUserSummary[]>([]);
  readonly selectedAppId = signal('');
  readonly mode = signal<TargetMode>('USERS');
  readonly selectedUsers = signal<string[]>([]);
  readonly search = signal('');
  readonly preview = signal<ExcelPreview | null>(null);
  readonly result = signal<BulkSendResult | null>(null);
  readonly loading = signal(false);
  readonly sending = signal(false);
  readonly review = signal(false);

  readonly titleAr = signal(''); readonly titleEn = signal('');
  readonly messageAr = signal(''); readonly messageEn = signal('');
  readonly notificationType = signal('GENERAL');
  readonly priority = signal<'INFO'|'MEDIUM'|'HIGH'|'CRITICAL'>('INFO');
  readonly actionLink = signal('');

  readonly recipientCount = computed(() => this.mode() === 'APP'
    ? this.users().filter(u => u.active).length
    : this.mode() === 'EXCEL' ? (this.preview()?.validCount ?? 0) : this.selectedUsers().length);
  readonly canReview = computed(() => !!this.selectedAppId() && this.recipientCount() > 0
    && !!this.titleAr().trim() && !!this.titleEn().trim() && !!this.messageAr().trim() && !!this.messageEn().trim());

  constructor() { void this.loadApps(); }

  async loadApps(): Promise<void> {
    this.loading.set(true);
    try {
      const apps = await firstValueFrom(this.api.apps()); this.apps.set(apps);
      const preferred = this.auth.app()?.id; const appId = apps.find(a => a.id === preferred)?.id ?? apps[0]?.id ?? '';
      this.selectedAppId.set(appId); if (appId) await this.loadUsers();
    } catch (e) { this.toast.error(apiErrorMessage(e, this.i18n)); } finally { this.loading.set(false); }
  }

  async selectApp(appId: string): Promise<void> { this.selectedAppId.set(appId); this.selectedUsers.set([]); this.preview.set(null); await this.loadUsers(); }
  setMode(mode: TargetMode): void { this.mode.set(mode); this.review.set(false); this.result.set(null); }
  async loadUsers(): Promise<void> {
    if (!this.selectedAppId()) return;
    try { this.users.set(await firstValueFrom(this.api.users(this.selectedAppId(), this.search()))); }
    catch (e) { this.toast.error(apiErrorMessage(e, this.i18n)); }
  }
  toggleUser(username: string, checked: boolean): void {
    const set = new Set(this.selectedUsers()); checked ? set.add(username) : set.delete(username); this.selectedUsers.set([...set]);
  }
  selectAllVisible(): void { this.selectedUsers.set(this.users().filter(u => u.active).map(u => u.username)); }
  clearUsers(): void { this.selectedUsers.set([]); }

  async onExcel(file: File | null): Promise<void> {
    this.preview.set(null); if (!file) return; this.loading.set(true);
    try { this.preview.set(await firstValueFrom(this.api.previewExcel(this.selectedAppId(), file))); }
    catch (e) { this.toast.error(apiErrorMessage(e, this.i18n)); } finally { this.loading.set(false); }
  }

  downloadRecipientTemplate(): void {
    exportCsv([{ username: 'example.username' }], [{ key: 'username', label: 'username' }], 'notification-recipients-template.csv');
    this.toast.success(this.i18n.t('imports.templateDownloadSuccess'));
  }

  openReview(): void { if (this.canReview()) this.review.set(true); }
  cancelReview(): void { this.review.set(false); }

  async send(): Promise<void> {
    if (!this.canReview()) return; this.sending.set(true); this.result.set(null);
    try {
      const usernames = this.mode() === 'EXCEL' ? (this.preview()?.validUsernames ?? []) : this.mode() === 'USERS' ? this.selectedUsers() : [];
      const result = await firstValueFrom(this.api.send({
        targetAppId: this.selectedAppId(), mode: this.mode(), usernames, titleAr: this.titleAr().trim(), titleEn: this.titleEn().trim(),
        messageAr: this.messageAr().trim(), messageEn: this.messageEn().trim(), notificationType: this.notificationType().trim().toUpperCase() || 'GENERAL',
        priority: this.priority(), actionLink: this.actionLink().trim() || null,
      }));
      this.result.set(result); this.review.set(false); this.toast.success(this.i18n.t('notificationsSend.sent'));
    } catch (e) { this.toast.error(apiErrorMessage(e, this.i18n)); } finally { this.sending.set(false); }
  }
}
