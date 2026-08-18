import { ChangeDetectionStrategy, Component } from '@angular/core';
import { GuidedOnboardingComponent } from '../../settings/guided-onboarding.component';

@Component({
  selector: 'app-setup-readiness-page',
  standalone: true,
  imports: [GuidedOnboardingComponent],
  template: '<section class="page"><app-guided-onboarding /></section>',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SetupReadinessPage {}
