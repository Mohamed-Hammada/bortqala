import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EssService } from './ess.service';
import { I18nService } from '../../core/i18n.service';
import {
  EssProfileDto,
  EssPayslipSummaryDto,
  EssPayslipDetailDto,
  EssLeaveDto,
  EssAdvanceDto,
  EssAttendanceRecordDto,
} from './ess.models';

@Component({
  selector: 'app-ess-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ess.page.html',
  styleUrls: ['./ess.page.scss'],
})
export class EssPageComponent implements OnInit {
  readonly ess = inject(EssService);
  readonly i18n = inject(I18nService);

  readonly activeTab = signal<'overview' | 'payslips' | 'leaves' | 'advances' | 'attendance'>('overview');

  // Modals
  readonly showPayslipModal = signal(false);
  readonly showLeaveModal = signal(false);
  readonly showAdvanceModal = signal(false);

  // Forms
  readonly leaveForm = signal<{
    leaveTypeId: string;
    startDate: string;
    endDate: string;
    reason: string;
  }>({
    leaveTypeId: 'ANNUAL',
    startDate: new Date().toISOString().substring(0, 10),
    endDate: new Date().toISOString().substring(0, 10),
    reason: '',
  });

  readonly advanceForm = signal<{
    amount: number;
    totalInstallments: number;
    firstInstallmentDate: string;
    reason: string;
  }>({
    amount: 1000,
    totalInstallments: 3,
    firstInstallmentDate: new Date(Date.now() + 30 * 86400000).toISOString().substring(0, 10),
    reason: '',
  });

  async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  async setTab(tab: 'overview' | 'payslips' | 'leaves' | 'advances' | 'attendance'): Promise<void> {
    this.activeTab.set(tab);
    await this.loadData();
  }

  async loadData(): Promise<void> {
    const tab = this.activeTab();
    if (tab === 'overview') {
      await this.ess.loadProfile();
    } else if (tab === 'payslips') {
      await this.ess.loadPayslips();
    } else if (tab === 'leaves') {
      await this.ess.loadLeaves();
    } else if (tab === 'advances') {
      await this.ess.loadAdvances();
    } else if (tab === 'attendance') {
      await this.ess.loadAttendance();
    }
  }

  // --- Payslip Actions ---

  async viewPayslip(payslip: EssPayslipSummaryDto): Promise<void> {
    await this.ess.loadPayslipDetail(payslip.paymentId);
    this.showPayslipModal.set(true);
  }

  // --- Leave Actions ---

  openLeaveModal(): void {
    this.leaveForm.set({
      leaveTypeId: 'ANNUAL',
      startDate: new Date().toISOString().substring(0, 10),
      endDate: new Date().toISOString().substring(0, 10),
      reason: '',
    });
    this.showLeaveModal.set(true);
  }

  async saveLeave(): Promise<void> {
    const form = this.leaveForm();
    await this.ess.submitLeave(form);
    this.showLeaveModal.set(false);
    if (this.activeTab() === 'leaves') {
      await this.ess.loadLeaves();
    } else {
      await this.ess.loadProfile();
    }
  }

  // --- Advance Actions ---

  openAdvanceModal(): void {
    this.advanceForm.set({
      amount: 1000,
      totalInstallments: 3,
      firstInstallmentDate: new Date(Date.now() + 30 * 86400000).toISOString().substring(0, 10),
      reason: '',
    });
    this.showAdvanceModal.set(true);
  }

  async saveAdvance(): Promise<void> {
    const form = this.advanceForm();
    await this.ess.submitAdvance(form);
    this.showAdvanceModal.set(false);
    if (this.activeTab() === 'advances') {
      await this.ess.loadAdvances();
    } else {
      await this.ess.loadProfile();
    }
  }
}
