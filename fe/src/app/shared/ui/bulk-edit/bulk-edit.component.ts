import { ChangeDetectionStrategy, Component, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { apiErrorMessage } from '../../../core/api-error';
import { BulkUpdateRequest, BulkUpdateResultItem } from '../../../features/settings/integrations.models';

export interface BulkFieldOption {
  field: string;
  labelKey: string;
  options: { value: string; labelKey: string }[];
}

@Component({
  selector: 'app-bulk-edit',
  standalone: true,
  template: `
    @if (selectedIds.length > 0) {
      <div class="bulk-edit-bar">
        <span class="bulk-count">{{ i18n.t('bulkEdit.selected', { count: selectedIds.length }) }}</span>
        <select (change)="onFieldChange($event)">
          <option value="">{{ i18n.t('common.select') }}</option>
          @for (field of fields; track field.field) {
            <option [value]="field.field">{{ i18n.t(field.labelKey) }}</option>
          }
        </select>
        @if (selectedField()) {
          <select (change)="onValueChange($event)">
            <option value="">{{ i18n.t('common.select') }}</option>
            @for (opt of fieldOptions(); track opt.value) {
              <option [value]="opt.value">{{ i18n.t(opt.labelKey) }}</option>
            }
          </select>
          <button class="btn btn-primary btn-sm" (click)="confirmBulk()" [disabled]="!selectedValue()">
            {{ i18n.t('common.confirm') }}
          </button>
        }
        <button class="btn btn-ghost btn-sm" (click)="clearSelection()">
          {{ i18n.t('common.cancel') }}
        </button>
      </div>
    }

    @if (showConfirm()) {
      <div class="overlay" (click)="cancelConfirm()">
        <div class="confirm-card" (click)="$event.stopPropagation()">
          <h3>{{ i18n.t('bulkEdit.confirmTitle') }}</h3>
          <div class="confirm-distribution">
            <div class="dist-header">
              <span>{{ i18n.t('bulkEdit.before') }}</span>
              <span>→</span>
              <span>{{ i18n.t('bulkEdit.after') }}</span>
            </div>
            @for (item of distribution(); track item.from) {
              <div class="dist-row">
                <span>{{ item.fromLabel }} ({{ item.count }})</span>
                <span>→</span>
                <span>{{ item.toLabel }}</span>
              </div>
            }
          </div>
          <div class="confirm-actions">
            <button class="btn btn-primary" (click)="executeBulk()" [disabled]="executing()">
              {{ executing() ? i18n.t('common.processing') : i18n.t('common.confirm') }}
            </button>
            <button class="btn btn-secondary" (click)="cancelConfirm()">{{ i18n.t('common.cancel') }}</button>
          </div>
          @if (bulkResults()) {
            <div class="results-summary">
              <span class="success-count">{{ i18n.t('bulkEdit.succeeded') }}: {{ resultsSummary().succeeded }}</span>
              @if (resultsSummary().failed > 0) {
                <span class="fail-count">{{ i18n.t('bulkEdit.failed') }}: {{ resultsSummary().failed }}</span>
              }
            </div>
          }
        </div>
      </div>
    }
  `,
  styleUrl: './bulk-edit.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BulkEditComponent {
  @Input() entityType = '';
  @Input() fields: BulkFieldOption[] = [];
  @Input() selectedIds: string[] = [];
  @Input() getDistribution: (field: string, value: string) => { from: string; fromLabel: string; toLabel: string; count: number }[] = () => [];
  @Output() selectionCleared = new EventEmitter<void>();
  @Output() bulkComplete = new EventEmitter<BulkUpdateResultItem[]>();

  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);
  private readonly notification = inject(NotificationService);

  readonly selectedField = signal('');
  readonly selectedValue = signal('');
  readonly showConfirm = signal(false);
  readonly executing = signal(false);
  readonly bulkResults = signal<BulkUpdateResultItem[] | null>(null);

  readonly fieldOptions = signal<{ value: string; labelKey: string }[]>([]);

  readonly distribution = signal<{ from: string; fromLabel: string; toLabel: string; count: number }[]>([]);

  readonly resultsSummary = signal({ succeeded: 0, failed: 0 });

  onFieldChange(event: Event) {
    const field = (event.target as HTMLSelectElement).value;
    this.selectedField.set(field);
    this.selectedValue.set('');
    const opt = this.fields.find(f => f.field === field);
    this.fieldOptions.set(opt?.options ?? []);
  }

  onValueChange(event: Event) {
    this.selectedValue.set((event.target as HTMLSelectElement).value);
  }

  confirmBulk() {
    const field = this.selectedField();
    const value = this.selectedValue();
    if (!field || !value) return;
    this.distribution.set(this.getDistribution(field, value));
    this.showConfirm.set(true);
  }

  cancelConfirm() {
    this.showConfirm.set(false);
    this.bulkResults.set(null);
  }

  clearSelection() {
    this.selectedField.set('');
    this.selectedValue.set('');
    this.selectionCleared.emit();
  }

  async executeBulk() {
    const field = this.selectedField();
    const value = this.selectedValue();
    if (!field || !value) return;

    this.executing.set(true);
    try {
      const result = await import('rxjs').then(rx =>
        rx.firstValueFrom(
          this.http.post<{ results: BulkUpdateResultItem[] }>('/api/v1/bulk-update', {
            entityType: this.entityType,
            field,
            value,
            ids: this.selectedIds,
          } as BulkUpdateRequest),
        ),
      );
      this.bulkResults.set(result.results);
      const succeeded = result.results.filter(r => r.success).length;
      const failed = result.results.filter(r => !r.success).length;
      this.resultsSummary.set({ succeeded, failed });
      this.bulkComplete.emit(result.results);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.executing.set(false);
    }
  }
}
