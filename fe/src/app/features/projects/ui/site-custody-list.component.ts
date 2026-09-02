import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n.service';
import { NotificationService } from '../../../core/notification.service';
import { ProjectService } from '../data-access/project.service';
import {
  IssueCustodyRequest,
  RecordCustodyExpenseRequest,
  SettleCustodyRequest,
  SiteCustodyExpenseResponse,
  SiteCustodyResponse,
} from '../models/project.models';

@Component({
  selector: 'app-site-custody-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './site-custody-list.component.html',
  styleUrls: ['./site-custody-list.component.scss'],
})
export class SiteCustodyListComponent implements OnInit {
  @Input({ required: true }) projectId!: string;

  private readonly projectService = inject(ProjectService);
  private readonly notification = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly custodies = signal<SiteCustodyResponse[]>([]);
  readonly selectedCustody = signal<SiteCustodyResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Modals
  readonly showIssueModal = signal<boolean>(false);
  issueForm: IssueCustodyRequest = {
    custodyCode: '',
    custodianName: '',
    custodyType: 'CASH',
    initialAmount: 10000,
    notes: '',
  };

  readonly showExpenseModal = signal<boolean>(false);
  expenseForm: RecordCustodyExpenseRequest = {
    expenseDate: Date.now(),
    amount: 500,
    category: 'FUEL',
    description: '',
    receiptNumber: '',
  };

  readonly showSettleModal = signal<boolean>(false);
  settleForm: SettleCustodyRequest = {
    amountReturned: 0,
    receivedBy: '',
    notes: '',
  };

  ngOnInit(): void {
    if (this.projectId) {
      this.loadCustodies();
    }
  }

  loadCustodies(): void {
    this.loading.set(true);
    this.projectService.loadProjectCustodies(this.projectId).subscribe({
      next: (list) => {
        this.custodies.set(list);
        if (list.length > 0) {
          const currentSelected = this.selectedCustody();
          const match = currentSelected ? list.find((c) => c.id === currentSelected.id) : list[0];
          this.selectedCustody.set(match || list[0]);
        } else {
          this.selectedCustody.set(null);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openIssueModal(): void {
    this.issueForm = {
      custodyCode: 'CUST-' + Math.floor(1000 + Math.random() * 9000),
      custodianName: '',
      custodyType: 'CASH',
      initialAmount: 10000,
      notes: '',
    };
    this.showIssueModal.set(true);
  }

  submitIssueCustody(): void {
    if (!this.issueForm.custodyCode || !this.issueForm.custodianName) return;
    this.saving.set(true);
    this.projectService.issueCustody(this.projectId, this.issueForm).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.showIssueModal.set(false);
        this.loadCustodies();
        this.notification.success(this.i18n.t('projects.custodyIssued'));
      },
      error: () => this.saving.set(false),
    });
  }

  openExpenseModal(): void {
    this.expenseForm = {
      expenseDate: Date.now(),
      amount: 100,
      category: 'SITE_SUPPLIES',
      description: '',
      receiptNumber: '',
    };
    this.showExpenseModal.set(true);
  }

  submitExpense(): void {
    const custody = this.selectedCustody();
    if (!custody || !this.expenseForm.description) return;
    this.saving.set(true);
    this.projectService.recordCustodyExpense(custody.id, this.expenseForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showExpenseModal.set(false);
        this.loadCustodies();
        this.notification.success(this.i18n.t('projects.custodyExpenseRecorded'));
      },
      error: () => this.saving.set(false),
    });
  }

  approveExpense(expense: SiteCustodyExpenseResponse): void {
    this.projectService.approveCustodyExpense(expense.id).subscribe({
      next: () => {
        this.loadCustodies();
        this.notification.success(this.i18n.t('projects.custodyExpenseApproved'));
      },
    });
  }

  rejectExpense(expense: SiteCustodyExpenseResponse): void {
    this.projectService.rejectCustodyExpense(expense.id).subscribe({
      next: () => {
        this.loadCustodies();
        this.notification.success(this.i18n.t('projects.custodyExpenseRejected'));
      },
    });
  }

  openSettleModal(): void {
    const custody = this.selectedCustody();
    if (!custody) return;
    this.settleForm = {
      amountReturned: custody.remainingBalance,
      receivedBy: '',
      notes: '',
    };
    this.showSettleModal.set(true);
  }

  submitSettle(): void {
    const custody = this.selectedCustody();
    if (!custody) return;
    this.saving.set(true);
    this.projectService.settleCustody(custody.id, this.settleForm).subscribe({
      next: () => {
        this.saving.set(false);
        this.showSettleModal.set(false);
        this.loadCustodies();
        this.notification.success(this.i18n.t('projects.custodySettled'));
      },
      error: () => this.saving.set(false),
    });
  }
}
