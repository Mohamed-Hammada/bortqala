import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  templateUrl: './skeleton.component.html',
  styleUrl: './skeleton.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SkeletonComponent {
  readonly loading = input<boolean>(true);
  readonly error = input<string | null>(null);
  readonly rowCount = input<number>(5);
  readonly retryText = input<string>('إعادة المحاولة');
  readonly errorMessage = input<string>('تعذر تحميل البيانات. يرجى التأكد من الاتصال بالشبكة والمحاولة مرة أخرى.');

  readonly retry = output<void>();

  get skeletonRows(): number[] {
    return Array.from({ length: this.rowCount() }, (_, i) => i);
  }

  onRetry(): void {
    this.retry.emit();
  }
}
