import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../../core/i18n.service';

@Component({
  selector: 'app-table-pagination',
  templateUrl: './table-pagination.component.html',
  styleUrl: './table-pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TablePaginationComponent {
  readonly total = input.required<number>();
  readonly page = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly pageChange = output<number>();
  readonly pageSizeChange = output<number>();
  readonly i18n = inject(I18nService);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.pageSize())));
  readonly from = computed(() => (this.total() ? (this.page() - 1) * this.pageSize() + 1 : 0));
  readonly to = computed(() => Math.min(this.total(), this.page() * this.pageSize()));

  selectPageSize(event: Event): void {
    this.pageSizeChange.emit(Number((event.target as HTMLSelectElement).value));
  }
}
