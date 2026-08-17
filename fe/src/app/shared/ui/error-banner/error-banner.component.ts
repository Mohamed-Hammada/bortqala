import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-banner',
  standalone: true,
  templateUrl: './error-banner.component.html',
  styleUrls: ['./error-banner.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorBannerComponent {
  readonly errorMessage = input<string | null>(null);
  readonly retry = output<void>();
}
