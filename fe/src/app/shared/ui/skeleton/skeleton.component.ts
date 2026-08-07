import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
  styleUrl: './skeleton.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonComponent {
  readonly i18n = inject(I18nService);

  readonly loading = input<boolean>(true);
  readonly error = input<string | null>(null);
  readonly rowCount = input<number>(5);
  readonly retryText = input<string>(this.i18n.t('skeleton.retryText'));
  readonly errorMessage = input<string>(this.i18n.t('skeleton.errorMessage'));

  readonly retry = output<void>();

  get skeletonRows(): number[] {
    return Array.from({ length: this.rowCount() }, (_, i) => i);
  }

  onRetry(): void {
    this.retry.emit();
  }
}
