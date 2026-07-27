import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-banner',
  standalone: true,
  template: `
    @if (errorMessage()) {
      <div class="error-banner">
        <div class="error-content">
          <span class="error-icon">🚨</span>
          <div class="error-text">
            <strong>حدث خطأ في العملية:</strong>
            <p>{{ errorMessage() }}</p>
          </div>
        </div>
        <button class="button retry-btn" type="button" (click)="retry.emit()">
          🔄 إعادة المحاولة (Retry)
        </button>
      </div>
    }
  `,
  styles: [`
    .error-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 18px;
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.35);
      border-radius: 8px;
      margin-bottom: 16px;
    }
    .error-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .error-icon { font-size: 20px; }
    .error-text p { margin: 2px 0 0; font-size: 13px; color: var(--text-color); }
    .retry-btn {
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.4);
      font-size: 13px;
      white-space: nowrap;
      padding: 6px 12px;
      cursor: pointer;
    }
    .retry-btn:hover { background: rgba(239, 68, 68, 0.3); }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorBannerComponent {
  readonly errorMessage = input<string | null>(null);
  readonly retry = output<void>();
}
