import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { WorkforceAdvance, AdvanceRepayRequest, AdvancePolicy } from '../../models/workforce.models';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { exportCsv } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-advances',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, AppTooltipDirective],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">{{ i18n.t('workforce.ui.advances.eyebrow') }}</span>
          <h1>{{ i18n.t('workforce.ui.advances.title') }}</h1>
        </div>
        <div style="display: flex; gap: 0.75rem;">
          <button type="button" class="btn btn-secondary" (click)="openPolicyModal()">⚙ {{ i18n.t('workforce.ui.advances.policy') }}</button>
          <button type="button" class="btn btn-secondary" (click)="exportCsv()">{{ i18n.t('workforce.ui.exportExcel') }}</button>
          <button type="button" class="btn btn-primary" (click)="openCreateModal()">
            {{ i18n.t('workforce.ui.advances.new') }}
          </button>
        </div>
      </header>

      <div class="card policy-summary-card">
        <div><strong>{{ i18n.t('workforce.ui.advances.policySummary') }}</strong><p>{{ i18n.t('workforce.ui.advances.policyPriority') }}</p></div>
        <div class="policy-chips">@for (policy of workforceService.advancePolicies(); track policy.id) {<span class="badge policy-chip">{{ policy.scopeName || getPolicyScopeLabel(policy) }} · {{ i18n.t('workforce.ui.advances.version') }} {{ policy.version }} · {{ policy.effectiveFrom }} → {{ policy.effectiveTo || i18n.t('workforce.ui.advances.openEnded') }} · {{ policy.deductionMode === 'AUTO' ? i18n.t('workforce.ui.advances.auto') : i18n.t('workforce.ui.advances.manual') }} · {{ policy.maxDeductionPercent }}%</span>}</div>
      </div>

      <!-- Summary Stats -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-label">{{ i18n.t('workforce.ui.advances.totalOpen') }}</span>
          <span class="stat-value">{{ workforceService.advances().length }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ i18n.t('workforce.ui.advances.totalGranted') }}</span>
          <span class="stat-value amount-val">{{ totalGranted() | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">{{ i18n.t('workforce.ui.advances.totalRemaining') }}</span>
          <span class="stat-value balance-val">{{ totalRemaining() | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
        </div>
      </div>

      <!-- Loading -->
      @if (loading()) {
        <div class="card">
          @for (_ of [1,2,3]; track $index) {
            <div class="skeleton-row"></div>
          }
        </div>
      }

      <!-- Table -->
      @else {
        <div class="card">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ i18n.t('workforce.ui.advances.recipient') }}</th>
                <th>{{ i18n.t('workforce.ui.type') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.totalAmount') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.termType') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.installments') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.installmentAmount') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.remainingBalance') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.frequency') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.maxDeduction') }}</th>
                <th>{{ i18n.t('workforce.ui.status') }}</th>
                <th>{{ i18n.t('workforce.ui.advances.createdAt') }}</th>
              </tr>
            </thead>
            <tbody>
              @for (adv of workforceService.advances(); track adv.id) {
                <tr>
                  <td><strong>{{ recipientName(adv) }}</strong></td>
                  <td><span class="badge type-badge">{{ recipientTypeLabel(adv.recipientType) }}</span></td>
                  <td>{{ adv.amount | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</td>
                  <td><span class="badge term-badge" [class.long-term]="adv.termType === 'LONG_TERM'">{{ getTermLabel(adv.termType) }}</span></td>
                  <td>{{ adv.totalInstallments }} {{ i18n.t('workforce.ui.advances.installmentUnit') }}</td>
                  <td>{{ adv.installmentAmount | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</td>
                  <td><strong class="rem-bal">{{ adv.remainingBalance | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }}</strong></td>
                  <td>{{ getFrequencyLabel(adv.deductionFrequency) }}</td>
                  <td>{{ adv.maxDeductionPercent }} %</td>
                  <td>
                    <span class="badge" [class.status-active]="adv.status === 'ACTIVE'"
                          [class.status-paid]="adv.status === 'PAID_OFF'"
                          [class.status-suspended]="adv.status === 'PAUSED' || adv.status === 'SUSPENDED'">
                      {{ getStatusLabel(adv.status) }}
                    </span>
                  </td>
                  <td>
                    <div style="display: flex; gap: 0.25rem;">
                      @if (adv.status === 'ACTIVE') {
                        <button type="button" class="btn btn-secondary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="pauseAdvance(adv)">⏸️ {{ i18n.t('workforce.ui.advances.pause') }}</button>
                        <button type="button" class="btn btn-secondary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="repayAdvance(adv)">💵 {{ i18n.t('workforce.ui.advances.repay') }}</button>
                      }
                      @if (adv.status === 'PAUSED' || adv.status === 'SUSPENDED') {
                        <button type="button" class="btn btn-primary btn-sm" style="padding: 2px 6px; font-size: 0.75rem;" (click)="resumeAdvance(adv)">▶️ {{ i18n.t('workforce.ui.advances.resume') }}</button>
                      }
                    </div>
                  </td>
                </tr>
              }
              @if (workforceService.advances().length === 0) {
                <tr><td colspan="11" class="empty-cell">{{ i18n.t('workforce.ui.advances.empty') }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Create Modal -->
      <app-modal-dialog
        [isOpen]="isModalOpen"
        [title]="i18n.t('workforce.ui.advances.createTitle')"
        size="wide"
        [preventOutsideClose]="true"
        (close)="isModalOpen = false">

        <form (ngSubmit)="saveAdvance()" class="modal-form">
          <div class="form-grid">

            <!-- Recipient Type -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.recipientTypeRequired') }}</label>
              <select [(ngModel)]="form.recipientType" name="recipientType" class="form-input"
                      (ngModelChange)="onRecipientTypeChange()">
                <option value="WORKER">{{ i18n.t('workforce.ui.worker') }}</option>
                <option value="CONTRACTOR">{{ i18n.t('workforce.ui.contractor') }}</option>
                <option value="EMPLOYEE">{{ i18n.t('workforce.ui.employee') }}</option>
              </select>
            </div>

            <!-- Worker / Contractor selector -->
            @if (form.recipientType === 'WORKER') {
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.selectWorker') }}</label>
                <select [(ngModel)]="form.workerId" name="workerId" class="form-input" (ngModelChange)="applyAdvancePolicy()">
                  @for (w of workforceService.workers(); track w.id) {
                    <option [value]="w.id">{{ w.fullName }} ({{ w.code }})</option>
                  }
                </select>
              </div>
            } @else if (form.recipientType === 'CONTRACTOR') {
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.selectContractor') }}</label>
                <select [(ngModel)]="form.contractorId" name="contractorId" class="form-input">
                  @for (c of workforceService.contractors(); track c.id) {
                    <option [value]="c.id">{{ c.name }} ({{ c.code }})</option>
                  }
                </select>
              </div>
            } @else {
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.selectEmployee') }}</label>
                <select [(ngModel)]="form.employeeId" name="employeeId" class="form-input"
                        [attr.aria-label]="i18n.t('workforce.ui.advances.employeeAria')" (ngModelChange)="applyAdvancePolicy()">
                  @if (workforceService.employees().length === 0) {
                    <option value="" disabled>{{ i18n.t('workforce.ui.advances.noEmployees') }}</option>
                  }
                  @for (employee of workforceService.employees(); track employee.id) {
                    <option [value]="employee.id">{{ employee.fullName }} ({{ employee.employeeCode }})</option>
                  }
                </select>
              </div>
            }

            @if (effectivePolicyPreview(); as policy) {
              <div class="summary-box col-span-2">
                <strong>{{ i18n.t('workforce.ui.advances.effectivePolicy') }}</strong>
                {{ policy.scopeName || getPolicyScopeLabel(policy) }} · {{ i18n.t('workforce.ui.advances.version') }} {{ policy.version }} ·
                {{ policy.deductionMode === 'AUTO' ? i18n.t('workforce.ui.advances.autoDeduction') : i18n.t('workforce.ui.advances.manualDeduction') }} · {{ i18n.t('workforce.ui.advances.maxDeduction') }} {{ policy.maxDeductionPercent }}% ·
                {{ i18n.t('workforce.ui.advances.effectiveFrom') }} {{ policy.effectiveFrom }} {{ policy.effectiveTo ? ('→ ' + policy.effectiveTo) : '' }}
              </div>
            }

            <!-- Amount -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.amountRequired') }}</label>
              <input type="number" [(ngModel)]="form.amount" name="amount" required
                     class="form-input" min="1" (ngModelChange)="recalcInstallment()" />
            </div>

            <!-- Term Type -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.termType') }}</label>
              <select [(ngModel)]="form.termType" name="termType" class="form-input">
                <option value="SHORT_TERM">{{ i18n.t('workforce.ui.advances.shortDescription') }}</option>
                <option value="LONG_TERM">{{ i18n.t('workforce.ui.advances.longDescription') }}</option>
              </select>
            </div>

            <!-- Installments count (only for LONG_TERM) -->
            @if (form.termType === 'LONG_TERM') {
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.installmentCount') }}</label>
                <input type="number" [(ngModel)]="form.totalInstallments" name="totalInstallments"
                       class="form-input" min="2" max="60" (ngModelChange)="recalcInstallment()" />
              </div>

              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.installmentAuto') }} <span tabindex="0" [attr.aria-label]="i18n.t('workforce.ui.advances.installmentHelpAria')" [appTooltip]="i18n.t('workforce.ui.advances.installmentHelp')">ⓘ</span></label>
                <input type="number" [(ngModel)]="form.installmentAmount" name="installmentAmount"
                       class="form-input" min="1" />
              </div>

              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.firstInstallmentDate') }}</label>
                <input type="date" [(ngModel)]="form.firstInstallmentDate" name="firstInstallmentDate"
                       class="form-input" (ngModelChange)="applyAdvancePolicy()" />
              </div>

              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.frequency') }}</label>
                <select [(ngModel)]="form.deductionFrequency" name="deductionFrequency" class="form-input">
                  <option value="HALF_MONTH">{{ i18n.t('workforce.ui.advances.halfMonth15') }}</option>
                  <option value="MONTHLY">{{ i18n.t('workforce.ui.advances.monthly') }}</option>
                  <option value="WEEKLY">{{ i18n.t('workforce.ui.advances.weekly') }}</option>
                </select>
              </div>

              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.deductionMode') }}</label>
                <select [(ngModel)]="form.deductionMode" name="deductionMode" class="form-input">
                  <option value="AUTO">{{ i18n.t('workforce.ui.advances.autoWithSettlement') }}</option>
                  <option value="MANUAL">{{ i18n.t('workforce.ui.advances.manualIntervention') }}</option>
                </select>
              </div>

              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.deferral') }}</label>
                <input type="number" [(ngModel)]="form.deferralPeriods" name="deferralPeriods"
                       class="form-input" min="0" max="12" />
              </div>
            }

            <!-- Max deduction % -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.maxPeriodDeduction') }}</label>
              <input type="number" [(ngModel)]="form.maxDeductionPercent" name="maxDeductionPercent"
                     class="form-input" min="10" max="100" />
            </div>

            <!-- Reason -->
            <div class="form-group col-span-2">
              <label>{{ i18n.t('workforce.ui.advances.reason') }}</label>
              <input type="text" [(ngModel)]="form.reason" name="reason" class="form-input"
                     [placeholder]="i18n.t('workforce.ui.advances.reasonPlaceholder')" />
            </div>

            <!-- Summary for long term -->
            @if (form.termType === 'LONG_TERM' && form.totalInstallments > 0) {
              <div class="summary-box col-span-2">
                <strong>{{ i18n.t('workforce.ui.advances.summary') }}</strong> {{ form.amount | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }} ÷
                {{ form.totalInstallments }} {{ i18n.t('workforce.ui.advances.installmentUnit') }} = {{ form.installmentAmount | number:'1.2-2' }} {{ i18n.t('workforce.ui.currencyEgp') }} / {{ i18n.t('workforce.ui.advances.installmentUnit') }}
                @if (form.firstInstallmentDate) {
                  | {{ i18n.t('workforce.ui.advances.firstInstallment') }} {{ form.firstInstallmentDate }}
                }
              </div>
            }
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="saveAdvance()">
            {{ saving() ? i18n.t('common.saving') : i18n.t('workforce.ui.advances.approveDisburse') }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="isModalOpen = false">{{ i18n.t('workforce.ui.cancel') }}</button>
        </div>
      </app-modal-dialog>

      <app-modal-dialog [isOpen]="policyModalOpen()" [title]="i18n.t('workforce.ui.advances.policyTitle')" size="normal" [preventOutsideClose]="true" (close)="policyModalOpen.set(false)">
        <form class="modal-form">
          <div class="form-grid">
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.policyScope') }}</label><select [(ngModel)]="policyForm.scopeType" name="policyScope" class="form-input" (ngModelChange)="policyForm.scopeId = ''"><option value="GLOBAL">{{ i18n.t('workforce.ui.advances.global') }}</option><option value="CATEGORY">{{ i18n.t('workforce.ui.advances.workerCategoryException') }}</option><option value="WORKER">{{ i18n.t('workforce.ui.advances.workerException') }}</option><option value="EMPLOYEE_CATEGORY">{{ i18n.t('workforce.ui.advances.employeeCategoryException') }}</option><option value="EMPLOYEE">{{ i18n.t('workforce.ui.advances.employeeException') }}</option></select></div>
            @if (policyForm.scopeType === 'CATEGORY') {<div class="form-group"><label>{{ i18n.t('workforce.ui.advances.categoryRequired') }}</label><select [(ngModel)]="policyForm.scopeId" name="policyCategory" class="form-input">@for (category of workforceService.categories(); track category.id) {<option [value]="category.id">{{ category.name }}</option>}</select></div>}
            @if (policyForm.scopeType === 'WORKER') {<div class="form-group"><label>{{ i18n.t('workforce.ui.advances.workerRequired') }}</label><select [(ngModel)]="policyForm.scopeId" name="policyWorker" class="form-input">@for (worker of workforceService.workers(); track worker.id) {<option [value]="worker.id">{{ worker.fullName }} ({{ worker.code }})</option>}</select></div>}
            @if (policyForm.scopeType === 'EMPLOYEE_CATEGORY') {<div class="form-group"><label>{{ i18n.t('workforce.ui.advances.employeeCategoryRequired') }}</label><select [(ngModel)]="policyForm.scopeId" name="policyEmployeeCategory" class="form-input">@for (category of employeeCategories(); track category.id) {<option [value]="category.id">{{ category.name }}</option>}</select></div>}
            @if (policyForm.scopeType === 'EMPLOYEE') {<div class="form-group"><label>{{ i18n.t('workforce.ui.advances.employeeRequired') }}</label><select [(ngModel)]="policyForm.scopeId" name="policyEmployee" class="form-input">@for (employee of workforceService.employees(); track employee.id) {<option [value]="employee.id">{{ employee.fullName }} ({{ employee.employeeCode }})</option>}</select></div>}
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.defaultDeductionMode') }}</label><select [(ngModel)]="policyForm.deductionMode" name="policyMode" class="form-input"><option value="AUTO">{{ i18n.t('workforce.ui.advances.autoSettlement') }}</option><option value="MANUAL">{{ i18n.t('workforce.ui.advances.manual') }}</option></select></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.frequency') }}</label><select [(ngModel)]="policyForm.deductionFrequency" name="policyFrequency" class="form-input"><option value="HALF_MONTH">{{ i18n.t('workforce.ui.advances.halfMonthly') }}</option><option value="MONTHLY">{{ i18n.t('workforce.ui.advances.monthly') }}</option><option value="WEEKLY">{{ i18n.t('workforce.ui.advances.weekly') }}</option></select></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.policyMax') }}</label><input type="number" min="1" max="100" [(ngModel)]="policyForm.maxDeductionPercent" name="policyMax" class="form-input" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.defaultInstallments') }}</label><input type="number" min="1" max="60" [(ngModel)]="policyForm.defaultInstallments" name="policyInstallments" class="form-input" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.defaultDeferral') }}</label><input type="number" min="0" max="12" [(ngModel)]="policyForm.deferralPeriods" name="policyDeferral" class="form-input" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.policyStart') }}</label><input type="date" [(ngModel)]="policyForm.effectiveFrom" name="policyEffectiveFrom" class="form-input" /></div>
            <div class="form-group"><label>{{ i18n.t('workforce.ui.advances.policyEnd') }}</label><input type="date" [(ngModel)]="policyForm.effectiveTo" name="policyEffectiveTo" class="form-input" /></div>
            <label class="form-group"><span>{{ i18n.t('workforce.ui.status') }}</span><input type="checkbox" [(ngModel)]="policyForm.active" name="policyActive" /> {{ i18n.t('workforce.ui.advances.enabled') }}</label>
          </div>
        </form>
        <div modal-actions class="modal-actions-bar"><button type="button" class="btn btn-primary" [disabled]="saving()" (click)="savePolicy()">{{ i18n.t('workforce.ui.advances.savePolicy') }}</button><button type="button" class="btn btn-secondary" (click)="policyModalOpen.set(false)">{{ i18n.t('workforce.ui.cancel') }}</button></div>
      </app-modal-dialog>

      <!-- Repayment Modal -->
      <app-modal-dialog
        [isOpen]="repayModalOpen()"
        [title]="i18n.t('workforce.ui.advances.earlyRepayment')"
        size="normal"
        [preventOutsideClose]="true"
        (close)="closeRepayModal()">

        @if (repayTarget(); as adv) {
          <div class="repay-container">

            <!-- Recipient Info -->
            <div class="repay-info-grid">
              <div class="repay-info-item">
                <span class="info-label">{{ i18n.t('workforce.ui.advances.beneficiary') }}</span>
                <span class="info-value">{{ recipientName(adv) }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">{{ i18n.t('workforce.ui.type') }}</span>
                <span class="info-value">{{ recipientTypeLabel(adv.recipientType) }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">{{ i18n.t('workforce.ui.advances.originalAmount') }} <span tabindex="0" [attr.aria-label]="i18n.t('workforce.ui.advances.originalAmountAria')" [appTooltip]="i18n.t('workforce.ui.advances.originalAmountHelp')">ⓘ</span></span>
                <span class="info-value">{{ adv.amount | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">{{ i18n.t('workforce.ui.advances.previouslyPaid') }}</span>
                <span class="info-value">{{ (adv.amount - adv.remainingBalance) | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
              </div>
              <div class="repay-info-item">
                <span class="info-label">{{ i18n.t('workforce.ui.advances.remainingBalance') }} <span tabindex="0" [attr.aria-label]="i18n.t('workforce.ui.advances.remainingHelpAria')" [appTooltip]="i18n.t('workforce.ui.advances.remainingHelp')">ⓘ</span></span>
                <span class="info-value balance-highlight">{{ adv.remainingBalance | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
              </div>
            </div>

            <!-- Repayment Type -->
            <div class="repay-type-group">
              <label class="repay-type-option">
                <input type="radio" [(ngModel)]="repayForm.repaymentType" name="repayType"
                       value="FULL" (change)="onRepayTypeChange()" />
                <span>{{ i18n.t('workforce.ui.advances.fullRepayment') }}</span>
              </label>
              <label class="repay-type-option">
                <input type="radio" [(ngModel)]="repayForm.repaymentType" name="repayType"
                       value="PARTIAL" (change)="onRepayTypeChange()" />
                <span>{{ i18n.t('workforce.ui.advances.partialRepayment') }}</span>
              </label>
            </div>

            <!-- Amount -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.repaymentAmount') }}</label>
              <input type="number" [(ngModel)]="repayForm.amount" name="repayAmount"
                     class="form-input" min="1" [max]="adv.remainingBalance" />
              <small class="hint">{{ i18n.t('workforce.ui.advances.maximum') }} {{ adv.remainingBalance | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</small>
            </div>

            <!-- Date & Method Row -->
            <div class="form-grid">
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.repaymentDate') }}</label>
                <input type="date" [(ngModel)]="repayForm.repaymentDate" name="repayDate" class="form-input" />
              </div>
              <div class="form-group">
                <label>{{ i18n.t('workforce.ui.advances.paymentMethod') }}</label>
                <select [(ngModel)]="repayForm.paymentMethod" name="repayMethod" class="form-input">
                  <option value="">{{ i18n.t('workforce.ui.advances.select') }}</option>
                  <option value="CASH">{{ i18n.t('workforce.ui.advances.cash') }}</option>
                  <option value="BANK_TRANSFER">{{ i18n.t('workforce.ui.advances.bankTransfer') }}</option>
                  <option value="CHEQUE">{{ i18n.t('workforce.ui.advances.cheque') }}</option>
                  <option value="DEDUCTION">{{ i18n.t('workforce.ui.advances.entitlementDeduction') }}</option>
                </select>
              </div>
            </div>

            <!-- Receipt Ref -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.receiptRef') }}</label>
              <input type="text" [(ngModel)]="repayForm.receiptRef" name="receiptRef" class="form-input"
                     [placeholder]="i18n.t('workforce.ui.advances.optional')" />
            </div>

            <!-- Notes -->
            <div class="form-group">
              <label>{{ i18n.t('workforce.ui.advances.notes') }}</label>
              <input type="text" [(ngModel)]="repayForm.notes" name="repayNotes" class="form-input"
                     [placeholder]="i18n.t('workforce.ui.advances.optional')" />
            </div>

            <!-- Preview -->
            @if (repayPreview(); as preview) {
              <div class="repay-preview" [class.preview-close]="preview.willClose">
                <strong>{{ i18n.t('workforce.ui.advances.preview') }}</strong>
                <div class="preview-row">
                  <span>{{ i18n.t('workforce.ui.advances.beforeRepayment') }}</span>
                  <span>{{ preview.before | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
                </div>
                <div class="preview-row">
                  <span>{{ i18n.t('workforce.ui.advances.repaymentAmountLabel') }}</span>
                  <span class="deduct-amount">- {{ preview.amount | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
                </div>
                <div class="preview-row total-row">
                  <span>{{ i18n.t('workforce.ui.advances.afterRepayment') }}</span>
                  <span [class.zero-balance]="preview.after <= 0">{{ preview.after | number:'1.0-0' }} {{ i18n.t('workforce.ui.currencyEgp') }}</span>
                </div>
                <div class="preview-impact">{{ preview.impact }}</div>
              </div>
            }
          </div>
        }
        @if (repayTarget()) {
          <div modal-actions class="modal-actions-bar">
            <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="confirmRepayment()">
              {{ saving() ? i18n.t('common.saving') : i18n.t('workforce.ui.advances.confirmRepayment') }}
            </button>
            <button type="button" class="btn btn-secondary" [disabled]="saving()" (click)="closeRepayModal()">{{ i18n.t('workforce.ui.cancel') }}</button>
          </div>
        }
      </app-modal-dialog>

      <!-- Confirmation Dialog -->
      @if (confirmAction(); as action) {
        <div class="confirm-overlay" (click)="cancelAction()">
          <div class="confirm-dialog" (click)="$event.stopPropagation()">
            <div class="confirm-icon">⚠️</div>
            <div class="confirm-message">{{ action.message }}</div>
            <div class="confirm-actions">
              <button type="button" class="btn btn-primary" (click)="action.onConfirm()">{{ i18n.t('workforce.ui.advances.confirm') }}</button>
              <button type="button" class="btn btn-secondary" (click)="cancelAction()">{{ i18n.t('workforce.ui.cancel') }}</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem;  }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.5rem; font-weight: 800; color: var(--ink); margin: 0.25rem 0 0 0; }
    .stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .policy-summary-card { display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .policy-summary-card p { margin: .25rem 0 0; color: var(--muted); font-size: .8rem; }
    .policy-chips { display: flex; flex-wrap: wrap; gap: .4rem; justify-content: flex-end; }
    .policy-chip { background: var(--surface-muted); color: var(--secondary-text); }
    .stat-card { background: var(--surface); border-radius: 12px; border: 1px solid var(--line); padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .stat-label { font-size: 0.8125rem; color: var(--muted); }
    .stat-value { font-size: 1.5rem; font-weight: 800; color: var(--ink); }
    .amount-val { color: var(--secondary-text); }
    .balance-val { color: var(--danger); }
    .card { background: var(--surface); border-radius: 12px; border: 1px solid var(--line); padding: 1.25rem; }
    .skeleton-row { height: 48px; background: linear-gradient(90deg, var(--surface-muted) 25%, var(--line) 50%, var(--surface-muted) 75%); background-size: 200% 100%; border-radius: 6px; animation: shimmer 1.5s infinite; margin-bottom: 0.5rem; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    .data-table { width: 100%; border-collapse: collapse; text-align: start; }
    .data-table th, .data-table td { padding: 0.625rem 0.75rem; border-bottom: 1px solid var(--line); font-size: 0.875rem; }
    .data-table th { background: var(--surface-muted); font-weight: 700; color: var(--secondary-text); }
    .btn { padding: 0.625rem 1.25rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn:disabled { opacity: 0.6; cursor:not-allowed; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: var(--line); color: var(--secondary-text); }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge.type-badge { background: var(--surface-muted); color: var(--secondary-text); }
    .badge.term-badge { background: #e0e7ff; color: #3730a3; }
    .badge.term-badge.long-term { background: #fef3c7; color: #92400e; }
    .badge.status-active { background: #dcfce7; color: var(--success); }
    .badge.status-paid { background: var(--success-soft); color: var(--success); }
    .badge.status-suspended { background: #fef3c7; color: #92400e; }
    .rem-bal { color: var(--danger); }
    .empty-cell { text-align: center; color: var(--muted); padding: 2rem; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .col-span-2 { grid-column: span 2; }
    .form-group { display: flex; flex-direction: column; gap: 0.375rem; }
    .form-group label { font-weight: 600; font-size: 0.8125rem; color: var(--secondary-text); }
    .form-input { padding: 0.625rem; border: 1px solid var(--line); border-radius: 8px; font-size: 0.875rem; }
    .summary-box { background: var(--surface-muted); border: 1px solid #bfdbfe; border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.875rem; color: var(--secondary-text); }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-start; }

    /* Repayment Modal Styles */
    .repay-container { display: flex; flex-direction: column; gap: 1rem; }
    .repay-info-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; background: var(--surface-muted); border-radius: 8px; padding: 0.75rem; }
    .repay-info-item { display: flex; flex-direction: column; gap: 0.25rem; }
    .info-label { font-size: 0.75rem; color: var(--muted); }
    .info-value { font-size: 0.9375rem; font-weight: 700; color: var(--ink); }
    .balance-highlight { color: var(--danger); font-size: 1.125rem; }
    .repay-type-group { display: flex; flex-direction: column; gap: 0.5rem; background: var(--warning-soft); border: 1px solid color-mix(in srgb, var(--warning) 45%, var(--line)); border-radius: 8px; padding: 0.75rem; }
    .repay-type-option { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; font-size: 0.875rem; }
    .repay-type-option input[type="radio"] { width: 1rem; height: 1rem; accent-color: #d97706; }
    .repay-preview { background: var(--success-soft); border: 1px solid color-mix(in srgb, var(--success) 45%, var(--line)); border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.875rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .repay-preview.preview-close { background: var(--danger-soft); border-color: color-mix(in srgb, var(--danger) 45%, var(--line)); }
    .preview-row { display: flex; justify-content: space-between; }
    .preview-row.total-row { border-top: 1px solid var(--line); padding-top: 0.375rem; font-weight: 700; }
    .deduct-amount { color: var(--danger); }
    .zero-balance { color: #16a34a; }
    .preview-impact { font-weight: 600; color: var(--secondary-text); margin-top: 0.25rem; }
    .repay-preview.preview-close .preview-impact { color: var(--danger); }
    .hint { font-size: 0.75rem; color: var(--muted); }

    /* Confirmation Dialog */
    .confirm-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 9999; }
    .confirm-dialog { background: var(--surface); border-radius: 12px; padding: 2rem; min-width: 360px; max-width: 480px; text-align: center; display: flex; flex-direction: column; gap: 1rem; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
    .confirm-icon { font-size: 2rem; }
    .confirm-message { font-size: 1rem; color: var(--secondary-text); line-height: 1.6; }
    .confirm-actions { display: flex; gap: 0.75rem; justify-content: center; }
  `]
})
export class AdvancesComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);
  readonly i18n = inject(I18nService);
  private route = inject(ActivatedRoute);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;
  policyModalOpen = signal(false);
  policyForm: AdvancePolicy = this.defaultPolicyForm();
  effectivePolicyPreview = signal<AdvancePolicy | null>(null);

  // Repayment modal state
  repayModalOpen = signal(false);
  repayTarget = signal<WorkforceAdvance | null>(null);
  repayForm: {
    repaymentType: 'PARTIAL' | 'FULL';
    amount: number;
    repaymentDate: string;
    paymentMethod: string;
    receiptRef: string;
    notes: string;
  } = this.defaultRepayForm();

  repayPreview = computed(() => {
    const adv = this.repayTarget();
    if (!adv) return null;
    const before = adv.remainingBalance;
    const amount = this.repayForm.amount || 0;
    const after = Math.max(0, before - amount);
    const willClose = after <= 0;
    const impact = willClose ? this.i18n.t('workforce.ui.advances.repayImpactClose') : this.i18n.t('workforce.ui.advances.repayImpactRemaining', { amount: after });
    return { before, amount, after, willClose, impact };
  });

  form: {
    recipientType: string; workerId: string; contractorId: string; employeeId: string;
    amount: number; termType: string; totalInstallments: number;
    installmentAmount: number; deductionFrequency: string;
    maxDeductionPercent: number; reason: string;
    firstInstallmentDate: string; deductionMode: string; deferralPeriods: number;
  } = this.defaultForm();

  totalGranted = () => this.workforceService.advances().reduce((s, a) => s + (a.amount ?? 0), 0);
  totalRemaining = () => this.workforceService.advances().reduce((s, a) => s + (a.remainingBalance ?? 0), 0);
  employeeCategories = computed(() => {
    const unique = new Map<string, string>();
    this.workforceService.employees().forEach(employee => unique.set(employee.categoryId, employee.categoryName));
    return Array.from(unique, ([id, name]) => ({ id, name }));
  });

  ngOnInit() {
    this.loading.set(true);
    this.workforceService.loadAdvances().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
    this.workforceService.loadWorkers().subscribe();
    this.workforceService.loadContractors().subscribe();
    this.workforceService.loadEmployees().subscribe({
      next: () => {
        if (this.route.snapshot.queryParamMap.get('recipientType') === 'EMPLOYEE') {
          this.openCreateModal('EMPLOYEE');
        }
      }
    });
    this.workforceService.loadCategories().subscribe();
    this.workforceService.loadAdvancePolicies().subscribe();
  }

  openCreateModal(recipientType: 'WORKER' | 'CONTRACTOR' | 'EMPLOYEE' = 'WORKER') {
    this.form = this.defaultForm();
    this.form.recipientType = recipientType;
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    const employees = this.workforceService.employees();
    if (recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
    if (recipientType === 'EMPLOYEE' && employees.length > 0) this.form.employeeId = employees[0].id;
    this.applyAdvancePolicy();
    this.isModalOpen = true;
  }

  onRecipientTypeChange() {
    this.form.workerId = '';
    this.form.contractorId = '';
    this.form.employeeId = '';
    const workers = this.workforceService.workers();
    const contractors = this.workforceService.contractors();
    const employees = this.workforceService.employees();
    if (this.form.recipientType === 'WORKER' && workers.length > 0) this.form.workerId = workers[0].id;
    if (this.form.recipientType === 'CONTRACTOR' && contractors.length > 0) this.form.contractorId = contractors[0].id;
    if (this.form.recipientType === 'EMPLOYEE' && employees.length > 0) this.form.employeeId = employees[0].id;
    this.applyAdvancePolicy();
  }

  openPolicyModal(): void { this.policyForm = this.defaultPolicyForm(); this.policyModalOpen.set(true); }

  savePolicy(): void {
    if (this.policyForm.scopeType !== 'GLOBAL' && !this.policyForm.scopeId) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policySelectScope')); return; }
    if (!this.policyForm.effectiveFrom) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policyStartRequired')); return; }
    if (this.policyForm.effectiveTo && this.policyForm.effectiveTo < this.policyForm.effectiveFrom) { this.notificationService.warning(this.i18n.t('workforce.ui.advances.policyDateInvalid')); return; }
    this.saving.set(true);
    this.workforceService.saveAdvancePolicy(this.policyForm).subscribe({
      next: () => { this.saving.set(false); this.policyModalOpen.set(false); this.notificationService.success(this.i18n.t('workforce.ui.advances.policySaved')); },
      error: error => { this.saving.set(false); this.notificationService.error(apiErrorDetail(error, this.i18n.t('workforce.ui.advances.policySaveFailed'))); },
    });
  }

  applyAdvancePolicy(): void {
    const effectiveDate = this.form.firstInstallmentDate || new Date().toISOString().slice(0, 10);
    const policies = this.workforceService.advancePolicies().filter(policy => policy.active
      && policy.effectiveFrom <= effectiveDate && (!policy.effectiveTo || policy.effectiveTo >= effectiveDate));
    const worker = this.workforceService.workers().find(item => item.id === this.form.workerId);
    const employee = this.workforceService.employees().find(item => item.id === this.form.employeeId);
    const newest = (items: AdvancePolicy[]) => items.sort((a, b) => b.version - a.version)[0];
    const policy = (this.form.recipientType === 'EMPLOYEE'
      ? newest(policies.filter(item => item.scopeType === 'EMPLOYEE' && item.scopeId === employee?.id))
        ?? newest(policies.filter(item => item.scopeType === 'EMPLOYEE_CATEGORY' && item.scopeId === employee?.categoryId))
      : newest(policies.filter(item => item.scopeType === 'WORKER' && item.scopeId === worker?.id))
        ?? newest(policies.filter(item => item.scopeType === 'CATEGORY' && item.scopeId === worker?.categoryId)))
      ?? newest(policies.filter(item => item.scopeType === 'GLOBAL'));
    this.effectivePolicyPreview.set(policy ?? null);
    if (!policy) return;
    Object.assign(this.form, { deductionMode: policy.deductionMode, deductionFrequency: policy.deductionFrequency, maxDeductionPercent: policy.maxDeductionPercent, totalInstallments: policy.defaultInstallments, deferralPeriods: policy.deferralPeriods });
    this.recalcInstallment();
  }

  getPolicyScopeLabel(policy: AdvancePolicy): string {
    const keys: Record<AdvancePolicy['scopeType'], string> = { GLOBAL: 'workforce.ui.advances.policyScopeGlobal', CATEGORY: 'workforce.ui.advances.policyScopeWorkerCategory', WORKER: 'workforce.ui.advances.policyScopeWorker', EMPLOYEE_CATEGORY: 'workforce.ui.advances.policyScopeEmployeeCategory', EMPLOYEE: 'workforce.ui.advances.policyScopeEmployee' };
    return this.i18n.t(keys[policy.scopeType]);
  }

  recalcInstallment() {
    if (this.form.totalInstallments > 0 && this.form.amount > 0) {
      this.form.installmentAmount = Math.round((this.form.amount / this.form.totalInstallments) * 100) / 100;
    }
  }

  saveAdvance() {
    if (!this.form.amount || this.form.amount <= 0) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.amountRequiredWarning'));
      return;
    }
    const selectedRecipientId = this.form.recipientType === 'WORKER'
      ? this.form.workerId : this.form.recipientType === 'CONTRACTOR'
        ? this.form.contractorId : this.form.employeeId;
    if (!selectedRecipientId) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.recipientRequiredWarning'));
      return;
    }
    if (this.form.termType === 'LONG_TERM' && this.form.totalInstallments < 2) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.installmentsMinimum'));
      return;
    }
    this.saving.set(true);
    this.workforceService.createAdvance(this.form).subscribe({
      next: () => {
        this.saving.set(false);
        this.isModalOpen = false;
        this.notificationService.success(this.i18n.t('workforce.ui.advances.createdSuccess'));
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? this.i18n.t('workforce.ui.unexpectedError'));
        this.notificationService.error(this.i18n.t('workforce.ui.advances.saveFailed', { detail: msg }));
      }
    });
  }

  confirmAction = signal<{ message: string; onConfirm: () => void } | null>(null);

  pauseAdvance(adv: WorkforceAdvance) {
    const msg = this.i18n.t('workforce.ui.advances.pauseConfirm', { name: this.recipientName(adv) });
    this.confirmAction.set({
      message: msg,
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.pauseAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success(this.i18n.t('workforce.ui.advances.pauseSuccess'));
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error(this.i18n.t('workforce.ui.advances.pauseFailed', { detail: e?.error?.message ?? e?.message ?? '' }))
        });
      }
    });
  }

  resumeAdvance(adv: WorkforceAdvance) {
    this.confirmAction.set({
      message: this.i18n.t('workforce.ui.advances.resumeConfirm'),
      onConfirm: () => {
        this.confirmAction.set(null);
        this.workforceService.resumeAdvance(adv.id).subscribe({
          next: () => {
            this.notificationService.success(this.i18n.t('workforce.ui.advances.resumeSuccess'));
            this.workforceService.loadAdvances().subscribe();
          },
          error: (e) => this.notificationService.error(this.i18n.t('workforce.ui.advances.resumeFailed', { detail: e?.error?.message ?? e?.message ?? '' }))
        });
      }
    });
  }

  cancelAction() {
    this.confirmAction.set(null);
  }

  openRepayModal(adv: WorkforceAdvance) {
    this.repayTarget.set(adv);
    this.repayForm = this.defaultRepayForm();
    this.repayForm.amount = adv.remainingBalance;
    this.repayForm.repaymentDate = new Date().toISOString().slice(0, 10);
    this.repayModalOpen.set(true);
  }

  onRepayTypeChange() {
    const adv = this.repayTarget();
    if (!adv) return;
    if (this.repayForm.repaymentType === 'FULL') {
      this.repayForm.amount = adv.remainingBalance;
    }
  }

  confirmRepayment() {
    const adv = this.repayTarget();
    if (!adv) return;
    const amount = this.repayForm.amount;
    if (!amount || amount <= 0) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.repaymentAmountInvalid'));
      return;
    }
    if (amount > adv.remainingBalance) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.repaymentExceedsBalance'));
      return;
    }
    if (this.repayForm.repaymentType === 'FULL' && amount !== adv.remainingBalance) {
      this.notificationService.warning(this.i18n.t('workforce.ui.advances.fullRepaymentMismatch'));
      return;
    }

    this.saving.set(true);
    const payload: AdvanceRepayRequest = {
      amount,
      repaymentType: this.repayForm.repaymentType,
      repaymentDate: this.repayForm.repaymentDate || undefined,
      paymentMethod: this.repayForm.paymentMethod || undefined,
      receiptRef: this.repayForm.receiptRef || undefined,
      notes: this.repayForm.notes || undefined
    };
    this.workforceService.repayAdvance(adv.id, payload).subscribe({
      next: () => {
        this.saving.set(false);
        this.repayModalOpen.set(false);
        this.repayTarget.set(null);
        this.notificationService.success(this.i18n.t('workforce.ui.advances.repaymentSuccess', { amount }));
        this.workforceService.loadAdvances().subscribe();
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? this.i18n.t('workforce.ui.unexpectedError'));
        this.notificationService.error(this.i18n.t('workforce.ui.advances.repaymentFailed', { detail: msg }));
      }
    });
  }

  closeRepayModal() {
    this.repayModalOpen.set(false);
    this.repayTarget.set(null);
  }

  repayAdvance(adv: WorkforceAdvance) {
    this.openRepayModal(adv);
  }

  exportCsv(): void {
    const rows = this.workforceService.advances().map((adv) => ({
      recipient: this.recipientName(adv),
      type: this.recipientTypeLabel(adv.recipientType),
      amount: adv.amount,
      termType: this.getTermLabel(adv.termType),
      totalInstallments: adv.totalInstallments,
      installmentAmount: adv.installmentAmount,
      remainingBalance: adv.remainingBalance,
      deductionFrequency: this.getFrequencyLabel(adv.deductionFrequency),
      maxDeductionPercent: adv.maxDeductionPercent,
      status: this.getStatusLabel(adv.status),
    }));
    exportCsv(
      rows,
      [
        { key: 'recipient', label: this.i18n.t('workforce.ui.advances.recipient') },
        { key: 'type', label: this.i18n.t('workforce.ui.type') },
        { key: 'amount', label: this.i18n.t('workforce.ui.advances.totalAmount') },
        { key: 'termType', label: this.i18n.t('workforce.ui.advances.termType') },
        { key: 'totalInstallments', label: this.i18n.t('workforce.ui.advances.installments') },
        { key: 'installmentAmount', label: this.i18n.t('workforce.ui.advances.installmentAmount') },
        { key: 'remainingBalance', label: this.i18n.t('workforce.ui.advances.remainingBalance') },
        { key: 'deductionFrequency', label: this.i18n.t('workforce.ui.advances.frequency') },
        { key: 'maxDeductionPercent', label: this.i18n.t('workforce.ui.advances.maxDeduction') },
        { key: 'status', label: this.i18n.t('workforce.ui.status') },
      ],
      `advances-${new Date().toISOString().slice(0, 10)}.csv`,
    );
  }

  // --- Labels ---
  getTermLabel(term: string): string { return this.i18n.t(term === 'SHORT_TERM' ? 'workforce.ui.advances.shortTerm' : 'workforce.ui.advances.longTerm'); }

  getFrequencyLabel(freq: string): string { const keys:Record<string,string>={HALF_MONTH:'workforce.ui.advances.halfMonthly',MONTHLY:'workforce.ui.advances.monthly',WEEKLY:'workforce.ui.advances.weekly'}; return keys[freq] ? this.i18n.t(keys[freq]) : freq; }

  getStatusLabel(status: string): string {
    const m: Record<string, string> = { ACTIVE: 'نشطة', PAID_OFF: 'مسدّدة', SUSPENDED: 'موقوفة' };
    return m[status] ?? status;
  }

  recipientName(advance: WorkforceAdvance): string {
    if (advance.recipientType === 'EMPLOYEE') return advance.employeeName ?? '—';
    if (advance.recipientType === 'CONTRACTOR') return advance.contractorName ?? '—';
    return advance.workerName ?? '—';
  }

  recipientTypeLabel(type: WorkforceAdvance['recipientType']): string { const key=type==='EMPLOYEE'?'workforce.ui.employee':type==='CONTRACTOR'?'workforce.ui.contractor':'workforce.ui.worker'; return this.i18n.t(key); }

  private defaultForm() {
    return {
      recipientType: 'WORKER', workerId: '', contractorId: '', employeeId: '',
      amount: 1000, termType: 'SHORT_TERM', totalInstallments: 1,
      installmentAmount: 1000, deductionFrequency: 'HALF_MONTH',
      maxDeductionPercent: 50, reason: '',
      firstInstallmentDate: '', deductionMode: 'AUTO', deferralPeriods: 0
    };
  }

  private defaultRepayForm() {
    return {
      repaymentType: 'FULL' as 'PARTIAL' | 'FULL',
      amount: 0,
      repaymentDate: '',
      paymentMethod: '',
      receiptRef: '',
      notes: ''
    };
  }

  private defaultPolicyForm(): AdvancePolicy {
    return { scopeType: 'GLOBAL', scopeId: '', deductionMode: 'AUTO', deductionFrequency: 'HALF_MONTH', maxDeductionPercent: 50, defaultInstallments: 1, deferralPeriods: 0, version: 1, effectiveFrom: new Date().toISOString().slice(0, 10), effectiveTo: '', active: true };
  }
}
