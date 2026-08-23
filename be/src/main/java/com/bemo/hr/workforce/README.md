# Workforce — Contractor Daily-Wage Module (عمال المقاولين اليوميون)

**EN:** Complete contractor-workforce lifecycle (Sessions 3+; roadmap item 27 attribution V322–V323): labor requests → contractor supplies worker batch → worker assignment → manual attendance (no fingerprint) → attendance lock → 15-day settlement periods → advances & deduction adjustments → settlement review/approval → finance journal posting + partner ledger → supplier invoice link → payment/disbursal. Worker categories mirror HR categories; project attribution syncs costs to the projects cost ledger.

**AR:** دورة حياة كاملة لعمال المقاولين اليوميين: طلبات العمالة ← توريد دفعة عمال من المقاول ← الإسناد ← حضور يدوي دون بصمة ← إقفال الحضور ← فترات تسوية نصف شهرية ← السلف والخصومات ← مراجعة التسوية واعتمادها ← ترحيل القيود ودليل الحسابات ← ربط فاتورة المورد ← الصرف. فئات العمال توازي فئات الموظفين، وتُحمَّل التكاليف على المشاريع تلقائياً.

- Key files: `domain/WorkforceAdvanceInstallment.java`, `application/WorkforceAdvanceService.calculateEmployeePayrollDeduction(...)` (consumed by PayrollService), settlement line/adjustment aggregates (V104–V105 schema).
- Idempotency: settlement posting uses `operationId`; reversals reverse advance settlements symmetrically.
- Frontend: `fe/src/app/features/workforce` (pages/ui/data-access/models) incl. `ContractorSettlementDetailModalComponent`.
- Gap tracked: client-side revenue billing for manpower-supply companies = WP-17 (implementation guide).
