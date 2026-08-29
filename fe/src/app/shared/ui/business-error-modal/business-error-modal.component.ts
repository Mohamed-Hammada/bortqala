import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../../core/i18n.service';

export interface BusinessErrorDetail {
  code: string;
  itemIdentifier?: string;
  currentValue?: string | number;
  allowedLimit?: string | number;
  messageAr: string;
  messageEn: string;
  actionHintAr?: string;
  actionHintEn?: string;
}

@Component({
  selector: 'app-business-error-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (open) {
      <div class="modal-backdrop" (click)="close()">
        <div class="modal-dialog" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
          <div class="modal-header">
            <div class="header-icon">⚠️</div>
            <h3 class="header-title">{{ i18n.locale() === 'ar-EG' ? titleAr : titleEn }}</h3>
            <button type="button" class="close-btn" (click)="close()">✕</button>
          </div>

          <div class="modal-body">
            <p class="summary-text">{{ i18n.locale() === 'ar-EG' ? summaryAr : summaryEn }}</p>

            @if (errors && errors.length > 0) {
              <div class="error-table-container">
                <table class="error-table">
                  <thead>
                    <tr>
                      <th>{{ i18n.locale() === 'ar-EG' ? 'رمز البند / المرجع' : 'Item / Ref' }}</th>
                      <th>{{ i18n.locale() === 'ar-EG' ? 'القيمة الحالية' : 'Current Value' }}</th>
                      <th>{{ i18n.locale() === 'ar-EG' ? 'الحد المسموح' : 'Allowed Limit' }}</th>
                      <th>{{ i18n.locale() === 'ar-EG' ? 'سبب الرفض' : 'Reason' }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (err of errors; track err.code) {
                      <tr>
                        <td class="font-mono">{{ err.itemIdentifier || err.code }}</td>
                        <td>{{ err.currentValue || '—' }}</td>
                        <td>{{ err.allowedLimit || '—' }}</td>
                        <td>{{ i18n.locale() === 'ar-EG' ? err.messageAr : err.messageEn }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-primary" (click)="close()">
              {{ i18n.locale() === 'ar-EG' ? 'فهمت، سأقوم بالتعديل' : 'Understood, I will adjust' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
      padding: 1rem;
    }
    .modal-dialog {
      background: var(--surface-primary, #ffffff);
      border-radius: 12px;
      max-width: 650px;
      width: 100%;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .modal-header {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid var(--border-color, #e2e8f0);
      background: #fef2f2;
    }
    .header-icon {
      font-size: 1.5rem;
    }
    .header-title {
      flex: 1;
      margin: 0;
      font-size: 1.15rem;
      font-weight: 600;
      color: #991b1b;
    }
    .close-btn {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: #991b1b;
    }
    .modal-body {
      padding: 1.5rem;
    }
    .summary-text {
      margin-top: 0;
      font-size: 0.95rem;
      color: var(--text-color, #334155);
      line-height: 1.5;
    }
    .error-table-container {
      margin-top: 1rem;
      border: 1px solid var(--border-color, #e2e8f0);
      border-radius: 8px;
      overflow: hidden;
    }
    .error-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.85rem;
    }
    .error-table th {
      background: var(--surface-secondary, #f8fafc);
      padding: 0.6rem 0.75rem;
      text-align: start;
      font-weight: 600;
      border-bottom: 1px solid var(--border-color, #e2e8f0);
    }
    .error-table td {
      padding: 0.6rem 0.75rem;
      border-bottom: 1px solid var(--border-color, #e2e8f0);
    }
    .font-mono {
      font-family: monospace;
      font-weight: 600;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      padding: 1rem 1.5rem;
      border-top: 1px solid var(--border-color, #e2e8f0);
      background: var(--surface-secondary, #f8fafc);
    }
    .btn {
      padding: 0.5rem 1.25rem;
      border-radius: 6px;
      font-weight: 500;
      font-size: 0.9rem;
      cursor: pointer;
      border: none;
    }
    .btn-primary {
      background: #dc2626;
      color: #ffffff;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BusinessErrorModalComponent {
  readonly i18n = inject(I18nService);

  @Input() open = false;
  @Input() titleAr = 'تعذر إتمام المعاملة المالية';
  @Input() titleEn = 'Operation Could Not Be Completed';
  @Input() summaryAr = 'تحتوي المعاملة على قيود أو بنود تتجاوز الحدود المعتمدة. يرجى مراجعة التفاصيل أدناه:';
  @Input() summaryEn = 'The transaction contains rules or items exceeding approved limits. Please review below:';
  @Input() errors: BusinessErrorDetail[] = [];

  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }
}
