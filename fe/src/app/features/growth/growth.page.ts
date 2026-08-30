import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { DecimalPipe } from '@angular/common';

interface LoyaltyAccount { id: string; partyId: string; pointsBalance: number; totalEarned: number; totalRedeemed: number; totalExpired: number; createdAt: number; }
interface LoyaltyLedgerEntry { id: string; loyaltyAccountId: string; partyId: string; type: string; points: number; runningBalance: number; referenceType: string; referenceId: string; ruleSnapshot: string; notes: string; actor: string; createdAt: number; }
interface LoyaltyRule { pointsPerHundredEgp: number; redeemMaxPercent: number; expiryMonths: number; }
interface MembershipPlan { id: string; name: string; nameEn: string; price: number; currencyCode: string; periodDays: number; graceDays: number; autoRenew: boolean; active: boolean; loyaltyEarnRate: number; createdAt: number; }
interface MemberSubscription { id: string; partyId: string; planId: string; planName: string; startDate: number; currentPeriodEnd: number; nextInvoiceDate: number; status: string; cancelledAt: number; createdAt: number; }
interface Referral { id: string; referrerPartyId: string; referredPartyId: string; status: string; rewardPoints: number; firstPurchaseReferenceId: string; createdAt: number; }
interface ReferralRule { referrerPoints: number; referredPoints: number; }
interface ReferralReportEntry { partyId: string; partyName: string; totalReferrals: number; rewardedReferrals: number; totalPointsEarned: number; }

@Component({
  selector: 'app-growth-page',
  imports: [ModalDialogComponent, DecimalPipe],
  templateUrl: './growth.page.html',
  styleUrl: './growth.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GrowthPage {
  readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);

  readonly activeTab = signal<'loyalty' | 'membership' | 'referral'>('loyalty');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly loyaltyAccount = signal<LoyaltyAccount | null>(null);
  readonly loyaltyLedger = signal<LoyaltyLedgerEntry[]>([]);
  readonly loyaltyRules = signal<LoyaltyRule | null>(null);
  readonly membershipPlans = signal<MembershipPlan[]>([]);
  readonly subscriptions = signal<MemberSubscription[]>([]);
  readonly referralRules = signal<ReferralRule | null>(null);
  readonly referrals = signal<Referral[]>([]);
  readonly topReferrers = signal<ReferralReportEntry[]>([]);

  readonly loyaltyDrawerOpen = signal(false);
  readonly loyaltyPartyId = signal('');
  readonly earnPointsAmount = signal(0);
  readonly redeemPointsAmount = signal(0);

  readonly planDrawerOpen = signal(false);
  readonly planName = signal('');
  readonly planPrice = signal(0);
  readonly planPeriodDays = signal(30);

  readonly referralDrawerOpen = signal(false);
  readonly referrerPartyId = signal('');
  readonly referredPartyId = signal('');

  constructor() { this.loadAll(); }

  async loadAll(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [rules, plans, subs, rRules, topRef] = await Promise.all([
        this.http.get<LoyaltyRule>('/api/v1/growth/loyalty/rules').toPromise(),
        this.http.get<MembershipPlan[]>('/api/v1/growth/membership/plans').toPromise(),
        this.http.get<MemberSubscription[]>('/api/v1/growth/membership/subscriptions').toPromise(),
        this.http.get<ReferralRule>('/api/v1/growth/referrals/rules').toPromise(),
        this.http.get<ReferralReportEntry[]>('/api/v1/growth/referrals/report/top?limit=10').toPromise(),
      ]);
      if (rules) this.loyaltyRules.set(rules);
      this.membershipPlans.set(plans ?? []);
      this.subscriptions.set(subs ?? []);
      if (rRules) this.referralRules.set(rRules);
      this.topReferrers.set(topRef ?? []);
    } catch (e: any) {
      this.error.set(e?.error?.detail ?? e?.message ?? 'Failed to load');
    } finally {
      this.loading.set(false);
    }
  }

  async loadLoyaltyAccount(partyId: string): Promise<void> {
    if (!partyId.trim()) return;
    this.loyaltyPartyId.set(partyId);
    try {
      const [acct, ledger] = await Promise.all([
        this.http.get<LoyaltyAccount>(`/api/v1/growth/loyalty/accounts/${partyId}`).toPromise(),
        this.http.get<LoyaltyLedgerEntry[]>(`/api/v1/growth/loyalty/ledger/${partyId}`).toPromise(),
      ]);
      this.loyaltyAccount.set(acct ?? null);
      this.loyaltyLedger.set(ledger ?? []);
      this.loyaltyDrawerOpen.set(true);
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to load loyalty account');
    }
  }

  async earnPoints(): Promise<void> {
    if (!this.loyaltyPartyId() || this.earnPointsAmount() <= 0) return;
    try {
      await this.http.post('/api/v1/growth/loyalty/earn', {
        partyId: this.loyaltyPartyId(), points: this.earnPointsAmount(), notes: 'Manual earn'
      }).toPromise();
      this.notification.success(this.i18n.t('growth.earnPoints') + ' ✓');
      this.earnPointsAmount.set(0);
      await this.loadLoyaltyAccount(this.loyaltyPartyId());
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to earn points');
    }
  }

  async redeemPoints(): Promise<void> {
    if (!this.loyaltyPartyId() || this.redeemPointsAmount() <= 0) return;
    try {
      await this.http.post('/api/v1/growth/loyalty/redeem', {
        partyId: this.loyaltyPartyId(), points: this.redeemPointsAmount(), notes: 'Manual redeem'
      }).toPromise();
      this.notification.success(this.i18n.t('growth.redeemPoints') + ' ✓');
      this.redeemPointsAmount.set(0);
      await this.loadLoyaltyAccount(this.loyaltyPartyId());
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to redeem points');
    }
  }

  async createPlan(): Promise<void> {
    if (!this.planName()) return;
    try {
      await this.http.post('/api/v1/growth/membership/plans', {
        name: this.planName(), price: this.planPrice(), currencyCode: 'EGP',
        periodDays: this.planPeriodDays(), graceDays: 7, autoRenew: true
      }).toPromise();
      this.notification.success(this.i18n.t('growth.createPlan') + ' ✓');
      this.planDrawerOpen.set(false);
      this.planName.set(''); this.planPrice.set(0);
      await this.loadAll();
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to create plan');
    }
  }

  async createReferral(): Promise<void> {
    if (!this.referrerPartyId() || !this.referredPartyId()) return;
    try {
      await this.http.post('/api/v1/growth/referrals', {
        referrerPartyId: this.referrerPartyId(), referredPartyId: this.referredPartyId()
      }).toPromise();
      this.notification.success(this.i18n.t('growth.createReferral') + ' ✓');
      this.referralDrawerOpen.set(false);
      await this.loadAll();
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to create referral');
    }
  }

  async runRenewal(): Promise<void> {
    try {
      const result: any = await this.http.post('/api/v1/growth/membership/renewal/run', {}).toPromise();
      this.notification.success(`${this.i18n.t('growth.renewalResult')}: grace=${result?.graceCount}, expired=${result?.expiredCount}`);
      await this.loadAll();
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Renewal failed');
    }
  }

  async cancelSub(subId: string): Promise<void> {
    try {
      await this.http.post(`/api/v1/growth/membership/subscriptions/${subId}/cancel`, {}).toPromise();
      this.notification.success(this.i18n.t('growth.cancelSubscription') + ' ✓');
      await this.loadAll();
    } catch (e: any) {
      this.notification.error(e?.error?.detail ?? 'Failed to cancel');
    }
  }

  closeLoyalty(): void { this.loyaltyDrawerOpen.set(false); }
  closePlan(): void { this.planDrawerOpen.set(false); }
  closeReferral(): void { this.referralDrawerOpen.set(false); }

  date(epoch: number): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleDateString();
  }
}
