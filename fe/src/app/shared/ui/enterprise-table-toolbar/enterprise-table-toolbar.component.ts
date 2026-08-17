import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';

export interface EnterpriseTableColumn { key: string; label: string; visible?: boolean; }
export interface EnterpriseBulkAction { key: string; label: string; disabled?: boolean; }
export interface SavedFilterPreset { name: string; state: Record<string, unknown>; }

@Component({
  selector: 'app-enterprise-table-toolbar',
  standalone: true,
  templateUrl: './enterprise-table-toolbar.component.html',
  styleUrl: './enterprise-table-toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnterpriseTableToolbarComponent {
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) preferenceKey = '';
  @Input() filename = 'export.xlsx';
  @Input() sheetName = 'Export';
  @Input() columns: EnterpriseTableColumn[] = [];
  @Input() currentRows: Record<string, unknown>[] = [];
  @Input() allRows: Record<string, unknown>[] | null = null;
  @Input() selectedRows: Record<string, unknown>[] = [];
  @Input() summaryRows: Record<string, unknown>[] = [];
  @Input() filterState: Record<string, unknown> = {};
  @Input() bulkActions: EnterpriseBulkAction[] = [];
  @Output() presetApplied = new EventEmitter<Record<string, unknown>>();
  @Output() bulkActionRequested = new EventEmitter<string>();

  readonly exporting = signal(false);
  readonly openPanel = signal<'none' | 'export' | 'columns' | 'presets'>('none');
  readonly hiddenKeys = signal<Set<string>>(new Set());
  readonly presets = signal<SavedFilterPreset[]>([]);
  readonly presetName = signal('');
  readonly exportError = signal('');
  readonly selectedCount = computed(() => this.selectedRows.length);

  ngOnInit(): void { this.restorePreferences(); }

  togglePanel(panel: 'export' | 'columns' | 'presets'): void {
    this.openPanel.update((current) => current === panel ? 'none' : panel);
  }

  visible(column: EnterpriseTableColumn): boolean {
    return column.visible !== false && !this.hiddenKeys().has(column.key);
  }

  toggleColumn(key: string): void {
    this.hiddenKeys.update((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
    this.persistHiddenColumns();
  }

  async exportRows(kind: 'current' | 'all' | 'selected' | 'summary'): Promise<void> {
    const rows = kind === 'current' ? this.currentRows
      : kind === 'all' ? this.allRows
      : kind === 'selected' ? this.selectedRows
      : this.summaryRows;
    if (rows == null) {
      this.exportError.set(this.i18n.t('enterpriseTable.exportAllUnavailable'));
      return;
    }
    this.exporting.set(true);
    this.exportError.set('');
    try {
      const columns = this.columns.filter((column) => this.visible(column)).map((column) => ({ key: column.key, header: column.label }));
      const blob = await firstValueFrom(this.http.post('/api/v1/data-exchange/export', {
        filename: this.filename, sheetName: this.sheetName, columns, rows,
      }, { responseType: 'blob' }));
      this.saveBlob(blob, this.filename.toLowerCase().endsWith('.xlsx') ? this.filename : `${this.filename}.xlsx`);
      this.openPanel.set('none');
    } catch {
      this.exportError.set(this.i18n.t('enterpriseTable.exportFailed'));
    } finally {
      this.exporting.set(false);
    }
  }

  savePreset(): void {
    const name = this.presetName().trim();
    if (!name || !this.preferenceKey) return;
    const next = [...this.presets().filter((item) => item.name !== name), { name, state: structuredClone(this.filterState) }];
    this.presets.set(next);
    localStorage.setItem(this.storageKey('filters'), JSON.stringify(next));
    this.presetName.set('');
  }

  deletePreset(name: string): void {
    const next = this.presets().filter((item) => item.name !== name);
    this.presets.set(next);
    localStorage.setItem(this.storageKey('filters'), JSON.stringify(next));
  }

  applyPreset(preset: SavedFilterPreset): void { this.presetApplied.emit(structuredClone(preset.state)); }
  requestBulkAction(action: EnterpriseBulkAction): void { if (!action.disabled) this.bulkActionRequested.emit(action.key); }

  private restorePreferences(): void {
    if (!this.preferenceKey) return;
    try {
      const hidden = JSON.parse(localStorage.getItem(this.storageKey('columns')) ?? '[]') as string[];
      this.hiddenKeys.set(new Set(hidden));
      const presets = JSON.parse(localStorage.getItem(this.storageKey('filters')) ?? '[]') as SavedFilterPreset[];
      this.presets.set(Array.isArray(presets) ? presets : []);
    } catch {
      this.hiddenKeys.set(new Set());
      this.presets.set([]);
    }
  }

  private persistHiddenColumns(): void {
    if (this.preferenceKey) localStorage.setItem(this.storageKey('columns'), JSON.stringify([...this.hiddenKeys()]));
  }

  private storageKey(kind: string): string { return `bemo.table.${this.preferenceKey}.${kind}.v1`; }
  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = filename; anchor.click(); URL.revokeObjectURL(url);
  }
}
