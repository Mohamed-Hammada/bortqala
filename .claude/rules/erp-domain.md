# ERP Domain Safety Rules

The platform spans HR/attendance, contractor workforce, payroll, procurement, sales, inventory, manufacturing, finance, notifications, support, analytics, branches, and executive reporting.

When changing a business flow:
- preserve accounting/ledger side effects
- preserve inventory movement semantics
- preserve audit logging where applicable
- respect document/workflow state transitions
- check cross-module side effects
- preserve tenant and branch boundaries
- update both backend contracts and frontend consumers when API behavior changes

For financial or payroll calculations, inspect existing formulas, rounding, period boundaries, and statutory/business rules before modifying behavior.
