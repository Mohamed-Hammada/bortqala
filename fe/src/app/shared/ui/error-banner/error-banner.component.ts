import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-error-banner',
  standalone: true,
  templateUrl: './error-banner.component.html',
  styleUrls: ['./error-banner.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorBannerComponent {
  readonly i18n = inject(I18nService);
  readonly errorMessage = input<string | null>(null);
  readonly retry = output<void>();
}
