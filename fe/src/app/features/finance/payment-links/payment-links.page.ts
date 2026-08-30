import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { PaymentLink } from './payment-link.models';
import { PaymentLinkService } from './payment-link.service';

@Component({
  selector: 'app-payment-links-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './payment-links.page.html',
  styleUrl: './payment-links.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentLinksPage implements OnInit {
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  private readonly service = inject(PaymentLinkService);

  readonly links = signal<PaymentLink[]>([]);
  readonly loading = signal(true);
  readonly creating = signal(false);
  readonly showCreate = signal(false);
  readonly newAmount = signal(0);
  readonly newDescription = signal('');
  readonly newKind = signal('INVOICE');
  readonly gatewayEnabled = signal(false);

  async ngOnInit() {
    await this.loadLinks();
    try {
      const config = await this.service.getGatewayConfig();
      this.gatewayEnabled.set(config.enabled);
    } catch { /* ignore */ }
  }

  async loadLinks() {
    this.loading.set(true);
    try {
      this.links.set(await this.service.listLinks());
    } catch (e: any) {
      this.notification.error(e?.error?.message ?? this.i18n.t('common.loadError'));
    } finally {
      this.loading.set(false);
    }
  }

  async createLink() {
    if (this.newAmount() <= 0) return;
    this.creating.set(true);
    try {
      await this.service.createLink({
        kind: this.newKind(),
        amount: this.newAmount(),
        description: this.newDescription() || undefined,
      });
      this.showCreate.set(false);
      this.newAmount.set(0);
      this.newDescription.set('');
      this.notification.success(this.i18n.t('finance.paylinkCreate'));
      await this.loadLinks();
    } catch (e: any) {
      this.notification.error(e?.error?.message ?? this.i18n.t('common.error'));
    } finally {
      this.creating.set(false);
    }
  }

  async cancelLink(id: string) {
    try {
      await this.service.cancelLink(id);
      await this.loadLinks();
    } catch (e: any) {
      this.notification.error(e?.error?.message ?? this.i18n.t('common.error'));
    }
  }

  copyLink(token: string) {
    const url = `${window.location.origin}/p/${token}`;
    navigator.clipboard?.writeText(url);
    this.notification.success(this.i18n.t('finance.paylinkCopy'));
  }

  shareWhatsApp(token: string, amount: number) {
    const url = `${window.location.origin}/p/${token}`;
    const text = `Payment of ${amount} EGP: ${url}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank');
  }

  statusColor(status: string): string {
    switch (status) {
      case 'PENDING': return 'var(--gold)';
      case 'PAID': return 'var(--success)';
      case 'EXPIRED': return 'var(--muted)';
      case 'CANCELLED': return 'var(--danger)';
      default: return 'var(--muted)';
    }
  }
}
