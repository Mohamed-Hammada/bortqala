import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-not-found-page',
  templateUrl: './not-found.page.html',
  styleUrl: './not-found.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPage {
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  goHome(): void {
    void this.router.navigate(['/dashboard']);
  }
}
