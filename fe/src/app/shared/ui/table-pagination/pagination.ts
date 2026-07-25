import { signal } from '@angular/core';

export class TablePagination {
  readonly page = signal(1);
  readonly pageSize = signal(25);

  constructor(initialSize = 25) {
    if ([10, 25, 50, 100].includes(initialSize)) {
      this.pageSize.set(initialSize);
    }
  }

  slice<T>(items: readonly T[]): T[] {
    const page = this.currentPage(items.length);
    const start = (page - 1) * this.pageSize();
    return items.slice(start, start + this.pageSize());
  }

  currentPage(total: number): number {
    return Math.min(this.page(), this.totalPages(total));
  }

  totalPages(total: number): number {
    return Math.max(1, Math.ceil(total / this.pageSize()));
  }

  changePage(page: number, total: number): void {
    this.page.set(Math.max(1, Math.min(page, this.totalPages(total))));
  }

  changePageSize(pageSize: number): void {
    if ([10, 25, 50, 100].includes(pageSize)) {
      this.pageSize.set(pageSize);
      this.page.set(1);
    }
  }
}
