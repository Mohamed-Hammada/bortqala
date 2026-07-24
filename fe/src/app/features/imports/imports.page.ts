import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { formatDateTime } from '../../core/date';
import { ImportsStore } from './imports.store';
@Component({
  selector: 'app-imports-page',
  imports: [RouterLink],
  providers: [ImportsStore],
  templateUrl: './imports.page.html',
  styleUrl: './imports.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsPage {
  readonly store = inject(ImportsStore);
  readonly file = signal<File | null>(null);
  readonly deviceName = signal('جهاز الحضور الرئيسي');
  readonly expanded = signal<string | null>(null);
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
