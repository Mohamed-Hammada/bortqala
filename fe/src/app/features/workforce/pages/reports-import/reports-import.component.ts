import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkforceService } from '../../data-access/workforce.service';

@Component({
  selector: 'app-reports-import',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="workforce-container">
      <header class="page-header">
        <div>
          <span class="eyebrow">التكامل والتقارير</span>
          <h1>تقارير العمالة والمقاولين واستيراد ملفات Excel</h1>
        </div>
      </header>

      <div class="reports-grid">
        <div class="card">
          <h3>استيراد وتدقيق ملفات Excel القديمة</h3>
          <p>رفع وتدقيق كشوف الحضور واليوميات السابقة مع المطابقة التلقائية بين أوراق العمل.</p>
          
          <div class="import-box">
            <button type="button" class="btn btn-primary" (click)="runDiagnostic()">
              تشغيل فحص المطابقة واستيراد الملف المرجعي
            </button>
          </div>

          <div *ngIf="diagnosticResult" class="diagnostic-results">
            <h4>نتيجة فحص المطابقة المرجعي:</h4>
            <ul>
              <li>عدد الأوراق المعالجة: <strong>{{ diagnosticResult.totalSheetsProcessed }} أوراق</strong></li>
              <li>إجمالي وحدات الحضور في كشف حساب الفترة: <strong>{{ diagnosticResult.totalDaysInSettlement }}</strong></li>
              <li>إجمالي وحدات الحضور في كشف العدد اليومي: <strong>{{ diagnosticResult.totalDaysInSummary }}</strong></li>
              <li class="warning-li" *ngIf="diagnosticResult.requiresReconciliationWarning">
                ⚠️ <strong>فرق مطابقة مكتشف: {{ diagnosticResult.discrepancyDays }} وحدة حضور.</strong>
                <p class="warning-desc">وجد اختلاف بين الأوراق. يعتمد النظام حقيقة معادلاته الخاصة ويرفض الاعتماد حتى مراجعة الفرق.</p>
              </li>
            </ul>
          </div>
        </div>

        <div class="card">
          <h3>تقارير العمالة القياسية المتاحة</h3>
          <ul class="reports-list">
            <li>📄 كشف حضور وانصراف العامل التفصيلي</li>
            <li>📄 كشف اليوميات والمستحقات بالفترة</li>
            <li>📄 كشف خصومات وسُلف العمال والأقساط المسددة</li>
            <li>📄 كشف حساب المقاول وعمولات التشغيل</li>
            <li>📄 كشف استلام المقاول للطباعة والتوقيع</li>
          </ul>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .workforce-container { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; }
    .eyebrow { font-size: 0.875rem; color: #d97706; font-weight: 600; }
    .page-header h1 { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0.25rem 0 0 0; }
    .reports-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1.5rem; }
    .card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; padding: 1.5rem; }
    .btn { padding: 0.75rem 1.5rem; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; }
    .btn-primary { background: #d97706; color: #fff; }
    .import-box { margin: 1.5rem 0; }
    .diagnostic-results { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 1rem; }
    .diagnostic-results ul { list-style: none; padding: 0; margin: 0.5rem 0 0 0; display: flex; flex-direction: column; gap: 0.5rem; }
    .warning-li { background: #fffbeb; border: 1px solid #fde68a; padding: 0.75rem; border-radius: 6px; color: #92400e; }
    .warning-desc { font-size: 0.875rem; margin: 0.25rem 0 0 0; }
    .reports-list { list-style: none; padding: 0; display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1rem; }
    .reports-list li { font-weight: 600; color: #334155; }
  `]
})
export class ReportsImportComponent {
  workforceService = inject(WorkforceService);
  diagnosticResult: any = null;

  runDiagnostic() {
    this.workforceService.analyzeImport(1550, 1635).subscribe(res => {
      this.diagnosticResult = res;
    });
  }
}
