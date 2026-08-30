import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';
import { SubscriptionSettingsComponent } from '../settings/subscription-settings.component';
import { TrialDemoSettingsComponent } from '../settings/trial-demo-settings.component';
import { IndustryPackSettingsComponent } from '../settings/industry-pack-settings.component';
import { EntitlementSettingsComponent } from '../settings/entitlement-settings.component';
import { TranslationManagementComponent } from '../settings/translation-management.component';
import { PlatformDiagnosticsComponent } from './platform-diagnostics.component';
import { PlatformBackupsComponent } from './platform-backups.component';
import { PlatformLicensingComponent } from './platform-licensing.component';

type PlatformAdminTab =
  | 'subscription'
  | 'trial'
  | 'industry'
  | 'entitlements'
  | 'translations'
  | 'diagnostics'
  | 'backups'
  | 'licensing';

const PLATFORM_TABS: readonly PlatformAdminTab[] = [
  'subscription',
  'trial',
  'industry',
  'entitlements',
  'translations',
  'diagnostics',
  'backups',
  'licensing',
];

@Component({
  selector: 'app-platform-admin-page',
  standalone: true,
  imports: [
    SubscriptionSettingsComponent,
    TrialDemoSettingsComponent,
    IndustryPackSettingsComponent,
    EntitlementSettingsComponent,
    TranslationManagementComponent,
    PlatformDiagnosticsComponent,
    PlatformBackupsComponent,
    PlatformLicensingComponent,
  ],
  templateUrl: './platform-admin.page.html',
  styleUrl: './platform-admin.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformAdminPage {
  readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly activeTab = signal<PlatformAdminTab>('subscription');

  constructor() {
    const requested = this.route.snapshot.queryParamMap.get('tab');
    if (requested && PLATFORM_TABS.includes(requested as PlatformAdminTab)) {
      this.activeTab.set(requested as PlatformAdminTab);
    }
  }

  setTab(tab: PlatformAdminTab): void {
    this.activeTab.set(tab);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
