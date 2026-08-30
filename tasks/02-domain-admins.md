# TASK 02 — Domain-Based Administrators
## Goal
Allow SUPER_ADMIN to create administrators restricted to business domains.
## Domains
Finance, Inventory, Purchasing, Sales, HR, Payroll, Manufacturing, Projects, POS, Laptop Shops.
## Model
Effective access = Tenant + User + Role + Domain + Permission + Scope.
## Acceptance Criteria
- [ ] Domain is persisted and validated.
- [ ] Domain ADMIN sees only its domain.
- [ ] Direct API access to another domain is rejected.
- [ ] Data queries enforce domain/scope.
- [ ] Domain changes are audited.
- [ ] Cross-domain negative tests exist.
