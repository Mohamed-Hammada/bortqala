import { Component, Input, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { TenderService } from '../data-access/tender.service';
import {
  ProjectTender,
  CreateTenderRequest,
  UpdateTenderRequest,
  TenderType,
  TenderStatus
} from '../models/tender.models';
import { TenderEditorModalComponent } from './tender-editor-modal.component';
import { TenderDetailModalComponent } from './tender-detail-modal.component';

@Component({
  selector: 'app-tenders-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TenderEditorModalComponent,
    TenderDetailModalComponent
  ],
  templateUrl: './tenders-list.component.html',
  styleUrls: ['./tenders-list.component.scss']
})
export class TendersListComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notify = inject(NotificationService);
  readonly tenderService = inject(TenderService);

  @Input() projectId?: string | null = null;

  searchTerm = signal<string>('');
  filterType = signal<string>('ALL');
  filterStatus = signal<string>('ALL');

  showEditorModal = signal<boolean>(false);
  editingTender = signal<ProjectTender | null>(null);
  isSavingTender = signal<boolean>(false);

  showDetailModal = signal<boolean>(false);
  selectedTenderId = signal<string | null>(null);

  showDeleteModal = signal<boolean>(false);
  deletingTenderId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadTenders();
  }

  loadTenders(): void {
    this.tenderService.loadTenders().subscribe();
  }

  filteredTenders = computed(() => {
    let list = this.tenderService.tenders();
    if (this.projectId) {
      list = list.filter(t => t.projectId === this.projectId);
    }
    const search = this.searchTerm().trim().toLowerCase();
    if (search) {
      list = list.filter(t =>
        t.title.toLowerCase().includes(search) ||
        t.tenderNumber.toLowerCase().includes(search) ||
        (t.titleEn && t.titleEn.toLowerCase().includes(search))
      );
    }
    const type = this.filterType();
    if (type !== 'ALL') {
      list = list.filter(t => t.tenderType === type);
    }
    const status = this.filterStatus();
    if (status !== 'ALL') {
      list = list.filter(t => t.status === status);
    }
    return list;
  });

  kpiTotal = computed(() => this.filteredTenders().length);
  kpiExternal = computed(() => this.filteredTenders().filter(t => t.tenderType === 'EXTERNAL').length);
  kpiInternal = computed(() => this.filteredTenders().filter(t => t.tenderType === 'INTERNAL').length);
  kpiAwarded = computed(() => this.filteredTenders().filter(t => t.status === 'AWARDED').length);
  kpiTotalValue = computed(() =>
    this.filteredTenders().reduce((acc, t) => acc + (t.estimatedValue || 0), 0)
  );

  formatDate(epoch?: number | null): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleDateString();
  }

  onOpenCreate(): void {
    this.editingTender.set(null);
    this.showEditorModal.set(true);
  }

  onOpenEdit(t: ProjectTender): void {
    this.editingTender.set(t);
    this.showEditorModal.set(true);
  }

  onOpenDetail(t: ProjectTender): void {
    this.selectedTenderId.set(t.id);
    this.showDetailModal.set(true);
  }

  onSaveTender(req: CreateTenderRequest | UpdateTenderRequest): void {
    this.isSavingTender.set(true);
    const editing = this.editingTender();

    if (editing) {
      this.tenderService.updateTender(editing.id, req as UpdateTenderRequest).subscribe({
        next: () => {
          this.notify.success(this.i18n.t('tenders.tenderUpdatedSuccess'));
          this.isSavingTender.set(false);
          this.showEditorModal.set(false);
          this.loadTenders();
        },
        error: () => {
          this.notify.error(this.i18n.t('common.genericError'));
          this.isSavingTender.set(false);
        }
      });
    } else {
      this.tenderService.createTender(req as CreateTenderRequest).subscribe({
        next: () => {
          this.notify.success(this.i18n.t('tenders.tenderCreatedSuccess'));
          this.isSavingTender.set(false);
          this.showEditorModal.set(false);
          this.loadTenders();
        },
        error: () => {
          this.notify.error(this.i18n.t('common.genericError'));
          this.isSavingTender.set(false);
        }
      });
    }
  }

  onPromptDelete(id: string): void {
    this.deletingTenderId.set(id);
    this.showDeleteModal.set(true);
  }

  onConfirmDelete(): void {
    const id = this.deletingTenderId();
    if (!id) return;

    this.tenderService.deleteTender(id).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderDeletedSuccess'));
        this.showDeleteModal.set(false);
        this.loadTenders();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }
}
