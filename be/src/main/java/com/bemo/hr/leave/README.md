# Leave Requests, Accruals & Entitlements (الإجازات والأرصدة)

**EN:** Leave management for employees (roadmap item 18, V299–V301): leave requests with approval flow, per-category accrual policies that grant entitlement over time, and balance tracking (earned / taken / remaining) per employee and leave type. Decisions integrate with attendance reporting: an approved leave explains absence days instead of a deduction, and the `OFFICIAL_HOLIDAY` bulk decision covers confirmed holidays so the reviewer is never asked about the same day twice.

**AR:** إدارة إجازات الموظفين (البند 18، ترحيلات V299–V301): طلبات الإجازة بموافقة المدير، وسياسات استحقاق تتراكم لكل فئة عمل، وتتبع الأرصدة (المستحق/المستهلك/المتبقي) لكل موظف ونوع إجازة. تتكامل القرارات مع تقارير الحضور: الإجازة المعتمدة تفسّر أيام الغياب بدل الخصم، وقرار العطلة الرسمية الجماعي يسجّل اليوم مرة واحدة فلا يتكرر السؤال عنه.

- Layers: api (request/approve/balance endpoints) · application (accrual calculation — backend-owned) · domain (request aggregate with `@TenantId`, `@Version`) · infrastructure.
- Frontend: `fe/src/app/features/leaves` (models/service/page/spec).
- Rules v1: no negative balances; accrual posts are period evidence, corrections create new audited entries.
