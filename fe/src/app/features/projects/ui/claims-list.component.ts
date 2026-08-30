import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { ClaimService } from '../data-access/claim.service';
import {
  ClaimStatus,
  ClaimType,
  CreateProgressClaimRequest,
  ProjectProgressClaim,
  UpdateProgressClaimRequest,
} from '../models/claim.models';
import { ClaimEditorModalComponent } from './claim-editor-modal.component';
import { ClaimCertificateModalComponent } from './claim-certificate-modal.component';

@Component({
  selector: 'app-claims-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    ClaimEditorModalComponent,
    ClaimCertificateModalComponent,
  ],
  templateUrl: './claims-list.component.html',
  styleUrl: './claims-list.component.scss',
})
export class ClaimsListComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly claimService = inject(ClaimService);
  private readonly notification = inject(NotificationService);
  private readonly confirm = inject(ConfirmDialogService);

  @Input() projectId = '';
  @Input() projectName = '';
  @Input() parties: Array<{ id: string; name: string }> = [];

  readonly searchTerm = signal('');
  readonly typeFilter = signal<'ALL' | ClaimType>('ALL');
  readonly statusFilter = signal<'ALL' | ClaimStatus>('ALL');

  readonly editorModalOpen = signal(false);
  readonly certificateModalOpen = signal(false);
  readonly activeClaim = signal<ProjectProgressClaim | null>(null);

  readonly filteredClaims = computed(() => {
    const list = this.claimService.claims();
    const term = this.searchTerm().trim().toLowerCase();
    const type = this.typeFilter();
    const status = this.statusFilter();

    return list.filter((c) => {
      if (type !== 'ALL' && c.claimType !== type) return false;
      if (status !== 'ALL' && c.status !== status) return false;
      if (term) {
        const numMatch = c.claimNumber.toLowerCase().includes(term);
        const partyMatch = c.partyName ? c.partyName.toLowerCase().includes(term) : false;
        if (!numMatch && !partyMatch) return false;
      }
      return true;
    });
  });

  readonly kpiTotal = computed(() => this.claimService.claims().length);
  readonly kpiOwner = computed(
    () => this.claimService.claims().filter((c) => c.claimType === 'OWNER_IPC').length
  );
  readonly kpiSubcontractor = computed(
    () => this.claimService.claims().filter((c) => c.claimType === 'SUBCONTRACTOR_IPC').length
  );
  readonly kpiTotalNetPayable = computed(() =>
    this.claimService.claims().reduce((sum, c) => sum + (c.currentNetPayableAmount || 0), 0)
  );

  ngOnInit(): void {
    if (this.projectId) {
      this.claimService.loadClaims(this.projectId).subscribe({
        error: () => this.notification.error(this.i18n.t('common.genericError')),
      });
    }
  }

  openCreate(): void {
    this.activeClaim.set(null);
    this.editorModalOpen.set(true);
  }

  openEdit(claim: ProjectProgressClaim): void {
    this.claimService.getClaim(claim.id).subscribe({
      next: (full) => {
        this.activeClaim.set(full);
        this.editorModalOpen.set(true);
      },
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  openCertificate(claim: ProjectProgressClaim): void {
    this.claimService.getClaim(claim.id).subscribe({
      next: (full) => {
        this.activeClaim.set(full);
        this.certificateModalOpen.set(true);
      },
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  handleSaveCreate(req: CreateProgressClaimRequest): void {
    this.claimService.createClaim(this.projectId, req).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('claims.createClaim'));
        this.editorModalOpen.set(false);
      },
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  handleSaveUpdate(event: { id: string; req: UpdateProgressClaimRequest }): void {
    this.claimService.updateDraftClaim(event.id, event.req).subscribe({
      next: () => {
        this.notification.success(this.i18n.t('claims.editClaim'));
        this.editorModalOpen.set(false);
      },
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  submitClaim(claim: ProjectProgressClaim): void {
    this.claimService.submitClaim(claim.id).subscribe({
      next: () => this.notification.success(this.i18n.t('claims.submitSuccess')),
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  reviewClaim(claim: ProjectProgressClaim): void {
    this.claimService.reviewClaim(claim.id).subscribe({
      next: () => this.notification.success(this.i18n.t('claims.reviewSuccess')),
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  certifyClaim(claim: ProjectProgressClaim): void {
    this.claimService.certifyClaim(claim.id, { notes: 'Certified' }).subscribe({
      next: () => this.notification.success(this.i18n.t('claims.certifySuccess')),
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  postClaimToFinance(claim: ProjectProgressClaim): void {
    this.claimService.postClaimToFinance(claim.id).subscribe({
      next: () => this.notification.success(this.i18n.t('claims.postFinanceSuccess')),
      error: () => this.notification.error(this.i18n.t('common.genericError')),
    });
  }

  async deleteClaim(claim: ProjectProgressClaim): Promise<void> {
    const confirmed = await this.confirm.confirmOptions({
      titleKey: 'claims.deleteConfirmTitle',
      messageKey: 'claims.deleteConfirmMessage',
      confirmKey: 'common.delete',
      cancelKey: 'common.cancel',
      danger: true,
    });

    if (confirmed) {
      this.claimService.deleteDraftClaim(claim.id).subscribe({
        next: () => this.notification.success(this.i18n.t('claims.deleteClaim')),
        error: () => this.notification.error(this.i18n.t('common.genericError')),
      });
    }
  }
}
