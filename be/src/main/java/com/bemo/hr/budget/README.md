# Budget Control & Encumbrance (الموازنات والالتزامات)

**EN:** Department-scoped budget control for procurement. Budgets define yearly/monthly amounts per department; issuing a purchase order encumbers (reserves) the amount from the department budget, GRN/invoice liquidation consumes the encumbrance, and cancellation releases it. `budget.blocking` property decides whether exceeding a budget is a warning or a hard block (`BUDGET_*` exception codes). Exposes `/api/v1/budget`: budgets CRUD, live status, encumbrance list, Excel export. Frontend: `fe/src/app/features/finance/budgets`.

**AR:** ضبط الموازنات على مستوى الإدارات للمشتريات: تُحدد الموازنات لكل إدارة، وعند إصدار أمر الشراء يُحجز المبلغ من موازنة الإدارة، ويُستهلك الحجز عند الاستلام/الفاتورة، ويُفرج عنه عند الإلغاء. الخاصية `budget.blocking` تحدد بين التحذير والمنع التام (أكواد `BUDGET_*`). الواجهة: `fe/src/app/features/finance/budgets`.

- Layers: `domain` (Budget, Encumbrance) · `application` (BudgetService availability checks, encumber/release/liquidate lifecycle) · `api` (`BudgetController`) · `infrastructure` (repositories).
- Tables/migrations: V122 schema · V123 translations · V124 allowed_menus · V125 hints.
- PO link: optional `PurchaseOrder.departmentId`; issuing a department-scoped PO triggers encumbrance.
- Verification: `./gradlew test -PskipDockerTests` (budget suites) · `npm run check:i18n`.
