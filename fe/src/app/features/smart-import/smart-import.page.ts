import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { SmartImportCellError, SmartImportPreviewRow, SmartImportWorkflow } from './smart-import.models';
import { SmartImportStore } from './smart-import.store';

@Component({
  selector: 'app-smart-import',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  providers: [SmartImportStore],
  templateUrl: './smart-import.page.html',
  styleUrl: './smart-import.page.scss',
})
export class SmartImportPage implements OnInit, OnDestroy {
  readonly store = inject(SmartImportStore);
  readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private routeSub?: Subscription;

  readonly workflowKey = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);
  readonly activeSheet = signal<string | null>(null);
  readonly skipInvalid = signal(false);
  readonly sampleTemplate = signal(true);
  readonly dragOver = signal(false);

  readonly workflow = computed(() => this.store.workflows().find((item) => item.key === this.workflowKey()) ?? null);
  readonly previewRows = computed(() => {
    const preview = this.store.preview();
    const sheet = this.activeSheet();
    return preview ? preview.rows.filter((row) => !sheet || row.sheet === sheet) : [];
  });
  readonly currentSheet = computed(() => {
    const workflow = this.workflow();
    const key = this.activeSheet();
    return workflow?.sheets.find((sheet) => sheet.key === key) ?? workflow?.sheets[0] ?? null;
  });
  readonly step = computed(() => {
    if (this.store.result()) return 5;
    if (this.store.preview()) return 3;
    if (this.selectedFile()) return 2;
    return 1;
  });
  readonly rtl = computed(() => this.i18n.locale().toLowerCase().startsWith('ar'));

  async ngOnInit(): Promise<void> {
    await this.store.loadWorkflows();
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const key = params.get('workflow');
      this.workflowKey.set(key);
      this.store.reset();
      this.selectedFile.set(null);
      const workflow = this.store.workflows().find((item) => item.key === key);
      this.activeSheet.set(workflow?.sheets[0]?.key ?? null);
    });
  }

  ngOnDestroy(): void { this.routeSub?.unsubscribe(); }

  chooseWorkflow(workflow: SmartImportWorkflow): void {
    void this.router.navigate(['/smart-import', workflow.key]);
  }

  onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file) this.setFile(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0] ?? null;
    if (file) this.setFile(file);
  }

  setFile(file: File): void {
    const lower = file.name.toLowerCase();
    if (!lower.endsWith('.xlsx') && !lower.endsWith('.xls') && !lower.endsWith('.csv')) return;
    this.selectedFile.set(file);
    this.store.reset();
  }

  async preview(): Promise<void> {
    const workflow = this.workflow();
    const file = this.selectedFile();
    if (!workflow || !file) return;
    const preview = await this.store.previewFile(workflow.key, file);
    this.activeSheet.set(preview?.workflow.sheets[0]?.key ?? null);
  }

  async commit(): Promise<void> {
    const workflow = this.workflow();
    const preview = this.store.preview();
    if (!workflow || !preview) return;
    await this.store.commit(workflow.key, this.skipInvalid(), preview.rows);
  }

  cellErrors(row: SmartImportPreviewRow, column: string): SmartImportCellError[] {
    return row.errors.filter((error) => error.column === column);
  }

  rowHasErrors(row: SmartImportPreviewRow): boolean { return row.errors.length > 0; }

  clearFile(): void {
    this.selectedFile.set(null);
    this.store.reset();
  }
}
