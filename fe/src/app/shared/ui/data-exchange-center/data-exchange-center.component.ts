import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../../core/i18n.service';

interface TemplateColumn {
  header: string;
  required: boolean;
  type: string;
  uniqueWithinFile: boolean;
  allowedValues: string[];
  description: string;
}

interface TemplateSheet { name: string; columns: TemplateColumn[]; }

interface TemplateSummary {
  key: string;
  module: string;
  title: string;
  workspaceRoute: string;
  description: string;
  commitSupported: boolean;
  sheets: TemplateSheet[];
}

interface CellValidationError { column: string; message: string; }
interface RowValidationResult { rowNumber: number; values: Record<string, string>; errors: CellValidationError[]; }
interface SheetValidationResult {
  sheet: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  sheetErrors: string[];
  rows: RowValidationResult[];
}
interface ValidationResult {
  templateKey: string;
  valid: boolean;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  sheets: SheetValidationResult[];
}

@Component({
  selector: 'app-data-exchange-center',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './data-exchange-center.component.html',
  styleUrl: './data-exchange-center.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataExchangeCenterComponent {
  private static readonly MAX_FILE_BYTES = 10 * 1024 * 1024;
  private readonly http = inject(HttpClient);
  readonly i18n = inject(I18nService);

  readonly templates = signal<TemplateSummary[]>([]);
  readonly selectedKey = signal('');
  readonly query = signal('');
  readonly selectedModule = signal('all');
  readonly file = signal<File | null>(null);
  readonly loadingCatalog = signal(false);
  readonly validating = signal(false);
  readonly downloading = signal(false);
  readonly error = signal('');
  readonly validation = signal<ValidationResult | null>(null);
  readonly commitStrategy = signal<'skip_invalid' | 'rollback_all'>('rollback_all');

  readonly modules = computed(() => [...new Set(this.templates().map((item) => item.module))].sort());
  readonly selected = computed(() => this.templates().find((item) => item.key === this.selectedKey()) ?? null);
  readonly filtered = computed(() => {
    this.i18n.locale();
    const module = this.selectedModule();
    const q = this.query().trim().toLocaleLowerCase(this.i18n.locale());
    return this.templates().filter((item) => {
      if (module !== 'all' && item.module !== module) return false;
      if (!q) return true;
      return [item.title, item.description, item.module, item.key, item.workspaceRoute]
        .join(' ')
        .toLocaleLowerCase(this.i18n.locale())
        .includes(q);
    });
  });

  constructor() {
    void this.loadCatalog();
  }

  async loadCatalog(): Promise<void> {
    this.loadingCatalog.set(true);
    this.error.set('');
    try {
      const result = await firstValueFrom(this.http.get<TemplateSummary[]>('/api/v1/data-exchange/catalog'));
      this.templates.set(result);
      if (!this.selectedKey() && result.length > 0) this.selectedKey.set(result[0].key);
    } catch {
      this.error.set(this.i18n.t('dataExchange.error.catalog'));
    } finally {
      this.loadingCatalog.set(false);
    }
  }

  chooseTemplate(key: string): void {
    this.selectedKey.set(key);
    this.file.set(null);
    this.validation.set(null);
    this.error.set('');
  }

  chooseFile(input: HTMLInputElement): void {
    const file = input.files?.item(0) ?? null;
    this.validation.set(null);
    this.error.set('');
    if (!file) {
      this.file.set(null);
      return;
    }
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      this.error.set(this.i18n.t('dataExchange.error.type'));
      input.value = '';
      return;
    }
    if (file.size > DataExchangeCenterComponent.MAX_FILE_BYTES) {
      this.error.set(this.i18n.t('dataExchange.error.size'));
      input.value = '';
      return;
    }
    this.file.set(file);
  }

  async downloadTemplate(sample: boolean): Promise<void> {
    const selected = this.selected();
    if (!selected) return;
    this.downloading.set(true);
    this.error.set('');
    try {
      const blob = await firstValueFrom(this.http.get(`/api/v1/data-exchange/templates/${encodeURIComponent(selected.key)}`, {
        params: { sample }, responseType: 'blob',
      }));
      this.saveBlob(blob, `${selected.key}-${sample ? 'sample' : 'blank'}.xlsx`);
    } catch {
      this.error.set(this.i18n.t('dataExchange.error.download'));
    } finally {
      this.downloading.set(false);
    }
  }

  async validateUpload(): Promise<void> {
    const selected = this.selected();
    const file = this.file();
    if (!selected || !file) return;
    this.validating.set(true);
    this.error.set('');
    this.validation.set(null);
    try {
      const body = new FormData();
      body.append('file', file);
      const result = await firstValueFrom(this.http.post<ValidationResult>(
        `/api/v1/data-exchange/validate/${encodeURIComponent(selected.key)}`, body));
      this.validation.set(result);
    } catch {
      this.error.set(this.i18n.t('dataExchange.error.validate'));
    } finally {
      this.validating.set(false);
    }
  }

  async downloadErrors(): Promise<void> {
    const selected = this.selected();
    const file = this.file();
    if (!selected || !file || !this.validation() || this.validation()!.invalidRows === 0) return;
    this.downloading.set(true);
    try {
      const body = new FormData();
      body.append('file', file);
      const blob = await firstValueFrom(this.http.post(
        `/api/v1/data-exchange/error-workbook/${encodeURIComponent(selected.key)}`, body, { responseType: 'blob' }));
      this.saveBlob(blob, `${selected.key}-errors.xlsx`);
    } catch {
      this.error.set(this.i18n.t('dataExchange.error.errorWorkbook'));
    } finally {
      this.downloading.set(false);
    }
  }

  setStrategy(value: string): void {
    this.commitStrategy.set(value === 'skip_invalid' ? 'skip_invalid' : 'rollback_all');
  }

  invalidRows(sheet: SheetValidationResult): RowValidationResult[] {
    return sheet.rows.filter((row) => row.errors.length > 0);
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
