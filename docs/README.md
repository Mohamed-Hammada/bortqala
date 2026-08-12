# BEMO ERP — Documentation Index

Welcome to the canonical documentation tree for **BEMO ERP**.

---

## Documentation Structure

```text
docs/
├── README.md                              # Main documentation index (this file)
├── product/
│   ├── BUSINESS_REQUIREMENTS.md            # Target business requirements & domain models
│   ├── BUSINESS_FLOWS.md                   # End-to-end P2P, O2C, Payroll, Manufacturing flows
│   └── GLOSSARY.md                         # Terminology & business definitions
├── architecture/
│   ├── SYSTEM_OVERVIEW.md                  # High-level architecture & tech stack
│   ├── BACKEND_ARCHITECTURE.md             # Spring Boot, JPA, Liquibase, Multi-tenancy
│   ├── FRONTEND_ARCHITECTURE.md            # Angular standalone components, signals, state
│   ├── ACCOUNTING_POSTING_MODEL.md         # Double-entry ledger, posting profiles, source links
│   └── DECIMAL_AND_MONEY_STANDARD.md       # High-precision monetary calculations (DECIMAL 15,4)
├── modules/
│   ├── FINANCE_AND_CLOSE.md                # General ledger, fiscal close, subledger reconciliation
│   ├── PAYROLL.md                          # Payroll runs, input snapshots, component evaluator
│   ├── ATTENDANCE.md                       # Biometrics, shift schedules, downtime decisions
│   ├── WORKFORCE.md                        # Contractor labor requests, settlement lifecycle
│   ├── PROCUREMENT.md                      # Requisitions, RFQs, quotes, awards, POs, 3-way match
│   ├── SALES.md                            # Orders, reservations, deliveries, COGS, RMA returns
│   ├── INVENTORY.md                        # Warehouses, bins, reservations, transfers, counts
│   ├── MANUFACTURING_AND_QUALITY.md        # Production orders, material issues, WIP, variance
│   ├── BUDGET_AND_TREASURY.md              # Budget availability, revisions, payment batches
│   └── SUPPORT_NOTIFICATIONS_ABOUT.md      # Web push, bulk send, support tickets, system about
├── implementation/
│   ├── IMPLEMENTATION_STATUS.md            # Authoritative feature & DOD verification status
│   ├── REMAINING_WORK.md                   # Pending roadmap & technical tasks
│   └── DEFINITION_OF_DONE.md               # Strict 13-point DOD criteria
├── testing/
│   ├── TEST_STRATEGY.md                    # Unit, integration, concurrency & negative testing
│   ├── QA_REGRESSION.md                    # Manual & automated regression test suites
│   └── VERIFICATION.md                     # Empirical runtime test evidence
├── operations/
│   ├── DEPLOYMENT.md                       # Docker, PostgreSQL, desktop build packaging
│   └── ENVIRONMENT_CONFIGURATION.md        # Spring profiles, JWT, VAPID, DB configuration
└── release/
    ├── CHANGELOG.md                        # Release history
    └── RELEASE_CHECKLIST.md                # Production readiness checklist
```

---

## Quick Navigation

- **[System Architecture](file:///d:/hamada-bemo-01/docs/architecture/SYSTEM_OVERVIEW.md)**
- **[Implementation Status](file:///d:/hamada-bemo-01/docs/implementation/IMPLEMENTATION_STATUS.md)**
- **[Verification Evidence](file:///d:/hamada-bemo-01/docs/testing/VERIFICATION.md)**
- **[Definition of Done](file:///d:/hamada-bemo-01/docs/implementation/DEFINITION_OF_DONE.md)**
