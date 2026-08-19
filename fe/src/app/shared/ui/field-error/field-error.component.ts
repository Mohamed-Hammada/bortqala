import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-field-error',
  standalone: true,
  templateUrl: './field-error.component.html',
  styleUrls: ['./field-error.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldErrorComponent {
  readonly i18n = inject(I18nService);
  readonly control = input<AbstractControl | null>(null);
}
