import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDateTime } from '../../core/date';
import { ImportsStore } from './imports.store';
import { I18nService } from '../../core/i18n.service';
import { TablePagination } from '../../shared/ui/table-pagination/pagination';
import { TablePaginationComponent } from '../../shared/ui/table-pagination/table-pagination.component';
@Component({
  selector: 'app-imports-page',
  imports: [RouterLink, TablePaginationComponent],
  providers: [ImportsStore],
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsPage {
  readonly store = inject(ImportsStore);
  readonly i18n = inject(I18nService);
  readonly file = signal<File | null>(null);
  readonly deviceName = signal(this.i18n.t('imports.defaultDevice'));
  readonly expanded = signal<string | null>(null);
  readonly pagination = new TablePagination();
  readonly pagedUnmatched = computed(() => this.pagination.slice(this.store.unmatched()));
  constructor() {
    void this.store.load();
  }
  choose(event: Event) {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.item(0) ?? null);
  }
  async upload() {
    const file = this.file();
    if (file && this.deviceName().trim())
      if (await this.store.upload(file, this.deviceName())) this.file.set(null);
  }
  dateTime(value: number) {
    return formatDateTime(value);
  }
}
