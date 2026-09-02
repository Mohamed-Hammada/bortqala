# BUG-004 — Procurement exposed but disabled

Priority: **CRITICAL**

/trade/procurement exposes a New Purchase Order action but the module is disabled.

Acceptance:
- [ ] Procurement is genuinely enabled end-to-end OR clearly unavailable.
- [ ] Supplier selection works.
- [ ] Order lines work.
- [ ] Approval/status lifecycle works.
- [ ] Partial receipts work.
- [ ] Invoice matching works.
- [ ] Accounting posting works.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.