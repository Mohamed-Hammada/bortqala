import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';
import { PaymentLinkService } from '../../finance/payment-links/payment-link.service';
import { PublicPagePayload } from '../../finance/payment-links/payment-link.models';

@Component({
  selector: 'app-public-pay-page',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <section class="public-pay">
      @if (loading()) {
        <div class="pay-card">{{ i18n.t('common.loading') }}</div>
      } @else if (page()) {
        <div class="pay-card">
          <h1>{{ i18n.t('finance.paylinkPageTitle') }}</h1>
          @if (page()!.paid) {
            <div class="status paid">{{ i18n.t('finance.paylinkPagePaid') }}</div>
          } @else if (page()!.expired) {
            <div class="status expired">{{ i18n.t('finance.paylinkPageExpired') }}</div>
          } @else {
            <p class="company">{{ i18n.t('finance.paylinkPageCompany') }}: {{ page()!.companyName }}</p>
            <p class="desc">{{ i18n.t('finance.paylinkPageDesc') }}: {{ page()!.description }}</p>
            <p class="amount">{{ i18n.t('finance.paylinkPageAmount') }}: {{ page()!.amount | number:'1.2-2' }} {{ page()!.currency }}</p>
            <button class="pay-btn" (click)="pay()">{{ i18n.t('finance.paylinkPagePayBtn') }}</button>
          }
        </div>
      } @else {
        <div class="pay-card error">{{ i18n.t('finance.paylinkNotFound') }}</div>
      }
    </section>
  `,
  styles: [`
    .public-pay { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: var(--surface); }
    .pay-card { background: var(--surface-card); border-radius: 12px; padding: 2.5rem; max-width: 420px; width: 100%; text-align: center; box-shadow: 0 4px 24px rgba(0,0,0,0.08); }
    h1 { margin-bottom: 1.5rem; color: var(--ink); }
    .company, .desc { color: var(--muted); margin: 0.5rem 0; }
    .amount { font-size: 1.8rem; font-weight: 700; color: var(--gold); margin: 1.5rem 0; }
    .pay-btn { width: 100%; padding: 0.9rem; border: none; border-radius: 8px; background: var(--gold); color: var(--ink); font-size: 1.1rem; font-weight: 600; cursor: pointer; }
    .status { padding: 1rem; border-radius: 8px; font-weight: 600; }
    .status.paid { background: var(--success-soft); color: var(--success); }
    .status.expired { background: var(--warning-soft); color: var(--warning-text); }
    .error { color: var(--muted); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicPayPage implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(PaymentLinkService);
  readonly page = signal<PublicPagePayload | null>(null);
  readonly loading = signal(true);

  async ngOnInit() {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    try {
      this.page.set(await this.service.getPublicPage(token));
    } catch {
      this.page.set(null);
    } finally {
      this.loading.set(false);
    }
  }

  pay() {
    // Redirect to gateway checkout — in production this would use the gateway SDK
  }
}
