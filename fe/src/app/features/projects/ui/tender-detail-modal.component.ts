import { Component, EventEmitter, Input, Output, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { TenderService } from '../data-access/tender.service';
import {
  ProjectTender,
  TenderBoqItem,
  TenderBidder,
  TenderClarification,
  CreateBoqItemRequest,
  InviteBidderRequest,
  SubmitBidRequest,
  BidLineSubmission,
  RecordBidBondRequest,
  TechnicalEvaluationRequest,
  CreateClarificationRequest,
  AnswerClarificationRequest,
  AwardTenderRequest
} from '../models/tender.models';

@Component({
  selector: 'app-tender-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tender-detail-modal.component.html',
  styleUrls: ['./tender-detail-modal.component.scss']
})
export class TenderDetailModalComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly notify = inject(NotificationService);
  readonly tenderService = inject(TenderService);

  @Input() tenderId!: string;
  @Output() close = new EventEmitter<void>();
  @Output() refreshList = new EventEmitter<void>();

  activeTab = signal<'overview' | 'boq' | 'bidders' | 'evaluation' | 'clarifications' | 'award'>('overview');

  // Sub-modal / form state
  showBoqModal = signal<boolean>(false);
  editingBoqItem = signal<TenderBoqItem | null>(null);
  boqItemCode = '';
  boqDescription = '';
  boqDescriptionEn = '';
  boqUnit = 'PCS';
  boqQuantity = 1;
  boqEstimatedRate = 0;
  boqSortOrder = 0;

  showInviteModal = signal<boolean>(false);
  bidderName = '';
  bidderPartyId = '';
  bidderEmail = '';
  bidderPhone = '';
  bidderNotes = '';

  showSubmitBidModal = signal<boolean>(false);
  submittingBidder = signal<TenderBidder | null>(null);
  bidLines: { boqItemId: string; itemCode: string; description: string; quantity: number; unitRate: number; remarks: string }[] = [];

  showTechEvalModal = signal<boolean>(false);
  evaluatingBidder = signal<TenderBidder | null>(null);
  techScore = 80;

  showClarifModal = signal<boolean>(false);
  clarifQuestion = '';
  clarifIsPublic = true;

  showAnswerModal = signal<boolean>(false);
  answeringClarif = signal<TenderClarification | null>(null);
  clarifAnswer = '';
  answerIsPublic = true;

  showAwardModal = signal<boolean>(false);
  selectedAwardBidderId = '';
  awardNotes = '';
  updateProjectContract = true;

  ngOnInit(): void {
    this.loadTenderDetails();
  }

  loadTenderDetails(): void {
    this.tenderService.loadTender(this.tenderId).subscribe();
  }

  formatDate(epoch?: number | null): string {
    if (!epoch) return '—';
    return new Date(epoch).toLocaleDateString();
  }

  get tender(): ProjectTender | null {
    return this.tenderService.selectedTender();
  }

  // ─── Header Actions ───────────────────────────────────────────────

  onPublish(): void {
    this.tenderService.publishTender(this.tenderId).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.publishSuccess'));
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  onCancel(): void {
    this.tenderService.cancelTender(this.tenderId).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderUpdatedSuccess'));
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  // ─── BOQ Item Operations ──────────────────────────────────────────

  onOpenAddBoq(): void {
    this.editingBoqItem.set(null);
    const count = this.tender?.boqItems?.length || 0;
    this.boqItemCode = `BOQ-${count + 1}`;
    this.boqDescription = '';
    this.boqDescriptionEn = '';
    this.boqUnit = 'PCS';
    this.boqQuantity = 1;
    this.boqEstimatedRate = 0;
    this.boqSortOrder = count + 1;
    this.showBoqModal.set(true);
  }

  onOpenEditBoq(item: TenderBoqItem): void {
    this.editingBoqItem.set(item);
    this.boqItemCode = item.itemCode;
    this.boqDescription = item.description;
    this.boqDescriptionEn = item.descriptionEn || '';
    this.boqUnit = item.unitOfMeasure;
    this.boqQuantity = item.quantity;
    this.boqEstimatedRate = item.estimatedRate;
    this.boqSortOrder = item.sortOrder;
    this.showBoqModal.set(true);
  }

  onSaveBoq(): void {
    const req: CreateBoqItemRequest = {
      itemCode: this.boqItemCode.trim(),
      description: this.boqDescription.trim(),
      descriptionEn: this.boqDescriptionEn.trim() || null,
      unitOfMeasure: this.boqUnit.trim(),
      quantity: this.boqQuantity,
      estimatedRate: this.boqEstimatedRate,
      sortOrder: this.boqSortOrder
    };

    const editing = this.editingBoqItem();
    if (editing) {
      this.tenderService.updateBoqItem(this.tenderId, editing.id, req).subscribe({
        next: () => {
          this.notify.success(this.i18n.t('tenders.tenderUpdatedSuccess'));
          this.showBoqModal.set(false);
          this.refreshList.emit();
        },
        error: () => this.notify.error(this.i18n.t('common.genericError'))
      });
    } else {
      this.tenderService.addBoqItem(this.tenderId, req).subscribe({
        next: () => {
          this.notify.success(this.i18n.t('tenders.tenderCreatedSuccess'));
          this.showBoqModal.set(false);
          this.refreshList.emit();
        },
        error: () => this.notify.error(this.i18n.t('common.genericError'))
      });
    }
  }

  onDeleteBoq(itemId: string): void {
    this.tenderService.deleteBoqItem(this.tenderId, itemId).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderDeletedSuccess'));
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  // ─── Bidder & Pricing Operations ──────────────────────────────────

  onOpenInvite(): void {
    this.bidderName = '';
    this.bidderPartyId = '';
    this.bidderEmail = '';
    this.bidderPhone = '';
    this.bidderNotes = '';
    this.showInviteModal.set(true);
  }

  onSaveInvite(): void {
    const req: InviteBidderRequest = {
      bidderName: this.bidderName.trim(),
      partyId: this.bidderPartyId.trim() || null,
      contactEmail: this.bidderEmail.trim() || null,
      contactPhone: this.bidderPhone.trim() || null,
      notes: this.bidderNotes.trim() || null
    };

    this.tenderService.inviteBidder(this.tenderId, req).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderCreatedSuccess'));
        this.showInviteModal.set(false);
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  onOpenSubmitBid(b: TenderBidder): void {
    this.submittingBidder.set(b);
    const boq = this.tender?.boqItems || [];
    this.bidLines = boq.map(item => {
      const existingLine = b.submissionLines?.find(l => l.boqItemId === item.id);
      return {
        boqItemId: item.id,
        itemCode: item.itemCode,
        description: item.description,
        quantity: item.quantity,
        unitRate: existingLine ? existingLine.unitRate : item.estimatedRate,
        remarks: existingLine?.technicalRemarks || ''
      };
    });
    this.showSubmitBidModal.set(true);
  }

  onSaveBidSubmission(): void {
    const bidder = this.submittingBidder();
    if (!bidder) return;

    const lines: BidLineSubmission[] = this.bidLines.map(l => ({
      boqItemId: l.boqItemId,
      unitRate: l.unitRate,
      technicalRemarks: l.remarks || null
    }));

    this.tenderService.submitBid(this.tenderId, bidder.id, { lines }).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderCreatedSuccess'));
        this.showSubmitBidModal.set(false);
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  onOpenTechEval(b: TenderBidder): void {
    this.evaluatingBidder.set(b);
    this.techScore = b.technicalScore || 80;
    this.showTechEvalModal.set(true);
  }

  onSaveTechEval(): void {
    const b = this.evaluatingBidder();
    if (!b) return;

    this.tenderService.evaluateBidderTechnical(this.tenderId, b.id, { technicalScore: this.techScore }).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderUpdatedSuccess'));
        this.showTechEvalModal.set(false);
        this.loadTenderDetails();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  // ─── Evaluation Matrix Calculation ────────────────────────────────

  onCalculateEvaluation(): void {
    this.tenderService.calculateEvaluation(this.tenderId).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.evaluationSuccess'));
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  // ─── Clarifications ───────────────────────────────────────────────

  onOpenAddClarif(): void {
    this.clarifQuestion = '';
    this.clarifIsPublic = true;
    this.showClarifModal.set(true);
  }

  onSaveClarif(): void {
    const req: CreateClarificationRequest = {
      question: this.clarifQuestion.trim(),
      isPublicAddendum: this.clarifIsPublic
    };

    this.tenderService.addClarification(this.tenderId, req).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderCreatedSuccess'));
        this.showClarifModal.set(false);
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  onOpenAnswer(c: TenderClarification): void {
    this.answeringClarif.set(c);
    this.clarifAnswer = c.answer || '';
    this.answerIsPublic = c.isPublicAddendum;
    this.showAnswerModal.set(true);
  }

  onSaveAnswer(): void {
    const c = this.answeringClarif();
    if (!c) return;

    this.tenderService.answerClarification(this.tenderId, c.id, {
      answer: this.clarifAnswer.trim(),
      isPublicAddendum: this.answerIsPublic
    }).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.tenderUpdatedSuccess'));
        this.showAnswerModal.set(false);
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }

  // ─── Awarding ─────────────────────────────────────────────────────

  onOpenAward(): void {
    const topBidder = this.tender?.bidders?.find(b => b.rankOrder === 1);
    this.selectedAwardBidderId = topBidder ? topBidder.id : (this.tender?.bidders?.[0]?.id || '');
    this.awardNotes = '';
    this.updateProjectContract = true;
    this.showAwardModal.set(true);
  }

  onConfirmAward(): void {
    if (!this.selectedAwardBidderId) return;

    const req: AwardTenderRequest = {
      awardedBidderId: this.selectedAwardBidderId,
      notes: this.awardNotes.trim() || null,
      updateProjectContract: this.updateProjectContract
    };

    this.tenderService.awardTender(this.tenderId, req).subscribe({
      next: () => {
        this.notify.success(this.i18n.t('tenders.awardSuccess'));
        this.showAwardModal.set(false);
        this.refreshList.emit();
      },
      error: () => this.notify.error(this.i18n.t('common.genericError'))
    });
  }
}
