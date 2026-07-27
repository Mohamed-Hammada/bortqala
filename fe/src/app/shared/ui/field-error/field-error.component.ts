import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-field-error',
  standalone: true,
  template: `
    @if (control() && control()?.invalid && (control()?.touched || control()?.dirty)) {
      <span class="field-error-message">
        @if (control()?.errors?.['required']) {
          ⚠️ هذا الحقل مطلوب ولا يمكن تركه فارغاً.
        } @else if (control()?.errors?.['minlength']) {
          ⚠️ يجب أن يحتوي الحقل على {{ control()?.errors?.['minlength']?.requiredLength }} أحرف على الأقل.
        } @else if (control()?.errors?.['maxlength']) {
          ⚠️ تجاوزت الحد الأقصى للمسموح ({{ control()?.errors?.['maxlength']?.requiredLength }} حرفاً).
        } @else if (control()?.errors?.['min']) {
          ⚠️ القيمة أدنى من الحد المسموح بها (أصغر قيمة {{ control()?.errors?.['min']?.min }}).
        } @else if (control()?.errors?.['email']) {
          ⚠️ صيغة البريد الإلكتروني غير صحيحة.
        } @else {
          ⚠️ مدخلات غير صحيحة، يرجى التعديل.
        }
      </span>
    }
  `,
  styles: [`
    .field-error-message {
      display: inline-block;
      margin-top: 4px;
      font-size: 12px;
      font-weight: 600;
      color: #ef4444;
      animation: fadeIn 0.2s ease-in-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-2px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldErrorComponent {
  readonly control = input<AbstractControl | null>(null);
}
