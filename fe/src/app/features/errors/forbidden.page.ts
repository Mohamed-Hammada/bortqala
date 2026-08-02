import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-forbidden-page',
  templateUrl: './forbidden.page.html',
  styleUrl: './forbidden.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForbiddenPage {
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  goHome(): void {
    void this.router.navigate(['/dashboard']);
  }
}
