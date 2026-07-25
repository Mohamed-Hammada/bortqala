import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NotificationService, ToastMessage } from '../../../core/notification.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  templateUrl: './toast-container.component.html',
  styleUrl: './toast-container.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainerComponent {
  readonly notificationService = inject(NotificationService);

  iconFor(type: string): string {
    switch (type) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '⚠️';
      default:
        return 'ℹ';
    }
  }

  dismiss(toast: ToastMessage): void {
    this.notificationService.remove(toast.id);
  }
}
