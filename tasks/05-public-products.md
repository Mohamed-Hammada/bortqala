# TASK 05 — Public Product Pages
## Goal
Enable anonymous product browsing.
## Routes
`/products`, `/products/:slug`, `/categories/:slug`, `/brands/:slug`
## Must NOT expose
Supplier cost, internal cost, margin, private stock, accounting data or tenant-private data.
## Acceptance Criteria
- [ ] Anonymous listing works.
- [ ] Product detail works.
- [ ] Search/filter/pagination work.
- [ ] Draft/private products excluded.
- [ ] Tenant isolation verified.
- [ ] Sensitive fields never serialized.
- [ ] Mobile behavior verified.
