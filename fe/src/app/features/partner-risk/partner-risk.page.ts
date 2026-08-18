import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PartnerRiskSettingsComponent } from '../settings/partner-risk-settings.component';

@Component({
  selector: 'app-partner-risk-page',
  standalone: true,
  imports: [PartnerRiskSettingsComponent],
  template: '<section class="page"><app-partner-risk-settings /></section>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerRiskPage {}
