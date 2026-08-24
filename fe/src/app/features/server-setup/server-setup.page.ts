import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { NativeBridgeService } from '../../core/native/native-bridge.service';
import { ServerProbeService } from '../../core/native/server-probe.service';

/** WP-14 AC-1: first-launch company-server picker, reachable outside the auth guard. */
@Component({
  selector: 'app-server-setup-page',
  imports: [FormsModule],
  templateUrl: './server-setup.page.html',
  styleUrl: './server-setup.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServerSetupPage {
  private readonly native = inject(NativeBridgeService);
  private readonly probe = inject(ServerProbeService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);

  serverUrlValue = '';
  readonly checking = signal(false);
  readonly errorKey = signal<string | null>(null);

  async connect(): Promise<void> {
    if (this.checking()) return;
    this.errorKey.set(null);
    this.checking.set(true);
    try {
      const result = await this.probe.probe(this.serverUrlValue);
      if (!result.ok) {
        this.errorKey.set(result.errorKey ?? 'native.probeUnreachable');
        return;
      }
      await this.native.storeServerUrl(this.serverUrlValue);
      this.notification.success(this.i18n.t('native.serverSaved'));
      await this.router.navigateByUrl('/login');
    } catch (error) {
      this.notification.error(apiErrorMessage(error, this.i18n));
    } finally {
      this.checking.set(false);
    }
  }
}
