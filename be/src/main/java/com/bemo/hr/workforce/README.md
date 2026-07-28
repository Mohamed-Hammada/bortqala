# Workforce & Contractors Module (إدارة العمالة والمقاولين)

This package implements the core business domain, data persistence, settlement calculations, labor requests, advance ledgers, manual attendance matrices, and REST APIs for contractors and daily/hourly workforce management.

## Key Services
- `ContractorService`: Manages contractor profiles, historical rates, and 4 calculation models (`worker_net_total`, `contractor_daily_rate`, `worker_cost_plus_fee`, `fixed_period_amount`).
- `WorkerService`: Manages daily worker entities, category defaults, rate versions, and contractor assignment history.
- `LaborRequestService`: Handles labor headcount requests, contractor fulfillments, and variance tracking.
- `WorkforceAttendanceService`: Saves matrix attendance entries (1, 0.5, 0, hours) and calculates daily effective earnings.
- `WorkforceSettlementService`: Calculates and locks 15-day / monthly settlements with audit snapshotting.
- `WorkforceAdvanceService`: Short/long term workforce advances with installment scheduling and maximum deduction thresholds.
- `WorkforceExcelImportService`: Multi-sheet Excel parsing and discrepancy diagnostics (e.g. 1635 vs 1550 day discrepancy detection).
