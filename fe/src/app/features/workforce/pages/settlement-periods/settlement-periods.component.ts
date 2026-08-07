import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';
import { SettlementIssue, SettlementPeriod, SettlementCalculationSummary, ContractorSettlementDetail } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { ContractorSettlementDetailModalComponent } from '../../ui/contractor-settlement-detail-modal.component';
import { NotificationService } from '../../../../core/notification.service';
import { I18nService } from '../../../../core/i18n.service';
import { downloadBlob } from '../../../../core/download';
import { apiErrorDetail } from '../../../../core/api-error';

@Component({
  selector: 'app-settlement-periods',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, ContractorSettlementDetailModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="workforce-container" dir="rtl">
      <header class="page-header">
        <div>
          <span class="eyebrow">دورة التسوية المستندية المالية</span>
          <h1>فترات تسوية العمالة والمقاولين</h1>
          <p>احتساب أجور العمال ومستحقات المقاولين وتتبع القيود والسداد.</p>
        </div>
        <button type="button" class="btn primary" (click)="openCreateModal()">＋ فتح فترة جديدة</button>
      </header>

      <div class="state-flow" aria-label="دورة حالات التسوية">
        <span>1 مسودة</span><b>←</b><span>2 تم الاحتساب</span><b>←</b><span>3 تمت المراجعة</span><b>←</b><span>4 معتمدة</span><b>←</b><span>5 ترحيل للمالية</span><b>←</b><span>6 مقفلة / صرفت</span>
      </div>

      @if (pageError()) { <div class="alert error">{{ pageError() }}</div> }
      @if (workforceService.loading()) { <div class="alert">جارٍ تحميل فترات التسوية…</div> }

      <div class="card table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>الفترة</th>
              <th>الحالة</th>
              <th>آخر احتساب</th>
              <th>الإصدار</th>
              <th>ملخص النتيجة</th>
              <th>تسويات المقاولين</th>
              <th>التحذيرات</th>
              <th>الإجراءات</th>
            </tr>
          </thead>
          <tbody>
            @for (period of workforceService.settlementPeriods(); track period.id) {
              <tr [class.stale]="period.needsRecalculation">
                <td>
                  <strong>{{ period.periodCode }}</strong>
                  <small>{{ period.startDate }} ← {{ period.endDate }} · {{ cycleLabel(period.cycleType) }}</small>
                </td>
                <td>
                  <span class="badge" [class]="'badge ' + period.status.toLowerCase()">{{ statusLabel(period.status) }}</span>
                  @if (period.needsRecalculation) { <small class="stale-note">⚠ يحتاج إعادة احتساب</small> }
                </td>
                <td>
                  @if (period.lastCalculatedAt) {
                    <strong>{{ period.lastCalculatedAt | date:'yyyy-MM-dd HH:mm' }}</strong>
                    <small>{{ period.lastCalculatedBy }}</small>
                  } @else { — }
                  @if (period.lastCalculationError) {
                    <small class="failure">آخر محاولة فشلت: {{ period.lastCalculationError }}</small>
                  }
                </td>
                <td>v{{ period.calculationVersion }}</td>
                <td>
                  <span>{{ period.resultRecordCount }} سجل</span>
                  <small>إجمالي {{ period.resultGrossAmount | number:'1.2-2' }} · خصومات {{ period.resultDeductions | number:'1.2-2' }} · سلف {{ period.resultAdvances | number:'1.2-2' }} · صافي {{ period.resultNetAmount | number:'1.2-2' }}</small>
                </td>
                <td>
                  <button type="button" class="btn secondary link-btn-full" [disabled]="period.calculationVersion === 0" (click)="openContractorSettlementsModal(period)">
                    📋 تسويات المقاولين
                  </button>
                </td>
                <td>
                  <button type="button" class="link-btn" (click)="showIssues(period)">
                    ⚠ {{ period.resultWarningCount }} · ⛔ {{ period.resultErrorCount }}
                  </button>
                </td>
                <td class="actions">
                  <button type="button" class="btn secondary" [disabled]="calculatingId() === period.id || period.status === 'APPROVED' || period.status === 'LOCKED'" (click)="calculatePeriod(period)">
                    {{ calculatingId() === period.id ? 'جارٍ الاحتساب…' : 'إعادة الاحتساب' }}
                  </button>
                  @if (period.status === 'CALCULATED' && !period.needsRecalculation) {
                    <button type="button" class="btn" (click)="reviewPeriod(period)">تأكيد المراجعة</button>
                  }
                  @if (period.status === 'REVIEWED' && !period.needsRecalculation && period.resultErrorCount === 0) {
                    <button type="button" class="btn primary" (click)="approvePeriod(period)">اعتماد</button>
                  }
                  @if (period.status === 'APPROVED') {
                    <button type="button" class="btn danger" (click)="lockPeriod(period)">قفل نهائي</button>
                  }
                  <button type="button" class="btn success" [disabled]="period.calculationVersion === 0" (click)="exportExcel(period)">⇩ Excel</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="empty">لا توجد فترات تسوية.</td></tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Contractor Settlements List Modal for Period -->
      <app-modal-dialog [isOpen]="contractorListOpen()" [title]="'تسويات المقاولين للفترة — ' + (selectedPeriod()?.periodCode || '')" size="wide" (close)="contractorListOpen.set(false)">
        @if (loadingContractorSettlements()) {
          <div class="alert">جارٍ تحميل تسويات المقاولين…</div>
        } @else {
          <table class="data-table">
            <thead>
              <tr>
                <th>المقاول</th>
                <th>نموذج المحاسبة</th>
                <th>إجمالي أجور العمال</th>
                <th>عمولة / رسوم الخدمة</th>
                <th>صافي المستحق</th>
                <th>المنصرف</th>
                <th>الحالة</th>
                <th>الإجراء</th>
              </tr>
            </thead>
            <tbody>
              @for (cs of contractorSettlements(); track cs.id) {
                <tr>
                  <td><strong>{{ cs.contractorName }}</strong></td>
                  <td>{{ cs.accountingModel }}</td>
                  <td>{{ cs.workersNetTotal | number:'1.2-2' }}</td>
                  <td>{{ cs.commissionAmount | number:'1.2-2' }}</td>
                  <td><strong>{{ cs.netPayable | number:'1.2-2' }}</strong></td>
                  <td>{{ cs.paidAmount | number:'1.2-2' }}</td>
                  <td>
                    <span class="badge" [class.posted]="cs.status === 'POSTED'" [class.paid]="cs.status === 'PAID'">
                      {{ cs.status }}
                    </span>
                  </td>
                  <td>
                    <button type="button" class="btn primary" (click)="openDetailModal(cs)">
                      🔍 عرض التفاصيل والإجراءات
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="8" class="empty">لا توجد تسويات مقاولين لهذه الفترة.</td></tr>
              }
            </tbody>
          </table>
        }
        <div modal-actions>
          <button type="button" class="btn secondary" (click)="contractorListOpen.set(false)">إغلاق</button>
        </div>
      </app-modal-dialog>

      <!-- Contractor Settlement Single Detail Modal -->
      <app-contractor-settlement-detail-modal
        [isOpen]="detailModalOpen()"
        [settlement]="selectedContractorSettlement()"
        (close)="detailModalOpen.set(false)"
        (updated)="onSettlementUpdated($event)">
      </app-contractor-settlement-detail-modal>

      <!-- Calculation Result Modal -->
      <app-modal-dialog [isOpen]="summaryOpen()" title="نتيجة إعادة احتساب التسوية" size="wide" (close)="summaryOpen.set(false)">
        @if (calculationError()) {
          <div class="alert error">
            <strong>فشلت المحاولة الجديدة.</strong>
            <span>{{ calculationError() }}</span>
            <small>ظلت آخر نتيجة ناجحة محفوظة دون تغيير.</small>
          </div>
        }
        @if (summary(); as result) {
          <div class="run-meta">
            <span>نجحت العملية</span>
            <span>الإصدار v{{ result.calculationVersion }}</span>
            <span>{{ result.executedAt | date:'yyyy-MM-dd HH:mm:ss' }}</span>
            <span>بواسطة {{ result.executedBy }}</span>
          </div>
          <div class="summary-grid">
            <article><small>السجلات</small><strong>{{ result.totalWorkers }}</strong></article>
            <article><small>إجمالي المستحقات</small><strong>{{ result.grossWorkersAmount | number:'1.2-2' }}</strong></article>
            <article><small>الخصومات</small><strong>{{ result.totalDeductions | number:'1.2-2' }}</strong></article>
            <article><small>السلف</small><strong>{{ result.totalAdvanceDeductions | number:'1.2-2' }}</strong></article>
            <article><small>صافي العمال</small><strong>{{ result.netWorkersAmount | number:'1.2-2' }}</strong></article>
            <article><small>صافي المقاولين</small><strong>{{ result.netContractorsPayable | number:'1.2-2' }}</strong></article>
          </div>
        }
        @if (issues().length) {
          <h3 id="settlement-issues">السجلات التي تحتاج إجراء ({{ issues().length }})</h3>
          <table class="data-table">
            <thead><tr><th>العامل</th><th>النوع</th><th>المشكلة</th><th>الإجراء</th></tr></thead>
            <tbody>
              @for (issue of issues(); track issue.id) {
                <tr>
                  <td>{{ issue.workerName || issue.workerId || '—' }}</td>
                  <td>{{ issue.severity === 'ERROR' ? 'خطأ' : 'تحذير' }}</td>
                  <td>{{ issue.message }}</td>
                  <td><a href="/workforce/workers">فتح سجل العمال</a></td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="empty">لا توجد تحذيرات أو أخطاء في الإصدار المحدد.</p>
        }
        <div modal-actions><button type="button" class="btn primary" (click)="summaryOpen.set(false)">إغلاق</button></div>
      </app-modal-dialog>

      <!-- Create Period Modal -->
      <app-modal-dialog [isOpen]="createOpen()" title="إنشاء فترة تسوية" [preventOutsideClose]="true" (close)="createOpen.set(false)">
        <form class="form" (ngSubmit)="savePeriod()">
          <label>كود الفترة *<input [(ngModel)]="createForm.periodCode" name="periodCode" required /></label>
          <label>تاريخ البداية *<input type="date" [(ngModel)]="createForm.startDate" name="startDate" required /></label>
          <label>تاريخ النهاية *<input type="date" [(ngModel)]="createForm.endDate" name="endDate" required /></label>
        </form>
        <div modal-actions>
          <button type="button" class="btn primary" (click)="savePeriod()">إنشاء</button>
          <button type="button" class="btn secondary" (click)="createOpen.set(false)">إلغاء</button>
        </div>
      </app-modal-dialog>
    </section>
  `,
  styles: [`
    .workforce-container{padding:1.5rem;display:grid;gap:1.25rem}.page-header{display:flex;justify-content:space-between;gap:1rem;align-items:center}.page-header h1{margin:.2rem 0}.page-header p,small{color:#64748b}.eyebrow{color:#b7791f;font-weight:800}.state-flow{display:flex;gap:.65rem;flex-wrap:wrap;align-items:center;background:#fff8e7;border:1px solid #ead7a4;border-radius:12px;padding:.8rem}.card{background:#fff;border:1px solid #e2e8f0;border-radius:14px}.table-wrap{overflow:auto}.data-table{width:100%;border-collapse:collapse;min-width:1050px}.data-table th,.data-table td{padding:.75rem;border-bottom:1px solid #edf0f4;text-align:right;vertical-align:top}.data-table td small{display:block;margin-top:.3rem}.actions{display:flex;gap:.35rem;flex-wrap:wrap}.btn{border:0;border-radius:8px;padding:.55rem .75rem;font-weight:700;cursor:pointer;background:#e8edf3;color:#243247}.btn:disabled{opacity:.5;cursor:not-allowed}.primary{background:#b7791f;color:#fff}.secondary{background:#e8edf3}.success{background:#dcfce7;color:#166534}.danger{background:#fee2e2;color:#991b1b}.badge{display:inline-block;border-radius:999px;padding:.25rem .6rem;background:#eef2f7}.badge.calculated{background:#dbeafe;color:#1d4ed8}.badge.reviewed{background:#fef3c7;color:#92400e}.badge.approved,.badge.locked,.badge.posted,.badge.paid{background:#dcfce7;color:#166534}.stale{background:#fffaf0}.stale-note,.failure{color:#b45309!important}.failure{max-width:280px}.alert{padding:.8rem;border-radius:10px;background:#eff6ff;display:grid;gap:.25rem}.alert.error{background:#fef2f2;color:#991b1b}.run-meta,.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:.75rem}.run-meta{margin-bottom:1rem}.run-meta span,.summary-grid article{padding:.8rem;border:1px solid #e2e8f0;border-radius:10px}.summary-grid article{display:grid;gap:.25rem}.summary-grid strong{font-size:1.25rem}.link-btn{border:0;background:transparent;color:#9a6700;text-decoration:underline;cursor:pointer}.link-btn-full{color:#b7791f;font-weight:800}.form{display:grid;gap:1rem}.form label{display:grid;gap:.35rem;font-weight:700}.form input{padding:.65rem;border:1px solid #cbd5e1;border-radius:8px}.empty{padding:1rem;text-align:center;color:#64748b}@media(max-width:900px){.page-header{align-items:stretch;flex-direction:column}.run-meta,.summary-grid{grid-template-columns:1fr 1fr}}
  `]
})
export class SettlementPeriodsComponent implements OnInit {
  readonly workforceService = inject(WorkforceService);
  readonly i18n = inject(I18nService);
  private readonly notification = inject(NotificationService);

  readonly createOpen = signal(false);
  readonly summaryOpen = signal(false);
  readonly contractorListOpen = signal(false);
  readonly detailModalOpen = signal(false);
  readonly loadingContractorSettlements = signal(false);

  readonly calculatingId = signal<string | null>(null);
  readonly summary = signal<SettlementCalculationSummary | null>(null);
  readonly issues = signal<SettlementIssue[]>([]);
  readonly selectedPeriod = signal<SettlementPeriod | null>(null);
  readonly contractorSettlements = signal<ContractorSettlementDetail[]>([]);
  readonly selectedContractorSettlement = signal<ContractorSettlementDetail | null>(null);
  readonly calculationError = signal<string | null>(null);
  readonly pageError = signal<string | null>(null);

  createForm = { periodCode: '', startDate: new Date().toISOString().slice(0, 10), endDate: new Date().toISOString().slice(0, 10), cycleType: 'HALF_MONTH' };

  ngOnInit(): void { this.reload(); }

  reload(): void {
    this.workforceService.loadSettlementPeriods().subscribe({
      error: error => this.pageError.set(apiErrorDetail(error, 'تعذر تحميل فترات التسوية.'))
    });
  }

  openCreateModal(): void {
    this.createForm = { periodCode: `PER-${Date.now().toString().slice(-6)}`, startDate: new Date().toISOString().slice(0, 10), endDate: new Date().toISOString().slice(0, 10), cycleType: 'HALF_MONTH' };
    this.createOpen.set(true);
  }

  savePeriod(): void {
    this.workforceService.createSettlementPeriod(this.createForm).subscribe({
      next: () => { this.createOpen.set(false); this.notification.success(this.i18n.t('workforce.settlementPeriodCreated')); },
      error: error => this.notification.error(apiErrorDetail(error, 'تعذر إنشاء الفترة.'))
    });
  }

  calculatePeriod(period: SettlementPeriod): void {
    this.calculatingId.set(period.id); this.calculationError.set(null); this.summary.set(null); this.issues.set([]); this.summaryOpen.set(true);
    this.workforceService.calculatePeriod(period.id).subscribe({
      next: result => {
        this.summary.set(result); this.issues.set(result.issues); this.calculatingId.set(null); this.reload();
        this.notification.success(`تم الاحتساب بنجاح — الإصدار v${result.calculationVersion}`);
      },
      error: error => {
        this.calculatingId.set(null); this.calculationError.set(apiErrorDetail(error, 'فشلت إعادة الاحتساب.')); this.reload();
      }
    });
  }

  showIssues(period: SettlementPeriod): void {
    this.summary.set(null); this.calculationError.set(period.lastCalculationError ?? null); this.summaryOpen.set(true);
    this.workforceService.loadSettlementIssues(period.id).subscribe({
      next: value => this.issues.set(value),
      error: error => this.calculationError.set(apiErrorDetail(error, 'تعذر تحميل المشاكل.'))
    });
  }

  openContractorSettlementsModal(period: SettlementPeriod): void {
    this.selectedPeriod.set(period);
    this.contractorListOpen.set(true);
    this.loadingContractorSettlements.set(true);
    this.workforceService.loadContractorSettlementsForPeriod(period.id).subscribe({
      next: list => {
        this.contractorSettlements.set(list);
        this.loadingContractorSettlements.set(false);
      },
      error: error => {
        this.loadingContractorSettlements.set(false);
        this.notification.error(apiErrorDetail(error, 'تعذر تحميل تسويات المقاولين.'));
      }
    });
  }

  openDetailModal(cs: ContractorSettlementDetail): void {
    this.selectedContractorSettlement.set(cs);
    this.detailModalOpen.set(true);
  }

  onSettlementUpdated(updatedItem: ContractorSettlementDetail): void {
    this.selectedContractorSettlement.set(updatedItem);
    if (this.selectedPeriod()) {
      this.openContractorSettlementsModal(this.selectedPeriod()!);
    }
  }

  reviewPeriod(period: SettlementPeriod): void {
    this.workforceService.reviewPeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodReviewed')); },
      error: error => this.notification.error(apiErrorDetail(error, 'تعذر مراجعة الفترة.'))
    });
  }

  approvePeriod(period: SettlementPeriod): void {
    this.workforceService.approvePeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodApproved')); },
      error: error => this.notification.error(apiErrorDetail(error, 'تعذر اعتماد الفترة.'))
    });
  }

  lockPeriod(period: SettlementPeriod): void {
    this.workforceService.lockPeriod(period.id).subscribe({
      next: () => { this.reload(); this.notification.success(this.i18n.t('workforce.settlementPeriodLocked')); },
      error: error => this.notification.error(apiErrorDetail(error, 'تعذر قفل الفترة.'))
    });
  }

  exportExcel(period: SettlementPeriod): void {
    this.workforceService.exportSettlementPeriodExcel(period.id).subscribe({
      next: blob => downloadBlob(blob, `settlement-${period.periodCode}-v${period.calculationVersion}.xlsx`),
      error: error => this.notification.error(apiErrorDetail(error, 'تعذر التصدير.'))
    });
  }

  statusLabel(status: string): string {
    return ({ DRAFT: 'مسودة', CALCULATED: 'تم الاحتساب', REVIEWED: 'تمت المراجعة', APPROVED: 'معتمدة', LOCKED: 'مقفلة' } as Record<string, string>)[status] ?? status;
  }

  cycleLabel(cycle: string): string {
    return cycle === 'HALF_MONTH' ? 'نصف شهري' : cycle === 'MONTHLY' ? 'شهري' : cycle;
  }
}
