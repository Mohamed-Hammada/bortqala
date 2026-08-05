import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { WorkforceService } from '../../data-access/workforce.service';
import { NotificationService } from '../../../../core/notification.service';
import { apiErrorDetail } from '../../../../core/api-error';
import { LaborRequest, LaborRequestItem } from '../../models/workforce.models';
import { ModalDialogComponent } from '../../../../shared/ui/modal-dialog/modal-dialog.component';
import { AppTooltipDirective } from '../../../../shared/ui/app-tooltip/app-tooltip.directive';

@Component({
  selector: 'app-labor-requests',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalDialogComponent, AppTooltipDirective],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">طلبات العمالة</span>
          <h1>سجل طلبات الاحتياج والمتابعة اليومية</h1>
        </div>
        <button type="button" class="btn btn-primary" (click)="openCreateModal()">
          + إنشاء طلب عمالة جديد
        </button>
      </header>

      <!-- Stats bar -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-label">إجمالي الطلبات</span>
          <span class="stat-value">{{ workforceService.laborRequests().length }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">مسودة</span>
          <span class="stat-value draft-count">{{ countByStatus('DRAFT') }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-label">معتمد</span>
          <span class="stat-value approved-count">{{ countByStatus('APPROVED') }}</span>
        </div>
      </div>

      <!-- Loading skeleton -->
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
                <th>رقم الطلب</th>
                <th>التاريخ</th>
                <th>المقاول المكلف</th>
                <th>الوردية</th>
                <th>بنود الطلب</th>
                <th>الإجمالي المطلوب</th>
                <th>الحالة</th>
                <th>منشئ الطلب</th>
                <th>إجراءات</th>
              </tr>
            </thead>
            <tbody>
              @for (req of workforceService.laborRequests(); track req.id) {
                <tr>
                  <td><strong>{{ req.requestNumber }}</strong></td>
                  <td>{{ req.requestDate | date:'yyyy/MM/dd' }}</td>
                  <td>{{ req.contractorName }}</td>
                  <td>{{ req.shiftName || 'الوردية الأولى' }}</td>
                  <td>
                    @if (req.items && req.items.length > 0) {
                      <span class="items-count">{{ req.items.length }} بند</span>
                      <div class="items-preview">
                        @for (item of req.items.slice(0,2); track $index) {
                          <span class="item-chip">{{ item.categoryName || item.categoryId }}: {{ item.requestedCount }}</span>
                        }
                        @if (req.items.length > 2) {
                          <span class="item-chip more">+{{ req.items.length - 2 }}</span>
                        }
                      </div>
                    } @else {
                      <span class="no-items">—</span>
                    }
                  </td>
                  <td>{{ getTotalRequested(req) }} عامل</td>
                  <td>
                    <span class="badge"
                          [class.badge-draft]="req.status === 'DRAFT'"
                          [class.badge-sent]="req.status === 'SENT'"
                          [class.badge-approved]="req.status === 'APPROVED'"
                          [class.badge-completed]="req.status === 'COMPLETED'"
                          [class.badge-cancelled]="req.status === 'CANCELLED'">
                      {{ getStatusLabel(req.status) }}
                    </span>
                  </td>
                  <td>{{ req.createdBy || 'النظام' }}</td>
                  <td>
                    @if (req.status === 'DRAFT') {
                      <button type="button" class="btn btn-sm btn-approve" (click)="approveRequest(req.id)">
                        ✓ اعتماد
                      </button>
                    }
                  </td>
                </tr>
              }
              @if (workforceService.laborRequests().length === 0) {
                <tr><td colspan="9" class="empty-cell">لا توجد طلبات عمالة مسجّلة</td></tr>
              }
            </tbody>
          </table>
        </div>
      }

      <!-- Create Modal -->
      <app-modal-dialog
        [isOpen]="isModalOpen"
        title="إنشاء طلب عمالة جديد"
        size="wide"
        [preventOutsideClose]="true"
        (close)="closeModal()">

        <form (ngSubmit)="saveRequest()" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>رقم الطلب *</label>
              <input type="text" [(ngModel)]="form.requestNumber" name="requestNumber"
                     required class="form-input" />
            </div>

            <div class="form-group">
              <label>المقاول المكلف *</label>
              <select [(ngModel)]="form.contractorId" name="contractorId" required class="form-input">
                @for (c of workforceService.contractors(); track c.id) {
                  <option [value]="c.id">{{ c.name }} ({{ c.code }})</option>
                }
              </select>
            </div>

            <div class="form-group">
              <label>اسم الوردية</label>
              <input type="text" [(ngModel)]="form.shiftName" name="shiftName"
                     class="form-input" placeholder="الوردية الصباحية" />
            </div>

            <div class="form-group">
              <label>ملاحظات الطلب</label>
              <input type="text" [(ngModel)]="form.notes" name="notes" class="form-input" />
            </div>
          </div>

          <!-- Request Items -->
          <div class="items-section">
            <div class="items-header">
              <h3>بنود الطلب (الفئات والكميات)</h3>
              <button type="button" class="btn btn-secondary btn-sm" (click)="addItem()">
                + إضافة بند
              </button>
            </div>

            @if (form.items.length === 0) {
              <div class="empty-items">
                لا توجد بنود بعد — اضغط على "إضافة بند" لتحديد الفئة والكمية المطلوبة
              </div>
            }

            @for (item of form.items; track $index; let i = $index) {
              <div class="item-row">
                <div class="form-group">
                  <label>فئة العمال *</label>
                  <select [(ngModel)]="item.categoryId" [name]="'cat_' + i" class="form-input">
                    @for (cat of workforceService.categories(); track cat.id) {
                      <option [value]="cat.id">{{ cat.name }}</option>
                    }
                  </select>
                </div>
                <div class="form-group">
                  <label>الكمية المطلوبة *</label>
                  <input type="number" [(ngModel)]="item.requestedCount" [name]="'qty_' + i"
                         class="form-input" min="1" required />
                </div>
                <div class="form-group">
                  <label>المُرسَل</label>
                  <input type="number" [(ngModel)]="item.sentCount" [name]="'sent_' + i"
                         class="form-input" min="0" />
                </div>
                <div class="form-group">
                  <label>المقبول</label>
                  <input type="number" [(ngModel)]="item.acceptedCount" [name]="'acc_' + i"
                         class="form-input" min="0" />
                </div>
                <button type="button" class="btn btn-remove" (click)="removeItem(i)" aria-label="حذف بند طلب العمالة" appTooltip="حذف البند — إزالة هذه الفئة من الطلب">✕</button>
              </div>
            }

            @if (form.items.length > 0) {
              <div class="items-total">
                الإجمالي المطلوب: <strong>{{ getTotalRequestedFromForm() }} عامل</strong>
              </div>
            }
          </div>
        </form>

        <div modal-actions class="modal-actions-bar">
          <button type="button" class="btn btn-primary" [disabled]="saving()" (click)="saveRequest()">
            {{ saving() ? 'جارٍ الإرسال...' : 'إرسال الطلب للمقاول' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closeModal()">إلغاء</button>
        </div>
      </app-modal-dialog>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; direction: rtl; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header { display: flex; justify-content: space-between; align-items: center; }
    .page-header h1 { font-size: 1.5rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
    .stat-card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.375rem; }
    .stat-label { font-size: 0.8125rem; color: #64748b; }
    .stat-value { font-size: 1.5rem; font-weight: 800; color: #0f172a; }
    .draft-count { color: #b45309; }
    .approved-count { color: #15803d; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.25rem; }
    .skeleton-row { height: 48px; background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%); background-size: 200% 100%; border-radius: 6px; animation: shimmer 1.5s infinite; margin-bottom: 0.5rem; }
    @keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
    .data-table { width: 100%; border-collapse: collapse; text-align: right; }
    .data-table th, .data-table td { padding: 0.625rem 0.75rem; border-bottom: 1px solid #e2e8f0; font-size: 0.875rem; }
    .data-table th { background: #f8fafc; font-weight: 700; color: #475569; }
    .items-count { font-weight: 600; color: #1e40af; font-size: 0.8125rem; }
    .items-preview { display: flex; flex-wrap: wrap; gap: 0.25rem; margin-top: 0.25rem; }
    .item-chip { background: #eff6ff; color: #1e40af; padding: 0.125rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }
    .item-chip.more { background: #f1f5f9; color: #64748b; }
    .no-items { color: #94a3b8; }
    .btn { padding: 0.5rem 1rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; font-size: 0.875rem; }
    .btn:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-primary { background: #d97706; color: #fff; }
    .btn-secondary { background: #e2e8f0; color: #334155; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    .btn-approve { background: #dcfce7; color: #166534; padding: 0.375rem 0.75rem; font-size: 0.8125rem; }
    .btn-remove { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; padding: 0.375rem 0.5rem; border-radius: 6px; cursor: pointer; align-self: flex-end; }
    .badge { padding: 0.25rem 0.625rem; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
    .badge-draft { background: #fef3c7; color: #92400e; }
    .badge-sent { background: #dbeafe; color: #1e40af; }
    .badge-approved { background: #dcfce7; color: #166534; }
    .badge-completed { background: #f0fdf4; color: #15803d; }
    .badge-cancelled { background: #fee2e2; color: #991b1b; }
    .empty-cell { text-align: center; color: #94a3b8; padding: 2rem; }
    .form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    .form-group { display: flex; flex-direction: column; gap: 0.375rem; }
    .form-group label { font-weight: 600; font-size: 0.8125rem; color: #334155; }
    .form-input { padding: 0.625rem; border: 1px solid #cbd5e1; border-radius: 8px; font-size: 0.875rem; }
    .items-section { margin-top: 1.25rem; border-top: 1px solid #e2e8f0; padding-top: 1rem; }
    .items-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
    .items-header h3 { font-size: 0.9375rem; font-weight: 700; color: #0f172a; }
    .empty-items { text-align: center; color: #94a3b8; padding: 1rem; background: #f8fafc; border-radius: 8px; font-size: 0.875rem; }
    .item-row { display: grid; grid-template-columns: 1fr 100px 100px 100px 40px; gap: 0.75rem; align-items: end; margin-bottom: 0.75rem; padding: 0.75rem; background: #f8fafc; border-radius: 8px; }
    .items-total { background: #eff6ff; border-radius: 6px; padding: 0.5rem 0.75rem; font-size: 0.875rem; color: #1e40af; text-align: left; }
    .modal-actions-bar { width: 100%; display: flex; gap: 0.75rem; justify-content: flex-start; }
  `]
})
export class LaborRequestsComponent implements OnInit {
  workforceService = inject(WorkforceService);
  private notificationService = inject(NotificationService);

  loading = signal(false);
  saving = signal(false);
  isModalOpen = false;

  form: {
    requestNumber: string; contractorId: string; shiftName: string;
    notes: string; items: LaborRequestItem[];
  } = this.defaultForm();

  ngOnInit() {
    this.loading.set(true);
    forkJoin({
      requests: this.workforceService.loadLaborRequests(),
      contractors: this.workforceService.loadContractors(),
      categories: this.workforceService.loadCategories(),
    }).subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false)
    });
  }

  openCreateModal() {
    this.form = this.defaultForm();
    const ctrs = this.workforceService.contractors();
    if (ctrs.length > 0) this.form.contractorId = ctrs[0].id;
    this.isModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
  }

  addItem() {
    const cats = this.workforceService.categories();
    this.form.items.push({
      categoryId: cats.length > 0 ? cats[0].id : '',
      categoryName: cats.length > 0 ? cats[0].name : '',
      requestedCount: 1,
      sentCount: 0,
      acceptedCount: 0,
      varianceCount: 0
    });
  }

  removeItem(index: number) {
    this.form.items.splice(index, 1);
  }

  saveRequest() {
    if (!this.form.requestNumber || !this.form.contractorId) {
      this.notificationService.warning('يجب إدخال رقم الطلب واختيار المقاول');
      return;
    }
    this.saving.set(true);
    this.workforceService.createLaborRequest(this.form).subscribe({
      next: (res) => {
        this.saving.set(false);
        this.closeModal();
        this.notificationService.success(`تم إنشاء الطلب ${res.requestNumber} بنجاح ✓`);
      },
      error: (e) => {
        this.saving.set(false);
        const msg = apiErrorDetail(e, e?.error?.message ?? e?.message ?? 'خطأ غير متوقع');
        this.notificationService.error('فشل إنشاء الطلب: ' + msg);
      }
    });
  }

  approveRequest(id: string) {
    // Approve endpoint: PUT /api/v1/workforce/labor-requests/{id}/approve
    this.notificationService.info('جاري الاعتماد...');
  }

  // --- Helpers ---
  countByStatus(status: string): number {
    return this.workforceService.laborRequests().filter(r => r.status === status).length;
  }

  getTotalRequested(req: LaborRequest): number {
    return (req.items ?? []).reduce((s, i) => s + (i.requestedCount ?? 0), 0);
  }

  getTotalRequestedFromForm(): number {
    return this.form.items.reduce((s, i) => s + (i.requestedCount ?? 0), 0);
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'مسودة', SENT: 'أُرسل للمقاول', PARTIAL: 'مكتمل جزئياً',
      APPROVED: 'معتمد', COMPLETED: 'مكتمل', CLOSED: 'مغلق',
      CANCELLED: 'ملغي', REJECTED: 'مرفوض'
    };
    return map[status] ?? status;
  }

  private defaultForm() {
    return {
      requestNumber: 'REQ-' + String(Date.now()).slice(-6),
      contractorId: '', shiftName: 'الوردية الأولى',
      notes: '', items: [] as LaborRequestItem[]
    };
  }
}
