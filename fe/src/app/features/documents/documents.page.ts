import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { I18nService } from '../../core/i18n.service';
import { NotificationService } from '../../core/notification.service';
import { apiErrorMessage } from '../../core/api-error';
import { ModalDialogComponent } from '../../shared/ui/modal-dialog/modal-dialog.component';
import { DocFolder, DocSearchResult, DocTag } from './documents.models';
import { DocumentsService } from './documents.service';

@Component({
  selector: 'app-documents-page',
  imports: [ReactiveFormsModule, ModalDialogComponent],
  templateUrl: './documents.page.html',
  styleUrl: './documents.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentsPage {
  readonly i18n = inject(I18nService);
  readonly notification = inject(NotificationService);
  readonly documents = inject(DocumentsService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly searchLoading = signal(false);
  readonly submitting = signal(false);
  readonly folderDrawerOpen = signal(false);
  readonly editingFolderId = signal<string | null>(null);
  readonly tagDrawerOpen = signal(false);
  readonly editingTagId = signal<string | null>(null);
  readonly moveFolderId = signal<string | null>(null);
  readonly tagTarget = signal<DocSearchResult | null>(null);

  readonly folders = signal<DocFolder[]>([]);
  readonly tags = signal<DocTag[]>([]);
  readonly results = signal<DocSearchResult[]>([]);
  readonly query = signal('');
  readonly activeTag = signal<string | null>(null);

  readonly folderForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    parentId: new FormControl('', { nonNullable: true }),
  });

  readonly tagForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    color: new FormControl('#d4af37', { nonNullable: true }),
  });

  readonly moveForm = new FormGroup({
    parentId: new FormControl('', { nonNullable: true }),
  });

  readonly rootFolders = computed(() => this.folders().filter((folder) => !folder.parentId));
  readonly childrenOf = computed(() => (id: string) => this.folders().filter((folder) => folder.parentId === id));

  constructor() {
    void this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [folders, tags] = await Promise.all([
        firstValueFrom(this.documents.listFolders()),
        firstValueFrom(this.documents.listTags()),
      ]);
      this.folders.set(folders ?? []);
      this.tags.set(tags ?? []);
    } catch (e) {
      this.error.set(apiErrorMessage(e, this.i18n));
    } finally {
      this.loading.set(false);
    }
  }

  folderPath(id: string | null): string {
    if (!id) return '—';
    const folder = this.folders().find((item) => item.id === id);
    if (!folder) return id;
    return folder.parentId ? `${this.folderPath(folder.parentId)} / ${folder.name}` : folder.name;
  }

  tagName(id: string): string {
    return this.tags().find((tag) => tag.id === id)?.name ?? id;
  }

  // ---- Folders ----

  openNewFolder(): void {
    this.editingFolderId.set(null);
    this.folderForm.reset({ name: '', parentId: '' });
    this.folderDrawerOpen.set(true);
  }

  openRenameFolder(folder: DocFolder): void {
    this.editingFolderId.set(folder.id);
    this.folderForm.reset({ name: folder.name, parentId: '' });
    this.folderDrawerOpen.set(true);
  }

  closeFolderDrawer(): void {
    this.folderDrawerOpen.set(false);
  }

  async submitFolder(): Promise<void> {
    if (this.folderForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const value = this.folderForm.getRawValue();
    try {
      const editingId = this.editingFolderId();
      if (editingId) {
        await firstValueFrom(this.documents.renameFolder(editingId, value.name));
      } else {
        await firstValueFrom(this.documents.createFolder(value.name, value.parentId || undefined));
      }
      this.folderDrawerOpen.set(false);
      this.notification.success(this.i18n.t(editingId ? 'doc.renamed' : 'doc.folderCreated'));
      await this.load();
      await this.search();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  openMoveFolder(folder: DocFolder): void {
    this.moveFolderId.set(folder.id);
    this.moveForm.reset({ parentId: folder.parentId ?? '' });
  }

  closeMoveFolder(): void {
    this.moveFolderId.set(null);
  }

  async submitMove(): Promise<void> {
    const id = this.moveFolderId();
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    try {
      await firstValueFrom(this.documents.moveFolder(id, this.moveForm.getRawValue().parentId || undefined));
      this.moveFolderId.set(null);
      this.notification.success(this.i18n.t('doc.folderMoved'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async deleteFolder(folder: DocFolder): Promise<void> {
    try {
      await firstValueFrom(this.documents.deleteFolder(folder.id));
      this.notification.success(this.i18n.t('doc.folderDeleted'));
      await this.load();
      await this.search();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  // ---- Tags ----

  openNewTag(): void {
    this.editingTagId.set(null);
    this.tagForm.reset({ name: '', color: '#d4af37' });
    this.tagDrawerOpen.set(true);
  }

  closeTagDrawer(): void {
    this.tagDrawerOpen.set(false);
  }

  async submitTag(): Promise<void> {
    if (this.tagForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const value = this.tagForm.getRawValue();
    try {
      await firstValueFrom(this.documents.createTag(value.name, value.color));
      this.tagDrawerOpen.set(false);
      this.notification.success(this.i18n.t('doc.tagCreated'));
      await this.load();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.submitting.set(false);
    }
  }

  async deleteTag(tag: DocTag): Promise<void> {
    try {
      await firstValueFrom(this.documents.deleteTag(tag.id));
      this.notification.success(this.i18n.t('doc.tagDeleted'));
      await this.load();
      await this.search();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  // ---- Search ----

  async search(): Promise<void> {
    this.searchLoading.set(true);
    try {
      const q = this.query().trim();
      const tag = this.activeTag();
      this.results.set((await firstValueFrom(
        this.documents.searchDocuments(q, tag || undefined))) ?? []);
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    } finally {
      this.searchLoading.set(false);
    }
  }

  // ---- Tag assignment ----

  assignableTags(result: DocSearchResult): DocTag[] {
    const owned = new Set(result.tags.map((tag) => tag.id));
    return this.tags().filter((tag) => !owned.has(tag.id));
  }

  onAssignTag(result: DocSearchResult, event: Event): void {
    const index = Number((event.target as HTMLSelectElement).value);
    const tag = this.assignableTags(result)[index];
    if (tag) void this.assignTag(result, tag);
  }

  async assignTag(result: DocSearchResult, tag: DocTag): Promise<void> {
    try {
      await firstValueFrom(this.documents.assignTag(result.id, tag.id));
      this.notification.success(this.i18n.t('doc.tagAssigned'));
      await this.search();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  async removeTag(result: DocSearchResult, tag: DocTag): Promise<void> {
    try {
      await firstValueFrom(this.documents.removeTag(result.id, tag.id));
      this.notification.success(this.i18n.t('doc.tagRemoved'));
      await this.search();
    } catch (e) {
      this.notification.error(apiErrorMessage(e, this.i18n));
    }
  }

  formatDate(epoch: number): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleString(this.i18n.locale() === 'ar-EG' ? 'ar-EG' : 'en-US');
  }
}