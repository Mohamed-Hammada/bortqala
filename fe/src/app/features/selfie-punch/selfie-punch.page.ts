import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Capacitor } from '@capacitor/core';
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { OfflineOutboxService } from '../../core/native/offline-outbox.service';

/**
 * WP-14 AC-3: employee selfie punch. The photo is captured with the device camera and
 * POSTed to the idempotent `/api/v1/attendance/selfie-punch` endpoint. If the request
 * fails (offline, server unreachable) it lands in the offline outbox under a
 * client-generated operationId and replays exactly-once when connectivity returns.
 */
@Component({
  selector: 'app-selfie-punch-page',
  templateUrl: './selfie-punch.page.html',
  styleUrl: './selfie-punch.page.scss',
})
export class SelfiePunchPage {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly outbox = inject(OfflineOutboxService);

  readonly previewDataUrl = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly queuedCount = signal(0);

  private operationId: string | null = null;
  private readonly maxBytes = 2 * 1024 * 1024;

  readonly canSubmit = computed(() => this.previewDataUrl() !== null && !this.submitting());

  async captureSelfie(): Promise<void> {
    try {
      if (Capacitor.isNativePlatform()) {
        const photo = await Camera.getPhoto({
          resultType: CameraResultType.DataUrl,
          source: CameraSource.Camera,
          direction: 'front' as unknown as never,
          quality: 60,
          width: 960,
        });
        this.applyImage(photo.dataUrl ?? null, 'image/jpeg');
      } else {
        document.getElementById('selfie-file-input')?.click();
      }
    } catch {
      this.notification.error(this.i18n.t('attendance.selfieCaptureFailed'));
    }
    await this.refreshQueued();
  }

  onFilePicked(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return Promise.resolve();
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        this.applyImage(typeof reader.result === 'string' ? reader.result : null, file.type || 'image/jpeg');
        resolve();
      };
      reader.onerror = () => {
        this.notification.error(this.i18n.t('attendance.selfieCaptureFailed'));
        resolve();
      };
      reader.readAsDataURL(file);
    });
  }

  async submitPunch(): Promise<void> {
    const dataUrl = this.previewDataUrl();
    if (!dataUrl || this.submitting()) return;
    this.operationId = this.operationId ?? crypto.randomUUID().replace(/-/g, '');
    const [meta, payload] = dataUrl.split(',');
    const contentType = meta.slice(meta.indexOf(':') + 1, meta.indexOf(';')) || 'image/jpeg';
    const bytes = Math.floor((payload.length * 3) / 4);
    if (bytes > this.maxBytes) {
      this.notification.error(this.i18n.t('attendance.selfieTooLarge'));
      return;
    }
    const body = {
      operationId: this.operationId,
      clientTimestamp: Date.now(),
      imageContentType: contentType,
      imageBytes: bytes,
      imageBase64: payload,
    };
    this.submitting.set(true);
    try {
      try {
        await firstValueFrom(this.http.post('/api/v1/attendance/selfie-punch', body));
        this.notification.success(this.i18n.t('attendance.selfiePunchSuccess'));
        this.reset();
      } catch {
        await this.outbox.enqueue('/api/v1/attendance/selfie-punch', body, body.operationId);
        this.notification.warning(this.i18n.t('attendance.selfieQueuedOffline'));
        this.reset();
      }
    } finally {
      this.submitting.set(false);
      await this.refreshQueued();
    }
  }

  private applyImage(dataUrl: string | null, contentType: string): void {
    if (!dataUrl) {
      this.notification.error(this.i18n.t('attendance.selfieCaptureFailed'));
      return;
    }
    this.operationId = null;
    this.previewDataUrl.set(dataUrl.startsWith('data:') ? dataUrl : `data:${contentType};base64,${dataUrl}`);
  }

  private reset(): void {
    this.previewDataUrl.set(null);
    this.operationId = null;
  }

  private async refreshQueued(): Promise<void> {
    this.queuedCount.set(await this.outbox.count());
  }
}
