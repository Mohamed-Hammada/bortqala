import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { GlobalLoadingService } from '../../../core/global-loading/global-loading.service';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-global-loading-dialog',
  templateUrl: './global-loading-dialog.html',
  styleUrl: './global-loading-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlobalLoadingDialogComponent {
  readonly loading = inject(GlobalLoadingService);
  readonly i18n = inject(I18nService);
}
